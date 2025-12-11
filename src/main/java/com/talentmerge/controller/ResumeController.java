package com.talentmerge.controller;

import com.talentmerge.dto.CandidateResponseDTO;
import com.talentmerge.dto.EducationDTO;
import com.talentmerge.dto.WorkExperienceDTO;
import com.talentmerge.dto.ResumeExtractResponse;
import com.talentmerge.dto.ResumeParseRequest;
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
@RequestMapping("/resumes")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}) // React dev server
public class ResumeController {

    private final FileStorageService fileStorageService;
    private final IToolParsingService IToolParsingService;
    private final IParsingService parsingService;
    private final HybridAnonymizationService anonymizationService;
    private final CandidateRepository candidateRepository;

    @Autowired
    public ResumeController(
            FileStorageService fileStorageService,
            IToolParsingService IToolParsingService,
            @Qualifier("ai") IParsingService parsingService,
            HybridAnonymizationService anonymizationService,
            CandidateRepository candidateRepository) {
        this.fileStorageService = fileStorageService;
        this.IToolParsingService = IToolParsingService;
        this.parsingService = parsingService;
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

            // 2. Parse the resume content to raw text only
            String rawText = IToolParsingService.parseResume(file.getInputStream(), file.getContentType());
            if (rawText.startsWith("Unsupported file type") || rawText.startsWith("Error parsing resume")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(rawText);
            }

            // 3. Return raw text and original file path so the user can review/edit
            return ResponseEntity.ok(new ResumeExtractResponse(rawText, filePathString));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process the file: " + e.getMessage());
        }
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parseEditedText(@RequestBody ResumeParseRequest request) {
        String rawText = request.getRawText();
        if (rawText == null || rawText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("rawText is required");
        }
        try {
            Candidate candidate = parsingService.parseCandidateFromText(rawText);
            if (request.getOriginalFilePath() != null) {
                candidate.setOriginalFilePath(request.getOriginalFilePath());
            }
            Candidate savedCandidate = candidateRepository.save(candidate);

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
                    rawText,
                    savedCandidate.getOriginalFilePath(),
                    workExperienceDTOs,
                    educationDTOs
            );

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to parse edited text: " + e.getMessage());
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