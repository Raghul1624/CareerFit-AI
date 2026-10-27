#!/usr/bin/env python3
"""
Generates the Flyway migrations of the Interview Preparation system:

  V9__interview_schema.sql            tables (static, hand written)
  V10__interview_seed_core.sql        roles, round settings, and the aptitude + technical MCQs COPIED (by SQL) from the existing
                                      mock-test question bank, mapped BEGINNER/INTERMEDIATE/ADVANCED -> EASY/MEDIUM/HARD
  V11__interview_question_bank.sql    new technical MCQs, technical-interview and HR questions with their rubrics
  V12__interview_coding_problems.sql  coding problems and test cases (expected outputs computed by running reference solutions)

  python generate_interview_migrations.py report
  python generate_interview_migrations.py build --force
"""
from __future__ import annotations
import re, sys, argparse, random
from pathlib import Path
from collections import Counter, defaultdict

import interview_format as fmt
import coding_problems as cp

HERE = Path(__file__).resolve().parent
MIG = HERE.parent.parent / "src" / "main" / "resources" / "db" / "migration"
ROLE_IDS = {r: i for i, r in enumerate(fmt.ROLES, start=1)}
ROLE_NAMES = {
    "JAVA": ("Java Developer", "Core Java, OOP, collections, JVM, Spring and SQL for Java backend roles."),
    "PYTHON": ("Python Developer", "Python language, data structures, OOP, concurrency and tooling for Python roles."),
    "FRONTEND": ("Frontend Developer", "HTML, CSS, JavaScript, React, performance and accessibility for UI roles."),
    "FULLSTACK": ("Full Stack Developer", "Front end, back end, databases, APIs, security and system design."),
    "QA": ("Software Tester / QA Engineer", "Testing fundamentals, test design, Selenium, API and performance testing."),
    "DATA_ANALYST": ("Data Analyst", "SQL, statistics, data cleaning, visualisation and analytical thinking."),
}
# skills of the existing mock-test bank used for each role's technical MCQ round
ROLE_SKILLS = {"JAVA": [1, 2, 3, 16, 17, 7], "PYTHON": [4, 5, 25, 7], "FRONTEND": [13, 14, 15],
               "FULLSTACK": [13, 14, 15, 17, 16, 1, 7], "QA": [18, 19, 20, 7], "DATA_ANALYST": [7, 8, 9, 10, 24, 11, 6]}
APTITUDE_SKILLS = [21, 22]
TECH_SKILLS = sorted({s for v in ROLE_SKILLS.values() for s in v})
# (question_count, duration_minutes, passing_percentage) per round and level
CONFIG = {
    "APTITUDE": {"EASY": (10, 15, 50), "MEDIUM": (10, 18, 50), "HARD": (10, 20, 50)},
    "TECHNICAL_MCQ": {"EASY": (10, 15, 50), "MEDIUM": (10, 18, 50), "HARD": (10, 20, 50)},
    "CODING": {"EASY": (2, 40, 50), "MEDIUM": (2, 50, 50), "HARD": (2, 60, 50)},
    "TECHNICAL_INTERVIEW": {"EASY": (5, 25, 50), "MEDIUM": (5, 30, 50), "HARD": (5, 35, 50)},
    "HR_INTERVIEW": {"EASY": (6, 25, 50), "MEDIUM": (6, 30, 50), "HARD": (6, 35, 50)},
}
COPY_OFFSET = 100000
NEW_Q, NEW_OPT, CODING_Q = 200001, 300001, 400001
MAX_INPUT = 20000


def sql(s):
    return "'" + s.replace("'", "''") + "'"


def existing_texts() -> set[str]:
    rx = re.compile(r"'(?:BEGINNER|INTERMEDIATE|ADVANCED)', '((?:[^']|'')*)', '(?:[^']|'')*', (?:TRUE|FALSE)\)")
    texts = set()
    for f in MIG.glob("V*.sql"):
        for m in rx.finditer(f.read_text(encoding="utf-8")):
            texts.add(re.sub(r"\s+", " ", m.group(1).replace("''", "'").strip().lower()))
    return texts


def load_text_bank():
    qs = []
    for f in sorted((HERE / "bank").glob("*.qb")):
        qs += fmt.parse_file(f)
    return qs


