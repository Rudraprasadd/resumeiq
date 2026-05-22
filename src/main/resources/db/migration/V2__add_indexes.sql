-- ============================================================
-- V2__add_indexes.sql
-- Production indexes for common lookup and list queries
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_resumes_user_uploaded_at
    ON resumes(user_id, uploaded_at DESC);

CREATE INDEX IF NOT EXISTS idx_analyses_user_created_at
    ON analyses(user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_analyses_ai_cache_key
    ON analyses(ai_cache_key)
    WHERE ai_cache_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_analyses_user_created_at_range
    ON analyses(user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_subscriptions_user_created_at
    ON subscriptions(user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_subscriptions_razorpay_order_id
    ON subscriptions(razorpay_order_id)
    WHERE razorpay_order_id IS NOT NULL;
