package com.talentmerge.service;

import org.springframework.stereotype.Service;

@Service
public class PromptService {

    public String createResumeParsingPrompt(String resumeText) {
        return """
            You are an expert resume parser. Parse the following resume text and extract structured information.
            
            Return ONLY a valid JSON object with the following structure (no markdown, no explanation):
            
            {
                "name": "Full name of the candidate (if found)",
                "email": "Email address (if found)",
                "phone": "Phone number (if found)",
                "skills": "Comma-separated list of technical skills and competencies",
                "workExperiences": [
                    {
                        "jobTitle": "Position title",
                        "company": "Company name",
                        "startDate": "YYYY-MM-DD format (use 01 for day/month if not specified)",
                        "endDate": "YYYY-MM-DD format or null if current job",
                        "description": "Brief description of responsibilities and achievements"
                    }
                ],
                "educations": [
                    {
                        "institution": "School/University name",
                        "degree": "Degree type and field of study",
                        "graduationDate": "YYYY-MM-DD format (use 01 for day/month if not specified)"
                    }
                ]
            }
            
            Important parsing guidelines:
            - Use null for missing information, don't make up data
            - For dates, if only year is provided, use January 1st (YYYY-01-01)
            - If month and year are provided, use 1st of that month (YYYY-MM-01)
            - Combine related skills into a readable comma-separated format
            - Keep job descriptions concise but informative
            - Extract the most relevant information only
            - If the text appears to be anonymized (e.g., "COMPANY_1", "PERSON_1"), use those values as-is
            
            Resume text to parse:
            
            """ + resumeText;
    }
    
    public String createAnonymizedResumeParsingPrompt(String anonymizedResumeText) {
        return """
            You are an expert resume parser working with anonymized resume data. Parse the following anonymized resume text and extract structured information.
            
            This resume has been anonymized with patterns like:
            - PERSON_X for names
            - COMPANY_X for company names  
            - UNIVERSITY_X for educational institutions
            - LOCATION_X for places
            - Keep these anonymized values exactly as they appear.
            
            Return ONLY a valid JSON object with the following structure (no markdown, no explanation):
            
            {
                "name": "Anonymized name (e.g., PERSON_1)",
                "email": "Anonymized or removed email",
                "phone": "Anonymized or removed phone",
                "skills": "Comma-separated list of technical skills and competencies",
                "workExperiences": [
                    {
                        "jobTitle": "Position title",
                        "company": "Anonymized company name (e.g., COMPANY_1)",
                        "startDate": "YYYY-MM-DD format (use 01 for day/month if not specified)",
                        "endDate": "YYYY-MM-DD format or null if current job",
                        "description": "Brief description of responsibilities and achievements"
                    }
                ],
                "educations": [
                    {
                        "institution": "Anonymized institution name (e.g., UNIVERSITY_1)",
                        "degree": "Degree type and field of study",
                        "graduationDate": "YYYY-MM-DD format (use 01 for day/month if not specified)"
                    }
                ]
            }
            
            Important parsing guidelines:
            - Preserve ALL anonymized identifiers exactly (PERSON_X, COMPANY_X, etc.)
            - Use null for missing information, don't make up data
            - For dates, if only year is provided, use January 1st (YYYY-01-01)
            - If month and year are provided, use 1st of that month (YYYY-MM-01)
            - Keep technical skills as-is (these are usually not anonymized)
            - Keep job descriptions but preserve any anonymized references
            
            Anonymized resume text to parse:
            
            """ + anonymizedResumeText;
    }
}