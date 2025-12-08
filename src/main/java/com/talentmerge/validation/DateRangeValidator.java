package com.talentmerge.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {
    
    private String startDateFieldName;
    private String endDateFieldName;
    
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startDateFieldName = constraintAnnotation.startDateField();
        this.endDateFieldName = constraintAnnotation.endDateField();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let other validators handle null checks
        }
        
        try {
            Field startDateField = value.getClass().getDeclaredField(startDateFieldName);
            Field endDateField = value.getClass().getDeclaredField(endDateFieldName);
            
            startDateField.setAccessible(true);
            endDateField.setAccessible(true);
            
            LocalDate startDate = (LocalDate) startDateField.get(value);
            LocalDate endDate = (LocalDate) endDateField.get(value);
            
            // If either date is null, skip validation (handled by other validators)
            if (startDate == null || endDate == null) {
                return true;
            }
            
            // End date must be after or equal to start date
            return !endDate.isBefore(startDate);
            
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If reflection fails, assume valid to avoid breaking the application
            return true;
        }
    }
}