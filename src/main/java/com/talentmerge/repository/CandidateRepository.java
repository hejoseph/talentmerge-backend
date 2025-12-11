package com.talentmerge.repository;

import com.talentmerge.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Optional<Candidate> findByEmailAndOwnerId(String email, Long ownerId);
    
    /**
     * Check if candidate exists by email (case-insensitive)
     */
    boolean existsByEmail(String email);
    boolean existsByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Fetch candidates with work experiences in a single query
     */
    @Query("select distinct c from Candidate c left join fetch c.workExperiences where c.id in :ids")
    List<Candidate> findWithWorkExperiencesByIdIn(@Param("ids") List<Long> ids);

    /**
     * Fetch candidates with educations in a single query
     */
    @Query("select distinct c from Candidate c left join fetch c.educations where c.id in :ids")
    List<Candidate> findWithEducationsByIdIn(@Param("ids") List<Long> ids);

    Page<Candidate> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<Candidate> findByIdAndOwnerId(Long id, Long ownerId);
}
