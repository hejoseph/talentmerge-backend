package com.talentmerge.repository;

import com.talentmerge.model.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {
    
    /**
     * Find candidate by email (case-insensitive)
     */
    Optional<Candidate> findByEmail(String email);
    
    /**
     * Check if candidate exists by email (case-insensitive)
     */
    boolean existsByEmail(String email);
    
    /**
     * Search candidates by name containing the search term (case-insensitive)
     */
    @Query("SELECT c FROM Candidate c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Candidate> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Find candidates by skills containing specific skill (case-insensitive)
     */
    @Query("SELECT c FROM Candidate c WHERE LOWER(c.skills) LIKE LOWER(CONCAT('%', :skill, '%'))")
    List<Candidate> findBySkillsContainingIgnoreCase(@Param("skill") String skill);
    
    /**
     * Find candidates by company in work experience
     */
    @Query("SELECT DISTINCT c FROM Candidate c JOIN c.workExperiences we WHERE LOWER(we.company) LIKE LOWER(CONCAT('%', :company, '%'))")
    List<Candidate> findByWorkExperienceCompanyContainingIgnoreCase(@Param("company") String company);
    
    /**
     * Find candidates by education institution
     */
    @Query("SELECT DISTINCT c FROM Candidate c JOIN c.educations e WHERE LOWER(e.institution) LIKE LOWER(CONCAT('%', :institution, '%'))")
    List<Candidate> findByEducationInstitutionContainingIgnoreCase(@Param("institution") String institution);
}
