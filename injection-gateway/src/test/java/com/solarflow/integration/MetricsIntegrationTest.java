package com.solarflow.integration;

import com.solarflow.config.AppConfig;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import com.solarflow.web.router.ApiRouter;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith({ VertxExtension.class, MockitoExtension.class })
public class MetricsIntegrationTest {

    @Mock
    private MongoRepository mongoRepo;
    @Mock
    private InfluxRepository influxRepo;

    private HttpServer server;
    private WebClient webClient;

    /**
     * Prepara el entorno de prueba antes de cada test.
     * Crea un servidor HTTP con las rutas API y mocks de repositorios.
     * 
     * @param vertx       Instancia de Vert.x
     * @param testContext Contexto de prueba
     */
    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        Router router = ApiRouter.create(vertx, AppConfig.fromJsonObject(new io.vertx.core.json.JsonObject()),
                mongoRepo, influxRepo);
        server = vertx.createHttpServer()
                .requestHandler(router)
                .listen(0, testContext.succeeding(s -> {
                    webClient = WebClient.create(vertx);
                    testContext.completeNow();
                }));
    }

    /**
     * Cierra el servidor HTTP después de cada prueba.
     * 
     * @param testContext Contexto de prueba
     */
    @AfterEach
    void tearDown(VertxTestContext testContext) {
        server.close(testContext.succeedingThenComplete());
    }

    /**
     * Prueba el endpoint GET /metrics/recent que retorna las últimas métricas de un
     * dispositivo.
     * 
     * @param vertx       Instancia de Vert.x
     * @param testContext Contexto de prueba
     */
    @Test
    void testGetRecentMetrics_Integration_ShouldReturn200(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        when(mongoRepo.getRecentData(anyString(), anyInt()))
                .thenReturn(Future.succeededFuture(List.of(new JsonObject().put("temperatura", 25.0))));

        // Act & Assert
        webClient.get(server.actualPort(), "localhost", "/metrics/recent?deviceId=esp32_1")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    assertNotNull(response.bodyAsJsonObject().getJsonArray("data"));
                    testContext.completeNow();
                })));
    }
}
