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
import java.text.Normalizer;

@Service
public class PdfBoxAndPoiParsingService implements ParsingService {

    private static final List<String> SECTION_KEYWORDS = Arrays.asList(
            // English
            "experience", "employment history", "work experience", "professional experience",
            "work history", "career history", "employment",
            "education", "academic background", "academic history",
            "skills", "technical skills", "competencies", "core competencies",
            "summary", "profile", "objective", "about",
            // French
            "expérience professionnelle", "expériences professionnelles", "expériences", 
            "expérience", "historique professionnel", "parcours professionnel",
            "formation", "formations", "éducation", "parcours académique",
            "compétences", "compétences techniques", "savoir-faire",
            "profil", "à propos", "résumé", "objectif"
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
        // Stage 1: Text Preprocessing
        String preprocessedText = preprocessText(text);
        
        Map<String, String> sections = new HashMap<>();
        String[] lines = preprocessedText.split("\\r?\\n");
        String currentSection = "summary"; // Default section for text at the beginning
        StringBuilder content = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            String lowercasedLine = trimmedLine.toLowerCase();
            String matchedKeyword = null;

            // Check if line contains section keywords (more flexible matching)
            for (String keyword : SECTION_KEYWORDS) {
                if (lowercasedLine.contains(keyword)) {
                    // Additional check to ensure it's likely a section header
                    // (short line, uppercase, or contains common header patterns)
                    if (trimmedLine.length() < 50 && 
                        (lowercasedLine.equals(keyword) || 
                         trimmedLine.toUpperCase().contains(keyword.toUpperCase()) ||
                         lowercasedLine.startsWith(keyword))) {
                        matchedKeyword = keyword;
                        break;
                    }
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
        String lowerKeyword = keyword.toLowerCase();
        if (lowerKeyword.contains("experience") || lowerKeyword.contains("expérience") || 
            lowerKeyword.contains("employment") || lowerKeyword.contains("career") ||
            lowerKeyword.contains("work") || lowerKeyword.contains("professional") ||
            lowerKeyword.contains("parcours professionnel") || lowerKeyword.contains("historique professionnel")) 
            return "experience";
        if (lowerKeyword.contains("education") || lowerKeyword.contains("formation") || 
            lowerKeyword.contains("academic") || lowerKeyword.contains("éducation") ||
            lowerKeyword.contains("parcours académique")) 
            return "education";
        if (lowerKeyword.contains("skill") || lowerKeyword.contains("compétences") || 
            lowerKeyword.contains("competencies") || lowerKeyword.contains("savoir-faire")) 
            return "skills";
        if (lowerKeyword.contains("summary") || lowerKeyword.contains("profil") || 
            lowerKeyword.contains("à propos") || lowerKeyword.contains("objective") ||
            lowerKeyword.contains("résumé") || lowerKeyword.contains("about")) 
            return "summary";
        return "summary"; // Default
    }

    private List<WorkExperience> parseWorkExperience(String text) {
        List<WorkExperience> experiences = new ArrayList<>();
        
        if (text == null || text.trim().isEmpty()) {
            return experiences;
        }
        
        // Split text into lines and process
        String[] lines = text.split("\\r?\\n");
        
        // Use a simpler approach - look for sequences of job title, company, dates
        for (int i = 0; i < lines.length - 2; i++) {
            String line1 = lines[i].trim();
            String line2 = i + 1 < lines.length ? lines[i + 1].trim() : "";
            String line3 = i + 2 < lines.length ? lines[i + 2].trim() : "";
            String line4 = i + 3 < lines.length ? lines[i + 3].trim() : "";
            
            // Skip empty lines and obvious section headers
            if (line1.isEmpty() || isObviousSectionHeader(line1)) {
                continue;
            }
            
            // Look for date patterns in the next few lines
            String dateLineContent = null;
            int dateLineIndex = -1;
            
            // Check lines 2, 3, 4 for date patterns
            for (int j = i + 1; j <= i + 3 && j < lines.length; j++) {
                String checkLine = lines[j].trim();
                if (containsDateRange(checkLine)) {
                    dateLineContent = checkLine;
                    dateLineIndex = j;
                    break;
                }
            }
            
            // If we found a date pattern, this might be a work experience entry
            if (dateLineContent != null && dateLineIndex >= i + 1) {
                WorkExperience exp = new WorkExperience();
                
                // Set job title and company based on position relative to date line
                if (dateLineIndex == i + 2) {
                    // Pattern: Job Title, Company, Date
                    exp.setJobTitle(line1);
                    exp.setCompany(line2);
                } else if (dateLineIndex == i + 3) {
                    // Pattern: Job Title, Company, something, Date  OR  Company, Job Title, something, Date
                    if (seemsLikeJobTitle(line1)) {
                        exp.setJobTitle(line1);
                        exp.setCompany(line2);
                    } else {
                        exp.setJobTitle(line2);
                        exp.setCompany(line1);
                    }
                } else {
                    // Pattern: Job Title, Date  (company might be missing)
                    exp.setJobTitle(line1);
                    exp.setCompany("N/A");
                }
                
                // Parse the date range
                String[] dates = splitDateRange(dateLineContent);
                if (dates.length >= 1) {
                    exp.setStartDate(parseDate(dates[0]));
                }
                if (dates.length >= 2) {
                    exp.setEndDate(parseDate(dates[1]));
                }
                
                // Look for description after the date line
                StringBuilder description = new StringBuilder();
                for (int j = dateLineIndex + 1; j < Math.min(dateLineIndex + 5, lines.length); j++) {
                    String descLine = lines[j].trim();
                    if (descLine.isEmpty()) continue;
                    if (isObviousSectionHeader(descLine)) break;
                    if (containsDateRange(descLine)) break; // Next job entry
                    
                    if (descLine.startsWith("•") || descLine.startsWith("-") || descLine.startsWith("*") || 
                        descLine.startsWith("◦") || descLine.toLowerCase().matches(".*\\b(developed|built|managed|led|created|implemented|designed|responsable|développé|géré|créé|conçu)\\b.*")) {
                        if (description.length() > 0) description.append(" ");
                        description.append(descLine);
                    }
                }
                exp.setDescription(description.toString());
                
                // Only add if we have at least a job title
                if (exp.getJobTitle() != null && !exp.getJobTitle().trim().isEmpty() && !exp.getJobTitle().equals("N/A")) {
                    experiences.add(exp);
                }
                
                // Skip processed lines
                i = dateLineIndex;
            }
        }
        
        return experiences;
    }
    
    private boolean isObviousSectionHeader(String line) {
        if (line.length() > 50) return false;
        String lower = line.toLowerCase();
        return line.toUpperCase().equals(line) && line.length() > 2 ||
               SECTION_KEYWORDS.stream().anyMatch(keyword -> lower.equals(keyword.toLowerCase()));
    }
    
    private boolean containsDateRange(String line) {
        if (line.length() > 100) return false;
        
        // Look for various date patterns
        return line.matches("(?i).*\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|january|february|march|april|may|june|july|august|september|october|november|december|janv|févr|mars|avr|mai|juin|juil|août|sept|oct|nov|déc|janvier|février|avril|juillet|août|septembre|octobre|novembre|décembre)\\b.*\\b(19|20)\\d{2}\\b.*") ||
               line.matches("(?i).*(19|20)\\d{2}\\s*[-–—to àau]\\s*(19|20)\\d{2}.*") ||
               line.matches("(?i).*\\d{1,2}/\\d{4}\\s*[-–—to àau]\\s*\\d{1,2}/\\d{4}.*") ||
               line.matches("(?i).*(present|current|aujourd'hui|actuel).*") ||
               line.matches("(?i).*\\b(19|20)\\d{2}\\b.*[-–—].*\\b(present|current|aujourd'hui|actuel|19|20\\d{2})\\b.*");
    }
    
    private boolean seemsLikeJobTitle(String line) {
        if (line.isEmpty() || line.length() > 80) return false;
        String lower = line.toLowerCase();
        return lower.contains("engineer") || lower.contains("developer") || lower.contains("manager") ||
               lower.contains("director") || lower.contains("analyst") || lower.contains("specialist") ||
               lower.contains("consultant") || lower.contains("coordinator") || lower.contains("lead") ||
               lower.contains("senior") || lower.contains("junior") || lower.contains("principal") ||
               lower.contains("ingénieur") || lower.contains("développeur") || lower.contains("gestionnaire") ||
               lower.contains("directeur") || lower.contains("analyste") || lower.contains("chef") ||
               lower.contains("responsable") || lower.contains("consultant") || 
               Character.isUpperCase(line.charAt(0));
    }
    
    private String[] splitDateRange(String dateRange) {
        // Split on various separators
        String[] parts = dateRange.split("(?i)\\s*(?:[-–—]|to|à|au)\\s*");
        if (parts.length >= 2) {
            return new String[]{parts[0].trim(), parts[1].trim()};
        } else if (parts.length == 1) {
            return new String[]{parts[0].trim()};
        }
        return new String[]{};
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
    
    /**
     * Stage 1: Preprocess text to normalize formatting and fix common issues
     */
    private String preprocessText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 1. Unicode normalization - decompose accented characters then recompose
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        
        // 2. Fix common OCR errors
        normalized = fixCommonOcrErrors(normalized);
        
        // 3. Standardize line breaks
        normalized = normalized.replaceAll("\\r\\n", "\n")
                              .replaceAll("\\r", "\n");
        
        // 4. Clean up excessive whitespace while preserving structure
        normalized = cleanWhitespace(normalized);
        
        return normalized;
    }
    
    /**
     * Fix common OCR errors that affect section detection
     */
    private String fixCommonOcrErrors(String text) {
        return text
            // Fix common character misrecognition
            .replaceAll("\\bl\\b", "I")  // lowercase l mistaken for I
            .replaceAll("\\brn\\b", "m") // rn mistaken for m
            .replaceAll("\\bO\\b", "0")  // O mistaken for 0 in dates
            // Fix common French accent issues
            .replaceAll("e'", "é")
            .replaceAll("a'", "à")
            .replaceAll("E'", "É")
            .replaceAll("A'", "À")
            // Fix spacing around punctuation
            .replaceAll("\\s+:", ":")
            .replaceAll("\\s+;", ";");
    }
    
    /**
     * Clean whitespace while preserving document structure
     */
    private String cleanWhitespace(String text) {
        String[] lines = text.split("\\n");
        StringBuilder cleaned = new StringBuilder();
        
        for (String line : lines) {
            // Remove trailing whitespace
            String trimmed = line.replaceAll("\\s+$", "");
            
            // Normalize internal whitespace (multiple spaces/tabs to single space)
            trimmed = trimmed.replaceAll("\\s+", " ");
            
            // Preserve leading whitespace structure for indentation
            if (!trimmed.trim().isEmpty()) {
                cleaned.append(trimmed).append("\n");
            } else if (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 1) != '\n') {
                // Preserve single empty lines for structure
                cleaned.append("\n");
            }
        }
        
        return cleaned.toString();
    }
}