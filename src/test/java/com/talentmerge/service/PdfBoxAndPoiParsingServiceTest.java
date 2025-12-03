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

    @Test
    @DisplayName("Should parse MM/YYYY date format in work experience")
    void testParseCandidateFromText_workExperienceWithMmYyyyDate() {
        String text = "Expérience professionnelle\n" +
                      "Software Engineer\n" +
                      "Tech Company\n" +
                      "01/2020 - 12/2022\n" +
                      "Developed cool stuff.";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals(1, candidate.getWorkExperiences().size());
        assertEquals("2020-01-01", candidate.getWorkExperiences().get(0).getStartDate().toString());
        assertEquals("2022-12-01", candidate.getWorkExperiences().get(0).getEndDate().toString());
    }

    @Test
    @DisplayName("Should parse MM/YYYY date format in education")
    void testParseCandidateFromText_educationWithMmYyyyDate() {
        String text = "Formation\n" +
                      "Master's Degree\n" +
                      "University of Technology\n" +
                      "09/2019\n" +
                      "Graduated with honors.";
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals(1, candidate.getEducations().size());
        assertEquals("2019-09-01", candidate.getEducations().get(0).getGraduationDate().toString());
    }

    @Test
    @DisplayName("Should parse English work experience with month names")
    void testParseCandidateFromText_englishWorkExperienceWithMonthNames() {
        String text = "EXPERIENCE\\n" +
                      "Software Engineer\\n" +
                      "Google Inc.\\n" +
                      "January 2020 - Present\\n" +
                      "• Developed scalable web applications\\n" +
                      "• Led team of 5 developers\\n\\n" +
                      "Senior Developer\\n" +
                      "Microsoft Corporation\\n" +
                      "June 2018 - December 2019\\n" +
                      "• Built cloud-based solutions\\n";
        
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals(2, candidate.getWorkExperiences().size());
        
        // First experience
        assertEquals("Software Engineer", candidate.getWorkExperiences().get(0).getJobTitle());
        assertEquals("Google Inc.", candidate.getWorkExperiences().get(0).getCompany());
        assertEquals("2020-01-01", candidate.getWorkExperiences().get(0).getStartDate().toString());
        
        // Second experience
        assertEquals("Senior Developer", candidate.getWorkExperiences().get(1).getJobTitle());
        assertEquals("Microsoft Corporation", candidate.getWorkExperiences().get(1).getCompany());
        assertEquals("2018-06-01", candidate.getWorkExperiences().get(1).getStartDate().toString());
        assertEquals("2019-12-01", candidate.getWorkExperiences().get(1).getEndDate().toString());
    }

    @Test
    @DisplayName("Should parse French work experience with month names")
    void testParseCandidateFromText_frenchWorkExperienceWithMonthNames() {
        String text = "EXPÉRIENCE PROFESSIONNELLE\\n" +
                      "Ingénieur Logiciel\\n" +
                      "Google France\\n" +
                      "janvier 2020 - Aujourd'hui\\n" +
                      "• Développement d'applications web évolutives\\n" +
                      "• Direction d'une équipe de 5 développeurs\\n\\n" +
                      "Développeur Senior\\n" +
                      "Microsoft France\\n" +
                      "juin 2018 - décembre 2019\\n" +
                      "• Construction de solutions basées sur le cloud\\n";
        
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals(2, candidate.getWorkExperiences().size());
        
        // First experience
        assertEquals("Ingénieur Logiciel", candidate.getWorkExperiences().get(0).getJobTitle());
        assertEquals("Google France", candidate.getWorkExperiences().get(0).getCompany());
        assertEquals("2020-01-01", candidate.getWorkExperiences().get(0).getStartDate().toString());
        
        // Second experience
        assertEquals("Développeur Senior", candidate.getWorkExperiences().get(1).getJobTitle());
        assertEquals("Microsoft France", candidate.getWorkExperiences().get(1).getCompany());
        assertEquals("2018-06-01", candidate.getWorkExperiences().get(1).getStartDate().toString());
        assertEquals("2019-12-01", candidate.getWorkExperiences().get(1).getEndDate().toString());
    }

    @Test
    @DisplayName("Should parse work experience with abbreviated French months")
    void testParseCandidateFromText_frenchWorkExperienceWithAbbreviatedMonths() {
        String text = "EXPÉRIENCE\\n" +
                      "Chef de Projet\\n" +
                      "Société Générale\\n" +
                      "janv. 2021 - mars 2023\\n" +
                      "• Gestion de projets IT\\n";
        
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals(1, candidate.getWorkExperiences().size());
        
        assertEquals("Chef de Projet", candidate.getWorkExperiences().get(0).getJobTitle());
        assertEquals("Société Générale", candidate.getWorkExperiences().get(0).getCompany());
        assertEquals("2021-01-01", candidate.getWorkExperiences().get(0).getStartDate().toString());
        assertEquals("2023-03-01", candidate.getWorkExperiences().get(0).getEndDate().toString());
    }

    @Test
    @DisplayName("Should parse work experience with year only dates")
    void testParseCandidateFromText_workExperienceWithYearOnly() {
        String text = "WORK EXPERIENCE\\n" +
                      "Technical Lead\\n" +
                      "Apple Inc.\\n" +
                      "2019 - 2022\\n" +
                      "• Led development team\\n";
        
        Candidate candidate = parsingService.parseCandidateFromText(text);
        assertEquals(1, candidate.getWorkExperiences().size());
        
        assertEquals("Technical Lead", candidate.getWorkExperiences().get(0).getJobTitle());
        assertEquals("Apple Inc.", candidate.getWorkExperiences().get(0).getCompany());
        assertEquals("2019-01-01", candidate.getWorkExperiences().get(0).getStartDate().toString());
        assertEquals("2022-01-01", candidate.getWorkExperiences().get(0).getEndDate().toString());
    }
}