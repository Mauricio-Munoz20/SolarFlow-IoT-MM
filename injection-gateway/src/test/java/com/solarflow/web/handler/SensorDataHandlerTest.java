package com.solarflow.web.handler;

import com.solarflow.exception.ValidationException;
import com.solarflow.model.SensorData;
import com.solarflow.repository.InfluxRepository;
import com.solarflow.repository.MongoRepository;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class })
class SensorDataHandlerTest {
    @Mock
    private MongoRepository mongoRepo;
    @Mock
    private InfluxRepository influxRepo;
    @Mock
    private RoutingContext ctx;
    @Mock
    private RequestBody requestBody;
    @Mock
    private HttpServerResponse response;

    private SensorDataHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SensorDataHandler(mongoRepo, influxRepo);
    }

    @Test
    void handle_ValidPayLoad_ShouldReturn202() {
        SensorData validData = new SensorData("esp32_1", 25.5, 60.0, 2, System.currentTimeMillis());
        String json = Json.encodePrettily(validData);

        when(ctx.body()).thenReturn(requestBody);
        when(requestBody.asString()).thenReturn(json);

        // Mover los mocks de response acá (porque solo este test los usa)
        when(ctx.response()).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.end(anyString())).thenReturn(Future.succeededFuture());

        when(mongoRepo.saveSensorData(any())).thenReturn(Future.succeededFuture());
        when(influxRepo.writeMetrics(any())).thenReturn(Future.succeededFuture());

        handler.handle(ctx);

        verify(response).setStatusCode(202);
        verify(response).end(anyString());
        verify(mongoRepo).saveSensorData(any());
        verify(influxRepo).writeMetrics(any());
    }

    @Test
    void handle_InvalidWaterLevel_ShouldFailWith400() {
        SensorData invalidData = new SensorData("esp32_1", 25.5, 60.0, 5, System.currentTimeMillis());
        String json = Json.encodePrettily(invalidData);

        when(ctx.body()).thenReturn(requestBody);
        when(requestBody.asString()).thenReturn(json);

        handler.handle(ctx);

        verify(ctx).fail(any(ValidationException.class));
        verify(mongoRepo, never()).saveSensorData(any());

    }

}
