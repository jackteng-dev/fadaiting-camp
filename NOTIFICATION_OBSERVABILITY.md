# 通知佇列營運觀測

## 通知歷史保留試算

ADMIN 通知頁新增全站唯讀 dry-run，去重／佇列／寄送／金流與未結案資料保護。180 天為草案，沒有刪除或排程。詳見 [NOTIFICATION_RETENTION.md](NOTIFICATION_RETENTION.md)。

ADMIN 在「預約管理 → 通知紀錄」查看全站佇列指標、觀測時間與處理建議，按「更新觀測」重新取得當前狀態。列表的訂單、通道、事件與狀態篩選只影響下方紀錄，不會縮小全站告警範圍。使用 PostgreSQL 彙總與既有後台，沒有新增付費服務。

觀測是唯讀：不 claim 任務、不回收 lease、不改狀態／版本、不新增寄送嘗試，也不寄 Email／LINE。告警顯示在後台，沒有另外向外傳送告警或背景輪詢。worker 和真實寄信維持預設關閉；SMTP 模式的標示也不代表設定正確或已送達。

## 指標與告警

所有指標只計 `queue_managed=true` 且 `channel=email` 的紀錄。舊通知、站內提醒、manual 通道排除；各 delivery mode 合計，任務仍依既有 mode 規則領取，不會自動切換。

| API 欄位 | 含義 |
| --- | --- |
| `dueCount` | queued／retry_wait 且 next_attempt_at 已到期、仍有重試預算 |
| `waitingCount` | retry_wait 的下次時間尚未到、仍有重試預算 |
| `processingCount` | processing，包含已過期或資料不完整的租約 |
| `failedCount` | failed，包含寄送結果不明 |
| `uncertainCount` | failed 且 error_code=DELIVERY_UNCERTAIN，需要先人工核對 |
| `expiredLeaseCount` | processing 且 lease_until 已到期，尚未經既有批次回收 |
| `overdueCount` | due 中 next_attempt_at 已比告警門檻早或相等 |
| `blockedCount` | queued／retry_wait 缺 next_attempt_at 或預算用盡；processing 缺 lease_until／lease_token |
| `oldestDueAt`／`oldestDueAgeSeconds` | 有預算的到期任務最早時間與已等待秒數；沒有任務時時間 null、秒數 0 |
| `oldestExpiredLeaseAt` | 最早已到期 lease，沒有時為 null |
| `observedAt`／`overdueThresholdSeconds` | 本次觀測時間與實際使用的門檻 |

指標有重疊，不能相加當作任務總數。`dueCount` 現在排除預算用盡的任務，這類紀錄改列 blocked；future queued 不算到期，也不屬於 waiting retry。到期／租約／門檻比較採 `<=`；僅以單次服務 Clock 取樣時間執行一條聚合 SQL，前端時間顯示為台北。

| 告警 code | 等級 | 處理建議 |
| --- | --- | --- |
| DELIVERY_UNCERTAIN | critical | 先核對寄件紀錄與嘗試歷程，勿直接重送 |
| LEASE_EXPIRED | critical | 核對 worker 與嘗試；SMTP 可能已寄出，觀測不自動回收 |
| QUEUE_BLOCKED | warning | 核對預算、排程時間與租約資料；不要批次直接改 DB |
| DELIVERY_FAILED | warning | 不含結果不明的其他 failed，先修正錯誤，再決定人工重新排入或略過 |
| QUEUE_OVERDUE | warning | 查處理模式、worker 與錯誤；模擬完成不等於寄信 |
| WORKER_PAUSED | info | 有 due 且 worker 關閉或 deliveryMode=disabled；可明確選擇 ADMIN 免費模擬 |

`health` 依 critical → warning → info 取最高等級，沒有告警為 healthy。空佇列且 worker 關閉可以是 healthy，含義是沒有佇列告警，不是 transport／worker 心跳或外部服務健康證明。HTTP 失敗時頁面清除舊指標並顯示「目前不能判定」，列表可獨立載入；反向的列表失敗明示「列表未載入」，不顯示為零筆，觀測仍可用。較舊請求晚抵達不會覆蓋最新觀測。

逾期 processing 在既有 dispatch 回收之前只計租約到期；SMTP 回收後 failed／DELIVERY_UNCERTAIN 會改計結果不明。讀取 API 不推進這個狀態轉換。原有人工重試、版本保護、模擬與 SMTP fencing 規則見 [NOTIFICATION_QUEUE.md](NOTIFICATION_QUEUE.md)。

## API 與設定

沿用 ADMIN 專用 `GET /api/notification/queue`，保留原有欄位並追加上述指標、`health` 和 `alerts`。alerts 每項包含固定 `code`、`severity`、彙總 `count`、繁體中文 `message`／`action`。觀測回應不含訂單編號、收件地址、內文、payload、租約 UUID 或原始 exception。STAFF 回 403、匿名回 401，沒有新增公開監控端點。

`NOTIFICATION_OVERDUE_THRESHOLD_SECONDS` 預設 900 秒，可設 60 至 86,400 秒；超出範圍會由配置驗證阻擋啟動。`.env.example`、dev／production Compose 及 Spring properties 已同步；變更後重啟後端。這是通知到期後的等候門檻，與預約付款期限、lease 長度和 SMTP timeout 分開。

不新增 migration，不改既有通知資料。正式產物另外修正 Bootstrap 載入：透過 npm ESM 入口提供全域 `bootstrap` 給既有後台模板，避免本地 UMD 在 production bundle 留下 `require` 而使登入頁空白；不改樣式或模板行為。

## 驗證與後續

本輪 PostgreSQL 17 實測涵蓋：各通道與舊紀錄／終態排除、重疊計數、門檻／租約相等邊界、可調門檻、worker 狀態、空佇列、唯讀／無 transport 呼叫及無個資回應。另跑實際 Cookie HTTP 與正式 build 預覽瀏覽器驗證；最新數字見 [HANDOFF](docs/HANDOFF.md) 與 [驗證摘要](docs/verification/notification-observability-2026-10-06.json)。

備份持久紀錄／觀測已另完成，見 [BACKUP_OBSERVABILITY.md](BACKUP_OBSERVABILITY.md)。仍待：通知保留／清理政策、worker 心跳／趨勢／吞吐量監控、外部告警通道與抑制／確認機制。本輪無自動刪資料、寄送告警或啟用 worker；大規模資料的聚合查詢與負載需另壓測。本機 PostgreSQL 18、實際 SMTP／LINE／手機送達驗收仍見 [待補清單](CLOUD_LOCAL_FOLLOWUP.md)。
