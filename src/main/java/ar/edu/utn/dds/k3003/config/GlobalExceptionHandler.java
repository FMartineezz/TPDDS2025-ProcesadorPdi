package ar.edu.utn.dds.k3003.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.security.InvalidParameterException;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice(basePackages = "ar.edu.utn.dds.k3003.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElementException(NoSuchElementException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Not Found");
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<Map<String, String>> handleInvalidParameterException(InvalidParameterException e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Bad Request");
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        Map<String, String> response = new HashMap<>();
        response.put("error", "Internal Server Error");
        response.put("message", "An unexpected error occurred");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,String>> handleRSE(ResponseStatusException e) {
        Map<String,String> body = new HashMap<>();
        body.put("error", e.getStatusCode().toString());
        body.put("message", e.getReason() != null ? e.getReason() : e.getMessage());
        return new ResponseEntity<>(body, e.getStatusCode());
    }

    @ExceptionHandler(IllegalStateException.class) // por si en algún lado siguen tirando esta
    public ResponseEntity<Map<String,String>> handleIllegalState(IllegalStateException e) {
        Map<String,String> body = new HashMap<>();
        body.put("error", "CONFLICT");
        body.put("message", e.getMessage());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT); // 409
    }
}