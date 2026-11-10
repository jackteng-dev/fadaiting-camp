# 發呆亭最新交接

## 2026-11-08：自動更新端到端驗證（發呆亭 Camp）

本次只增加此交接段落；不變更應用邏輯、資料庫、帳號、映像 recipe、CI 或部署契約。

- 目的：驗證 Atlas／Encore CD 恢復與主機讀取憑證換新後，正常 PR → 原完整 CI → merge → 私人 GHCR 發布 → Ubuntu 主機自然排程部署的完整鏈仍可用。
- 無可見內容變更；以主機實際部署來源 SHA 等於本次 main 作為判定。
- 主機私人部署完成後，再確認 Funnel 公開唯讀版自動跟隨新部署版本重建。
- 未完成上述實測前，不以本段文件或映像發布宣稱自動更新 PASS；結果保存於 server-management 私人交接。

---

## 2026-10-19：私人主機 CD 首發完成，啟動最小自動更新驗證

本次只增加此交接段落與可回復的驗證標記 `CAMP-CD-VERIFY-20261019-A`；不變更應用程式、資料庫、帳號、映像 recipe、CI 或部署契約。

- **首次正式 CD：PASS。** 來源 `bf5ddb91f0398c4d745871690f9799fac016e5e2`，沿既有私人 GHCR 的固定 backend／gateway digest 與來源驗證部署。
- **資料保護與隔離演練：PASS。** 主機完成 Camp 專屬加密備份、隔離資料庫還原、MinIO／Redis 還原位元組核對、真舊新映像升級與隔離故障回舊版；正式資料庫沒有回灌，原資料容器／掛載及其他四作品版本保留。
- **正式網站與 API：PASS。** 首發驗收通過後才接受 Camp 自動更新，沿原共用 timer 串行執行。
- **這次新提交的完整自動更新：BLOCKED，等待實測。** 必須等此 PR 的原完整 CI 通過並正常合併，再核真正 main CI、私人 GHCR 新 digest、Ubuntu 自然排程交易與實際容器 OCI revision；不能只以本段文件或映像發布宣稱 AUTO-CD PASS。
- 無資料初始化、真實付款／通知、公開入口、網路設定或其他作品部署變更。本機原前端資產限制仍由 CD guard 保護。

完成後的主機證據保存於 server-management 的私人稽核與 HANDOFF；本文件不含主機憑證或個人聯絡資訊。

---

## 2026-10-06：展示階段最終收尾（本輪最新交接）

起始基準為遠端 main `038dbb2`。本輪先 fetch／讀交接／核對 Git，compaction 後再次核對遠端、status、diff／staged diff 與 log。使用者已指定「最後一輪收尾後開始下一個專案」；本輪只整理發呆亭展示文件並驗證，不再擴充功能或 schema。實際保存 SHA 以遠端與 Git log 為準。

### 本次完成內容

- 新增 [展示操作](../DEMO_WALKTHROUGH.md)：準備條件、8–12 分鐘展示順序、兩筆不同用途的合成訂單、固定角色、mock 金流／通知、備份觀測與正式營運界限。明確區分今日入住與未來取消退款情境。
- 重整 [TODO_AI.md](../TODO_AI.md)，移除無統一基準百分比、已完成的舊待辦及「尚缺 integration／e2e」舊敘述，整理現況與六類延期工作。發呆亭停止新增大型功能，下一個作品為 ELPSY Shop。
- README／CONTEXT／AI／schema／新機設定／Cloud 待補入口同步階段停點；修正 README 舊電腦絕對文件連結與 LINE 自報 userId 舊敘述。沒有登入帳號、業務程式、SQL／migration 變更。
- 已開始 ELPSY Shop 唯讀來源盤點：`Shop_back`（Vue 3／TypeScript 前端）＋`shop_bak_ap`（Spring Boot 3.2.1 後端）只有 main；`ELPSY_Shop-WEB`（Vue 3／Vite 前端）＋`ELPSY_Shop-AP`（Spring Boot 3.2.1 後端）只有 master。兩組後端使用 MySQL／Redis／MinIO，不能直接套用 camp PostgreSQL 設定。四個候選指定路徑 AGENTS.md／docs/HANDOFF.md 均回 404。名稱不足以判定正式來源，已提出來源確認，未 clone／修改／推送候選倉庫。日日選物 `e-commerce-platform` 是另一個作品。

### 重要修改檔案

`DEMO_WALKTHROUGH.md`、`TODO_AI.md`、`README.md`、`PROJECT_CONTEXT.md`、`README_AI.md`、`SCHEMA.md`、`NEW_MACHINE_SETUP.md`、`CLOUD_LOCAL_FOLLOWUP.md`、本交接及 [非敏感驗證彙總](verification/demo-closeout-2026-10-06.json)。原始測試腳本／日誌／fixture 留於 ignored work，帳密與建置產物不提交。

### 測試／build 結果

- **後端本輪 189 項通過，0 failure／error／skipped，Maven package BUILD SUCCESS**。22 組明確測試，通知與 LINE 整合開關啟用，camp 專用 PG17／Redis。以本輪 Maven Running 清單核對 XML，未混入 target 中其他歷史測試報告。
- **前端型別檢查與 production build 成功**。既有 simplebar 非 module、混合 import、大 chunk 警告保留。
- **真實 HTTP 21 項通過**：公開報價／送單／查單與錯誤電話、庫存保留／取消釋放、mock 重複付款不重複記帳、退款核准／模擬不假造實際退款、通知模擬不寄信、STAFF／匿名授權、版本 409、現場備註與今日入住／退房完成。
- **正式 dist preview Chromium 14 項通過**：官網／營位／預約、真實合成查單、ADMIN 登入／訂單明細／通知與保留／未設定備份／員工管理，STAFF 住宿備註及三個 ADMIN 管理入口直達拒絕，0 未捕捉 JS 錯誤。測試選擇器修正為實際 submit、可見元素與 textarea value，避免前台 KeepAlive 隱藏的同編號誤判；沒有因此改產品程式。
- HTTP／瀏覽器使用本輪 package JAR，運行檔 SHA-256 與 target 核對一致。瀏覽器驗證僅本機 loopback，非本機可選資產阻擋；沒有外部 HTTPS／真正手機驗收宣稱。
- 本輪 2 筆合成訂單、退款／付款／通知／timeline 及獨立營位／價格／庫存已清理；依 fixture 範圍先刪自己的退款再由 FK cascade 清理。既有 booking／notification 清理前後快照一致。獨立 backend19091／preview15174 已停止，原展示實例不動。
- 備份 Python 27 項及隔離還原是前輪歷史證據，本輪未重跑；不把歷史驗證標成本輪。未做真實金流、寄信／LINE、帳號變更、下載模型／image、其他倉庫修改或全域 Docker／proxy 調整。

