package com.solarflow.web.router;

import com.solarflow.config.AppConfig;
import com.solarflow.exception.GlobalErrorHandler;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import com.solarflow.web.handler.HealthHandler;
import com.solarflow.web.handler.MetricsHandler;
import com.solarflow.web.handler.SensorDataHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class ApiRouter {

    public static Router create(Vertx vertx, AppConfig config,
                                MongoRepository mongoRepo, InfluxRepository influxRepo) {
        Router router = Router.router(vertx);

        // Middleware global para parsear cuerpos JSON (Obligatorio para POST)
        router.route().handler(BodyHandler.create());

        // Instanciar Handlers
        SensorDataHandler sensorHandler = new SensorDataHandler(mongoRepo, influxRepo);
        MetricsHandler metricsHandler = new MetricsHandler(mongoRepo, influxRepo);
        HealthHandler healthHandler = new HealthHandler();

        //  RUTAS OBLIGATORIAS Y DE NEGOCIO
        router.post("/sensor-data").handler(sensorHandler::handle);

        router.get("/metrics/recent").handler(metricsHandler::getRecent);
        router.get("/metrics/average").handler(metricsHandler::getHourlyAverage);

        router.get("/health").handler(healthHandler::handle);

        // Manejador global de errores (captura ValidationException y 500s)
        router.route().failureHandler(new GlobalErrorHandler());
        return router;
    }
}


