-- ============================================================
-- V1__initial_schema.sql
-- ResumeIQ - Complete Database Schema
-- ============================================================
-- Flyway runs this automatically on first startup.
-- To add a new table later, create V2__add_something.sql
-- ============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255),
    plan          VARCHAR(20)  NOT NULL DEFAULT 'FREE',   -- FREE | PRO
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_plan CHECK (plan IN ('FREE', 'PRO'))
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================================
-- SUBSCRIPTIONS (payment / plan management)
-- ============================================================
CREATE TABLE subscriptions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan                VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | EXPIRED | CANCELLED
    razorpay_order_id   VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    amount_paise        INTEGER,                 -- amount in paise (₹99 = 9900)
    starts_at           TIMESTAMP   NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMP,               -- NULL = free plan never expires
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_sub_plan   CHECK (plan   IN ('FREE', 'PRO')),
    CONSTRAINT chk_sub_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_subscriptions_user_id ON subscriptions(user_id);

-- ============================================================
-- USAGE QUOTA (track free-tier usage per month)
-- ============================================================
CREATE TABLE usage_quota (
    id            UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id       UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    month_year    CHAR(7) NOT NULL,   -- e.g. "2025-09"
    analysis_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(user_id, month_year)
);

-- ============================================================
-- RESUMES (uploaded files)
-- ============================================================
CREATE TABLE resumes (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    storage_key  VARCHAR(500) NOT NULL,    -- local path or S3 key
    content_text TEXT,                     -- extracted plain text from PDF
    file_size_kb INTEGER,
    uploaded_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resumes_user_id ON resumes(user_id);

-- ============================================================
-- ANALYSES (AI comparison results)
-- ============================================================
CREATE TABLE analyses (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resume_id       UUID    NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    job_description TEXT    NOT NULL,
    job_title       VARCHAR(255),
    company_name    VARCHAR(255),

    -- AI results stored as JSON columns for flexibility
    match_score         INTEGER,            -- 0-100
    missing_keywords    JSONB,              -- ["Java", "Spring Boot", ...]
    keyword_matches     JSONB,              -- {"Java": true, "Python": false, ...}
    rewrite_suggestions JSONB,              -- [{original: "...", improved: "..."}, ...]
    ats_score           INTEGER,            -- 0-100
    cover_letter        TEXT,
    interview_questions JSONB,              -- ["Tell me about ...", ...]

    -- Caching
    ai_cache_key    VARCHAR(64),            -- SHA-256 hash of resume+JD for Redis cache
    processing_time_ms BIGINT,

    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_score CHECK (match_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_analyses_user_id  ON analyses(user_id);
CREATE INDEX idx_analyses_resume_id ON analyses(resume_id);

-- ============================================================
-- REFRESH TOKENS (for JWT rotation)
-- ============================================================
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token   ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ============================================================
-- AUTO-UPDATE updated_at TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE PROCEDURE trigger_set_updated_at();