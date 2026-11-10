# SCHEMA.md

## 2026-10-29：SeaweedFS 替換分支（正式主機尚未切換）

使用者指定以 SeaweedFS 取代 Camp MinIO。產品 S3 SDK、全新獨立 Volume、同源圖片路徑與真服務測試見 [SEAWEEDFS_MIGRATION](SEAWEEDFS_MIGRATION.md)。不改 SQL／migration／帳號，不將舊 MinIO volume 交給 SeaweedFS。中央 v4 是既有主機部署契約；目前正式來源仍為 `1d9b8d100654fd2ab9d0a32afbcc541c6bf5c2cd` 的已驗收快照，不以 Cloud 測試宣稱已搬遷。

儲存服務、設定 fence、發布 recipe 與圖片 URL／資料搬遷需要主機管理者另審；本輪用獨立 PR 交付，不直接合 main 或部署。以下 MinIO／舊部署描述保留為歷史；最新替換範圍與未完成主機驗收以連結文件為準。


## 展示收尾（無 schema 修改）

本輪未新增／修改 SQL、migration 或商業程式。主要流程使用 camp 專用 PG17／Redis 與合成 fixture 驗證，PG18／MinIO／全服務復原仍待本機。依 [展示稿](DEMO_WALKTHROUGH.md) 展示目前資料／權限與交易保護，不用文件百分比代表正式上線。

## 2026-10-06：通知保留試算（無 migration）

唯讀聚合 notification_record、notification_attempt、booking 與 payment_record，無新表／欄位／索引、無 DELETE。notification_record.event_key 仍是持久去重鍵；queue_managed 或有鍵者保護，attempts／金流不清理。既有無時區 timestamp 以營區本地時間試算，詳見 [NOTIFICATION_RETENTION.md](NOTIFICATION_RETENTION.md)。

## 2026-10-06：備份觀測（無 migration）

新增私有目錄中的格式 1 執行紀錄，不建 SQL 資料表、不修改角色／權限初始化資料。後端只讀紀錄並以 sourceKey／archiveSha256 核對，不從 DB 或 dump 存在推定成功；完整欄位與界限見 [BACKUP_OBSERVABILITY.md](BACKUP_OBSERVABILITY.md)。

## 2026-10-06：通知觀測（無 migration）

新增指標僅從 008 既有 notification_record 欄位聚合，不新增欄位或更改來源。範圍為 queue_managed 的 Email；due 排除預算用盡，blocked 額外計缺少排程／租約資料，failed 含 uncertain，processing 含 expired。單條 SELECT 產生計數與最早時間，讀取不鎖定領取、不改狀態；不是新的持久告警表。欄位與邊界見 [NOTIFICATION_OBSERVABILITY.md](NOTIFICATION_OBSERVABILITY.md)。

## 2026-10-06：備份核對範圍

本輪沒有 migration 或欄位變更。[備份工具](BACKUP_RESTORE.md)備份 public schema，核對逐表完整資料／筆數、欄位／CHECK／validated 外鍵／索引／普通 view／sequence 定義。Cloud PostgreSQL 17 還原演練通過；其他 schema、cluster role／ACL、MinIO、WAL／PITR 不在範圍內。sequence counter 不受 MVCC 快照保護；靜置副本另測 counter 延續與新增預約 ID。現有 public 無自訂函式／trigger；未測其他擴充物件的完整結構核對。

## 2026-10-06：通知佇列與失敗重試（Cloud）

預約／付款通知在同一資料庫交易入列，事件唯一鍵去重；PostgreSQL SKIP LOCKED 與短租約支援多 worker，退避重試、次數上限與每次嘗試紀錄保留。SMTP 結果不明時停下待人工核對，過期 worker 不能覆蓋結果。

ADMIN 通知頁可免費模擬、查看重試歷程、保留原因重新排入失敗任務、略過或確認站內提醒；模擬完成不計為已寄信，處理中與終態不能任意重送，版本衝突回 409。STAFF／匿名無權限。真實寄信與 worker 預設關閉；本輪未寄送 Email／LINE。

