#!/usr/bin/env python3
"""
Loads the whole Flyway chain into an in-memory SQLite database (small dialect shim) and checks the
invariants the application relies on. This is NOT a MySQL/H2 run; it proves the SQL executes and the
data is internally consistent. The real check is the Spring integration test suite (mvn test).
"""
import re, sqlite3, sys
from collections import defaultdict
from pathlib import Path

MIG = Path(__file__).resolve().parents[2] / "src/main/resources/db/migration"

def shim(sql: str) -> str:
    sql = sql.replace("BIGINT AUTO_INCREMENT PRIMARY KEY", "INTEGER PRIMARY KEY")
    sql = sql.replace("CURRENT_TIMESTAMP(6)", "CURRENT_TIMESTAMP")
    return sql

def main() -> int:
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys = ON")
    for f in sorted(MIG.glob("V*.sql"), key=lambda p: int(re.match(r"V(\d+)__", p.name).group(1))):
        db.executescript(shim(f.read_text(encoding="utf-8")))
        print(f"applied {f.name}")
    fail = []
    def check(name, rows):
        if rows:
            fail.append(name); print(f"FAIL {name}: {rows[:5]}")
        else:
            print(f"ok   {name}")
    q = lambda s: db.execute(s).fetchall()

    check("every question has an active skill", q("select id from questions where skill_id not in (select id from skills)"))
    check("single/true-false questions have exactly one correct option", q("""
        select q.id from questions q join question_options o on o.question_id = q.id
        where q.question_type in ('SINGLE_CHOICE','TRUE_FALSE') group by q.id having sum(o.is_correct) <> 1"""))
    check("multiple-choice questions have 2+ correct options", q("""
        select q.id from questions q join question_options o on o.question_id = q.id
        where q.question_type = 'MULTIPLE_CHOICE' group by q.id having sum(o.is_correct) < 2"""))
    check("every question has 2-5 options", q("""
        select q.id from questions q left join question_options o on o.question_id = q.id
        group by q.id having count(o.id) < 2 or count(o.id) > 5"""))
    check("display_order is 1..n without gaps per question", q("""
        select question_id from question_options group by question_id
        having min(display_order) <> 1 or max(display_order) <> count(*) or count(distinct display_order) <> count(*)"""))
    check("no duplicate question text", q("select question_text from questions group by question_text having count(*) > 1"))
    check("every question has a topic and explanation", q("select id from questions where topic is null or topic = '' or explanation is null or explanation = ''"))
    check("every mock test has skills", q("select id from mock_tests where id not in (select mock_test_id from mock_test_skills)"))
    check("every career test skill belongs to its career path", q("""
        select t.id, s.skill_id from mock_tests t join mock_test_skills s on s.mock_test_id = t.id
        where t.test_mode = 'CAREER' and s.skill_id not in
          (select skill_id from career_path_skills where career_path_id = t.career_path_id)"""))
    check("skill tests have exactly one skill and no career path", q("""
        select t.id from mock_tests t where t.test_mode = 'SKILL'
        and (t.career_path_id is not null or (select count(*) from mock_test_skills s where s.mock_test_id = t.id) <> 1)"""))
    check("each of the 20 career paths has exactly one career test", q("""
        select p.id from career_paths p where (select count(*) from mock_tests t
        where t.test_mode = 'CAREER' and t.career_path_id = p.id) <> 1"""))
    check("each skill has exactly one skill test", q("""
        select s.id from skills s where (select count(*) from mock_tests t join mock_test_skills m on m.mock_test_id = t.id
        where t.test_mode = 'SKILL' and m.skill_id = s.id) <> 1"""))
    check("pool >= question_count for every test", q("""
        select t.id, t.question_count, (select count(*) from questions q where q.active = 1 and q.skill_id in
          (select skill_id from mock_test_skills m where m.mock_test_id = t.id)) as pool
        from mock_tests t where pool < t.question_count"""))
    check("pool >= 50 for every career path test", q("""
        select t.id from mock_tests t where t.test_mode = 'CAREER' and (select count(*) from questions q where q.active = 1
          and q.skill_id in (select skill_id from mock_test_skills m where m.mock_test_id = t.id)) < 50"""))
    check("ids of the original 23 questions and 83 options are untouched", q("""
        select 1 where (select count(*) from questions where id <= 23) <> 23 or (select count(*) from question_options where id <= 83) <> 83"""))

    print("\ncorrect-option position for the 23 original questions after re-ordering:")
    pos = q("select display_order from question_options where is_correct = 1 and question_id <= 23 and question_id not in (4, 9, 14, 19, 23) and question_id not in (5, 15)")
    print("  ", sorted(set(p[0] for p in pos)), "(was always 1)")
    print("\nmock tests:", q("select count(*) from mock_tests")[0][0], " career:", q("select count(*) from mock_tests where test_mode='CAREER'")[0][0],
          " skill:", q("select count(*) from mock_tests where test_mode='SKILL'")[0][0], " questions:", q("select count(*) from questions")[0][0])
    return 1 if fail else 0

if __name__ == "__main__":
    sys.exit(main())
