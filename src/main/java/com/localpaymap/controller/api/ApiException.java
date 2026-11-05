package com.localpaymap.controller.api;

import org.springframework.http.HttpStatus;

/** HTTP 상태코드를 함께 전달하는 API 오류. ApiExceptionHandler가 {"message": ...} 형태로 응답한다. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
