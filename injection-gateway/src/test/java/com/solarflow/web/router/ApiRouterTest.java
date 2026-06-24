package com.solarflow.web.router;

import com.solarflow.config.AppConfig;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class ApiRouterTest {

    @Mock private MongoRepository mongoRepo;
    @Mock private InfluxRepository influxRepo;

    private HttpServer server;
    private WebClient webClient;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        // Configuramos el router simulando las dependencias
        Router router = ApiRouter.create(vertx, AppConfig.fromJsonObject(new io.vertx.core.json.JsonObject()), mongoRepo, influxRepo);

        // Levantamos un servidor en un puerto aleatorio (0) para aislar el test
        server = vertx.createHttpServer()
                .requestHandler(router)
                .listen(0, testContext.succeeding(s -> {
                    webClient = WebClient.create(vertx);
                    testContext.completeNow();
                }));
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        server.close(testContext.succeedingThenComplete());
    }

    @Test
    void testHealthEndpoint_ShouldReturn200OK(Vertx vertx, VertxTestContext testContext) {
        webClient.get(server.actualPort(), "localhost", "/health")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(200, response.statusCode());
                    assertEquals("success", response.bodyAsJsonObject().getString("status"));
                    testContext.completeNow();
                })));
    }

    @Test
    void testUnknownRoute_ShouldReturn404(Vertx vertx, VertxTestContext testContext) {
        webClient.get(server.actualPort(), "localhost", "/ruta-que-no-existe")
                .send(testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(404, response.statusCode());
                    testContext.completeNow();
                })));
    }
}
