package com.careerfit.entity;

/** Overall verdict of a saved code submission. */
public enum CodeVerdict {
    ACCEPTED,             // every test case passed
    PARTIAL,              // some test cases passed
    WRONG_ANSWER,         // the program ran but no test case passed
    COMPILATION_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT_EXCEEDED
}
