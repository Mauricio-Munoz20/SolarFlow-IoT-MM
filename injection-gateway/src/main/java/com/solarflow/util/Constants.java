package com.solarflow.util;

/**
 * Constantes derivadas EXCLUSIVAMENTE del PDF del Taller 5 + reglas de negocio
 * configurables.
 * No se inventan rangos de sensores no especificados.
 */
public final class Constants {
    private Constants() {
        /* Utility class */ }

    public static final int WATER_LEVEL_MIN = 0;
    public static final int WATER_LEVEL_MAX = 4;

    public static final double DEFAULT_ALERT_TEMP_THRESHOLD = 60.0; // X configurable
    public static final int DEFAULT_ALERT_WATER_THRESHOLD = 1; // <1 = "bajo" (25%)

    // Headers & Caché
    public static final String HEADER_CONTENT_TYPE = "application/json";
    public static final String HEADER_API_KEY = "X-API-Key";
    public static final String CACHE_SENSOR_PREFIX = "sensor:config:";
}