### 尚未完成事項

六類延期：真實金流與旅客授權；通知送達與 LINE；本機部署與完整復原；營運報表；正式內容與 UX；長期營運維護。細項分別保留 TODO 與 Cloud 本機待補文件，**不阻擋轉往下一個展示作品**。

ELPSY Shop 正式來源待使用者確認。確認後先同步正確遠端分支、讀文件與 Git 狀態、盤點程式／依賴／資料模型及可建置性，補共同 AGENTS／HANDOFF 與 Cloud／本機待補分工；不先升級技術棧或預設付款功能已可營運。若採只有 master 的舊倉庫，先確認部署依賴與 main 流程處理，不自行改名或建立假 main。

### 已知問題

- 展示範圍已驗證，不等於正式營運。mock 付款／退款／通知不能宣稱商家扣款、退款或送達；備份觀測未設定應如實顯示未知。
- PG18／MinIO／Redis 狀態與環境憑證／全服務恢復、使用者本機容量、HTTPS／Secure cookie、正式手機、圖片／條款仍待實機與帳號驗收。
- 保留 180 天只是草案，無清理 API／排程；封存／去重／稽核與清理另需獨立需求。既有前端資產警告與整體手機 header 待補，不把局部頁面驗證延伸成全站手機完成。
- 沒有精確 context／usage 查詢介面；本輪只收尾及唯讀 Shop 盤點，先保存 camp 再進入已確認來源的下一里程碑。

### 下一步建議

1. **切換 ELPSY Shop**。先取得兩組候選的正式來源確認，再以正確遠端與實際程式建立第一個可驗證 Cloud 里程碑。保留原功能與資料，不把日日選物當 Shop，不改備份／舊版候選。
2. 初期以免費隔離環境、商品／會員／購物車／訂單／權限現況與 build 為基準，真實金流、商家／手機／App 本機驗收另列文件；先避免混用作品資料庫與密鑰。
3. 發呆亭留在展示停點；除新發現阻擋展示的 bug 或使用者重新指定，不追加報表、封存／清理或新觀測。每里程碑測試／build、六欄交接、commit／push main，最後核對乾淨與遠端一致。


## 2026-10-06：通知歷史保留與唯讀 dry-run（上一輪紀錄）

起始基準為遠端 main `c87edaf`；已 fetch、讀遠端交接、核對乾淨 main／log 並 ff-only 同步。僅修改 fadaiting-camp，完成單一可驗證里程碑後提交／推送 main；實際 SHA 以遠端與 git log 為準。

### 本次完成內容

- 新增 ADMIN `GET /notification/retention-preview`，readOnly transaction、一條 SQL snapshot 彙總全站所有通道／模式／新舊通知；總量 = 候選 + 保護，原因按優先序互斥分類。只回固定理由、數量與活動時間範圍，不回可刪 ID、訂單／收件人／內文／payload／去重鍵。
- 草案試算門檻 180 天（30–3650 可部署設定），單一 Clock／營區本地時間，取通知建立／修改／sent_time、訂單修改與預定退房日最大值；嚴格早於截止才可候選，相等保留。不是已核定正式政策，不接受 request 縮短門檻。
- 結果不明（含歷史 attempts）、待處理／failed／未知狀態、新版佇列／event_key、任何 attempts 或曾有次數但缺歷程、Email／sent／SMTP／未知 provider／錯誤、金流／非白名單事件、未結束訂單、排程／租約、近期或不完整時間均保護。只評估無付款舊站內／人工的 simulated／skipped／acknowledged；不破壞現有 event_key 去重。
- 通知頁新增獨立保留試算區，三項數字／保護理由／草案限制、手動更新；不受列表篩選，API 失敗清除舊值、不當零候選，列表故障不抹除試算。390px 區塊不溢出。沒有刪除 API／按鈕／排程、沒有寄信／claim／回收租約或修改歷程。
- `.env.example`／application.yml／dev／prod Compose 接入 `NOTIFICATION_RETENTION_PREVIEW_DAYS`，無 migration／SQL 寫入／帳號或權限資料更新。使用現有免費工具，不擴到其他作品。

### 重要修改檔案

新增後端 `NotificationRetentionProperties`、`NotificationRetentionBucketDto`／`NotificationRetentionPreviewDto`、`NotificationRetentionService`、`NotificationRetentionIntegrationTest`；更新 `NotificationMapper`、`NotificationAdminController`、`CampRolePolicy`／測試與 application.yml。前端新增 `api/notificationRetention.ts`、`components/booking/NotificationRetentionPreview.vue`，更新 `NotificationList.vue`；`.env.example`、dev／prod Compose。[操作文件](../NOTIFICATION_RETENTION.md)、[驗證彙總](verification/notification-retention-2026-10-06.json)，本交接及 README／CONTEXT／AI／TODO／SCHEMA／新機設定／Cloud 待補／通知佇列與觀測文件同步。

### 測試／build 結果

