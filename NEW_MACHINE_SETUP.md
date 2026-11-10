# 新電腦接手與 Codex 快速啟動

## 2026-10-29：SeaweedFS 替換分支（正式主機尚未切換）

使用者指定以 SeaweedFS 取代 Camp MinIO。產品 S3 SDK、全新獨立 Volume、同源圖片路徑與真服務測試見 [SEAWEEDFS_MIGRATION](SEAWEEDFS_MIGRATION.md)。不改 SQL／migration／帳號，不將舊 MinIO volume 交給 SeaweedFS。中央 v4 是既有主機部署契約；目前正式來源仍為 `1d9b8d100654fd2ab9d0a32afbcc541c6bf5c2cd` 的已驗收快照，不以 Cloud 測試宣稱已搬遷。

儲存服務、設定 fence、發布 recipe 與圖片 URL／資料搬遷需要主機管理者另審；本輪用獨立 PR 交付，不直接合 main 或部署。以下 MinIO／舊部署描述保留為歷史；最新替換範圍與未完成主機驗收以連結文件為準。


## 展示版本接手入口

發呆亭本輪收尾後進入展示停點；回本機先依本文件準備環境，按 [DEMO_WALKTHROUGH.md](DEMO_WALKTHROUGH.md) 操作。六類延期工作見 [TODO_AI.md](TODO_AI.md)，最新實測與限制見 [交接](docs/HANDOFF.md)，不要為展示啟用真實金流／寄信或刪除原有資料。

## 2026-10-06：通知保留試算設定

`NOTIFICATION_RETENTION_PREVIEW_DAYS=180` 預設草案，可設 30–3650；dev／prod Compose 同步。ADMIN 在通知頁查看全站唯讀試算，不啟動刪除排程、worker 或寄信。實際期限須另核定，本機 PG18／正式資料驗收維持待補，見 [NOTIFICATION_RETENTION.md](NOTIFICATION_RETENTION.md)。

## 2026-10-06：可選備份觀測部署

先以 [備份觀測操作文件](BACKUP_OBSERVABILITY.md) 建立私有紀錄，設定來源識別碼與門檻；本地 Java 設絕對目錄，容器額外使用 `docker-compose.backup-observation.yml` 並指定既有主機私有目錄。bind 唯讀、不自建來源、不掛 Docker socket，預設保持未設定。Linux／WSL 權限與本機 PG18／MinIO／正式部署仍需實機驗收。

本文件給「全新電腦、全新 VS Code、全新 DBeaver、全新 Codex 對話」使用。目標是 clone 後可以快速恢復目前的開發節奏。

## 目前專案狀態

- 專案：`發呆亭露營區官網與訂位平台`
- 前端 package：`fadaiting-camp`
- 後端 artifact：`fadaiting-camp-api`
- 後端 Java package：`com.fadaiting.camp`
- 後端 context path：`/fadaiting-camp`
- 前端統一透過 `/api` proxy 呼叫後端，不要在前端硬寫 context path。
- PostgreSQL DB：`fadaiting_camp`
- PostgreSQL user/password：`camp` / `camp`
- 預設後台帳號：`admin001` / `<seed 預設密碼：見 db/init，首次登入後立即更改>`

## 升級到員工管理、住宿、付款期限、退款、身分安全與通知佇列版本

備份及一次性隔離還原操作見 [BACKUP_RESTORE.md](BACKUP_RESTORE.md)：Python 3.10+、Docker 與 Linux／WSL，dump 以二進位搬移。工具要求來源至少有 migration 008 的必要表；更舊資料升級前仍須先由管理者獨立備份。Cloud 實測為 PostgreSQL 17，本文件 Compose 使用 18，需在可信本機測試副本另驗收；MinIO／環境設定另備份，不把臨時還原容器當正式服務。

此功能已整合至 `main`。開始工作先同步遠端 `main` 並閱讀 [docs/HANDOFF.md](docs/HANDOFF.md)，不要繼續使用舊功能分支作為基準。既有資料庫需先備份，再依序套用 `db/init/002_operational_roles.sql`（已套用可略過）、`003_account_sessions.sql`、`004_booking_stays.sql`、`005_booking_holds.sql`、`006_payment_refunds.sql`、`007_line_identity.sql` 與 `008_notification_queue.sql`，再啟動新版前後端；不刪除 volume。新資料庫會依序初始化八份 SQL。升級後舊 JWT 必須重新登入，且啟動須設定私有 `JWT_SECRET`，詳見 [CAMP_PERMISSIONS.md](CAMP_PERMISSIONS.md)。管理員登入後從「系統管理 → 員工帳號」管理固定角色。實際入住／退房操作與資料規則見 [BOOKING_STAYS.md](BOOKING_STAYS.md)。

