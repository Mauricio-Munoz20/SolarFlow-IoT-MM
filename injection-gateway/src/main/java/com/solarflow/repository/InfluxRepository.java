package com.solarflow.repository;

import com.solarflow.config.AppConfig;
import com.solarflow.model.SensorData;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import java.util.Locale;

/**
 * Repositorio reactivo de Influx DB IoX
 * Usa Line Protocol para escritura (ultra-baja latencia) y SQL HTTP para
 * consultas analíticas
 */

public class InfluxRepository {
    private static final Logger log = LoggerFactory.getLogger(InfluxRepository.class);
    private final WebClient client;
    private final String baseUrl;
    private final String token;
    private final String bucket;

    public InfluxRepository(Vertx vertx, AppConfig appConfig) {
        this.baseUrl = appConfig.influxUrl();
        this.token = appConfig.influxToken();
        this.bucket = appConfig.influxBucket();

        this.client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(baseUrl));
        log.info("Influx DB IOx configurado en {}", baseUrl);
    }

    /**
     *
     */
    public Future<Void> writeMetrics(SensorData data) {
        // Locale.US se usa para evitar comas decimales
        // MUY IMPORTANTE: InfluxDB odia los espacios antes o después del signo '='.
        // El formato ESTRICTO es: measurement,tagK=tagV fieldK=fieldV,fieldK2=fieldV2
        // timestamp
        String line = String.format(Locale.US,
                "sensor_metrics,deviceId=%s temperatura=%f,humedad=%f,nivelAgua=%di %d000000",
                data.deviceId(), data.temperatura(), data.humedad(), data.nivelAgua(), data.timestamp());

        return client.postAbs(baseUrl + "/api/v2/write?bucket=" + bucket + "&precision=ns")
                .putHeader("Authorization", "Token " + token)
                .putHeader("Content-Type", "text/plain; charset=utf-8")
                .sendBuffer(Buffer.buffer(line))
                .compose(res -> {
                    if (res.statusCode() == 204 || res.statusCode() == 200) {
                        log.debug(" Métrica de {} inyectada en InfluxDB", data.deviceId());
                        // Java necesita el <Void> explícito aquí para no inferir Future<Object>
                        return Future.<Void>succeededFuture();
                    }
                    return Future.<Void>failedFuture(
                            "Influx write failed: HTTP " + res.statusCode() + " - " + res.bodyAsString());
                })
                .onFailure(err -> log.error(" Error escribiendo en IOx: {}", err.getMessage()));
    }

    /**
     * Consulta analítica: Promedio de temperatura por hora (Idea del profesor).
     * Usa el endpoint /api/v3/query_sql de IOx v3.
     */
    public Future<JsonObject> getHourlyAverage(String deviceId) {
        String sql = "SELECT DATE_TRUNC('hour', time) AS hour, AVG(temperatura) AS avg_temp " +
                "FROM sensor_metrics WHERE deviceId = '" + deviceId + "' " +
                "GROUP BY hour ORDER BY hour DESC LIMIT 24";

        JsonObject body = new JsonObject().put("query", sql);

        return client.postAbs(baseUrl + "/api/v3/query_sql?database=" + bucket)
                .putHeader("Authorization", "Bearer " + token)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .compose(res -> {
                    if (res.statusCode() == 200) {
                        return Future.succeededFuture(res.bodyAsJsonObject());
                    }
                    return Future.failedFuture("Influx query failed: HTTP " + res.statusCode());
                })
                .onFailure(err -> log.error(" Error consultando IOx: {}", err.getMessage()));
    }
}
