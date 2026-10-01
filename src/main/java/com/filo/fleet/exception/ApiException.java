package com.filo.fleet.exception;

import org.springframework.http.HttpStatus;

/** Uygulamanın tüm iş kuralı hatalarının ortak atası. HTTP durumu ve makine-okunur hata kodu taşır. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
