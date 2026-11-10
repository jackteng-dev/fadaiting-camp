# Camp 圖片儲存：MinIO → SeaweedFS

本分支實作產品程式與 CI 隔離驗收，不代表正式主機已切換。正式主機的切換由伺服器端受控工具執行（備份、更新部署規則、切換、驗證、失敗回復），不能直接在正式主機套用 repository 的開發 Compose。

## 產品契約

- 移除 MinIO SDK／Controller，改用固定版本 AWS SDK v2，透過 SeaweedFS S3、path-style、明確靜態憑證與 `us-east-1`；不讀取 AWS 預設憑證鏈，不自動重試寫入。
- 保留 `/api/upload/userAvatar`、`/api/upload/updateAvatar`、`/api/upload/admin-image` 與原角色／CSRF；不改資料表、初始化 SQL 或帳號。
- 圖片 bucket 限 `user-avatar`、`camp-images`、`rental-item-images`，保留既有 UUID object key。bucket 不存在時建立，403 不當成不存在。
- 新上傳只接受 30MB 以內的 PNG/JPEG/WebP/GIF MIME；不接收 SVG。這是 MIME 篩選，不宣稱新增完整影像解碼／重新編碼。
- 回傳同源 `/objects/<bucket>/<key>?<原始簽名>`；gateway 僅 GET/HEAD，保留簽名要求的內部 Host、raw path/query，清除 Cookie／Authorization。不公開 filer、master、volume 或管理介面。
- **沿用舊七天 presigned URL 限制**，不是永久圖片 URL，也沒有自動刷新 DB 內過期簽名。物件不會隨簽名到期刪除。正式搬遷前必須核對已保存 URL，不能只搬 bytes 就宣稱舊圖片全部可讀。

## 僅供全新本機 scope 的設定

先在 repository 外或 ignored `work/` 建立權限 0700 的私有目錄，以可信方式產生 Camp 專用隨機 `CAMP_STORAGE_ACCESS_KEY`（至少16字元）與 `CAMP_STORAGE_SECRET_KEY`（至少32字元），只放私有環境；不要使用 Shop 金鑰。

```bash
python3 scripts/configure-camp-storage.py --output work/<私有目錄>/s3.json
```

設定 `CAMP_STORAGE_CONFIG_FILE` 為上述檔案的絕對路徑。工具只建立新的0600檔案，拒絕覆寫／symlink／不安全父目錄，不連主機、不部署、不顯示金鑰。若所選容器 UID 無法讀取，須審核 UID/GID 與私有掛載，不要將正式憑證設成 world-readable。

其餘變數見 `.env.example`：`CAMP_STORAGE_ENDPOINT`、`CAMP_STORAGE_REGION`、`CAMP_STORAGE_PUBLIC_BASE_PATH`。Compose 的 backend 使用 `http://seaweed:8333`；本機 Java 預設 `http://localhost:9000`。Vite `/objects` proxy 需與簽名 endpoint Host 一致，Docker 開發 overlay 已設定 `VITE_OBJECT_PROXY_TARGET`。

SeaweedFS 固定 digest 與已使用的 Shop 基準一致，但資料、身份、bucket 及 Volume 獨立。`/data` 同時保存 volume/master 與 filer LevelDB2 metadata；本機範例上限四個128MB volume，master 每次配置一個，三個 bucket 不會被第一個 collection 用盡名額；正式容量與告警由管理者依實際資料另審。新 volume 是 `seaweed-data`／`seaweed-prod-data`，**不是舊 `minio-data`／`minio-prod-data`**。舊 Volume 只保留，不能把磁碟格式直接改名掛入。9000 僅 loopback 對應內部8333，移除9001 console；其餘內部埠不 publish。

單容器各元件廣告地址用 `127.0.0.1`、bind `0.0.0.0`；只有同容器 master/filer/volume 使用 loopback，S3 仍經固定容器入口。這避免 Cloud 注入 proxy 時內部 gRPC 誤經外部 proxy，不變更全域網路或代理。

## 合併／主機切換前的必要審查

