package com.solarflow.integration;

import com.solarflow.config.AppConfig;
import com.solarflow.model.SensorData;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import com.solarflow.web.router.ApiRouter;
import io.vertx.core.Future;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class SensorDataIntegrationTest {

    @Mock private MongoRepository mongoRepo;
    @Mock private InfluxRepository influxRepo;

    private HttpServer server;
    private WebClient webClient;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext testContext) {
        Router router = ApiRouter.create(vertx, AppConfig.fromJsonObject(new io.vertx.core.json.JsonObject()), mongoRepo, influxRepo);
        // Desplegar en puerto dinámico para evitar conflictos si tienes el gateway encendido
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
    void testPostSensorData_Integration_ShouldReturn202(Vertx vertx, VertxTestContext testContext) {
        // Arrange
        SensorData validData = new SensorData("esp32_1", 25.5, 60.0, 2, System.currentTimeMillis());
        when(mongoRepo.saveSensorData(any())).thenReturn(Future.succeededFuture());
        when(influxRepo.writeMetrics(any())).thenReturn(Future.succeededFuture());

        // Act & Assert (Se simula un cliente HTTP real llamando a la API)
        webClient.post(server.actualPort(), "localhost", "/sensor-data")
                .sendJson(validData, testContext.succeeding(response -> testContext.verify(() -> {
                    assertEquals(202, response.statusCode());
                    assertEquals("Datos ingeridos correctamente", response.bodyAsJsonObject().getString("message"));
                    testContext.completeNow();
                })));
    }
}
