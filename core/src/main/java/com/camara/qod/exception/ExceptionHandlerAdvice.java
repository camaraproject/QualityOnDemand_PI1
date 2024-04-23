/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2024 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
 *
 * The contributor of this file confirms his sign-off for the Developer Certificate of Origin
 *             (https://developercertificate.org).
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package com.camara.qod.exception;

import static com.camara.qod.exception.ErrorCode.VALIDATION_FAILED;

import com.camara.qod.api.model.ErrorInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.util.List;
import lombok.Generated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * This class handles occurred exceptions.
 */
@ControllerAdvice
@Slf4j
@Generated
public class ExceptionHandlerAdvice {

  private static final String SCHEMA_VALIDATION_FAILED_MESSAGE = "Schema validation failed at ";

  /**
   * This function handles all other exceptions.
   *
   * @param ex The generic exception {@link Exception}
   * @return HTTP response with the status 500 - Internal Server Error
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorInfo> handleException(Exception ex) {
    log.error("Unhandled exception occurred: ", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorInfo()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("Internal Server Error"));
  }

  /**
   * Method for handling MethodArgumentTypeMismatchException.
   *
   * @param ex the core exception with a message
   * @return the response with Bad Request
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorInfo> handleException(
      MethodArgumentTypeMismatchException ex) {
    String error;
    Class<?> requiredType = ex.getRequiredType();
    if (requiredType != null) {
      error =
          ex.getName() + " should be of type " + requiredType.getSimpleName();
    } else {
      error = ex.getMessage();
    }
    return ResponseEntity.badRequest().body(
        new ErrorInfo()
            .status(HttpStatus.BAD_REQUEST.value())
            .code(VALIDATION_FAILED.name())
            .message(error));
  }

  /**
   * This function handles EventProducerException exceptions.
   *
   * @param ex The generic exception {@link EventProducerException}
   * @return HTTP response with the status 500 - Internal Server Error
   */
  @ExceptionHandler(EventProducerException.class)
  public ResponseEntity<HttpStatus> handleException(EventProducerException ex) {
    log.error("EventProducerException occurred: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
  }

  /**
   * This function handles {@link QodApiException}.
   *
   * @param ex The internal API exception {@link QodApiException}
   * @return HTTP response with the status defined in {@code ex}
   */
  @ExceptionHandler(QodApiException.class)
  public ResponseEntity<ErrorInfo> handleException(QodApiException ex) {
    log.error("API exception raised: {}", ex.getMessage());
    return ResponseEntity.status(ex.getHttpStatus())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo()
            .status(ex.getHttpStatus().value())
            .code(ex.getErrorCode())
            .message(ex.getMessage()));
  }

  /**
   * This function handles {@link MethodArgumentNotValidException}.
   *
   * @param ex The exception {@link MethodArgumentNotValidException}, that is caused by Spring validations (e.g., parameter outside allowed
   *           range)
   * @return HTTP response with the status 400 - Bad Request
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorInfo> handleException(MethodArgumentNotValidException ex) {
    log.error("MethodArgumentNotValidException raised: {}", ex.getMessage());

    FieldError fe = ex.getBindingResult().getFieldError();
    String field = "<unknown>";
    if (fe != null) {
      field = fe.getField();
    }
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo()
            .status(HttpStatus.BAD_REQUEST.value())
            .code(ex.toString().contains("rejected value [null]")
                ? ErrorCode.PARAMETER_MISSING.name()
                : VALIDATION_FAILED.name())
            .message("Validation failed for parameter '" + field + "'"));
  }

  /**
   * This function handles {@link ValueInstantiationException}.
   *
   * @param ex The exception {@link ValueInstantiationException}, that is caused by Spring validations (e.g., invalid enum value)
   * @return HTTP response with the status 400 - Bad Request
   */
  @ExceptionHandler(ValueInstantiationException.class)
  public ResponseEntity<ErrorInfo> handleException(ValueInstantiationException ex) {
    log.error("ValueInstantiationException raised: {}", ex.getMessage());
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo()
            .status(HttpStatus.BAD_REQUEST.value())
            .code(VALIDATION_FAILED.name())
            .message("Validation failed for parameter '" + ex.getPath().get(0).getFieldName())
        );
  }

  /**
   * This function handles {@link InvalidFormatException}.
   *
   * @param ex The exception {@link ValueInstantiationException}, that is caused by Spring validations (e.g., passing a wrong type)
   * @return HTTP response with the status 400 - Bad Request
   */
  @ExceptionHandler(InvalidFormatException.class)
  public ResponseEntity<ErrorInfo> handleException(InvalidFormatException ex) {
    log.error("InvalidFormatException raised: {}", ex.getMessage());
    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(
        new ErrorInfo()
            .status(HttpStatus.BAD_REQUEST.value())
            .code(ErrorCode.INVALID_INPUT.name())
            .message("Required: " + ex.getTargetType().getSimpleName() + ", provided: '" + ex.getValue().toString() + "'"));
  }

  /**
   * This function handles {@link HttpMessageNotReadableException}.
   *
   * @param ex The exception {@link HttpMessageNotReadableException}
   * @return HTTP response with the status 400 - Bad Request
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorInfo> handleException(HttpMessageNotReadableException ex) {
    Throwable throwable = ex.getCause();
    log.error("HttpMessageNotReadableException raised: {}", ex.getMessage());
    String errorMessage = ex.getMessage();
    if (throwable instanceof JsonEOFException) {
      errorMessage = "Invalid json: start '{' or end-braces '}'";
    } else if (throwable instanceof JsonParseException jsonParseException) {
      errorMessage = jsonParseException.getOriginalMessage();
    } else if (throwable instanceof UnrecognizedPropertyException unrecognizedPropertyException) {
      errorMessage = unrecognizedPropertyException.getMessage();
      List<Reference> jsonReferences = unrecognizedPropertyException.getPath();
      if (!jsonReferences.isEmpty()) {
        String invalidField = jsonReferences.get(0).getFieldName();
        errorMessage = "The field '" + invalidField + "' is an unsupported parameter.";
      }
    } else if (throwable instanceof JsonMappingException jsonMappingException) {
      List<Reference> jsonReferences = jsonMappingException.getPath();
      if (!jsonReferences.isEmpty()) {
        String invalidField = jsonReferences.get(0).getFieldName();
        errorMessage = SCHEMA_VALIDATION_FAILED_MESSAGE + invalidField;
      }
    } else {
      errorMessage = "Undefined HTTP Message not readable exception" + errorMessage;
    }
    return ResponseEntity.badRequest().body(
        new ErrorInfo()
            .status(HttpStatus.BAD_REQUEST.value())
            .code(VALIDATION_FAILED.name())
            .message(errorMessage));
  }

  /**
   * This function handles {@link HttpMediaTypeNotAcceptableException}.
   *
   * @param ex The exception {@link HttpMediaTypeNotAcceptableException}.
   * @return HTTP response with status 406 - Not Acceptable
   */
  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<ErrorInfo> handleException(HttpMediaTypeNotAcceptableException ex) {
    log.error("Unsupported media-type exception occurred: {}", ex.getMessage());
    return ResponseEntity.status(ex.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo()
            .status(ex.getStatusCode().value())
            .code(ErrorCode.INVALID_INPUT.name())
            .message(ex.getMessage() + ". Supported media types: " + ex.getSupportedMediaTypes()));
  }

  /**
   * This function handles {@link HttpMediaTypeNotSupportedException}.
   *
   * @param ex The exception {@link HttpMediaTypeNotSupportedException}.
   * @return HTTP response with status 415 - Unsupported Media Type
   */
  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ErrorInfo> handleException(HttpMediaTypeNotSupportedException ex) {
    log.error("Unsupported media-type exception occurred: {}", ex.getMessage());
    return ResponseEntity.status(ex.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo()
            .status(ex.getStatusCode().value())
            .code(ErrorCode.INVALID_INPUT.name())
            .message(ex.getMessage()));
  }

  /**
   * This function handles {@link HttpRequestMethodNotSupportedException}.
   *
   * @param ex The exception {@link HttpRequestMethodNotSupportedException}
   * @return HTTP response with status 405 - Method Not Allowed
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ErrorInfo> handleException(HttpRequestMethodNotSupportedException ex) {
    log.error("Method {} is not allowed for this endpoint: {}", ex.getMethod(), ex.getMessage());
    return ResponseEntity.status(ex.getStatusCode())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo()
            .status(ex.getStatusCode().value())
            .code(ErrorCode.NOT_ALLOWED.name())
            .message(ex.getMessage()));
  }

  /**
   * This function handles {@link NoResourceFoundException}.
   *
   * @param ex The exception {@link NoResourceFoundException}
   * @return ex The exception with HTTP response with the status 404 - Not Found
   */
  @SneakyThrows
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<Object> handleException(NoResourceFoundException ex) {
    log.debug("No resource found exception occurred: {}", ex.getMessage());
    throw ex;
  }
}

