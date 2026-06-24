package com.solarflow.exception;

import java.io.Serial;

/**
 * Excepción de negocio lanzada cuando un payload no cumple las reglas de validación.
 * Incluye el código HTTP correspondiente para mapeo automático en el handler global.
 */

public class ValidationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int statusCode;

    public ValidationException(String message) {
        super(message);
        this.statusCode = 400;
    }

    public ValidationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 400;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

