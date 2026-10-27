package com.careerfit.repository;

import com.careerfit.entity.InterviewTestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InterviewTestCaseRepository extends JpaRepository<InterviewTestCase, Long> {
    List<InterviewTestCase> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);

    List<InterviewTestCase> findByQuestionIdInOrderByQuestionIdAscDisplayOrderAsc(Collection<Long> questionIds);
}
