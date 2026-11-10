# README_AI.md

## 2026-10-29：SeaweedFS 替換分支（正式主機尚未切換）

使用者指定以 SeaweedFS 取代 Camp MinIO。產品 S3 SDK、全新獨立 Volume、同源圖片路徑與真服務測試見 [SEAWEEDFS_MIGRATION](SEAWEEDFS_MIGRATION.md)。不改 SQL／migration／帳號，不將舊 MinIO volume 交給 SeaweedFS。中央 v4 是既有主機部署契約；目前正式來源仍為 `1d9b8d100654fd2ab9d0a32afbcc541c6bf5c2cd` 的已驗收快照，不以 Cloud 測試宣稱已搬遷。

儲存服務、設定 fence、發布 recipe 與圖片 URL／資料搬遷需要主機管理者另審；本輪用獨立 PR 交付，不直接合 main 或部署。以下 MinIO／舊部署描述保留為歷史；最新替換範圍與未完成主機驗收以連結文件為準。


## 展示收尾後接手規則（2026-10-06）

本輪以展示收尾為停點，依使用者指示切換 ELPSY Shop；不再自動把封存／清理列為立即下一步。TODO 已依實況重整，舊百分比不採用，integration／HTTP／正式頁面驗證已存在。回到發呆亭時仍從遠端 main／HANDOFF 開始，依 [展示稿](DEMO_WALKTHROUGH.md) 與 [六類延期項目](TODO_AI.md) 定範圍。

## 2026-10-06：通知保留試算接手入口

唯讀 dry-run 已完成，正式保留期限與清理仍未核定／實作。不能照舊試算結果刪資料；新版佇列與 event_key 不可直接刪，會破壞去重。下一階段先確認營運保留需求、封存／復原及競爭保護，再評估小批次人工清理。見 [NOTIFICATION_RETENTION.md](NOTIFICATION_RETENTION.md)及 [交接](docs/HANDOFF.md)。

## 2026-10-06：備份觀測接手入口

備份與還原持久紀錄／ADMIN 觀測已完成，CLI 全域 `--journal`／`--source-key` 放子命令前。預設未設定，不能補造舊紀錄；failed／running 不計成功，損壞或超出讀取上限明示無法判定。先從遠端 main 和 [交接](docs/HANDOFF.md) 續作；下一步通知保留政策與安全 dry-run，詳見 [BACKUP_OBSERVABILITY.md](BACKUP_OBSERVABILITY.md)。

## 2026-10-06：通知觀測接手入口

新版 ADMIN 通知頁及 queue API 可唯讀觀測積壓／租約／結果不明，告警只在後台呈現，不自動啟用 worker 或重送。讀取失敗顯示無法判定，列表可獨立載入；production Bootstrap 的 require 問題已修正。配置、指標重疊與驗收範圍見 [NOTIFICATION_OBSERVABILITY.md](NOTIFICATION_OBSERVABILITY.md)。下一輪依遠端 main 與 [交接](docs/HANDOFF.md)續作，備份狀態已完成；通知保留策略仍待補。

## 2026-10-06：備份／隔離還原接手入口

本輪已完成免費 Python 備份與還原工具及 PostgreSQL 17 真實演練，操作見 [BACKUP_RESTORE.md](BACKUP_RESTORE.md)。還原只能自行建立新容器，不接既有目標；不得把備份或含個資日誌提交。新階段先讀 [docs/HANDOFF.md](docs/HANDOFF.md)，本機 PG18／MinIO／正式切換仍待補，不能將 Cloud 演練標成正式整體復原。

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

本文件提供給 AI 助手快速掌握「怎麼跑、怎麼查、怎麼接」。

## 1. 專案概況

- Monorepo
- 前端：`frontend`
- 後端：`backend`
- LINE 專案骨架：`fadaiting-line`
- DB schema：`db/init/001_schema.sql`
- Docker：
  - `docker-compose.yml`
  - `docker-compose.dev.yml`
  - `docker-compose.prod.yml`
- 可攜帶 Codex skill：`codex-skills/fadaiting-camp-account-test-sync`

## 2. 掃描結果

### frontend/package.json

