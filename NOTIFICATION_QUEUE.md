# 通知佇列與失敗重試

## 通知歷史保留試算

ADMIN 通知頁新增全站唯讀 dry-run，去重／佇列／寄送／金流與未結案資料保護。180 天為草案，沒有刪除或排程。詳見 [NOTIFICATION_RETENTION.md](NOTIFICATION_RETENTION.md)。

ADMIN 現可在本頁面查看全站營運指標與分級告警，讀取不回收／重送任務；指標範圍、門檻與 API 新增欄位見 [NOTIFICATION_OBSERVABILITY.md](NOTIFICATION_OBSERVABILITY.md)。
2026-10-06，延續 Spring Boot／MyBatis／PostgreSQL，不新增付費服務或 message broker。本輪只處理既有預約送出、付款成功及出發前提醒的 Email 任務；站內提醒保留獨立確認操作，沒有實際寄送 Email／LINE。

## 開發與展示

先備份既有 DB 並依序補缺少的 `002` 到 `008_notification_queue.sql`，再部署新版前後端；不要刪 volume。新資料庫初始化八份 SQL。

預設 `NOTIFICATION_DELIVERY_MODE=disabled`、`NOTIFICATION_WORKER_ENABLED=false`。ADMIN 到「預約管理 → 通知紀錄」可按「免費模擬處理」，不需要 SMTP 帳號，也不會寄信。填入完整訂單編號可只處理該訂單，未填則處理全部到期任務，最多 20 筆。列表搜尋支援部分編號，批次操作的編號則必須完全相符。

模擬成功的狀態是 **模擬完成（未寄信）**，`simulatedCount` 增加，`sentCount=0`、`sent_time=NULL`。它是終態，不能再按重試把同一份模擬當成真實寄送；正式驗收應使用新的合成訂單。Email 格式錯誤仍會永久失敗，可用來測試人工處理。

若要自動模擬已入列任務，可明確設 `NOTIFICATION_DELIVERY_MODE=simulate` 與 `NOTIFICATION_WORKER_ENABLED=true`。目前排程只消費既有佇列；出發前提醒仍由 ADMIN 指定入住日期產生，只適用 confirmed 且未入住的預約，不會自動發 LINE 推播。

## 入列與去重

`notification_record` 同時作為 durable outbox 與管理紀錄。預約／付款及通知在同一 PostgreSQL 交易內寫入；交易回滾則通知也不存在。通知寫入失敗時業務交易失敗，不會以記憶體事件替代資料庫紀錄。

- 預約送出：`booking:{id}:submitted:guest / internal`。
- 付款成功：`payment:{paymentRecordId}:confirmed:guest / internal`，不同付款各有紀錄。
- 出發提醒：`booking:{id}:departure:{checkInDate}`，同一入住日期不重複產生。日期變更後的舊提醒在處理時略過，需產生新日期的提醒。

部分唯一索引配合 `ON CONFLICT DO NOTHING` 防止併發重複事件。沒有 Email 的旅客紀錄為 manual／skipped，不以電話冒充 Email；站內提醒不進寄信 worker。

008 為舊紀錄新增欄位，保留原狀態、內容與歷程，`queue_managed=false`，不補 event key 或嘗試紀錄。舊 queued／failed 不會在開 worker 後自動寄出，也不能直接重試變成新版任務。舊資料處置需人工盤點；不要以 SQL 全量改成 managed=true。

## 領取、重試與中斷

worker／手動批次每次在短交易內以 `FOR UPDATE SKIP LOCKED` 領一筆到期 Email，改 processing、增加累積嘗試次數、產生 UUID 租約及 `notification_attempt`，提交後才進行 transport 呼叫。不同 process 可共同消費，不持有資料庫交易等待 SMTP。

| 狀態 | 行為 |
| --- | --- |
| queued | 新任務或人工重新排入，等待到期領取 |
| processing | 已領取，其他 worker 不重複領取，管理員不能改狀態 |
| retry_wait | 暫時失敗，等待 `next_attempt_at` |
| failed | 永久失敗、次數用盡或 SMTP 結果不明，等待人工核對 |
| simulated | 模擬完成，未寄信，終態 |
| sent | SMTP 呼叫完成，終態；不表示旅客已閱讀或信箱一定收到 |
| skipped | ADMIN 略過、通知已不適用或未提供 Email，終態 |
| acknowledged | 站內提醒已確認，與寄信分開 |

預設重試上限 5 次，間隔為 60、120、240、480 秒，計算以 3600 秒封頂。`SMTP_UNAVAILABLE` 可退避重試；`SMTP_AUTH / INVALID_RECIPIENT / INVALID_CONTENT` 永久失敗。錯誤只保存固定代碼與一般訊息，不保存 SMTP 原始 exception、憑證或回應。

