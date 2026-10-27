package com.careerfit.service;

import com.careerfit.dto.CareerDtos.SkillDto;
import com.careerfit.entity.Skill;
import com.careerfit.entity.User;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.SkillRepository;
import com.careerfit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skills;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<SkillDto> all() {
        return skills.findAllByOrderByNameAsc().stream().map(SkillService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<SkillDto> mine() {
        User user = currentUser.get();
        return user.getDeclaredSkills().stream().sorted(Comparator.comparing(Skill::getName))
            .map(SkillService::toDto).toList();
    }

    @Transactional
    public List<SkillDto> setMine(List<Long> skillIds) {
        User user = currentUser.get();
        List<Long> distinct = skillIds.stream().distinct().toList();
        List<Skill> found = skills.findAllById(distinct);
        if (found.size() != distinct.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "One or more skills do not exist");
        }
        user.getDeclaredSkills().clear();
        user.getDeclaredSkills().addAll(found);
        return found.stream().sorted(Comparator.comparing(Skill::getName)).map(SkillService::toDto).toList();
    }

    private static SkillDto toDto(Skill s) {
        return new SkillDto(s.getId(), s.getName(), s.getDescription());
    }
}