- 名稱：`fadaiting-camp`
- scripts：
  - `npm run dev`
  - `npm run build`
  - `npm run preview`
- 主要依賴：
  - `vue`
  - `vue-router`
  - `pinia`
  - `element-plus`
  - `axios`
  - `bootstrap`
  - `bootstrap-icons`

### backend/pom.xml

- Spring Boot：`4.0.6`
- Java：`25`
- 打包：`jar`
- 主要依賴：
  - `spring-boot-starter-web`
  - `spring-boot-starter-security`
  - `spring-boot-starter-validation`
  - `mybatis-spring-boot-starter`
  - `postgresql`
  - `spring-boot-starter-data-redis`
  - `minio`
  - `java-jwt`
  - `pagehelper-spring-boot-starter`

### docker-compose

#### `docker-compose.yml`

提供基礎服務：

- PostgreSQL
- Redis
- MinIO

#### `docker-compose.dev.yml`

額外提供：

- backend（`maven:3.9.11-eclipse-temurin-25`）
- frontend（`node:24-alpine`）
- `minio-init`

#### `docker-compose.prod.yml`

額外提供：

- backend Docker build
- frontend Docker build（Nginx）

## 3. 環境變數

### 前端

來自 `frontend/vite.config.ts`

- `VITE_API_PROXY_TARGET`
  - 預設：`http://localhost:9090/fadaiting-camp`
  - Docker dev：`http://backend:9090/fadaiting-camp`

### 後端

來自 `backend/src/main/resources/application.yml`

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SPRING_DATA_REDIS_HOST`
- `SPRING_DATA_REDIS_PORT`
- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `LINE_PAY_ENABLED`
- `LINE_PAY_CHANNEL_ID`
- `LINE_PAY_CHANNEL_SECRET`
- `LINE_PAY_API_BASE_URL`
- `LINE_PAY_CONFIRM_URL_BASE`
- `LINE_PAY_CANCEL_URL`
- `LINE_PAY_DEVICE_PROFILE_ID`
- `LINE_BOT_ENABLED`
- `LINE_BOT_CHANNEL_SECRET`
- `LINE_BOT_CHANNEL_ACCESS_TOKEN`
- `LINE_BOT_API_BASE_URL`
- `LINE_BOT_FRONTEND_BASE_URL`
- `ECPAY_ENABLED`
- `ECPAY_MERCHANT_ID`
- `ECPAY_HASH_KEY`
- `ECPAY_HASH_IV`
- `ECPAY_CHECKOUT_URL`
- `ECPAY_RETURN_URL`
- `ECPAY_ORDER_RESULT_URL`
- `ECPAY_RESULT_PAGE_BASE_URL`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `NOTIFICATION_MAIL_FROM_NAME`
- `NOTIFICATION_MAIL_FROM_ADDRESS`

金流環境設定與驗證流程請直接搭配：

- [PAYMENT_SANDBOX_SETUP.md](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/PAYMENT_SANDBOX_SETUP.md)
- [backend/payment.env.example](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/backend/payment.env.example)
- [LINE_PLATFORM_SPLIT_PLAN.md](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/LINE_PLATFORM_SPLIT_PLAN.md)
- [LINE_OFFICIAL_ACCOUNT_SETUP.md](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/LINE_OFFICIAL_ACCOUNT_SETUP.md)

若要接 LINE 官方帳號 webhook，另需補：

- `LINE_BOT_ENABLED=true`
- `LINE_BOT_CHANNEL_SECRET`
- `LINE_BOT_CHANNEL_ACCESS_TOKEN`
- `LINE_BOT_FRONTEND_BASE_URL`

### Docker compose 預設值

- PostgreSQL
  - DB：`fadaiting_camp`
  - User：`camp`
  - Password：`camp`
- MinIO
  - User：`<MinIO 帳密：見本機 .env>`
  - Password：`<MinIO 帳密：見本機 .env>`

## 4. 啟動方式

### 只啟動基礎服務

```powershell
cd D:\Project\fadaiting-camp
docker compose up -d
```

### 完整 Docker 開發模式

```powershell
cd D:\Project\fadaiting-camp
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

### 後端本機啟動

前提：本機已安裝 Java 25

