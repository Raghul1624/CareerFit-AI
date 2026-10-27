package com.careerfit.repository;

import com.careerfit.entity.AttemptStatus;
import com.careerfit.entity.InterviewAttempt;
import com.careerfit.entity.InterviewLevel;
import com.careerfit.entity.InterviewRound;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterviewAttemptRepository extends JpaRepository<InterviewAttempt, Long>, JpaSpecificationExecutor<InterviewAttempt> {

    /** The ONLY way attempts are loaded for a request: always scoped to the owner, so other users' attempts are invisible. */
    @EntityGraph(attributePaths = "role")
    Optional<InterviewAttempt> findByIdAndUserId(Long id, Long userId);

    Optional<InterviewAttempt> findFirstByUserIdAndRoleIdAndRoundAndLevelAndStatusOrderByStartedAtDesc(
        Long userId, Long roleId, InterviewRound round, InterviewLevel level, AttemptStatus status);

    /** Questions the user already received in earlier attempts of the same role, round and level. */
    @Query("select distinct i.question.id from InterviewAttemptItem i where i.attempt.user.id = :uid "
        + "and i.attempt.role.id = :roleId and i.attempt.round = :round and i.attempt.level = :level")
    List<Long> findSeenQuestionIds(@Param("uid") Long userId, @Param("roleId") Long roleId,
                                   @Param("round") InterviewRound round, @Param("level") InterviewLevel level);

    @EntityGraph(attributePaths = "role")
    List<InterviewAttempt> findTop5ByUserIdOrderByStartedAtDesc(Long userId);

    long countByUserId(Long userId);

    List<InterviewAttempt> findByStatusAndExpiresAtBefore(AttemptStatus status, java.time.Instant cutoff);
}
