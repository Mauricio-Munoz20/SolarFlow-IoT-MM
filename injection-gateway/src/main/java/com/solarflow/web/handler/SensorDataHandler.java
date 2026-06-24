package com.solarflow.web.handler;

import com.solarflow.exception.ValidationException;
import com.solarflow.model.Alert;
import com.solarflow.model.ApiResponse;
import com.solarflow.model.SensorData;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import com.solarflow.util.Constants;
import com.solarflow.util.WaterLevelMapper;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SensorDataHandler {
    private static Logger logger = LoggerFactory.getLogger(SensorDataHandler.class);

    private final MongoRepository mongoRepo;
    private final InfluxRepository influxRepo;

    public SensorDataHandler(MongoRepository mongoRepo, InfluxRepository influxRepo) {
        this.mongoRepo = mongoRepo;
        this.influxRepo = influxRepo;
    }

    public void handle(RoutingContext ctx) {
        SensorData data;
        try {
            // Vert.x tiene su propio lector JSON, no necesitamos "new ObjectMapper()"
            data = Json.decodeValue(ctx.body().asString(), SensorData.class);
        } catch (Exception e) {
            ctx.fail(new ValidationException("JSON inválido o malformado", 400));
            return;
        }

        try {
            validateData(data);
        } catch (ValidationException e) {
            ctx.fail(e);
            return;
        }

        // Persistencia dual: Mongo -> Influx -> Alertas
        mongoRepo.saveSensorData(data)
                .compose(v -> influxRepo.writeMetrics(data))
                .compose(v -> evaluateAndSaveAlerts(data))
                .onSuccess(v -> {
                    ctx.response()
                            .setStatusCode(202) // 202 Accepted es más exacto para IoT asíncrono
                            .putHeader("Content-Type", "application/json")
                            .end(Json.encode(ApiResponse.success("Datos ingeridos correctamente", null)));
                })
                .onFailure(err -> {
                    logger.error(" Error en ingestión: {}", err.getMessage());
                    ctx.fail(500, err);
                });
    }

    private void validateData(SensorData data) {
        if (data.deviceId() == null || data.deviceId().isBlank()) {
            throw new ValidationException("deviceId es obligatorio");
        }
        if (data.temperatura() == null || !Double.isFinite(data.temperatura())) {
            throw new ValidationException("temperatura debe ser un número válido");
        }
        if (data.humedad() == null || !Double.isFinite(data.humedad())) {
            throw new ValidationException("humedad debe ser un número válido");
        }
        if (!WaterLevelMapper.isValid(data.nivelAgua())) {
            throw new ValidationException("nivelAgua debe estar entre 0 y 4");
        }
    }

    private Future<Void> evaluateAndSaveAlerts(SensorData data) {
        List<Future<Void>> alertFutures = new ArrayList<>();

        if (data.temperatura() > Constants.DEFAULT_ALERT_TEMP_THRESHOLD) {
            Alert alert = new Alert(
                    UUID.randomUUID().toString(), data.deviceId(),
                    Alert.AlertType.HIGH_TEMPERATURE, Alert.AlertSeverity.CRITICAL,
                    data.temperatura(), Constants.DEFAULT_ALERT_TEMP_THRESHOLD,
                    "Temperatura crítica detectada", System.currentTimeMillis());
            alertFutures.add(mongoRepo.saveAlert(alert));
        }

        if (data.nivelAgua() < Constants.DEFAULT_ALERT_WATER_THRESHOLD) {
            Alert alert = new Alert(
                    UUID.randomUUID().toString(), data.deviceId(),
                    Alert.AlertType.LOW_WATER_LEVEL, Alert.AlertSeverity.WARNING,
                    (double) data.nivelAgua(), (double) Constants.DEFAULT_ALERT_WATER_THRESHOLD,
                    "Nivel de agua bajo: " + WaterLevelMapper.toPercentage(data.nivelAgua()),
                    System.currentTimeMillis());
            alertFutures.add(mongoRepo.saveAlert(alert));
        }

        // Si no hubo alertas, devolvemos success. Si hubo, esperamos a que todas se
        // guarden.
        if (alertFutures.isEmpty()) {
            return Future.succeededFuture();
        }
        return Future.all(alertFutures).mapEmpty();
    }
}
