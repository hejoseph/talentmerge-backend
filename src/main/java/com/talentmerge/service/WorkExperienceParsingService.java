package com.talentmerge.service;

import com.talentmerge.model.WorkExperience;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dedicated service for parsing work experience entries from resume text
 * Handles multiple layout patterns and language-specific formats (English/French)
 */
@Service
public class WorkExperienceParsingService {

    // Date patterns for various formats
    private static final List<Pattern> DATE_PATTERNS = Arrays.asList(
        // English patterns
        Pattern.compile("(\\w{3,})\\s+(\\d{4})\\s*[-–]\\s*(\\w{3,})\\s+(\\d{4})", Pattern.CASE_INSENSITIVE), // "Jan 2020 - Dec 2022"
        Pattern.compile("(\\d{1,2})/(\\d{4})\\s*[-–]\\s*(\\d{1,2})/(\\d{4})"), // "01/2020 - 12/2022"
        Pattern.compile("(\\d{4})[-.]?(\\d{2})\\s*[-–]\\s*(\\d{4})[-.]?(\\d{2})"), // "2020-01 - 2022-12"
        Pattern.compile("(\\w{3,})\\s+(\\d{4})\\s*[-–]\\s*(present|current)", Pattern.CASE_INSENSITIVE), // "Jan 2020 - Present"
        
        // French patterns  
        Pattern.compile("(\\w{3,})\\s+(\\d{4})\\s*[-–]\\s*(aujourd'hui|actuel)", Pattern.CASE_INSENSITIVE), // "janv 2020 - Aujourd'hui"
        Pattern.compile("(\\d{1,2})/(\\d{4})\\s*[-–]\\s*(aujourd'hui|actuel)", Pattern.CASE_INSENSITIVE), // "01/2020 - Aujourd'hui"
        Pattern.compile("du\\s+(\\w{3,})\\s+(\\d{4})\\s+au\\s+(\\w{3,})\\s+(\\d{4})", Pattern.CASE_INSENSITIVE), // "du janv 2020 au déc 2022"
        Pattern.compile("de\\s+(\\d{1,2})/(\\d{4})\\s+à\\s+(\\d{1,2})/(\\d{4})") // "de 01/2020 à 12/2022"
    );

    // Job title indicators
    private static final List<String> JOB_TITLE_KEYWORDS = Arrays.asList(
        // English
        "engineer", "developer", "manager", "director", "analyst", "consultant", 
        "lead", "senior", "junior", "principal", "staff", "architect", "specialist",
        "coordinator", "supervisor", "executive", "officer", "administrator",
        
        // French
        "ingénieur", "développeur", "responsable", "directeur", "analyste", "consultant",
        "chef", "senior", "junior", "principal", "architecte", "spécialiste",
        "coordinateur", "superviseur", "chargé", "attaché", "gérant"
    );

    // Company indicators
    private static final List<String> COMPANY_INDICATORS = Arrays.asList(
        "inc", "corp", "corporation", "company", "ltd", "limited", "llc", "group",
        "sarl", "sas", "sa", "eurl", "société", "entreprise", "groupe", "gmbh", "ag"
    );

    /**
     * Main method to parse work experience from section text
     */
    public List<WorkExperience> parseWorkExperience(String experienceText) {
        if (experienceText == null || experienceText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<WorkExperience> experiences = new ArrayList<>();
        
        // Step 1: Split text into individual work experience entries
        List<WorkExperienceEntry> entries = extractWorkExperienceEntries(experienceText);
        
        // Step 2: Parse each entry to extract structured data
        for (WorkExperienceEntry entry : entries) {
            WorkExperience experience = parseWorkExperienceEntry(entry);
            if (experience != null) {
                experiences.add(experience);
            }
        }
        
        // Step 3: Sort by start date (most recent first)
        experiences.sort((a, b) -> {
            if (b.getStartDate() == null) return -1;
            if (a.getStartDate() == null) return 1;
            return b.getStartDate().compareTo(a.getStartDate());
        });
        
        return experiences;
    }

    /**
     * Step 1: Extract individual work experience entries from text
     */
    private List<WorkExperienceEntry> extractWorkExperienceEntries(String text) {
        String[] lines = text.split("\\r?\\n");
        List<WorkExperienceEntry> entries = new ArrayList<>();
        
        // Strategy 1: Split by date patterns
        List<Integer> dateLines = findDateLines(lines);
        
        if (dateLines.size() > 0) {
            entries = splitByDateLines(lines, dateLines);
        } else {
            // Strategy 2: Split by job title patterns
            entries = splitByJobTitlePatterns(lines);
        }
        
        return entries;
    }

    /**
     * Find lines that contain date patterns
     */
    private List<Integer> findDateLines(String[] lines) {
        List<Integer> dateLines = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (containsDatePattern(line)) {
                dateLines.add(i);
            }
        }
        
        return dateLines;
    }

