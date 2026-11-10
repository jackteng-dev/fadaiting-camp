-- 先備份既有資料庫再單獨套用；不推算或補寫歷史入住紀錄。
ALTER TABLE booking ADD COLUMN IF NOT EXISTS stay_status varchar(20) NOT NULL DEFAULT 'not_checked_in';
ALTER TABLE booking ADD COLUMN IF NOT EXISTS checked_in_at timestamptz;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS checked_out_at timestamptz;
ALTER TABLE booking ADD COLUMN IF NOT EXISTS operation_version bigint NOT NULL DEFAULT 0;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'booking_stay_state_check' AND conrelid = 'booking'::regclass) THEN
        ALTER TABLE booking ADD CONSTRAINT booking_stay_state_check CHECK (
            (stay_status = 'not_checked_in' AND checked_in_at IS NULL AND checked_out_at IS NULL)
            OR (stay_status = 'checked_in' AND status = 'confirmed' AND checked_in_at IS NOT NULL AND checked_out_at IS NULL)
            OR (stay_status = 'checked_out' AND status = 'completed' AND checked_in_at IS NOT NULL AND checked_out_at IS NOT NULL AND checked_out_at >= checked_in_at)
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'booking_operation_version_check' AND conrelid = 'booking'::regclass) THEN
        ALTER TABLE booking ADD CONSTRAINT booking_operation_version_check CHECK (operation_version >= 0);
    END IF;
END $$;
