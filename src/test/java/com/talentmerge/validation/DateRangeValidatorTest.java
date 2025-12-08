package com.talentmerge.validation;

import com.talentmerge.dto.WorkExperienceCreateDTO;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DateRangeValidatorTest {

    private DateRangeValidator validator;

    @Mock
    private ValidDateRange constraintAnnotation;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new DateRangeValidator();
        when(constraintAnnotation.startDateField()).thenReturn("startDate");
        when(constraintAnnotation.endDateField()).thenReturn("endDate");
        validator.initialize(constraintAnnotation);
    }

    @Test
    void isValid_WithNullValue_ShouldReturnTrue() {
        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithValidDateRange_ShouldReturnTrue() {
        // Given
        WorkExperienceCreateDTO workExp = new WorkExperienceCreateDTO();
        workExp.setStartDate(LocalDate.of(2020, 1, 1));
        workExp.setEndDate(LocalDate.of(2021, 1, 1));

        // When
        boolean result = validator.isValid(workExp, context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithSameDates_ShouldReturnTrue() {
        // Given
        WorkExperienceCreateDTO workExp = new WorkExperienceCreateDTO();
        LocalDate sameDate = LocalDate.of(2020, 1, 1);
        workExp.setStartDate(sameDate);
        workExp.setEndDate(sameDate);

        // When
        boolean result = validator.isValid(workExp, context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithEndDateBeforeStartDate_ShouldReturnFalse() {
        // Given
        WorkExperienceCreateDTO workExp = new WorkExperienceCreateDTO();
        workExp.setStartDate(LocalDate.of(2021, 1, 1));
        workExp.setEndDate(LocalDate.of(2020, 1, 1)); // End before start

        // When
        boolean result = validator.isValid(workExp, context);

        // Then
        assertFalse(result);
    }

    @Test
    void isValid_WithNullStartDate_ShouldReturnTrue() {
        // Given
        WorkExperienceCreateDTO workExp = new WorkExperienceCreateDTO();
        workExp.setStartDate(null);
        workExp.setEndDate(LocalDate.of(2021, 1, 1));

        // When
        boolean result = validator.isValid(workExp, context);

        // Then
        assertTrue(result); // Should skip validation if either date is null
    }

    @Test
    void isValid_WithNullEndDate_ShouldReturnTrue() {
        // Given
        WorkExperienceCreateDTO workExp = new WorkExperienceCreateDTO();
        workExp.setStartDate(LocalDate.of(2020, 1, 1));
        workExp.setEndDate(null); // Current position

        // When
        boolean result = validator.isValid(workExp, context);

        // Then
        assertTrue(result); // Should skip validation for current positions
    }

    @Test
    void isValid_WithBothDatesNull_ShouldReturnTrue() {
        // Given
        WorkExperienceCreateDTO workExp = new WorkExperienceCreateDTO();
        workExp.setStartDate(null);
        workExp.setEndDate(null);

        // When
        boolean result = validator.isValid(workExp, context);

        // Then
        assertTrue(result);
    }
}