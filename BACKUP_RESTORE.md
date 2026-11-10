# PostgreSQL 備份與隔離還原演練

## 可選執行紀錄與後台觀測

使用全域 `--journal work/backup-journal --source-key primary` 可持久保存每次備份／還原的 running／success／failed，必須放在 `backup` 或 `restore-check` 子命令之前。備份成功不自動代表還原成功；原有工具預設不改變。格式、權限與 ADMIN 唯讀部署見 [BACKUP_OBSERVABILITY.md](BACKUP_OBSERVABILITY.md)。

本工具只處理發呆亭 PostgreSQL `public` schema。備份是 `pg_dump -Fc` 的二進位檔；核對還原成功後自動移除一次性容器，不提供覆蓋既有資料庫或正式切換功能。工具免費，只需要 Python 3.10+、Docker CLI、正在運行的官方 PostgreSQL 容器，以及可從 container 內 socket 登入的資料庫使用者。

2026-10-06 Cloud 實測使用現有 PostgreSQL 17：28 張表、2,447 筆資料、2 個 view；中文／emoji、預約／付款／退款／通知關聯、序號延續及失敗清理均通過。來源資料／結構在演練前後一致。Compose 預設仍為 PostgreSQL 18，未宣稱已完成 18 或使用者本機資料驗收。非敏感證據見 [驗證摘要](docs/verification/backup-restore-2026-10-06.json)。

## 備份

在倉庫根目錄的 Linux／WSL 執行。Windows 請使用已能存取 Docker 的 WSL；本輪未測原生 PowerShell。不要用文字管線搬移 dump。

```bash
python3 scripts/camp_backup.py backup \
  --container fadaiting-camp-postgres \
  --database fadaiting_camp --user camp \
  --output work/backups/before-upgrade-2026-10-06
```

`--container` 必須指定本專案來源。工具只在來源執行唯讀查詢和 dump，不建立帳號、不更改資料。來源至少須有 migration 008 的必要資料表；舊資料庫升級前，仍需由管理者另外備份，不能為了使用本工具先跳過升級前保護。

輸出目錄必須是新的名稱，已有目錄會拒絕而不覆寫。倉庫內僅允許寫入被 Git 忽略的 `work`；也可指定倉庫外的私有備份目錄。產物為 `database.dump` 和 `manifest.json`，目錄權限 0700、檔案 0600。manifest 含資料表筆數／雜湊、核對用結構、來源 image ID、版本與 dump SHA-256，不輸出原始資料列或帳密。

備份使用同一個 exported MVCC snapshot 產生逐表資料指紋及 dump；寫入期間新增的交易不會讓兩者讀到不同版本。唯讀快照仍會持有鎖／延長版本保留，大資料庫或 DDL 升級前需安排合適時段；每條核對查詢最長 120 秒。序號值不受 MVCC 保護，併發新增可能產生合法的序號間隙；若需精確核對 counter，先停寫再備份。

## 隔離還原與核對

```bash
python3 scripts/camp_backup.py restore-check \
  --backup work/backups/before-upgrade-2026-10-06
```

先驗 manifest 與 SHA-256，再使用來源的**本地 image ID**建立全新 `camp-restore-<隨機識別碼>` 容器。設定為 `--pull=never`、`--network=none`、不 publish port、不掛載主機目錄；資料存在 512 MB tmpfs，memory 512 MB／CPU 1。只在該隔離容器使用 socket trust 登入；不啟動後端或通知 worker。

還原以單一交易執行，`--clean --if-exists` 只用於新建的空白還原容器，以處理預設已存在的 public schema。CLI 沒有指定既有還原目標的選項。核對所有 public 表的筆數與完整內容雜湊，以及欄位、CHECK／外鍵（須 validated）、索引、普通 view 與 sequence 定義；成功輸出 `verified: true` 和彙總數字。PostgreSQL 17 dump 會改寫純 varchar 字串陣列的 text 轉型表示；工具只對這個已驗證的等價寫法做窄範圍正規化，其他差異仍拒絕。

