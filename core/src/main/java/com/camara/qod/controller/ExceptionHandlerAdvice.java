/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
 * The contributor of this file confirms his sign-off for the
 * Developer Certificate of Origin (http://developercertificate.org).
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

package com.camara.qod.controller;

import com.camara.qod.api.model.ErrorInfo;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * This class handles occurred exceptions.
 */
@ControllerAdvice
public class ExceptionHandlerAdvice {

  private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerAdvice.class);

  /**
   * This function handles an occurred exception and puts the information into an HTTP response.
   *
   * @param e Exception, that is caused by our own application code
   * @return HTTP response entity with status code and error description
   */
  @ExceptionHandler(SessionApiException.class)
  public ResponseEntity<ErrorInfo> handleException(SessionApiException e) {
    log.error("Session API exception raised: ", e);
    return ResponseEntity.status(e.getHttpStatus())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo().code(e.getConstant())
            .message(e.getMessage()));
  }

  /**
   * This function handles an occurred exception and puts the information into an HTTP response.
   *
   * @param e Exception, that is caused by Spring validations (e.g. parameter outside allowed
   *          range)
   * @return HTTP response entity with status code and error description
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorInfo> handleException(MethodArgumentNotValidException e) {
    log.error("MethodArgumentNotValidException raised: ", e);

    FieldError fe = e.getBindingResult().getFieldError();
    String field = "<unknown>";
    if (fe != null) {
      field = fe.getField();
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo().code(e.toString().contains("rejected value [null]") ?
                ApplicationConstants.PARAMETER_MISSING.name() : ApplicationConstants.VALIDATION_FAILED.name())
            .message("Validation failed for parameter '" + field + "'"));
  }

  /**
   * This function handles an occurred exception and puts the information into an HTTP response.
   *
   * @param e Exception, that is caused by Spring validations (e.g. invalid enum value)
   * @return HTTP response entity with status code and error description
   */
  @ExceptionHandler(ValueInstantiationException.class)
  public ResponseEntity<ErrorInfo> handleException(ValueInstantiationException e) {
    log.error("ValueInstantiationException raised: ", e);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            new ErrorInfo().code(ApplicationConstants.VALIDATION_FAILED.name())
                .message("Schema validation failed at " + e.getPath().get(0).getFieldName()));
  }

  /**
   * This function handles an occurred exception and puts the information into an HTTP response.
   *
   * @param e Exception, that is caused by Spring validations
   *          (e.g. passing a wrong type)
   * @return HTTP response entity with status code and error description
   */
  @ExceptionHandler(InvalidFormatException.class)
  public ResponseEntity<ErrorInfo> handleException(InvalidFormatException e){
    log.error("InvalidFormatException raised: ", e);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorInfo().code(ApplicationConstants.INVALID_INPUT.name())
            .message("Required: " + e.getTargetType().getSimpleName() +
                ", provided: '" + e.getValue().toString() + "'"));
  }

  public enum ApplicationConstants {
    INVALID_INPUT, VALIDATION_FAILED, PARAMETER_MISSING, NOT_ALLOWED
  }

}
