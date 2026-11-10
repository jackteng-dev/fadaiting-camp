# 退款申請與人工對帳

2026-10-06，Cloud 免費驗證版。沿用既有付款、預約與固定角色；沒有呼叫銀行、LINE Pay 或綠界退款 API，沒有轉移資金。人工完成是登記已在系統外處理的結果，憑證真實性仍由管理員核對。

## 操作流程

ADMIN 從「付款紀錄」或「預約明細」的「退款／對帳」開啟共用視窗。STAFF 沒有入口，API 也回 403；未登入回 401。核對、申請、審核與完成保存實際登入管理員、時間、說明和內部訂單歷程。

一般 `paid` 收款只允許已取消或已完成的預約申請退款；先取消不會自動退款。退款原因與每次處理說明必填，可部分或全額申請，僅支援 TWD。

| 目前狀態 | 操作 | 結果與帳務 |
| --- | --- | --- |
| 無申請 | 申請退款 | `requested`，保留可退額度 |
| `requested` | 核准 | `approved`，繼續保留額度 |
| `requested` | 駁回 | `rejected`，釋放額度 |
| `approved` | 模擬退款 | `simulated`，釋放額度；不減少實際已收金額 |
| `approved` | 登記人工完成 | `completed`，需已退款確認及憑證；扣訂單已收淨額 |
| `rejected / simulated / completed` | 再處理 | 拒絕；需新增申請，仍受剩餘額度限制 |

模擬與人工完成都不是實際發起退款。Cloud 演練人工完成使用合成憑證，不能當成真實退款證明。同一付款的人工退款憑證不能重複使用，統一去除前後空白並轉大寫。

## 晚到收款與舊帳

合法晚到綠界 callback 留為 `paid_review`，尚未計入訂單已收金額。管理員填查單憑證和說明後選擇：

- 確認歸帳：轉 `paid / accepted` 並同步付款淨額；不重新開啟已取消預約、不恢復庫存。
- 安排退款：保留 `paid_review`，改 `refund_required`，才開放申請退款。人工完成不再次扣訂單金額，因這筆款未曾計入訂單。

核對決定後不能重複歸帳。一般 `paid` 付款如因舊版人工改金額造成帳本不一致，人工完成會拒絕；先用「核對並同步帳本」留下憑證與說明，再將訂單已收金額同步為付款紀錄扣除實際退款的淨額。不自動轉換或覆蓋舊資料。

這裡的核對基於系統付款紀錄和操作員輸入，尚未查詢商家 API、匯入銀行對帳單或驗證附件。正式對帳需要商家或營運資料，列在 [待補文件](CLOUD_LOCAL_FOLLOWUP.md)。

## 金額、重試與競爭

- 原始 `payment_record.amount` 與 `booking.total_amount` 保留；`booking.paid_amount` 為正常 `paid` 收款減 `completed` 退款的淨額。待核對收款不計入該淨額。
- 可申請額度＝原收款－`requested / approved / completed` 退款合計；待審核也占額度，駁回／模擬不占額度。只有 `completed` 計入實退。
- 申請帶 UUID `idempotencyKey`。同付款、金額、原因的原樣重試返回同一筆，即使版本已改；同碼改內容回 409。改內容產生新碼。
- 申請核對付款 `expectedVersion`；退款動作核對退款 `expectedVersion`；對帳同時核對付款及 `expectedBookingVersion`。過期版本回 409，前端載入新資料並保留錯誤與草稿。
- 付款、退款、核對、取消與逾時共用 booking 行鎖；退款、訂單淨額、版本與歷程同一交易。任何歷程故障全部回滾。
- 有待處理或已實退退款的取消訂單不能直接重開。已實退訂單不能再建立付款收取原總額與淨額之差；後續住宿或收款另建預約。駁回／模擬且無其他阻擋退款時可依既有庫存規則重開。

旅客公開查單與 LINE 查單只提供已實退金額，不包含原因、處理說明、憑證或操作者。旅客頁提示退款後餘額洽營區，停止繼續付款。STAFF 訂單明細去除核對憑證、說明、操作者與時間，並排除退款及人工核對內部歷程。

付款列表原有統計／匯出仍是原收款金額，不能當退款後淨收入報表；退款視窗顯示原收款、實退、申請保留、可申請金額。尚未提供獨立退款 CSV、會計報表、退款附件或雙人覆核（目前同一 ADMIN 可申請及審核）。

## API

前端用 `/api`；直連後端為 `/fadaiting-camp/api`。以下全部限定 ADMIN。

| Method／路徑 | 內容 |
| --- | --- |
| `GET /payment/finance/{paymentId}` | 退款紀錄、四種金額、付款／訂單版本、核對資訊 |
| `POST /payment/refunds` | `paymentRecordId, amount, reason, idempotencyKey, expectedVersion` |
| `PATCH /payment/refunds/{refundId}` | `action=approve/reject/simulate/complete_manual, expectedVersion, note`；人工完成另需 `externalReference` |
| `PATCH /payment/reconciliation/{paymentId}` | `resolution=accept/refund_required/sync_ledger, expectedVersion, expectedBookingVersion, note, externalReference` |

## 升級

既有 DB 先備份；依序補缺少的 `002` 到 `006_payment_refunds.sql`，再部署前後端。Compose 重啟不會補跑已有 volume 的 init SQL，不可刪 volume 重建。006 新增付款版本、核對欄位與 `refund_record`；可重跑，不轉換既有付款或補建退款。

沒有新的付費服務、商家金鑰或退款開關；本次 Cloud 使用 `LINE_PAY_ENABLED=false / ECPAY_ENABLED=false` 的 mock。既有已啟用商家設定不會由這個退款功能自動呼叫退款接口。

程式與 HTTP／資料庫／瀏覽器驗證紀錄見 [退款驗證](docs/verification/payment-refunds-2026-10-06.json)。本機升級、商家 sandbox 及實際憑證驗收暫緩。
