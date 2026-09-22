-- Fresh isolated candidate database; synthetic identity only.
SET NAMES utf8mb4;
CREATE DATABASE interview_studio_local CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE interview_studio_local;
-- Local profile only: compatible identity shape; production reuses existing user_info.
CREATE TABLE IF NOT EXISTS user_info (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, phone VARCHAR(20) NOT NULL UNIQUE,
 password VARCHAR(64) NOT NULL, status INT NOT NULL DEFAULT 1,
 user_name VARCHAR(50), is_deleted INT NOT NULL DEFAULT 0
);

-- Additive migration. user_info remains the shared identity table; no existing data is copied.
CREATE TABLE IF NOT EXISTS studio_session (
 token_hash VARCHAR(64) PRIMARY KEY, user_id BIGINT NOT NULL, expires_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS studio_profile (
 user_id BIGINT PRIMARY KEY, nickname VARCHAR(50) NOT NULL, role_id VARCHAR(20) NOT NULL,
 career_level VARCHAR(20) NOT NULL, graduation VARCHAR(4) NOT NULL, avatar_id VARCHAR(40)
);
CREATE TABLE IF NOT EXISTS studio_resume (
 id VARCHAR(40) PRIMARY KEY, owner_id BIGINT NOT NULL, filename VARCHAR(255) NOT NULL,
 file_hash VARCHAR(64) NOT NULL, storage_key VARCHAR(80) NOT NULL, status VARCHAR(20) NOT NULL,
 revision INT NOT NULL, raw_text LONGTEXT, fragments_json LONGTEXT, parser_json LONGTEXT,
 error_message VARCHAR(500), created_at BIGINT NOT NULL, confirmed_at BIGINT
);

-- Additive account migration: no existing identities, passwords or permissions are changed.
CREATE TABLE IF NOT EXISTS studio_security (
 user_id BIGINT PRIMARY KEY, email VARCHAR(254) UNIQUE, password_updated_at BIGINT
);
CREATE TABLE IF NOT EXISTS studio_login_history (
 id VARCHAR(40) PRIMARY KEY, user_id BIGINT NOT NULL, logged_at BIGINT NOT NULL,
 address VARCHAR(64) NOT NULL
);
CREATE TABLE IF NOT EXISTS studio_email_challenge (
 id VARCHAR(40) PRIMARY KEY, purpose VARCHAR(12) NOT NULL, user_id BIGINT,
 email VARCHAR(254) NOT NULL, code_hash VARCHAR(64) NOT NULL,
 created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, attempts INT NOT NULL DEFAULT 0,
 consumed INT NOT NULL DEFAULT 0, address VARCHAR(64) NOT NULL
);

-- Additive: owned WB sessions and last acknowledged result. WB keeps its original graph.
CREATE TABLE IF NOT EXISTS studio_interview (
 id VARCHAR(40) PRIMARY KEY, owner_id BIGINT NOT NULL, resume_id VARCHAR(40) NOT NULL,
 request_json LONGTEXT NOT NULL, result_json LONGTEXT, status VARCHAR(20) NOT NULL,
 created_at BIGINT NOT NULL
);
CREATE TABLE IF NOT EXISTS studio_interview_meta (
 session_id VARCHAR(40) PRIMARY KEY, role_id VARCHAR(20) NOT NULL,
 kind VARCHAR(20) NOT NULL, source_session_id VARCHAR(40), source_round INT,
 question_shown_at BIGINT, completed_at BIGINT
);
CREATE TABLE IF NOT EXISTS studio_interview_turn (
 session_id VARCHAR(40) NOT NULL, round_no INT NOT NULL,
 interviewer VARCHAR(20) NOT NULL, question LONGTEXT NOT NULL, answer LONGTEXT NOT NULL,
 status VARCHAR(20) NOT NULL, asked_at BIGINT, submitted_at BIGINT NOT NULL,
 duration_ms BIGINT, feedback LONGTEXT, score_json LONGTEXT, speech_metrics_json LONGTEXT,
 PRIMARY KEY(session_id, round_no)
);

INSERT INTO user_info(id,phone,password,status,user_name,is_deleted) VALUES (2001,'19900000001','$2a$10$fIAbCHWP0XFxvFgqAJTp5enX2nPTYzfIC33iDfFXKjGZ8SKGNVJjS',1,'交接测试同学',0);
INSERT INTO studio_profile(user_id,nickname,role_id,career_level,graduation) VALUES (2001,'交接测试同学','algorithm','校招','2027');
