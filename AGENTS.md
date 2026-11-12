# AGENTS.md

## 部署限制（最優先，2026-11-12）

本 repo 的 main 會由作者的自架伺服器自動部署（GitHub Actions 發布映像 → 伺服器 CD 自然輪詢、先備份與隔離演練再切換）。在本機或任何 AI 工具修改時，**下列規則優先於本檔其他內容**：

1. **一律走 PR**：開功能分支、push、開 PR，`verify` CI 通過後才 merge。不可直接 push main、不可 force push（分支保護也會拒絕）。本檔較舊段落提到「push main」者，以本規則為準。
2. **本機開發用本機資料庫**：用 repo 內的 docker compose／說明啟動全新空資料庫，不要連線或下載正式資料庫，不要把帳密、token、`.env` 寫進 Git。
3. **受保護路徑（CD 資料契約）**：PR 若修改下列路徑，CD 會拒絕自動部署。請先停下，告知作者在伺服器端執行受控切換（contract switch）後才能 merge：
   - `db/init/`（schema 與初始資料，含虛構營區資料）
   - `backend/src/main/resources/application.yml`
4. 只在本機跑測試、編譯、啟動，不會修改上述檔案（Maven 會把 resources 複製到 `target/`，該目錄已被忽略）。
5. 營區資料一律使用虛構內容（不得加入真實地址、電話、Email、社群連結）。

**資料庫結構變更（新增表格／欄位、索引、改名、刪除）不會自動部署。** 寫法：`db/init/` 的新 SQL（只在全新資料庫執行，正式資料庫不會自動套用）。流程：
1. 本機空資料庫測試通過後開 PR，**先不要 merge**，告知作者「這個 PR 有 schema 變更」；由作者在伺服器端以受控工具處理（暫停 CD → 備份正式資料庫 → 隔離副本試套 → merge 與發布 → 套用正式 → 恢復 CD）。
2. migration 必須**新舊版程式都能運作**（CD 會在新 schema 上演練回退到舊版）：可直接做的是新增表格、新增可 NULL 或有預設值的欄位、新增索引；改名、刪欄、改型別要分兩次（先加新欄位並讓程式相容，穩定後再移除舊的）；不可一次 DROP 舊版仍在使用的結構。
3. 不修改已套用過的 migration 檔（checksum 會失敗）。

push 前請用 `git diff --stat origin/main` 確認是否動到受保護路徑，並在 PR 說明中註明。

本文件提供給所有接手本專案的 AI 助手（Codex / ChatGPT / Claude / 其他代理）作為共同工作規範。

## 語言規範

- 一律使用繁體中文回覆、註解與文件。
- 除非既有程式已使用英文常數或外部 API 欄位，否則不要混用簡體中文。

## 核心原則

- 不要擅自修改整體架構、技術棧或目錄分層。
- 修改前先閱讀現有程式與相鄰模組，理解目前寫法後再動手。
- 優先延續既有命名、路由、DTO、Mapper、Service、View 結構。
- 只做和需求直接相關的變更，不順手大改無關區塊。
- 發現程式與文件不一致時，先以程式實際行為為準，再同步更新文件。

## 技術棧

### 前端

- Vue 3
- Composition API
- TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus

### 後端

- Spring Boot 4
- Java 25
- MyBatis
- PostgreSQL
- Redis
- SeaweedFS（S3 介面；主機替換另審，見 SEAWEEDFS_MIGRATION.md）

## 前端開發規範

- 一律使用 Vue 3 `script setup` + Composition API。
- 頁面放在 `frontend/src/views`。
- 可重用元件放在 `frontend/src/components`。
- 前台官網元件集中放在 `frontend/src/components/storefront`。
- 後台 layout 元件集中放在 `frontend/src/components/layout`。
- API 呼叫集中在 `frontend/src/api`，不要把 axios 請求直接散落在頁面裡。
- 狀態管理放在 `frontend/src/stores`。
- 新增前台頁面時，優先沿用目前 storefront layout 與路由分流。

## 後端開發規範

- Controller 只負責收參數、回應格式與權限入口，不塞商業邏輯。
- 商業邏輯放在 Service / ServiceImpl。
- SQL 與資料存取維持在 Mapper。
- DTO 命名維持 `*Dto` / `*Request` / `*Response`。
- 若是管理端 API，先確認是否需要同步 Redis 權限或快取資料。
- 不要任意變更資料表欄位型別、主外鍵或初始化資料，除非需求明確要求。

## API 路徑規則

