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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfBoxAndPoiParsingService implements ParsingService {

    private static final List<String> SECTION_KEYWORDS = Arrays.asList(
            "experience", "employment history", "work experience",
            "education", "academic background",
            "skills", "technical skills", "competencies",
            "summary", "profile", "objective"
    );

    // Date patterns for parsing dates in resumes
    private static final List<Pattern> DATE_PATTERNS = Arrays.asList(
            Pattern.compile("\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}\\b", Pattern.CASE_INSENSITIVE), // Month YYYY
            Pattern.compile("\\b\\d{2}/\\d{4}\\b"), // MM/YYYY
            Pattern.compile("\\b\\d{4}\\b") // YYYY
    );
    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");
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
                    // Optionally, handle unsupported file types or return a specific message
                    return "Unsupported file type: " + contentType;
            }
        } catch (IOException e) {
            // Log the exception and return an error message
            // For a real application, you'd use a logging framework
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

        candidate.setSkills(parseSkills(sections.getOrDefault("skills", "")));

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

            // Check if the line is a section header
            for (String keyword : SECTION_KEYWORDS) {
                if (lowercasedLine.startsWith(keyword)) {
                    matchedKeyword = keyword;
                    break;
                }
            }

            if (matchedKeyword != null) {
                // Save the previous section's content
                if (!content.toString().trim().isEmpty()) {
                    sections.put(normalizeSectionKey(currentSection), content.toString().trim());
                }
                // Start a new section
                currentSection = matchedKeyword;
                content = new StringBuilder();
                // Add the header line to the new section's content
                content.append(line).append(System.lineSeparator());
            } else {
                // Append line to the current section's content
                content.append(line).append(System.lineSeparator());
            }
        }
        // Add the last section
        sections.put(normalizeSectionKey(currentSection), content.toString().trim());

        return sections;
    }

    private String normalizeSectionKey(String keyword) {
        if (keyword.contains("experience") || keyword.contains("employment")) return "experience";
        if (keyword.contains("education") || keyword.contains("academic")) return "education";
        if (keyword.contains("skill") || keyword.contains("competencies")) return "skills";
        return "summary";
    }

    // Placeholder implementations for the next steps
    private List<WorkExperience> parseWorkExperience(String text) {
        // Logic to be implemented in Step 6
        List<WorkExperience> experiences = new ArrayList<>();
        Pattern jobPattern = Pattern.compile(
                "(?<jobTitle>[A-Za-z\\s,./-]+)\\n"
                        + "(?<company>[A-Za-z0-9\\s,.-]+)(?:\\s*|\\s*(?<location>[A-Za-z\\s,.]+))?\\n"
                        + "(?<startDate>\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{2}/\\d{4}|\\d{4})\\s*[-– ]\\s*(?<endDate>Present|\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{2}/\\d{4}|\\d{4})\\n"
                        + "(?<description>(?:(?! [A-Za-z\\s,./-]+\\n[A-Za-z0-9\\s,.-]+).)*?)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = jobPattern.matcher(text);
        while (matcher.find()) {
            WorkExperience exp = new WorkExperience();
            exp.setJobTitle(matcher.group("jobTitle").trim());
            exp.setCompany(matcher.group("company").trim());
            exp.setStartDate(parseDate(matcher.group("startDate")));
            exp.setEndDate(parseDate(matcher.group("endDate")));
            exp.setDescription(matcher.group("description").trim());
            experiences.add(exp);
        }
        return experiences;
    }

    private List<Education> parseEducation(String text) {
        // Logic to be implemented in Step 7
        List<Education> educations = new ArrayList<>();
        Pattern eduPattern = Pattern.compile(
                "(?<degree>[A-Za-z\\s.-]+)(?:,\\s*(?<major>[A-Za-z\\s.-]+))?\\n"
                        + "(?<institution>[A-Za-z0-9\\s,.-]+)(?:\\s*|\\s*(?<location>[A-Za-z\\s,.]+))?\\n"
                        + "(Graduated:\\s*)?(?<gradDate>\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}|\\d{2}/\\d{4}|\\d{4})",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = eduPattern.matcher(text);
        while (matcher.find()) {
            Education edu = new Education();
            // Prioritize degree and then fall back to the whole matched group if degree is null
            String degreeText = matcher.group("degree");
            String majorText = matcher.group("major");

            if (degreeText != null && !degreeText.trim().isEmpty()) {
                if (majorText != null && !majorText.trim().isEmpty()) {
                    edu.setDegree(degreeText.trim() + ", " + majorText.trim());
                } else {
                    edu.setDegree(degreeText.trim());
                }
            } else {
                edu.setDegree("N/A"); // Fallback if no specific degree or major found
            }

            edu.setInstitution(matcher.group("institution").trim());
            edu.setGraduationDate(parseDate(matcher.group("gradDate")));
            educations.add(edu);
        }
        return educations;
    }

    private String parseSkills(String text) {
        // Logic to be implemented in Step 8
        // For now, just return the whole block, cleaned up
        return text.replaceAll("\\s*\\r?\\n\\s*", ", ").trim();
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.equalsIgnoreCase("Present")) {
            return null;
        }
        dateString = dateString.replace(".", ""); // Remove dots from abbreviations like "Jan." -> "Jan"

        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(dateString);
            if (matcher.matches()) {
                try {
                    if (dateString.matches("(?i)(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{4}")) {
                        return LocalDate.parse(dateString, MONTH_YEAR_FORMATTER);
                    } else if (dateString.matches("\\d{2}/\\d{4}")) {
                        return LocalDate.parse("01/" + dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } else if (dateString.matches("\\d{4}")) {
                        return LocalDate.parse(dateString + "-01-01"); // Assume start of year
                    }
                } catch (DateTimeParseException e) {
                    // Continue to next pattern
                }
            }
        }
        return null; // Could not parse date
    }

    private String extractName(String text) {
        // This is a simple heuristic and might need to be improved.
        // It assumes the name is one of the first lines of the resume.
        Pattern pattern = Pattern.compile("^([A-Z][a-z]+(?:\\s[A-Z][a-z]+)+)", Pattern.MULTILINE);
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
        Pattern pattern = Pattern.compile("(\\+?\\d{1,3}[-\\.\\s]?)?\\(?\\d{3}\\)?[-\\.\\s]?\\d{3}[-\\.\\s]?\\d{4}");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return "N/A";
    }
}