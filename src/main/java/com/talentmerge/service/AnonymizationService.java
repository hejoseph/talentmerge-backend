package com.talentmerge.service;

import com.talentmerge.model.Candidate;
import org.springframework.stereotype.Service;

@Service
public class AnonymizationService {

    private final PersonalInfoDetectionService personalInfoDetectionService;

    public AnonymizationService(PersonalInfoDetectionService personalInfoDetectionService) {
        this.personalInfoDetectionService = personalInfoDetectionService;
    }

    public String anonymize(String resumeText) {
        if (resumeText == null || resumeText.isEmpty()) {
            return "";
        }

        String anonymizedText = resumeText;

        Candidate personalInfo = personalInfoDetectionService.detectPersonalInfo(resumeText);

        // Anonymize name
        String name = personalInfo.getName();
        if (name != null && !name.isEmpty()) {
            anonymizedText = anonymizedText.replace(name, "[NAME]");
        }

        // Anonymize email
        String email = personalInfo.getEmail();
        if (email != null && !email.isEmpty()) {
            anonymizedText = anonymizedText.replace(email, "[EMAIL]");
        }

        // Anonymize phone
        String phone = personalInfo.getPhone();
        if (phone != null && !phone.isEmpty()) {
            anonymizedText = anonymizedText.replace(phone, "[PHONE]");
        }
        
        // Anonymize LinkedIn URL
        String linkedInUrl = personalInfoDetectionService.extractLinkedInUrl(resumeText);
        if (linkedInUrl != null && !linkedInUrl.isEmpty()) {
            // The extracted URL includes "https://www."
            String rawLinkedInUrl = linkedInUrl.substring("https://www.".length());
            anonymizedText = anonymizedText.replace(rawLinkedInUrl, "[LINKEDIN]");
        }

        return anonymizedText;
    }
}
