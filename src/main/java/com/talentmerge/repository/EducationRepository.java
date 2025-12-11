package com.talentmerge.repository;

import com.talentmerge.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface EducationRepository extends JpaRepository<Education, Long> {
    @Query("select e.candidate.id as candidateId, count(e) as cnt from Education e where e.candidate.id in :ids group by e.candidate.id")
    java.util.List<Object[]> countByCandidateIds(@Param("ids") java.util.List<Long> ids);
}
