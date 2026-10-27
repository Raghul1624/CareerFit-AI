-- CareerFit AI: initial schema (portable MySQL 8 SQL; also runs on H2 in MySQL mode for tests)

CREATE TABLE roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(30) NOT NULL,
  CONSTRAINT uq_roles_name UNIQUE (name)
);

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(150) NOT NULL,
  password_hash VARCHAR(100) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE user_roles (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE career_categories (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  display_order INT NOT NULL DEFAULT 0,
  CONSTRAINT uq_career_categories_name UNIQUE (name)
);

CREATE TABLE career_paths (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT uq_career_paths_name UNIQUE (name),
  CONSTRAINT fk_career_paths_category FOREIGN KEY (category_id) REFERENCES career_categories (id)
);
CREATE INDEX idx_career_paths_category ON career_paths (category_id);

CREATE TABLE skills (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  CONSTRAINT uq_skills_name UNIQUE (name)
);

CREATE TABLE career_path_skills (
  career_path_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  PRIMARY KEY (career_path_id, skill_id),
  CONSTRAINT fk_cps_path FOREIGN KEY (career_path_id) REFERENCES career_paths (id) ON DELETE CASCADE,
  CONSTRAINT fk_cps_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE TABLE user_profiles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  target_career_path_id BIGINT,
  headline VARCHAR(150),
  education VARCHAR(200),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_user_profiles_user UNIQUE (user_id),
  CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_profiles_career FOREIGN KEY (target_career_path_id) REFERENCES career_paths (id) ON DELETE SET NULL
);

CREATE TABLE user_skills (
  user_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, skill_id),
  CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_user_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

-- Learning roadmap tables (API/UI arrive in a later phase)
CREATE TABLE learning_roadmaps (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  career_path_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(500),
  CONSTRAINT uq_roadmaps_path UNIQUE (career_path_id),
  CONSTRAINT fk_roadmaps_path FOREIGN KEY (career_path_id) REFERENCES career_paths (id) ON DELETE CASCADE
);

CREATE TABLE learning_roadmap_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  roadmap_id BIGINT NOT NULL,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(500),
  step_order INT NOT NULL,
  estimated_duration VARCHAR(50),
  CONSTRAINT uq_roadmap_items_order UNIQUE (roadmap_id, step_order),
  CONSTRAINT fk_roadmap_items_roadmap FOREIGN KEY (roadmap_id) REFERENCES learning_roadmaps (id) ON DELETE CASCADE
);

CREATE TABLE learning_roadmap_item_skills (
  item_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  PRIMARY KEY (item_id, skill_id),
  CONSTRAINT fk_lris_item FOREIGN KEY (item_id) REFERENCES learning_roadmap_items (id) ON DELETE CASCADE,
  CONSTRAINT fk_lris_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE TABLE user_learning_progress (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  item_id BIGINT NOT NULL,
  completed BOOLEAN NOT NULL DEFAULT FALSE,
  completed_at DATETIME(6),
  CONSTRAINT uq_progress_user_item UNIQUE (user_id, item_id),
  CONSTRAINT fk_progress_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_progress_item FOREIGN KEY (item_id) REFERENCES learning_roadmap_items (id) ON DELETE CASCADE
);

CREATE TABLE questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  skill_id BIGINT NOT NULL,
  topic VARCHAR(100),
  question_type VARCHAR(20) NOT NULL,
  difficulty VARCHAR(20) NOT NULL,
  question_text TEXT NOT NULL,
  explanation TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_questions_skill FOREIGN KEY (skill_id) REFERENCES skills (id)
);
CREATE INDEX idx_questions_skill_active ON questions (skill_id, active);

CREATE TABLE question_options (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_id BIGINT NOT NULL,
  option_text VARCHAR(500) NOT NULL,
  is_correct BOOLEAN NOT NULL DEFAULT FALSE,
  display_order INT NOT NULL,
  CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
);
CREATE INDEX idx_options_question ON question_options (question_id);