- **本輪最後版本後端 189 項通過，0 failure／error／skipped，Maven package BUILD SUCCESS**：上一輪 178＋10 項真實 PostgreSQL 保留規則＋1 項角色政策；camp 專用 PG17／Redis，通知／LINE 整合開關啟用。涵蓋三種候選終態、pending／failed／未知、歷史結果不明、去重／新版佇列、嘗試歷程／缺失、Email／未知 provider／錯誤、金流／未知事件／pending mock payment、租約、訂單活動／退房／已收款、截止相等／前一秒、設定天數／時區與唯讀／無個資。
- 首次 189 項通過後檢視保護邊界，補入 attempt_count 有值但缺 attempts 及終態殘留錯誤保護，重跑完整 189 與 package 成功。HTTP／瀏覽器使用此最後 JAR，實際運行檔 SHA-256 核對一致，未拿舊展示後端代替測試。
- **真實 HTTP 17 項通過**：ADMIN 200／STAFF 403／匿名 401、180 天草案／禁止刪除、9 筆合成通知中 2 候選與 7 保護的互斥理由 delta、GET 不改通知／attempts／timeline、無個資回應、request 縮短期限不生效、ADMIN POST 試算／DELETE 清理均 403。最後另核對全站既有通知／attempts／timeline 在重複 GET 與 fixture 清理後一致。
- **前端型別檢查及 production build 成功，正式 dist preview 瀏覽器 13 項通過**：實際三數／理由／草案說明、無清理按鈕、列表篩選不改全站試算、試算 503 清除舊值且列表／佇列保留、恢復重載、列表 503 保留獨立試算、390px、STAFF 直達拒絕、GET-only、0 未捕捉 JS 錯誤。可選非本機字型在驗收中阻擋，不繞 proxy／政策；不是外部 HTTPS 或真手機驗收。
- **dev／prod Compose 四項設定驗證通過**：各自預設 180 與覆寫 365；JSON 與新增相對文件連結／`git diff --check` 收尾核對。前端既有 simplebar 非 module、混合 import、大 chunk 警告仍在。
- 本輪合成 booking／9 通知／1 attempt 已刪除，既有資料保持不變；獨立 backend19091／preview15174 已停止，既有展示實例未停止。未改登入帳號或帳密，不寄 Email／LINE，不做真實金流，不下載 image／模型，不改其他倉庫／全域 Docker／proxy。原始日誌／fixture／截圖在 ignored work；Git 僅保存非敏感彙總與雜湊。

### 尚未完成事項

- 正式保留需求／法規與營運期限核定、去重 tombstone／稽核封存、匿名化、備份／復原證據與小批次人工清理。這輪沒有授權或執行既有通知刪除；不要以 180 天草案直接上自動排程。
- 大資料量查詢壓測、索引／門檻校準、封存保留／復原與競爭保護；worker 心跳／趨勢、外部告警、備份加密／離機副本／排程。
- 本機 PG18／使用者實際資料、MinIO、環境憑證與完整正式復原、SMTP／LINE／真正手機維持 [待補](../CLOUD_LOCAL_FOLLOWUP.md)。其他作品與搶票專案仍未修改。

### 已知問題

- 現行 notification_record.event_key 是唯一去重依據；刪掉它會失去事件去重。新版佇列／所有有鍵資料保護，不能以終態或備份完成為由刪除；需另外設計持久去重及稽核。
- 保守策略可能候選為零，這是保護結果，不是失敗。原因只取最先符合者，不顯示全部重疊理由；彙總與最後活動範圍不含可執行 ID。讀取是當下 snapshot，不能當日後清理憑證。
- 180 天是可設定草案，不是法定期限；本地 timestamp 沿用營區時間。全站查詢未大量壓測，不承諾吞吐量／保留 SLA。試算區與既有整體後台 header 的手機改善範圍不同，本輪只核對試算區不溢出。
- 沒有精確 context／usage 查詢介面；維持單一里程碑與收尾空間，不趁這輪擴大為實際清理或其他專案。

### 下一步建議

1. 下一輪重新 fetch main／讀本交接／核對 Git，先確認保留期限與哪些資料需要封存／稽核。可以先補封存／恢復的設計及驗證；不要直接拿試算計數批次刪。
2. 若後續明確授權低風險候選人工清理，獨立設計小批次、交易內重新核對全部條件與競爭／稽核／復原，保留新版佇列／去重鍵與 attempts；不預設自動排程。
3. 本機與真實服務維持文件待補，每里程碑測試／build、六項交接、commit／push main，最後核對乾淨且遠端一致。

## 2026-10-06：備份持久紀錄與 ADMIN 觀測（上一輪紀錄）

起始基準為遠端 main `9b2aa1b`；開始前已 fetch、讀取遠端交接、核對乾淨 main 並 ff-only 同步。本輪只修改 fadaiting-camp；實際 commit／遠端狀態以 git log 為準。

### 本次完成內容

- Python CLI 新增可選 `--journal`／`--source-key`，每次操作獨立 UUID 紀錄，原子置換與 fsync，running → success／failed。起始紀錄寫入失敗不開始操作；終態失敗保留實際產物，強制中斷不捏造成功。私有 0700／0600，拒絕符號連結／共享目錄；只存時間、來源識別碼、雜湊與固定錯誤碼，預設維持不紀錄。
- ADMIN 唯讀 `GET /admin/backup/status` 與後台「備份與還原觀測」，分開顯示最近成功備份、還原與最新嘗試。最新成功備份只有同 sourceKey、完整 archive SHA-256 相符的成功還原才算已驗證；其他還原不能代替。最近失敗仍保留先前成功歷史並顯示 critical，running 不代表進程存活。
- 未設定／無成功／損壞或無法完整讀取／超出容量不當健康；固定告警與可調備份／還原／未完成時間門檻。讀取嚴格格式、最多 2000 筆（可設定）、每檔 16 KiB，拒絕重複欄位／額外 JSON／不一致終態及未來時間超出五分鐘；後端不寫紀錄。
- dev／prod 預設未設定；新可選 Compose overlay 只掛既有私有紀錄目錄，read_only、create_host_path false，沒有 Docker socket。後台只看紀錄，不提供 HTTP 備份／還原／刪除入口。手機觀測區無橫向溢出，HTTP 失敗清除舊觀測。

