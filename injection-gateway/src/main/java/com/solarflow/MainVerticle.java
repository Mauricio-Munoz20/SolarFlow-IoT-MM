package com.solarflow;

import com.solarflow.config.AppConfig;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import com.solarflow.web.router.ApiRouter;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        log.info("⚙️ Iniciando secuencia de arranque de Vert.x...");

        // 1. Cargar la configuración (application.json + variables de entorno)
        io.vertx.config.ConfigStoreOptions fileStore = new io.vertx.config.ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new io.vertx.core.json.JsonObject().put("path", "application.json"));

        io.vertx.config.ConfigRetrieverOptions options = new io.vertx.config.ConfigRetrieverOptions()
                .addStore(fileStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        retriever.getConfig().onComplete(ar -> {
            if (ar.failed()) {
                log.error("❌ Fallo crítico al cargar configuración", ar.cause());
                startPromise.fail(ar.cause());
                return;
            }

            // Convertimos el JSON de configuración en nuestro Record AppConfig
            AppConfig config = AppConfig.fromJsonObject(ar.result());

            // 2. Inicializar Repositorios (Conexiones a BD)
            MongoRepository mongoRepo;
            InfluxRepository influxRepo;
            try {
                mongoRepo = new MongoRepository(vertx, config);
                influxRepo = new InfluxRepository(vertx, config);
            } catch (Exception e) {
                log.error("❌ Fallo al inicializar conexiones de bases de datos", e);
                startPromise.fail(e);
                return;
            }

            // 3. Crear el Enrutador Central (ApiRouter)
            Router router = ApiRouter.create(vertx, config, mongoRepo, influxRepo);

            // 4. Levantar el Servidor HTTP y atarlo al Router
            vertx.createHttpServer(new HttpServerOptions().setPort(config.serverPort()))
                 .requestHandler(router)
                 .listen(res -> {
                     if (res.succeeded()) {
                         log.info("🚀 Servidor HTTP desplegado y escuchando en el puerto {}", config.serverPort());
                         startPromise.complete(); // Le avisa al Main que el despliegue fue 100% exitoso
                     } else {
                         log.error("❌ Fallo al arrancar Servidor HTTP (¿El puerto está en uso?)", res.cause());
                         startPromise.fail(res.cause());
                     }
                 });
        });
    }
}
