> 開發紀錄：原 README 的完整里程碑紀錄（保留歷史，不再更新）。最新說明見 [README](../README.md)。

# 發呆亭露營區官網與訂位平台

## 展示階段收尾（2026-10-06）

主要預約、固定角色營運、住宿、付款／退款內部流程、通知佇列與備份／保留觀測已完成展示範圍。本輪收尾後暫停新增功能，下一個作品為 ELPSY Shop。展示順序見 [DEMO_WALKTHROUGH.md](../DEMO_WALKTHROUGH.md)，六類延期工作見 [TODO_AI.md](../TODO_AI.md)，本機／真實服務條件見 [待補清單](../CLOUD_LOCAL_FOLLOWUP.md)。這是可展示階段，不宣稱正式營運驗收完成。

## 2026-10-06：通知歷史保留唯讀試算

ADMIN 通知頁可查看全站保留／候選數量及互斥保護原因，預設 180 天草案，無刪除 API 或排程。去重鍵、新版佇列、寄送嘗試／結果不明、Email／金流、未結束訂單與近期活動皆保留。操作與界限見 [NOTIFICATION_RETENTION.md](../NOTIFICATION_RETENTION.md)，最新驗證見 [交接](HANDOFF.md)。

## 2026-10-06：備份持久紀錄與後台觀測

CLI 可選保存私有執行紀錄，ADMIN 後台分開顯示最近成功備份／還原與相同雜湊驗證，失敗保留成功歷史並告警；未設定或紀錄無法讀取不當健康。唯讀 API，不掛 Docker socket、不提供 HTTP 備份操作。設定與限制見 [BACKUP_OBSERVABILITY.md](../BACKUP_OBSERVABILITY.md)，驗證見 [交接](HANDOFF.md)。

## 2026-10-06：通知營運觀測與後台告警（Cloud）

ADMIN 通知頁新增等待時間、過久任務、結果不明、逾期租約及無法領取指標，依嚴重度給出人工核對建議。讀取不重送／回收任務；API 失敗不顯示舊數字。沿用 ADMIN 權限與免費工具，worker／真實寄信預設關閉。另修正正式 Bootstrap 載入造成登入頁空白的問題；操作、API 與限制見 [NOTIFICATION_OBSERVABILITY.md](../NOTIFICATION_OBSERVABILITY.md)，本輪結果見 [交接](HANDOFF.md)。

## 2026-10-06：PostgreSQL 備份與隔離還原（Cloud）

新增免費 Python／Docker 工具：二進位 `pg_dump -Fc`、同快照資料指紋與 SHA-256、全新無網路容器還原、結構／中文／預約／付款／退款／通知核對及失敗清理。Cloud PostgreSQL 17 的 17 項工具測試通過，來源保持不變；實際本機 PostgreSQL 18、MinIO 與正式復原切換仍待補。操作與限制見 [BACKUP_RESTORE.md](../BACKUP_RESTORE.md)，本輪結果見 [交接](HANDOFF.md)。

