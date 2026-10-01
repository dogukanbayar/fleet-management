package com.filo.fleet.exception;

import org.springframework.http.HttpStatus;

public class InvalidKilometerUpdateException extends ApiException {

    public static final String CODE = "INVALID_KILOMETER_UPDATE";

    public InvalidKilometerUpdateException(String message) {
        super(HttpStatus.BAD_REQUEST, CODE, message);
    }
}
