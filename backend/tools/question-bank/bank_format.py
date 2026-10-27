"""
Parser + validator for the CareerFit question bank (*.qb files).

File format (plain text, one file per skill):

    skill: Java Basics            <- must match a skill name in the database
    # comment lines start with '#' in column 0

    @ Strings | I                 <- '@ <topic> | <difficulty: B, I or A>'
    What is printed by the code below?
      String s = "abc";           <- question text; indent code by 2 spaces
      System.out.println(s.length());
    = 3                           <- correct option  (several '=' lines => multiple choice)
    - 2                           <- wrong option
    - 4
    - 5
    ? length() returns the number of characters.   <- explanation (required)

Rules enforced here (a bad bank fails the build of the migration, not production):
  * ASCII only, no backslashes (MySQL and H2 treat them differently inside string literals),
    no '${' (Flyway would treat it as a placeholder).
  * single choice: exactly one '=' and (3 or 4) wrong options, or True/False
  * multiple choice: 2+ correct options and at least one wrong option
  * unique question text across the WHOLE bank, unique options inside a question
"""
from __future__ import annotations

import re
import zlib
import random
from dataclasses import dataclass, field
from pathlib import Path

DIFFICULTY = {"B": "BEGINNER", "I": "INTERMEDIATE", "A": "ADVANCED"}


@dataclass
class Question:
    skill: str
    topic: str
    difficulty: str
    text: str
    correct: list[str]
    wrong: list[str]
    explanation: str
    source: str = ""
    line: int = 0

    @property
    def qtype(self) -> str:
        opts = {o.lower() for o in self.correct + self.wrong}
        if opts == {"true", "false"} and len(self.correct) == 1:
            return "TRUE_FALSE"
        return "MULTIPLE_CHOICE" if len(self.correct) > 1 else "SINGLE_CHOICE"

    def ordered_options(self) -> list[tuple[str, bool]]:
        """Deterministic, per-question shuffle so the correct answer is not always first."""
        options = [(t, True) for t in self.correct] + [(t, False) for t in self.wrong]
        if self.qtype == "TRUE_FALSE":
            return sorted(options, key=lambda o: o[0].lower() != "true")  # True, False
        rng = random.Random(zlib.crc32(self.text.encode("utf-8")))
        rng.shuffle(options)
        return options


class BankError(Exception):
    pass


def parse_file(path: Path) -> list[Question]:
    skill = None
    questions: list[Question] = []
    cur: dict | None = None

    def flush():
        nonlocal cur
        if cur is None:
            return
        q = Question(skill=skill, topic=cur["topic"], difficulty=cur["difficulty"],
                     text="\n".join(cur["text"]).rstrip(), correct=cur["correct"], wrong=cur["wrong"],
                     explanation=" ".join(cur["explain"]).strip(), source=path.name, line=cur["line"])
        questions.append(q)
        cur = None

    for no, raw in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.rstrip()
        if line.startswith("#") or (not line.strip() and cur is None):
            continue
        if line.startswith("skill:"):
            skill = line.split(":", 1)[1].strip()
            continue
        if line.startswith("@ "):
            flush()
            parts = [p.strip() for p in line[2:].split("|")]
            if len(parts) != 2 or parts[1] not in DIFFICULTY:
                raise BankError(f"{path.name}:{no}: bad header '{line}' (use '@ Topic | B/I/A')")
            cur = {"topic": parts[0], "difficulty": DIFFICULTY[parts[1]], "text": [], "correct": [],
                   "wrong": [], "explain": [], "line": no, "stage": "text"}
            continue
        if cur is None:
            if line.strip():
                raise BankError(f"{path.name}:{no}: text outside a question: '{line}'")
            continue
        if line.startswith("= "):
            cur["correct"].append(line[2:].strip()); cur["stage"] = "options"
        elif line.startswith("- "):
            cur["wrong"].append(line[2:].strip()); cur["stage"] = "options"
        elif line.startswith("? "):
            cur["explain"].append(line[2:].strip()); cur["stage"] = "explain"
        elif cur["stage"] == "text":
            cur["text"].append(line)
        elif cur["stage"] == "explain" and line.strip():
            cur["explain"].append(line.strip())
        elif line.strip():
            raise BankError(f"{path.name}:{no}: unexpected line after options: '{line}'")
    flush()
    if skill is None:
        raise BankError(f"{path.name}: missing 'skill:' header")
    return questions


def validate(questions: list[Question], known_skills: set[str]) -> list[str]:
    errors: list[str] = []
    seen_text: dict[str, str] = {}
    for q in questions:
        where = f"{q.source}:{q.line}"
        if q.skill not in known_skills:
            errors.append(f"{where}: unknown skill '{q.skill}'")
        blob = "\n".join([q.text, q.explanation, q.topic] + q.correct + q.wrong)
        if not blob.isascii():
            bad = sorted({c for c in blob if ord(c) > 127})
            errors.append(f"{where}: non-ASCII characters {bad}")
        if "\\" in blob:
            errors.append(f"{where}: backslash found (portable SQL strings must not contain one)")
        if "${" in blob:
            errors.append(f"{where}: '${{' found (Flyway placeholder syntax)")
        if len(q.text) < 12 or len(q.text) > 1500:
            errors.append(f"{where}: question text length {len(q.text)} out of range")
        if not q.explanation or len(q.explanation) > 700:
            errors.append(f"{where}: explanation missing or too long")
        opts = q.correct + q.wrong
        if len(set(opts)) != len(opts):   # exact match: 'Hello world' and 'hello world' may both be options
            errors.append(f"{where}: duplicate options")
        if any(not o or len(o) > 300 for o in opts):
            errors.append(f"{where}: empty or over-long option")
        n_correct, n_wrong = len(q.correct), len(q.wrong)
        if q.qtype == "SINGLE_CHOICE" and not (n_correct == 1 and n_wrong in (3, 4)):
            errors.append(f"{where}: single choice needs 1 correct + 3-4 wrong options")
        if q.qtype == "MULTIPLE_CHOICE" and not (n_correct >= 2 and n_wrong >= 1):
            errors.append(f"{where}: multiple choice needs 2+ correct and 1+ wrong options")
        if q.qtype == "MULTIPLE_CHOICE" and "select all" not in q.text.lower():
            errors.append(f"{where}: multiple-choice text must say '(Select all that apply)'")
        key = re.sub(r"\s+", " ", q.text.strip().lower())
        if key in seen_text:
            errors.append(f"{where}: duplicate question text (first seen {seen_text[key]})")
        seen_text[key] = where
    return errors