```powershell
cd D:\Project\fadaiting-camp\backend
mvn spring-boot:run
```

或

```powershell
cd D:\Project\fadaiting-camp\backend
mvn -DskipTests package
java -jar target\fadaiting-camp-api-1.0-SNAPSHOT.jar
```

### 前端本機啟動

```powershell
cd D:\Project\fadaiting-camp\frontend
npm install
npm run dev
```

區網其他電腦若要連前端，使用本機 IPv4：

```text
http://localhost:5173/
http://localhost:5173/admin/login
```

### Production Docker

```powershell
cd D:\Project\fadaiting-camp
docker compose -f docker-compose.prod.yml up -d --build
```

## 5. 常用指令

### 前端

```powershell
cd frontend
npm install
npm run dev
npm run build
```

### 後端

```powershell
cd backend
mvn spring-boot:run
mvn -DskipTests package
```

### Docker

```powershell
docker compose up -d
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
docker compose -f docker-compose.yml -f docker-compose.dev.yml down
docker compose -f docker-compose.prod.yml up -d --build
```

### Git

```powershell
git status
git add .
git commit -m "feat: xxx"
git push origin main
```

## 6. DB 連線資訊

### Docker / 本機預設

- Host：`localhost`
- Port：`5432`
- Database：`fadaiting_camp`
- Username：`camp`
- Password：`camp`

## 7. Port 整理

- Frontend dev：`5173`
- Backend：`9090`
- Backend context path：`/fadaiting-camp`
- Frontend production：`8080`
- PostgreSQL：`5432`
- Redis：`6379`
- MinIO API：`9000`
- MinIO Console：`9001`

## 8. API 文件位置

目前沒有發現 Swagger / OpenAPI 自動文件設定。

請直接參考：

- 後端 controller：
  - `backend/src/main/java/com/fadaiting/camp/controller`
- 主要露營區 API：
  - `CampSiteController.java`
- `PublicCampController.java`
- `PublicBookingController.java`
- `PublicLineBotController.java`
- `BookingAdminController.java`

## 9. LINE 官方帳號骨架

- 後端已新增 `POST /api/public/line/webhook`
- 主專案已新增：
  - `POST /api/public/line/bind/request`
  - `POST /api/public/line/bind/confirm`
  - `POST /api/public/line/booking/list`
  - `POST /api/public/line/booking/detail`
- 已支援：
  - `follow` 事件歡迎訊息
  - 文字關鍵字回覆
- 目前可處理關鍵字：
  - `營區資訊`
  - `營位`
  - `公告`
  - `FAQ`
  - `預約`
  - `查訂單`
- 回覆內容目前以純文字為主，並串接現有前台頁面：
  - `/booking`
  - `/booking/lookup`
  - `/news`
  - `/faq`
  - `/zones`
- 尚未完成：
  - LINE user 正式驗證策略
  - Flex Message / Rich Menu
  - LINE 內直接查我的訂單
  - LIFF / LINE MINI App 下訂流程

### LINE 綁定第一版

- 主專案 DB 已新增 `line_user_binding`
- 第一版綁定流程：
  1. 用 `bookingNo + contactPhone` 呼叫 `bind/request`
  2. 取得短效 `bindToken`
  3. 用 `bindToken + lineUserId` 呼叫 `bind/confirm`
  4. 後續即可用 `lineUserId` 查訂單列表與單筆訂單明細
- 這一版先讓未來的 `fadaiting-line` 專案有 API 可接
- 目前仍不等於正式身分驗證方案，後續建議再補手機驗證碼或 LIFF 二次驗證

### `fadaiting-line` 專案骨架

- 位置：
  - `fadaiting-line/server`
  - `fadaiting-line/web`
- `server`
  - Node.js / Express
  - 提供 LINE webhook 骨架
  - proxy 到主專案 `/api/public/line/*`
  - 已有第一版 Flex Message / Rich Menu JSON 產生器
  - 已有 `GET /api/line/menu-preview`
  - 已有 `GET /api/line/rich-menu/image`
  - 已有 `POST /api/line/rich-menu/create`
  - 已有 `POST /api/line/rich-menu/:richMenuId/image`
  - 已有 `POST /api/line/rich-menu/default`
  - 已有 `POST /api/line/rich-menu/publish`
  - 已有 `GET /api/line/logs`
