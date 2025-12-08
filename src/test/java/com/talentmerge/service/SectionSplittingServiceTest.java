package com.talentmerge.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionSplittingServiceTest {

    private SectionSplittingService sectionSplittingService;

    @BeforeEach
    void setUp() {
        sectionSplittingService = new SectionSplittingService();
    }

    @Test
    @DisplayName("Should split resume text into correct sections")
    void testSplitTextIntoSections() {
        String resumeText = "John Doe\n" +
                            "Software Engineer\n\n" +
                            "Summary\n" +
                            "A passionate software engineer.\n\n" +
                            "Work Experience\n" +
                            "Software Engineer at Google\n" +
                            "2020 - Present\n\n" +
                            "Education\n" +
                            "M.Sc. in Computer Science\n" +
                            "2018-2020\n\n" +
                            "Skills\n" +
                            "Java";
        Map<String, String> sections = sectionSplittingService.splitTextIntoSections(resumeText);
        System.out.println("Detected Sections for English Resume: " + sections);

        assertTrue(sections.containsKey("summary"));
        assertTrue(sections.containsKey("experience"));
        assertTrue(sections.containsKey("education"));
        assertTrue(sections.containsKey("skills"));

        assertEquals("A passionate software engineer.", sections.get("summary").trim());
        assertEquals("Software Engineer at Google\n2020 - Present", sections.get("experience").trim());
        assertEquals("M.Sc. in Computer Science\n2018-2020", sections.get("education").trim());
        assertEquals("Java, Python, Spring Boot", sections.get("skills").trim());
    }

    @Test
    @DisplayName("Should split French resume text into correct sections")
    void testSplitTextIntoSections_FrenchResume() {
        String resumeText = "Jean Dupont\n" +
                "Ingénieur Logiciel\n\n" +
                "Résumé\n" +
                "Un ingénieur logiciel passionné.\n\n" +
                "Expérience professionnelle\n" +
                "Ingénieur Logiciel chez Google\n" +
                "2020 - Présent\n\n" +
                "Formation\n" +
                "M.Sc. en Informatique\n" +
                "2018-2020\n\n" +
                "Compétences\n" +
                "Java, Python, Spring Boot";

        Map<String, String> sections = sectionSplittingService.splitTextIntoSections(resumeText);

        assertTrue(sections.containsKey("summary"));
        assertTrue(sections.containsKey("experience"));
        assertTrue(sections.containsKey("education"));
        assertTrue(sections.containsKey("skills"));

        assertEquals("Un ingénieur logiciel passionné.", sections.get("summary").trim());
        assertEquals("Ingénieur Logiciel chez Google\n2020 - Présent", sections.get("experience").trim());
        assertEquals("M.Sc. en Informatique\n2018-2020", sections.get("education").trim());
        assertEquals("Java, Python, Spring Boot", sections.get("skills").trim());
    }

    @Test
    @DisplayName("Should handle multi-line headers in a French resume")
    void testSplitTextIntoSections_FrenchResumeMultiLineHeader() {
        String resumeText = "Jean Dupont\n" +
                "Ingénieur Logiciel\n\n" +
                "EXPÉRIENCE\n" +
                "PROFESSIONNELLE\n" +
                "Ingénieur Logiciel chez Google\n" +
                "2020 - Présent\n\n" +
                "ÉDUCATION ET\n" +
                "FORMATION\n" +
                "M.Sc. en Informatique\n" +
                "2018-2020\n\n";

        Map<String, String> sections = sectionSplittingService.splitTextIntoSections(resumeText);

        assertTrue(sections.containsKey("experience"));
        assertTrue(sections.containsKey("education"));

        assertEquals("Ingénieur Logiciel chez Google\n2020 - Présent", sections.get("experience").trim());
        assertEquals("M.Sc. en Informatique\n2018-2020", sections.get("education").trim());
    }
}
