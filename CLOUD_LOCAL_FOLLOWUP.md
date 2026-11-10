# Cloud 已完成與後續待補

## 展示停點與延期決定

使用者已指定本輪完成發呆亭收尾後切換 ELPSY Shop。下方可於 Cloud 續作的封存／清理／排程等也是延期工作，不自動展開；完整六類待辦見 [TODO_AI.md](TODO_AI.md)，操作見 [展示稿](DEMO_WALKTHROUGH.md)。本機／商家帳號／真實 LINE 等條件仍保留，不能用展示驗證代替。

## 2026-10-06：通知保留唯讀試算已完成

Cloud 已完成全站 dry-run、保護原因與 ADMIN 權限／頁面。180 天只是試算草案，正式保留需求、封存／復原與人工小批次清理仍待另一階段；PG18／使用者主機／正式資料未代為核定。見 [NOTIFICATION_RETENTION.md](NOTIFICATION_RETENTION.md)。

## 2026-10-06：備份觀測已在 Cloud 完成

新增 CLI 私有執行紀錄與 ADMIN 分離成功時間、相同備份核對、失敗保留歷史與唯讀告警。[操作文件](BACKUP_OBSERVABILITY.md)及 [交接](docs/HANDOFF.md) 保留部署與驗證界限；PG18、使用者主機權限／持久磁碟、MinIO／環境憑證／正式整體恢復仍待補。

以免費工具持續開發；最終部署在使用者本機。這份清單區分可繼續在 Cloud 完成的程式工作與需要實際資料、帳號或設備的驗收，無需現在在個人電腦下載環境。

## Cloud 已完成（更新至 2026-10-06）

- ADMIN 通知營運觀測：新版全站 Email 指標、分級唯讀告警／建議、失敗清除舊觀測與請求競爭保護；正式 Bootstrap 載入修正。見 [NOTIFICATION_OBSERVABILITY.md](NOTIFICATION_OBSERVABILITY.md)。
- PostgreSQL 17 public schema 備份及無網路一次性還原：中文／關聯／序號、併發快照、損壞備份與失敗清理，全 17 項通過；來源保持不變。操作與限制見 [BACKUP_RESTORE.md](BACKUP_RESTORE.md)。
- 固定角色的員工管理頁與 API：建立、搜尋、角色調整、啟停、最後一位管理員保護；具交易鎖、版本衝突檢查與操作紀錄。
- `auth_version` 非破壞式 migration；角色／狀態異動讓舊登入永久失效，重新啟用需新登入。
- 固定 ADMIN／STAFF 的 API 操作界線、依角色顯示選單與明細操作。
- 帳號停用、撤權、登出立即阻擋後續管理請求；自助資料更新限制本人。
- 實際入住／退房、台北日期規則、狀態版本與訂單 row lock；退房完成訂單，狀態與操作者歷程同一交易。
- 逾期住宿保留於名單／日曆；後端日期篩選、租借品合併、完整分頁與實際狀態 CSV。
- 新預約付款期限、自動／ADMIN 逾時批次、取消與重開庫存保護、付款競爭／歷程故障回滾；綠界簽章與晚到收款待核對。
- ADMIN 退款申請／審核、模擬與人工完成、晚到收款核對／舊帳同步；退款額度、版本、重試、併發與交易回滾已驗證。詳見 [PAYMENT_REFUNDS.md](PAYMENT_REFUNDS.md)。
- Cookie／CSRF 寫入保護、15 分鐘／7 天 JWT 效期與原子 refresh、LINE ID token 核驗與逐筆綁定／舊資料不追認；使用 test classpath 合成 adapter 完成免費資料庫／HTTP 測試。
- PostgreSQL 持久通知佇列、事件去重、領取租約、失敗退避、人工重新排入與 ADMIN 模擬／歷程頁；真實寄信及 worker 預設關閉，未寄出 Email／LINE。
- 交接備註獨立 API，現場與金額操作分開。
- 補入固定角色的非破壞式 SQL，保留既有角色與權限資料。
- 移除預設 JWT 共用金鑰，Compose 要求私有環境變數。
- 真實 PostgreSQL／Redis、HTTP 與瀏覽器驗證；前端正式建置排除本機帳密輔助檔。

