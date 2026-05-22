-- ============================================================
-- V2__add_indexes.sql
-- Performance indexes and any schema additions for Phase 3/4
-- ============================================================

-- Index on ai_cache_key for fast cache lookups
CREATE INDEX IF NOT EXISTS idx_analyses_cache_key ON analyses(ai_cache_key);

-- Index on subscriptions for fast webhook idempotency checks
CREATE INDEX IF NOT EXISTS idx_subscriptions_order_id ON subscriptions(razorpay_order_id);