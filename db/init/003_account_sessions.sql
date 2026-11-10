-- 既有 DB 先備份後單獨套用；只補版本欄位，不修改帳號、角色或預約資料。
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS auth_version bigint NOT NULL DEFAULT 0;
