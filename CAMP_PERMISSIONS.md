# 單一營區的後台角色

本輪將後台收斂為固定的 `ADMIN`（營區管理員）與 `STAFF`（現場人員）。JWT、Redis、MyBatis 與既有選單格式保留，API 授權改由固定角色政策決定。管理員在「系統管理 → 員工帳號」分配角色，被異動的帳號需重新登入。

## 操作界線

| 功能 | ADMIN | STAFF |
| --- | --- | --- |
| 營運看板、預約列表、訂房日曆、入住名單與明細 | 可 | 可 |
| 內部交接備註 | 可 | 可，使用獨立的 `/api/booking/note` |
| 登記實際入住／退房 | 可；退房才標記完成 | 可；相同日期及狀態規則 |
| 確認、取消、重新開啟預約及未付款逾時批次 | 可；批次僅處理已到期未收款訂單 | 不可 |
| 改價、修改已收金額、人工補登收款 | 可 | 不可 |
| 付款紀錄／原始 callback、付款方式設定 | 可 | 不可；訂單內僅提供去除 callback 與付款連結的紀錄 |
| 退款申請／審核／模擬／人工完成與收款核對 | 可；需版本及處理說明，人工完成需憑證 | 不可；不提供內部憑證與說明 |
| 官網內容、營位、價格、每日庫存與用品設定 | 可 | 不可 |
| 通知管理、操作紀錄 | 可 | 不可 |
| 員工帳號建立、固定角色異動、啟用／停用 | 可；至少保留一位啟用管理員 | 不可 |
| 自己的個人資料、頭像、密碼 | 可 | 可；不能指定他人 ID 或帳號 |
| 舊版自訂角色、細粒度權限編輯 API | 不開放 | 不開放 |

訂單保留 `pending / confirmed / cancelled / completed`，另外保存實際 `stay_status` 與帶時區的入住／退房時間。預計日期與實際登記分開；規則與升級見 [BOOKING_STAYS.md](BOOKING_STAYS.md)。

## 後端行為

- 每次管理端請求重新讀取 `app_user.is_stop` 與 `user_role → role`。Redis 舊權限資料不授予操作權力。
- 員工管理異動角色或狀態、登出、修改本人密碼時增加 `app_user.auth_version`。每次請求及 refresh 都核對 JWT 的登入版本；舊 access／refresh 立即失效，重新啟用後也不會恢復舊憑證。舊憑證呼叫登出只清 cookie，不撤銷後來的新登入。
- 手動維護 DB 的角色時仍由每次 API 的角色查詢決定授權；請同步增加登入版本，避免之後恢復角色時舊 JWT 重新可用。
- 權限不足回 HTTP `403`；未登入、session 失效或把 refresh token 當作 access token 使用回 `401`。前端遇到 `403` 保留工作畫面，不重新刷新登入。
- ADMIN／STAFF 使用 `/booking/stay` 登記入住／退房，狀態與操作者歷程同一交易保存；核對操作版本並鎖定訂單，避免覆蓋同時取消。`/booking/status` 限 ADMIN，不能跳過登記直接完成。
- 交接備註 API 只接收訂單 ID 與備註，不更新金額、確認備註或取消原因；保留操作者歷程。
- 公開預約、付款 callback 與 LINE webhook 不套用後台角色。預約期限、付款鎖、綠界驗簽與瀏覽器唯讀導回規則見 [BOOKING_HOLDS.md](BOOKING_HOLDS.md)。
- 金額修改需 `expectedVersion`，避免舊表單覆蓋付款；逾時批次 `/booking/expire-unpaid` 只開放 ADMIN。
- 後台非公開寫入（含登入／refresh／logout）須 CSRF header，角色檢查仍獨立執行；HTTPS 安全 cookie、刷新規則及 LINE 逐筆綁定見 [AUTH_LINE_SECURITY.md](AUTH_LINE_SECURITY.md)。
- 固定角色政策採列舉路徑與 HTTP method；新增管理 API 時需明確加入，沒有管理員萬用通行路徑。
- `permission / role_permission` 留在資料庫，僅沿用必要選單中繼資料。舊角色／權限頁沒有重新出現在後台。

## 新資料庫與既有資料庫

新建資料庫依序執行 `db/init/001_schema.sql`、`002_operational_roles.sql`、`003_account_sessions.sql`、`004_booking_stays.sql`、`005_booking_holds.sql`、`006_payment_refunds.sql`、`007_line_identity.sql`。第二份只補入 `ADMIN / STAFF`，第三份只補 `auth_version bigint NOT NULL DEFAULT 0`，第四份補住宿欄位與一致性約束，第五份補預約付款期限／逾時時間，第六份補付款核對／版本與退款表，第七份補 LINE 核驗時間／授權索引；重跑不會刪除或重配帳號。

既有資料庫不會因 Compose 重啟而重新執行初始化 SQL。先備份，盤點帳號與角色，再依序補入第二、第三、第四、第五、第六、第七份 SQL，之後才啟動新版後端。已套用 `002` 可略過；升級後所有既有登入需重新登入。不要刪除 volume 或整份重跑舊 seed：

```sql
SELECT u.id, u.username, u.is_stop, r.name
FROM app_user u
LEFT JOIN user_role ur ON ur.user_id = u.id
LEFT JOIN role r ON r.id = ur.role_id
ORDER BY u.id, r.name;
```