既有 DB 先備份並依序補缺少的 `002` 到 `008_notification_queue.sql` 再部署，不刪 volume。舊通知保留、不自動入列。設定、狀態與限制見 [NOTIFICATION_QUEUE.md](NOTIFICATION_QUEUE.md)，待補事項見 [CLOUD_LOCAL_FOLLOWUP.md](CLOUD_LOCAL_FOLLOWUP.md)，驗證見 [本輪紀錄](docs/verification/notification-queue-2026-10-06.json)。

## 2026-10-06：Cookie／CSRF 與 LINE 身分核驗（Cloud）

後台登入、刷新、登出與非公開寫入啟用 CSRF header 驗證；Vue 自動取得 token，失效最多重試一次。HTTPS 預設 Secure／HttpOnly／SameSite=Lax 與 `__Host-camp_*` cookie，HTTP 開發須明確設定 `AUTH_COOKIE_SECURE=false`。Access JWT 15 分鐘、refresh 7 天，JTI 原子輪替，升級後重新登入。

LINE 綁定／查單改為必填 ID token，由固定官方 HTTPS 接口核驗並限定逐筆已綁定的預約；自報 userId 與同電話的未綁定訂單不授權。007 新增核驗時間，保留舊資料但不追認身分。先備份並依序補缺少的 `002` 到 `007_line_identity.sql`，再更新前後端；不可刪 volume。

使用免費測試工具與 test classpath 合成 LINE adapter；未使用真實 LINE 帳號或手機，正式程式沒有偽身分 fallback。操作、API 合約、設定與驗收範圍見 [AUTH_LINE_SECURITY.md](AUTH_LINE_SECURITY.md)，本機／真實帳號及獨立 LINE 呼叫方更新列在 [CLOUD_LOCAL_FOLLOWUP.md](CLOUD_LOCAL_FOLLOWUP.md)，測試見 [本輪紀錄](docs/verification/auth-line-security-2026-10-06.json)。


## 2026-10-06：退款與人工對帳（Cloud）

ADMIN 可從付款紀錄／預約明細申請、審核、模擬退款或登記人工完成。待審核保留額度；模擬不扣帳，人工完成需憑證並扣已收淨額。原付款與訂單總額保留，版本、訂單交易鎖、識別碼及歷程回滾防止超退與重複扣帳。STAFF 與旅客不取得內部核對憑證。

晚到收款可人工歸帳或安排退款；舊帳差異需留憑證核對，不自動覆蓋。有待處理／已實退的取消訂單不可重開，已實退後不自動再收原餘額。既有 DB 先備份並補缺少的 `002` 到 `006_payment_refunds.sql`，再更新前後端；不可刪 volume。

本次全部採免費 mock／合成憑證，沒有真實金流退款。本機升級與商家憑證驗收暫緩。操作、API、帳務定義及限制見 [PAYMENT_REFUNDS.md](PAYMENT_REFUNDS.md)，待補事項見 [CLOUD_LOCAL_FOLLOWUP.md](CLOUD_LOCAL_FOLLOWUP.md)，驗證見 [本輪紀錄](docs/verification/payment-refunds-2026-10-06.json)。


## 2026-10-06：未付款期限與庫存釋放（Cloud）

新預約保留 30 分鐘（可設定），到期且未收款的 pending 預約自動取消，逐晚釋放全部營位項目；租借品隨有效訂單釋放。舊訂單不補期限，已收訂金／確認／入住／完成不自動取消。人工重開重新檢查營位與租借庫存，舊付款連結保持失效。

付款、取消、金額表單與逾時共用訂單交易鎖；付款與金額調整也增加操作版本，金額 PATCH 新增必填 `expectedVersion`。綠界瀏覽器導回僅導頁；晚到合法收款標記 `paid_review` 待人工核對，不恢復庫存或自動退款。

