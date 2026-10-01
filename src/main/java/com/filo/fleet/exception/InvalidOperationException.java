package com.filo.fleet.exception;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends ApiException {

    public static final String CODE = "INVALID_OPERATION";

    public InvalidOperationException(String message) {
        super(HttpStatus.CONFLICT, CODE, message);
    }
}
