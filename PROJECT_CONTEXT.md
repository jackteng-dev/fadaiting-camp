# PROJECT_CONTEXT.md

## 2026-10-29：SeaweedFS 替換分支（正式主機尚未切換）

使用者指定以 SeaweedFS 取代 Camp MinIO。產品 S3 SDK、全新獨立 Volume、同源圖片路徑與真服務測試見 [SEAWEEDFS_MIGRATION](SEAWEEDFS_MIGRATION.md)。不改 SQL／migration／帳號，不將舊 MinIO volume 交給 SeaweedFS。中央 v4 是既有主機部署契約；目前正式來源仍為 `1d9b8d100654fd2ab9d0a32afbcc541c6bf5c2cd` 的已驗收快照，不以 Cloud 測試宣稱已搬遷。

儲存服務、設定 fence、發布 recipe 與圖片 URL／資料搬遷需要主機管理者另審；本輪用獨立 PR 交付，不直接合 main 或部署。以下 MinIO／舊部署描述保留為歷史；最新替換範圍與未完成主機驗收以連結文件為準。


## 展示停點與跨作品接續（2026-10-06）

發呆亭展示範圍已收斂，本輪只整理文件並驗證既有主要流程，不新增商業程式、migration 或運維功能。後續封存／清理／排程／報表等延期；下一個作品為 ELPSY Shop，須先確認正確倉庫。入口：[展示稿](DEMO_WALKTHROUGH.md)、[目前待辦](TODO_AI.md)、[最新交接](docs/HANDOFF.md)。

## 2026-10-06：通知保留 dry-run

新增 ADMIN `GET /notification/retention-preview`，單一 Clock／一條 SQL snapshot 分類全站歷史。180 天只是可設定試算草案；結果不含通知 ID／個資，不改資料、不提供刪除。保留 event_key／新版佇列避免破壞去重，保留 attempts／金流稽核；通知列表與試算獨立。無 migration，見 [操作文件](NOTIFICATION_RETENTION.md)。

## 2026-10-06：備份觀測里程碑

新增 Python 原子執行紀錄、ADMIN 專用 `GET /admin/backup/status` 與後台頁；以來源識別碼及完整備份 SHA-256 匹配成功還原，分開保存嘗試與成功歷史。無 migration，讀取不執行 Docker；可選 Compose overlay 唯讀掛載。詳見 [操作文件](BACKUP_OBSERVABILITY.md)。

## 2026-10-06：通知觀測里程碑

沿用 ADMIN `GET /notification/queue`，一條 SQL 彙總新版 Email 佇列，增加觀測時間／最早到期／等待秒數、結果不明／逾期 lease／blocked 與分級告警。頁面明示全站範圍，HTTP 失敗清除舊指標，較舊請求不覆蓋最新；讀取無寫入／寄信副作用。門檻預設 900 秒，Compose 同步。無 migration；正式 Bootstrap 改 npm ESM 並保留模板全域物件，詳見 [操作文件](NOTIFICATION_OBSERVABILITY.md)。

## 2026-10-06：備份／隔離還原里程碑

已提供 public schema 的 PostgreSQL 自訂格式備份、同快照逐表指紋、全新無網路容器還原與自動清理；Cloud PostgreSQL 17 完整 17 項測試通過。來源不寫入，測試 fixture 僅存在隔離副本；沒有 migration 或商業程式變更。操作及容量／版本限制見 [BACKUP_RESTORE.md](BACKUP_RESTORE.md)，實際本機 PostgreSQL 18／MinIO／正式切換留待 [待補清單](CLOUD_LOCAL_FOLLOWUP.md)。

