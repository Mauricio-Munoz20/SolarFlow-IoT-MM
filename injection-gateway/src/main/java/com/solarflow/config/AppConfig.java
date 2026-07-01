package com.solarflow.config;

import com.solarflow.exception.GlobalErrorHandler;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contenedor inmutable para la configuración de la aplicación.
 * Se instancia a partir del JsonObject que provee Vert.x Config.
 */

public record AppConfig(
        int serverPort,
        String mongoUri,
        String mongoDatabase,
        String influxUrl,
        String influxToken,
        String influxOrg,
        String influxBucket) {
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    public static AppConfig fromJsonObject(JsonObject jsonObject) {
        int port = envInt("SERVER_PORT", jsonObject.getInteger("SERVERT_PORT", 8080));
        String mongoUri = env("MONGO_URI", String.format("mongodb://%s:%s@localhost:27017/",
                jsonObject.getString("MONGO_USER", "admin"),
                jsonObject.getString("MONGO_PASSWORD", "solarflow_secure_123")));
        String influxToken = env("INFLUX_ADMIN_TOKEN", jsonObject.getString("INFLUX_ADMIN_TOKEN", "default-token"));

        AppConfig config = new AppConfig(
                port,
                mongoUri,
                jsonObject.getString("MONGO_DB", "solarflow"),
                env("INFLUX_URL", "http://localhost:8086"),
                influxToken,
                env("INFLUX_ORG", "solarflow"),
                env("INFLUX_BUCKET", "solarflow_metrics"));

        log.info("Configuración cargada - Puerto {}, MongoDB: [oculto], InfluxDB: {}",
                config.serverPort(), config.influxUrl());
        return config;

    }

    // Helpers para ayudar a leer el env con fallback
    private static String env(String key, String fallback) {
        String val = System.getenv(key);
        return val != null && !val.isBlank() ? val : fallback;
    }

    private static int envInt(String key, int fallback) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                /* ignora, usa fallback */ }
        }
        return fallback;
    }
}
