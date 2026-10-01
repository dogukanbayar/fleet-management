package com.filo.fleet.exception;

import org.springframework.http.HttpStatus;

public class DriverNotAvailableException extends ApiException {

    public static final String CODE = "DRIVER_NOT_AVAILABLE";

    public DriverNotAvailableException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, CODE, message);
    }
}
