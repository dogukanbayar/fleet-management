package com.filo.fleet.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ApiException {

    public static final String CODE = "DUPLICATE_RESOURCE";

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, CODE, message);
    }
}
