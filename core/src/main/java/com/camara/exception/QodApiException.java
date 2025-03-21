/*-
 * ---license-start
 * CAMARA Project
 * ---
 * Copyright (C) 2022 - 2025 Contributors | Deutsche Telekom AG to CAMARA a Series of LF Projects, LLC
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

package com.camara.exception;

import lombok.Generated;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

/**
 * A QodApiException is raised during runtime if something went wrong (e.g., invalid request parameters, error from NEF, ...).
 */
@Generated
@Getter
@ToString
public class QodApiException extends RuntimeException {

  private final HttpStatus httpStatus;
  private final String errorCode;

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may subsequently be initialized
   * by a call to {@link #initCause}.
   *
   * @param httpStatus the {@link HttpStatus}
   * @param message    the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
   */
  public QodApiException(HttpStatus httpStatus, String message) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = httpStatus.name();
  }

  /**
   * Constructs a new runtime exception with the specified detail message and an {@link ErrorCode}.
   *
   * @param httpStatus the {@link HttpStatus}
   * @param message    the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
   * @param errorCode  {@link ErrorCode}
   */
  public QodApiException(HttpStatus httpStatus, String message, ErrorCode errorCode) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode.name();
  }

  /**
   * Constructs a new runtime exception with the specified detail message and {@link ErrorCode} as string.
   *
   * @param httpStatus the {@link HttpStatus}
   * @param message    the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
   * @param errorCode  {@link String}
   */
  public QodApiException(HttpStatus httpStatus, String message, String errorCode) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }
}
