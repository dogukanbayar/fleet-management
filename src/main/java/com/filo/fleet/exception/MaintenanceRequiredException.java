package com.filo.fleet.exception;

import org.springframework.http.HttpStatus;

public class MaintenanceRequiredException extends ApiException {

    public static final String CODE = "MAINTENANCE_REQUIRED";

    public MaintenanceRequiredException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, CODE, message);
    }
}