> 2026-10-06：工作流程適用於整個專案展示平台，各作品各自同步 main／驗證／交接／推送。最新狀態見 [docs/HANDOFF.md](docs/HANDOFF.md)，平台入口見 [共同規範](https://github.com/jackteng-dev/developer-portfolio/blob/main/docs/PLATFORM_WORKFLOW.md)。

## 2026-10-06：固定 main 工作與交接流程

最新工作基準改為遠端 `main`，已整合至 `c055d49` 的員工權限、住宿、付款、退款、身分安全與通知佇列成果。每輪先同步遠端 main、閱讀 [docs/HANDOFF.md](docs/HANDOFF.md)，再核對工作目錄；完成測試與 production build 後更新交接、commit 並 push main。固定規範見 [AGENTS.md](AGENTS.md)。

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

## 專案名稱

發呆亭露營區官網與訂位平台

## 專案目的

將目前專案收斂成單一露營區的正式對外網站與後台管理平台，支援：

- 前台品牌官網
- 營位介紹
- 營區地圖
- 線上預約
- 最新消息 / FAQ
- 後台營區與預約管理

## 目前完成度判斷

- 目前估計完成度：約 `65% - 75%`
- 目前已達：
  - 可 demo
  - 可內部試跑主要預約與付款 mock 流程
  - 可用於持續開發正式營運版本
- 目前尚未達：
  - 正式金流上線
  - 正式通知系統
  - 完整訂單生命週期
  - 正式部署與營運監控

## 技術棧

### 前端

- Vue 3
- TypeScript
- Composition API
- Vite
- Pinia
- Vue Router
- Element Plus
- Bootstrap / Bootstrap Icons

### 後端

- Spring Boot 4.0.6
- Java 25
- MyBatis
- Spring Security
- JWT
- Redis
- PostgreSQL
- MinIO

### 基礎設施

- Docker Compose
- Nginx（frontend production image）

## 專案目錄結構

```text
backend/                    Spring Boot API
  src/main/java/com/fadaiting/camp/
    controller/             API 控制器
    service/                商業邏輯介面
    service/impl/           商業邏輯實作
    mapper/                 MyBatis 存取層
    dto/                    請求 / 回應 DTO
    pojo/                   資料模型
    utils/                  JWT / Security / 共用工具
  src/main/resources/
    application.yml         後端設定

frontend/
  src/
    api/                    前端 API 呼叫封裝
    components/
      layout/               後台 layout
      storefront/           前台官網共用元件
    router/                 路由設定
    stores/                 Pinia store
    views/
      storefront/           前台頁面
      camp/                 後台營區頁面
      booking/              後台預約頁面
      account/role/user/    舊有後台模組；目前僅保留登入與個人資料相關能力

fadaiting-line/
  server/                   獨立 LINE webhook / API proxy 骨架
  web/                      獨立 LINE / LIFF 原型前端

db/
  init/
    001_schema.sql          PostgreSQL schema 與初始化資料

docker-compose.yml          PostgreSQL / Redis / MinIO
docker-compose.dev.yml      Docker 開發模式前後端
docker-compose.prod.yml     Docker production stack
```

## 已完成功能

- Docker 開發環境已可啟動 PostgreSQL / Redis / MinIO / backend / frontend
- 後端已完成露營區核心 schema
- 前台官網已拆分為獨立 storefront layout
- 前台已完成頁面：
  - `/` 首頁
  - `/zones` 營位介紹
  - `/booking` 線上預約
  - `/booking/lookup` 查詢預約
  - `/map` 營區地圖
  - `/news` 最新消息
  - `/faq` 常見問題
- 首頁目前已收斂為：
  - hero
  - 關於我們
  - 照片相簿
  - footer 聯絡資訊
- 前後台已分流：
  - 前台使用 `StorefrontLayout`
  - 後台使用 `BaseLayout`
- 後台已完成：
  - 管理員登入
  - 權限動態路由
  - 查詢預約頁
  - 預約明細頁
  - 營區基本資料頁
  - 營區區域管理頁
  - 營位管理頁
  - 租借品管理頁
  - 公告管理頁
  - FAQ 管理頁
  - 相簿管理頁
  - 後台操作紀錄頁
- 公開 API 已可提供：
  - 營區資訊
  - 分區與營位
  - 公告
  - 預約即時報價 / 可售檢查
  - 預約送單
- 後台 API 已可提供：
  - 預約列表
  - 預約狀態更新
  - 營區基本資料讀寫
  - 營區區域 CRUD
  - 營位 CRUD
  - 價格方案 CRUD
  - 每日庫存 CRUD
  - 租借品 CRUD
  - 操作紀錄查詢
- 專案命名已更新：
  - 前端 package：`fadaiting-camp`
  - 後端 artifact：`fadaiting-camp-api`
  - Java package：`com.fadaiting.camp`
  - context path：`/fadaiting-camp`
  - DB：`fadaiting_camp`
- Codex 測試帳號同步 skill 已保留於 `codex-skills/fadaiting-camp-account-test-sync`
- Tampermonkey 後台登入自動填密碼已改為 `header + local runtime @require` 模式

## 近期重要變更

### 2026-05-26

- 專案命名從舊電商 `shop` / `ELPSY` 顯示名稱收斂為發呆亭露營區專案。
- 前端品牌顯示已從 Mantis 模板改為 `發呆亭露營區`。
- 後端 context path 已改為 `/fadaiting-camp`。
- 後端 artifact 已改為 `fadaiting-camp-api`。
- Java package 已改為 `com.fadaiting.camp`。
- Docker 基礎服務 container 已改為 `fadaiting-camp-postgres`、`fadaiting-camp-redis`、`fadaiting-camp-minio`。
- PostgreSQL 新 DB 為 `fadaiting_camp`，使用者為 `camp`。
- 新 DB 中文資料曾因 Windows PowerShell 管線轉碼變成 `?`，已用 container 內 `pg_dump -Fc` / `pg_restore` 重新還原修復。
- `fadaiting-camp-account-test-sync` skill 已複製到 repo 的 `codex-skills/fadaiting-camp-account-test-sync`，方便新電腦安裝。

### 2026-05-27

- 後台價格方案與每日庫存已完成前後端 CRUD。
- `db/init/001_schema.sql` 權限 seed 已補齊價格 / 庫存管理相關權限。
- 本機資料庫已同步到目前 SQL，`rate_plan` seed 可正常查詢。
- `fadaiting-camp-account-test-sync` skill 已改成產生：
  - `admin-login-autofill.user.js`
  - `admin-login-autofill.runtime.js`
- Tampermonkey userscript 本體只保留 header，真正帳密映射放在本機 runtime 檔，更新測試帳號後通常只需重新整理登入頁即可生效。

### 2026-05-28

- 後台公告管理已完成前後端 CRUD。
- `/api/camp/announcement` 現在同時提供後台公告列表與新增 / 更新 / 刪除。
- 前端已新增 `/admin/camp/announcement` 管理頁，並接入動態路由映射。
- 權限 seed 已補齊公告管理的 GET / POST / PUT / DELETE 權限。
- 後台 FAQ 管理已完成前後端 CRUD。
- `/api/camp/faq` 現在提供後台 FAQ 列表與新增 / 更新 / 刪除。
- 前端已新增 `/admin/camp/faq` 管理頁，並接入動態路由映射。
- 權限 seed 已補齊 FAQ 管理的 GET / POST / PUT / DELETE 權限。
- 後台相簿管理已完成前後端 CRUD。
- `/api/camp/gallery` 現在提供後台相簿列表與新增 / 更新 / 刪除。
- 前端已新增 `/admin/camp/gallery` 管理頁，並接入動態路由映射。
- 首頁照片相簿已改成優先讀取 `site_image` 真實資料，沒有資料時才回退示意圖。
- 後台預約明細頁已完成，列表改為導向獨立明細頁。
- `/api/booking/detail/{bookingId}` 現在提供完整 `booking / guest / items` 明細資料。
- 後台預約明細現在可直接更新已收金額、調整金額與內部備註，總額會依小計加上調整金額即時重算。
- 後台預約列表與明細頁現在會依 `paidAmount / totalAmount` 顯示付款狀態，明細頁也提供未收 / 50% 訂金 / 收清的快速帶入。
- 後台訂單明細頁已可補登人工付款，會建立 `payment_record`、同步 `booking.paid_amount`、寫入處理歷程，並建立付款成功通知紀錄。
- 後台訂單現在拆分出 `客人備註 / 內部備註 / 人工確認備註 / 取消原因`，避免把營運處理紀錄混在同一欄。
- 後台預約明細現在會顯示處理歷程，包含前台送單、後台改狀態、改金額與備註等紀錄，並記錄操作者。
- 後台已新增付款紀錄頁 `/admin/booking/payments`，可依日期區間查詢付款單、訂單、金流 provider、狀態、交易編號與 callback payload，並顯示摘要統計與匯出 CSV。
- 後台已新增支付方式管理頁 `/admin/booking/payment-methods`，可維護 LINE Pay / 綠界的顯示名稱、說明、付款模式、排序與前台啟用狀態；前台付款區塊只顯示啟用中的支付方式，後端建立付款請求時也會再次檢查是否啟用。
- 已建立 `payment_record` 支付骨架與 `POST /api/public/payment/request`、`POST /api/public/payment/confirm`。
- LINE Pay 現在已有 provider adapter 第一版；若未設定正式憑證會走 mock 模式，付款確認後會同步更新 `booking.paid_amount` 並寫入訂單 timeline。
- LINE Pay 正式模式已改為使用官方商家 `v2` reserve / confirm 流程格式，前台付款確認頁也可吃 `orderId / transactionId` 導回參數。
- 已新增 `PAYMENT_SANDBOX_SETUP.md` 與 `backend/payment.env.example`，整理 LINE Pay / 綠界 sandbox 與正式環境欄位、callback / 導回網址與實測清單。
- 綠界現在已有 POST form 導轉與 `ReturnURL` callback 驗證骨架；正式模式會驗證 `CheckMacValue`，且重複通知不會重複入帳。
- 綠界現在已補上瀏覽器端 `OrderResultURL` 導回閉環，付款完成後可由後端轉導回前台成功 / 失敗頁，非成功結果也會從 `pending` 正確轉成 `failed`。
- 前台預約頁已接入 `rate_plan / availability` 即時報價。
- `/api/public/booking/quote` 現在會依日期逐晚回傳價格、剩餘組數與是否關閉。
- 前台送單時會先驗證日期、營位上限與可售名額，再建立訂單並寫入 `availability.reserved_quantity`。
- 訂單狀態改為 `cancelled` 時，會釋放對應日期的保留庫存；從 `cancelled` 改回有效狀態時，會先重做可售檢查。
- 前台現在已有 `booking/success`、付款頁、付款確認頁與付款取消頁，能把送單後流程一路走到 mock 付款結果。
- 前台現在可用訂單編號與聯絡電話查詢預約，公開 API 僅回傳旅客需要的遮罩聯絡資料、付款摘要與公開處理歷程。
- 前台付款取消頁會呼叫取消 API，將待付款紀錄更新為 `cancelled` 並寫入公開歷程；重複取消為冪等操作。
- 前台付款失敗頁會呼叫失敗 API，將待付款紀錄更新為 `failed` 並寫入公開歷程；旅客可返回訂單頁重新建立付款請求。
- 付款成功 / 取消 / 失敗 API 回應會帶回 `bookingId` 與 `bookingNo`，前台不再只依賴 sessionStorage 產生回訂單連結。
- 前台訂單結果頁會用 `bookingId + bookingNo` 重新載入公開摘要，第三方金流導回或換裝置開啟時仍可顯示訂單資料。
- 前台查詢預約頁已接 `POST /api/public/payment/retry`，用訂單編號與聯絡電話核對後重新建立 LINE Pay / 綠界付款請求，舊的 `pending` 付款會標記為 `expired` 並寫入公開歷程。
- 已新增 `notification_record` 通知紀錄骨架，前台送單與付款成功時會自動建立通知紀錄。
- 後台已新增 `/admin/booking/notifications` 通知紀錄頁，可查看通知事件、通道、收件對象、狀態、內容與 payload。
- 通知系統已新增 Email 發送器第一版骨架，可從後台手動發送 `queued` 的 Email 通知；是否真正送出仍取決於 SMTP 設定。
- 後台通知紀錄頁已可指定入住日期產生出發前提醒，預設可用 3 天後入住日期，不會重複建立同訂單的 `departure_reminder`。
- 已新增 LINE 官方帳號 webhook 第一版骨架，可接 `follow` 與文字訊息，依關鍵字回覆營區資訊、營位、公告、FAQ、預約與查詢入口。
- LINE 後續規劃已定調為「獨立 LINE 專案 + 主專案保留單一後台」模式，切分方案整理於 `LINE_PLATFORM_SPLIT_PLAN.md`。
- 後台已新增租借品管理第一版 `/admin/camp/rental-item`，可維護帳棚、防水墊、炊事用品等租借品主檔、圖片、類別、單價、庫存數量與啟用狀態。
- `db/init/001_schema.sql` 已新增 `rental_item` 表與租借品管理權限 seed；前台預約頁已可加選租借品並納入報價與訂單明細，目前尚未接日期占用庫存。
- 已新增後台圖片上傳 API `POST /api/upload/admin-image`，目前租借品圖片會上傳到 MinIO `rental-item-images` bucket，bucket 不存在時會自動建立。
- 已新增公開租借品 API `GET /api/public/camp/rental-items`，前台預約頁會顯示啟用中的租借品圖片、單價、庫存與數量選擇。
- 後台精簡第一波已開始：側欄只顯示有實際後台頁面的營運功能，舊用戶 / 角色 / 權限管理頁已從前端動態路由映射移除；JWT、Redis 登入與權限資料結構仍保留，避免影響登入與授權。
- 後台選單已拆成「官網管理」與「營位與用品管理」：
  - 官網管理：營區基本資料、公告、相簿、FAQ。
  - 營位與用品管理：營區區域、營位、價格、庫存、租借品。
- 個人帳號設定已從左側選單移到右上角使用者下拉，包含我的資料、修改密碼與登出。
- 後台右上角信箱圖示已接最近預約資料，會顯示最新 5 筆訂單、訂單總數提示，並可點入預約明細或前往預約列表。
- 專案正式 Logo 已整理到 `frontend/src/assets/images/brand/`，目前前台 header / footer、後台 sidebar、登入頁與 favicon 都已改用同一組發呆亭品牌圖。
- 後台已新增專案自己的整體密度樣式 `frontend/src/assets/css/admin-density.css`，用於放大後台字級、側欄、header、表單、按鈕與表格，不直接修改第三方模板大檔。
- 後台首頁 `/admin/home` 已由模板 dashboard 改為營運看板，會用現有預約列表 API 顯示今日入住、待確認、未收清、近 7 日入住、今日行程、近期入住、需要處理與最新預約。
- 後台已新增入住名單頁 `/admin/booking/check-ins`，可依日期查看入住、住宿中、退房客人，並匯出 CSV 給開門與清潔作業使用。
- 後台已新增訂房日曆 `/admin/booking/calendar`，可用月份視圖查看每日入住、住宿中與退房數，並點日期查看當日訂單。
- 租借品已補上日期庫存檢查：新訂單租借明細會保存 `booking_item.rental_item_id`，前台報價會回傳所選日期的已租與可租數量，選超過可租量時會阻止送單。
- 前台已新增 `/booking-guide`、`/refund-policy`、`/privacy-policy` 與正式 404 頁；預約頁與頁尾皆有政策入口。
- 後台已新增操作紀錄功能，會記錄管理員登入成功 / 失敗、登出，以及後台 API 的查看、新增、更新、刪除等操作；操作內容會翻譯成「查看預約列表」、「更新營區區域：雨棚區」這類一般使用者看得懂的描述，後台可在 `/admin/operation-log/list` 查詢使用者、操作類型、結果、路徑與時間。
- 主專案已新增 `line_user_binding` 與 LINE 整合 API 第一版，提供綁定請求、綁定確認、依 `lineUserId` 查詢訂單列表與單筆訂單明細。
- 已新增 `fadaiting-line` 獨立專案骨架，包含 Node.js / Express server 與 Vue 3 / Vite LIFF 原型頁；web 端已接 LIFF SDK 初始化與 profile 自動帶入骨架，server 端已補第一版 Flex Message、Rich Menu JSON、Rich Menu PNG 產生與上傳骨架，並新增 webhook / reply / main-api JSONL log。
- 已新增 `LINE_OFFICIAL_ACCOUNT_SETUP.md`，整理 LINE 官方帳號、webhook、LIFF、Rich Menu、正式 HTTPS URL 與環境變數檢查表。

## 進行中功能

- 將前台從示意版持續收斂成正式上線版
- 預約流程仍在以「人工確認」模式運作

## 待完成功能

- 更完整的訂單流程與狀態追蹤
- 前台預約成功頁 / 送單後流程優化
- 正式上線所需 SEO / 監控 / 錯誤頁 / 圖片資產整理

## API 說明

### 實際後端 base

- `http://localhost:9090/fadaiting-camp`

### 前端代理 base

- `/api`

### 公開 API

- `GET /api/public/camp/site`
  - 取得營區詳細資料、設施與 FAQ
- `GET /api/public/camp/zones`
  - 取得營區分區與營位資訊
- `GET /api/public/camp/announcements`
  - 取得公告列表
- `POST /api/public/booking/quote`
  - 依營位、日期、人數、車輛數回傳可售狀態與逐晚價格
- `POST /api/public/booking`
  - 送出預約申請
- `POST /api/public/booking/lookup`
  - 使用訂單編號與聯絡電話查詢公開預約摘要、付款進度與處理歷程
- `POST /api/public/booking/summary`
  - 使用訂單 ID 與訂單編號查詢公開摘要，供付款導回頁恢復結果頁資料
- `POST /api/public/payment/request`
  - 建立付款請求，回傳 payment record 與付款導頁資訊
- `GET /api/public/payment/methods`
  - 回傳前台啟用中的支付方式設定，供預約成功頁動態顯示付款入口
- `POST /api/public/payment/retry`
  - 使用訂單編號與聯絡電話重新建立付款請求，並將舊待付款紀錄標記為逾時
- `POST /api/public/payment/confirm`
  - 確認付款結果，更新付款紀錄與訂單已收金額
- `POST /api/public/payment/cancel`
  - 將待付款紀錄更新為取消，並新增一筆公開付款取消歷程
- `POST /api/public/payment/fail`
  - 將待付款紀錄更新為失敗，並新增一筆公開付款失敗歷程
- `POST /api/public/payment/ecpay/callback`
  - 接收綠界付款通知，驗證檢查碼並冪等更新付款狀態
- `POST /api/public/payment/ecpay/return`
  - 接收綠界付款完成後的瀏覽器導回，並轉導到前台付款結果頁
- `POST /api/public/line/webhook`
  - 接收 LINE 官方帳號 webhook，驗證 `X-Line-Signature` 後依關鍵字回覆營區資訊、營位、公告、FAQ、預約與查單入口
- `POST /api/public/line/bind/request`
  - 使用訂單編號與聯絡電話建立 LINE 綁定請求，回傳短效 `bindToken`
- `POST /api/public/line/bind/confirm`
  - 使用 `bindToken` 與 `lineUserId` 完成綁定
- `POST /api/public/line/booking/list`
  - 依 `lineUserId` 查詢已綁定聯絡人的訂單列表
- `POST /api/public/line/booking/detail`
  - 依 `lineUserId + bookingId` 查詢公開訂單摘要、付款與處理歷程
- `POST /api/notification/list`
  - 查詢通知紀錄，支援依訂單編號、事件、通道、狀態過濾
- `PATCH /api/notification/status`
  - 手動更新通知紀錄狀態
- `POST /api/notification/send-queued`
  - 手動處理待寄 Email 通知，回傳本次處理 / 成功 / 失敗 / 略過筆數
- `POST /api/notification/departure-reminders`
  - 後台依入住日期產生出發前提醒通知，未提供日期時使用 `daysBefore` 推算

### 後台 API（目前與露營區直接相關）

- `GET /api/camp/site`
- `POST /api/payment/list`
  - 查詢付款紀錄，支援訂單編號、金流、付款狀態與付款單號篩選
- `POST /api/payment/summary`
  - 依目前付款篩選條件回傳各付款狀態的筆數與金額摘要
- `POST /api/payment/manual`
  - 後台補登人工付款，會檢查未收金額、建立付款紀錄、同步訂單已收金額與處理歷程
- `GET /api/payment/methods`
  - 後台查詢 LINE Pay / 綠界支付方式設定
- `PUT /api/payment/methods`
  - 後台更新支付方式顯示名稱、說明、付款模式、排序與啟用狀態
- `POST /api/operation-log/list`
  - 後台查詢操作紀錄，支援帳號、操作類型、成功狀態、API 路徑與日期區間篩選
- `GET /api/camp/site/detail`
- `GET /api/camp/zone`
- `GET /api/camp/zone/list`
- `POST /api/camp/zone`
- `PUT /api/camp/zone`
- `DELETE /api/camp/zone/{id}`
- `GET /api/camp/unit/list`
- `POST /api/camp/unit`
- `PUT /api/camp/unit`
- `DELETE /api/camp/unit/{id}`
- `GET /api/camp/rate-plan/list`
- `POST /api/camp/rate-plan`
- `PUT /api/camp/rate-plan`
- `DELETE /api/camp/rate-plan/{id}`
- `GET /api/camp/availability/list`
- `POST /api/camp/availability`
- `PUT /api/camp/availability`
- `DELETE /api/camp/availability/{id}`
- `GET /api/camp/announcement`
- `POST /api/camp/announcement`
- `PUT /api/camp/announcement`
- `DELETE /api/camp/announcement/{id}`
- `GET /api/camp/faq`
- `POST /api/camp/faq`
- `PUT /api/camp/faq`
- `DELETE /api/camp/faq/{id}`
- `GET /api/camp/gallery`
- `POST /api/camp/gallery`
- `PUT /api/camp/gallery`
- `DELETE /api/camp/gallery/{id}`
- `GET /api/booking/detail/{bookingId}`
- `PUT /api/camp/site`
- `POST /api/booking/list`
- `PATCH /api/booking/status`
- `PATCH /api/booking/detail`

### 其他既有後台 API

- `/api/admin/*`
- `/api/role/*`
- `/api/category/*`
- `/api/article/*`
- `/api/upload/*`

## DB 說明

- DB 類型：PostgreSQL 18
- DB 名稱：`fadaiting_camp`
- DB 帳號：`camp`
- schema 檔案：`db/init/001_schema.sql`
- 目前以單一營區模式為主，但 schema 已保留多個 camp site 的擴充能力
- 主要核心表：
  - `camp_site`
  - `camp_zone`
  - `camp_unit`
  - `rate_plan`
  - `availability`
  - `booking`
  - `booking_guest`
  - `booking_item`
  - `payment_method_config`
  - `admin_operation_log`
  - `announcement`
  - `facility`
  - `faq`
  - `site_image`

詳見：`SCHEMA.md`

## Docker 啟動方式

### 只啟動基礎服務

```powershell
docker compose up -d
```

### 啟動完整開發環境

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

### 啟動 production stack

```powershell
docker compose -f docker-compose.prod.yml up -d --build
```

## 開發環境

### Docker 開發

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:9090/fadaiting-camp`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

### 預設管理員帳號

- username: `admin001`
- password: `<seed 預設密碼：見 db/init，首次登入後立即更改>`

## 已知問題

- 本機若不用 Docker 跑 backend，需要 Java 25。
- Tampermonkey 若未開啟 `Allow access to file URLs`，`@require file:///...` 的本機 runtime 不會生效。
- Vite build 目前有警告：
  - `simplebar.min.js` 非 module script
  - `pcoded.js` 有 static / dynamic import 混用警告
  - chunk size 偏大
- 專案仍保留部分舊有文章 / 分類 / 使用者管理模組；使用者 / 角色 / 權限目前先作為登入與授權基礎保留，日常後台選單已開始精簡。
- 前台品牌 Logo 已換成正式圖，但內容圖片仍多為示意圖，尚未全面替換為實際營區照片。
- 尚未看到 Swagger / OpenAPI 文件。
- 自動化測試覆蓋不足。

## 下一步工作

1. 補前台預約成功頁、查詢流程與更完整訂單狀態 UX
2. 整理前台正式文案與真實圖片
3. 處理 build warnings、效能與上線前檢查
