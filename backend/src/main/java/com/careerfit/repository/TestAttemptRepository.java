package com.careerfit.repository;

import com.careerfit.entity.AttemptStatus;
import com.careerfit.entity.TestAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    Optional<TestAttempt> findByIdAndUserId(Long id, Long userId);

    Optional<TestAttempt> findFirstByUserIdAndMockTestIdAndStatusOrderByStartedAtDesc(Long userId, Long mockTestId, AttemptStatus status);

    long countByUserIdAndMockTestId(Long userId, Long mockTestId);

    long countByUserIdAndStatusNot(Long userId, AttemptStatus status);

    @EntityGraph(attributePaths = {"mockTest", "mockTest.careerPath"})
    Page<TestAttempt> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"mockTest", "mockTest.careerPath"})
    List<TestAttempt> findTop5ByUserIdAndStatusNotOrderByStartedAtDesc(Long userId, AttemptStatus status);

    @EntityGraph(attributePaths = {"mockTest", "mockTest.careerPath"})
    List<TestAttempt> findTop50ByUserIdAndStatusNotOrderByStartedAtDesc(Long userId, AttemptStatus status);

    List<TestAttempt> findByStatusAndExpiresAtBefore(AttemptStatus status, Instant cutoff);

    @Query("select avg(a.percentage) from TestAttempt a where a.user.id = :uid and a.status <> :excluded")
    Double averagePercentage(@Param("uid") Long uid, @Param("excluded") AttemptStatus excluded);

    @Query("select max(a.percentage) from TestAttempt a where a.user.id = :uid and a.status <> :excluded")
    Double bestPercentage(@Param("uid") Long uid, @Param("excluded") AttemptStatus excluded);

    /** Rows: [skillName, correctCount, totalCount] across all finished attempts of the user. */
    @Query("""
        select q.skill.name, sum(case when aq.correct = true then 1 else 0 end), count(aq)
        from TestAttempt a join a.items aq join aq.question q
        where a.user.id = :uid and a.status <> :excluded
        group by q.skill.name
        order by q.skill.name
        """)
    List<Object[]> skillPerformance(@Param("uid") Long uid, @Param("excluded") AttemptStatus excluded);

    /**
     * Ids of the questions this user has already been given in earlier attempts of the same test.
     * The selector uses them as a "prefer something new" hint so a retake does not repeat questions
     * unless the bank has nothing else left.
     */
    @Query("""
        select distinct aq.question.id
        from TestAttemptQuestion aq
        where aq.attempt.user.id = :uid and aq.attempt.mockTest.id = :testId
        """)
    List<Long> findQuestionIdsSeenByUser(@Param("uid") Long uid, @Param("testId") Long testId);
}
