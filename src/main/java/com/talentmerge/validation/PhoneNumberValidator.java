package com.talentmerge.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    // Pattern for international phone numbers (flexible format)
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[1-9]\\d{1,14}$|^[+]?[(]?[\\d\\s\\-().]{7,20}$"
    );
    
    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return true; // Allow null/empty (handled by @NotBlank if required)
        }
        
        // Remove common formatting characters for validation
        String cleanNumber = phoneNumber.replaceAll("[\\s\\-().+]", "");
        
        // Basic checks
        if (cleanNumber.length() < 7 || cleanNumber.length() > 15) {
            return false;
        }
        
        // Must contain only digits after cleaning
        return cleanNumber.matches("\\d+");
    }
}