### 重要修改檔案

新增 `scripts/backup_journal.py`、`test_backup_journal.py`，更新 `camp_backup.py`；新增後端 `BackupMonitorProperties`／`BackupMonitorStatusDto`／`BackupMonitorService`／`BackupMonitorController` 與服務測試，更新 `CampRolePolicy`／測試及 application.yml；新增前端 `api/backup.ts`、`views/operation/BackupObservation.vue`，更新 router／Sidebar。`.env.example`、dev／prod Compose 與 `docker-compose.backup-observation.yml`；[操作文件](../BACKUP_OBSERVABILITY.md)、[驗證彙總](verification/backup-observability-2026-10-06.json)，README／CONTEXT／AI／TODO／SCHEMA／新機設定／Cloud 待補／備份與通知觀測文件同步。

### 測試／build 結果

- Python 紀錄 **10 項**、備份工具 **17 項**（含 6 項真實 PostgreSQL 17 演練）全數通過，0 skipped。覆蓋失敗／中斷／原子保存失敗、已完成產物保留、私有權限、來源隔離與原有備份安全保護；17 項 suite 核對來源前後資料不變。首次權限測試受環境 umask 077 影響，已明確 chmod 0755 建立共享 fixture，重跑通過，沒有放寬正式檢查。
- 後端原 165 項＋12 項觀測服務＋1 項角色政策，**178 項通過，0 failure／error／skipped，Maven package BUILD SUCCESS**；通知／LINE 整合開關啟用，camp 專用 PG17／Redis。嚴格格式、上限、inclusive 門檻、未知狀態、雜湊／來源匹配、唯讀、失敗保留成功等皆通過。
- **真實 HTTP 15 項**：ADMIN 200／STAFF 403／匿名 401；CLI 備份成功後尚未驗證，故意對已存在備份輸出再執行會拒絕覆寫並保存 failed，成功歷史仍在；其後真實隔離 restore-check verified true，最新備份匹配且仍保留最近備份失敗告警。共三筆獨立紀錄；重複 GET 不改內容／mtime，不洩漏私有路徑；ADMIN POST 觀測／還原均 403。使用最後 package JAR 並核對執行檔 SHA-256，沒有使用舊展示後端代替驗收。
- 前端型別檢查及 **production build 成功**；正式 dist preview **15 項瀏覽器通過**，包含真實紀錄與失敗歷史、三指標、未設定／健康／尚未驗證的合成呈現、503 清除舊成功、恢復重載、390 px 觀測區、STAFF 側欄與直達拒絕、只有 GET、0 未捕捉 JS 錯誤。可選非本機字型請求在瀏覽器驗證中阻擋，無繞過 proxy 或外部 HTTPS 驗收宣稱。
- **Compose／唯讀掛載 5 項**：dev／prod 預設未設定與 overlay config，共四種；另用已存在 PG image、network none、read-only bind 實測能讀紀錄且 touch 被拒絕，測試容器自動移除。config 正規化會省略 false 欄位，核對其預設 false 與原始 overlay 明確設定。
- 本輪 backend 19091／正式 preview 15174 已停止，既有展示實例未停止；未建立新通知 fixture、未改帳號／測試帳密、不寄信、不做真實金流、不下載 image／模型、不修改其他作品或全域 Docker／proxy。原始紀錄／dump／manifest／日誌／截圖僅在 ignored work，Git 只保存非敏感彙總與雜湊。文件連結及 diff 收尾核對。

### 尚未完成事項

- 本機 PostgreSQL 18／使用者原有資料、MinIO、私有環境憑證、Redis 與正式整體服務復原；主機持久磁碟／容器擁有權及 Linux／WSL 以外權限實機驗收，見 [待補清單](../CLOUD_LOCAL_FOLLOWUP.md)。
- 排程、加密／離機副本、紀錄保留封存／簽章、備份目前是否可讀檢查、PITR、外部告警及容量／負載驗收。預設不自動刪紀錄，超出上限明示無法判定。
- 通知歷史保留需求與 dry-run／安全清理、worker 心跳／趨勢。ELPSY Shop 倉庫對應及搶票作品維持待辦，本輪沒有其他作品修改。

### 已知問題

- 歷史操作紀錄由可信管理者保存，沒有簽章；SHA-256 是內容／損壞核對，不能證明來源或 dump 目前仍存在、已加密／離機、完整平台可復原。sourceKey 需正確對應環境；相同內容可匹配舊驗證，但仍另計還原時間逾期。
- running 可能是中斷遺留，超時只是提醒，不代表進程活著或可以直接清理。沒有定時輪詢或自動啟動任務，刷新只讀紀錄；正式告警門檻／保留與大資料容量需另定。
- 前端原 simplebar 非 module、靜態／動態 import 與大 chunk 警告保留；既有整體後台手機 header 需另改善，本輪只驗證觀測區與卡片不溢出。PG18／實際手機／外部網址未重測。
- 沒有精確 context／usage 查詢介面；本輪維持單一里程碑，compaction 後重新 fetch 核對 main、HANDOFF、git 狀態／diff／log、實際檔案與失敗日誌，不依摘要判定成功。

### 下一步建議

1. 新階段先 fetch 遠端 main，讀本交接並核對 Git；先設計通知歷史保留需求，提供唯讀 dry-run 候選筆數／狀態／保護條件，禁止刪 pending／processing／uncertain 或稽核必要資料。清理另作獨立里程碑，不預設自動刪。
2. 備份排程／封存／加密與離機儲存另定範圍，避免把本輪唯讀觀測擴成公開 Docker 操作；本機完整驗收維持待補。
3. 每里程碑相關測試／build、六項交接、commit／push main，最後乾淨且遠端一致。

## 2026-10-06：通知營運觀測與後台告警（上一輪紀錄）

起始基準為遠端 main `7a3bd01`，開始前已 fetch、閱讀遠端交接、核對乾淨 main 並 ff-only 同步。僅修改 fadaiting-camp，這一個可驗證里程碑完成後提交／推送 main；確切 commit 以遠端與 git log 為準。