相容原本精確命名為 `admin` 的角色，其操作效果為 `ADMIN`，不需改帳號或密碼。`user`、其他自訂角色與 `app_user.role` 字串不會自動變成現場人員。若一個帳號同時具有 ADMIN 與 STAFF，以 ADMIN 為準。

退款與對帳 API 限定 ADMIN；角色於取得訂單交易鎖後再次核對。操作與帳務界線見 [PAYMENT_REFUNDS.md](PAYMENT_REFUNDS.md)。

## 員工帳號管理

管理員登入後，從「系統管理 → 員工帳號」開啟 `/admin/staff`：

1. 「建立員工帳號」輸入 8–20 碼英數字／底線帳號、初始密碼及角色；預設現場人員。密碼為 8–24 碼、至少含大小寫／數字／符號中的三種，bcrypt 儲存，UI 成功後清除密碼。
2. 「調整角色與狀態」選固定角色與啟用狀態，確認後儲存。異動自己的帳號會回登入頁，異動他人則要求對方重新登入。
3. `409` 表示同名帳號、最後一位管理員保護或資料已更新；關閉對話框、重新載入後再核對。沒有變更時不送出更新、不撤銷登入。

| API（前端 proxy 路徑） | 用途 |
| --- | --- |
| `GET /api/admin/staff?username=&pageNum=1&pageSize=20` | 帳號搜尋與分頁，pageSize 上限 100 |
| `POST /api/admin/staff` | `username / password / confirmPassword / campRole`，姓名選填 |
| `PATCH /api/admin/staff/{id}` | `campRole / enabled / expectedVersion`；版本取列表 `authVersion` |

上述 API 限 ADMIN。列表僅提供既有 `admin / ADMIN / STAFF` 帳號，不回傳密碼、雜湊、旅客聯絡資訊。建立與角色／狀態更新同一交易完成；PostgreSQL 交易鎖保護最後一位啟用管理員，等待鎖後重新核對操作者。樂觀版本檢查阻止舊頁面覆蓋新設定，交易失敗會連角色與登入版本一起回滾。

一般旅客／舊自訂角色不自動轉為員工，若確實需要轉換既有帳號，留待備份後的資料庫維護窗口核對；手動 SQL 不受管理 API 交易鎖保護。此頁不提供刪除帳號、管理員重設他人密碼或任意權限編輯。員工可以在個人資料頁修改自己的密碼。

## 登入金鑰

原 JWT 共用金鑰已移除。啟動前必須設定至少 32 字元的私有 `JWT_SECRET`；建議產生隨機值，保留於本機環境變數或 Git 忽略的根目錄 `.env`。更換金鑰後所有既有 JWT 需重新登入。

Windows 本機直接啟動 Java 時，可在同一個 PowerShell 工作階段設定：

```powershell
$env:JWT_SECRET = node -p "require('node:crypto').randomBytes(48).toString('base64url')"
```

Compose 會讀取根目錄 `.env` 的 `JWT_SECRET`，沒有設定時直接停止啟動；本機 Java 不會自動載入這份 `.env`。不把共用預設金鑰寫回設定檔。

測試帳號的 Excel／userscript 仍依專案 skill 同步，全部忽略 Git；正式 Vite build 額外移除 `local-userscripts`，不將明文帳密輔助檔包入可部署內容。

## Cloud 驗證

2026-10-06 接著補完實際入住／退房、逾期住宿及日期分頁；結果見 [住宿驗證紀錄](docs/verification/booking-stays-2026-10-06.json)。先前 STAFF 可經一般狀態 API 直接完成的流程已由登記退房取代，歷史紀錄不代表目前授權。

2026-10-06 補完帳號管理，通過 34 項後端測試、39 項真實 HTTP／PostgreSQL 交易與併發檢查，16 項瀏覽器操作與前端型別／正式建置驗證。見 [員工帳號驗證紀錄](docs/verification/staff-accounts-2026-10-06.json)。前一輪紀錄保留為歷史結果，角色異動後須重新登入的行為以本輪為準。

2026-10-05 使用 Java 25、真實 PostgreSQL 17／Redis 7、Spring Boot API 與 Chromium 檢查。本輪測試資料與預約皆為隔離合成資料，未修改任何既有營運資料庫。驗證摘要見 [權限驗證紀錄](docs/verification/camp-permissions-2026-10-05.json)。

重跑建置前先依 README 啟動隔離的 PostgreSQL／Redis，設定私有 `JWT_SECRET` 並使用 Java 25：

```bash
mvn -f backend/pom.xml package
cd frontend
npm ci
npm run build
```

若 DB／Redis 使用非預設 port，透過 `SPRING_DATASOURCE_URL`、`SPRING_DATA_REDIS_HOST`、`SPRING_DATA_REDIS_PORT` 指定。Cloud 的 HTTP／瀏覽器檢查使用合成預約與獨立帳號，包含直接越權請求、舊 Redis 權限偽造、角色撤銷、停用、登出及本人資料限制；驗證 JSON 不含帳密。

本機後續事項見 [CLOUD_LOCAL_FOLLOWUP.md](CLOUD_LOCAL_FOLLOWUP.md)。這次角色收斂不代表金流、LINE 綁定、正式帳號安全與部署已完成驗收。

通知佇列摘要、嘗試紀錄、免費模擬及人工重新排入均僅 ADMIN；沿用 CSRF 與版本檢查，STAFF 不取得通知內容。升級再補 `008_notification_queue.sql`，規則見 [NOTIFICATION_QUEUE.md](NOTIFICATION_QUEUE.md)。
