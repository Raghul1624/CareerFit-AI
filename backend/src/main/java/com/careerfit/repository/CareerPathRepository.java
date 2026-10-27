package com.careerfit.repository;

import com.careerfit.entity.CareerPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CareerPathRepository extends JpaRepository<CareerPath, Long> {

    @Query("select p from CareerPath p join fetch p.category c where p.active = true order by c.displayOrder, p.name")
    List<CareerPath> findAllActiveWithCategory();

    Optional<CareerPath> findByIdAndActiveTrue(Long id);
}
