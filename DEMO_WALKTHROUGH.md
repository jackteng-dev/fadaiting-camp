# 發呆亭展示操作與階段停點

此版本展示「營區預約 → 固定角色營運 → 付款／退款內部流程 → 通知與備份觀測」。展示可用免費 mock 與隔離測試資料；目前收斂為作品集案例，真實營運驗收仍在 [待補清單](CLOUD_LOCAL_FOLLOWUP.md)。完成本輪收尾後切換 ELPSY Shop，延期事項見 [TODO_AI.md](TODO_AI.md)，不繼續追加封存／清理等大型功能。

## 展示前準備

1. 以遠端 main 和 [最新交接](docs/HANDOFF.md) 為準，依 [新機設定](NEW_MACHINE_SETUP.md) 準備私有設定。隔離本專案 DB／Redis／MinIO、port 與其他作品；不得清空原有 volume。既有資料庫先備份，再套用尚缺的 002–008 SQL。
2. 使用可信測試資料、既有 ADMIN／STAFF 帳號；密碼從自己的私有設定取得，不在展示稿／截圖／Git 留帳密。沒有測試帳號時按 [權限操作](CAMP_PERMISSIONS.md) 建立，不使用正式旅客資料。
3. 確認支付方式為 **mock**、通知 worker 關閉、delivery mode disabled；不要為展示啟用真實金流或 SMTP／LINE。展示用未來日期及今日可入住的獨立測試營位，預先核對價格／庫存。
4. 前端使用 `npm --prefix frontend run build` 的正式產物，API 經 `/api` 代理。HTTP 本機展示明確 `AUTH_COOKIE_SECURE=false`；真正 HTTPS 保持 Secure。啟動方式見新機設定，不沿用過期臨時 Tunnel 網址。
5. 要展示備份成功，先依 [備份工具](BACKUP_RESTORE.md) 執行可信 backup／restore-check，加 `--journal`／`--source-key`，再依 [備份觀測](BACKUP_OBSERVABILITY.md) 明確設定唯讀目錄。未設定會顯示無法判定，不能補造成功紀錄；本輪不重跑 PG18／整體服務還原。

Cloud 是開發與測試環境；最終展示部署在使用者本機。臨時外部連結不視為持久部署，也不能從 build 通過推定外部 HTTPS 可用。

## 建議 8–12 分鐘展示順序

| 步驟 | 畫面／操作 | 可說明的成果與預期 |
| --- | --- | --- |
| 1 官網與預約 | `/`、`/zones`、`/booking` | 選日期／營位／租借品、即時報價與可售檢查，送出合成預約並記錄訂單編號 |
| 2 查單與 mock 付款 | `/booking/lookup` | 用訂單編號＋測試電話查單，查看訂單摘要並操作 mock 付款；表示內部帳務流程，不表示實際扣款 |
| 3 ADMIN 營運 | `/admin/login`、`/admin/booking/list`、`/admin/booking/detail/:id` | 篩選訂單、查看付款與時間線；確認／取消使用版本檢查，取消釋放測試庫存 |
| 4 STAFF 現場工作 | 今日訂單明細、`/admin/booking/check-ins`、`/admin/booking/calendar` | 現場備註、入住／退房，實際退房完成訂單；STAFF 無員工管理／金流／通知觀測權限 |
| 5 退款保護 | ADMIN 明細或 `/admin/booking/payments` | 取消已付款合成訂單後申請／核准／模擬退款；模擬保留原已收款，不能宣稱銀行已退款。人工完成另外需要憑證 |
| 6 通知與保留試算 | `/admin/booking/notifications` | 免費模擬指定訂單、查看嘗試歷程／佇列告警；保留試算全站彙總、去重／稽核保護，不刪資料。模擬完成仍未寄信 |
| 7 備份觀測 | `/admin/backup-observation` | 分開顯示最近成功備份與還原，依同來源／完整雜湊核對；未設定／失敗提示也是真實可展示狀態 |
| 8 收尾與界限 | [交接](docs/HANDOFF.md)／[延期清單](TODO_AI.md) | 說明已驗證交易／權限／故障行為及正式帳號／本機 PG18／MinIO／手機仍待驗收 |

入住示範需今日且已確認、尚未入住的訂單；不能用未來訂單假裝今天入住。取消／退款展示另用一筆尚未入住的訂單，避免改動已入住的資料。過期版本應回 409，不能靠重新整理覆蓋另一位使用者操作。

## 可以展示的技術證據

- 固定角色界線、Cookie／CSRF、停用／撤權及登入輪替；匿名／STAFF 不能繞過後端授權。
- 訂單 row lock、operationVersion、逐晚庫存與退款額度保護；重複付款／操作不會重複記帳。
- PostgreSQL outbox、SKIP LOCKED／短租約、重試退避與結果不明待人工核對，非「Email 一定送達」。
- 私有 public schema dump、同快照資料指紋與無網路隔離還原；成功紀錄是歷史證據，不是完整災難復原保證。
- 唯讀觀測／保留試算、API 失敗不顯示舊健康、正式 build 的瀏覽器驗證。各輪非敏感摘要在 `docs/verification`，原始日誌／fixture 留於 ignored work。

## 已知限制與停止條件

- 圖片與部分文案是示意；實際營運政策、SEO／OG、整體手機 header、後台模板／bundle 效能尚待整理。
- 真實商家 sandbox／付款授權、SMTP／LINE／LIFF 手機、PG18／MinIO／環境憑證與全服務恢復未完成；不對外收真實款項或接收正式旅客營運資料。
- 通知保留 180 天是試算草案；沒有刪除或清理排程。備份加密／離機／PITR／簽章、外部告警與大資料壓測延期。
- 螢幕重現失敗先讀錯誤與交接，不改正式帳號或資料去製造成功；版本／權限錯誤按角色與重新載入處理。
- 展示完成後核對自己的合成訂單／營位才清理，停止自己啟動的實例，不停止其他專案或共用服務；紀錄失敗／未執行的項目，不當成通過。

本輪實測與歷史驗證分開記錄在 [最新交接](docs/HANDOFF.md)及 [收尾摘要](docs/verification/demo-closeout-2026-10-06.json)。
