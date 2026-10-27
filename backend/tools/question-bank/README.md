# Career question bank: source, generator and how to extend it

The question bank is kept as readable text files (`bank/*.qb`, one per skill). `generate_migrations.py` validates
them and writes the Flyway migrations. Flyway applies each migration exactly once, so questions are never duplicated
when the application restarts. **Never edit a migration that has already been applied** (Flyway checksums it); add a new one.

## Source format

```
skill: Java Basics                 # must match a skill name in the database

@ Strings | I                      # topic | difficulty: B (beginner), I (intermediate), A (advanced)
What does this code print?         # question text; indent code lines with two spaces
  String s = "abc";
  System.out.println(s.length());
= 3                                # correct option (two or more '=' lines make a multiple-choice question)
- 2                                # wrong options
- 4
- 5
? length() returns the number of characters.     # explanation (required), shown only after submission
```

Rules checked by the generator (a bad question stops the build, not production): ASCII only; no backslashes (MySQL and
H2 read them differently inside strings); no `${` (Flyway placeholder); exactly one correct option for single choice;
multiple-choice text must contain "(Select all that apply)"; unique question text across the whole bank; unique options;
an explanation on every question. The option order is shuffled deterministically per question so the correct answer is not
always first (positions are spread evenly across A-D).

## Commands

```powershell
cd backend\tools\question-bank
python generate_migrations.py report                         # validate + coverage report, writes nothing
python generate_migrations.py build --force                  # (re)generate V6 + V7 - only before they were ever applied
python generate_migrations.py add --version 9 --files bank\new_topic.qb   # NEW questions as V9 (ids continue automatically)
python verify_migrations.py                                  # load V1..V8 into SQLite and check data invariants
```

To add questions later: create a new `.qb` file (do **not** edit the old ones), run `add`, start the backend. To add a new
skill or test, write a new hand-made migration. `verify_migrations.py` is a SQLite smoke test (SQL executes, data is consistent);
it is not a MySQL run.

## Skill -> question counts and the individual skill test

| id | Skill | Questions | Skill test |
|---|---|---|---|
| 1 | Java Basics | 23 | 10 q / 15 min |
| 2 | Core Java and OOP | 25 | 10 q / 15 min |
| 3 | Advanced Java | 14 | 8 q / 12 min |
| 4 | Python Basics | 20 | 10 q / 15 min |
| 5 | Python Data Structures | 13 | 8 q / 12 min |
| 6 | Pandas and NumPy | 12 | 8 q / 12 min |
| 7 | SQL Basics | 21 | 10 q / 15 min |
| 8 | SQL Joins and Aggregations | 14 | 8 q / 12 min |
| 9 | Excel | 14 | 8 q / 12 min |
| 10 | Statistics | 14 | 8 q / 12 min |
| 11 | Power BI | 10 | 5 q / 8 min |
| 12 | Qlik Sense | 8 | 5 q / 8 min |
| 13 | HTML and CSS | 21 | 10 q / 15 min |
| 14 | JavaScript | 22 | 10 q / 15 min |
| 15 | React | 16 | 10 q / 15 min |
| 16 | Spring Boot | 18 | 10 q / 15 min |
| 17 | REST APIs | 15 | 10 q / 15 min |
| 18 | Manual Testing | 31 | 15 q / 20 min |
| 19 | Selenium | 16 | 10 q / 15 min |
| 20 | API Testing | 14 | 8 q / 12 min |
| 21 | Quantitative Aptitude | 52 | 15 q / 20 min |
| 22 | Logical Reasoning | 52 | 15 q / 20 min |
| 23 | Verbal Ability | 51 | 15 q / 20 min |
| 24 | Data Analysis Fundamentals | 18 | 10 q / 15 min |
| 25 | Python Functions and OOP | 17 | 10 q / 15 min |
| 26 | Database Design and Optimization | 19 | 10 q / 15 min |

## Career path -> mock test -> eligible questions

"Eligible questions" are the unique questions that path's mock test can draw from. Skills are shared between paths
(for example SQL Basics feeds eight paths), so the pools overlap; each path's own test still only ever uses the skills listed.
Every path has at least 50.

| id | Career path | Eligible questions | Mock test | Skills |
|---|---|---|---|---|
| 1 | Java Developer | 116 | 25 q / 30 min | Java Basics, Core Java and OOP, Advanced Java, SQL Basics, Spring Boot, REST APIs |
| 2 | Python Developer | 71 | 20 q / 25 min | Python Basics, Python Data Structures, Python Functions and OOP, SQL Basics |
| 3 | Full Stack Developer | 136 | 25 q / 30 min | HTML and CSS, JavaScript, React, REST APIs, Java Basics, Spring Boot, SQL Basics |
| 4 | Frontend Developer | 59 | 20 q / 25 min | HTML and CSS, JavaScript, React |
| 5 | Backend Developer | 77 | 25 q / 30 min | Java Basics, SQL Basics, REST APIs, Spring Boot |
| 6 | Data Analyst | 91 | 25 q / 30 min | SQL Basics, SQL Joins and Aggregations, Excel, Statistics, Data Analysis Fundamentals, Power BI |
| 7 | Business Analyst | 77 | 25 q / 30 min | SQL Basics, Excel, Statistics, Power BI, Data Analysis Fundamentals |
| 8 | Data Scientist | 80 | 25 q / 30 min | Python Basics, Python Data Structures, Pandas and NumPy, Statistics, SQL Basics |
| 9 | Data Engineer | 67 | 20 q / 25 min | Python Basics, SQL Basics, SQL Joins and Aggregations, Pandas and NumPy |
| 10 | Business Intelligence Developer | 67 | 20 q / 25 min | SQL Basics, SQL Joins and Aggregations, Excel, Power BI, Qlik Sense |
| 11 | Manual Testing Engineer | 66 | 20 q / 25 min | Manual Testing, SQL Basics, API Testing |
| 12 | Automation Testing Engineer | 91 | 25 q / 30 min | Manual Testing, Selenium, Java Basics, SQL Basics |
| 13 | Selenium with Java | 64 | 20 q / 25 min | Selenium, Java Basics, Core Java and OOP |
| 14 | API Testing Engineer | 60 | 20 q / 25 min | API Testing, REST APIs, Manual Testing |
| 15 | SQL Developer | 54 | 20 q / 25 min | SQL Basics, SQL Joins and Aggregations, Database Design and Optimization |
| 16 | Database Administrator | 54 | 20 q / 25 min | SQL Basics, SQL Joins and Aggregations, Database Design and Optimization |
| 17 | Quantitative Aptitude | 52 | 20 q / 25 min | Quantitative Aptitude |
| 18 | Logical Reasoning | 52 | 20 q / 25 min | Logical Reasoning |
| 19 | Verbal Ability | 51 | 20 q / 25 min | Verbal Ability |
| 20 | Problem Solving | 104 | 25 q / 30 min | Logical Reasoning, Quantitative Aptitude |

Totals: 550 questions (23 original + 527 new). Difficulty of the 527 new questions: 130 beginner, 303 intermediate, 94 advanced
(ratings are my own judgement, not calibrated with real test takers). Question types: 525 single choice, 2 multiple choice;
the 23 original questions also include true/false and multiple choice.