既有 DB 先備份，依序補缺少的 `002`、`003`、`004`、`005_booking_holds.sql` 再更新前後端；不可刪 volume。使用者電腦升級與商家 sandbox 仍暫緩；Cloud 退款／人工核對已完成，見本文件最上方最新狀態。

- [期限規則、API、設定與升級](BOOKING_HOLDS.md)
- [Cloud／本機待補清單](CLOUD_LOCAL_FOLLOWUP.md)
- [本輪驗證紀錄](docs/verification/booking-holds-2026-10-06.json)


## 2026-10-06：入住／退房登記（Cloud）

ADMIN／STAFF 都可在入住名單、列表與明細登記實際入住／退房；退房才會完成訂單。預計日期與實際狀態分開，逾期未退房仍列入當日名單／日曆。舊完成訂單保留空的實際時間，不推算補登。

一般狀態修改限定 ADMIN，僅允許尚未入住的確認、取消、重新開啟；不得直接標記完成。操作版本與訂單 row lock 防止重複登記、取消競爭及舊頁面覆寫，狀態與操作者歷程同一交易保存。日期查詢合併租借品並完整讀取分頁。

既有 DB 先備份，依序補 `002`、`003`、`004_booking_stays.sql`（已套用者可略過）再更新前後端；不可刪 volume 重建。一般狀態／住宿寫入都需 `expectedVersion`。私有 `JWT_SECRET` 與固定角色的員工管理頁仍保留；回本機的升級與營運驗收暫緩。

- [住宿登記規則、API 與升級](BOOKING_STAYS.md)
- [角色矩陣與帳號操作](CAMP_PERMISSIONS.md)
- [Cloud／本機待補清單](CLOUD_LOCAL_FOLLOWUP.md)
- [本輪驗證紀錄](docs/verification/booking-stays-2026-10-06.json)

資料來源：`db/init/001_schema.sql`、`002_operational_roles.sql`、`003_account_sessions.sql`、`004_booking_stays.sql`、`005_booking_holds.sql`、`006_payment_refunds.sql`、`007_line_identity.sql`

## 資料庫類型

- PostgreSQL
- 目前本機 DB：`fadaiting_camp`
- 目前本機帳號：`camp`

## 核心關聯總覽

- `camp_site` 是露營區主表
- `camp_zone` 從屬於 `camp_site`
- `camp_unit` 從屬於 `camp_site` 與 `camp_zone`
- `rate_plan` 從屬於 `camp_site` 與 `camp_unit`
- `availability` 從屬於 `camp_site` 與 `camp_unit`
- `rental_item` 從屬於 `camp_site`，代表可租借或加購的露營裝備主檔
- `booking` 從屬於 `camp_site`
- `booking_guest` 與 `booking` 一對一
- `booking_item` 與 `booking` 一對多，營位明細指向 `camp_unit`，租借品明細可指向 `rental_item`
- `booking_timeline` 與 `booking` 一對多，記錄狀態與處理歷程
- `payment_record` 與 `booking` 一對多，記錄付款請求與金流回應
- `payment_method_config` 記錄前台可用支付方式設定，控制 LINE Pay / 綠界是否顯示與可建立付款請求
- `notification_record` 與 `booking` 一對多，記錄通知事件與發送狀態
- `admin_operation_log` 記錄後台登入、登出與管理 API 操作，供後台稽核查詢
- `line_user_binding` 可對應 LINE 使用者與旅客聯絡資訊，供 LINE 專案查單與後續推播使用
- `announcement` / `facility` / `faq` / `site_image` 均從屬於 `camp_site`
- `role` / `permission` / `user_role` / `role_permission` 為權限系統

## 資料表整理

### app_user

用途：系統使用者 / 管理員帳號

主要欄位：

