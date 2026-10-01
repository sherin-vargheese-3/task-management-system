package com.example.taskmanager.common.exception;

import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.TransactionException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String SQL_STATE_CONNECTION_EXCEPTION = "08";
  private static final String SQL_STATE_SERVER_SHUTDOWN = "57P";

  @ExceptionHandler(ApiException.class)
  public ProblemDetail handleApiException(ApiException ex) {
    return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName()));
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ProblemDetail handleInvalidSortProperty(PropertyReferenceException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Unknown sort property '%s'".formatted(ex.getPropertyName()));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The resource was modified concurrently, reload and retry");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
    log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getClass().getSimpleName());
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "The request conflicts with existing data");
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    if (isDatabaseUnavailable(ex)) {
      log.error("Database unavailable: {}", NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
      return ProblemDetail.forStatusAndDetail(
          HttpStatus.SERVICE_UNAVAILABLE, "The database is unavailable, please try again shortly");
    }
    log.error("Unhandled exception", ex);
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  static boolean isDatabaseUnavailable(Throwable ex) {
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      if (cause instanceof CannotCreateTransactionException
          || cause instanceof SQLTransientConnectionException
          || isTransactionLostWithConnection(cause)) {
        return true;
      }
      if (cause instanceof SQLException sql && isConnectionFailureState(sql.getSQLState())) {
        return true;
      }
    }
    return false;
  }

  private static boolean isTransactionLostWithConnection(Throwable cause) {
    return cause instanceof TransactionException && cause.getCause() instanceof SQLException;
  }

  private static boolean isConnectionFailureState(String sqlState) {
    return sqlState != null
        && (sqlState.startsWith(SQL_STATE_CONNECTION_EXCEPTION)
            || sqlState.startsWith(SQL_STATE_SERVER_SHUTDOWN));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Map<String, String>> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> fieldError(error.getField(), error.getDefaultMessage()))
            .toList();
    return ResponseEntity.badRequest().body(validationProblem(fieldErrors));
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<Map<String, String>> fieldErrors =
        ex.getParameterValidationResults().stream().flatMap(this::toFieldErrors).toList();
    return ResponseEntity.badRequest().body(validationProblem(fieldErrors));
  }

  private Stream<Map<String, String>> toFieldErrors(ParameterValidationResult result) {
    if (result instanceof ParameterErrors errors) {
      return errors.getFieldErrors().stream()
          .map(error -> fieldError(error.getField(), error.getDefaultMessage()));
    }
    String parameterName = result.getMethodParameter().getParameterName();
    return result.getResolvableErrors().stream()
        .map(error -> fieldError(parameterName, error.getDefaultMessage()));
  }

  private static Map<String, String> fieldError(String field, String message) {
    return Map.of("field", String.valueOf(field), "message", String.valueOf(message));
  }

  private static ProblemDetail validationProblem(List<Map<String, String>> fieldErrors) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
    body.setProperty("errors", fieldErrors);
    return body;
  }
}