- 後端實際 context path 為 `/fadaiting-camp`，由 `application.yml` 設定。
- 前端一律透過 `/api` proxy 呼叫後端，不要在前端硬寫 `/fadaiting-camp`。
- 公開 API 使用：
  - `/api/public/...`
- 後台管理 API 使用：
  - `/api/admin/...`
  - `/api/camp/...`
  - `/api/booking/...`
  - `/api/role/...`
  - `/api/category/...`
  - `/api/article/...`
- API path 維持小寫，必要時使用連字號，例如 `rate-plan`。

## 命名規則

### 前端

- Vue 元件檔名：`PascalCase.vue`
- 頁面元件：
  - 前台以 `Storefront*.vue`
  - 管理頁依功能命名，例如 `BookingList.vue`
- API 方法命名：
  - 查詢：`getXxxService`
  - 新增：`createXxxService`
  - 更新：`updateXxxService`
  - 刪除：`deleteXxxService`

### 後端

- 類別：`PascalCase`
- 變數 / 方法：`camelCase`
- Controller：`*Controller`
- Service：`*Service` / `*ServiceImpl`
- Mapper：`*Mapper`
- DTO：`*Dto` / `*Request` / `*Response`

## 元件規範

- 不要把前台與後台 UI 混在同一個 layout。
- 首頁只保留首頁應該有的摘要內容；已有獨立頁的內容不要在首頁重複做完整區塊。
- Footer 承接聯絡資訊；Header 承接主要導覽。
- 若新頁面有共用區塊，先抽成元件再重複使用。
- 樣式以 scoped CSS 為主，除非必須共用才放全域樣式。

## 程式風格

- 延續現有程式縮排與分段風格，不強行套用不同團隊習慣。
- 優先寫可讀性高的直接寫法，不過度抽象。
- 避免單字母變數名稱。
- 避免大段未使用程式、註解掉的舊邏輯、或過度冗長的註解。
- 若有複雜邏輯，可補一行短註解說明目的，不要寫流水帳註解。

## Commit 規則

- Commit 訊息請清楚描述功能，建議格式：
  - `feat: 新增前台營區地圖頁`
  - `fix: 修正預約表單送出錯誤`
  - `docs: 更新 AI 接手文件`
  - `refactor: 整理 storefront layout`
- 若一次改很多層面，訊息要能概括主軸，不要只寫 `update` 或 `modify`。

## 開發注意事項

- 本機若不用 Docker 跑後端，需 Java 25。
- 若使用 Docker 開發，可直接用 `docker-compose.yml + docker-compose.dev.yml` 跑前後端與基礎服務。
- PostgreSQL 預設 DB：`fadaiting_camp`，帳號 / 密碼：`camp` / `camp`。
- 前端 dev server 預設 port：`5173`
- 後端 port：`9090`
- 生產前端容器對外 port：`8080`
- PostgreSQL 預設 port：`5432`
- Redis 預設 port：`6379`
- SeaweedFS 本機 S3：`127.0.0.1:9000` → 容器8333；不公開 filer／master／console，正式主機仍須另審遷移。
- 目前前台圖片多為示意圖，未全面替換成真實營區照片。
- 目前專案仍混有部分舊的文章 / 分類 / 會員管理模組，開發露營區官網時不要誤刪舊模組，除非需求明確要求。
- 若新增或修改本機測試登入帳號，必須使用 `fadaiting-camp-account-test-sync` skill；repo 內備份位於 `codex-skills/fadaiting-camp-account-test-sync`，新電腦需安裝到 `$CODEX_HOME\skills`。
- 不要用 PowerShell 文字管線搬移 PostgreSQL 中文資料；跨 DB 備份 / 還原請用 container 內 `pg_dump -Fc` / `pg_restore`。

## 工作階段與 context 安全規則

本規則適用於整個展示平台的各倉庫與長任務；交接及保存進度的優先級高於繼續實作。