- `web`
  - Vue 3 / Vite
  - 作為 LIFF 原型頁
  - 目前可直接測主專案綁定 / 查單 API
  - 已接 LIFF SDK 初始化骨架
  - `VITE_LIFF_ID` 未填時保留本機測試模式
  - `VITE_LIFF_ID` 填入後可自動帶入 LINE profile
- 預設開發 port：
  - server：`9191`
  - web：`5174`
- 注意：
  - Rich Menu 正式建立前，`WEB_BASE_URL` 與 `MAIN_FRONTEND_BASE_URL` 應改成 HTTPS 對外網址
  - Rich Menu 圖片目前由 `sharp` 依六格選單自動產生
  - 圖片上傳會呼叫 `LINE_DATA_API_BASE_URL`，預設 `https://api-data.line.me`
  - LINE event / message log 目前寫入 `fadaiting-line/server/logs/line-events.jsonl`
  - log 會遮罩 `phone`、`token`、`secret`、`authorization` 相關欄位
  - 正式 LINE Developers 設定請照 `LINE_OFFICIAL_ACCOUNT_SETUP.md`

目前和露營區預約直接相關的新公開 API：

- `POST /api/public/booking/quote`
  - 回傳逐晚價格、剩餘組數、是否關閉、營位小計、租借品小計與總金額
  - 可帶 `rentalItems` 陣列，格式為 `{ rentalItemId, quantity }`
  - 租借品報價會依入住 / 離營日期計算該日期區間的已租數與可租數，選超過可租量時回傳 `available = false`
- `POST /api/public/booking`
  - 建立預約單，並同步寫入每日庫存保留數
  - 可帶 `rentalItems` 陣列；後端會重新查詢啟用租借品與單價，寫入 `booking_item` 並保存 `rental_item_id`
- `GET /api/public/camp/rental-items`
  - 回傳前台可選的啟用租借品，包含圖片、類別、單價與總庫存
- `POST /api/public/booking/lookup`
  - 使用訂單編號與聯絡電話查詢旅客可見的預約摘要、付款進度與公開歷程
  - 不回傳內部備註、完整交易編號或後台操作者
- `POST /api/public/booking/summary`
  - 使用 `bookingId + bookingNo` 載入旅客可見摘要
  - 供 `/booking/success` 在沒有 sessionStorage 時自動恢復資料
- `POST /api/public/payment/request`
  - 建立付款請求，先落 payment record；LINE Pay 未設正式憑證時會走 mock 模式
  - LINE Pay 正式模式目前已改為接近官方商家 `v2 payments/request` 的 reserve 流程
- `GET /api/public/payment/methods`
  - 回傳前台啟用中的支付方式設定
  - `/booking/success` 會用這份清單動態顯示 LINE Pay / 綠界等付款入口；停用後前台不顯示，後端也會拒絕建立該 provider 的付款請求
- `POST /api/public/payment/retry`
  - 使用 `bookingNo + contactPhone` 重新建立付款請求
  - 會先把同訂單尚未完成的 `pending` 付款紀錄標記為 `expired`，再建立新的 payment record
  - 查詢預約頁會使用此 API 提供 LINE Pay / 綠界重新付款入口
- `POST /api/public/payment/confirm`
  - 確認付款結果；mock 模式會把 `payment_record` 改為 `paid`，同步更新 `booking.paid_amount` 並寫入訂單 timeline
  - 回應包含 `bookingId` / `bookingNo`，供前台結果頁返回訂單使用
  - LINE Pay 導回時，除了 `paymentRecordId`，也可用 `orderId / transactionId` 找回付款紀錄
- `POST /api/public/payment/cancel`
  - 將 `pending` 付款紀錄改為 `cancelled` 並寫入公開 timeline
  - 重複取消不會重複新增 timeline
  - 回應包含 `bookingId` / `bookingNo`
- `POST /api/public/payment/fail`
  - 將 `pending` 付款紀錄改為 `failed` 並寫入公開 timeline
  - 目前先供 mock 失敗流程驗證；正式 provider 失敗 callback 待商家環境實測
  - 回應包含 `bookingId` / `bookingNo`
