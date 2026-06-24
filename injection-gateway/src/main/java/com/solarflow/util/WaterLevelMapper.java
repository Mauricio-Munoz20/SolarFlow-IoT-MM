package com.solarflow.util;

/**
 * Convierte el índice entero 0-4 del ESP32 a porcentaje legible.
 * Útil para respuestas HTTP, logs y métricas de negocio.
 */

public final class WaterLevelMapper {
    /* Utility class */

    // Usamos un Array en lugar de un Map. Es más rápido en memoria y acceso
    // ya que los índices del array coinciden exactamente con los números del ESP32.

    private static final String[] LEVELS = { "0%", "25%", "50%", "75%", "100%" };

    /**
     * Convierte el índice numerico (0-4) del ESP32 a un String legible (0% - 100%).
     */
    public static String toPercentage(int level) {
        return isValid(level) ? LEVELS[level] : "Invalid";
    }

    /**
     * Valida que el nivel esté dentro del rango esperado (0-4).
     */
    public static boolean isValid(int level) {
        return level >= Constants.WATER_LEVEL_MIN && level <= Constants.WATER_LEVEL_MAX;
    }
}
