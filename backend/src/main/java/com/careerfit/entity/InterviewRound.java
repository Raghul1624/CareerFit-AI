package com.careerfit.entity;

/** The five rounds every interview role has. */
public enum InterviewRound {
    APTITUDE("Aptitude Round", "Logical reasoning and quantitative aptitude multiple-choice questions."),
    TECHNICAL_MCQ("Technical MCQ Round", "Role-specific technical multiple-choice questions."),
    CODING("Coding Round", "Solve programming problems in Java, Python or JavaScript. Hidden test cases are judged on the server."),
    TECHNICAL_INTERVIEW("Technical Interview", "Explain technical concepts in your own words and get rubric-based feedback."),
    HR_INTERVIEW("HR Interview", "Behavioural and situational questions with structured feedback.");

    private final String label;
    private final String description;

    InterviewRound(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() { return label; }
    public String description() { return description; }

    public boolean isMultipleChoice() { return this == APTITUDE || this == TECHNICAL_MCQ; }
    public boolean isCoding() { return this == CODING; }
    public boolean isText() { return this == TECHNICAL_INTERVIEW || this == HR_INTERVIEW; }
}
