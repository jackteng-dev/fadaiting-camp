# 發呆亭露營區交接文件

> 自 2026-10-06 起，最新交接移至 [docs/HANDOFF.md](docs/HANDOFF.md)。本文件保留歷史；下一輪以遠端 main 的最新交接為準。

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

## Project

Single-campsite official site and booking platform for `發呆亭露營區`.

Repository:

- `https://github.com/jackteng-dev/fadaiting-camp`

## Current Structure

```text
backend/    Spring Boot API
frontend/   Vue frontend (public site + admin)
db/init/    PostgreSQL initialization SQL
```

## Current URLs

Frontend:

- Public site: `http://localhost:5173/`
- Admin login: `http://localhost:5173/admin/login`

Backend:

- Base: `http://localhost:9090/fadaiting-camp`

## Default Admin Account

- Username: `admin001`
- Password: `<seed 預設密碼：見 db/init，首次登入後立即更改>`

## Core Docs

- [README.md](D:/Project/fadaiting-camp/README.md)
- [CAMP_PLATFORM_PLAN.md](D:/Project/fadaiting-camp/CAMP_PLATFORM_PLAN.md)
- [DEPLOY.md](D:/Project/fadaiting-camp/DEPLOY.md)

## What Is Already Done

- Monorepo created and pushed to GitHub
- Java 25 + Spring Boot 4 backend is running
- PostgreSQL / Redis / MinIO Docker stack prepared
- Campground schema added to `db/init/001_schema.sql`
- Public campground page `/` is connected to real API data
- Public booking API is implemented
- Admin booking list API and status update API are implemented
- Admin camp site profile, camp zone CRUD, and camp unit CRUD are implemented
- Admin rate plan CRUD and availability CRUD are implemented
- Project naming has been moved from `shop` / `ELPSY` display names toward `fadaiting-camp`
- Current DB is `fadaiting_camp` with `camp` / `camp`
- A portable Codex skill exists at `codex-skills/fadaiting-camp-account-test-sync`
- Tampermonkey login autofill has been switched to `header + local runtime @require` mode

## 2026-05-27 Snapshot

- 後台「價格管理」與「庫存管理」已完成前後端 CRUD。
- 後台左選單是 DB permission 驅動的動態選單，前端用 `dynamicRouteMap` 對應 Vue 頁面。
- 專案命名已由舊電商方向收斂：
  - 前端 package：`fadaiting-camp`
  - 後端 artifact：`fadaiting-camp-api`
  - 後端 Java package：`com.fadaiting.camp`
  - context path：`/fadaiting-camp`
  - Docker container：`fadaiting-camp-*`
  - DB：`fadaiting_camp`
- 新 DB 曾因 PowerShell 管線複製中文資料出現 `?`，已改用 container 內 `pg_dump -Fc` / `pg_restore` 修復；之後搬 DB 不要用文字管線。
- 前端與後端已驗證：
  - `npm run build`
  - `mvn -DskipTests package`
  - `/api/admin/login`
  - `/api/public/camp/site`
  - `/api/camp/unit/list`
  - `/api/camp/rate-plan/list`
  - `/api/camp/availability/list`
- 本機測試帳號同步 skill 目前除 Excel / JSON 外，也會產生：
  - `frontend/public/local-userscripts/admin-login-autofill.user.js`
  - `frontend/public/local-userscripts/admin-login-autofill.runtime.js`
- Tampermonkey 現在採 `@require file:///...` 載入 runtime，所以新增帳號後通常只需重新整理登入頁即可讀到最新映射；前提是 Chrome 對 Tampermonkey 已開啟 `Allow access to file URLs`。

## Important API Endpoints

Public:

- `GET /fadaiting-camp/api/public/camp/site`
- `GET /fadaiting-camp/api/public/camp/zones`
- `GET /fadaiting-camp/api/public/camp/announcements`
- `POST /fadaiting-camp/api/public/booking`

Admin:

- `GET /fadaiting-camp/api/camp/site`
- `GET /fadaiting-camp/api/camp/zone/list`
- `POST /fadaiting-camp/api/camp/zone`
- `PUT /fadaiting-camp/api/camp/zone`
- `DELETE /fadaiting-camp/api/camp/zone/{id}`
- `GET /fadaiting-camp/api/camp/unit/list`
- `POST /fadaiting-camp/api/camp/unit`
- `PUT /fadaiting-camp/api/camp/unit`
- `DELETE /fadaiting-camp/api/camp/unit/{id}`
- `GET /fadaiting-camp/api/camp/rate-plan/list`
- `POST /fadaiting-camp/api/camp/rate-plan`
- `PUT /fadaiting-camp/api/camp/rate-plan`
- `DELETE /fadaiting-camp/api/camp/rate-plan/{id}`
- `GET /fadaiting-camp/api/camp/availability/list`
- `POST /fadaiting-camp/api/camp/availability`
- `PUT /fadaiting-camp/api/camp/availability`
- `DELETE /fadaiting-camp/api/camp/availability/{id}`
- `POST /fadaiting-camp/api/booking/list`
- `PATCH /fadaiting-camp/api/booking/status`

## Run Locally

### Infrastructure

```powershell
cd D:\Project\fadaiting-camp
docker compose up -d
```

### Full Docker Development Stack

```powershell
cd D:\Project\fadaiting-camp
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

This does not require Java 25 on the host because the backend runs in a Maven + Java 25 container.

### Backend

```powershell
cd D:\Project\fadaiting-camp\backend
mvn -DskipTests package
java -jar target\fadaiting-camp-api-1.0-SNAPSHOT.jar
```

### Frontend

```powershell
cd D:\Project\fadaiting-camp\frontend
npm install
npm run dev
```

## Notes For Another Computer

- Codex chat history and live local process state do not automatically fully transfer between machines
- GitHub code does transfer after `git clone`
- Docker containers, running dev servers, and local plugin authorizations must be set up again on the new machine
- Follow `NEW_MACHINE_SETUP.md` to install the project skill into `$CODEX_HOME\skills`
- Do not copy PostgreSQL data through PowerShell text pipes; use container-side `pg_dump -Fc` / `pg_restore` to preserve Chinese text

## Best Restart Prompt For Codex

Use this in a new Codex thread on another machine:

```text
This is the 發呆亭露營區 monorepo. Please read AGENTS.md, README.md, HANDOFF.md, PROJECT_CONTEXT.md, README_AI.md, TODO_AI.md, and NEW_MACHINE_SETUP.md. Also check whether codex-skills/fadaiting-camp-account-test-sync has been installed into $CODEX_HOME/skills, then continue from the latest campground platform work.
```

## Suggested Next Work

- Build announcement / FAQ / gallery admin CRUD
- Add booking detail view and richer order workflow
- Connect `rate_plan / availability` into booking-side availability checks
