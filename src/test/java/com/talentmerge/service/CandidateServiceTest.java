package com.talentmerge.service;

import com.talentmerge.dto.CandidateCreateRequestDTO;
import com.talentmerge.dto.CandidateResponseDTO;
import com.talentmerge.dto.EducationCreateDTO;
import com.talentmerge.dto.WorkExperienceCreateDTO;
import com.talentmerge.exception.CandidateValidationException;
import com.talentmerge.model.Candidate;
import com.talentmerge.model.Education;
import com.talentmerge.model.WorkExperience;
import com.talentmerge.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateServiceTest {

    @Mock
    private CandidateRepository candidateRepository;

    @InjectMocks
    private CandidateService candidateService;

    private CandidateCreateRequestDTO validRequest;
    private Candidate mockCandidate;

    @BeforeEach
    void setUp() {
        // Setup valid request
        validRequest = new CandidateCreateRequestDTO();
        validRequest.setName("John Doe");
        validRequest.setEmail("john.doe@example.com");
        validRequest.setPhone("+1234567890");
        validRequest.setSkills("Java, Spring Boot, React");
        
        // Setup work experience
        WorkExperienceCreateDTO workExp = new WorkExperienceCreateDTO();
        workExp.setJobTitle("Software Developer");
        workExp.setCompany("Tech Corp");
        workExp.setStartDate(LocalDate.of(2020, 1, 1));
        workExp.setEndDate(LocalDate.of(2022, 12, 31));
        workExp.setDescription("Developed web applications");
        List<WorkExperienceCreateDTO> workExps = new ArrayList<>();
        workExps.add(workExp);
        validRequest.setWorkExperiences(workExps);
        
        // Setup education
        EducationCreateDTO education = new EducationCreateDTO();
        education.setInstitution("University of Technology");
        education.setDegree("Bachelor of Computer Science");
        education.setGraduationDate(LocalDate.of(2019, 6, 1));
        List<EducationCreateDTO> educations = new ArrayList<>();
        educations.add(education);
        validRequest.setEducations(educations);

        // Setup mock candidate
        mockCandidate = new Candidate();
        mockCandidate.setId(1L);
        mockCandidate.setName("John Doe");
        mockCandidate.setEmail("john.doe@example.com");
        mockCandidate.setPhone("+1234567890");
        mockCandidate.setSkills("Java, Spring Boot, React");
        
        WorkExperience mockWorkExp = new WorkExperience();
        mockWorkExp.setId(1L);
        mockWorkExp.setJobTitle("Software Developer");
        mockWorkExp.setCompany("Tech Corp");
        mockWorkExp.setStartDate(LocalDate.of(2020, 1, 1));
        mockWorkExp.setEndDate(LocalDate.of(2022, 12, 31));
        mockWorkExp.setDescription("Developed web applications");
        List<WorkExperience> mockWorkExps = new ArrayList<>();
        mockWorkExps.add(mockWorkExp);
        mockCandidate.setWorkExperiences(mockWorkExps);
        
        Education mockEducation = new Education();
        mockEducation.setId(1L);
        mockEducation.setInstitution("University of Technology");
        mockEducation.setDegree("Bachelor of Computer Science");
        mockEducation.setGraduationDate(LocalDate.of(2019, 6, 1));
        List<Education> mockEducations = new ArrayList<>();
        mockEducations.add(mockEducation);
        mockCandidate.setEducations(mockEducations);
    }

    @Test
    void createCandidate_WithValidData_ShouldReturnCandidateResponseDTO() {
        // Given
        when(candidateRepository.save(any(Candidate.class))).thenReturn(mockCandidate);

        // When
        CandidateResponseDTO result = candidateService.createCandidate(validRequest);

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john.doe@example.com", result.getEmail());
        assertEquals("+1234567890", result.getPhone());
        assertEquals("Java, Spring Boot, React", result.getSkills());
        assertEquals(1, result.getWorkExperiences().size());
        assertEquals(1, result.getEducations().size());
        
        verify(candidateRepository, times(1)).save(any(Candidate.class));
    }

    @Test
    void createCandidate_WithDuplicateEmail_ShouldThrowCandidateValidationException() {
        // Given
        when(candidateRepository.save(any(Candidate.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        // When & Then
        CandidateValidationException exception = assertThrows(
            CandidateValidationException.class,
            () -> candidateService.createCandidate(validRequest)
        );
        
        assertTrue(exception.getMessage().contains("email already exists"));
        assertNotNull(exception.getFieldErrors());
        assertTrue(exception.getFieldErrors().containsKey("email"));
    }

    @Test
    void createCandidate_WithOverlappingWorkExperience_ShouldThrowCandidateValidationException() {
        // Given
        WorkExperienceCreateDTO overlappingExp = new WorkExperienceCreateDTO();
        overlappingExp.setJobTitle("Senior Developer");
        overlappingExp.setCompany("Tech Corp"); // Same company
        overlappingExp.setStartDate(LocalDate.of(2021, 6, 1)); // Overlaps with existing
        overlappingExp.setEndDate(LocalDate.of(2023, 12, 31));
        overlappingExp.setDescription("Senior role");
        
        List<WorkExperienceCreateDTO> workExps = new ArrayList<>(validRequest.getWorkExperiences());
        workExps.add(overlappingExp);
        validRequest.setWorkExperiences(workExps);

        // When & Then
        CandidateValidationException exception = assertThrows(
            CandidateValidationException.class,
            () -> candidateService.createCandidate(validRequest)
        );
        
        assertTrue(exception.getMessage().contains("Validation failed"));
        assertNotNull(exception.getFieldErrors());
    }

    @Test
    void createCandidate_WithInvalidDateRange_ShouldThrowCandidateValidationException() {
        // Given
        WorkExperienceCreateDTO invalidExp = new WorkExperienceCreateDTO();
        invalidExp.setJobTitle("Developer");
        invalidExp.setCompany("Another Corp");
        invalidExp.setStartDate(LocalDate.of(2022, 1, 1));
        invalidExp.setEndDate(LocalDate.of(2021, 1, 1)); // End before start
        invalidExp.setDescription("Invalid dates");
        
        List<WorkExperienceCreateDTO> invalidExps = new ArrayList<>();
        invalidExps.add(invalidExp);
        validRequest.setWorkExperiences(invalidExps);

        // When & Then
        CandidateValidationException exception = assertThrows(
            CandidateValidationException.class,
            () -> candidateService.createCandidate(validRequest)
        );
        
        assertNotNull(exception.getFieldErrors());
    }

    @Test
    void getAllCandidates_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Candidate> candidates = List.of(mockCandidate);
        Page<Candidate> candidatePage = new PageImpl<>(candidates, pageable, 1);
        
        when(candidateRepository.findAll(pageable)).thenReturn(candidatePage);

        // When
        Page<CandidateResponseDTO> result = candidateService.getAllCandidates(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertEquals("John Doe", result.getContent().get(0).getName());
        
        verify(candidateRepository, times(1)).findAll(pageable);
    }

    @Test
    void getCandidateById_WithExistingId_ShouldReturnCandidate() {
        // Given
        Long candidateId = 1L;
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));

        // When
        Optional<CandidateResponseDTO> result = candidateService.getCandidateById(candidateId);

        // Then
        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().getName());
        assertEquals("john.doe@example.com", result.get().getEmail());
        
        verify(candidateRepository, times(1)).findById(candidateId);
    }

    @Test
    void getCandidateById_WithNonExistingId_ShouldReturnEmpty() {
        // Given
        Long candidateId = 999L;
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        // When
        Optional<CandidateResponseDTO> result = candidateService.getCandidateById(candidateId);

        // Then
        assertTrue(result.isEmpty());
        
        verify(candidateRepository, times(1)).findById(candidateId);
    }

    @Test
    void updateCandidate_WithValidData_ShouldReturnUpdatedCandidate() {
        // Given
        Long candidateId = 1L;
        CandidateCreateRequestDTO updateRequest = new CandidateCreateRequestDTO();
        updateRequest.setName("Jane Doe Updated");
        updateRequest.setEmail("jane.updated@example.com");
        updateRequest.setPhone("+9876543210");
        updateRequest.setSkills("Python, Django, Vue.js");
        updateRequest.setWorkExperiences(new ArrayList<>());
        updateRequest.setEducations(new ArrayList<>());
        
        Candidate updatedCandidate = new Candidate();
        updatedCandidate.setId(candidateId);
        updatedCandidate.setName("Jane Doe Updated");
        updatedCandidate.setEmail("jane.updated@example.com");
        updatedCandidate.setPhone("+9876543210");
        updatedCandidate.setSkills("Python, Django, Vue.js");
        updatedCandidate.setWorkExperiences(new ArrayList<>());
        updatedCandidate.setEducations(new ArrayList<>());
        
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(mockCandidate));
        when(candidateRepository.save(any(Candidate.class))).thenReturn(updatedCandidate);

        // When
        CandidateResponseDTO result = candidateService.updateCandidate(candidateId, updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("Jane Doe Updated", result.getName());
        assertEquals("jane.updated@example.com", result.getEmail());
        assertEquals("+9876543210", result.getPhone());
        
        verify(candidateRepository, times(1)).findById(candidateId);
        verify(candidateRepository, times(1)).save(any(Candidate.class));
    }

    @Test
    void updateCandidate_WithNonExistingId_ShouldThrowIllegalArgumentException() {
        // Given
        Long candidateId = 999L;
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> candidateService.updateCandidate(candidateId, validRequest)
        );
        
        assertTrue(exception.getMessage().contains("Candidate not found"));
        
        verify(candidateRepository, times(1)).findById(candidateId);
        verify(candidateRepository, never()).save(any(Candidate.class));
    }

    @Test
    void deleteCandidate_WithExistingId_ShouldDeleteSuccessfully() {
        // Given
        Long candidateId = 1L;
        when(candidateRepository.existsById(candidateId)).thenReturn(true);
        doNothing().when(candidateRepository).deleteById(candidateId);

        // When
        assertDoesNotThrow(() -> candidateService.deleteCandidate(candidateId));

        // Then
        verify(candidateRepository, times(1)).existsById(candidateId);
        verify(candidateRepository, times(1)).deleteById(candidateId);
    }

    @Test
    void deleteCandidate_WithNonExistingId_ShouldThrowIllegalArgumentException() {
        // Given
        Long candidateId = 999L;
        when(candidateRepository.existsById(candidateId)).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> candidateService.deleteCandidate(candidateId)
        );
        
        assertTrue(exception.getMessage().contains("Candidate not found"));
        
        verify(candidateRepository, times(1)).existsById(candidateId);
        verify(candidateRepository, never()).deleteById(anyLong());
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnCandidate() {
        // Given
        String email = "john.doe@example.com";
        when(candidateRepository.findByEmail(email)).thenReturn(Optional.of(mockCandidate));

        // When
        Optional<CandidateResponseDTO> result = candidateService.findByEmail(email);

        // Then
        assertTrue(result.isPresent());
        assertEquals("John Doe", result.get().getName());
        assertEquals(email, result.get().getEmail());
        
        verify(candidateRepository, times(1)).findByEmail(email);
    }

    @Test
    void findByEmail_WithNonExistingEmail_ShouldReturnEmpty() {
        // Given
        String email = "nonexistent@example.com";
        when(candidateRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        Optional<CandidateResponseDTO> result = candidateService.findByEmail(email);

        // Then
        assertTrue(result.isEmpty());
        
        verify(candidateRepository, times(1)).findByEmail(email);
    }

    @Test
    void createCandidate_WithCurrentPosition_ShouldHandleNullEndDate() {
        // Given
        WorkExperienceCreateDTO currentJob = new WorkExperienceCreateDTO();
        currentJob.setJobTitle("Current Developer");
        currentJob.setCompany("Current Corp");
        currentJob.setStartDate(LocalDate.of(2023, 1, 1));
        currentJob.setEndDate(null); // Current position
        currentJob.setDescription("Current role");
        
        List<WorkExperienceCreateDTO> currentJobs = new ArrayList<>();
        currentJobs.add(currentJob);
        validRequest.setWorkExperiences(currentJobs);
        
        Candidate candidateWithCurrentJob = new Candidate();
        candidateWithCurrentJob.setId(1L);
        candidateWithCurrentJob.setName("John Doe");
        candidateWithCurrentJob.setEmail("john.doe@example.com");
        
        when(candidateRepository.save(any(Candidate.class))).thenReturn(candidateWithCurrentJob);

        // When
        CandidateResponseDTO result = candidateService.createCandidate(validRequest);

        // Then
        assertNotNull(result);
        verify(candidateRepository, times(1)).save(any(Candidate.class));
    }

    @Test
    void createCandidate_WithTrimmedData_ShouldNormalizeInput() {
        // Given
        validRequest.setName("  John Doe  ");
        validRequest.setEmail("  JOHN.DOE@EXAMPLE.COM  ");
        validRequest.setPhone("  +1234567890  ");
        validRequest.setSkills("  Java, Spring Boot  ");
        
        when(candidateRepository.save(any(Candidate.class))).thenReturn(mockCandidate);

        // When
        CandidateResponseDTO result = candidateService.createCandidate(validRequest);

        // Then
        assertNotNull(result);
        verify(candidateRepository, times(1)).save(argThat(candidate -> 
            candidate.getName().equals("John Doe") &&
            candidate.getEmail().equals("john.doe@example.com") &&
            candidate.getPhone().equals("+1234567890") &&
            candidate.getSkills().equals("Java, Spring Boot")
        ));
    }
}