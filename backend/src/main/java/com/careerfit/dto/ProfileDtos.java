package com.careerfit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProfileDtos {
    private ProfileDtos() {}

    public record ProfileDto(String email, String fullName, String headline, String education,
                             Long targetCareerPathId, String targetCareerPathName) {}

    public record ProfileUpdateRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 150) String headline,
        @Size(max = 200) String education,
        Long targetCareerPathId) {}

    public record TargetCareerRequest(@NotNull Long careerPathId) {}
}