本專案遵守整個展示平台的 [共同工作流程](https://github.com/jackteng-dev/developer-portfolio/blob/main/docs/PLATFORM_WORKFLOW.md)：每輪以遠端 `main` 為準，先讀 [docs/HANDOFF.md](HANDOFF.md)，完成測試與 production build 後更新交接、commit 並 push `main`。各作品分別維護交接與驗證；新 App／搶票專案亦適用。

## 2026-10-06：固定 main 工作與交接流程

最新工作基準改為遠端 `main`，已整合至 `c055d49` 的員工權限、住宿、付款、退款、身分安全與通知佇列成果。每輪先同步遠端 main、閱讀 [docs/HANDOFF.md](HANDOFF.md)，再核對工作目錄；完成測試與 production build 後更新交接、commit 並 push main。固定規範見 [AGENTS.md](../AGENTS.md)。

## 2026-10-06：通知佇列與失敗重試（Cloud）

預約／付款通知在同一資料庫交易入列，事件唯一鍵去重；PostgreSQL SKIP LOCKED 與短租約支援多 worker，退避重試、次數上限與每次嘗試紀錄保留。SMTP 結果不明時停下待人工核對，過期 worker 不能覆蓋結果。

ADMIN 通知頁可免費模擬、查看重試歷程、保留原因重新排入失敗任務、略過或確認站內提醒；模擬完成不計為已寄信，處理中與終態不能任意重送，版本衝突回 409。STAFF／匿名無權限。真實寄信與 worker 預設關閉；本輪未寄送 Email／LINE。

既有 DB 先備份並依序補缺少的 `002` 到 `008_notification_queue.sql` 再部署，不刪 volume。舊通知保留、不自動入列。設定、狀態與限制見 [NOTIFICATION_QUEUE.md](../NOTIFICATION_QUEUE.md)，待補事項見 [CLOUD_LOCAL_FOLLOWUP.md](../CLOUD_LOCAL_FOLLOWUP.md)，驗證見 [本輪紀錄](verification/notification-queue-2026-10-06.json)。

## 2026-10-06：Cookie／CSRF 與 LINE 身分核驗（Cloud）

後台登入、刷新、登出與非公開寫入啟用 CSRF header 驗證；Vue 自動取得 token，失效最多重試一次。HTTPS 預設 Secure／HttpOnly／SameSite=Lax 與 `__Host-camp_*` cookie，HTTP 開發須明確設定 `AUTH_COOKIE_SECURE=false`。Access JWT 15 分鐘、refresh 7 天，JTI 原子輪替，升級後重新登入。

LINE 綁定／查單改為必填 ID token，由固定官方 HTTPS 接口核驗並限定逐筆已綁定的預約；自報 userId 與同電話的未綁定訂單不授權。007 新增核驗時間，保留舊資料但不追認身分。先備份並依序補缺少的 `002` 到 `007_line_identity.sql`，再更新前後端；不可刪 volume。

使用免費測試工具與 test classpath 合成 LINE adapter；未使用真實 LINE 帳號或手機，正式程式沒有偽身分 fallback。操作、API 合約、設定與驗收範圍見 [AUTH_LINE_SECURITY.md](../AUTH_LINE_SECURITY.md)，本機／真實帳號及獨立 LINE 呼叫方更新列在 [CLOUD_LOCAL_FOLLOWUP.md](../CLOUD_LOCAL_FOLLOWUP.md)，測試見 [本輪紀錄](verification/auth-line-security-2026-10-06.json)。


## 2026-10-06：退款與人工對帳（Cloud）

ADMIN 可從付款紀錄／預約明細申請、審核、模擬退款或登記人工完成。待審核保留額度；模擬不扣帳，人工完成需憑證並扣已收淨額。原付款與訂單總額保留，版本、訂單交易鎖、識別碼及歷程回滾防止超退與重複扣帳。STAFF 與旅客不取得內部核對憑證。

晚到收款可人工歸帳或安排退款；舊帳差異需留憑證核對，不自動覆蓋。有待處理／已實退的取消訂單不可重開，已實退後不自動再收原餘額。既有 DB 先備份並補缺少的 `002` 到 `006_payment_refunds.sql`，再更新前後端；不可刪 volume。

本次全部採免費 mock／合成憑證，沒有真實金流退款。本機升級與商家憑證驗收暫緩。操作、API、帳務定義及限制見 [PAYMENT_REFUNDS.md](../PAYMENT_REFUNDS.md)，待補事項見 [CLOUD_LOCAL_FOLLOWUP.md](../CLOUD_LOCAL_FOLLOWUP.md)，驗證見 [本輪紀錄](verification/payment-refunds-2026-10-06.json)。


## 2026-10-06：未付款期限與庫存釋放（Cloud）

新預約保留 30 分鐘（可設定），到期且未收款的 pending 預約自動取消，逐晚釋放全部營位項目；租借品隨有效訂單釋放。舊訂單不補期限，已收訂金／確認／入住／完成不自動取消。人工重開重新檢查營位與租借庫存，舊付款連結保持失效。

付款、取消、金額表單與逾時共用訂單交易鎖；付款與金額調整也增加操作版本，金額 PATCH 新增必填 `expectedVersion`。綠界瀏覽器導回僅導頁；晚到合法收款標記 `paid_review` 待人工核對，不恢復庫存或自動退款。

既有 DB 先備份，依序補缺少的 `002`、`003`、`004`、`005_booking_holds.sql` 再更新前後端；不可刪 volume。使用者電腦升級與商家 sandbox 仍暫緩；Cloud 退款／人工核對已完成，見本文件最上方最新狀態。

- [期限規則、API、設定與升級](../BOOKING_HOLDS.md)
- [Cloud／本機待補清單](../CLOUD_LOCAL_FOLLOWUP.md)
- [本輪驗證紀錄](verification/booking-holds-2026-10-06.json)


## 2026-10-06：入住／退房登記（Cloud）

ADMIN／STAFF 都可在入住名單、列表與明細登記實際入住／退房；退房才會完成訂單。預計日期與實際狀態分開，逾期未退房仍列入當日名單／日曆。舊完成訂單保留空的實際時間，不推算補登。

一般狀態修改限定 ADMIN，僅允許尚未入住的確認、取消、重新開啟；不得直接標記完成。操作版本與訂單 row lock 防止重複登記、取消競爭及舊頁面覆寫，狀態與操作者歷程同一交易保存。日期查詢合併租借品並完整讀取分頁。

既有 DB 先備份，依序補 `002`、`003`、`004_booking_stays.sql`（已套用者可略過）再更新前後端；不可刪 volume 重建。一般狀態／住宿寫入都需 `expectedVersion`。私有 `JWT_SECRET` 與固定角色的員工管理頁仍保留；回本機的升級與營運驗收暫緩。

- [住宿登記規則、API 與升級](../BOOKING_STAYS.md)
- [角色矩陣與帳號操作](../CAMP_PERMISSIONS.md)
- [Cloud／本機待補清單](../CLOUD_LOCAL_FOLLOWUP.md)
- [本輪驗證紀錄](verification/booking-stays-2026-10-06.json)

Java 25 / Spring Boot 4 backend, Vue frontend, PostgreSQL, Redis, and MinIO.

This repository is being refocused into a single-campsite official site and booking platform for Fa Dai Ting Campground.

目前已完成：

- 前台官網首頁、營位介紹、營區地圖、最新消息、FAQ、線上預約頁
- 後台登入與 DB 權限驅動的動態路由
- 後台營區基本資料、營區區域、營位、價格方案、每日庫存 CRUD
- 後台預約列表與狀態更新
- 後台預約可顯示付款狀態，並在明細調整已收金額、加減費用與內部備註
- 後台付款紀錄頁可依日期區間查詢、查看摘要統計，並匯出 LINE Pay / 綠界 / mock 付款單與 callback payload
- 已建立 payment record 支付骨架，支援 `request / confirm` mock 流程、LINE Pay adapter 與綠界 POST form / callback / 瀏覽器導回骨架
- 付款取消會同步更新付款紀錄與公開處理歷程，重複通知不會重複寫入
- mock 付款失敗會同步更新付款紀錄與公開處理歷程，旅客可回訂單頁重新付款
- 前台已提供旅客預約查詢頁，可用訂單編號與聯絡電話查看公開狀態、付款摘要與處理進度，並重新建立 LINE Pay / 綠界付款請求
- 已新增 [PAYMENT_SANDBOX_SETUP.md](../PAYMENT_SANDBOX_SETUP.md) 與 [backend/payment.env.example](../backend/payment.env.example)，整理 LINE Pay / 綠界 sandbox 與正式設定欄位
- 已新增通知紀錄骨架：前台送單與付款成功會自動建立通知紀錄，後台可在通知紀錄頁查看與更新狀態
- 已新增 LINE 官方帳號 webhook 骨架：可接 `follow` 與文字訊息，回覆營區資訊、營位、公告、FAQ、預約與查單入口
- 已新增 `line_user_binding` 與 LINE 整合 API 骨架，依核驗 LINE ID token 與逐筆綁定查單；自報 `lineUserId` 不授權，實際 LIFF／獨立呼叫方仍待驗收
- 本機測試帳號同步 skill 與 Tampermonkey 自動填密碼流程

Planning notes:

- [CAMP_PLATFORM_PLAN.md](../CAMP_PLATFORM_PLAN.md)
- [LINE_PLATFORM_SPLIT_PLAN.md](../LINE_PLATFORM_SPLIT_PLAN.md)
- [LINE_OFFICIAL_ACCOUNT_SETUP.md](../LINE_OFFICIAL_ACCOUNT_SETUP.md)

## Structure

```text
backend/    Spring Boot API
frontend/   Vue admin frontend
db/init/    PostgreSQL initialization SQL
codex-skills/  Portable Codex skills for this project
```

## New Machine Handoff

For a fresh computer or fresh Codex thread, start with:

- [NEW_MACHINE_SETUP.md](../NEW_MACHINE_SETUP.md)
- [HANDOFF.md](HANDOFF.md)
- [README_AI.md](../README_AI.md)

## Local Development

啟動前先設定私有 `JWT_SECRET`，見 [角色與登入設定](../CAMP_PERMISSIONS.md)。

Start infrastructure:

```powershell
docker compose up -d
```

Or run the full Docker development stack, including backend and frontend:

```powershell
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

With the full Docker development stack, Java 25 and Node dependencies run inside containers. Your machine only needs Docker.

Run backend:

```powershell
cd backend
mvn -DskipTests package
java -jar target\fadaiting-camp-api-1.0-SNAPSHOT.jar
```

Run frontend:

```powershell
cd frontend
npm install
npm run dev
```

LAN access from another computer on the same network:

```text
http://localhost:5173/
http://localhost:5173/admin/login
```

Notes:

- Frontend dev server now listens on `0.0.0.0:5173`
- Backend now listens on `0.0.0.0:9090`
- If another computer still cannot connect, check Windows Firewall inbound rules for ports `5173` and `9090`

Open:

```text
http://127.0.0.1:5173/
http://127.0.0.1:5173/admin/login
```

Database connection:

```text
Host: localhost
Port: 5432
Database: fadaiting_camp
Username: camp
Password: camp
```

Default development login:

```text
username: admin001
password: <seed 預設密碼：見 db/init，首次登入後立即更改>
```

本機若安裝 Tampermonkey 測試腳本：

- 登入頁帳號空白時會自動帶入 `admin001`
- 輸入已同步的測試帳號時會自動補密碼
- userscript 本體採 `@require file:///...` 載入本機 runtime
- 需在 Chrome 擴充功能頁為 Tampermonkey 開啟 `Allow access to file URLs`

## Full Docker Stack

```powershell
docker compose -f docker-compose.prod.yml up -d --build
```

Open:

```text
http://localhost:8080/admin/login
```

## Current Campground API

Public read endpoints:

```text
GET /fadaiting-camp/api/public/camp/site
GET /fadaiting-camp/api/public/camp/zones
GET /fadaiting-camp/api/public/camp/announcements
POST /fadaiting-camp/api/public/booking
POST /fadaiting-camp/api/public/booking/lookup
POST /fadaiting-camp/api/public/booking/summary
POST /fadaiting-camp/api/public/payment/request
POST /fadaiting-camp/api/public/payment/retry
POST /fadaiting-camp/api/public/payment/confirm
POST /fadaiting-camp/api/public/payment/cancel
POST /fadaiting-camp/api/public/payment/fail
POST /fadaiting-camp/api/public/payment/ecpay/callback
POST /fadaiting-camp/api/public/payment/ecpay/return
POST /fadaiting-camp/api/public/line/webhook
POST /fadaiting-camp/api/public/line/bind/request
POST /fadaiting-camp/api/public/line/bind/confirm
POST /fadaiting-camp/api/public/line/booking/list
POST /fadaiting-camp/api/public/line/booking/detail
GET /fadaiting-camp/api/camp/zone/list
POST /fadaiting-camp/api/camp/zone
PUT /fadaiting-camp/api/camp/zone
DELETE /fadaiting-camp/api/camp/zone/{id}
GET /fadaiting-camp/api/camp/unit/list
POST /fadaiting-camp/api/camp/unit
PUT /fadaiting-camp/api/camp/unit
DELETE /fadaiting-camp/api/camp/unit/{id}
GET /fadaiting-camp/api/camp/rate-plan/list
POST /fadaiting-camp/api/camp/rate-plan
PUT /fadaiting-camp/api/camp/rate-plan
DELETE /fadaiting-camp/api/camp/rate-plan/{id}
GET /fadaiting-camp/api/camp/availability/list
POST /fadaiting-camp/api/camp/availability
PUT /fadaiting-camp/api/camp/availability
DELETE /fadaiting-camp/api/camp/availability/{id}
POST /fadaiting-camp/api/booking/list
PATCH /fadaiting-camp/api/booking/status
PATCH /fadaiting-camp/api/booking/detail
POST /fadaiting-camp/api/payment/list
```
