package com.careerfit.controller;

import com.careerfit.repository.MockTestRepository;
import com.careerfit.repository.QuestionRepository;
import com.careerfit.repository.TestAttemptRepository;
import com.careerfit.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Access is enforced in SecurityConfig: /api/admin/** requires ROLE_ADMIN. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository users;
    private final QuestionRepository questions;
    private final MockTestRepository tests;
    private final TestAttemptRepository attempts;

    @GetMapping("/summary")
    public Map<String, Long> summary() {
        return Map.of("users", users.count(), "questions", questions.count(),
            "mockTests", tests.count(), "attempts", attempts.count());
    }
}
