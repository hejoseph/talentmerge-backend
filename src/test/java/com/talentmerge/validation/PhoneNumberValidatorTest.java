package com.talentmerge.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PhoneNumberValidatorTest {

    private PhoneNumberValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new PhoneNumberValidator();
    }

    @Test
    void isValid_WithNullPhoneNumber_ShouldReturnTrue() {
        // When
        boolean result = validator.isValid(null, context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithEmptyPhoneNumber_ShouldReturnTrue() {
        // When
        boolean result = validator.isValid("", context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithWhitespaceOnlyPhoneNumber_ShouldReturnTrue() {
        // When
        boolean result = validator.isValid("   ", context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithValidUSPhoneNumber_ShouldReturnTrue() {
        // Given
        String[] validNumbers = {
            "+1234567890",
            "1234567890",
            "(123) 456-7890",
            "123-456-7890",
            "123.456.7890",
            "+1 (123) 456-7890"
        };

        // When & Then
        for (String phoneNumber : validNumbers) {
            boolean result = validator.isValid(phoneNumber, context);
            assertTrue(result, "Phone number should be valid: " + phoneNumber);
        }
    }

    @Test
    void isValid_WithValidInternationalPhoneNumbers_ShouldReturnTrue() {
        // Given
        String[] validNumbers = {
            "+44 20 7946 0958",  // UK
            "+33 1 42 86 83 26", // France
            "+49 30 12345678",   // Germany
            "+86 10 12345678",   // China
            "+91 98765 43210",   // India
            "+61 2 9876 5432"    // Australia
        };

        // When & Then
        for (String phoneNumber : validNumbers) {
            boolean result = validator.isValid(phoneNumber, context);
            assertTrue(result, "Phone number should be valid: " + phoneNumber);
        }
    }

    @Test
    void isValid_WithTooShortPhoneNumber_ShouldReturnFalse() {
        // Given
        String[] shortNumbers = {
            "123",
            "12345",
            "123456"
        };

        // When & Then
        for (String phoneNumber : shortNumbers) {
            boolean result = validator.isValid(phoneNumber, context);
            assertFalse(result, "Phone number should be invalid (too short): " + phoneNumber);
        }
    }

    @Test
    void isValid_WithTooLongPhoneNumber_ShouldReturnFalse() {
        // Given
        String phoneNumber = "1234567890123456"; // 16 digits

        // When
        boolean result = validator.isValid(phoneNumber, context);

        // Then
        assertFalse(result, "Phone number should be invalid (too long)");
    }

    @Test
    void isValid_WithNonNumericCharacters_ShouldReturnFalse() {
        // Given
        String[] invalidNumbers = {
            "123456789a",
            "abc1234567",
            "123-456-789x",
            "phone-number",
            "123@456#789"
        };

        // When & Then
        for (String phoneNumber : invalidNumbers) {
            boolean result = validator.isValid(phoneNumber, context);
            assertFalse(result, "Phone number should be invalid (non-numeric): " + phoneNumber);
        }
    }

    @Test
    void isValid_WithValidFormattedNumbers_ShouldReturnTrue() {
        // Given
        String[] formattedNumbers = {
            "(555) 123-4567",
            "555-123-4567",
            "555.123.4567",
            "555 123 4567",
            "+1 555 123 4567",
            "+1-555-123-4567",
            "+1.555.123.4567"
        };

        // When & Then
        for (String phoneNumber : formattedNumbers) {
            boolean result = validator.isValid(phoneNumber, context);
            assertTrue(result, "Formatted phone number should be valid: " + phoneNumber);
        }
    }

    @Test
    void isValid_WithMinimumValidLength_ShouldReturnTrue() {
        // Given
        String phoneNumber = "1234567"; // Exactly 7 digits

        // When
        boolean result = validator.isValid(phoneNumber, context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithMaximumValidLength_ShouldReturnTrue() {
        // Given
        String phoneNumber = "+123456789012345"; // 15 digits total

        // When
        boolean result = validator.isValid(phoneNumber, context);

        // Then
        assertTrue(result);
    }

    @Test
    void isValid_WithComplexFormatting_ShouldReturnTrue() {
        // Given
        String phoneNumber = "+1 (555) 123-4567 ext. 1234";

        // When
        boolean result = validator.isValid(phoneNumber, context);

        // Then
        // This should fail because "ext" contains letters
        assertFalse(result);
    }

    @Test
    void isValid_WithOnlyFormatCharacters_ShouldReturnFalse() {
        // Given
        String phoneNumber = "()-.+ ";

        // When
        boolean result = validator.isValid(phoneNumber, context);

        // Then
        assertFalse(result);
    }
}