- `id`
- `username`
- `password`
- `email`
- `telephone`
- `role`
- `user_pic`
- `is_stop`
- `auth_version`：`bigint NOT NULL DEFAULT 0`，由 `003_account_sessions.sql` 補入；角色／啟用狀態異動、登出及改本人密碼時增加，用於撤銷 access／refresh 並阻止舊設定覆寫。

關聯：

- 被 `category.create_user`
- 被 `article.create_user`
- 被 `booking.member_user_id`
- 被 `announcement.created_by`
- 被 `user_role.user_id`

### category

用途：文章分類

主要欄位：

- `id`
- `category_name`
- `category_alias`
- `create_user`

關聯：

- `create_user -> app_user.id`
- 被 `article.category_id`

### article

用途：文章管理

主要欄位：

- `id`
- `title`
- `content`
- `cover_img`
- `state`
- `category_id`
- `create_user`

關聯：

- `category_id -> category.id`
- `create_user -> app_user.id`

### camp_site

用途：露營區主資料

主要欄位：

- `id`
- `name`
- `slug`
- `short_description`
- `full_description`
- `address_line`
- `city`
- `county`
- `postal_code`
- `contact_name`
- `contact_phone`
- `contact_email`
- `facebook_url`
- `check_in_time`
- `check_out_time`
- `booking_notice`
- `refund_policy`
- `is_active`

關聯：

- 被 `camp_zone`
- 被 `camp_unit`
- 被 `rate_plan`
- 被 `availability`
- 被 `rental_item`
- 被 `booking`
- 被 `site_image`
- 被 `announcement`
- 被 `facility`
- 被 `faq`

### camp_zone

用途：營區分區

主要欄位：

- `id`
- `camp_site_id`
- `name`
- `slug`
- `zone_type`
- `description`
- `sort_order`
- `is_active`

關聯：

- `camp_site_id -> camp_site.id`
- 被 `camp_unit.zone_id`
- 被 `site_image.zone_id`

### camp_unit

用途：實際可預約的營位 / 帳篷方案

主要欄位：

- `id`
- `camp_site_id`
- `zone_id`
- `name`
- `unit_type`
- `description`
- `max_guests`
- `max_vehicles`
- `max_tents`
- `has_shelter`
- `electricity_available`
- `private_bathroom`
- `quantity`
- `sort_order`
- `is_active`

關聯：

- `camp_site_id -> camp_site.id`
- `zone_id -> camp_zone.id`
- 被 `rate_plan.camp_unit_id`
- 被 `availability.camp_unit_id`
- 被 `booking_item.camp_unit_id`

### rate_plan

用途：營位價格方案

主要欄位：

- `id`
- `camp_site_id`
- `camp_unit_id`
- `name`
- `day_type`
- `base_price`
- `extra_person_price`
- `cleaning_fee`
- `description`
- `is_active`

關聯：

- `camp_site_id -> camp_site.id`
- `camp_unit_id -> camp_unit.id`
- 被 `booking_item.rate_plan_id`

### availability

用途：每日庫存與關閉狀態

主要欄位：

- `id`
- `camp_site_id`
- `camp_unit_id`
- `available_date`
- `total_quantity`
- `reserved_quantity`
- `blocked_quantity`
- `price_override`
- `is_closed`

關聯：

- `camp_site_id -> camp_site.id`
- 被 `booking_item.rental_item_id`
- `camp_unit_id -> camp_unit.id`

### rental_item

用途：可租借或加購的露營裝備主檔，例如帳棚、防水墊、炊事用品、照明設備

主要欄位：

- `id`
- `camp_site_id`
- `name`
- `category`
- `description`
- `image_url`
- `unit_price`
- `total_quantity`
- `sort_order`
- `is_active`

關聯：

- `camp_site_id -> camp_site.id`

備註：

- 後台 `/admin/camp/rental-item` 可維護租借品主檔
- 租借品圖片可透過 `POST /api/upload/admin-image` 上傳，並存入 `image_url`
- 前台 `/booking` 會透過 `GET /api/public/camp/rental-items` 顯示啟用租借品
- 租借品已接到前台預約選配、報價與訂單明細
- 日期占用庫存目前依有效訂單的入住 / 離營日期與 `booking_item.rental_item_id` 即時計算