- `POST /api/public/payment/ecpay/callback`
  - 接收綠界 server-side `ReturnURL` 通知，驗證 `CheckMacValue` 後冪等更新付款紀錄
  - 正式綠界模式只接受 callback 入帳，不允許前台直接 confirm
- `POST /api/public/payment/ecpay/return`
  - 接收綠界瀏覽器端 `OrderResultURL` 回傳，並導回前台 `/payment/confirm` 或 `/payment/fail`
  - 非成功結果不再一直停留在 `pending`
- `POST /api/notification/list`
  - 後台通知紀錄列表，支援依訂單編號、事件、通道、狀態查詢
- `PATCH /api/notification/status`
  - 後台可將通知紀錄標記為已發送 / 失敗 / 略過
- `POST /api/notification/send-queued`
  - 手動發送待寄 Email 通知；目前會處理 `channel = email` 且 `status = queued` 的紀錄
- `POST /api/notification/departure-reminders`
  - 後台依入住日期產生出發前提醒；未提供日期時使用 `daysBefore` 推算，並避免同訂單重複建立
- `POST /api/payment/list`
  - 後台付款紀錄查詢，支援訂單編號、付款單號、金流、狀態與建立日期區間
- `POST /api/payment/manual`
  - 後台補登人工付款，可記錄現金、轉帳或其他收款方式
  - 會建立 `payment_record`、同步 `booking.paid_amount`、寫入訂單處理歷程與付款成功通知紀錄
- `GET /api/payment/methods`
  - 後台支付方式設定列表
- `PUT /api/payment/methods`
  - 後台更新支付方式顯示名稱、說明、付款模式、排序與前台啟用狀態
- `POST /api/operation-log/list`
  - 後台操作紀錄查詢，支援後台帳號、操作類型、成功 / 失敗、API 路徑與日期區間篩選
- `GET /api/camp/rental-item/list`
  - 後台租借品列表，回傳帳棚、防水墊、炊事用品等租借品主檔
- `POST /api/camp/rental-item`
  - 新增租借品，欄位包含名稱、圖片、類別、描述、單價、總庫存、排序與啟用狀態
- `PUT /api/camp/rental-item`
  - 更新租借品主檔
- `DELETE /api/camp/rental-item/{id}`
  - 刪除租借品；目前尚未接訂單租借明細，後續接上後應改為已有歷史訂單只能停用
- `POST /api/upload/admin-image`
  - 後台通用圖片上傳 API，表單欄位為 `file`，可選 `bucket`
  - 租借品圖片目前使用 `rental-item-images` bucket，bucket 不存在時後端會自動建立

## 9. 路由與頁面位置

### 前台頁面

- `/` -> `frontend/src/views/storefront/StorefrontHome.vue`
- `/zones` -> `StorefrontZones.vue`
- `/booking` -> `StorefrontBooking.vue`
  - 會讀取啟用租借品並顯示圖片卡片與數量選擇
  - 報價會顯示營位小計、租借品小計、租借明細與總額
  - 租借品卡片在選定日期並報價後會顯示該日期區間的可租數與已租數
- `/booking/success` -> `StorefrontBookingSuccess.vue`
  - 會讀取 `GET /api/public/payment/methods`，只顯示後台啟用中的支付方式
- `/booking/lookup` -> `StorefrontBookingLookup.vue`
- `/admin/booking/payments` -> `frontend/src/views/booking/PaymentList.vue`
  - 後台付款紀錄查詢頁，使用 `POST /api/payment/list`
  - 目前共用 `查詢預約` 權限，預約管理頁右上角有入口
  - 支援建立時間日期區間篩選與摘要統計卡
  - 可依目前篩選條件匯出 CSV，最多一次匯出 10000 筆
- `/admin/booking/payment-methods` -> `frontend/src/views/booking/PaymentMethodList.vue`
  - 後台支付方式管理頁，使用 `GET /api/payment/methods` 與 `PUT /api/payment/methods`
  - 可控制前台是否顯示 LINE Pay / 綠界，以及各 provider 使用的付款模式
