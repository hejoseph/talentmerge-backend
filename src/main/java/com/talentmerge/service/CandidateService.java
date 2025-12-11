package com.talentmerge.service;

import com.talentmerge.dto.CandidateCreateRequestDTO;
import com.talentmerge.dto.CandidateResponseDTO;
import com.talentmerge.dto.CandidateListItemDTO;
import com.talentmerge.dto.EducationCreateDTO;
import com.talentmerge.dto.EducationDTO;
import com.talentmerge.dto.WorkExperienceCreateDTO;
import com.talentmerge.dto.WorkExperienceDTO;
import com.talentmerge.exception.CandidateValidationException;
import com.talentmerge.model.Candidate;
import com.talentmerge.model.Education;
import com.talentmerge.model.WorkExperience;
import com.talentmerge.repository.CandidateRepository;
import com.talentmerge.repository.WorkExperienceRepository;
import com.talentmerge.repository.EducationRepository;
import com.talentmerge.repository.UserRepository;
import com.talentmerge.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final WorkExperienceRepository workExperienceRepository;
    private final EducationRepository educationRepository;
    private final UserRepository userRepository;

   private Long getCurrentUserId() {
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();
       if (auth == null || !(auth.getPrincipal() instanceof com.talentmerge.security.UserDetailsImpl u)) {
           throw new IllegalStateException("No authenticated user");
       }
       return u.getId();
   }

   private User getCurrentUser() {
       Long id = getCurrentUserId();
       return userRepository.findById(id)
               .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
   }

    /**
     * Create a new candidate manually
     */
    public CandidateResponseDTO createCandidate(CandidateCreateRequestDTO request) {
        log.info("Creating new candidate: {}", request.getName());
        
        // Business validation
        validateCandidateRequest(request);
        
        try {
            // Create candidate entity
            Candidate candidate = new Candidate();

            // Ownership: set current authenticated user as owner
            User owner = getCurrentUser();
            candidate.setOwner(owner);
            candidate.setName(request.getName().trim());
            candidate.setEmail(request.getEmail().trim().toLowerCase());
            candidate.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
            candidate.setSkills(request.getSkills() != null ? request.getSkills().trim() : null);
            candidate.setOriginalFilePath(null); // No file for manual entry
            
            // Add work experiences
            if (request.getWorkExperiences() != null) {
                for (WorkExperienceCreateDTO expDto : request.getWorkExperiences()) {
                    WorkExperience experience = new WorkExperience();
                    experience.setJobTitle(expDto.getJobTitle().trim());
                    experience.setCompany(expDto.getCompany().trim());
                    experience.setStartDate(expDto.getStartDate());
                    experience.setEndDate(expDto.getEndDate());
                    experience.setDescription(expDto.getDescription() != null ? expDto.getDescription().trim() : null);
                    
                    candidate.addWorkExperience(experience);
                }
            }
            
            // Add education entries
            if (request.getEducations() != null) {
                for (EducationCreateDTO eduDto : request.getEducations()) {
                    Education education = new Education();
                    education.setInstitution(eduDto.getInstitution().trim());
                    education.setDegree(eduDto.getDegree().trim());
                    education.setGraduationDate(eduDto.getGraduationDate());
                    
                    candidate.addEducation(education);
                }
            }
            
            // Save candidate
            Candidate savedCandidate = candidateRepository.save(candidate);
            log.info("Successfully created candidate with ID: {}", savedCandidate.getId());
            
            return convertToResponseDTO(savedCandidate);
            
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while creating candidate: {}", e.getMessage());
            throw new CandidateValidationException("A candidate with this email already exists", 
                Map.of("email", List.of("Email address is already in use")));
        } catch (Exception e) {
            log.error("Unexpected error while creating candidate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create candidate: " + e.getMessage());
        }
    }

    /**
     * Backward-compatible overload used by existing tests and callers expecting full details.
     */
    @Transactional(readOnly = true)
    public Page<CandidateResponseDTO> getAllCandidates(Pageable pageable) {
        Page<?> page = getAllCandidates(pageable, true);
        @SuppressWarnings("unchecked")
        Page<CandidateResponseDTO> typed = (Page<CandidateResponseDTO>) (Page<?>) page;
        return typed;
    }

    /**
     * Get all candidates with pagination
     */
    @Transactional(readOnly = true)
    public Page<?> getAllCandidates(Pageable pageable, boolean includeDetails) {
        log.info("Retrieving all candidates with pagination: {}", pageable);
        
        Long ownerId = getCurrentUserId();
        Page<Candidate> pageResult = candidateRepository.findByOwnerId(ownerId, pageable);

        if (!includeDetails) {
            List<Long> ids = pageResult.getContent().stream().map(Candidate::getId).toList();

            // Batch count associated rows in two queries
            var weCounts = workExperienceRepository.countByCandidateIds(ids);
            var edCounts = educationRepository.countByCandidateIds(ids);

            Map<Long, Long> weMap = new HashMap<>();
            for (Object[] row : weCounts) {
                weMap.put((Long) row[0], (Long) row[1]);
            }
            Map<Long, Long> edMap = new HashMap<>();
            for (Object[] row : edCounts) {
                edMap.put((Long) row[0], (Long) row[1]);
            }

            return pageResult.map(c -> new CandidateListItemDTO(
                c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getSkills(),
                weMap.getOrDefault(c.getId(), 0L), edMap.getOrDefault(c.getId(), 0L)
            ));
        } else {
            // Preload associations to avoid N+1 and map full detail DTOs
            List<Long> ids = pageResult.getContent().stream().map(Candidate::getId).toList();
            if (!ids.isEmpty()) {
                candidateRepository.findWithWorkExperiencesByIdIn(ids);
                candidateRepository.findWithEducationsByIdIn(ids);
                pageResult.getContent().forEach(c -> {
                    c.getWorkExperiences().size();
                    c.getEducations().size();
                });
            }
            return pageResult.map(this::convertToResponseDTO);
        }
    }

    /**
     * Get candidate by ID
     */
    @Transactional(readOnly = true)
    public Optional<CandidateResponseDTO> getCandidateById(Long id) {
        log.info("Retrieving candidate by ID: {}", id);
        
        Long ownerId = getCurrentUserId();
        return candidateRepository.findByIdAndOwnerId(id, ownerId)
                .map(this::convertToResponseDTO);
    }

    /**
     * Update existing candidate
     */
    public CandidateResponseDTO updateCandidate(Long id, CandidateCreateRequestDTO request) {
        log.info("Updating candidate with ID: {}", id);
        
        // Business validation
        validateCandidateRequest(request);
        
        Long ownerId = getCurrentUserId();
        Candidate existingCandidate = candidateRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found with ID: " + id));
        
        try {
            // Update basic information
            existingCandidate.setName(request.getName().trim());
            existingCandidate.setEmail(request.getEmail().trim().toLowerCase());
            existingCandidate.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
            existingCandidate.setSkills(request.getSkills() != null ? request.getSkills().trim() : null);
            
            // Clear existing relationships
            existingCandidate.getWorkExperiences().clear();
            existingCandidate.getEducations().clear();
            
            // Add updated work experiences
            if (request.getWorkExperiences() != null) {
                for (WorkExperienceCreateDTO expDto : request.getWorkExperiences()) {
                    WorkExperience experience = new WorkExperience();
                    experience.setJobTitle(expDto.getJobTitle().trim());
                    experience.setCompany(expDto.getCompany().trim());
                    experience.setStartDate(expDto.getStartDate());
                    experience.setEndDate(expDto.getEndDate());
                    experience.setDescription(expDto.getDescription() != null ? expDto.getDescription().trim() : null);
                    
                    existingCandidate.addWorkExperience(experience);
                }
            }
            
            // Add updated education entries
            if (request.getEducations() != null) {
                for (EducationCreateDTO eduDto : request.getEducations()) {
                    Education education = new Education();
                    education.setInstitution(eduDto.getInstitution().trim());
                    education.setDegree(eduDto.getDegree().trim());
                    education.setGraduationDate(eduDto.getGraduationDate());
                    
                    existingCandidate.addEducation(education);
                }
            }
            
            Candidate savedCandidate = candidateRepository.save(existingCandidate);
            log.info("Successfully updated candidate with ID: {}", savedCandidate.getId());
            
            return convertToResponseDTO(savedCandidate);
            
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating candidate: {}", e.getMessage());
            throw new CandidateValidationException("Email address is already in use by another candidate", 
                Map.of("email", List.of("Email address is already in use")));
        } catch (Exception e) {
            log.error("Unexpected error while updating candidate: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update candidate: " + e.getMessage());
        }
    }

    /**
     * Delete candidate by ID
     */
    public void deleteCandidate(Long id) {
        log.info("Deleting candidate with ID: {}", id);
        
        Long ownerId = getCurrentUserId();
        if (!candidateRepository.existsByIdAndOwnerId(id, ownerId)) {
            throw new IllegalArgumentException("Candidate not found with ID: " + id);
        }
        
        candidateRepository.deleteById(id);
        log.info("Successfully deleted candidate with ID: {}", id);
    }

    /**
     * Search candidates by email
     */
    @Transactional(readOnly = true)
    public Optional<CandidateResponseDTO> findByEmail(String email) {
        log.info("Searching for candidate by email: {}", email);
        
        Long ownerId = getCurrentUserId();
        return candidateRepository.findByEmailAndOwnerId(email.trim().toLowerCase(), ownerId)
                .map(this::convertToResponseDTO);
    }

    /**
     * Business validation for candidate requests
     */
    private void validateCandidateRequest(CandidateCreateRequestDTO request) {
        Map<String, List<String>> errors = new HashMap<>();
        
        // Additional business rules can be added here
        if (request.getWorkExperiences() != null) {
            for (int i = 0; i < request.getWorkExperiences().size(); i++) {
                WorkExperienceCreateDTO exp = request.getWorkExperiences().get(i);
                if (exp.getEndDate() != null && exp.getStartDate() != null && 
                    exp.getEndDate().isBefore(exp.getStartDate())) {
                    errors.computeIfAbsent("workExperiences[" + i + "].endDate", k -> new ArrayList<>())
                           .add("End date must be after start date");
                }
            }
        }
        
        // Check for duplicate work experiences (same company and overlapping dates)
        if (request.getWorkExperiences() != null && request.getWorkExperiences().size() > 1) {
            validateWorkExperienceOverlaps(request.getWorkExperiences(), errors);
        }
        
        if (!errors.isEmpty()) {
            throw new CandidateValidationException("Validation failed for candidate data", errors);
        }
    }

    /**
     * Validate work experience date overlaps
     */
    private void validateWorkExperienceOverlaps(List<WorkExperienceCreateDTO> experiences, Map<String, List<String>> errors) {
        for (int i = 0; i < experiences.size(); i++) {
            WorkExperienceCreateDTO exp1 = experiences.get(i);
            for (int j = i + 1; j < experiences.size(); j++) {
                WorkExperienceCreateDTO exp2 = experiences.get(j);
                
                // Check if same company with overlapping dates
                if (exp1.getCompany().equalsIgnoreCase(exp2.getCompany()) && 
                    datesOverlap(exp1.getStartDate(), exp1.getEndDate(), exp2.getStartDate(), exp2.getEndDate())) {
                    
                    errors.computeIfAbsent("workExperiences[" + j + "].company", k -> new ArrayList<>())
                           .add("Overlapping work experience at the same company");
                }
            }
        }
    }

    /**
     * Check if two date ranges overlap
     */
    private boolean datesOverlap(java.time.LocalDate start1, java.time.LocalDate end1, 
                                java.time.LocalDate start2, java.time.LocalDate end2) {
        // Treat null end date as "current" (far future)
        java.time.LocalDate effectiveEnd1 = end1 != null ? end1 : java.time.LocalDate.now().plusYears(100);
        java.time.LocalDate effectiveEnd2 = end2 != null ? end2 : java.time.LocalDate.now().plusYears(100);
        
        return start1.isBefore(effectiveEnd2) && start2.isBefore(effectiveEnd1);
    }

    /**
     * Convert Candidate entity to response DTO
     */
    private CandidateResponseDTO convertToResponseDTO(Candidate candidate) {
        List<WorkExperienceDTO> workExperienceDTOs = candidate.getWorkExperiences().stream()
                .map(exp -> new WorkExperienceDTO(
                        exp.getId(),
                        exp.getJobTitle(),
                        exp.getCompany(),
                        exp.getStartDate(),
                        exp.getEndDate(),
                        exp.getDescription()
                ))
                .collect(Collectors.toList());

        List<EducationDTO> educationDTOs = candidate.getEducations().stream()
                .map(edu -> new EducationDTO(
                        edu.getId(),
                        edu.getInstitution(),
                        edu.getDegree(),
                        edu.getGraduationDate()
                ))
                .collect(Collectors.toList());

        return new CandidateResponseDTO(
                candidate.getId(),
                candidate.getName(),
                candidate.getEmail(),
                candidate.getPhone(),
                candidate.getSkills(),
                null, // No raw text for manual entries
                candidate.getOriginalFilePath(),
                workExperienceDTOs,
                educationDTOs
        );
    }
}