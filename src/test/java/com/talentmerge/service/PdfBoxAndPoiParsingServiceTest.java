package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfBoxAndPoiParsingServiceTest {

    private PdfBoxAndPoiParsingService parsingService;

    @BeforeEach
    void setUp() {
        parsingService = new PdfBoxAndPoiParsingService();
    }

    @Test
    @DisplayName("Should extract international phone with spaces")
    void testParseCandidateFromText_internationalPhone() {
        String text = "Contact: +33 6 95 89 58 91";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("+33 6 95 89 58 91", candidate.getPhone());
    }

    @Test
    @DisplayName("Should extract local phone with spaces")
    void testParseCandidateFromText_localPhoneWithSpaces() {
        String text = "Call me at 06 95 89 58 91";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("06 95 89 58 91", candidate.getPhone());
    }

    @Test
    @DisplayName("Should extract phone with continuous digits")
    void testParseCandidateFromText_continuousDigits() {
        String text = "My number is 0612312345.";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("0612312345", candidate.getPhone());
    }

    @Test
    @DisplayName("Should extract US phone with hyphens")
    void testParseCandidateFromText_usPhoneWithHyphens() {
        String text = "Phone: 123-456-7890";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("123-456-7890", candidate.getPhone());
    }

    @Test
    @DisplayName("Should extract US phone with parentheses and spaces")
    void testParseCandidateFromText_usPhoneWithParentheses() {
        String text = "Reach out at (123) 456 7890";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("(123) 456 7890", candidate.getPhone());
    }
    
    @Test
    @DisplayName("Should extract mixed international format")
    void testParseCandidateFromText_mixedInternational() {
        String text = "Emergency: +1 (555) 123-4567";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("+1 (555) 123-4567", candidate.getPhone());
    }

    @Test
    @DisplayName("Should extract phone embedded in text")
    void testParseCandidateFromText_embeddedPhone() {
        String text = "Here is some text and my phone 07771234567 other text";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("07771234567", candidate.getPhone());
    }

    @Test
    @DisplayName("Should extract the first phone number when multiple are present")
    void testParseCandidateFromText_multiplePhones() {
        String text = "First number: 111-222-3333, Second number: 444-555-6666";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("111-222-3333", candidate.getPhone());
    }

    @Test
    @DisplayName("Should return N/A when no phone number is found")
    void testParseCandidateFromText_noPhone() {
        String text = "This text has no phone number.";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("N/A", candidate.getPhone());
    }

    @Test
    @DisplayName("Should return N/A for a number with less than 9 digits")
    void testParseCandidateFromText_shortNumber() {
        String text = "Short num 12345678";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals("N/A", candidate.getPhone());
    }
}