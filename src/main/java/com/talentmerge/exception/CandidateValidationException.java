package com.talentmerge.exception;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class CandidateValidationException extends RuntimeException {
    
    private final Map<String, List<String>> fieldErrors;
    
    public CandidateValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }
    
    public CandidateValidationException(String message) {
        super(message);
        this.fieldErrors = Map.of();
    }
}