### booking

用途：訂單主表

主要欄位：

- `id`
- `booking_no`
- `camp_site_id`
- `member_user_id`
- `status`
- `stay_status`：`not_checked_in / checked_in / checked_out`，預設未入住，不補造歷史紀錄
- `checked_in_at / checked_out_at`：實際時間，`timestamptz`，API 帶時區
- `operation_version`：`bigint NOT NULL DEFAULT 0`，狀態、住宿、收款與金額修改共用的操作版本，用於拒絕舊頁面
- `payment_due_at`：預約付款截止 `timestamptz`，舊訂單為 NULL，不追溯取消
- `expired_at`：付款逾時取消時間 `timestamptz`，由 005 migration 補入
- `source`
- `check_in_date`
- `check_out_date`
- `nights`
- `guest_count`
- `vehicle_count`
- `subtotal_amount`
- `adjustment_amount`
- `total_amount`
- `paid_amount`
- `note`
- `admin_note`
- `confirm_note`
- `cancel_reason`

關聯：

- `camp_site_id -> camp_site.id`
- `member_user_id -> app_user.id`
- 被 `booking_guest.booking_id`
- 被 `booking_item.booking_id`

住宿欄位與一致性約束由 `004_booking_stays.sql` 補入。`checked_in` 必須仍為 confirmed 且有入住時間；`checked_out` 必須為 completed 且退房時間不早於入住。狀態及住宿操作使用同一訂單 row lock，與 `booking_timeline` 同一交易；規則見 [BOOKING_STAYS.md](BOOKING_STAYS.md)。

### booking_guest

用途：訂單聯絡人資訊

主要欄位：

- `id`
- `booking_id`
- `contact_name`
- `contact_phone`
- `contact_email`
- `emergency_contact_name`
- `emergency_contact_phone`
- `arrival_note`

關聯：

- `booking_id -> booking.id`
- 與 `booking` 一對一

### booking_item

用途：訂單明細

主要欄位：

- `id`
- `booking_id`
- `camp_unit_id`
- `rate_plan_id`
- `rental_item_id`
- `item_name`
- `quantity`
- `unit_price`
- `nights`
- `line_total`

關聯：

- `booking_id -> booking.id`
- `camp_unit_id -> camp_unit.id`
- `rate_plan_id -> rate_plan.id`
- `rental_item_id -> rental_item.id`

備註：

- 營位本體會寫入一筆 `booking_item`
- 前台加選租借品時，也會以 `租借：品項名稱` 寫入額外 `booking_item`，並保存 `rental_item_id`
- 租借品日期庫存目前以有效訂單的 `booking.check_in_date / check_out_date` 加上 `booking_item.rental_item_id` 計算指定日期區間的最大占用量

### booking_timeline

用途：訂單狀態 / 金額 / 備註等處理歷程

主要欄位：

- `id`
- `booking_id`
- `action_type`
- `action_label`
- `actor_user_id`
- `actor_name`
- `detail`
- `created_time`

關聯：

- `booking_id -> booking.id`
- `actor_user_id -> app_user.id`

付款期限規則、到期索引及收款待人工核對 `paid_review` 狀態見 [BOOKING_HOLDS.md](BOOKING_HOLDS.md)。

### payment_record

用途：付款請求、第三方交易編號、付款狀態與回呼資料

主要欄位：

- `id`
- `booking_id`
- `provider`
- `payment_method`
- `status`
- `amount`
- `currency`
- `merchant_order_no`
- `provider_transaction_id`
- `payment_no`
- `payment_url`
- `callback_payload`
- `paid_time`
- `created_time`
- `modified_time`

關聯：

- `booking_id -> booking.id`

目前用途補充：

