-- 既有 LINE 綁定保留，但不追認其使用者身分；需重新完成 ID token 驗證與綁定。
ALTER TABLE line_user_binding ADD COLUMN IF NOT EXISTS identity_verified_at timestamptz;
CREATE UNIQUE INDEX IF NOT EXISTS idx_line_verified_booking
    ON line_user_binding(booking_id) WHERE status = 'active' AND identity_verified_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_line_verified_user
    ON line_user_binding(line_user_id, booking_id) WHERE status = 'active' AND identity_verified_at IS NOT NULL;
