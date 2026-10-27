"""
Structure configuration for the question bank: skills, career-path links, mock tests.
Everything here is plain data so a reviewer can read it top to bottom.
"""

# --- ids that already exist in V2__seed_sample_data.sql (never renumber these) -------------------
EXISTING_SKILLS = {
    1: "Java Basics", 2: "Core Java and OOP", 3: "Advanced Java", 4: "Python Basics",
    5: "Python Data Structures", 6: "Pandas and NumPy", 7: "SQL Basics",
    8: "SQL Joins and Aggregations", 9: "Excel", 10: "Statistics", 11: "Power BI", 12: "Qlik Sense",
    13: "HTML and CSS", 14: "JavaScript", 15: "React", 16: "Spring Boot", 17: "REST APIs",
    18: "Manual Testing", 19: "Selenium", 20: "API Testing", 21: "Quantitative Aptitude",
    22: "Logical Reasoning", 23: "Verbal Ability",
}

NEW_SKILLS = {
    24: ("Data Analysis Fundamentals", "Data cleaning, data visualization, data interpretation and analytical reasoning."),
    25: ("Python Functions and OOP", "Python functions, classes, exception handling, files and basic coding problems."),
    26: ("Database Design and Optimization", "Normalization, indexing, transactions, views, query tuning, backup and security."),
}

EXISTING_PATHS = {
    1: "Java Developer", 2: "Python Developer", 3: "Full Stack Developer", 4: "Frontend Developer",
    5: "Backend Developer", 6: "Data Analyst", 7: "Business Analyst", 8: "Data Scientist",
    9: "Data Engineer", 10: "Business Intelligence Developer", 11: "Manual Testing Engineer",
    12: "Automation Testing Engineer", 13: "Selenium with Java", 14: "API Testing Engineer",
    15: "SQL Developer", 16: "Database Administrator", 17: "Quantitative Aptitude",
    18: "Logical Reasoning", 19: "Verbal Ability", 20: "Problem Solving",
}

# --- extra career_path_skills links (the paths' "required skills") -------------------------------
# Needed so every question category the role needs is a skill of that path.
NEW_PATH_SKILL_LINKS = [
    (2, 25),   # Python Developer   + Python Functions and OOP
    (6, 24),   # Data Analyst       + Data Analysis Fundamentals
    (7, 24),   # Business Analyst   + Data Analysis Fundamentals
    (11, 20),  # Manual Testing     + API Testing
    (12, 7),   # Automation Testing + SQL Basics
    (14, 18),  # API Testing        + Manual Testing (testing fundamentals)
    (15, 26),  # SQL Developer      + Database Design and Optimization
    (16, 26),  # DBA                + Database Design and Optimization
]

# Topic labels of the 23 seeded questions, aligned with the new topic vocabulary.
EXISTING_TOPIC_RENAMES = {
    1: "OOP Concepts", 2: "Data Types and Operators", 3: "Data Types and Operators",
    4: "Core Java Fundamentals", 5: "Collections",
    6: "Queries and Filtering", 7: "DDL and DML", 8: "Joins", 9: "Keys and Constraints", 10: "Aggregation",
    11: "Data Types", 12: "Operators", 13: "Built-in Functions", 14: "Syntax and Variables", 15: "Data Types",
    16: "HTML Structure", 17: "CSS Basics", 18: "Box Model and Layout", 19: "Box Model and Layout",
    20: "Variables and Scope", 21: "Types and Operators", 22: "Arrays and Objects", 23: "Types and Operators",
}

# --- career mock tests: one per career path --------------------------------------------------------
# test id -> path id. Ids 6, 7, 8 already exist (Java, Python, Full Stack) and are upgraded in place.
EXISTING_CAREER_TEST_IDS = {1: 6, 2: 7, 3: 8}          # path id -> test id
EXISTING_SKILL_TEST_IDS = {1: 1, 7: 2, 4: 3, 13: 4, 14: 5}   # skill id -> test id