CREATE TABLE mock_tests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  description VARCHAR(500),
  test_mode VARCHAR(20) NOT NULL,
  career_path_id BIGINT,
  duration_minutes INT NOT NULL,
  question_count INT NOT NULL,
  marks_per_question DOUBLE NOT NULL DEFAULT 1,
  negative_marks DOUBLE NOT NULL DEFAULT 0,
  passing_percentage DOUBLE NOT NULL DEFAULT 40,
  max_attempts INT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_mock_tests_career FOREIGN KEY (career_path_id) REFERENCES career_paths (id) ON DELETE SET NULL
);

CREATE TABLE mock_test_skills (
  mock_test_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  PRIMARY KEY (mock_test_id, skill_id),
  CONSTRAINT fk_mts_test FOREIGN KEY (mock_test_id) REFERENCES mock_tests (id) ON DELETE CASCADE,
  CONSTRAINT fk_mts_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE TABLE test_attempts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  mock_test_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  started_at DATETIME(6) NOT NULL,
  expires_at DATETIME(6) NOT NULL,
  submitted_at DATETIME(6),
  duration_minutes INT NOT NULL,
  marks_per_question DOUBLE NOT NULL,
  negative_marks DOUBLE NOT NULL,
  passing_percentage DOUBLE NOT NULL,
  total_questions INT NOT NULL,
  max_score DOUBLE NOT NULL,
  score DOUBLE,
  correct_count INT,
  incorrect_count INT,
  unanswered_count INT,
  percentage DOUBLE,
  passed BOOLEAN,
  CONSTRAINT fk_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_attempts_test FOREIGN KEY (mock_test_id) REFERENCES mock_tests (id)
);
CREATE INDEX idx_attempts_user_started ON test_attempts (user_id, started_at);
CREATE INDEX idx_attempts_status_expiry ON test_attempts (status, expires_at);

-- Per-attempt questions (persisted selection + order) together with the user's answer.
CREATE TABLE test_attempt_questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  seq_no INT NOT NULL,
  selected_option_ids VARCHAR(255),
  marked_for_review BOOLEAN NOT NULL DEFAULT FALSE,
  is_correct BOOLEAN,
  awarded_marks DOUBLE,
  CONSTRAINT uq_taq_attempt_question UNIQUE (attempt_id, question_id),
  CONSTRAINT uq_taq_attempt_seq UNIQUE (attempt_id, seq_no),
  CONSTRAINT fk_taq_attempt FOREIGN KEY (attempt_id) REFERENCES test_attempts (id) ON DELETE CASCADE,
  CONSTRAINT fk_taq_question FOREIGN KEY (question_id) REFERENCES questions (id)
);

-- Tables below are created now; their APIs/UI are implemented in later phases.
CREATE TABLE resumes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE resume_analyses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  resume_id BIGINT NOT NULL,
  career_path_id BIGINT,
  keyword_coverage DOUBLE,
  extracted_skills TEXT,
  suggestions TEXT,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT fk_analyses_resume FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE CASCADE,
  CONSTRAINT fk_analyses_career FOREIGN KEY (career_path_id) REFERENCES career_paths (id) ON DELETE SET NULL
);

CREATE TABLE jobs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  company VARCHAR(150) NOT NULL,
  description TEXT,
  location VARCHAR(100),
  work_mode VARCHAR(20),
  experience_level VARCHAR(30),
  employment_type VARCHAR(30),
  application_url VARCHAR(500),
  is_sample BOOLEAN NOT NULL DEFAULT TRUE,
  posted_at DATETIME(6),
  active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE job_skills (
  job_id BIGINT NOT NULL,
  skill_id BIGINT NOT NULL,
  required BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (job_id, skill_id),
  CONSTRAINT fk_job_skills_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
  CONSTRAINT fk_job_skills_skill FOREIGN KEY (skill_id) REFERENCES skills (id) ON DELETE CASCADE
);

CREATE TABLE saved_jobs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  saved_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_saved_jobs UNIQUE (user_id, job_id),
  CONSTRAINT fk_saved_jobs_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_saved_jobs_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
);

CREATE TABLE job_applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  job_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  notes VARCHAR(500),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  CONSTRAINT uq_job_applications UNIQUE (user_id, job_id),
  CONSTRAINT fk_job_apps_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_job_apps_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
);
