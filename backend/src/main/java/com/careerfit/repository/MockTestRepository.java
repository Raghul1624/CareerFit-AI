package com.careerfit.repository;

import com.careerfit.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MockTestRepository extends JpaRepository<MockTest, Long> {

    List<MockTest> findByActiveTrueOrderByTitleAsc();

    /**
     * All active tests with their skills, career path and the career path's skills in one query, so
     * listing the tests does not run extra queries per test. (Both collections are Sets, so fetching
     * the two of them together is allowed.)
     */
    @Query("""
        select distinct t from MockTest t
        left join fetch t.skills
        left join fetch t.careerPath cp
        left join fetch cp.skills
        where t.active = true
        order by t.title asc
        """)
    List<MockTest> findAllActiveWithSkills();
}