- 前台 `POST /api/public/payment/request` 會先建立一筆 `pending` 紀錄
- 前台 `POST /api/public/payment/retry` 會以訂單編號與聯絡電話核對身分，將同訂單舊的 `pending` 紀錄標記為 `expired` 後建立新付款請求
- 前台 `POST /api/public/payment/confirm` 會更新付款狀態
- 前台 `POST /api/public/payment/cancel` 會將待付款紀錄更新為 `cancelled`，重複取消不會重複新增歷程
- 前台 `POST /api/public/payment/fail` 會將待付款紀錄更新為 `failed`，重複同步不會重複新增歷程
- 正常 `paid` 收款減 `completed` 退款的淨額回寫 `booking.paid_amount`；`paid_review` 未歸帳款不計入
- 付款確認也會新增一筆 `booking_timeline`
- 付款逾時會新增一筆公開 `payment_expired` 歷程
- 後台 `POST /api/payment/manual` 可建立 `provider = manual`、`status = paid` 的人工付款紀錄，並同步訂單已收金額與處理歷程
- 綠界正式 callback 會驗證 `CheckMacValue`，並以付款紀錄狀態避免重複入帳
- 前台 `POST /api/public/booking/lookup` 會以訂單編號與聯絡電話核對後，回傳遮罩化的旅客查詢摘要
- 前台 `POST /api/public/booking/summary` 會以 `bookingId + bookingNo` 核對後，回傳付款導回頁所需的公開摘要
- 後台 `POST /api/payment/list` 會聯結 `booking`、`booking_guest` 查詢付款紀錄，供金流查帳與 callback 除錯
- 後台付款紀錄列表支援依建立日期區間篩選，列表與摘要使用相同條件

006 新增付款欄位：`operation_version bigint NOT NULL DEFAULT 0`、`reconciliation_status varchar(30) NOT NULL DEFAULT unreviewed`、`reconciliation_note text`、`reconciliation_reference varchar(100)`、`reconciled_by -> app_user.id`、`reconciled_at timestamptz`。核對狀態由服務管理 `unreviewed / accepted / refund_required / ledger_checked`；付款或退款寫入增加付款版本。`refunded_amount` 為查詢時彙總，非付款表實體欄位。

### refund_record

006 新增，一筆付款對多筆退款；非真實退款指令，操作規則見 [PAYMENT_REFUNDS.md](PAYMENT_REFUNDS.md)。

- `id integer` identity 主鍵、`payment_record_id -> payment_record.id`
- `amount numeric(12,2)` 非零正數
- `status varchar(20)`，限定 `requested / approved / rejected / completed / simulated`
- `idempotency_key uuid UNIQUE NOT NULL`；MyBatis 註冊 PostgreSQL UUID handler
- `reason varchar(1000)`、`decision_note varchar(1000)`、`external_reference varchar(100)`
- `requested_by / decided_by / completed_by -> app_user.id`
- `requested_at / decided_at / completed_at timestamptz`
- `operation_version bigint NOT NULL DEFAULT 0`，不可負數
- 人工完成必須有憑證、時間、操作者；核准以後及駁回需審核者／時間
- 同付款的憑證唯一；有付款 ID 與退款 ID 索引，UUID 全域唯一

`requested / approved / completed` 合計占可退額度；僅 `completed` 扣正常收款淨額，`simulated` 不扣帳。各動作與內部 `booking_timeline` 同一交易保存，舊說明保留在歷程。

### payment_method_config

用途：管理前台可用的線上支付方式，以及各 provider 實際使用的付款模式

主要欄位：

- `id`
- `provider`
- `display_name`
- `description`
- `payment_method`
- `sort_order`
- `is_active`
- `created_time`
- `modified_time`

目前用途補充：

- 預設資料包含 `linepay` 與 `ecpay`
- 後台 `/admin/booking/payment-methods` 可維護顯示名稱、說明、付款模式、排序與前台啟用狀態
- 前台 `GET /api/public/payment/methods` 只回傳 `is_active = true` 的支付方式
- 前台 `/booking/success` 會依這份設定動態顯示付款入口
- 後端建立付款請求時會再次檢查 provider 是否啟用，避免前台繞過隱藏狀態

