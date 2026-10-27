package com.careerfit.repository;

import com.careerfit.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySkillIdInAndActiveTrue(Collection<Long> skillIds);

    long countBySkillIdInAndActiveTrue(Collection<Long> skillIds);

    /**
     * Selection pool: id, skill, topic and difficulty only. No question text, options or answer key
     * is loaded, and there is a single query no matter how many questions the pool holds.
     */
    @Query("""
        select new com.careerfit.repository.QuestionRef(q.id, q.skill.id, q.topic, q.difficulty)
        from Question q
        where q.active = true and q.skill.id in :skillIds
        """)
    List<QuestionRef> findPool(@Param("skillIds") Collection<Long> skillIds);

    /** Question counts per skill, topic and difficulty for the whole bank (used for test summaries). */
    @Query("""
        select new com.careerfit.repository.PoolStat(q.skill.id, q.topic, q.difficulty, count(q))
        from Question q
        where q.active = true
        group by q.skill.id, q.topic, q.difficulty
        """)
    List<PoolStat> poolStats();

    /**
     * Loads exactly the selected questions together with their skill and options in ONE query,
     * so building an attempt never triggers a query per question (no N+1).
     */
    @Query("""
        select distinct q from Question q
        join fetch q.skill
        left join fetch q.options
        where q.id in :ids
        """)
    List<Question> findAllWithOptionsByIdIn(@Param("ids") Collection<Long> ids);
}
