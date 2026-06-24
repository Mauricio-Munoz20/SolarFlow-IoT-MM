package com.solarflow.web.handler;

import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsHandlerTest {

    @Mock
    private MongoRepository mongoRepo;
    @Mock
    private InfluxRepository influxRepo;
    @Mock
    private RoutingContext ctx;

    private MetricsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MetricsHandler(mongoRepo, influxRepo);
    }

    @Test
    void getRecent_ShouldReturn200_WhenMongoSucceeds() {
        // Arrange
        when(ctx.queryParam("deviceId")).thenReturn(List.of("esp32_1"));
        when(mongoRepo.getRecentData("esp32_1", 10))
                .thenReturn(Future.succeededFuture(List.of(new JsonObject().put("temperatura", 25.0))));

        handler.getRecent(ctx);

        verify(mongoRepo).getRecentData("esp32_1", 10);
        verify(ctx).json(any()); // Comprueba que intentó devolver el JSON al usuario
    }

    @Test
    void getRecent_ShouldReturn500_WhenMongoFails() {
        // Arrange
        when(ctx.queryParam("deviceId")).thenReturn(List.of("esp32_1"));
        Exception dbError = new RuntimeException("Mongo explotó");
        when(mongoRepo.getRecentData("esp32_1", 10)).thenReturn(Future.failedFuture(dbError));

        handler.getRecent(ctx);

        verify(ctx).fail(eq(500), eq(dbError)); // Verifica que devuelve error 500
        verify(ctx, never()).json(any()); // Nunca debe enviar un JSON de éxito
    }

    @Test
    void getHourlyAverage_ShouldReturn200_WhenInfluxSucceeds() {
        // Arrange
        when(ctx.queryParam("deviceId")).thenReturn(List.of("esp32_1"));
        JsonObject fakeInfluxResult = new JsonObject().put("avg_temp", 26.5);
        when(influxRepo.getHourlyAverage("esp32_1")).thenReturn(Future.succeededFuture(fakeInfluxResult));

        handler.getHourlyAverage(ctx);

        verify(influxRepo).getHourlyAverage("esp32_1");
        verify(ctx).json(any());
    }
}