### notification_record

用途：通知事件、收件對象、發送狀態與補送追蹤

主要欄位：

- `id`
- `booking_id`
- `channel`
- `event_type`
- `recipient`
- `recipient_name`
- `subject`
- `message`
- `status`
- `provider`
- `error_message`
- `payload`
- `sent_time`
- `created_time`
- `modified_time`

關聯：

- `booking_id -> booking.id`

目前用途補充：

- 前台送單成功時會建立 `booking_submitted`
- 付款成功時會建立 `payment_confirmed`
- 後台新訂單提醒也會建立 `admin_new_booking`
- 後台可用 `POST /api/notification/list` 查詢
- 後台可用 `PATCH /api/notification/status` 手動更新狀態
- 後台可用 `POST /api/notification/send-queued` 手動處理待寄 Email
- 後台可用 `POST /api/notification/departure-reminders` 依入住日期產生 `departure_reminder`，同訂單不重複建立

### admin_operation_log

用途：後台操作稽核紀錄，包含登入成功 / 失敗、登出與後台 API 操作

主要欄位：

- `id`
- `user_id`
- `username`
- `action_type`
- `action_label`
- `request_method`
- `request_path`
- `query_string`
- `status_code`
- `success`
- `message`
- `ip_address`
- `user_agent`
- `created_time`

目前用途補充：

- 後台登入成功、登入失敗與登出會由 `AdminUserController` 寫入
- 後台管理 API 會由 `AdminOperationLogInterceptor` 自動記錄查看、新增、更新與刪除操作
- 操作內容會依 API 路徑與 request body 轉成具體中文描述，例如查看預約列表、更新營區區域、補登人工付款等
- 後台可用 `POST /api/operation-log/list` 查詢，支援帳號、操作類型、成功狀態、路徑與日期區間篩選
- 這張表屬於稽核用途，不影響原本業務交易資料

### line_user_binding

用途：主專案保存 LINE 使用者與旅客聯絡資訊的綁定結果，供獨立 LINE 專案查詢訂單與後續通知使用

主要欄位：

- `id`
- `line_user_id`
- `display_name`
- `picture_url`
- `booking_id`
- `booking_no`
- `contact_name`
- `contact_phone`
- `contact_email`
- `status`
- `bind_token`
- `bind_token_expire_time`
- `bound_at`
- `last_interaction_time`
- `created_time`
- `modified_time`

關聯：

- `booking_id -> booking.id`

目前用途補充：

- `POST /api/public/line/bind/request` 會用 `bookingNo + contactPhone` 驗證訂單並建立短效綁定請求
- `POST /api/public/line/bind/confirm` 會用 `bindToken + lineUserId` 完成綁定
- `POST /api/public/line/booking/list` 可依 `lineUserId` 查詢該綁定聯絡人的訂單列表
- `POST /api/public/line/booking/detail` 可依 `lineUserId + bookingId` 查詢單筆訂單公開摘要
- 這是 LINE 專案拆分後的主資料入口之一，不建議由 LINE 專案直接改核心 `booking` / `payment_record`

007 增加 `identity_verified_at timestamptz`，舊記錄保持 NULL，不授權查單；新的已核驗 active 綁定才可查閱對應預約。`idx_line_verified_booking` 對已核驗 active 的 booking ID 唯一，`idx_line_verified_user` 用於 subject／預約查詢；舊未核驗 active 不受唯一索引限制。bind token 有效 10 分鐘並固定給申請時核驗的 subject，確認時同一交易鎖定 booking 與 binding，失敗全部回滾。同電話不授予其他訂單讀取權。詳見 [AUTH_LINE_SECURITY.md](AUTH_LINE_SECURITY.md)。

### site_image

用途：營區相簿 / 圖片管理

