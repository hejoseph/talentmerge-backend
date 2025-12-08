package com.talentmerge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentmerge.dto.CandidateCreateRequestDTO;
import com.talentmerge.dto.CandidateResponseDTO;
import com.talentmerge.dto.EducationCreateDTO;
import com.talentmerge.dto.WorkExperienceCreateDTO;
import com.talentmerge.service.CandidateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(CandidateController.class)
class CandidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CandidateService candidateService;

    @Autowired
    private ObjectMapper objectMapper;

    private CandidateCreateRequestDTO validRequest;
    private CandidateResponseDTO mockResponse;

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

        // Setup mock response
        mockResponse = new CandidateResponseDTO();
        mockResponse.setId(1L);
        mockResponse.setName("John Doe");
        mockResponse.setEmail("john.doe@example.com");
        mockResponse.setPhone("+1234567890");
        mockResponse.setSkills("Java, Spring Boot, React");
        mockResponse.setWorkExperiences(new ArrayList<>());
        mockResponse.setEducations(new ArrayList<>());
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCandidate_WithValidData_ShouldReturnCreated() throws Exception {
        // Given
        when(candidateService.createCandidate(any(CandidateCreateRequestDTO.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("+1234567890"));

        verify(candidateService, times(1)).createCandidate(any(CandidateCreateRequestDTO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createCandidate_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given - invalid request with empty name
        CandidateCreateRequestDTO invalidRequest = new CandidateCreateRequestDTO();
        invalidRequest.setName(""); // Invalid: empty name
        invalidRequest.setEmail("invalid-email"); // Invalid: bad email format
        invalidRequest.setWorkExperiences(new ArrayList<>());
        invalidRequest.setEducations(new ArrayList<>());

        // When & Then
        mockMvc.perform(post("/api/candidates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(candidateService, never()).createCandidate(any(CandidateCreateRequestDTO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllCandidates_ShouldReturnPagedResults() throws Exception {
        // Given
        List<CandidateResponseDTO> candidates = List.of(mockResponse);
        Page<CandidateResponseDTO> candidatePage = new PageImpl<>(candidates, PageRequest.of(0, 10), 1);
        
        when(candidateService.getAllCandidates(any())).thenReturn(candidatePage);

        // When & Then
        mockMvc.perform(get("/api/candidates")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "name")
                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("John Doe"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.size").value(10));

        verify(candidateService, times(1)).getAllCandidates(any());
    }

    @Test
    @WithMockUser(roles = "USER") 
    void getCandidateById_WithExistingId_ShouldReturnCandidate() throws Exception {
        // Given
        Long candidateId = 1L;
        when(candidateService.getCandidateById(candidateId)).thenReturn(Optional.of(mockResponse));

        // When & Then
        mockMvc.perform(get("/api/candidates/{id}", candidateId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(candidateService, times(1)).getCandidateById(candidateId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCandidateById_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        // Given
        Long candidateId = 999L;
        when(candidateService.getCandidateById(candidateId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/candidates/{id}", candidateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CANDIDATE_NOT_FOUND"));

        verify(candidateService, times(1)).getCandidateById(candidateId);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getCandidateById_WithInvalidId_ShouldReturnBadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/candidates/{id}", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ID"));

        verify(candidateService, never()).getCandidateById(anyLong());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateCandidate_WithValidData_ShouldReturnUpdatedCandidate() throws Exception {
        // Given
        Long candidateId = 1L;
        when(candidateService.updateCandidate(eq(candidateId), any(CandidateCreateRequestDTO.class)))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(put("/api/candidates/{id}", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(candidateService, times(1)).updateCandidate(eq(candidateId), any(CandidateCreateRequestDTO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateCandidate_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        // Given
        Long candidateId = 999L;
        when(candidateService.updateCandidate(eq(candidateId), any(CandidateCreateRequestDTO.class)))
                .thenThrow(new IllegalArgumentException("Candidate not found"));

        // When & Then
        mockMvc.perform(put("/api/candidates/{id}", candidateId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CANDIDATE_NOT_FOUND"));

        verify(candidateService, times(1)).updateCandidate(eq(candidateId), any(CandidateCreateRequestDTO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void deleteCandidate_WithExistingId_ShouldReturnNoContent() throws Exception {
        // Given
        Long candidateId = 1L;
        doNothing().when(candidateService).deleteCandidate(candidateId);

        // When & Then
        mockMvc.perform(delete("/api/candidates/{id}", candidateId))
                .andExpect(status().isNoContent());

        verify(candidateService, times(1)).deleteCandidate(candidateId);
    }

    @Test
    void deleteCandidate_WithNonExistingId_ShouldReturnNotFound() throws Exception {
        // Given
        Long candidateId = 999L;
        doThrow(new IllegalArgumentException("Candidate not found"))
                .when(candidateService).deleteCandidate(candidateId);

        // When & Then
        mockMvc.perform(delete("/api/candidates/{id}", candidateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CANDIDATE_NOT_FOUND"));

        verify(candidateService, times(1)).deleteCandidate(candidateId);
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnCandidate() throws Exception {
        // Given
        String email = "john.doe@example.com";
        when(candidateService.findByEmail(email)).thenReturn(Optional.of(mockResponse));

        // When & Then
        mockMvc.perform(get("/api/candidates/search/email")
                .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(candidateService, times(1)).findByEmail(email);
    }

    @Test
    void findByEmail_WithNonExistingEmail_ShouldReturnNotFound() throws Exception {
        // Given
        String email = "nonexistent@example.com";
        when(candidateService.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/candidates/search/email")
                .param("email", email))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CANDIDATE_NOT_FOUND"));

        verify(candidateService, times(1)).findByEmail(email);
    }

    @Test
    void checkEmailExists_WithExistingEmail_ShouldReturnTrue() throws Exception {
        // Given
        String email = "john.doe@example.com";
        when(candidateService.findByEmail(email)).thenReturn(Optional.of(mockResponse));

        // When & Then
        mockMvc.perform(get("/api/candidates/check-email")
                .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.email").value(email));

        verify(candidateService, times(1)).findByEmail(email);
    }

    @Test
    void checkEmailExists_WithNonExistingEmail_ShouldReturnFalse() throws Exception {
        // Given
        String email = "new@example.com";
        when(candidateService.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/candidates/check-email")
                .param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.email").value(email));

        verify(candidateService, times(1)).findByEmail(email);
    }

    @Test
    void getCandidateStats_ShouldReturnStatistics() throws Exception {
        // Given
        List<CandidateResponseDTO> candidates = List.of(mockResponse);
        Page<CandidateResponseDTO> candidatePage = new PageImpl<>(candidates, PageRequest.of(0, 1), 5);
        when(candidateService.getAllCandidates(any())).thenReturn(candidatePage);

        // When & Then
        mockMvc.perform(get("/api/candidates/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCandidates").value(5));

        verify(candidateService, times(1)).getAllCandidates(any());
    }
}