def build_coding():
    """Runs reference solutions; cross-checks with brute force; returns problems with computed expected outputs."""
    out, errors = [], []
    for p in cp.PROBLEMS:
        if p["brute"] and p["small"]:
            rnd = random.Random(zlib_seed(p["title"]))
            for _ in range(400):
                inp = p["small"](rnd)
                a, b = p["ref"](inp).strip(), p["brute"](inp).strip()
                if a != b:
                    errors.append(f"{p['title']}: reference and brute force disagree on {inp!r}: {a!r} vs {b!r}"); break
        tests = []
        for kind, inputs in (("sample", p["samples"]), ("hidden", p["hidden"])):
            for inp in inputs:
                if len(inp) > MAX_INPUT: errors.append(f"{p['title']}: test input of {len(inp)} chars exceeds {MAX_INPUT}")
                exp = p["ref"](inp).strip()
                if not exp: errors.append(f"{p['title']}: empty expected output")
                if "\\" in inp or "\\" in exp or not (inp + exp).isascii(): errors.append(f"{p['title']}: non-portable characters")
                tests.append((kind == "hidden", inp, exp))
        if len(p["samples"]) < 1 or len(p["hidden"]) < 3: errors.append(f"{p['title']}: needs samples and 3+ hidden tests")
        if len({t[1] for t in tests}) != len(tests): errors.append(f"{p['title']}: duplicate test inputs")
        out.append((p, tests))
    return out, errors


def zlib_seed(s):
    import zlib
    return zlib.crc32(s.encode())


def analyse():
    errors = []
    text_qs = load_text_bank()
    errors += fmt.validate(text_qs, existing_texts())
    coding, cerr = build_coding()
    errors += cerr
    pools = Counter()   # (role, round, level) -> number of questions that will exist
    return text_qs, coding, errors


# --------------------------------------------------------------------------------------------------- SQL
def v10() -> str:
    o = ["-- Interview Preparation: roles, round settings and the MCQs reused from the existing mock-test question bank.",
         "-- GENERATED by backend/tools/interview-bank/generate_interview_migrations.py", ""]
    for code, rid in ROLE_IDS.items():
        name, desc = ROLE_NAMES[code]
        o.append(f"INSERT INTO interview_roles (id, code, name, description, display_order, active) VALUES "
                 f"({rid}, {sql(code)}, {sql(name)}, {sql(desc)}, {rid}, TRUE);")
    o.append("")
    cid = 1
    for rnd, levels in CONFIG.items():
        for lvl, (n, mins, pas) in levels.items():
            o.append(f"INSERT INTO interview_round_configs (id, round_type, difficulty_level, question_count, duration_minutes, passing_percentage) "
                     f"VALUES ({cid}, {sql(rnd)}, {sql(lvl)}, {n}, {mins}, {pas});")
            cid += 1
    o.append("")
    level_case = "CASE q.difficulty WHEN 'BEGINNER' THEN 'EASY' WHEN 'INTERMEDIATE' THEN 'MEDIUM' ELSE 'HARD' END"
    type_case = "CASE q.question_type WHEN 'MULTIPLE_CHOICE' THEN 'MULTIPLE_CHOICE' ELSE 'SINGLE_CHOICE' END"
    for rnd, skills in (("APTITUDE", APTITUDE_SKILLS), ("TECHNICAL_MCQ", TECH_SKILLS)):
        sk = ", ".join(map(str, skills))
        o.append(f"-- {rnd}: copy the active mock-test questions of skills ({sk}); ids are the original id + {COPY_OFFSET}")
        o.append(f"INSERT INTO interview_questions (id, round_type, difficulty_level, question_type, topic, question_text, explanation, active)\n"
                 f"SELECT q.id + {COPY_OFFSET}, '{rnd}', {level_case}, {type_case}, q.topic, q.question_text, q.explanation, TRUE\n"
                 f"FROM questions q WHERE q.skill_id IN ({sk}) AND q.active = TRUE;")
        o.append(f"INSERT INTO interview_options (id, question_id, option_text, is_correct, display_order)\n"
                 f"SELECT o.id + {COPY_OFFSET}, o.question_id + {COPY_OFFSET}, o.option_text, o.is_correct, o.display_order\n"
                 f"FROM question_options o JOIN questions q ON q.id = o.question_id WHERE q.skill_id IN ({sk}) AND q.active = TRUE;")
    o.append("")
    o.append("-- Aptitude questions are shared by every role")
    sk = ", ".join(map(str, APTITUDE_SKILLS))
    for code, rid in ROLE_IDS.items():
        o.append(f"INSERT INTO interview_question_roles (question_id, role_id) SELECT q.id + {COPY_OFFSET}, {rid} FROM questions q "
                 f"WHERE q.skill_id IN ({sk}) AND q.active = TRUE;")
    o.append("")
    o.append("-- Technical MCQs follow the skills of each role")
    for code, rid in ROLE_IDS.items():
        sk = ", ".join(map(str, ROLE_SKILLS[code]))
        o.append(f"INSERT INTO interview_question_roles (question_id, role_id) SELECT q.id + {COPY_OFFSET}, {rid} FROM questions q "
                 f"WHERE q.skill_id IN ({sk}) AND q.active = TRUE;")
    return "\n".join(o) + "\n"


