package com.talentmerge.controller;

import com.talentmerge.dto.CandidateCreateRequestDTO;
import com.talentmerge.dto.CandidateResponseDTO;
import com.talentmerge.dto.ErrorResponse;
import com.talentmerge.service.CandidateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/candidates")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"}) // React dev server
public class CandidateController {

    private final CandidateService candidateService;

    /**
     * Create a new candidate manually
     */
    @PostMapping
    public ResponseEntity<CandidateResponseDTO> createCandidate(
            @Valid @RequestBody CandidateCreateRequestDTO request) {
        
        log.info("Creating candidate manually: {}", request.getName());
        
        CandidateResponseDTO candidate = candidateService.createCandidate(request);
        
        log.info("Successfully created candidate with ID: {}", candidate.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
    }

    /**
     * Get all candidates with pagination and sorting
     */
    @GetMapping
    public ResponseEntity<Page<?>> getAllCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search) {
        
        log.info("Retrieving candidates - page: {}, size: {}, sortBy: {}, sortDir: {}, search: {}", 
                page, size, sortBy, sortDir, search);
        
        // Validate pagination parameters
        if (page < 0) page = 0;
        if (size < 1) size = 10;
        if (size > 100) size = 100; // Limit max size to prevent performance issues
        
        // Create sort direction
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        
        // Validate sort field
        String validSortBy = validateSortField(sortBy);
        
        // Create pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortBy));
        
        // Get candidates (optimized summary for list view)
        boolean includeDetails = false;
        Page<?> candidates = candidateService.getAllCandidates(pageable, includeDetails);
        
        log.info("Retrieved {} candidates out of {} total", 
                candidates.getNumberOfElements(), candidates.getTotalElements());
        
        return ResponseEntity.ok(candidates);
    }

    /**
     * Get candidate by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCandidateById(@PathVariable Long id) {
        log.info("Retrieving candidate by ID: {}", id);
        
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_ID", "Candidate ID must be positive", null));
        }
        
        Optional<CandidateResponseDTO> candidate = candidateService.getCandidateById(id);
        
        if (candidate.isPresent()) {
            log.info("Found candidate: {}", candidate.get().getName());
            return ResponseEntity.ok(candidate.get());
        } else {
            log.warn("Candidate not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("CANDIDATE_NOT_FOUND", 
                            "Candidate not found with ID: " + id, null));
        }
    }

    /**
     * Update existing candidate
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCandidate(
            @PathVariable Long id,
            @Valid @RequestBody CandidateCreateRequestDTO request) {
        
        log.info("Updating candidate with ID: {}", id);
        
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_ID", "Candidate ID must be positive", null));
        }
        
        try {
            CandidateResponseDTO updatedCandidate = candidateService.updateCandidate(id, request);
            log.info("Successfully updated candidate: {}", updatedCandidate.getName());
            return ResponseEntity.ok(updatedCandidate);
            
        } catch (IllegalArgumentException e) {
            log.warn("Candidate not found for update: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("CANDIDATE_NOT_FOUND", e.getMessage(), null));
        }
    }

    /**
     * Delete candidate by ID
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCandidate(@PathVariable Long id) {
        log.info("Deleting candidate with ID: {}", id);
        
        if (id <= 0) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_ID", "Candidate ID must be positive", null));
        }
        
        try {
            candidateService.deleteCandidate(id);
            log.info("Successfully deleted candidate with ID: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            log.warn("Candidate not found for deletion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("CANDIDATE_NOT_FOUND", e.getMessage(), null));
        }
    }

    /**
     * Search candidate by email
     */
    @GetMapping("/search/email")
    public ResponseEntity<?> findByEmail(@RequestParam String email) {
        log.info("Searching for candidate by email: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_EMAIL", "Email parameter is required", null));
        }
        
        Optional<CandidateResponseDTO> candidate = candidateService.findByEmail(email);
        
        if (candidate.isPresent()) {
            log.info("Found candidate by email: {}", candidate.get().getName());
            return ResponseEntity.ok(candidate.get());
        } else {
            log.info("No candidate found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("CANDIDATE_NOT_FOUND", 
                            "No candidate found with email: " + email, null));
        }
    }

    /**
     * Check if email exists (for duplicate validation during form input)
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmailExists(@RequestParam String email) {
        log.info("Checking if email exists: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("INVALID_EMAIL", "Email parameter is required", null));
        }
        
        Optional<CandidateResponseDTO> candidate = candidateService.findByEmail(email);
        
        return ResponseEntity.ok(new EmailExistsResponse(candidate.isPresent(), email));
    }

    /**
     * Get candidate statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<CandidateStatsResponse> getCandidateStats() {
        log.info("Retrieving candidate statistics");
        
        // Get total count by requesting first page
        Page<?> firstPage = candidateService.getAllCandidates(PageRequest.of(0, 1), false);
        long totalCandidates = firstPage.getTotalElements();
        
        CandidateStatsResponse stats = new CandidateStatsResponse(totalCandidates);
        return ResponseEntity.ok(stats);
    }

    /**
     * Validate sort field to prevent SQL injection and invalid fields
     */
    private String validateSortField(String sortBy) {
        if (sortBy == null) return "id";
        
        return switch (sortBy.toLowerCase()) {
            case "name", "email", "phone" -> sortBy.toLowerCase();
            case "createdat", "created" -> "id"; // Use ID as proxy for creation time
            default -> "id"; // Default fallback
        };
    }

    /**
     * Response DTO for email existence check
     */
    public record EmailExistsResponse(boolean exists, String email) {}

    /**
     * Response DTO for candidate statistics
     */
    public record CandidateStatsResponse(long totalCandidates) {}
}