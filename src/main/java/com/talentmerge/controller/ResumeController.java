package com.talentmerge.controller;

import com.talentmerge.dto.CandidateResponseDTO;
import com.talentmerge.model.Candidate;
import com.talentmerge.repository.CandidateRepository;
import com.talentmerge.service.FileStorageService;
import com.talentmerge.service.ParsingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final FileStorageService fileStorageService;
    private final ParsingService parsingService;
    private final CandidateRepository candidateRepository;

    @Autowired
    public ResumeController(
            FileStorageService fileStorageService,
            ParsingService parsingService,
            CandidateRepository candidateRepository) {
        this.fileStorageService = fileStorageService;
        this.parsingService = parsingService;
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
            String rawText = parsingService.parseResume(file.getInputStream(), file.getContentType());
            if (rawText.startsWith("Unsupported file type") || rawText.startsWith("Error parsing resume")) {
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(rawText);
            }

            // 3. Extract structured data into a Candidate object
            Candidate candidate = parsingService.parseCandidateFromText(rawText);
            candidate.setOriginalFilePath(filePathString);

            // 4. Save the candidate to the database
            Candidate savedCandidate = candidateRepository.save(candidate);

            // 5. Return the structured data as a DTO
            CandidateResponseDTO responseDTO = new CandidateResponseDTO(
                    savedCandidate.getId(),
                    savedCandidate.getName(),
                    savedCandidate.getEmail(),
                    savedCandidate.getPhone(),
                    savedCandidate.getSkills(),
                    savedCandidate.getOriginalFilePath()
            );

            return ResponseEntity.ok(responseDTO);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not process the file: " + e.getMessage());
        }
    }
}