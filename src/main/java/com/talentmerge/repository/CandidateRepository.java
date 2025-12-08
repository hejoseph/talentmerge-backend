package com.talentmerge.repository;

import com.talentmerge.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
