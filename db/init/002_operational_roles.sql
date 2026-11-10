-- 新資料庫自動執行；既有資料庫需依 CAMP_PERMISSIONS.md 備份後單獨套用。
-- 不重新分配舊帳號，不刪除舊角色、權限或營運資料。
INSERT INTO role (name, description) VALUES
    ('ADMIN', '營區管理員：設定、金額與營運管理'),
    ('STAFF', '現場人員：查看預約、完成住宿與交接備註')
ON CONFLICT (name) DO NOTHING;
