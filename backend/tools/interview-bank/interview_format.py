"""Parser and validator for the interview question files (*.qb) - MCQ, technical interview and HR rubrics."""
from __future__ import annotations
import re
import zlib
import random
from dataclasses import dataclass, field

ROLES = ["JAVA", "PYTHON", "FRONTEND", "FULLSTACK", "QA", "DATA_ANALYST"]
LEVELS = {"E": "EASY", "M": "MEDIUM", "H": "HARD"}
MCQ_ROUNDS = {"APTITUDE", "TECHNICAL_MCQ"}
TEXT_ROUNDS = {"TECHNICAL_INTERVIEW", "HR_INTERVIEW"}


@dataclass
class Concept:
    name: str
    weight: int
    required: bool
    keywords: list


@dataclass
class IQ:
    round: str
    level: str
    topic: str
    text: str
    roles: list
    explanation: str = ""
    guidance: str = ""
    sample: str = ""
    correct: list = field(default_factory=list)
    wrong: list = field(default_factory=list)
    concepts: list = field(default_factory=list)
    source: str = ""
    line: int = 0

    @property
    def qtype(self):
        if self.round in TEXT_ROUNDS:
            return "TEXT"
        return "MULTIPLE_CHOICE" if len(self.correct) > 1 else "SINGLE_CHOICE"

    def ordered_options(self):
        opts = [(t, True) for t in self.correct] + [(t, False) for t in self.wrong]
        random.Random(zlib.crc32(("iv:" + self.text).encode())).shuffle(opts)
        return opts


class FormatError(Exception):
    pass


def parse_file(path) -> list[IQ]:
    round_ = None
    file_roles: list = []
    out: list[IQ] = []
    cur = None

    def flush():
        nonlocal cur
        if cur is not None:
            cur.text = "\n".join(cur._text).rstrip()
            cur.explanation = " ".join(cur._expl).strip()
            if cur._sample:
                cur.sample = " ".join(cur._sample).strip()
                cur.explanation = (cur.explanation + " Sample answer: " + cur.sample).strip()
            cur.guidance = " ".join(cur._guid).strip()
            del cur._text, cur._expl, cur._guid, cur._sample
            out.append(cur)
            cur = None

    for no, raw in enumerate(open(path, encoding="utf-8").read().splitlines(), start=1):
        line = raw.rstrip()
        if line.startswith("round:"):
            round_ = line.split(":", 1)[1].strip(); continue
        if line.startswith("roles:"):
            v = [x.strip() for x in line.split(":", 1)[1].split(",")]
            file_roles = ROLES if v == ["ALL"] else v; continue
        if line.startswith("#") or (not line.strip() and cur is None):
            continue
        if line.startswith("@ "):
            flush()
            parts = [p.strip() for p in line[2:].split("|")]
            if len(parts) not in (2, 3) or parts[1] not in LEVELS:
                raise FormatError(f"{path}:{no}: bad header {line!r}")
            roles = file_roles
            if len(parts) == 3:
                roles = ROLES if parts[2] == "ALL" else [x.strip() for x in parts[2].split(",")]
            cur = IQ(round=round_, level=LEVELS[parts[1]], topic=parts[0], text="", roles=roles,
                     source=str(path).split("/")[-1], line=no)
            cur._text, cur._expl, cur._guid, cur._sample = [], [], [], []
            cur._stage = "text"
            continue
        if cur is None:
            if line.strip(): raise FormatError(f"{path}:{no}: text outside a question")
            continue
        if line.startswith("= "):
            cur.correct.append(line[2:].strip()); cur._stage = "opts"
        elif line.startswith("- "):
            cur.wrong.append(line[2:].strip()); cur._stage = "opts"
        elif line.startswith("~ "):
            p = [x.strip() for x in line[2:].split("|")]
            if len(p) != 4 or p[2] not in ("req", "opt") or not p[1].isdigit():
                raise FormatError(f"{path}:{no}: bad concept {line!r}")
            kws = [k.strip().lower() for k in p[3].split(",") if k.strip()]
            cur.concepts.append(Concept(p[0], int(p[1]), p[2] == "req", kws)); cur._stage = "concepts"
        elif line.startswith("! "):
            cur._guid.append(line[2:].strip())
        elif line.startswith("^ "):
            cur._sample.append(line[2:].strip()); cur._stage = "sample"
        elif line.startswith("? "):
            cur._expl.append(line[2:].strip()); cur._stage = "expl"
        elif cur._stage == "text":
            cur._text.append(line)
        elif cur._stage == "expl" and line.strip():
            cur._expl.append(line.strip())
        elif cur._stage == "sample" and line.strip():
            cur._sample.append(line.strip())
        elif line.strip():
            raise FormatError(f"{path}:{no}: unexpected line {line!r}")
    flush()
    return out


def validate(questions: list[IQ], existing_texts: set[str]) -> list[str]:
    errors, seen = [], {}
    for q in questions:
        w = f"{q.source}:{q.line}"
        if q.round not in MCQ_ROUNDS | TEXT_ROUNDS: errors.append(f"{w}: unknown round {q.round}")
        bad_roles = [r for r in q.roles if r not in ROLES]
        if bad_roles or not q.roles: errors.append(f"{w}: bad roles {q.roles}")
        blob = "\n".join([q.text, q.explanation, q.guidance, q.topic] + q.correct + q.wrong
                         + [c.name + " ".join(c.keywords) for c in q.concepts])
        if not blob.isascii(): errors.append(f"{w}: non-ASCII characters {sorted({c for c in blob if ord(c) > 127})}")
        if "\\" in blob: errors.append(f"{w}: backslash")
        if "${" in blob: errors.append(f"{w}: '${{' placeholder")
        if len(q.text) < 12 or len(q.text) > 1500: errors.append(f"{w}: question length")
        if not q.explanation or len(q.explanation) > 3000: errors.append(f"{w}: explanation missing or too long")
        if q.round in MCQ_ROUNDS:
            opts = q.correct + q.wrong
            if len(set(opts)) != len(opts): errors.append(f"{w}: duplicate options")
            if any(len(o) > 300 or not o for o in opts): errors.append(f"{w}: option length")
            if q.qtype == "SINGLE_CHOICE" and not (len(q.correct) == 1 and len(q.wrong) in (3, 4)):
                errors.append(f"{w}: single choice needs 1 correct and 3-4 wrong options")
            if q.concepts: errors.append(f"{w}: MCQ must not have rubric concepts")
        else:
            if len(q.concepts) < 3: errors.append(f"{w}: needs at least 3 rubric concepts")
            if not any(c.required for c in q.concepts): errors.append(f"{w}: needs a required concept")
            if q.correct or q.wrong: errors.append(f"{w}: text question must not have options")
            for c in q.concepts:
                if not c.keywords or c.weight < 1 or c.weight > 3: errors.append(f"{w}: bad concept {c.name!r}")
                if len(c.name) > 100 or len(", ".join(c.keywords)) > 480: errors.append(f"{w}: concept too long {c.name!r}")
        key = re.sub(r"\s+", " ", q.text.strip().lower())
        if key in seen: errors.append(f"{w}: duplicate question (first at {seen[key]})")
        if key in existing_texts: errors.append(f"{w}: already exists in the mock-test bank")
        seen[key] = w
    return errors
