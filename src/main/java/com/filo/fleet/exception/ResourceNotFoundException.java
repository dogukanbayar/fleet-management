package com.filo.fleet.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public static final String CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, CODE, resource + " bulunamadı. (id: " + id + ")");
    }
}
