package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import com.talentmerge.model.Education;
import com.talentmerge.model.WorkExperience;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manual parsing service that uses rule-based algorithms to extract candidate information
 * from resume text. This is an alternative to AI/LLM-based parsing.
 */
@Service
public class ManualParsingService implements IParsingService{

    private final SectionSplittingService sectionSplittingService;
    private final WorkExperienceParsingService workExperienceParsingService;
    private final PersonalInfoDetectionService personalInfoDetectionService;

    private static final List<String> SKILL_DICTIONARY = List.of(
            "Java", "Python", "JavaScript", "C++", "C#", "Ruby", "Go", "TypeScript", "PHP", "Swift",
            "React", "Angular", "Vue.js", "Node.js", "Spring Boot", "Django", "Flask", "Ruby on Rails",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Oracle",
            "AWS", "Azure", "Google Cloud", "Docker", "Kubernetes",
            "HTML", "CSS", "Sass", "Less",
            "Agile", "Scrum", "JIRA", "Git", "Jenkins"
    );

    public ManualParsingService(SectionSplittingService sectionSplittingService, 
                               WorkExperienceParsingService workExperienceParsingService, 
                               PersonalInfoDetectionService personalInfoDetectionService) {
        this.sectionSplittingService = sectionSplittingService;
        this.workExperienceParsingService = workExperienceParsingService;
        this.personalInfoDetectionService = personalInfoDetectionService;
    }

    /**
     * Parse candidate information from text using manual/rule-based parsing
     */
    public Candidate parseCandidateFromText(String text) {
        Candidate candidate = personalInfoDetectionService.detectPersonalInfo(text);

        Map<String, String> sections = sectionSplittingService.splitTextIntoSections(text);

        List<WorkExperience> experiences = workExperienceParsingService.parseWorkExperience(sections.getOrDefault("experience", ""));
        experiences.forEach(candidate::addWorkExperience);

        List<Education> educations = parseEducation(sections.getOrDefault("education", ""));
        educations.forEach(candidate::addEducation);

        String skillsSection = sections.getOrDefault("skills", "");
        String skills = parseSkills(skillsSection);
        if (skills.isEmpty()) {
            skills = parseSkills(text); // Fallback to searching the whole text
        }
        candidate.setSkills(skills);

        return candidate;
    }

    private List<Education> parseEducation(String text) {
        List<Education> educations = new ArrayList<>();
        // This regex is adapted for both English and French formats.
        Pattern eduPattern = Pattern.compile(
                "(?<degree>.+?)\\n"
                        + "(?<institution>.+?)\\n"
                        + "(?:Graduated: |Obtenu en )?(?<gradDate>\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)[a-z.]*\\s+\\d{4}|\\d{2}/\\d{4}|\\d{4})"
                        + "(?:\\n(?<details>(?:(?!^.+?\\n.+?\\n(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)).|\\n)*))?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = eduPattern.matcher(text);
        while (matcher.find()) {
            Education edu = new Education();
            edu.setDegree(matcher.group("degree").trim());
            edu.setInstitution(matcher.group("institution").trim());
            edu.setGraduationDate(parseDate(matcher.group("gradDate")));
            educations.add(edu);
        }
        return educations;
    }

    private String parseSkills(String text) {
        List<String> foundSkills = new ArrayList<>();
        for (String skill : SKILL_DICTIONARY) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(skill) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                if (!foundSkills.contains(skill)) {
                    foundSkills.add(skill);
                }
            }
        }
        return String.join(", ", foundSkills);
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty() || 
            dateString.equalsIgnoreCase("Present") || dateString.equalsIgnoreCase("Aujourd'hui") ||
            dateString.equalsIgnoreCase("Current") || dateString.equalsIgnoreCase("Actuel")) {
            return null;
        }
        
        // Clean the input string
        String cleanedDate = dateString.trim()
            .replace(".", "")  // remove dots from janv. -> janv
            .replaceAll("\\s+", " "); // normalize spaces
        
        // Try MM/YYYY format first
        Pattern mmYyyyPattern = Pattern.compile("\\d{1,2}/\\d{4}");
        Matcher mmYyyyMatcher = mmYyyyPattern.matcher(cleanedDate);
        if (mmYyyyMatcher.find()) {
            try {
                String matched = mmYyyyMatcher.group();
                String[] parts = matched.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);
                return LocalDate.of(year, month, 1);
            } catch (Exception e) {
                // Continue to other patterns
            }
        }
        
        // Try year only format
        Pattern yearPattern = Pattern.compile("\\b(19|20)\\d{2}\\b");
        Matcher yearMatcher = yearPattern.matcher(cleanedDate);
        if (yearMatcher.find()) {
            try {
                int year = Integer.parseInt(yearMatcher.group());
                return LocalDate.of(year, 1, 1);
            } catch (Exception e) {
                // Continue to other patterns
            }
        }
        
        // Try month year formats (both English and French)
        Map<String, Integer> monthMap = new HashMap<>();
        // English months
        monthMap.put("jan", 1); monthMap.put("january", 1);
        monthMap.put("feb", 2); monthMap.put("february", 2);
        monthMap.put("mar", 3); monthMap.put("march", 3);
        monthMap.put("apr", 4); monthMap.put("april", 4);
        monthMap.put("may", 5);
        monthMap.put("jun", 6); monthMap.put("june", 6);
        monthMap.put("jul", 7); monthMap.put("july", 7);
        monthMap.put("aug", 8); monthMap.put("august", 8);
        monthMap.put("sep", 9); monthMap.put("september", 9);
        monthMap.put("oct", 10); monthMap.put("october", 10);
        monthMap.put("nov", 11); monthMap.put("november", 11);
        monthMap.put("dec", 12); monthMap.put("december", 12);
        
        // French months
        monthMap.put("janv", 1); monthMap.put("janvier", 1);
        monthMap.put("févr", 2); monthMap.put("février", 2);
        monthMap.put("mars", 3);
        monthMap.put("avr", 4); monthMap.put("avril", 4);
        monthMap.put("mai", 5);
        monthMap.put("juin", 6);
        monthMap.put("juil", 7); monthMap.put("juillet", 7);
        monthMap.put("août", 8);
        monthMap.put("sept", 9); monthMap.put("septembre", 9);
        monthMap.put("oct", 10); monthMap.put("octobre", 10);
        monthMap.put("nov", 11); monthMap.put("novembre", 11);
        monthMap.put("déc", 12); monthMap.put("décembre", 12);
        
        // Try to find month and year in the string
        Pattern monthYearPattern = Pattern.compile("(?i)\\b(\\d{4})\\b|\\b(" + String.join("|", monthMap.keySet()) + ")\\b");
        Matcher monthYearMatcher = monthYearPattern.matcher(cleanedDate.toLowerCase());
        
        Integer month = null;
        Integer year = null;
        
        while (monthYearMatcher.find()) {
            String matched = monthYearMatcher.group().toLowerCase();
            if (matched.matches("\\d{4}")) {
                year = Integer.parseInt(matched);
            } else if (monthMap.containsKey(matched)) {
                month = monthMap.get(matched);
            }
        }
        
        if (year != null) {
            if (month != null) {
                return LocalDate.of(year, month, 1);
            } else {
                return LocalDate.of(year, 1, 1);
            }
        }
        
        return null; // Could not parse date
    }
}