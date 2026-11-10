# 備份與隔離還原觀測

ADMIN 登入後選「備份與還原觀測」，可查看最近成功備份、最近成功還原驗證，以及最新成功備份是否曾通過相同 SHA-256 的隔離還原。使用既有後台、Python 標準函式庫與本地私有檔案，不需要付費服務。

## CLI 寫入紀錄

原本的 [備份與還原工具](BACKUP_RESTORE.md)預設不寫入觀測紀錄。啟用時，兩個全域參數必須放在子命令前；同一環境使用一致的 `source-key`，不同正式／測試來源分開識別。以下只示範本專案容器，先核對實際名稱與可信的儲存位置：

```bash
python3 scripts/camp_backup.py \
  --journal work/backup-journal --source-key primary \
  backup --container fadaiting-camp-postgres --output work/backups/camp-20261006

python3 scripts/camp_backup.py \
  --journal work/backup-journal --source-key primary \
  restore-check --backup work/backups/camp-20261006
```

特定 Docker daemon 的 `--docker-host` 也放在子命令前，不改全域設定。`cleanup` 不接受紀錄參數。原有 dump 不會自動補成成功紀錄，需實際重新執行；不要以交接文字或檔案存在捏造成功。

紀錄目錄為 0700、每筆 JSON 為 0600；倉庫內只能寫在被 Git 忽略的 `work`，也可用倉庫外私有絕對路徑。拒絕符號連結及可被其他使用者存取的紀錄目錄。每次產生獨立 UUID 檔，先以原子置換及 fsync 保存 `running`，操作完成後置換為 `success` 或 `failed`。起始紀錄無法保存就不開始操作；終態保存失敗回傳失敗，保留已完成的實際備份，不宣稱紀錄成功。強制中斷可能留下 running。

格式 1 固定十個欄位：`format`、`project`、`sourceKey`、`runId`、`action`、`status`、`startedAt`、`finishedAt`、`archiveSha256`、`errorCode`。成功才有完整備份雜湊；還原必須 `verified: true` 才能成功。失敗只保存固定 `OPERATION_FAILED`，不保存路徑、帳密、資料列或原始錯誤。這是可信操作人管理的執行歷史，沒有簽章；SHA-256 不是來源認證，來源識別碼需由操作人正確對應。

## 後端與部署設定

| 環境變數 | 預設 | 用途 |
| --- | --- | --- |
| `BACKUP_MONITOR_JOURNAL_PATH` | 空 | 後端可讀的私有紀錄目錄；空值顯示未設定 |
| `BACKUP_MONITOR_SOURCE_KEY` | primary | 小寫英數及連字號，1–40 字元 |
| `BACKUP_MONITOR_MAX_BACKUP_AGE_SECONDS` | 86400 | 成功備份提醒，60–31536000 秒 |
| `BACKUP_MONITOR_MAX_RESTORE_AGE_SECONDS` | 604800 | 成功還原提醒，同上 |
| `BACKUP_MONITOR_RUN_TIMEOUT_SECONDS` | 3600 | 未完成操作提醒，60–86400 秒 |
| `BACKUP_MONITOR_RECORD_LIMIT` | 2000 | 整個目錄最大 JSON 數，1–10000 |
| `BACKUP_MONITOR_JOURNAL_HOST_PATH` | 空 | 只供可選 Compose overlay 的主機 bind 路徑 |

本地 Java 可設定 `BACKUP_MONITOR_JOURNAL_PATH` 為可信的絕對路徑。CLI 和後端必須有適當的同使用者／檔案擁有權讀取安排，不能為了部署把紀錄開成公開可讀。Linux／WSL 的設定與演練；其他系統權限模型需另外驗收。

容器部署先建立私有主機目錄並實際寫入紀錄，再設定其絕對路徑及明確加入 overlay，例如：

```bash
BACKUP_MONITOR_JOURNAL_HOST_PATH=/private/camp/backup-journal \
  docker compose -f docker-compose.yml -f docker-compose.prod.yml \
  -f docker-compose.backup-observation.yml config --quiet
```

確認後以相同檔案集合部署。overlay 只為後端加入 `/var/lib/camp-backup-journal` 的 **唯讀** bind，`create_host_path: false`；不自動建立主機目錄，不掛 Docker socket。CLI 由主機運維人員執行，後端沒有備份／還原執行權。dev／prod 預設都未設定紀錄，不自動開啟觀測來源或排程。

## 權限、判讀與故障

唯讀 `GET /api/admin/backup/status` 限 ADMIN；STAFF 403，匿名 401。沒有 HTTP 備份、還原或刪除端點。回應只含彙總時間、狀態、完整雜湊與固定告警，不公開私有路徑、原始 JSON 或例外。頁面手動更新，沒有背景操作或外部告警；HTTP 失敗清除舊觀測，明示無法判定。

- 備份與還原成功時間分開。最新成功備份須有同來源、相同完整 archive SHA-256 的成功還原紀錄才顯示已驗證；其他備份曾還原成功不能代替。相同內容的備份可匹配已有驗證，但仍另計還原時間是否逾期。
- 最新嘗試失敗顯示 critical，同時保留最後成功歷史；running 不計成功，也不代表進程還活著，超時後 critical。門檻相等即逾期。沒有成功備份時 health 為 unknown；正常只代表紀錄規則檢查通過。
- 未設定、不存在／讀不到、損壞、符號連結、格式不符或超出讀取上限時不能判為健康。整個目錄最多讀設定筆數，每檔最多 16 KiB；不截斷成部分成功。其他來源的 JSON 仍需格式有效，再依 sourceKey 篩選。
- 拒絕重複欄位、額外 JSON、非 UUID 檔名、不一致終態、結束早於開始及超過觀測時間五分鐘的未來時間。原子替換的 `.tmp` 不計入紀錄。後端不建立或改寫檔案。
- 歷史成功不保證 dump 目前仍存在、已加密或有離機副本，也不證明 MinIO／環境憑證／Redis／正式整體恢復完成。仍需 [本機待補驗收](CLOUD_LOCAL_FOLLOWUP.md)。

## 驗證與後續

本輪在 Cloud PostgreSQL 17 執行真實 backup／restore-check，另驗證 CLI 失敗會留失敗紀錄；API 及正式產物頁面實測，結果見 [交接](docs/HANDOFF.md)與 [彙總](docs/verification/backup-observability-2026-10-06.json)。原始紀錄、dump、manifest、登入帳密與日誌不提交。

尚未提供自動排程、紀錄／備份保留清理、加密／離機副本、簽章、PITR、worker 心跳或外部告警；本機 PostgreSQL 18、MinIO 與正式切換仍待補。紀錄接近上限時先規劃可信歷史封存，不刪掉待查 running／failed 或稽核需要的紀錄來製造健康。
