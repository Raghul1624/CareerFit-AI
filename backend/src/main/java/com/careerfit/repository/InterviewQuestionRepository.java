package com.careerfit.repository;

import com.careerfit.entity.InterviewLevel;
import com.careerfit.entity.InterviewQuestion;
import com.careerfit.entity.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    /**
     * A question may only be offered when it can actually be served: a multiple-choice question needs a correct
     * option and at least two options, a coding problem needs its problem data plus at least one sample and one hidden
     * test case, a text question needs a rubric of at least three concepts. Availability and selection both use this
     * predicate, so an assessment is never shown as available when its questions are incomplete.
     */
    String SERVABLE = """
        q.active = true and (
          (q.type in (com.careerfit.entity.InterviewQuestionType.SINGLE_CHOICE, com.careerfit.entity.InterviewQuestionType.MULTIPLE_CHOICE)
             and exists (select o.id from InterviewOption o where o.question = q and o.correct = true)
             and (select count(o2.id) from InterviewOption o2 where o2.question = q) >= 2)
          or (q.type = com.careerfit.entity.InterviewQuestionType.CODING
             and exists (select p.questionId from InterviewCodingProblem p where p.questionId = q.id)
             and exists (select t.id from InterviewTestCase t where t.questionId = q.id and t.hidden = true)
             and exists (select t2.id from InterviewTestCase t2 where t2.questionId = q.id and t2.hidden = false))
          or (q.type = com.careerfit.entity.InterviewQuestionType.TEXT
             and (select count(c.id) from InterviewRubricConcept c where c.questionId = q.id) >= 3))
        """;

    /** Number of servable questions per role, round and level. Columns: roleId, round, level, count. */
    @Query("select r.id, q.round, q.level, count(distinct q.id) from InterviewQuestion q join q.roles r "
        + "where r.active = true and " + SERVABLE + " group by r.id, q.round, q.level")
    List<Object[]> countServable();

    /** Selection pool for one role, round and level. Columns: id, topic. */
    @Query("select distinct q.id, q.topic from InterviewQuestion q join q.roles r "
        + "where r.id = :roleId and q.round = :round and q.level = :level and " + SERVABLE)
    List<Object[]> findPool(@Param("roleId") Long roleId, @Param("round") InterviewRound round,
                            @Param("level") InterviewLevel level);

    /** Loads the chosen questions together with their options in one query (no N+1). */
    @Query("select distinct q from InterviewQuestion q left join fetch q.options where q.id in :ids")
    List<InterviewQuestion> findAllWithOptionsByIdIn(@Param("ids") Collection<Long> ids);
}