CAREER_TESTS = {
    1: (["Java Basics", "Core Java and OOP", "Advanced Java", "SQL Basics", "Spring Boot", "REST APIs"],
        "Core Java, OOP, collections, exception handling, strings and arrays, JDBC, SQL, Spring Boot and REST APIs."),
    2: (["Python Basics", "Python Data Structures", "Python Functions and OOP", "SQL Basics"],
        "Python syntax and data types, lists, tuples, sets and dictionaries, functions, OOP, exception handling, coding problems and SQL."),
    3: (["HTML and CSS", "JavaScript", "React", "REST APIs", "Java Basics", "Spring Boot", "SQL Basics"],
        "HTML, CSS, JavaScript, React, REST APIs, front-end to back-end integration, Java, Spring Boot and SQL."),
    4: (["HTML and CSS", "JavaScript", "React"],
        "HTML, CSS, JavaScript and React fundamentals for front-end roles."),
    5: (["Java Basics", "SQL Basics", "REST APIs", "Spring Boot"],
        "Java, SQL, REST APIs and Spring Boot for back-end roles."),
    6: (["SQL Basics", "SQL Joins and Aggregations", "Excel", "Statistics", "Data Analysis Fundamentals", "Power BI"],
        "SQL queries and joins, Excel, data cleaning, statistics and probability, charts and visualization, and data interpretation."),
    7: (["SQL Basics", "Excel", "Statistics", "Power BI", "Data Analysis Fundamentals"],
        "SQL, Excel, statistics, dashboards and data interpretation for business analysis."),
    8: (["Python Basics", "Python Data Structures", "Pandas and NumPy", "Statistics", "SQL Basics"],
        "Python, data structures, Pandas and NumPy, statistics and SQL."),
    9: (["Python Basics", "SQL Basics", "SQL Joins and Aggregations", "Pandas and NumPy"],
        "Python, SQL, joins and aggregations, and data processing with Pandas and NumPy."),
    10: (["SQL Basics", "SQL Joins and Aggregations", "Excel", "Power BI", "Qlik Sense"],
         "SQL, joins, Excel, Power BI and Qlik Sense for reporting roles."),
    11: (["Manual Testing", "SQL Basics", "API Testing"],
         "Testing fundamentals, test cases and techniques, bug life cycle, SDLC and STLC, SQL and API testing basics."),
    12: (["Manual Testing", "Selenium", "Java Basics", "SQL Basics"],
         "Manual testing fundamentals, test cases, bug life cycle, SDLC and STLC, Selenium WebDriver, Java fundamentals and SQL."),
    13: (["Selenium", "Java Basics", "Core Java and OOP"],
         "Selenium WebDriver with Core Java and OOP fundamentals."),
    14: (["API Testing", "REST APIs", "Manual Testing"],
         "API testing, Postman, REST concepts and testing fundamentals."),
    15: (["SQL Basics", "SQL Joins and Aggregations", "Database Design and Optimization"],
         "SQL queries, joins, aggregation, normalization, indexing, transactions and query tuning."),
    16: (["SQL Basics", "SQL Joins and Aggregations", "Database Design and Optimization"],
         "SQL, joins, indexing, transactions, views, backup and security for database administration."),
    17: (["Quantitative Aptitude"], "Percentages, profit and loss, ratios, time and work, speed, interest, averages and probability."),
    18: (["Logical Reasoning"], "Series, coding-decoding, blood relations, directions, syllogisms, ordering, puzzles and clocks."),
    19: (["Verbal Ability"], "Synonyms, antonyms, grammar, fill in the blanks, idioms, comprehension and sentence transformation."),
    20: (["Logical Reasoning", "Quantitative Aptitude"], "A mix of logical reasoning and quantitative problem solving."),
}

# Mark scheme shared by all tests (unchanged from the original seed).
MARKS_PER_QUESTION = 1
NEGATIVE_MARKS = 0.25
CAREER_PASS_PERCENT = 50
SKILL_PASS_PERCENT = 40


def career_test_size(pool: int) -> tuple[int, int]:
    """(question_count, duration_minutes) for a career test given its eligible pool size."""
    return (25, 30) if pool >= 75 else (20, 25)


def skill_test_size(pool: int) -> tuple[int, int]:
    if pool >= 30:
        return 15, 20
    if pool >= 15:
        return 10, 15
    if pool >= 12:
        return 8, 12
    return 5, 8
