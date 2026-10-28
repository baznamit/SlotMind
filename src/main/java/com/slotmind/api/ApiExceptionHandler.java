package com.slotmind.api;

import com.slotmind.service.ConflictException;
import com.slotmind.service.NotFoundException;
import com.slotmind.service.NoEligibleSlotException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    record ErrorResponse(Instant timestamp, int status, String error, String message) { }
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException ex) { return response(HttpStatus.NOT_FOUND, "The requested resource was not found."); }
    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ErrorResponse> conflict(ConflictException ex) {
        var message = ex.getMessage() != null && ex.getMessage().contains("no longer available")
                ? "This slot has already been booked. Refresh the dashboard and reject this outdated suggestion."
                : ex.getMessage() != null && ex.getMessage().contains("no longer pending")
                ? "This suggestion is no longer pending. Refresh the dashboard."
                : "This rescheduling action conflicts with the current appointment state. Refresh the dashboard and try again.";
        return response(HttpStatus.CONFLICT, message);
    }
    @ExceptionHandler({NoEligibleSlotException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception ex) { return response(HttpStatus.BAD_REQUEST, "The request could not be completed."); }
    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }
}