主要欄位：

- `id`
- `camp_site_id`
- `zone_id`
- `title`
- `image_url`
- `alt_text`
- `sort_order`
- `is_cover`
- `is_active`

關聯：

- `camp_site_id -> camp_site.id`
- `zone_id -> camp_zone.id`

### announcement

用途：公告 / 最新消息

主要欄位：

- `id`
- `camp_site_id`
- `title`
- `content`
- `publish_start_time`
- `publish_end_time`
- `is_pinned`
- `is_active`
- `created_by`

關聯：

- `camp_site_id -> camp_site.id`
- `created_by -> app_user.id`

### facility

用途：營區設施 / 特色

主要欄位：

- `id`
- `camp_site_id`
- `name`
- `icon`
- `description`
- `sort_order`
- `is_active`

關聯：

- `camp_site_id -> camp_site.id`

### faq

用途：常見問題

主要欄位：

- `id`
- `camp_site_id`
- `question`
- `answer`
- `sort_order`
- `is_active`

關聯：

- `camp_site_id -> camp_site.id`

### role

用途：角色主表

主要欄位：

- `id`
- `name`
- `description`

關聯：

- 被 `role_permission.role_id`
- 被 `user_role.role_id`

### permission

用途：權限主表

主要欄位：

- `id`
- `name`
- `description`
- `parent_id`
- `level`
- `url`
- `method`
- `admin_router`
- `permission_type`

關聯：

- `parent_id -> permission.id`
- 被 `role_permission.permission_id`

### role_permission

用途：角色與權限對照

主要欄位：

- `role_id`
- `permission_id`

關聯：

- `role_id -> role.id`
- `permission_id -> permission.id`

### user_role

用途：使用者與角色對照

主要欄位：

- `user_id`
- `role_id`

關聯：

- `user_id -> app_user.id`
- `role_id -> role.id`

## View

### permissions_backend

用途：過濾 `permission_type = 'backend'` 的權限資料

### permissions_frontend

用途：過濾 `permission_type = 'frontend'` 的權限資料

## 初始化資料

目前 schema 內已內建：

- 預設角色：
  - `admin`
  - `user`
- 預設管理員：
  - `admin001`
- 預設營區：
  - `發呆亭露營區`
- 預設分區：
  - `雨棚區`
  - `草地營位區`
  - `免搭帳區`
- 預設營位、價格、設施、FAQ、公告
- 預設後台營區區域 CRUD 權限
- 預設後台營位 CRUD 權限
- 預設後台價格方案 CRUD 權限
- 預設後台每日庫存 CRUD 權限
- 預設後台租借品 CRUD 權限
- 預設後台圖片上傳權限
- 預設後台支付方式管理權限
- 預設後台操作紀錄查詢權限
- 預設支付方式設定：LINE Pay、綠界科技 ECPay

## 008 通知佇列增量

- `notification_record` 新增 `queue_managed boolean`（舊列 false）、`event_key varchar(180)`、`attempt_count / attempt_limit integer`、`next_attempt_at / lease_until timestamptz`、`lease_token uuid`、`delivery_mode varchar(20)`、`error_code varchar(50)`、`operation_version bigint`。
- 部分唯一 `idx_notification_event_key` 防重複事件；`idx_notification_due / idx_notification_lease` 支援到期及中斷查詢。
- `notification_attempt`：bigint identity PK、notification FK cascade、累積 attempt_number、唯一 lease_token、mode／status／固定 error_code、started_at／finished_at timestamptz、actor_user_id／actor_name。`(notification_id, attempt_number)` 唯一。API 不回租約 token。
- `sent_time` 沿用 timestamp，只有 SMTP 正常完成才更新；模擬保持 NULL，站內確認獨立 acknowledged。008 不補寫舊事件或嘗試資料。
- SQL 來源：`db/init/008_notification_queue.sql`；既有環境備份後依序套用，新環境讀取八份初始化 SQL。
