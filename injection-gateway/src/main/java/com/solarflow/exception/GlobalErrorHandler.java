package com.solarflow.exception;

import com.solarflow.model.ApiResponse;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FailureHandler global de Vert.x. Captura excepciones no manejadas y devuelve
 * JSON limpio.
 * Debe registrarse en el router: router.route().failureHandler(new
 * GlobalErrorHandler());
 */

public class GlobalErrorHandler implements Handler<RoutingContext> {
    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @Override
    public void handle(RoutingContext ctx) {
        Throwable failure = ctx.failure();
        int statusCode = ctx.statusCode() > 0 ? ctx.statusCode() : 500;
        if (ctx.response().ended()) {
            return; //Como se realizo la respuesta, realmente no hace nada
        }
        if (failure instanceof ValidationException ve) {
            statusCode = ve.getStatusCode();
            log.warn("Validación fallida: {}", ve.getMessage());
            respond(ctx, statusCode, ApiResponse.error(ve.getMessage()));
        } else if (failure != null){
            log.error("Error interno no manejado: {}", failure.getMessage());
            respond(ctx, statusCode, ApiResponse.error("Internal server error"));
        } else {
            // ctx.fail(404) sin excepción — respetamos el código pedido
            log.warn(" Fallo HTTP sin excepción. Código: {}", statusCode);
            respond(ctx, statusCode, ApiResponse.error(httpMessage(statusCode)));
        }
    }

    private void respond(RoutingContext ctx, int statusCode, ApiResponse<?> body) {
        ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(Json.encode(body));
    }
    private String httpMessage(int code) {
        return switch (code) {
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            default  -> "Internal server error";
        };
    }


}