預約付款期限、批次開關與庫存核對見 [BOOKING_HOLDS.md](BOOKING_HOLDS.md)。005 不為舊訂單補期限；先核對庫存保留數量，缺少保留紀錄會回滾並要求人工修復。

退款與人工核對需要先套用 006，保留舊付款與原總額；規則見 [PAYMENT_REFUNDS.md](PAYMENT_REFUNDS.md)。人工完成登記不代表本系統執行轉帳，本機與商家驗收仍待補。

Cookie／CSRF 與 LINE 身分設定見 [AUTH_LINE_SECURITY.md](AUTH_LINE_SECURITY.md)。HTTP 開發明確設 `AUTH_COOKIE_SECURE=false`；正式 HTTPS 保持 true。007 不追認舊 LINE 綁定，LINE 呼叫方需改送 ID token；未設定 Login Channel 時 LINE 身分接口回 503，網站查單仍可用。

通知佇列需先套用 008；舊通知保留但不自動排入。真實寄信與 worker 預設關閉，Cloud／本機可使用後台免費模擬。設定見 [NOTIFICATION_QUEUE.md](NOTIFICATION_QUEUE.md)。

ADMIN 通知頁有全站唯讀觀測與告警；`NOTIFICATION_OVERDUE_THRESHOLD_SECONDS` 預設 900 秒（60–86,400），dev／production Compose 都已接入。觀測不會啟用寄信或重送。詳見 [NOTIFICATION_OBSERVABILITY.md](NOTIFICATION_OBSERVABILITY.md)；完整部署仍需本機送達／容量驗收。

## 新電腦需要先裝

- Docker Desktop
- Git
- VS Code
- DBeaver
- Java 25（若要本機跑後端；只用 Docker 跑完整 dev stack 則可晚點裝）
- Node.js 24 或相容版本（若要本機跑前端；只用 Docker 跑完整 dev stack 則可晚點裝）

VS Code 建議 extension：

- Vue - Official / Volar
- Extension Pack for Java
- Docker
- GitLens
- SQLTools（可選，DBeaver 已足夠）

## Clone 後第一步

```powershell
cd D:\Project
git clone https://github.com/jackteng-dev/fadaiting-camp.git
cd D:\Project\fadaiting-camp
```

若路徑不是 `D:\Project\fadaiting-camp`，後續指令的 `--project` 或文件路徑要跟著改。

若之後要接正式金流，請一併查看：

- [PAYMENT_SANDBOX_SETUP.md](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/PAYMENT_SANDBOX_SETUP.md)
- [backend/payment.env.example](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/backend/payment.env.example)

若之後要接 LINE 官方帳號，也請同步準備：

- `LINE_BOT_ENABLED=true`
- `LINE_BOT_CHANNEL_SECRET`
- `LINE_BOT_CHANNEL_ACCESS_TOKEN`
- `LINE_BOT_FRONTEND_BASE_URL`

正式 LINE 官方帳號、LIFF、Rich Menu 串接請先閱讀：

- [LINE_OFFICIAL_ACCOUNT_SETUP.md](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/LINE_OFFICIAL_ACCOUNT_SETUP.md)

## 啟動方式 A：只跑基礎服務，本機跑前後端

```powershell
cd D:\Project\fadaiting-camp
docker compose up -d
```

後端：

```powershell
cd D:\Project\fadaiting-camp\backend
mvn -DskipTests package
java -jar target\fadaiting-camp-api-1.0-SNAPSHOT.jar
```

前端：

```powershell
cd D:\Project\fadaiting-camp\frontend
npm install
npm run dev
```

打開：

- 前台：`http://127.0.0.1:5173/`
- 後台：`http://127.0.0.1:5173/admin/login`

若也要開獨立 LINE 專案骨架：

```powershell
cd D:\Project\fadaiting-camp\fadaiting-line\server
copy .env.example .env
npm install
npm run dev

cd D:\Project\fadaiting-camp\fadaiting-line\web
copy .env.example .env
npm install
npm run dev
```

打開：

- LINE server health：`http://127.0.0.1:9191/api/health`
- LINE web：`http://127.0.0.1:5174/`
- LINE menu preview：`http://127.0.0.1:9191/api/line/menu-preview`

若要正式接 LINE Developers：

- `LINE_CHANNEL_SECRET`
- `LINE_CHANNEL_ACCESS_TOKEN`
- `LINE_DATA_API_BASE_URL`
- `WEB_BASE_URL` 改成對外 HTTPS LIFF 網址
- `MAIN_FRONTEND_BASE_URL` 改成對外 HTTPS 官網網址
- `VITE_LIFF_ID` 填入 LINE Developers 建立的 LIFF ID

## 啟動方式 B：完整 Docker 開發模式

