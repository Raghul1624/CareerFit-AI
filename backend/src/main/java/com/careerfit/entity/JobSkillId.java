package com.careerfit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class JobSkillId implements Serializable {
    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "skill_id")
    private Long skillId;
}
