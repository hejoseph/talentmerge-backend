package com.talentmerge.repository;

import com.talentmerge.model.WorkExperience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface WorkExperienceRepository extends JpaRepository<WorkExperience, Long> {
    @Query("select we.candidate.id as candidateId, count(we) as cnt from WorkExperience we where we.candidate.id in :ids group by we.candidate.id")
    java.util.List<Object[]> countByCandidateIds(@Param("ids") java.util.List<Long> ids);
}
