package com.talentmerge.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String errorCode;
    private String message;
    private Map<String, List<String>> fieldErrors;
    private LocalDateTime timestamp;
    private int status;
    private String path;
    
    // Constructor for validation errors
    public ErrorResponse(String errorCode, String message, Map<String, List<String>> fieldErrors) {
        this.errorCode = errorCode;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.timestamp = LocalDateTime.now();
    }
    
    // Legacy constructor for backward compatibility
    public ErrorResponse(int status, String error, String message, String path) {
        this.errorCode = error;
        this.message = message;
        this.fieldErrors = Collections.emptyMap();
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.path = path;
    }
    
    // Simple constructor for basic errors
    public ErrorResponse(String error) {
        this.errorCode = "GENERIC_ERROR";
        this.message = error;
        this.fieldErrors = Collections.emptyMap();
        this.timestamp = LocalDateTime.now();
    }
}