1. 長任務期間若能取得 context／token／usage／使用額度狀態，必須主動檢查，至少在規劃下一階段及完成里程碑時核對；不得捏造剩餘數字。沒有可用介面時，依已知限制與任務規模保守安排階段及保存點。
2. 不可等到限制耗盡才交接；開始大型階段前，須預留相關測試／build、更新交接、commit、push 與遠端核對所需的 context 及額度。
3. 判斷剩餘 context 或使用額度可能不足以安全完成下一階段並保存時，立即停止新增大型修改，縮小本輪範圍，先收尾目前工作。
4. 此時優先執行相關測試／production build、更新 `docs/HANDOFF.md`、在功能分支 commit、push 並開 PR。交接須寫明目前狀態、已完成與未完成項目、實際驗證結果及續作方式；失敗或未執行的檢查不可標為成功。
5. 交接與保存進度的優先級高於繼續實作；完成保存與必要狀態核對前，不開啟下一個大型工作階段。
6. 每完成一個可獨立驗證的里程碑，就執行相應檢查、更新 HANDOFF、在功能分支 commit、push 並開 PR，確認保存後再接下一階段；不要等整個大型任務完成才第一次保存。
7. 發生 context compaction、資訊遺失或目前狀態不明時，必須重新從 repository 原始檔、`git status`、`git diff`（含 staged diff）、`git log` 與 `docs/HANDOFF.md` 確認實際狀態，並核對遠端 `main`，才能繼續。不得僅依對話摘要推定測試已通過、修改已完成或 commit 已推送，也不得丟棄尚未確認來源的修改。
8. 每次階段收尾最後確認 `git status` 乾淨，fetch 後核對遠端 `main` 已包含最新 commit、本地與遠端一致。若測試／build／push 或分支保護阻擋完成，保留安全且可審查的成果，記錄具體原因，不 force push、不偽稱已保存或狀態乾淨。

## 固定工作與交接流程（使用者指定）

本流程適用於整個專案展示平台及所有作品，包含 Web／後端／App 與後續搶票專案。自 2026-10-06 起，遠端 `origin/main` 是每個新工作階段的基準；不得假設本地 workspace 或上一輪功能分支已是最新版本。

1. 開始前先 `git fetch origin main`，取得遠端最新版本；不得以 reset 或強制切換覆蓋未提交修改。
2. 先閱讀遠端最新版 `docs/HANDOFF.md`（可用 `git show origin/main:docs/HANDOFF.md`），了解上一輪完成內容、待辦、已知問題與下一步。
3. 再查看 `git status`、目前 branch 與最近 commit。確認狀態正常後切換 `main` 並以 `git pull --ff-only origin main` 同步，然後才修改。若有分歧或未完成修改，先保留並查明來源，不任意丟棄。
4. 依需求閱讀 `README.md`、`PROJECT_CONTEXT.md`、`README_AI.md`、`TODO_AI.md`、`NEW_MACHINE_SETUP.md` 與相關程式。每個專案各自維護 `docs/HANDOFF.md`；一般單一作品任務仍只操作該作品，跨倉庫修改須在本次需求範圍內。
5. 每一階段完成後執行相關測試與 production build；前端包含型別檢查與 production build，Java 後端包含相應測試與 Maven package／verify；App 依自身工具鏈驗證，僅前端專案不要求後端建置。依實際範圍使用隔離資料庫驗證，不連線到其他專案或正式資料。
6. 測試與 build 成功後更新 `docs/HANDOFF.md`，至少記錄本次完成內容、重要修改檔案、測試／build 結果、尚未完成事項、已知問題與下一步建議；區分本輪實測與歷史驗證。
7. 最後在功能分支 commit、push 並開 PR 到 `main`。`main` 受分支保護（2026-11-09 起）：必須透過 PR、`verify` CI 通過才能 merge，管理員也不例外，禁止 force push 與刪除。CI 通過後以一般 merge 合併；merge 後由自架主機 CD 自動部署，不手動部署。push 前再次 fetch 核對遠端，若前進則整合後重跑必要檢查。
8. 確認重要修改皆已提交、PR 已合併或狀態已記錄，合併後本地 `main` 與遠端一致。無法完成的工作須在交接文件寫明狀態、原因與續作方式；帳密、原始敏感日誌與建置產物不得提交。若因權限或分支保護無法 push，保留可審查的 commit 並如實記錄，不擅自變更保護規則。
9. 下一輪重新從遠端 `main` 開始上述流程，不使用舊交接摘要代替最新版文件。

平台共同規範與倉庫盤點見 [PLATFORM_WORKFLOW.md](https://github.com/jackteng-dev/developer-portfolio/blob/main/docs/PLATFORM_WORKFLOW.md)。每個倉庫都保留本摘要，單獨 clone 仍需遵守；新作品初始化時加入同一規範與交接文件。若既有倉庫只有 master，先盤點歷史與部署依賴，不自行改名或建立假 main。

## 文件維護規則

- 只要功能狀態、啟動方式、API、資料表或已知問題有變動，就同步更新：
  - `docs/HANDOFF.md`
  - `PROJECT_CONTEXT.md`
  - `README_AI.md`
  - `TODO_AI.md`
  - `SCHEMA.md`
