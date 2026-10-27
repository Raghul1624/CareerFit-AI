package com.careerfit.controller;

import com.careerfit.dto.ProfileDtos.*;
import com.careerfit.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileDto get() {
        return profileService.get();
    }

    @PutMapping
    public ProfileDto update(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.update(request);
    }

    @PutMapping("/target-career")
    public ProfileDto setTarget(@Valid @RequestBody TargetCareerRequest request) {
        return profileService.setTargetCareer(request.careerPathId());
    }
}
