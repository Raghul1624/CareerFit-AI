package com.careerfit.repository;

import com.careerfit.entity.InterviewRubricConcept;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InterviewRubricRepository extends JpaRepository<InterviewRubricConcept, Long> {
    List<InterviewRubricConcept> findByQuestionIdInOrderByQuestionIdAscDisplayOrderAsc(Collection<Long> questionIds);
}
