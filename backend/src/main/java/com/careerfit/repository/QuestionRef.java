package com.careerfit.repository;

import com.careerfit.entity.Difficulty;

/**
 * Lightweight view of a question used for selection: no text, no options, no answer key.
 * Loading thousands of these is one cheap query instead of one query per question.
 */
public record QuestionRef(Long id, Long skillId, String topic, Difficulty difficulty) {
}
