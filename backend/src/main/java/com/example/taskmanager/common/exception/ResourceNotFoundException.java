package com.example.taskmanager.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

  public ResourceNotFoundException(String resource, Object id) {
    super(HttpStatus.NOT_FOUND, "%s with id %s not found".formatted(resource, id));
  }
}
