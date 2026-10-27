package com.careerfit.controller;

import com.careerfit.dto.CareerDtos.*;
import com.careerfit.service.CareerPathService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/career-paths")
@RequiredArgsConstructor
public class CareerPathController {

    private final CareerPathService service;

    @GetMapping
    public List<CareerPathSummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CareerPathDetail get(@PathVariable Long id) {
        return service.get(id);
    }
}
