package com.solarflow.web.handler;

import com.solarflow.model.ApiResponse;
import io.vertx.ext.web.RoutingContext;

public class HealthHandler {

    public void handle(RoutingContext ctx) {
        // ctx.json() automáticamente hace el Json.encode() y pone el Header "Content-Type"
        ctx.json(ApiResponse.success("Ingestion Gateway is healthy ☀️", null));
    }
}