## 可接著在 Cloud 做

1. 正式通知保留政策、封存／復原及安全清理；worker 心跳／趨勢。唯讀 dry-run、備份持久紀錄與通知觀測已完成。
2. 備份排程、私有儲存／保留策略設計；實際商家 callback 在取得 sandbox 帳號後也可先於 Cloud 測試。

## 留待可使用本機或帳號時驗收

| 項目 | 需要的條件 | 驗收內容 |
| --- | --- | --- |
| 舊資料庫過渡 | 使用者現有 DB 與備份 | 盤點角色、庫存保留一致性與舊訂單付款政策，保留至少一位管理員，依序補 `002`、`003`、`004`、`005`、`006`、`007`、`008` 再更新前後端，所有帳號重新登入並逐一驗證；不要刪 volume 重建 |
| 本機啟動與展示 | Docker 或 Java 25／Node、實際網路 | 私有 JWT 金鑰、隔離各專案 port、反向代理、啟停流程與 HTTPS／Secure cookie；HTTP 開發設定 false，正式 HTTPS 設 true |
| 本機／完整備份還原 | 實際 PostgreSQL 18／MinIO 資料與私有設定 | Cloud PostgreSQL 17 工具已完成；本機先對可信測試副本驗收版本／容量，再演練圖片、憑證設定、離機加密副本及恢復服務；不覆寫來源、不誤啟通知 worker |
| 金流 sandbox | LINE Pay／綠界商家測試資料 | 合法簽章、重複／晚到 callback、失敗／取消／逾時、非即時付款期限、查單與退款；預設不使用付費交易 |
| 真實退款與對帳 | 商家 sandbox／銀行對帳、實際退款憑證 | 確認憑證與入退帳淨額、退款政策與退款後再收款規則；Cloud 的人工完成僅用合成資料，未退款／驗證銀行憑證；尚無自動退款 API 或淨收入報表 |
| 通知送達 | 本機 Mailpit 或實際免費 SMTP／LINE 帳號 | 先用本機收信容器確認內容與 transport，再驗證 TLS、免費額度、寄件身分、退信與收件；Cloud 僅模擬，不算已寄達。舊紀錄不自動入列，正式啟用 SMTP 前須人工核對結果不明任務 |
| LINE／LIFF | 實際 Channel、LIFF ID 與手機 | 新版 Login Channel audience、openid scope、實際 ID token／手機核驗、逐筆綁定及 Rich Menu／通知；舊綁定需重做，Cloud fixture 不算實機驗收 |
| 獨立 LINE 呼叫方 | 後續在 fadaiting-line 專案工作 | 改送 liff.getIDToken()；停止自報 userId／profile，處理 401／503、重新綁定與移轉協助；本輪只修改 camp 主服務 |
| JWT 金鑰輪替 | 本機或正式部署的副本設定 | 所有副本同步新金鑰並重啟，舊 token 全部重新登入；本版未提供新舊多金鑰平滑過渡 |
| 真實內容與營運 | 營區照片、文案、實際值班流程 | 管理員／現場人員矩陣、實際到場／離營、晚到／逾期、付款保留與尾款政策，退款政策與旅客資料保留規則 |
| 多專案同機 | 最終 24–32 GB 可用環境 | 整合 port／資源配置與實際負載，Cloud 的測試通過不換算為吞吐量承諾 |

若任何外部測試要求付費方案，先找免費替代方式；沒有免費方案時再向使用者說明，不自行啟用付費服務。

本輪 Cloud 只執行隔離的資料與 mock／內部流程；沒有啟用真實商家付款、寄送 Email 或發布 LINE 訊息。
