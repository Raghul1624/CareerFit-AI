package com.careerfit.service;

import java.util.List;
import java.util.Optional;

/**
 * Optional extension point for AI-generated resume feedback (implemented in a later phase).
 * By design it receives only skill names and the career name, NEVER the resume text or personal details.
 * With no implementation registered, the analyzer is purely rule-based.
 */
public interface AiFeedbackProvider {
    Optional<List<String>> feedback(String careerName, List<String> detectedSkills, List<String> missingSkills);
}
