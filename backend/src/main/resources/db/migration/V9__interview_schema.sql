-- Interview Preparation and Testing System: schema only (backward compatible, no existing table is changed).

CREATE TABLE interview_roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(30) NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  display_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_interview_roles_code UNIQUE (code)
);

-- How big each round is per difficulty level (one row per round and level).
CREATE TABLE interview_round_configs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  round_type VARCHAR(30) NOT NULL,
  difficulty_level VARCHAR(10) NOT NULL,
  question_count INT NOT NULL,
  duration_minutes INT NOT NULL,
  passing_percentage DOUBLE NOT NULL,
  CONSTRAINT uq_interview_round_config UNIQUE (round_type, difficulty_level)
);

-- One table for every kind of question: SINGLE_CHOICE / MULTIPLE_CHOICE, CODING and TEXT (interview answers).
CREATE TABLE interview_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  round_type VARCHAR(30) NOT NULL,
  difficulty_level VARCHAR(10) NOT NULL,
  question_type VARCHAR(20) NOT NULL,
  topic VARCHAR(100),
  title VARCHAR(200),
  question_text TEXT NOT NULL,
  guidance VARCHAR(500),
  explanation TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX idx_interview_questions_pool ON interview_questions (round_type, difficulty_level, active);

CREATE TABLE interview_question_roles (
  question_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (question_id, role_id),
  CONSTRAINT fk_iqr_question FOREIGN KEY (question_id) REFERENCES interview_questions (id),
  CONSTRAINT fk_iqr_role FOREIGN KEY (role_id) REFERENCES interview_roles (id)
);
CREATE INDEX idx_iqr_role ON interview_question_roles (role_id);

CREATE TABLE interview_options (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  option_text VARCHAR(500) NOT NULL,
  is_correct BOOLEAN NOT NULL DEFAULT FALSE,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_io_question FOREIGN KEY (question_id) REFERENCES interview_questions (id)
);
CREATE INDEX idx_io_question ON interview_options (question_id);

CREATE TABLE interview_coding_problems (
  question_id BIGINT PRIMARY KEY,
  constraints_text TEXT,
  input_format TEXT,
  output_format TEXT,
  time_limit_seconds DOUBLE NOT NULL DEFAULT 2,
  memory_limit_mb INT NOT NULL DEFAULT 256,
  CONSTRAINT fk_icp_question FOREIGN KEY (question_id) REFERENCES interview_questions (id)
);

-- Sample cases (hidden = FALSE) are shown to the candidate; hidden cases are evaluated on the server only.
CREATE TABLE interview_test_cases (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  input_data TEXT NOT NULL,
  expected_output TEXT NOT NULL,
  hidden BOOLEAN NOT NULL DEFAULT FALSE,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_itc_question FOREIGN KEY (question_id) REFERENCES interview_questions (id)
);
CREATE INDEX idx_itc_question ON interview_test_cases (question_id);

-- Rubric of a TEXT question: the concepts a good answer covers (keywords are matched as lower-case substrings).
CREATE TABLE interview_rubric_concepts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  keywords VARCHAR(500) NOT NULL,
  weight INT NOT NULL DEFAULT 1,
  required BOOLEAN NOT NULL DEFAULT FALSE,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_irc_question FOREIGN KEY (question_id) REFERENCES interview_questions (id)
);
CREATE INDEX idx_irc_question ON interview_rubric_concepts (question_id);

CREATE TABLE interview_attempts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  round_type VARCHAR(30) NOT NULL,
  difficulty_level VARCHAR(10) NOT NULL,
  status VARCHAR(20) NOT NULL,
  started_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  submitted_at DATETIME(6),
  duration_minutes INT NOT NULL,
  passing_percentage DOUBLE NOT NULL,
  total_questions INT NOT NULL,
  max_score DOUBLE NOT NULL DEFAULT 0,
  score DOUBLE,
  percentage DOUBLE,
  passed BOOLEAN,
  summary_feedback TEXT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_ia_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_ia_role FOREIGN KEY (role_id) REFERENCES interview_roles (id)
);
CREATE INDEX idx_ia_user_started ON interview_attempts (user_id, started_at);

CREATE TABLE interview_attempt_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  seq_no INT NOT NULL,
  selected_option_ids VARCHAR(255),
  answer_text TEXT,
  language VARCHAR(20),
  marked_for_review BOOLEAN NOT NULL DEFAULT FALSE,
  correct BOOLEAN,
  awarded_score DOUBLE,
  max_score DOUBLE NOT NULL,
  evaluation_json TEXT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_iai_attempt FOREIGN KEY (attempt_id) REFERENCES interview_attempts (id),
  CONSTRAINT fk_iai_question FOREIGN KEY (question_id) REFERENCES interview_questions (id),
  CONSTRAINT uq_iai_attempt_question UNIQUE (attempt_id, question_id)
);
CREATE INDEX idx_iai_attempt ON interview_attempt_items (attempt_id);

CREATE TABLE code_submissions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_item_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  language VARCHAR(20) NOT NULL,
  source_code TEXT NOT NULL,
  status VARCHAR(30) NOT NULL,
  passed_tests INT NOT NULL DEFAULT 0,
  total_tests INT NOT NULL DEFAULT 0,
  score_percent DOUBLE NOT NULL DEFAULT 0,
  error_output TEXT,
  execution_time_ms INT,
  results_json TEXT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_cs_item FOREIGN KEY (attempt_item_id) REFERENCES interview_attempt_items (id),
  CONSTRAINT fk_cs_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_cs_item ON code_submissions (attempt_item_id, created_at);