| 項目 | 必須審查的差異 |
|---|---|
| Port／Network | 主機既有 Caddy/Tailscale 入口不改；審查 Camp scope 中 SeaweedFS、S3 endpoint Host、gateway `/objects`、內部依賴及資源共存；不能公開 filer/管理埠 |
| Volume／資料 | 新 SSD 持久路徑、UID/GID、filer metadata＋volume＋master 一致性、容量／日誌／加密備份及完整隔離還原；保留原 MinIO bytes |
| Secret | 由管理者安全產生並掛入 Camp 專用 S3 身份／backend 設定，不從 Cloud 索取正式值，不重用 Shop 或舊 MinIO 管理員金鑰 |
| DB／Migration | 本分支沒有 schema／SQL migration；但舊圖片 URL 轉換屬正式資料寫入，須另批、先備份、逐欄 preview/CAS，不在啟動時改寫 |
| CI／Recipe | Workflow YAML 不變；原完整 CI 新增真 SeaweedFS。Publisher 移除編譯前 MinIO patch，改為產品原始碼直接驗收；recipe hash 會改變 |
| Host trust | `application.yml` 位於 Camp config fence；管理者必須審新 recipe／contract／controller/service、實際資料 adapter與 policy。不得改 trust verifier、停 Timer 或跳 source/OCI/Private gate |

在新 recipe／config fence／資料遷移未審核前，只交付獨立 PR，不直接更新 main 觸發正式發布。產品的自動 image-only CD 不能代替這次儲存格式遷移。

## 管理者受控搬遷與回復順序（本輪未執行）

1. 讀取即時主機 current、正式映像／OCI、Compose fence、Private Package、磁碟與其他站基線；以實際最新來源為準，不拿此文件固定快照當現場狀態。
2. 盤點三個 bucket、object key、bytes、SHA256、Content-Type／必要 metadata，以及引用欄位：app_user.user_pic、article.cover_img、rental_item.image_url、site_image.image_url；現場須再次核對實際 schema與其他引用。不要改 payment_url、LINE profile picture_url 或外站 URL。
3. 在批准的 Camp 寫入靜置窗口，建立同一一致性時點的 PG／MinIO／Redis／設定加密備份；先做完整隔離還原。保留來源只讀副本與原 app/MinIO/DB old refs。
4. 在獨立私有 scope 透過 S3 客戶端逐 object 搬入全新 SeaweedFS，保留 bucket/key/bytes/metadata；逐物件讀回 SHA256、總數／總 bytes 與 DB 引用。不能只比較 ETag（multipart／實作可能不同），也不能 mount 舊 MinIO volume。
5. 舊簽名含 MinIO Host／憑證／期限，不能直接替換 hostname。針對 allowlist 圖片引用產生新 provider 簽名、檢查欄位長度、預覽並以舊值 CAS 更新；正式 DB 改寫另批。若要永久 URL／自動刷新，需先另定產品契約，不能冒稱目前已提供。
6. 用真登入／角色／CSRF驗證頭像、租借品／營區圖片上傳與讀取、匿名無簽名拒絕、過期／錯簽名拒絕、重啟 bytes、備份還原，以及其他四站不變。另演練故意新儲存故障 → 原 app/MinIO 的相容回復。
7. 若新站已接受上傳，直接回舊 MinIO 會遺失新物件引用；先靜置並受控補回新 objects／對應 URL，或停止切換交由人工處理。不能為 rollback 覆寫整個正式 DB、降 schema、清 bucket 或丟掉新照片。
8. 全部批准後才安排合併／新 main CI SUCCESS／Private GHCR，主機管理者以新 recipe/fence 與資料 adapter 執行受控切換並核對 actual digest／OCI／資料／receipt+journal。後續原 Timer 自然 already current 才記正式驗收。

## Cloud 驗證與界線

原 full Maven suite 保留，新增 S3 上傳／私有授權／wrong credentials／raw signed URL／頭像回歸，CI 在真 PG18、Redis、SeaweedFS disposable scope 中執行。完整 Maven 完成後用原容器 restart 核對圖片 SHA256，再真正啟動 Publisher 產生的 Nginx recipe，核對 `/objects` bytes 與匿名403；不以 mock S3 或單純 healthz 取代。

Cloud 測試使用合成圖片與新憑證，不讀正式圖片／DB。實際測試數字與失敗紀錄寫在 `docs/HANDOFF.md`；本地等價驗收、真正 Hosted CI、Ubuntu 遷移分開記錄。Cloud PASS 不代表正式搬遷、完整物件遷移工具、永久 URL 或主機備份 adapter 已完成。
