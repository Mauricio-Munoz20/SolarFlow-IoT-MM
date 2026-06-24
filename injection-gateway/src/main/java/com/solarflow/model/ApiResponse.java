package com.solarflow.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)

// El <T> significa que esta clase es genérica, es decir, que puede recibir cualquier tipo de dato.

public record ApiResponse<T>(
    String status, // success o error
    String message, // Mensaje descriptivo
    T data, // Datos relevantes
    Long timestamp // Fecha de la respuesta

) {
    // Constructor Factory para crear respuestas exitosas
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data, Instant.now().toEpochMilli());
    }
    // Sobrecarga para respuestas de error
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null, Instant.now().toEpochMilli());
    }
    // Sobrecarga para respuestas de error con datos
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>("error", message, data, Instant.now().toEpochMilli());
    }
}
