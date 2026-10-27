package com.careerfit.service;

import com.careerfit.dto.ProfileDtos.*;
import com.careerfit.entity.CareerPath;
import com.careerfit.entity.User;
import com.careerfit.entity.UserProfile;
import com.careerfit.exception.ApiException;
import com.careerfit.repository.CareerPathRepository;
import com.careerfit.repository.UserProfileRepository;
import com.careerfit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CurrentUser currentUser;
    private final UserProfileRepository profiles;
    private final CareerPathRepository careerPaths;

    @Transactional
    public ProfileDto get() {
        User user = currentUser.get();
        return toDto(user, findOrCreate(user));
    }

    @Transactional
    public ProfileDto update(ProfileUpdateRequest request) {
        User user = currentUser.get();
        UserProfile profile = findOrCreate(user);
        user.setFullName(request.fullName().trim());
        profile.setHeadline(blankToNull(request.headline()));
        profile.setEducation(blankToNull(request.education()));
        profile.setTargetCareerPath(resolvePath(request.targetCareerPathId()));
        return toDto(user, profile);
    }

    @Transactional
    public ProfileDto setTargetCareer(Long careerPathId) {
        User user = currentUser.get();
        UserProfile profile = findOrCreate(user);
        profile.setTargetCareerPath(resolvePath(careerPathId));
        return toDto(user, profile);
    }

    private CareerPath resolvePath(Long id) {
        if (id == null) return null;
        return careerPaths.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown career path"));
    }

    private UserProfile findOrCreate(User user) {
        return profiles.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUser(user);
            return profiles.save(p);
        });
    }

    private ProfileDto toDto(User user, UserProfile p) {
        CareerPath path = p.getTargetCareerPath();
        return new ProfileDto(user.getEmail(), user.getFullName(), p.getHeadline(), p.getEducation(),
            path == null ? null : path.getId(), path == null ? null : path.getName());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
