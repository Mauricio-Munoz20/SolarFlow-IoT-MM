package com.solarflow;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Inicializamos el framework reactivo
        Vertx vertx = Vertx.vertx();

        logger.info("Iniciando despliegue de Ingestion Gateway...");

        // Desplegamos el MainVerticle y ESCUCHAMOS el resultado asíncrono de su Promise
        vertx.deployVerticle(new MainVerticle())
                .onSuccess(id -> {
                    logger.info(" Despliegue exitoso. ID del Verticle: {}", id);
                    logger.info(" Gateway en línea y listo para recibir telemetría de ESP32.");
                })
                .onFailure(error -> {
                    logger.error(" Fallo catastrófico al iniciar el Gateway. Abortando misión.", error);
                    // Si falla el inicio (ej. base de datos caída o puerto ocupado), matamos la aplicación
                    System.exit(1);
                });
    }
}
