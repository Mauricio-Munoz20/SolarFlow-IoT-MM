package com.solarflow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Alert(
        String alertaId,
        String deviceId,
        AlertType type,
        AlertSeverity severity,
        // Valor que dispara la alarma
        Double triggerValue,
        // Umbral configurado
        Double threshold,
        String message,
        long timestamp

) {
    public enum AlertType {
        HIGH_TEMPERATURE,
        LOW_WATER_LEVEL,
        HIGH_HUMIDITY,
        SENSOR_OFFLINE
    }

    public enum AlertSeverity {
        WARNING,
        CRITICAL
    }

    // Uso de Factory Method para generar UUID y timestamp automaticamente.
    public static Alert create(String deviceId, AlertType type, AlertSeverity severity, Double triggerValue,
            Double threshold, String message) {
        return new Alert(UUID.randomUUID().toString(), deviceId, type, severity, triggerValue, threshold, message,
                Instant.now().toEpochMilli());
    }
}
