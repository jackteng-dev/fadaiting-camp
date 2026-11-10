# 發呆亭目前狀態與後續待辦

## 2026-10-29：SeaweedFS 替換分支（正式主機尚未切換）

使用者指定以 SeaweedFS 取代 Camp MinIO。產品 S3 SDK、全新獨立 Volume、同源圖片路徑與真服務測試見 [SEAWEEDFS_MIGRATION](SEAWEEDFS_MIGRATION.md)。不改 SQL／migration／帳號，不將舊 MinIO volume 交給 SeaweedFS。中央 v4 是既有主機部署契約；目前正式來源仍為 `1d9b8d100654fd2ab9d0a32afbcc541c6bf5c2cd` 的已驗收快照，不以 Cloud 測試宣稱已搬遷。

儲存服務、設定 fence、發布 recipe 與圖片 URL／資料搬遷需要主機管理者另審；本輪用獨立 PR 交付，不直接合 main 或部署。以下 MinIO／舊部署描述保留為歷史；最新替換範圍與未完成主機驗收以連結文件為準。


更新：2026-10-06。最新階段與驗證結果以 [docs/HANDOFF.md](docs/HANDOFF.md) 為準。此文件依目前程式與已完成驗證整理，移除未具統一驗收基準的百分比；歷史提交留在 Git，不把已實作功能列成未完成。

## 階段停點

發呆亭目前收斂為**可展示、可在隔離環境試跑的營區預約與營運案例**。本輪完成展示收尾後暫停新增功能，切換 ELPSY Shop；正式營運需要的帳號／主機／資料／政策驗收保留在 [CLOUD_LOCAL_FOLLOWUP.md](CLOUD_LOCAL_FOLLOWUP.md)。不宣稱真實付款／送達／整體復原已完成。

展示順序與準備見 [DEMO_WALKTHROUGH.md](DEMO_WALKTHROUGH.md)。每次續作仍須 fetch 遠端 main、讀最新交接、核對 Git 狀態；相關測試與 production build 後更新交接、commit／push main。

## 已完成的展示範圍

| 模組 | 已完成 | 重要界限 |
| --- | --- | --- |
| 前台官網 | 首頁、營位、地圖、消息、FAQ、預約、查單、須知／退款／隱私政策與 404 | 圖片／文案仍有示意內容；需要最終營運核定 |
| 預約與庫存 | 即時報價、逐晚可售、租借品、送單、查詢、取消／重開、付款期限與逾時釋放 | 實際營運容量／政策尚待本機驗收 |
| 員工與身分 | 固定 ADMIN／STAFF、員工管理、最後管理員保護、停用撤權即失效、Cookie／CSRF／refresh | 本機部署金鑰／HTTPS／角色移轉需驗收 |
| 住宿 | 入住／退房、逾期住宿、名單／日曆／CSV、版本與交易保護 | 已完成程式／Cloud 驗證，實際值班流程另驗收 |
| 付款與退款 | mock 付款、人工補登、晚到款核對、退款申請／核准／模擬／人工完成、金額／重複／競爭保護 | 模擬不是扣款；人工完成不是銀行轉帳，商家查單／自動退款／正式授權仍待補 |
| 通知 | durable outbox、事件去重、租約、退避重試、嘗試歷程、人工處理與免費模擬 | worker／真實寄信預設關閉；不等於 Email／LINE 送達 |
| 營運觀測 | 通知全站指標／分級告警、API 故障明示、ADMIN 備份觀測 | 沒有外部告警或 worker 心跳證明 |
| 備份／還原 | 私有 PG public dump、同快照指紋、無網路隔離核對、成功／失敗持久紀錄 | Cloud PG17 已驗證；PG18／MinIO／全服務復原未驗證 |
| 保留試算 | 全站唯讀 dry-run、候選／保護原因、180 天可調草案 | 沒有刪除／排程，草案不是核定政策 |
| 開發交接 | AGENTS 固定流程、各階段測試／build／非敏感證據、主機待補文件 | 每次以遠端 main 為準 |

## 六類尚未完成工作

以下全部延期，不阻擋切換下一個展示作品。開始其中一項前再定本輪範圍，避免把「最後收尾」延長成新的大型功能。

### 1. 真實金流與旅客授權

- [ ] 綠界／LINE Pay 商家 sandbox、HTTPS callback／導回／查單與真實退款驗收。
- [ ] 正式旅客付款授權與最終訂金／尾款、取消／退款政策核定。
- [ ] 自動商家退款及完整銀行／商家對帳；現有人工記錄不能當轉帳證明。

### 2. 通知送達與 LINE 整合

- [ ] 本機 Mailpit／免費 SMTP transport 與真實送達、TLS／額度／退信。
- [ ] LINE Login Channel／LIFF／手機 ID token、Rich Menu／Flex／推播實測。
- [ ] 獨立 fadaiting-line 呼叫方改送 ID token、重新綁定與 401／503 處理；不恢復自報 userId 授權。
- [ ] 額外通知事件依營運需求補齊；已完成事件的入列不要重做。

### 3. 本機部署與完整復原

- [ ] 使用者舊 DB 非破壞升級／角色移轉，實際 PG18 與容量驗收。
- [ ] HTTPS／Secure cookie、私有金鑰／憑證、各作品 port／資源隔離與啟停 SOP。
- [ ] MinIO 圖片、環境設定、Redis 登入狀態與整體服務復原演練。

### 4. 營運報表

- [ ] 淨營收／退款統計、入住率／付款率、訂單與付款匯出整合。
- [ ] 庫存日曆與實際值班流程驗收；現有住宿日曆／名單／CSV 已完成。

### 5. 前台內容與 UX

- [ ] 真實營區照片、正式文案／條款／SEO／OG／metadata 核定與補齊。
- [ ] 整體手機 header、後台模板與靜態資產效能；局部觀測區驗證不代表全站手機完成。
- [ ] simplebar 非 module、混合 import／大 chunk 與載入效能整理。

### 6. 長期營運與維護

- [ ] 核定通知保留需求、去重 tombstone／稽核封存／匿名化／復原後才評估人工小批次清理。
- [ ] 備份排程／加密／離機副本／封存／簽章、PITR／較大容量驗收。
- [ ] worker 心跳／趨勢、外部告警與大資料壓測；不從 Cloud 小資料測試推算 SLA。
- [ ] OpenAPI／API 文件中心、migration 工具及舊模板／模組整理。已有 integration／HTTP／瀏覽器驗證，不再列為「缺少 integration／e2e」。

## 接手入口

- [最新交接與本輪驗證](docs/HANDOFF.md)
- [展示操作](DEMO_WALKTHROUGH.md)
- [需本機／帳號待補清單](CLOUD_LOCAL_FOLLOWUP.md)
- [權限](CAMP_PERMISSIONS.md)、[住宿](BOOKING_STAYS.md)、[付款期限](BOOKING_HOLDS.md)、[退款](PAYMENT_REFUNDS.md)
- [通知佇列](NOTIFICATION_QUEUE.md)、[通知觀測](NOTIFICATION_OBSERVABILITY.md)、[保留試算](NOTIFICATION_RETENTION.md)
- [備份／還原](BACKUP_RESTORE.md)、[備份觀測](BACKUP_OBSERVABILITY.md)、[身分安全](AUTH_LINE_SECURITY.md)

下一個作品依正確倉庫與遠端交接開始；日日選物 e-commerce-platform 與 ELPSY Shop 分開，不能自行互換。ELPSY Shop 的正式來源若未確認，先完成唯讀盤點，不修改候選倉庫或自行改 master。