### 本次完成內容

- 沿用 ADMIN `GET /notification/queue`，以單次 Clock 與一條 PostgreSQL 聚合查詢補入結果不明／逾期 lease／等待過久／無法領取、最早時間、等待秒數、觀測時間及分級告警／處理建議。只計新版 managed Email，各模式合計；due 排除重試預算用盡，這類任務列 blocked。
- 後台通知頁有六項指標、全站範圍提示、手動更新與台北觀測時間。critical／warning／info 分級，API 失敗清除舊指標且不顯示假健康；列表可獨立載入，舊請求晚抵達不覆蓋新觀測。手機版觀測區無橫向溢出。
- 觀測唯讀，不 claim／回收 lease、不改通知狀態／版本、不新增寄送嘗試／寄信，也不自動啟用 worker。alerts 固定文字與彙總數字，不含收件地址、內文、payload、訂單或 lease UUID；沒有向外發送告警。
- 新門檻 `NOTIFICATION_OVERDUE_THRESHOLD_SECONDS=900`，支援 60–86,400；Spring、.env.example、dev／prod Compose 同步，沒有 migration 或清理排程。
- production preview 發現原本地 Bootstrap UMD 留下 `require`，使登入頁空白；改用既有 npm Bootstrap ESM 入口與小型 runtime bridge 保留 `window.bootstrap`，未新增依賴，正式登入、模板 tooltip 與觀測頁實測成功。

### 重要修改檔案

後端 `NotificationQueueProperties`、`NotificationQueueStatusDto`、新增 `NotificationQueueAlertDto`、`NotificationMapper`、`NotificationQueueService`、`application.yml`、`NotificationQueueIntegrationTest`；前端 `src/api/booking.ts`、`views/booking/NotificationList.vue`、`main.ts`、新增 `assets/js/bootstrap-runtime.js`；`.env.example`、dev／prod Compose。[操作文件](../NOTIFICATION_OBSERVABILITY.md)、[驗證摘要](verification/notification-observability-2026-10-06.json)、本交接與 README／CONTEXT／AI／TODO／SCHEMA／新機設定／Cloud 待補／通知佇列文件同步。

### 測試／build 結果

- **本輪後端 165 項通過，0 failure／error／skipped，Maven package BUILD SUCCESS**：原 161 項＋4 項通知觀測真實 PostgreSQL 測試；LINE／通知整合開關啟用，使用 camp 專用 PostgreSQL 17／Redis。覆蓋通道／舊紀錄／終態排除、指標重疊、門檻／租約相等邊界、可調門檻、worker 狀態、空佇列、唯讀／無寄送與無個資回應。
- **實際 HTTP 17 項通過**：ADMIN 200、STAFF 403、匿名 401，八項計數 delta、六種告警、設定／等待時間、無個資、GET 不改通知資料及不新增嘗試。合成通知包含 SMTP 結果不明／租約過期，但沒有真正呼叫 SMTP。
- **正式 build 預覽瀏覽器共 14 項通過**：首輪 12 項涵蓋登入與 Bootstrap tooltip、六指標／人工核對、全站與列表範圍、503 清除舊觀測而列表保留、恢復後重載、舊請求不能覆蓋、390 px 手機觀測區、STAFF 前端拒絕與 0 pageerror；最後另補列表 503／恢復兩項，列表錯誤明示未載入，不當成零筆且觀測獨立可用。
- **前端型別檢查與 production build 成功**，最後重建包含 Bootstrap bridge 與列表失敗處理；既有 simplebar 非 module、靜態／動態 import 與大 chunk 警告仍在。本輪沒有以 dev server 測試代替正式產物預覽；最後補驗阻擋非本機的可選外部字型請求，避免其載入逾時，沒有繞過 proxy／政策或宣稱外部 HTTPS 驗收。
- dev／prod Compose `config --quiet` 通過；新增文件連結與 `git diff --check` 收尾核對。原始日誌／截圖／私有帳密都不提交，Git 僅收錄彙總與 SHA-256。
- 本輪 fixture 預約及八筆通知已刪除，本輪 19091 backend／15174 preview 已停止；既有展示實例未停止。不修改登入帳號／測試帳密、其他作品、proxy 或全域 Docker 設定。

### 尚未完成事項

- 備份最近成功／最近成功還原的持久紀錄與觀測仍待下一個小里程碑；上一輪備份工具及成功證據保留。
- 通知保留需求／安全清理、worker 心跳／趨勢／吞吐量、外部告警與確認／抑制；本輪不自動刪歷史或發外部通知。
- 本機 PostgreSQL 18／MinIO／環境憑證／完整恢復服務、實際 SMTP／LINE／手機、金流 sandbox 維持 [待補清單](../CLOUD_LOCAL_FOLLOWUP.md)。ELPSY Shop 倉庫對應及新搶票作品仍未修改。

### 已知問題

- 健康值代表觀測當下的佇列規則，不證明 SMTP 可用、Email 送達或 worker 活著。逾期 processing 在既有批次回收前計 lease 到期，回收後 SMTP 結果不明改計 failed／uncertain；GET 不推進狀態。
- 指標重疊且跨 delivery mode；某模式的任務仍可能不被當前 mode 領取，沒有自動切換。大規模資料的聚合查詢／告警門檻需另壓測與校準，沒有吞吐量或送達 SLA 承諾。
- 讀取依人工更新／列表操作，沒有定時輪詢、持久告警事件或外部送達；頁面載入失敗明示無法判定，不能將上次數字當最新健康。
- 沒有精確 context／usage 查詢工具，本輪以單一里程碑保留測試／交接／推送空間，不擴大到下一個備份功能。

### 下一步建議