    /**
     * Check if line contains a date pattern
     */
    private boolean containsDatePattern(String line) {
        for (Pattern pattern : DATE_PATTERNS) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Split text by date lines to create work experience entries
     */
    private List<WorkExperienceEntry> splitByDateLines(String[] lines, List<Integer> dateLines) {
        List<WorkExperienceEntry> entries = new ArrayList<>();
        
        for (int i = 0; i < dateLines.size(); i++) {
            int startLine = (i == 0) ? 0 : dateLines.get(i - 1) + 1;
            int endLine = dateLines.get(i);
            int descriptionEnd = (i + 1 < dateLines.size()) ? dateLines.get(i + 1) - 1 : lines.length - 1;
            
            WorkExperienceEntry entry = new WorkExperienceEntry();
            entry.jobTitleLines = extractLines(lines, startLine, endLine);
            entry.dateLines = Arrays.asList(lines[dateLines.get(i)]);
            entry.descriptionLines = extractLines(lines, endLine + 1, descriptionEnd + 1);
            
            entries.add(entry);
        }
        
        return entries;
    }

    /**
     * Split text by job title patterns when dates are not clearly separated
     */
    private List<WorkExperienceEntry> splitByJobTitlePatterns(String[] lines) {
        List<WorkExperienceEntry> entries = new ArrayList<>();
        
        WorkExperienceEntry currentEntry = null;
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;
            
            if (looksLikeJobTitle(trimmedLine)) {
                if (currentEntry != null) {
                    entries.add(currentEntry);
                }
                currentEntry = new WorkExperienceEntry();
                currentEntry.jobTitleLines = Arrays.asList(trimmedLine);
                currentEntry.dateLines = new ArrayList<>();
                currentEntry.descriptionLines = new ArrayList<>();
            } else if (currentEntry != null) {
                if (containsDatePattern(trimmedLine)) {
                    currentEntry.dateLines.add(trimmedLine);
                } else {
                    currentEntry.descriptionLines.add(trimmedLine);
                }
            }
        }
        
        if (currentEntry != null) {
            entries.add(currentEntry);
        }
        
        return entries;
    }

    /**
     * Check if line looks like a job title
     */
    private boolean looksLikeJobTitle(String line) {
        String lowerLine = line.toLowerCase();
        
        // Check for job title keywords
        for (String keyword : JOB_TITLE_KEYWORDS) {
            if (lowerLine.contains(keyword)) {
                return true;
            }
        }
        
        // Check formatting patterns (Title Case, etc.)
        if (line.matches("^[A-Z][a-z]+(?:\\s+[A-Z][a-z]*)*$") && line.length() < 60) {
            return true;
        }
        
        return false;
    }