- `/map` -> `StorefrontMap.vue`
- `/news` -> `StorefrontNews.vue`
- `/faq` -> `StorefrontFaq.vue`
- `/booking-guide` -> `StorefrontPolicy.vue`
- `/refund-policy` -> `StorefrontPolicy.vue`
- `/privacy-policy` -> `StorefrontPolicy.vue`
- `/payment/checkout/:provider/:method/:orderNo` -> `StorefrontPaymentCheckout.vue`
- `/payment/linepay/confirm` -> `StorefrontPaymentConfirm.vue`
- `/payment/confirm` -> `StorefrontPaymentConfirm.vue`
- `/payment/cancel` -> `StorefrontPaymentCancel.vue`
- `/payment/fail` -> `StorefrontPaymentFail.vue`
- `/admin/booking/notifications` -> `frontend/src/views/booking/NotificationList.vue`
  - 後台通知紀錄頁，目前和預約管理共用 `查詢預約` 權限
  - 可查看預約送出與付款成功通知佇列，並手動更新狀態
  - 可指定入住日期產生出發前提醒，空白時使用預設天數
- `/admin/operation-log/list` -> `frontend/src/views/admin/AdminOperationLogList.vue`
  - 後台操作紀錄頁，可查看登入成功 / 失敗、登出與後台 API 操作紀錄

### 後台頁面

- `/admin/login`
- `/admin/home`
  - 後台營運首頁，使用 `POST /api/booking/list` 載入近期預約，顯示今日入住、待確認、未收清、近 7 日入住、今日行程、近期入住、需要處理與最新預約。
- `/admin/camp/site`
- `/admin/camp/zone`
- `/admin/camp/unit`
- `/admin/camp/rate-plan`
- `/admin/camp/availability`
- `/admin/camp/rental-item`
  - 後台租借品主檔管理，可維護品項、圖片、類別、單價、總庫存與啟用狀態
  - 已接前台預約選配、訂單租借明細與日期占用庫存檢查
- `/admin/camp/announcement`
- `/admin/camp/faq`
- `/admin/camp/gallery`
- `/admin/booking/list`
- `/admin/booking/check-ins`
  - 後台入住名單頁，使用 `POST /api/booking/list` 載入預約資料，依日期整理入住、住宿中、退房與車輛數，並可匯出 CSV。
- `/admin/booking/calendar`
  - 後台訂房日曆頁，使用 `POST /api/booking/list` 載入預約資料，以月份視圖顯示每日入住、住宿中與退房數，點日期可看當日訂單。
- `/admin/booking/detail/:id`
  - 可調整已收金額、調整金額、內部備註、人工確認備註與取消原因
  - 依已收 / 應收自動顯示未收款、已收訂金、已收清、超收款
  - 顯示付款紀錄與處理歷程
  - 可在付款紀錄區補登人工付款，處理現場現金、匯款或尾款補收
- `/admin/operation-log/list`
  - 後台系統管理用的操作紀錄列表，會顯示登入 / 登出與後台操作的使用者、時間、路徑、結果與訊息
  - 操作內容會由後端依 API 與 request body 轉成具體中文描述，例如「查看營區區域列表」、「更新營區區域：雨棚區」

路由檔案：

- `frontend/src/router/index.ts`

後台精簡狀態：

- 側欄會只顯示具有實際 `adminRouter` 的營運頁面，避免新增 / 更新 / 刪除等按鈕權限或舊父選單出現在左側。
- 舊用戶 / 角色 / 權限管理頁已從前端動態路由映射移除；登入、個人資料、修改密碼與底層授權資料表仍保留。
- 後台主選單分成：
  - `官網管理`：營區基本資料、公告管理、相簿管理、FAQ 管理。
  - `營位與用品管理`：營區區域管理、營位管理、價格管理、庫存管理、租借品管理。
  - `系統管理`：操作紀錄。
- 個人帳號操作不再放左側選單，改由右上角使用者下拉進入 `我的資料`、`修改密碼` 與 `登出`。

品牌資產：

