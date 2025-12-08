package com.talentmerge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnonymizationServiceTest {

    private AnonymizationService anonymizationService;
    private PersonalInfoDetectionService personalInfoDetectionService;

    @BeforeEach
    void setUp() {
        personalInfoDetectionService = new PersonalInfoDetectionService();
        anonymizationService = new AnonymizationService(personalInfoDetectionService);
    }

    @Test
    @DisplayName("Should replace personal info with placeholders")
    void testAnonymize() {
        String resumeText = "John Doe\n" +
                            "Software Engineer\n" +
                            "john.doe@example.com\n" +
                            "+1 123 456 7890\n" +
                            "linkedin.com/in/johndoe";

        String anonymizedText = anonymizationService.anonymize(resumeText);

        assertTrue(anonymizedText.contains("[NAME]"));
        assertTrue(anonymizedText.contains("[EMAIL]"));
        assertTrue(anonymizedText.contains("[PHONE]"));
        assertTrue(anonymizedText.contains("[LINKEDIN]"));

        assertFalse(anonymizedText.contains("John Doe"));
        assertFalse(anonymizedText.contains("john.doe@example.com"));
        assertFalse(anonymizedText.contains("+1 123 456 7890"));
        assertFalse(anonymizedText.contains("linkedin.com/in/johndoe"));
    }
}