    /**
     * Extract lines from array within range
     */
    private List<String> extractLines(String[] lines, int start, int end) {
        List<String> result = new ArrayList<>();
        for (int i = start; i < Math.min(end, lines.length); i++) {
            String trimmed = lines[i].trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Step 2: Parse individual work experience entry to extract structured data
     */
    private WorkExperience parseWorkExperienceEntry(WorkExperienceEntry entry) {
        WorkExperience experience = new WorkExperience();
        
        // Extract job title
        String jobTitle = extractJobTitle(entry.jobTitleLines);
        if (jobTitle == null || jobTitle.trim().isEmpty()) {
            return null; // Skip entries without clear job titles
        }
        experience.setJobTitle(jobTitle);
        
        // Extract company
        String company = extractCompany(entry);
        experience.setCompany(company != null ? company : "Unknown");
        
        // Extract dates
        DateRange dateRange = extractDateRange(entry.dateLines);
        if (dateRange != null) {
            experience.setStartDate(dateRange.startDate);
            experience.setEndDate(dateRange.endDate);
        }
        
        // Extract description
        String description = String.join("\n", entry.descriptionLines);
        experience.setDescription(description.trim());
        
        return experience;
    }

    /**
     * Extract job title from job title lines
     */
    private String extractJobTitle(List<String> jobTitleLines) {
        if (jobTitleLines.isEmpty()) return null;
        
        // Try to find the line with job title keywords
        for (String line : jobTitleLines) {
            if (looksLikeJobTitle(line)) {
                return cleanJobTitle(line);
            }
        }
        
        // If no clear job title found, use the first non-empty line
        for (String line : jobTitleLines) {
            if (!line.trim().isEmpty() && line.length() < 80) {
                return cleanJobTitle(line);
            }
        }
        
        return null;
    }

    /**
     * Clean and normalize job title
     */
    private String cleanJobTitle(String jobTitle) {
        return jobTitle.trim()
                      .replaceAll("^[•\\-\\*\\+]\\s+", "") // Remove bullet points
                      .replaceAll("\\s+", " "); // Normalize whitespace
    }

    /**
     * Extract company name from entry
     */
    private String extractCompany(WorkExperienceEntry entry) {
        // Look in job title lines first
        for (String line : entry.jobTitleLines) {
            String company = extractCompanyFromLine(line);
            if (company != null) return company;
        }
        
        // Look in description lines
        for (String line : entry.descriptionLines) {
            String company = extractCompanyFromLine(line);
            if (company != null) return company;
        }
        
        return null;
    }

    /**
     * Extract company name from a single line
     */
    private String extractCompanyFromLine(String line) {
        String lowerLine = line.toLowerCase();
        
        // Look for company indicators
        for (String indicator : COMPANY_INDICATORS) {
            if (lowerLine.contains(indicator)) {
                // Extract potential company name around the indicator
                String[] words = line.split("\\s+");
                StringBuilder company = new StringBuilder();
                
                for (int i = 0; i < words.length; i++) {
                    if (words[i].toLowerCase().contains(indicator)) {
                        // Include words before and after the indicator
                        int start = Math.max(0, i - 2);
                        int end = Math.min(words.length, i + 2);
                        
                        for (int j = start; j < end; j++) {
                            company.append(words[j]).append(" ");
                        }
                        break;
                    }
                }
                
                String result = company.toString().trim();
                if (result.length() > 0 && result.length() < 100) {
                    return result;
                }
            }
        }
        
        return null;
    }

    /**
     * Extract date range from date lines
     */
    private DateRange extractDateRange(List<String> dateLines) {
        for (String line : dateLines) {
            DateRange range = parseDateLine(line);
            if (range != null) return range;
        }
        return null;
    }

    /**
     * Parse a single date line to extract date range
     */
    private DateRange parseDateLine(String dateLine) {
        for (Pattern pattern : DATE_PATTERNS) {
            Matcher matcher = pattern.matcher(dateLine);
            if (matcher.find()) {
                try {
                    return parseDateMatch(matcher, dateLine);
                } catch (Exception e) {
                    // Continue to next pattern if parsing fails
                    continue;
                }
            }
        }
        return null;
    }

    /**
     * Parse matched date groups to create DateRange
     */
    private DateRange parseDateMatch(Matcher matcher, String originalLine) {
        DateRange range = new DateRange();
        String lowerLine = originalLine.toLowerCase();
        
        // Check if this is an ongoing position
        boolean isOngoing = lowerLine.contains("present") || lowerLine.contains("current") ||
                           lowerLine.contains("aujourd'hui") || lowerLine.contains("actuel");
        
        try {
            // Parse start date (first two groups typically)
            String startMonth = matcher.group(1);
            String startYear = matcher.group(2);
            range.startDate = parseDate(startMonth, startYear);
            
            if (!isOngoing) {
                // Parse end date (groups 3 and 4 typically)
                String endMonth = matcher.group(3);
                String endYear = matcher.group(4);
                range.endDate = parseDate(endMonth, endYear);
            }
            
        } catch (Exception e) {
            return null;
        }
        
        return range;
    }

    /**
     * Parse individual date components
     */
    private LocalDate parseDate(String monthStr, String yearStr) {
        try {
            int year = Integer.parseInt(yearStr);
            
            // Try to parse month
            int month;
            if (monthStr.matches("\\d+")) {
                month = Integer.parseInt(monthStr);
            } else {
                month = parseMonthName(monthStr);
            }
            
            return YearMonth.of(year, month).atDay(1);
        } catch (Exception e) {
            // If parsing fails, try with just year
            try {
                int year = Integer.parseInt(yearStr);
                return LocalDate.of(year, 1, 1);
            } catch (Exception ex) {
                throw new DateTimeParseException("Unable to parse date", monthStr + " " + yearStr, 0);
            }
        }
    }

    /**
     * Parse month names (English and French)
     */
    private int parseMonthName(String monthName) {
        Map<String, Integer> months = new HashMap<>();
        
        // English months
        months.put("jan", 1); months.put("january", 1);
        months.put("feb", 2); months.put("february", 2);
        months.put("mar", 3); months.put("march", 3);
        months.put("apr", 4); months.put("april", 4);
        months.put("may", 5);
        months.put("jun", 6); months.put("june", 6);
        months.put("jul", 7); months.put("july", 7);
        months.put("aug", 8); months.put("august", 8);
        months.put("sep", 9); months.put("september", 9);
        months.put("oct", 10); months.put("october", 10);
        months.put("nov", 11); months.put("november", 11);
        months.put("dec", 12); months.put("december", 12);
        
        // French months
        months.put("janv", 1); months.put("janvier", 1);
        months.put("févr", 2); months.put("février", 2);
        months.put("mars", 3);
        months.put("avr", 4); months.put("avril", 4);
        months.put("mai", 5);
        months.put("juin", 6);
        months.put("juil", 7); months.put("juillet", 7);
        months.put("août", 8);
        months.put("sept", 9); months.put("septembre", 9);
        months.put("oct", 10); months.put("octobre", 10);
        months.put("nov", 11); months.put("novembre", 11);
        months.put("déc", 12); months.put("décembre", 12);
        
        String lowerMonth = monthName.toLowerCase().replaceAll("[.]", "");
        return months.getOrDefault(lowerMonth, 1); // Default to January if not found
    }

    /**
     * Helper classes for parsing
     */
    private static class WorkExperienceEntry {
        List<String> jobTitleLines = new ArrayList<>();
        List<String> dateLines = new ArrayList<>();
        List<String> descriptionLines = new ArrayList<>();
    }

    private static class DateRange {
        LocalDate startDate;
        LocalDate endDate;
    }
}