- 正式 Logo 來源整理在 `frontend/src/assets/images/brand/`
- `fadaiting-logo-full.png`：完整 Logo 備用
- `fadaiting-logo-mark.png`：前台 header / footer、後台 sidebar、登入頁使用
- `fadaiting-favicon.png`：瀏覽器 favicon 使用

後台視覺密度：

- 後台整體放大與可讀性調整集中在 `frontend/src/assets/css/admin-density.css`
- BaseLayout 根節點使用 `.admin-layout` 作為作用範圍，避免影響前台官網與登入頁
- 若之後覺得後台太大或太小，優先調整 `--admin-sidebar-width`、`--admin-header-height` 與 `.admin-layout` 的基準字級

## 10. AI 接手建議順序

1. 讀 `AGENTS.md`
2. 讀 `PROJECT_CONTEXT.md`
3. 讀本文件 `README_AI.md`
4. 讀 `TODO_AI.md`
5. 開始看本次需求相關的 controller / api / view

## 11. 本機編輯程式建議

### 推薦工具

- 程式編輯器：`Visual Studio Code`
- 資料庫 GUI：`DBeaver` 或 `pgAdmin`

### 用 VS Code 開專案

```powershell
cd D:\Project\fadaiting-camp
code .
```

建議直接開 repo 根目錄，這樣前後端與文件可以一起看。

### 常用程式位置

- 前端頁面：`frontend/src/views`
- 前端元件：`frontend/src/components`
- 前端 API：`frontend/src/api`
- 前端路由：`frontend/src/router/index.ts`
- 後端 Controller：`backend/src/main/java/com/fadaiting/camp/controller`
- 後端 Service：`backend/src/main/java/com/fadaiting/camp/service`
- 後端 Mapper：
  - Java 介面：`backend/src/main/java/com/fadaiting/camp/mapper`
  - XML SQL：`backend/src/main/resources/com/fadaiting/camp/mapper`
- DB schema：`db/init/001_schema.sql`

## 12. 本機查看資料庫

### PostgreSQL 連線資訊

- Host：`localhost`
- Port：`5432`
- Database：`fadaiting_camp`
- Username：`camp`
- Password：`camp`

### 先啟動資料庫

```powershell
cd D:\Project\fadaiting-camp
docker compose up -d postgres
```

如果要連同 Redis / MinIO 一起啟動：

```powershell
cd D:\Project\fadaiting-camp
docker compose up -d
```

### 用 GUI 工具連線

建立 PostgreSQL 連線後填入上面的 Host / Port / Database / Username / Password 即可。

### 用命令列查看

若本機有安裝 `psql`：

```powershell
psql -h localhost -p 5432 -U camp -d fadaiting_camp
```

可先查目前露營區核心表：

```sql
\dt
select * from camp_site;
select * from camp_zone order by sort_order, id;
select * from availability order by available_date desc, id desc;
select * from booking order by id desc;
```

## 13. Codex Skill 交接

本專案有一個可攜帶 skill：

- repo 內位置：`codex-skills/fadaiting-camp-account-test-sync`
- 安裝目標：`$CODEX_HOME\skills\fadaiting-camp-account-test-sync`

用途：

- Codex 新增或修改本機測試帳號時，同步明文測試帳號 Excel / JSON。
- 同步 Tampermonkey userscript，讓後台登入時只輸入帳號即可自動填密碼。
- userscript 採 header + 本機 runtime `@require` 模式；若要讓 `file:///` runtime 生效，Chrome 的 Tampermonkey 需開啟 `Allow access to file URLs`。
- 日常開發新增帳號後，若 Tampermonkey 已更新到這個 header 版本，通常只要重新整理登入頁，不必每次重新安裝 userscript。
- 產出的本機明文檔案已被 `.gitignore` 排除，不要提交。

新電腦安裝：

```powershell
$env:CODEX_HOME = "$env:USERPROFILE\.codex"
New-Item -ItemType Directory -Force -Path "$env:CODEX_HOME\skills" | Out-Null
Copy-Item -Recurse -Force `
  "D:\Project\fadaiting-camp\codex-skills\fadaiting-camp-account-test-sync" `
  "$env:CODEX_HOME\skills\fadaiting-camp-account-test-sync"
```

詳見：`NEW_MACHINE_SETUP.md`
