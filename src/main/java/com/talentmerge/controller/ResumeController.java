package com.talentmerge.controller;

import com.talentmerge.dto.CandidateResponseDTO;
import com.talentmerge.dto.EducationDTO;
import com.talentmerge.dto.WorkExperienceDTO;
import com.talentmerge.model.Candidate;
import com.talentmerge.repository.CandidateRepository;
import com.talentmerge.service.FileStorageService;
import com.talentmerge.service.IToolParsingService;
import com.talentmerge.service.IParsingService;
import com.talentmerge.service.AiParsingService;
import com.talentmerge.service.HybridAnonymizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final FileStorageService fileStorageService;
    private final IToolParsingService IToolParsingService;
    private final IParsingService parsingService;
    private final AiParsingService aiParsingService;
    private final HybridAnonymizationService anonymizationService;
    private final CandidateRepository candidateRepository;

    @Autowired
    public ResumeController(
            FileStorageService fileStorageService,
            IToolParsingService IToolParsingService,
            @Qualifier("manual") IParsingService parsingService,
            @Qualifier("ai") AiParsingService aiParsingService,
            HybridAnonymizationService anonymizationService,
            CandidateRepository candidateRepository) {
        this.fileStorageService = fileStorageService;
        this.IToolParsingService = IToolParsingService;
        this.parsingService = parsingService;
        this.aiParsingService = aiParsingService;
        this.anonymizationService = anonymizationService;
        this.candidateRepository = candidateRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            // 1. Store the file and get its path
            String storedFileName = fileStorageService.storeFile(file);
            String filePathString = fileStorageService.getFile(storedFileName).toString();

            // 2. Parse the resume content
            String rawText = IToolParsingService.parseResume(file.getInputStream(), file.getContentType());
            if (rawText.startsWith("Unsupported file type") || rawText.startsWith("Error parsing resume")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(rawText);
            }

            // 3. Extract structured data into a Candidate object
            Candidate candidate = parsingService.parseCandidateFromText(rawText);
            candidate.setOriginalFilePath(filePathString);

            // 4. Save the candidate to the database
            Candidate savedCandidate = candidateRepository.save(candidate);

            // 5. Return the structured data as a DTO
            List<WorkExperienceDTO> workExperienceDTOs = savedCandidate.getWorkExperiences().stream()
                    .map(exp -> new WorkExperienceDTO(exp.getId(), exp.getJobTitle(), exp.getCompany(), exp.getStartDate(), exp.getEndDate(), exp.getDescription()))
                    .collect(Collectors.toList());

            List<EducationDTO> educationDTOs = savedCandidate.getEducations().stream()
                    .map(edu -> new EducationDTO(edu.getId(), edu.getInstitution(), edu.getDegree(), edu.getGraduationDate()))
                    .collect(Collectors.toList());

            CandidateResponseDTO responseDTO = new CandidateResponseDTO(
                    savedCandidate.getId(),
                    savedCandidate.getName(),
                    savedCandidate.getEmail(),
                    savedCandidate.getPhone(),
                    savedCandidate.getSkills(),
                    rawText, // Include the raw text for debugging
                    savedCandidate.getOriginalFilePath(),
                    workExperienceDTOs,
                    educationDTOs
            );

            return ResponseEntity.ok(responseDTO);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process the file: " + e.getMessage());
        }
    }

    @PostMapping("/upload/ai")
    public ResponseEntity<?> uploadResumeWithAI(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            // 1. Store the file and get its path
            String storedFileName = fileStorageService.storeFile(file);
            String filePathString = fileStorageService.getFile(storedFileName).toString();

            // 2. Parse the resume content
            String rawText = IToolParsingService.parseResume(file.getInputStream(), file.getContentType());
            if (rawText.startsWith("Unsupported file type") || rawText.startsWith("Error parsing resume")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(rawText);
            }

            // 3. Extract structured data using AI parsing
            Candidate candidate = aiParsingService.parseCandidateFromText(rawText);
            candidate.setOriginalFilePath(filePathString);

            // 4. Save the candidate to the database
            Candidate savedCandidate = candidateRepository.save(candidate);

            // 5. Return the structured data as a DTO
            return buildCandidateResponse(savedCandidate, rawText);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process the file with AI: " + e.getMessage());
        }
    }

    @PostMapping("/upload/ai-anonymous")
    public ResponseEntity<?> uploadResumeWithAnonymizedAI(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please select a file to upload.");
        }

        try {
            // 1. Store the file and get its path
            String storedFileName = fileStorageService.storeFile(file);
            String filePathString = fileStorageService.getFile(storedFileName).toString();

            // 2. Parse the resume content
            String rawText = IToolParsingService.parseResume(file.getInputStream(), file.getContentType());
            if (rawText.startsWith("Unsupported file type") || rawText.startsWith("Error parsing resume")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(rawText);
            }

            // 3. Anonymize the resume text
            String anonymizedText = anonymizationService.anonymizeText(rawText);

            // 4. Extract structured data using AI parsing on anonymized text
            Candidate candidate = aiParsingService.parseAnonymizedCandidateFromText(anonymizedText);
            candidate.setOriginalFilePath(filePathString);

            // 5. Save the candidate to the database
            Candidate savedCandidate = candidateRepository.save(candidate);

            // 6. Return the structured data as a DTO (with anonymized data)
            return buildCandidateResponse(savedCandidate, anonymizedText);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process the file with anonymized AI: " + e.getMessage());
        }
    }

    private ResponseEntity<CandidateResponseDTO> buildCandidateResponse(Candidate savedCandidate, String rawText) {
        List<WorkExperienceDTO> workExperienceDTOs = savedCandidate.getWorkExperiences().stream()
                .map(exp -> new WorkExperienceDTO(exp.getId(), exp.getJobTitle(), exp.getCompany(), 
                    exp.getStartDate(), exp.getEndDate(), exp.getDescription()))
                .collect(Collectors.toList());

        List<EducationDTO> educationDTOs = savedCandidate.getEducations().stream()
                .map(edu -> new EducationDTO(edu.getId(), edu.getInstitution(), edu.getDegree(), 
                    edu.getGraduationDate()))
                .collect(Collectors.toList());

        CandidateResponseDTO responseDTO = new CandidateResponseDTO(
                savedCandidate.getId(),
                savedCandidate.getName(),
                savedCandidate.getEmail(),
                savedCandidate.getPhone(),
                savedCandidate.getSkills(),
                rawText, // Include the raw text (original or anonymized)
                savedCandidate.getOriginalFilePath(),
                workExperienceDTOs,
                educationDTOs
        );

        return ResponseEntity.ok(responseDTO);
    }
}