一般成功及任何失敗都清理本次容器；不刪來源 volume。`--keep` **只保留已核對成功**的隔離實例，方便人工檢查：

```bash
python3 scripts/camp_backup.py restore-check \
  --backup work/backups/before-upgrade-2026-10-06 --keep
```

回應包含 `container`、`name` 和 `cleanupToken`。檢查完用回應中的完整值清理，不把來源容器 ID 填入：

```bash
python3 scripts/camp_backup.py cleanup \
  --container <回應的-container-ID> --token <回應的-cleanupToken>
```

清理須同時符合工具 label 與精確容器名稱。工具進程遭強制終止或主機斷電時，可能無法執行 finally；管理者需以 `docker ps -a --filter label=com.fadaiting.restore-run` 查詢並核對名稱／label 後清理自己那次的容器，不能批次刪除其他專案。保留實例不具持久儲存用途。

## 環境與限制

- 默認使用現有 Docker context；若環境要求指定 daemon，在子命令前加 `--docker-host unix:///var/run/docker.sock`。Cloud 的錯誤遠端 Docker TLS selector 應只在該次命令移除，不改全域設定；HTTP proxy 與 CA 保持原設定。工具不下載 image、模型或呼叫外部服務。
- 備份檔含旅客資料及密碼雜湊，**未加密**；檔案權限不能取代備份加密、離機儲存及保留策略。不要提交 dump、manifest、原始測試資料或含個資的日誌。
- SHA-256 只能偵測意外損壞，不能驗證來源；只還原自己產生且可信的 dump／manifest，不執行陌生人提供的備份。pg_restore 會執行備份中的 SQL。
- 跨機器演練需安全搬移備份，並先準備相同 image ID，例如另外用 Docker save／load 搬移可信的來源 image；工具不自行拉取或替換版本。升級不同 major 需要另外驗收。
- 512 MB tmpfs 與 300 秒預設 Docker timeout 適用本次小型展示資料；`--timeout` 可在子命令前調整至最多 3,600 秒，但大資料庫仍需另一套經容量驗收的還原規劃。工具沒有 production RPO／RTO 承諾。
- 此為 public schema 邏輯備份，排除其他 schema、cluster role／owner／ACL、tablespace、WAL／PITR、MinIO 圖片、Redis、環境設定與 JWT／SMTP／LINE 憑證。現有來源沒有 public 自訂函式／trigger；未測這些物件、extension 依賴、materialized view 或 RLS 的完整結構核對。不是整個平台的災難復原完成證明。
- 恢復服務前需另核對 MinIO 物件、環境憑證、正式 owner／privilege 及 Redis 登入狀態；需要重新登入。還原的通知資料不可直接接真實寄信 worker，先人工確認哪些已寄出及結果不明，避免重送。自動備份排程、保留／加密／離機副本及正式切換仍待補。

## 測試

```bash
python3 scripts/test_camp_backup.py
```

不啟用整合開關時跑 11 項保護測試、略過 6 項 PostgreSQL 測試。完整演練只連線到明確指定的本專案測試來源，再從備份建立無網路的 fixture 副本；所有合成預約、付款、退款與通知只寫入副本。不要對其他專案或正式資料開啟整合測試。

```bash
CAMP_BACKUP_INTEGRATION=true \
CAMP_BACKUP_SOURCE=fadaiting-camp-postgres \
python3 scripts/test_camp_backup.py
```

特定 daemon 可透過 `CAMP_BACKUP_DOCKER_HOST` 指定。完整 17 項涵蓋中文／關聯／序號、並行寫入快照、損壞 bytes 在建容器前攔截、重新算 checksum 的截斷 dump 還原失敗、資料指紋不符、失敗清理、既有備份不覆蓋、錯誤 schema／路徑／所有權保護。來源前後指紋比較要求演練期間沒有其他測試或使用者寫入；在靜置的本專案測試 DB 執行。
