package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PersonalInfoDetectionService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+?[0-9][0-9 ()-]{7,20})");
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile("linkedin\\.com/in/[a-zA-Z0-9-]+");

    public Candidate detectPersonalInfo(String text) {
        Candidate candidate = new Candidate();
        candidate.setName(extractName(text));
        candidate.setEmail(extractEmail(text));
        candidate.setPhone(extractPhoneNumber(text));
        // The candidate model does not have a field for linkedin url. I will add it later if the user wants it.
        return candidate;
    }

    private String extractName(String text) {
        // Simple heuristic: the first non-empty line is the name.
        // This can be improved later.
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                return line.trim();
            }
        }
        return null;
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String extractPhoneNumber(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    public String extractLinkedInUrl(String text) {
        Matcher matcher = LINKEDIN_PATTERN.matcher(text);
        if (matcher.find()) {
            return "https://www." + matcher.group();
        }
        return null;
    }
}
