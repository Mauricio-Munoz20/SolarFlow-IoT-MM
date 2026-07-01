package com.solarflow.repository;

import com.solarflow.config.AppConfig;
import com.solarflow.model.Alert;
import com.solarflow.model.SensorData;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.MongoClient;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * Repositorio reactivo de Mongo DB.
 */
public class MongoRepository {
        private static Logger log = LoggerFactory.getLogger(MongoRepository.class);

        private final MongoClient mongoClient;

        public MongoRepository(Vertx vertx, AppConfig appConfig) {
                JsonObject mongoConfig = new JsonObject().put("connection_string", appConfig.mongoUri())
                                .put("db_name", appConfig.mongoDatabase());

                // createShared recicla la conexión en el event loop
                this.mongoClient = MongoClient.createShared(vertx, mongoConfig);
                log.info("Mongo DB conectado exitosamente a {}", appConfig.mongoDatabase());
        }

        public Future<Void> saveSensorData(SensorData sensorData) {
                JsonObject doc = new JsonObject().put("deviceId", sensorData.deviceId())
                                .put("temperatura", sensorData.temperatura())
                                .put("humedad", sensorData.humedad()).put("nivelAgua", sensorData.nivelAgua())
                                .put("timestamp", new JsonObject().put("$date",
                                                java.time.Instant.ofEpochMilli(sensorData.timestamp()).toString()));

                return mongoClient.save("sensor_data", doc)
                                .onSuccess(id -> log.debug("Métrica de {} guardada en Mongo (ID: {})",
                                                sensorData.deviceId(), id))
                                .onFailure(err -> log.error("Error guardado en metrica {} en Mongo: {}",
                                                err.getMessage()))
                                .mapEmpty();
        }

        public Future<List<JsonObject>> getRecentData(String deviceID, int limit) {
                JsonObject query = new JsonObject().put("deviceId", deviceID);
                FindOptions options = new FindOptions().setSort(new JsonObject().put("timestamp", -1)) // Mas recientes
                                                                                                       // primero
                                .setLimit(limit);

                return mongoClient.findWithOptions("sensor_data", query, options)
                                .onFailure(err -> log.error("Error consultando recientes de {}: {}", deviceID,
                                                err.getMessage()));
        }

        public Future<Void> saveAlert(Alert alert) {
                JsonObject doc = new JsonObject().put("alertId", alert.alertaId()).put("deviceId", alert.deviceId())
                                .put("type", alert.type()).put("severity", alert.severity())
                                .put("triggerValue", alert.triggerValue())
                                .put("threshold", alert.threshold()).put("message", alert.message())
                                .put("timestamp", new JsonObject().put("$date",
                                                java.time.Instant.ofEpochMilli(alert.timestamp()).toString()));

                return mongoClient.save("alerts_history", doc)
                                .onSuccess(id -> log.warn("¡ALERTA! ({}) registrada en Mongo (ID: {})",
                                                alert.severity(), id))
                                .onFailure(err -> log.error("¡ERROR GROTESCO!: No se pudo guardar la alerta: {}",
                                                err.getMessage()))
                                .mapEmpty();
        }

        public Future<JsonObject> getAlertConfig(String deviceID) {
                return mongoClient.findOne("sensors", new JsonObject().put("deviceId", deviceID), null)
                                .map(doc -> doc != null ? doc.getJsonObject("alertConfig") : null)
                                .onFailure(err -> log.error("Error obteniendo config de {}: {}", deviceID,
                                                err.getMessage()));
        }

}