1. 從遠端 main／本交接／Git 實況開始，先做備份結果的持久紀錄與最近成功時間觀測；備份和還原成功分開紀錄，不能把已有 dump 當已還原成功。
2. 再定通知歷史保留期限與可刪範圍，先提供 dry-run／數量／保護條件，避免刪掉 pending／processing／uncertain 或稽核需要的資料；作為另一個獨立驗證里程碑。
3. 本機／帳號相關驗收維持待補；每里程碑跑相關測試／build、六項交接、commit／push main，最後核對乾淨且遠端一致。

## 2026-10-06：PostgreSQL 備份與隔離還原（上一輪紀錄）

起始基準為遠端 main `6090345`；僅操作 fadaiting-camp。新增運維工具及文件，沒有商業程式、API 或 migration 變更。此為一個可獨立驗證的里程碑，測試／build 後提交 main；最終提交識別碼以 git log／遠端 main 為準。

### 本次完成內容

- Python 標準函式庫＋Docker CLI 備份工具：來源 container 內 `pg_dump -Fc`，唯讀 exported MVCC snapshot 讓逐表指紋與 dump 一致；二進位存檔、SHA-256、目錄 0700／檔案 0600，既有備份不覆蓋。
- 還原只使用本地來源 image ID 自建全新容器，無網路／無公開 port／無主機掛載，512 MB tmpfs；同 major、單一交易 pg_restore，核對資料與欄位／CHECK／外鍵／索引／view／sequence 定義。失敗清理，`--keep` 只保留成功實例，清理須精確名稱＋owner label。
- 實測來源 PostgreSQL 17 的 28 張表／2,447 筆資料／41 個外鍵／2 個 view／26 個 sequence；備份與演練期間來源資料及結構一致。合成中文／emoji 預約、付款、退款與通知 fixture 僅寫入隔離副本，不新增或修改登入帳號，不寄信、不執行真實金流。
- 加入損壞 dump、錯誤指紋、來源保護、失敗清理及併發寫入快照測試，提供 Linux／WSL 操作文件。本輪未使用付費服務、下載模型、修改其他倉庫或全域 Docker／proxy 設定。

### 重要修改檔案

[scripts/camp_backup.py](../scripts/camp_backup.py)、[scripts/test_camp_backup.py](../scripts/test_camp_backup.py)、[BACKUP_RESTORE.md](../BACKUP_RESTORE.md)、[驗證摘要](verification/backup-restore-2026-10-06.json)、`.gitignore`、本交接、`README.md`、`PROJECT_CONTEXT.md`、`README_AI.md`、`TODO_AI.md`、`SCHEMA.md`、`CLOUD_LOCAL_FOLLOWUP.md`、`NEW_MACHINE_SETUP.md`。

### 測試／build 結果

- 本輪完整 Python 工具測試 **17 項通過，0 failure／error／skipped**：11 項保護＋6 項真實 PostgreSQL 演練，包含中文／關聯／序號後續新增 ID、損壞資料建容器前攔截、重新算 checksum 的截斷 dump、指紋不符、既有輸出保留及 concurrent committed write 不混入 snapshot。
- 正式 CLI 路徑完成一次來源 backup → 新容器 restore-check，`verified: true`；前述 28／2,447／41／2／26 全數核對，成功後自動清理。完整 suite 另核對來源演練前後相同。
- 後端 Maven package：指定 20 組角色／住宿／付款／退款／LINE／通知回歸，整合開關啟用，**161 項通過，0 failure／error／skipped，BUILD SUCCESS**；使用 camp 專用 PostgreSQL／Redis。
- 前端 `npm --prefix frontend run build`：型別檢查及 production build 成功。原有 simplebar 非 module、靜態／動態 import 及大 chunk 警告仍在，沒有新增 UI 變更。
- 初次 pg_restore 遇到預設 public schema 已存在，改為只在全新還原容器 `--clean --if-exists`；之後 PostgreSQL 17 的純 varchar 常值陣列 cast 重寫造成結構字串差異，加入精確等價正規化與回歸後，最新完整測試及 CLI 皆成功。沒有以忽略整個 CHECK／索引差異來繞過核對。
- 原始日誌、dump／manifest 留於被 Git 忽略的私有 work；Git 僅收錄不含個資／帳密的彙總及雜湊。瀏覽器／外部 HTTPS／手機與真實服務送達未重測。

### 尚未完成事項

- 本機 PostgreSQL 18／實際舊資料、MinIO 圖片、環境憑證、Redis 登入狀態與正式恢復服務，見 [待補清單](../CLOUD_LOCAL_FOLLOWUP.md)；Compose 仍使用 18，不把 Cloud 17 演練標成本機通過。
- 自動排程、備份加密／離機副本、保留策略、較大資料容量、不同 major 升級、WAL／PITR、production RPO／RTO 及正式切換。
- ELPSY Shop 正式倉庫對應與新搶票專案維持前輪待辦，本輪沒有修改其他作品。

### 已知問題

- 工具只涵蓋 public 邏輯備份，不保留 cluster role／owner／ACL 或其他 schema；現有 public 無自訂函式／trigger，未測 extension 依賴、materialized view 或 RLS 的完整結構核對。512 MB tmpfs／預設 300 秒適用目前展示資料，不能套用為大型正式庫承諾。
- SHA-256 是損壞偵測而非來源簽章，dump 未加密；只使用可信自產備份。跨機器需另準備相同可信 image ID。序號不受 MVCC 保護，並行寫入可能有合法 gaps，精確 counter 核對需先停寫。
- 進程被強制終止時可能留下臨時容器，操作文件提供按 label／名稱核對自己的 container 再清理；不得刪來源 volume 或其他專案。還原後通知須先人工核對寄送狀態，禁止直接接真實 worker 重送。
- 沒有可用的精確剩餘 usage 查詢介面；本輪按單一里程碑保留收尾空間，compaction 後已重新核對遠端、HANDOFF、Git 狀態、原始檔與實際測試結果。

### 下一步建議

