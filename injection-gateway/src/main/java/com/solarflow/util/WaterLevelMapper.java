package com.solarflow.util;

/**
 * Convierte el índice entero 0-4 del ESP32 a porcentaje legible.
 * Útil para respuestas HTTP, logs y métricas de negocio.
 */

public final class WaterLevelMapper {
    private static final String[] LEVELS = { "0%", "25%", "50%", "75%", "100%" };

    public static String toPercentage(int level) {
        return isValid(level) ? LEVELS[level] : "Invalid";
    }

    public static boolean isValid(int level) {
        return level >= Constants.WATER_LEVEL_MIN && level <= Constants.WATER_LEVEL_MAX;
    }
}