```powershell
cd D:\Project\fadaiting-camp
docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

這種方式會用 container 跑 Java 25 和 Node，不要求本機先裝好 Java / Node。

## DBeaver 連線

```text
Host: localhost
Port: 5432
Database: fadaiting_camp
Username: camp
Password: camp
```

重要：如果要從舊 DB 複製中文資料，不要用 PowerShell 文字管線跑 `pg_dump | psql`，會有機會把中文變成 `?`。請在 PostgreSQL container 內用 custom dump：

```powershell
docker exec fadaiting-camp-postgres bash -lc "pg_dump -U camp -d fadaiting_camp -Fc -f /tmp/fadaiting_camp.dump"
```

## 安裝 Codex skill

本 repo 已保留一份可攜帶的 skill：

```text
codex-skills/fadaiting-camp-account-test-sync
```

新電腦 clone 後，複製到 Codex skills 目錄：

```powershell
$env:CODEX_HOME = "$env:USERPROFILE\.codex"
New-Item -ItemType Directory -Force -Path "$env:CODEX_HOME\skills" | Out-Null
Copy-Item -Recurse -Force `
  "D:\Project\fadaiting-camp\codex-skills\fadaiting-camp-account-test-sync" `
  "$env:CODEX_HOME\skills\fadaiting-camp-account-test-sync"
```

用途：

- 當 Codex 新增或修改本機測試帳號時，自動更新：
  - `db/test-accounts-plaintext.xlsx`
  - `db/test-accounts-plaintext.json`
  - `frontend/public/local-userscripts/admin-login-autofill.user.js`
  - `frontend/public/local-userscripts/admin-login-autofill.runtime.js`
- 這些檔案含明文密碼，只供本機測試，已被 `.gitignore` 排除。

若要手動同步預設帳號：

```powershell
# 這兩個路徑若不同，請讓 Codex 先用 load_workspace_dependencies 找出本機 runtime。
$node = "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe"
$deps = "$env:USERPROFILE\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\node_modules"
if (-not (Test-Path "D:\Project\fadaiting-camp\.codex-runtime\node_modules")) {
  New-Item -ItemType Junction -Path "D:\Project\fadaiting-camp\.codex-runtime\node_modules" -Target $deps | Out-Null
}
& $node "$env:CODEX_HOME\skills\fadaiting-camp-account-test-sync\scripts\sync_fadaiting_camp_test_accounts.mjs" `
  --project "D:\Project\fadaiting-camp" `
  --username "admin001" `
  --password "<seed 預設密碼：見 db/init，首次登入後立即更改>" `
  --role "admin" `
  --purpose "預設後台管理員"
```

如果新電腦的 Codex runtime 路徑不同，讓 Codex 先執行一次 skill；它會依目前環境調整 Node runtime。

## Tampermonkey

目前測試登入靠 Tampermonkey，不再靠 Vue 預設帳密。安裝 userscript 後：

- 登入頁帳號空白時會自動填 `admin001`
- 輸入已同步的測試帳號時會自動填密碼
- 舊帳號 `swaggydickson` 已被視為 legacy，會清空
- userscript 本體只保留 header，真正邏輯由本機 runtime 檔 `@require`
- 若要讓 `@require file:///...` 生效，需在 Chrome 擴充功能頁為 Tampermonkey 開啟 `Allow access to file URLs`
- 當 Codex 再同步新測試帳號後，若 Tampermonkey 已安裝這個 header 版本，通常只要重新整理登入頁即可讀到新的 runtime 內容

本機 userscript 位置：

```text
frontend/public/local-userscripts/admin-login-autofill.user.js
frontend/public/local-userscripts/admin-login-autofill.runtime.js
```

建議安裝 / 驗證流程：

1. 用 `http://127.0.0.1:5173/local-userscripts/admin-login-autofill.user.js` 安裝或更新 Tampermonkey 腳本
2. 到 `chrome://extensions` 開啟 Tampermonkey 的 `Allow access to file URLs`
3. 確認前端 dev server 正在執行
4. 之後新增測試帳號時，先讓 Codex 執行 skill，同步 runtime 檔
5. 重新整理 `http://127.0.0.1:5173/admin/login`，直接輸入新帳號測試自動補密碼

## 新 Codex 對話建議開場

```text
這是發呆亭露營區官網與訂位平台。請依 AGENTS.md 固定流程先 fetch 遠端 main，閱讀該版本 docs/HANDOFF.md，再核對 git status、目前 branch 與最近 commit，確認後以 fast-forward 同步 main。再閱讀 README.md、PROJECT_CONTEXT.md、README_AI.md、TODO_AI.md、NEW_MACHINE_SETUP.md，並確認 codex-skills/fadaiting-camp-account-test-sync 是否已安裝到 $CODEX_HOME/skills。接著依現有 Vue 3 / Spring Boot 4 / MyBatis / PostgreSQL 架構繼續開發。
```