def v11(text_qs) -> str:
    o = [f"-- Interview Preparation question bank: {len(text_qs)} new questions (technical MCQs, technical interview, HR) with rubrics.",
         "-- GENERATED by backend/tools/interview-bank/generate_interview_migrations.py from bank/*.qb", ""]
    qid, oid, cid = NEW_Q, NEW_OPT, 1
    for q in text_qs:
        o.append(f"INSERT INTO interview_questions (id, round_type, difficulty_level, question_type, topic, question_text, guidance, explanation, active) "
                 f"VALUES ({qid}, {sql(q.round)}, {sql(q.level)}, {sql(q.qtype)}, {sql(q.topic)}, {sql(q.text)}, "
                 f"{sql(q.guidance) if q.guidance else 'NULL'}, {sql(q.explanation)}, TRUE);")
        if q.round in fmt.MCQ_ROUNDS:
            rows = []
            for pos, (t, ok) in enumerate(q.ordered_options(), start=1):
                rows.append(f"({oid}, {qid}, {sql(t)}, {'TRUE' if ok else 'FALSE'}, {pos})"); oid += 1
            o.append("INSERT INTO interview_options (id, question_id, option_text, is_correct, display_order) VALUES\n  " + ",\n  ".join(rows) + ";")
        else:
            rows = []
            for pos, c in enumerate(q.concepts, start=1):
                rows.append(f"({cid}, {qid}, {sql(c.name)}, {sql(', '.join(c.keywords))}, {c.weight}, {'TRUE' if c.required else 'FALSE'}, {pos})"); cid += 1
            o.append("INSERT INTO interview_rubric_concepts (id, question_id, name, keywords, weight, required, display_order) VALUES\n  " + ",\n  ".join(rows) + ";")
        o.append("INSERT INTO interview_question_roles (question_id, role_id) VALUES " + ", ".join(f"({qid}, {ROLE_IDS[r]})" for r in q.roles) + ";")
        qid += 1
    return "\n".join(o) + "\n"


def v12(coding) -> str:
    o = [f"-- Coding Round: {len(coding)} problems. Expected outputs were produced by running the reference solutions.",
         "-- GENERATED by backend/tools/interview-bank/generate_interview_migrations.py from coding_problems.py", ""]
    qid, tid = CODING_Q, 1
    for p, tests in coding:
        o.append(f"INSERT INTO interview_questions (id, round_type, difficulty_level, question_type, topic, title, question_text, explanation, active) "
                 f"VALUES ({qid}, 'CODING', {sql(p['level'])}, 'CODING', 'Problem solving', {sql(p['title'])}, {sql(p['statement'])}, NULL, TRUE);")
        o.append(f"INSERT INTO interview_coding_problems (question_id, constraints_text, input_format, output_format, time_limit_seconds, memory_limit_mb) "
                 f"VALUES ({qid}, {sql(p['constraints'])}, {sql(p['input_format'])}, {sql(p['output_format'])}, 2, 256);")
        rows = []
        for pos, (hidden, inp, exp) in enumerate(tests, start=1):
            rows.append(f"({tid}, {qid}, {sql(inp)}, {sql(exp)}, {'TRUE' if hidden else 'FALSE'}, {pos})"); tid += 1
        o.append("INSERT INTO interview_test_cases (id, question_id, input_data, expected_output, hidden, display_order) VALUES\n  " + ",\n  ".join(rows) + ";")
        o.append("INSERT INTO interview_question_roles (question_id, role_id) VALUES " + ", ".join(f"({qid}, {ROLE_IDS[r]})" for r in p["roles"]) + ";")
        qid += 1
    return "\n".join(o) + "\n"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("command", choices=["report", "build"]); ap.add_argument("--force", action="store_true")
    a = ap.parse_args()
    text_qs, coding, errors = analyse()
    if errors:
        print("VALIDATION FAILED:\n  " + "\n  ".join(errors)); return 1
    by = Counter((q.round, q.level) for q in text_qs)
    print(f"new questions: {len(text_qs)}  {dict(Counter(q.round for q in text_qs))}")
    print(f"coding problems: {len(coding)}  test cases: {sum(len(t) for _, t in coding)} "
          f"(hidden {sum(1 for _, t in coding for x in t if x[0])})")
    if a.command == "build":
        targets = {"V10__interview_seed_core.sql": v10(), "V11__interview_question_bank.sql": v11(text_qs), "V12__interview_coding_problems.sql": v12(coding)}
        for name in targets:
            if (MIG / name).exists() and not a.force: sys.exit(f"{name} exists; use --force before first release")
        for name, body in targets.items():
            (MIG / name).write_text(body, encoding="utf-8"); print("wrote", name, f"({len(body) // 1024} KB)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
