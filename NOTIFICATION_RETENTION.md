# 通知歷史保留與唯讀試算

ADMIN 在「預約管理 → 通知紀錄」上方查看「通知歷史保留試算」，按更新取得全站彙總。這一輪只提供 dry-run，沒有刪除 API、排程、清理按鈕或匯出可執行的通知 ID。候選是供營運評估的低風險範圍，不代表已核定正式保留／刪除政策。

## 草案門檻與時間

`NOTIFICATION_RETENTION_PREVIEW_DAYS` 預設 **180 天**，有效 30–3650；Spring、.env.example、dev／prod Compose 同步。只能由部署設定變更，不接受請求覆寫。這是免費的既有 PostgreSQL 查詢與後台呈現，不使用外部儲存或付費服務。

用單一 Clock 取得 observedAt，按營區時區 `Asia/Taipei` 計算 `cutoffAt`。既有無時區的 notification／booking timestamp 沿用營區本地時間，不依 PostgreSQL session timezone 隱含轉換。每筆最後活動取通知建立／修改／sent_time、訂單修改與預定退房日零時的最大值，只有**嚴格早於**截止才可能成為候選；相等仍保留。期限代表最近相關活動，不僅是通知建立日。

## 保護與候選規則

單次 SQL snapshot 讀全站**所有通道／處理模式／新版與舊版**，不受通知列表的篩選／分頁影響。每筆只歸入最先符合的原因，互斥且全部涵蓋，所以 `totalCount = candidateCount + protectedCount`，理由筆數相加等於全部。

| 優先順序／reason | 保護條件 |
| --- | --- |
| 1 `DELIVERY_UNCERTAIN` | 通知或任何歷史嘗試含結果不明代碼；即使後來略過也保留 |
| 2 `UNRESOLVED_STATUS` | 非 simulated／skipped／acknowledged／sent；含 pending、queued、processing、retry_wait、failed、lease_expired 及未知狀態 |
| 3 `QUEUE_IDENTITY` | queue_managed 或任何非 null event_key，保留新版佇列及事件去重身分 |
| 4 `ATTEMPT_HISTORY` | 任何發送嘗試，或 attempt_count > 0；歷程缺失也不能當作未寄送 |
| 5 `DELIVERY_AUDIT` | Email／其他非站內人工通道、sent、SMTP、非白名單 provider、仍有錯誤代碼／訊息 |
| 6 `FINANCIAL_AUDIT` | 非 booking_submitted／admin_new_booking／departure_reminder 的事件、訂單有非零已收款或任何付款紀錄（含 pending／mock）；付款／退款稽核保留 |
| 7 `BOOKING_OPEN` | 訂單缺失、非 cancelled／completed 或尚在入住中 |
| 8 `SCHEDULE_OR_LEASE` | 任何 next_attempt_at／lease_token／lease_until，包含過期及不一致殘留 |
| 9 `RECENT_ACTIVITY` | 必要時間不完整，或最後活動 >= cutoffAt |
| 10 `CANDIDATE` | 以上都不符合的舊站內／人工 simulated／skipped／acknowledged |

provider 白名單為 system／internal／manual，未知供應商保留。舊資料沒有 event_key 才可能進入候選，並不代表全部舊資料可刪。不同條件會重疊，但只回傳最優先的保留原因，並非各原因的獨立全集。

**去重與稽核是獨立保護條件。** 現行唯一 event_key 存在 notification_record；刪掉它可能重新產生同一事件。若未來要清理這類通知，需先設計獨立且持久的去重保留及歷程封存，不能靠本試算直接刪除。候選目前沒有 attempts，但訂單 timeline、operation log、付款／退款與備份等都不在清理範圍。

## API、權限與畫面

`GET /api/notification/retention-preview` 只授權 ADMIN。STAFF 403，匿名 401；POST 試算、DELETE 清理路徑不授權。沿用現有角色政策，不更改帳號或細粒度權限資料。

回應包含 `policyVersion=conservative-preview-v1`、`dryRun=true`、`deletionEnabled=false`、觀測／截止／時區／門檻、總計／候選／保護數量與 `buckets`。每個 bucket 只有固定 reason、數量、終態細分類及最後活動範圍，沒有通知／訂單 ID、收件資料、訊息、payload、去重鍵、lease UUID 或原始例外。

讀取服務使用 readOnly transaction；一條 CTE／聚合查詢，不鎖列、不 claim／回收租約、不寫版本／嘗試／歷程，不寄信。畫面獨立載入／手動更新；列表失敗不妨礙試算，試算 HTTP 失敗清除舊結果並顯示無法判定，不把失敗當零筆。較舊請求不能覆蓋新畫面結果。

## 驗證、限制與下一階段

本輪 Cloud 使用本專案 PG17／Redis，真實資料庫測試涵蓋候選／保護理由、結果不明歷史、去重、缺失嘗試歷程、金流、未知事件／provider、截止相等、訂單活動與退房、唯讀與不含個資。另做登入權限 HTTP、正式 dist 預覽與手機觀測區驗證，詳見 [交接](docs/HANDOFF.md)及 [結果彙總](docs/verification/notification-retention-2026-10-06.json)。未替使用者本機 PG18／正式資料核定期限，也沒有執行清理。

結果只代表觀測當下的資料，不能當成下一次執行的刪除憑證。全站聚合尚未做大量資料壓測；正式使用前依資料規模校準查詢、門檻與保留需求。本輪沒有新增 migration／索引。

下一階段先確認營運／法規保留需求、備份與恢復證據、人工封存／稽核範圍。若決定實作清理，需另外設計小批次、交易內重新核對條件、競爭保護、稽核／回復與明確操作授權；不直接照舊快照 ID 或總數刪除。排程、自動刪除、去重 tombstone／封存及個資匿名化皆未實作。
