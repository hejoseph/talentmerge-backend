package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import com.talentmerge.model.Education;
import com.talentmerge.model.WorkExperience;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfBoxAndPoiParsingService implements ParsingService {

    private static final List<String> SECTION_KEYWORDS = Arrays.asList(
            // English
            "experience", "employment history", "work experience",
            "education", "academic background",
            "skills", "technical skills", "competencies",
            "summary", "profile", "objective",
            // French
            "expérience professionnelle", "expériences",
            "formation", "éducation",
            "compétences",
            "profil", "à propos"
    );

    private static final List<String> SKILL_DICTIONARY = Arrays.asList(
            "Java", "Python", "JavaScript", "C++", "C#", "Ruby", "Go", "TypeScript", "PHP", "Swift",
            "React", "Angular", "Vue.js", "Node.js", "Spring Boot", "Django", "Flask", "Ruby on Rails",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis", "Oracle",
            "AWS", "Azure", "Google Cloud", "Docker", "Kubernetes",
            "HTML", "CSS", "Sass", "Less",
            "Agile", "Scrum", "JIRA", "Git", "Jenkins"
            // Add more skills as needed
    );

    // Date patterns for parsing dates in resumes, now including French months
    private static final List<Pattern> DATE_PATTERNS = Arrays.asList(
            Pattern.compile("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)[a-z.]*\\s+\\d{4}\\b", Pattern.CASE_INSENSITIVE), // Month YYYY
            Pattern.compile("\\b\\d{2}/\\d{4}\\b"), // MM/YYYY
            Pattern.compile("\\b\\d{4}\\b") // YYYY
    );

    // Locale-specific formatters
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER_EN = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM yyyy")
            .toFormatter(Locale.ENGLISH);
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER_FR = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM yyyy")
            .toFormatter(Locale.FRENCH);
    private static final DateTimeFormatter MM_YYYY_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter YYYY_FORMATTER = DateTimeFormatter.ofPattern("yyyy");


    @Override
    public String parseResume(InputStream inputStream, String contentType) {
        try {
            if (contentType == null) {
                throw new IOException("Could not determine file type.");
            }

            switch (contentType) {
                case "application/pdf":
                    return parsePdf(inputStream);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
                    return parseDocx(inputStream);
                default:
                    return "Unsupported file type: " + contentType;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error parsing resume: " + e.getMessage();
        }
    }

    private String parsePdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String parseDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    @Override
    public Candidate parseCandidateFromText(String text) {
        Candidate candidate = new Candidate();
        candidate.setName(extractName(text));
        candidate.setEmail(extractEmail(text));
        candidate.setPhone(extractPhone(text));

        Map<String, String> sections = splitTextIntoSections(text);

        List<WorkExperience> experiences = parseWorkExperience(sections.getOrDefault("experience", ""));
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

    private Map<String, String> splitTextIntoSections(String text) {
        Map<String, String> sections = new HashMap<>();
        String[] lines = text.split("\\r?\\n");
        String currentSection = "summary"; // Default section for text at the beginning
        StringBuilder content = new StringBuilder();

        for (String line : lines) {
            String lowercasedLine = line.trim().toLowerCase();
            String matchedKeyword = null;

            for (String keyword : SECTION_KEYWORDS) {
                if (lowercasedLine.startsWith(keyword)) {
                    matchedKeyword = keyword;
                    break;
                }
            }

            if (matchedKeyword != null) {
                if (!content.toString().trim().isEmpty()) {
                    sections.put(normalizeSectionKey(currentSection), content.toString().trim());
                }
                currentSection = matchedKeyword;
                content = new StringBuilder();
                content.append(line).append(System.lineSeparator());
            } else {
                content.append(line).append(System.lineSeparator());
            }
        }
        sections.put(normalizeSectionKey(currentSection), content.toString().trim());

        return sections;
    }

    private String normalizeSectionKey(String keyword) {
        if (keyword.contains("experience") || keyword.contains("expériences") || keyword.contains("employment")) return "experience";
        if (keyword.contains("education") || keyword.contains("formation") || keyword.contains("academic")) return "education";
        if (keyword.contains("skill") || keyword.contains("compétences") || keyword.contains("competencies")) return "skills";
        if (keyword.contains("summary") || keyword.contains("profil") || keyword.contains("à propos") || keyword.contains("objective")) return "summary";
        return "summary"; // Default
    }

    private List<WorkExperience> parseWorkExperience(String text) {
        List<WorkExperience> experiences = new ArrayList<>();
        // This regex is adapted for both English and French formats.
        Pattern jobPattern = Pattern.compile(
                "(?<jobTitle>.+?)\\n"
                        + "(?<company>.+?)\\n"
                        + "(?<startDate>\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)[a-z.]*\\s+\\d{4}|\\d{2}/\\d{4}|\\d{4})\\s*[-– ]\\s*(?<endDate>Present|Aujourd'hui|\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)[a-z.]*\\s+\\d{4}|\\d{2}/\\d{4}|\\d{4})"
                        + "(?:\\n(?<description>(?:(?!^.+?\\n.+?\\n(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec|janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc)).|\\n)*))?",
                Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = jobPattern.matcher(text);
        while (matcher.find()) {
            WorkExperience exp = new WorkExperience();
            exp.setJobTitle(matcher.group("jobTitle").trim());
            exp.setCompany(matcher.group("company").trim());
            exp.setStartDate(parseDate(matcher.group("startDate")));
            exp.setEndDate(parseDate(matcher.group("endDate")));
            String description = matcher.group("description") != null ? matcher.group("description").trim() : "";
            exp.setDescription(description);
            experiences.add(exp);
        }
        return experiences;
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
            // Optionally, you could add the `details` to the education object if the model is updated.
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
        if (dateString == null || dateString.equalsIgnoreCase("Present") || dateString.equalsIgnoreCase("Aujourd'hui")) {
            return null;
        }
        dateString = dateString.replace(".", ""); // remove dots from janv. -> janv

        // Try parsing MM/YYYY directly first
        try {
            if (dateString.matches("\\d{2}/\\d{4}")) {
                return LocalDate.parse("01/" + dateString.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
        } catch (DateTimeParseException e) {
            // Fallback to other patterns if this specific parse fails
        }

        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(dateString.trim());
            if (matcher.matches()) {
                try {
                    String matchedStr = matcher.group(0);
                    if (matchedStr.matches("(?i).*[a-zA-Z].*")) { // Contains letters, so it's a Month YYYY format
                        // Try English first
                        try {
                            return LocalDate.parse("01 " + matchedStr, new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yyyy").toFormatter(Locale.ENGLISH)).withDayOfMonth(1);
                        } catch (DateTimeParseException e) {
                            // Then try French
                            try {
                                return LocalDate.parse("01 " + matchedStr, new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd MMM yyyy").toFormatter(Locale.FRENCH)).withDayOfMonth(1);
                            } catch (DateTimeParseException e2) {
                                // Log failure if needed
                            }
                        }
                    } else if (matchedStr.matches("\\d{4}")) {
                        return LocalDate.parse(matchedStr + "-01-01");
                    }
                } catch (DateTimeParseException e) {
                    // Continue to next pattern
                }
            }
        }
        return null; // Could not parse date
    }

    private String extractName(String text) {
        Pattern pattern = Pattern.compile("^([A-ZÀ-ÿ][a-zà-ÿ]+(?:\\s[A-ZÀ-ÿ][a-zà-ÿ']+)+)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "N/A";
    }

    private String extractEmail(String text) {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "N/A";
    }

    private String extractPhone(String text) {
        Pattern pattern = Pattern.compile("\\+?[\\d\\s-().]{9,25}");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(0);
            long digitCount = candidate.chars().filter(Character::isDigit).count();
            if (digitCount >= 9) {
                String cleanedCandidate = candidate.trim().replaceAll("[.,;:]*$", "");
                return cleanedCandidate;
            }
        }
        return "N/A";
    }
}