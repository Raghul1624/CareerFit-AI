# CareerFit AI

Career intelligence and job-preparation platform: **React + Vite** frontend, **Java 21 / Spring Boot 3.5** backend, **MySQL 8** with **Flyway** migrations, **JWT** authentication.

> **Status: Phases 1-7 (foundation, auth, career paths + dashboard, mock-test engine, analytics/skill gap/learning roadmap, jobs, resume analyzer) are written. This source was generated without network access, so it has NOT been compiled, run, or tested yet.** Follow "First run checklist" below and send me the exact error text if anything fails.

## What works in this build (once it compiles and runs)

- Registration, login, JWT auth, `/auth/me`, BCrypt password hashing, USER / ADMIN roles
- Profile (name, headline, education, target career)
- 20 sample career paths in 5 categories, each with required skills and recommended tests
- Mock-test engine: skill tests and career mock tests; server-side question selection, persisted order, server clock, autosave, mark-for-review, auto-submit on expiry, server-side scoring with negative marking, results with explanations, topic-wise performance, history with sorting/pagination
- Dashboard from real attempt data (no fabricated analytics)
- Performance analytics: score trend and skill accuracy from your finished attempts
- Skill-gap analysis against your target career (rules: Strong >= 70% test accuracy, Improve < 70%, Unverified = declared but untested, Missing = neither), with a suggested learning order that follows the roadmap
- Declared skills on the profile page
- Jobs: public listing, filters (text, location, work mode, experience, type, career path), details, saved jobs, application tracker, and rule-based match scores. 8 **demo** listings are seeded and clearly labelled as sample (not live vacancies, no application links). Application URLs are only shown when they are valid http(s) URLs.
- Match formula: `percent = 100 * (w * requiredMatched/requiredTotal + (1-w) * preferredMatched/preferredTotal)`, `w = 0.7` (configurable via `app.matching.required-weight`). If a job has only required or only preferred skills, that part counts as 100%. A skill is "yours" if you declared it or scored 70%+ on it in finished tests.
- Resume analyzer: PDF upload (server checks extension, real `%PDF-` signature, size <= 2 MB, <= 10 pages, parseability, max 5 per user), text extraction with Apache PDFBox, and a rule-based analysis: detected skills (rules in `backend/src/main/resources/resume/skill-aliases.txt`, whole-word, case-insensitive), a keyword coverage score (`detected career skills / career skills`), and suggestions (missing sections, links, length, quantified results). The score is **not** an ATS score. Files are saved under random names in a private directory (`RESUME_STORAGE_DIR`, default `./data/resumes`), are never served over HTTP, and are readable only by the owner. Resume text is not stored or logged; only the derived skill lists and suggestions are. Retention: files and analyses stay until the user deletes the resume (there is no account-deletion feature yet). `AiFeedbackProvider` is an extension point that would receive only skill names, never resume text; no provider is configured, so the analysis is purely rule-based. Scanned/image PDFs are not OCR'd.
- Learning roadmaps (4 sample roadmaps: Java Developer, Python Developer, Full Stack Developer, Data Analyst) with per-user step completion saved in the database and linked practice tests
- Ownership checks (another user's attempt returns 404), admin API guarded by `ROLE_ADMIN`

- **Interview Preparation**: six roles x five rounds x three difficulty levels (aptitude, technical MCQ, coding with an OnlineCompiler sandbox, technical interview, HR interview). See the section below.

## Not implemented yet (tables exist, no API/UI)

AI integration (the interview rubrics are rule-based), admin CRUD (questions, tests, careers, jobs, interview content), password change, forgot-password. Roadmaps exist only for 4 career paths so far. Their sidebar pages show an honest "Not implemented yet" message.

## Project layout

```
careerfit-ai/
  backend/    Spring Boot app (com.careerfit.*), Flyway SQL in src/main/resources/db/migration
  frontend/   React + Vite app
  docker-compose.yml   OPTIONAL MySQL only
```

## Requirements (Windows)

- JDK 21, Maven 3.9+, Node.js 20+ (LTS), MySQL 8 (installed locally, or `docker compose up -d` for the optional container)

## Environment variables (backend)

| Variable | Required | Meaning |
|---|---|---|
| `DB_PASSWORD` | yes | MySQL password |
| `JWT_SECRET` | yes | at least 32 characters; app refuses to start otherwise |
| `DB_URL` | no | default `jdbc:mysql://localhost:3306/careerfit?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `DB_USERNAME` | no | default `root` |
| `JWT_EXPIRATION_MINUTES` | no | default 60 |
| `CORS_ALLOWED_ORIGINS` | no | default `http://localhost:5173` (comma-separated) |
| `RESUME_STORAGE_DIR` | no | private folder for uploaded resumes (default `./data/resumes`); use persistent storage in production |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | no | if both set, an admin account is created on startup (password >= 8 chars) |

Generate a strong secret in PowerShell:

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
```

Frontend: copy `frontend/.env.example` to `frontend/.env` (`VITE_API_BASE_URL=http://localhost:8080/api`).

## First run checklist

Backend (PowerShell, from `careerfit-ai/backend`):

```powershell
$env:DB_PASSWORD = "your-mysql-password"
$env:JWT_SECRET = "paste-the-generated-secret-here"
$env:ADMIN_EMAIL = "admin@example.com"        # optional
$env:ADMIN_PASSWORD = "ChangeMe12345"         # optional
mvn spring-boot:run
```

Expected: Flyway applies V1 and V2, then "Started CareerFitApplication". Check `http://localhost:8080/actuator/health` (status UP) and Swagger UI at `http://localhost:8080/swagger-ui.html`.

Frontend (new PowerShell window, from `careerfit-ai/frontend`):

```powershell
npm install
Copy-Item .env.example .env
npm run dev
```

Open `http://localhost:5173`, register, pick a career path, take "Java Basics Skill Test".

## Tests

```powershell
cd backend;  mvn test        # integration tests on in-memory H2 (MySQL mode); needs no MySQL
cd frontend; npm test        # vitest (utility tests only so far)
```

**Mock-test engine tests** (added with the career question bank): `QuestionSelectorTest` (pure unit tests of the selection algorithm), `CareerMockTestIntegrationTest` (right questions per career path, skill tests never mixing skills, no duplicates, retakes, scoring and explanations, validation, unauthenticated access, filters, career page, bank integrity) and `utils/mockTests.test.js`.

The backend tests cover registration, duplicate email, weak password, login failure, auth restrictions, admin 403, career path listing, answer-key secrecy, backend scoring, negative marking, duplicate-submit prevention, cross-user access (404), resume-the-same-attempt, history and dashboard. Resume tests cover PDF validation, owner-only access and analysis. **They have not been executed yet.**

## Interview Preparation and Testing

Six roles (Java Developer, Python Developer, Frontend Developer, Full Stack Developer, Software Tester / QA Engineer, Data Analyst), each with five rounds: **Aptitude**, **Technical MCQ**, **Coding**, **Technical Interview** and **HR Interview**, each at **Easy, Medium and Hard**. Pages: Interview Prep (sidebar) -> role -> round -> difficulty and instructions -> assessment -> results and feedback, plus attempt history.

| Round | Easy | Medium | Hard |
|---|---|---|---|
| Aptitude | 10 questions, 15 min | 10 questions, 18 min | 10 questions, 20 min |
| Technical MCQ | 10 questions, 15 min | 10 questions, 18 min | 10 questions, 20 min |
| Coding | 2 problems, 40 min | 2 problems, 50 min | 2 problems, 60 min |
| Technical Interview | 5 questions, 25 min | 5 questions, 30 min | 5 questions, 35 min |
| HR Interview | 6 questions, 25 min | 6 questions, 30 min | 6 questions, 35 min |

Pass mark is 50% everywhere (a row of `interview_round_configs`; change it there).

### Question bank (migrations V9 to V12)

`V9` schema, `V10` roles, round settings and the **copy of the existing bank** (the aptitude skills and the technical skills of the mock-test bank are copied by SQL, `BEGINNER/INTERMEDIATE/ADVANCED` becoming `EASY/MEDIUM/HARD`; the original tables are untouched), `V11` 158 new questions with rubrics, `V12` 28 coding problems with 196 test cases (56 samples, 140 hidden). Flyway runs each migration once, so nothing is duplicated on restart. Source files and the generator are in `backend/tools/interview-bank/`.

Servable questions per role (Easy/Medium/Hard); an assessment is offered only when this number reaches the test size, and a question is only counted when it can really be served (multiple choice needs a correct option, coding needs hidden and sample tests, text needs a rubric):

| Role | Aptitude | Technical MCQ | Coding | Technical Interview | HR Interview |
|---|---|---|---|---|---|
| Java Developer | 25/56/23 | 41/61/21 | 4/5/7 | 5/5/5 | 7/7/7 |
| Python Developer | 25/56/23 | 27/35/20 | 5/8/8 | 5/5/5 | 7/7/7 |
| Frontend Developer | 25/56/23 | 20/37/20 | 5/6/6 | 5/5/5 | 7/7/7 |
| Full Stack Developer | 25/56/23 | 41/78/22 | 5/6/7 | 5/5/5 | 7/7/7 |
| Software Tester / QA Engineer | 25/56/23 | 22/47/20 | 5/6/4 | 5/5/5 | 7/7/7 |
| Data Analyst | 25/56/23 | 28/57/20 | 4/3/6 | 5/5/5 | 7/7/7 |

Aptitude and HR questions are shared by all roles. Expected outputs of the coding problems are computed by running reference solutions (hard problems are cross-checked against brute force on random small inputs); they are not typed by hand.

### How each round works and is scored

* **Aptitude / Technical MCQ**: 1 mark per question, marked on the server, exact set of options for multiple-answer questions, no negative marking. The answer key and explanation are only returned by the result endpoint after submission.
* **Coding**: editor for Java, Python and JavaScript (dependency-free editor with syntax highlighting, line numbers, Tab and auto-indent). *Run code* executes the sample cases or your own input; *Submit code* judges all sample and hidden cases. A problem is worth 100 points: the share of test cases your best submission passes. Hidden cases are never sent to the browser, and a hidden case reports only passed or failed. Submissions are saved.
* **Technical Interview**: scored 0 to 100 per answer with a rule-based rubric: Correctness 35%, Completeness 25%, Clarity 20%, Technical understanding 20%. **HR Interview**: Relevance 30%, Structure 25%, Specificity 25%, Communication 20%. The result page shows each dimension, what went well, how to improve, and a model or sample answer. See `RubricEvaluator` for the exact rules.

### Code execution uses OnlineCompiler.io (external API key)

Submitted code is **never executed on the Spring Boot server**. It is sent to [OnlineCompiler.io](https://onlinecompiler.io/docs/run-code), which runs it in an isolated external sandbox. If `ONLINECOMPILER_API_KEY` is not set the application still works, but coding execution remains disabled and the coding endpoints return a clear configuration error. Results are never simulated. The API key is sent only by the backend in the `Authorization` header; never put it in the React frontend or commit it to source control.

| Variable | Meaning |
|---|---|
| Variable | Meaning |
|---|---|
| `ONLINECOMPILER_API_KEY` | Required to enable execution. Create a REST API key in the OnlineCompiler dashboard; sent by the backend as `Authorization`. Keep it secret. |
| `ONLINECOMPILER_JAVA_COMPILER` | Optional; defaults to `openjdk-25`. |
| `ONLINECOMPILER_PYTHON_COMPILER` | Optional; defaults to `python-3.14`. |
| `ONLINECOMPILER_JAVASCRIPT_COMPILER` | Optional; defaults to `nodejs-24`. |

PowerShell example (from `backend`):

```powershell
$env:ONLINECOMPILER_API_KEY = "paste-your-key-here"
mvn spring-boot:run
```

The key in this example is set only for the current PowerShell window. Do not paste it into chat, frontend `.env` files, `application.yml`, or source control. The provider may enforce monthly usage, concurrency and execution limits; this project maps HTTP 401/403 and 429 responses to clear errors. Each submitted test case uses an API run, so the number of coding problems and test cases affects usage. Java uses `openjdk-25`, Python uses `python-3.14`, and JavaScript uses `nodejs-24` by default. OnlineCompiler limits a single run to 30 seconds; its sandbox controls memory limits.

Each Run/Submit call is made while no database transaction is open, so a slow sandbox does not hold a database connection. Submissions are limited to 40 per problem, source code to 10,000 characters and custom input to 4,000.

### API (all under `/api/interview`, all require authentication)

`GET /roles`, `GET /roles/{code}` (rounds, levels, availability), `GET /overview`, `POST /attempts` (start or resume), `GET /attempts/{id}`, `PUT /attempts/{id}/answers`, `POST /attempts/{id}/submit`, `GET /attempts/{id}/result`, `GET /attempts/history?page&size&round&role`, `GET /coding/status`, `POST /attempts/{id}/questions/{qid}/run`, `POST .../submit-code`, `GET .../submissions`. Attempts are always loaded together with the authenticated user's id, so another user's attempt answers 404.

### Tests added for this feature

`QuestionSelectorTest`, `RubricEvaluatorTest`, `CodeJudgeTest`, `Judge0ResponseMappingTest` (native OnlineCompiler response mapping), `InterviewIntegrationTest` (catalog and availability, every role/MCQ round/level, answer-key leakage, scoring, coding with a **fake sandbox** bean, hidden cases, not-configured behaviour, rubric rounds, validation, ownership, authentication, expiry, history) and the frontend `interview.test.js`. The fake sandbox proves the application's plumbing and scoring, not that the external provider runs code.

### Limitations of the interview system

* **Coding execution is unverified end to end.** The OnlineCompiler integration uses the provider’s synchronous API. Verify it with your own API key before relying on it (start a Coding round, Run a sample, Submit). Java programs must use `public class Main`; programs read standard input and write standard output; the memory limit is controlled by the provider sandbox.
* **Technical Interview and HR rubrics are rule-based, not AI.** They measure coverage of expected ideas, structure and wording from keyword stems; they cannot judge whether claims are true and an answer in unusual wording can be under-scored. Keyword stuffing is penalised but not impossible to game. No AI evaluation is implemented (the application needs no API key).
* **Small interview pools.** The Technical Interview has exactly 5 questions per role and level and each attempt asks 5, so every attempt covers the same five (the order is random). HR has 7 per level and asks 6. Coding has 3 to 8 problems per role and level and asks 2 (Data Analyst Medium has only 3). The MCQ rounds draw 10 from pools of 20 to 78.
* **Hard MCQ pools are the thinnest** (20 to 23 per role). Difficulty labels of the reused questions come from the original mock-test bank and have not been calibrated with real candidates.
* **Copied questions are a snapshot.** The aptitude and technical MCQs were copied from the mock-test bank at migration time; later edits to the originals do not change the interview copies.
* The code editor is a lightweight built-in component (highlighting, line numbers, indentation) without autocomplete; Monaco or CodeMirror can replace it.
* The "you left this tab" notice is informational only, not an anti-cheating system. There is no admin screen for interview content: questions are managed through migrations (`backend/tools/interview-bank/`).

## Key design decisions and limitations

- **Token storage:** JWT in `localStorage` (simple; readable by injected scripts if XSS ever occurs). Hardening option: short-lived access token + HttpOnly refresh cookie. **Logout is client-side only** (stateless JWT); true revocation needs a deny-list.
- **Answer key secrecy:** the attempt API returns only option ids/text. Correct options and explanations are returned only by the result endpoint after submission.
- **Time:** the server stores `expires_at`; the browser countdown is derived from the server's clock; late saves are rejected and the attempt is auto-finalized (also by a 60-second sweep job, which is not cluster-safe).
- **Scoring:** multiple-select is all-or-nothing; score floors at 0; rules (marks, negative marks, pass %) are snapshotted onto each attempt.
- **Not implemented:** per-attempt option shuffling (option order is fixed per question, but no longer "correct answer first"), CSV import, rate limiting on login (put a gateway/WAF in front or add Bucket4j), email flows.
- `spring.jpa.hibernate.ddl-auto=none`: Flyway owns the schema. Change schema only with new `V3__...sql` files, never by editing applied migrations.
- Seed data is **sample data**. `V2` holds the original 23 starter questions; `V6`-`V8` add the career question bank (see below). Review before real use: difficulty labels are my own judgement and have not been calibrated with real test takers.

## Career mock tests and the question bank

**Two kinds of test, kept apart.** A *career mock test* belongs to one career path and combines the skills that role needs (Java Developer: Java, OOP, collections, JDBC, SQL, Spring Boot, REST). A *skill test* covers exactly one skill. Questions belong to a **skill**; career paths and tests link to skills (`career_path_skills`, `mock_test_skills`). A career test may only use skills that belong to its own career path (`MockTest.eligibleSkillIds()`), so a wrong link can never pull Python questions into a Java test.

**How an attempt is built** (`AttemptService.start` -> `QuestionSelector`):
1. eligible skills = the test's skills (career tests: intersected with the career path's skills);
2. if fewer questions exist than the configured count the request fails with HTTP 409 and a clear message - a test is never shortened or padded with unrelated questions (the list API reports `startable: false` and the reason);
3. the question count is split evenly across skills, topics are visited round-robin inside a skill, and difficulty is steered towards 30% beginner / 45% intermediate / 25% advanced (a preference: it uses what exists);
4. questions the user already saw in earlier attempts of the same test are used only when nothing new is left, so retakes differ;
5. only the chosen questions are loaded (with options) in one query; the selection pool is loaded as light `QuestionRef` rows, so there is no per-question query.
The selection, its order, the answers and the score are stored per attempt. Correct answers and explanations are only returned by the result endpoint after submission.

**The bank.** 550 questions (23 original + 527 new): 20 career tests (every path has at least 50 eligible questions) and 26 skill tests. See `backend/tools/question-bank/README.md` for the per-path numbers, the source format and how to add questions safely. Migrations: `V6` structure, `V7` questions, `V8` Automation Testing roadmap.

**API changes.** `GET /api/mock-tests` now also accepts `skillId` and returns `topics`, `coverage`, `difficulties`, `skillIds`, `availableQuestions`, `startable`, `unavailableReason`; attempt, result and history payloads carry `mode`, `careerPathId`, `careerPathName`; `GET /api/career-paths/{id}` adds `careerMockTest`, `learningTopics`, `roadmap`, `suggestedLearningOrder`.

**Limits.** All questions are multiple choice (single or multiple answer); there is no code-execution question type, so "coding" questions are code-reading questions (predict the output). Only 5 of the 20 paths have a curated roadmap; the others show a suggested learning order derived from their skills.

## Deployment outline (not deployed yet)

Frontend (Vercel/Netlify): build `npm run build`, publish `dist`, set `VITE_API_BASE_URL` to the public backend URL; SPA fallback is included for Netlify (`public/_redirects`), on Vercel add a rewrite to `/index.html`.
Backend (Render/Railway/Azure etc.): `mvn -DskipTests package`, run `java -jar target/careerfit-backend-0.1.0.jar`, set the env vars above, set `CORS_ALLOWED_ORIGINS` to the exact frontend origin, serve over HTTPS, health check `/actuator/health`.
Database: managed MySQL 8; set `DB_URL` (use `useSSL=true` if the provider requires it). Flyway migrates on startup. Local-disk file storage (resumes, later phase) is not persistent on most hosts; use object storage.

## Next phases

Say "CONTINUE" for Phase 8 (interview preparation and the optional AI integration), then admin tools and a security review.
