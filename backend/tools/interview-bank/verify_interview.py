#!/usr/bin/env python3
"""Loads V1..V12 into SQLite (dialect shim) and checks the invariants the interview system relies on. Not a MySQL run."""
import re, sqlite3, sys
from pathlib import Path
MIG = Path(__file__).resolve().parents[2] / "src/main/resources/db/migration"
shim = lambda s: s.replace("BIGINT AUTO_INCREMENT PRIMARY KEY", "INTEGER PRIMARY KEY").replace("CURRENT_TIMESTAMP(6)", "CURRENT_TIMESTAMP")

def main():
    db = sqlite3.connect(":memory:"); db.execute("PRAGMA foreign_keys = ON")
    for f in sorted(MIG.glob("V*.sql"), key=lambda p: int(re.match(r"V(\d+)__", p.name).group(1))):
        db.executescript(shim(f.read_text(encoding="utf-8")))
    q = lambda s: db.execute(s).fetchall()
    fails = []
    def check(name, rows):
        if rows: fails.append(name); print("FAIL", name, rows[:6])
        else: print("ok  ", name)

    # servable questions: MCQ has a correct option and 2+ options, CODING has a problem and hidden test, TEXT has rubric
    servable = """
      (q.question_type in ('SINGLE_CHOICE','MULTIPLE_CHOICE') and (select count(*) from interview_options o where o.question_id=q.id) >= 2
          and exists (select 1 from interview_options o where o.question_id=q.id and o.is_correct=1))
      or (q.question_type='CODING' and exists (select 1 from interview_coding_problems p where p.question_id=q.id)
          and exists (select 1 from interview_test_cases t where t.question_id=q.id and t.hidden=1)
          and exists (select 1 from interview_test_cases t where t.question_id=q.id and t.hidden=0))
      or (q.question_type='TEXT' and (select count(*) from interview_rubric_concepts c where c.question_id=q.id) >= 3)"""
    check("every active question is servable", q(f"select q.id from interview_questions q where q.active=1 and not ({servable})"))
    check("question type matches its round", q("""select id from interview_questions where
        (round_type in ('APTITUDE','TECHNICAL_MCQ') and question_type not in ('SINGLE_CHOICE','MULTIPLE_CHOICE'))
        or (round_type='CODING' and question_type<>'CODING')
        or (round_type in ('TECHNICAL_INTERVIEW','HR_INTERVIEW') and question_type<>'TEXT')"""))
    check("single-choice has exactly one correct option", q("""select q.id from interview_questions q join interview_options o on o.question_id=q.id
        where q.question_type='SINGLE_CHOICE' group by q.id having sum(o.is_correct)<>1"""))
    check("multiple-choice has 2+ correct options", q("""select q.id from interview_questions q join interview_options o on o.question_id=q.id
        where q.question_type='MULTIPLE_CHOICE' group by q.id having sum(o.is_correct)<2"""))
    check("MCQ explanations present", q("select id from interview_questions where round_type in ('APTITUDE','TECHNICAL_MCQ') and (explanation is null or explanation='')"))
    check("text questions have a model answer", q("select id from interview_questions where question_type='TEXT' and (explanation is null or explanation='')"))
    check("no duplicate question text in a round", q("select round_type, question_text from interview_questions group by round_type, question_text having count(*)>1"))
    check("every question belongs to a role", q("select id from interview_questions where id not in (select question_id from interview_question_roles)"))
    check("coding problems have 2+ sample and 3+ hidden or at least 1 sample/3 hidden", q("""select question_id from interview_test_cases group by question_id
        having sum(case when hidden=0 then 1 else 0 end)<1 or sum(case when hidden=1 then 1 else 0 end)<3"""))
    check("level and round values are valid", q("""select id from interview_questions where difficulty_level not in ('EASY','MEDIUM','HARD')
        or round_type not in ('APTITUDE','TECHNICAL_MCQ','CODING','TECHNICAL_INTERVIEW','HR_INTERVIEW')"""))
    check("15 round settings (5 rounds x 3 levels)", [] if q("select count(*) from interview_round_configs")[0][0] == 15 else [("count",)])

    print("\nservable questions per role / round / level versus the configured test size (must be >= size):")
    rows = q(f"""select r.code, c.round_type, c.difficulty_level, c.question_count,
        (select count(distinct q.id) from interview_questions q join interview_question_roles l on l.question_id=q.id
          where l.role_id=r.id and q.round_type=c.round_type and q.difficulty_level=c.difficulty_level and q.active=1 and ({servable})) as pool
        from interview_roles r cross join interview_round_configs c order by r.id, c.round_type, c.difficulty_level""")
    short = [r for r in rows if r[4] < r[3]]
    check("every role/round/level has enough questions", short)
    by = {}
    for code, rnd, lvl, n, pool in rows: by.setdefault(code, {}).setdefault(rnd, {})[lvl[0]] = f"{pool}/{n}"
    for code, d in by.items():
        print(f"  {code:13}", "  ".join(f"{rnd[:5]}:{'/'.join(v[k].split('/')[0] for k in 'EMH')}" for rnd, v in d.items()))
    print("\ntotals:", {r[0]: r[1] for r in q("select round_type, count(*) from interview_questions group by round_type")})
    return 1 if fails else 0
sys.exit(main())