1. 下一輪重新 fetch 遠端 main、讀本文件、核對 Git 狀態；不要依舊 workspace 推定最新。
2. Cloud 優先補營運觀測：通知待送／失敗／結果不明／逾期 lease、備份最近成功狀態與非敏感告警資料；通知保留／清理另做小里程碑，先定資料保留需求。
3. 到可用本機環境再以可信副本驗收 PostgreSQL 18、MinIO 與完整復原流程；不要用本工具直接覆寫現有 DB。每階段測試／build、六項交接、commit／push main、乾淨與遠端核對。

## 2026-10-06：工作階段與 context 安全（上一輪紀錄）

起始基準為遠端 main `9b1b474`；只更新規範與交接，沒有商業程式或 migration 變更。

### 本次完成內容

- 四個 workspace 倉庫的 AGENTS 與平台文件加入一致的八項規則：主動檢查可用 context／token／usage、預留收尾空間、額度可能不足時停止新增大型修改、優先測試／build 及交接／commit／push、每個里程碑立即保存、compaction 後重建實際狀態、最後核對乾淨及遠端 main。
- 沒有精確用量介面時採保守分段，不捏造剩餘數字；規則適用整個展示平台及後續 Web／後端／App／搶票專案。

### 重要修改檔案

[AGENTS.md](../AGENTS.md)、本交接、[驗證摘要](verification/context-safety-2026-10-06.json)；全文見 [平台規範](https://github.com/jackteng-dev/developer-portfolio/blob/main/docs/PLATFORM_WORKFLOW.md)。

### 測試／build 結果

- 本輪前端型別檢查與 production build 成功（npm --prefix frontend run build）。
- 本輪後端 161 項通過，0 failure／error／skipped，build 成功。Maven package；指定 20 組回歸、LINE／通知整合開關啟用，使用 camp 隔離 PostgreSQL／Redis。
- 八項要求完整，四份 AGENTS 與平台文件內容一致；交接六項、新增連結及 git diff --check 核對通過。原始日誌留在 workspace 暫存，Git 僅收錄非敏感摘要與雜湊。
- 瀏覽器／外部 HTTPS／真實模型品質未重跑，前輪數字維持歷史條件。

### 尚未完成事項

- 原功能與本機／設備／帳號待補工作維持前輪紀錄；本輪沒有新增功能。
- ELPSY Shop 正式倉庫來源待確認，新搶票倉庫尚未建立；規範適用，但不宣稱已修改那些倉庫。

### 已知問題

- 目前沒有可用的精確剩餘 context／帳戶 usage 查詢工具；保守保留收尾空間，不宣稱已讀取實際剩餘值。
- 測試／build／push 失敗時如實記錄並保留安全成果，不標為成功、不 force push；各作品既有功能限制仍見前輪紀錄。

### 下一步建議

1. 新階段重新核對遠端 main／交接／Git 狀態；每個可驗證里程碑完成就保存，避免累積到大型任務最後。
2. 優先接續 PostgreSQL 備份／隔離還原演練；本輪未實作，不連使用者本機資料。
3. 發生 compaction 或資訊不明時，重讀原始檔、status、diff（含 staged）、log、HANDOFF 與遠端後才續作；收尾確認 status 乾淨、main 已包含最新 commit。

## 2026-10-06：平台共同流程（上一輪紀錄）

起始遠端 main：`44964d0`；本輪驗證的程式基準：`44964d0`。完成後由各倉庫 main 提交並推送；確切提交以遠端 main／git log 為準。平台規範見 [共同工作流程](https://github.com/jackteng-dev/developer-portfolio/blob/main/docs/PLATFORM_WORKFLOW.md)。

### 本次完成內容

- 使用者指定的九步流程適用於整個平台，各倉庫各自同步 main、讀交接、核對狀態、測試／build、更新交接、commit／push；新 Web／後端／App 與搶票作品亦適用。
- 將上一輪發呆亭專用流程擴充為展示平台所有作品共用規範，連結平台入口。維持先前整合至 main 的員工權限、住宿、付款期限、退款、安全與通知成果；沒有改商業程式或 migration。
- 一般單一作品工作仍限於該作品；本輪需求明確涉及平台流程，才同步更新四個 workspace 倉庫。未修改其他 GitHub 倉庫、寄信、執行金流或下載模型。

### 重要修改檔案

[AGENTS.md](../AGENTS.md)、[README.md](../README.md)、[PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md)、[README_AI.md](../README_AI.md)、[TODO_AI.md](../TODO_AI.md)、`docs/HANDOFF.md`、[驗證紀錄](verification/platform-workflow-2026-10-06.json)。

### 測試／build 結果

- **本輪實測**：前端型別檢查與 production build（`npm --prefix frontend run build`）成功。
- **本輪實測**：後端 161 項，0 failure／error／skipped，build 成功。Maven package；指定 20 組回歸，LINE／通知整合開關啟用，使用 camp 隔離 PostgreSQL／Redis。
- 交接六項內容、共用規範與 `git diff --check` 已核對；原始日誌留在 workspace 暫存，Git 僅收錄不含帳密的摘要與雜湊。
- 舊功能、瀏覽器／HTTPS／真實模型與效能數字保留為歷史條件，本輪未重跑或改寫其結論。

### 尚未完成事項

- 備份／隔離還原腳本與演練、營運觀測及通知保留策略可在 Cloud 續做；本機舊資料升級、MinIO、SMTP／LINE／手機與商家 sandbox 仍待補，見 [待補清單](../CLOUD_LOCAL_FOLLOWUP.md)。
- 平台流程對 ELPSY Shop／App／新搶票專案同樣有效；缺少正式倉庫對應或尚未建立的項目不宣稱已修改。

### 已知問題

- 前端仍有 simplebar 非 module、靜態／動態 import 與大 chunk 警告。舊 JWT 需重登入，舊 LINE 綁定需重新核驗。通知模擬不代表寄達，SMTP 結果不明需人工核對；真實寄信／worker 預設關閉。
- 分支保護或檢查失敗時記錄真實阻礙，不 force push；既有 master 倉庫先盤點 main／部署依賴，不自行改名。

### 下一步建議

1. 重新 fetch 本倉庫遠端 main，先讀該版本本文件，再核對 status、branch 與近期 commit；正常後 fast-forward 同步再修改。
2. 優先做 container 內 pg_dump -Fc 與獨立還原 DB 的 pg_restore 演練，核對中文、筆數與預約／付款／退款／通知關聯；不要覆蓋來源或刪 volume。
3. 階段完成後跑相應測試與 production build，成功後更新六項交接，commit／push main，再確認遠端一致與沒有重要未提交修改。

## 先前里程碑紀錄（歷史）

更新日期：2026-10-06。此文件是最新交接入口，根目錄 `HANDOFF.md` 保留歷史。每輪以遠端 `origin/main` 為準；固定流程見 [AGENTS.md](../AGENTS.md)。

## 本次完成內容

- 將使用者指定的九步流程寫入 AGENTS：同步遠端 main、讀交接、核對工作目錄、測試與 production build、更新交接、commit 並 push main，最後確認同步與乾淨狀態。
- 原遠端 main 為 `46cc8be`；將已測試的功能分支 `codex/camp-operational-roles`（`c055d49`）以 fast-forward 整合至本地 main，本輪交接變更亦由 main 提交並發布。完整提交識別碼以遠端 main 與 git log 為準。
- 建立本文件並更新既有文件入口與新電腦分支說明。本輪只改工作規範、交接與驗證文件，沒有修改商業程式或新增 migration。
- main 基準包含固定 ADMIN／STAFF 權限及員工帳號、入住／退房、未付款期限與庫存釋放、退款審核／人工對帳、Cookie／CSRF／LINE ID token 身分核驗、持久通知佇列與重試。詳細操作見下方文件。
- 本輪只操作發呆亭主服務；沒有修改獨立 LINE 專案或其他專案，沒有寄送 Email／LINE 或執行真實金流。

## 重要修改檔案

- [AGENTS.md](../AGENTS.md)：永久工作流程與交接要求。
- `docs/HANDOFF.md`：本輪狀態、限制與下一步。
- [README.md](../README.md)、[PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md)、[README_AI.md](../README_AI.md)、[TODO_AI.md](../TODO_AI.md)：指向最新交接與 main 基準。
- [歷史交接](../HANDOFF.md)：保留既有紀錄並指向本文件。
- [NEW_MACHINE_SETUP.md](../NEW_MACHINE_SETUP.md)：改以遠端 main 接手，更新新對話啟動提示。
- [本輪驗證摘要](verification/main-handoff-2026-10-06.json)：測試／build 結果與本機日誌雜湊，不含帳密或原始敏感日誌。

## 測試／build 結果

- **本輪實測**：後端 Maven `package` 成功；指定的 20 組角色、員工、住宿、庫存、付款、退款、JWT、CSRF、LINE 核驗、通知測試共 **161 項，0 failure、0 error、0 skipped**。LINE 與通知整合測試開關均開啟，使用本專案隔離 PostgreSQL／Redis 與合成外部身分資料。
- **本輪實測**：前端 `npm run build` 成功，含 `vue-tsc -b` 與 Vite production build。
- 前端仍有既有 simplebar 非 module script、混合靜態／動態 import 與大 chunk 警告；本輪沒有處理這些警告。
- **歷史驗證**：`c055d49` 通知階段的 HTTP 39 項、瀏覽器 13 項及畫面核對見 [通知驗證](verification/notification-queue-2026-10-06.json)；本輪文件變更沒有重跑該組 HTTP／瀏覽器案例，不宣稱為本輪實測。

## 尚未完成事項

- Cloud 可續做：PostgreSQL 備份／隔離還原腳本與演練，之後再做營運觀測與通知保留策略。
- 使用者本機實際資料庫與 MinIO 備份、舊資料升級、HTTPS 部署及多專案同機驗收仍暫緩。既有資料庫須先備份，再補缺少的 `002` 至 `008` migration，不刪 volume。
- 真實 SMTP／LINE／LIFF 手機與商家 sandbox、真實退款憑證、獨立 LINE 呼叫方改送 ID token 尚未驗收。完整條件見 [CLOUD_LOCAL_FOLLOWUP.md](../CLOUD_LOCAL_FOLLOWUP.md)。

## 已知問題與限制

- 升級後舊 JWT 需重新登入；舊 LINE 綁定沒有核驗時間，不自動授權，需重新綁定。未設定 LINE Login Channel 時身分接口回 503。
- 真實寄信與通知 worker 預設關閉，模擬不代表送達。SMTP 結果不明任務需人工核對，不能保證外部寄信 exactly-once。
- Cloud 測試不是正式商家、手機、使用者既有資料或最終部署驗收；本輪沒有重建或測試公開 Tunnel，不提供未核驗網址。
- 根目錄舊交接與歷史報告可能保留功能分支或舊驗證數字，作為歷史資料；本輪最新狀態以本文件與遠端 main 為準。
- 本輪重要修改均納入提交；帳密、工具、建置產物與原始測試日誌留在忽略路徑，不列為未提交的程式工作。

## 下一步建議

1. 重新 fetch 遠端 main，先讀該版本的本文件，再核對 status、branch 與最近 commits，確認後同步 main 才開始。
2. 優先做免費的備份／還原演練：container 內 `pg_dump -Fc`，另建隔離還原 DB 執行 `pg_restore`，不得覆蓋來源或其他專案資料。
3. 核對中文內容、資料筆數、主外鍵、預約／付款／退款／通知關聯，測試失敗與還原流程，再執行 production build、更新本文件並 commit／push main。
4. 需要實際電腦、帳號或付費方案的工作，保留在後續清單；繼續優先採免費方式。

## 功能文件

- [員工權限](../CAMP_PERMISSIONS.md)／[住宿操作](../BOOKING_STAYS.md)
- [付款期限](../BOOKING_HOLDS.md)／[退款與對帳](../PAYMENT_REFUNDS.md)
- [Cookie／CSRF 與 LINE](../AUTH_LINE_SECURITY.md)／[通知佇列](../NOTIFICATION_QUEUE.md)