領取預設 120 秒到期。每次批次先回收全佇列中最多 100 筆過期租約，訂單範圍只限制新任務的領取。模擬 worker 中斷可在剩餘預算內退避重試；SMTP 中斷／寄出後 DB 完成失敗，可能已寄達，因此標 `DELIVERY_UNCERTAIN`，不自動重寄。完成寫入核對租約 UUID 與到期時間，過期的舊 worker 不能覆蓋新狀態；通知、嘗試結果及 booking timeline 同一交易提交。批次回 `unresolvedCount` 表示領取結果仍待核對，不當成成功。

SMTP 沒有和 PostgreSQL 共用交易，也沒有收件端去重機制；不能保證外部 exactly-once。暫時 SMTP 錯誤或人工重試仍可能造成重複郵件。人工核對寄件紀錄後才重新排入，避免把結果不明當成未寄。

## 管理操作與 API

所有接口僅 ADMIN，沿用 DB 帳號／角色／auth_version 核對與 CSRF。STAFF 與匿名者不能查看通知內容、佇列或嘗試。

| 方法／路徑（前端 `/api`） | 用途 |
| --- | --- |
| POST `/notification/list` | 以訂單、事件、通道、狀態查詢；資料庫分頁，每頁 1–100 筆 |
| GET `/notification/queue` | 到期／等待／處理中／失敗數、configured mode 及 worker 開關 |
| GET `/notification/{id}/attempts` | 最近 100 次嘗試，含模式、起迄、結果、操作者；不回租約 token 或收件資料 |
| POST `/notification/simulate-queued` | 免費模擬，`{limit?, bookingNo?}` |
| POST `/notification/send-queued` | 真實 SMTP 模式才允許，未設定時拒絕且不領取 |
| POST `/notification/departure-reminders` | 建立指定日期／預設台北今天+3天的 confirmed 未入住提醒 |
| PATCH `/notification/status` | `{notificationId, status, expectedVersion, reason}` |

建立提醒回傳 `createdCount`，不計入 `sentCount`。批次結果分開回 `sentCount / simulatedCount / retryCount / failedCount / skippedCount / unresolvedCount`。

`status` 管理操作只允許：failed 且 managed Email → queued；queued／retry_wait／failed → skipped；queued 站內提醒 → acknowledged。不可任意把 Email 標 sent、不可修改 processing 或重送終態通知。舊版本回 409，原因必填 1–300 字，操作者與原因寫入內部歷程；公開查單不曝光這些紀錄。

人工重新排入保留累積次數與全部舊嘗試，追加一輪重試預算（預設 5 次），不歸零或刪除失敗歷史。已嘗試的 delivery mode 不靜默切換：smtp 失敗只由 smtp 消費，simulate 失敗只由 simulate 消費。摘要是全佇列計數，未依單次批次 mode 篩選。時間判斷使用台北 Clock，lease／重試欄位用 timestamptz。

## 設定與後續驗收

`.env.example` 及 dev／prod Compose 均有設定；直接 Java 也讀相同環境變數。

| 環境變數 | 預設 |
| --- | --- |
| `NOTIFICATION_DELIVERY_MODE` | disabled，可設 simulate／smtp |
| `NOTIFICATION_WORKER_ENABLED` | false |
| `NOTIFICATION_BATCH_SIZE` | 20，允許 1–100 |
| `NOTIFICATION_MAX_ATTEMPTS` | 5，允許 1–10 |
| `NOTIFICATION_RETRY_BASE_SECONDS` | 60 |
| `NOTIFICATION_RETRY_MAX_SECONDS` | 3600 |
| `NOTIFICATION_LEASE_SECONDS` | 120，至少 60 |
| `NOTIFICATION_OVERDUE_THRESHOLD_SECONDS` | 900，60–86,400；到期任務等待告警門檻 |
| `NOTIFICATION_SWEEP_DELAY_MS` | 30000 |

真實 SMTP 需明確設 smtp mode、`SPRING_MAIL_HOST / PORT / USERNAME / PASSWORD` 與 `NOTIFICATION_MAIL_FROM_ADDRESS`。手動寄信不要求 worker 開啟；自動寄信另外要求 worker=true。本輪未設定帳號、不連外 SMTP。正式環境需要先核對 SMTP timeout、實際網路政策、免費額度與寄件身分；不因網路受限而繞過 proxy／TLS。沒有免費替代方案時先告知使用者。

免費驗收可先用本機 Mailpit 等收信容器；正式收件、垃圾信箱、退信、SMTP／LINE 服務額度與旅客同意、通知資料保留／清理策略仍待補。取消／入住／退款等額外通知事件也尚未接入。Cloud 測試使用真實隔離 PostgreSQL／Redis與 test classpath 合成 transport，正式程式沒有測試故障注入開關。

測試見 [本輪紀錄](docs/verification/notification-queue-2026-10-06.json)，本機與帳號驗收見 [待補清單](CLOUD_LOCAL_FOLLOWUP.md)。
