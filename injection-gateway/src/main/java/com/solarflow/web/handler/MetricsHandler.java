package com.solarflow.web.handler;

import com.solarflow.model.ApiResponse;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricsHandler {
    private static Logger log =  LoggerFactory.getLogger(MetricsHandler.class);
    private final MongoRepository mongoRepo;
    private final InfluxRepository influxRepo;

    public MetricsHandler(MongoRepository mongoRepo, InfluxRepository influxRepo) {
        this.mongoRepo = mongoRepo;
        this.influxRepo = influxRepo;
    }

    /**
     * Endpoint para obtener los últimos 10 registros (Ideal para gráficas en tiempo real + idea del profe)
     */
    public void getRecent(RoutingContext ctx){
        String deviceId = ctx.queryParam("deviceId")
                .stream().findFirst().orElse("ESP32_001");

        mongoRepo.getRecentData(deviceId, 10)
                .onSuccess(list -> {
                    JsonArray data = new JsonArray(list);
                    ctx.json(ApiResponse.success("Ultimos 10 registros", data));
                })
                .onFailure(err -> {
                    log.error("Error obteniendo los registros: {}",  err.getMessage());
                    ctx.fail(500, err);
                });
    }

    /**
     * Endpoint para obtener el promedio horario de temperatura (Idea del profe)
     */
    public void getHourlyAverage(RoutingContext ctx){
        String deviceId = ctx.queryParam("deviceId")
                .stream().findFirst().orElse("ESP32_001");

        influxRepo.getHourlyAverage(deviceId).onSuccess(result
        -> {ctx.json(ApiResponse.success("Promedio horario", result));
        })
                .onFailure(err -> {
                    log.error("Error obteniendo el promedio: {}",  err.getMessage());
                    ctx.fail(500, err);
                });
    }





}
