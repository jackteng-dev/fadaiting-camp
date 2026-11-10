-- 僅新預約設定付款期限；既有訂單保留 NULL，不追溯取消。
ALTER TABLE booking ADD COLUMN IF NOT EXISTS payment_due_at timestamptz;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS expired_at timestamptz;
CREATE INDEX IF NOT EXISTS idx_booking_payment_due
    ON booking (payment_due_at, id)
    WHERE status = 'pending' AND stay_status = 'not_checked_in' AND paid_amount = 0;
