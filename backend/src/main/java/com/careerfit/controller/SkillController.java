package com.careerfit.controller;

import com.careerfit.dto.CareerDtos.SkillDto;
import com.careerfit.dto.Phase5Dtos.SetSkillsRequest;
import com.careerfit.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SkillController {

    private final SkillService service;

    @GetMapping("/api/skills")
    public List<SkillDto> all() {
        return service.all();
    }

    @GetMapping("/api/profile/skills")
    public List<SkillDto> mine() {
        return service.mine();
    }

    @PutMapping("/api/profile/skills")
    public List<SkillDto> setMine(@Valid @RequestBody SetSkillsRequest request) {
        return service.setMine(request.skillIds());
    }
}
