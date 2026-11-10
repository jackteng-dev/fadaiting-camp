# PAYMENT_SANDBOX_SETUP.md

本文件整理目前專案的金流 sandbox / 正式環境接線方式，目標是讓後續拿到商家資料後，可以直接依清單完成設定與驗證。

## 目前狀態

- LINE Pay：
  - 已有付款 request / confirm 骨架
  - 已改為接近官方商家 `v2 payments/request` 與 `v2 payments/{transactionId}/confirm` 流程
  - 前台付款確認頁可接受 `paymentRecordId`、`orderId`、`transactionId`
- 綠界：
  - 已有 AIO 表單導轉骨架
  - 已有 server-side `ReturnURL` callback 驗證
  - 已有瀏覽器端 `OrderResultURL` 導回前台成功 / 失敗頁

## 後端環境變數

請優先參考：

- [backend/payment.env.example](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/backend/payment.env.example)
- [backend/src/main/resources/application.yml](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/backend/src/main/resources/application.yml)

## SMTP / Email 環境變數

- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `SPRING_MAIL_SMTP_AUTH`
- `SPRING_MAIL_SMTP_STARTTLS_ENABLE`
- `SPRING_MAIL_SMTP_CONNECTION_TIMEOUT`
- `SPRING_MAIL_SMTP_TIMEOUT`
- `SPRING_MAIL_SMTP_WRITE_TIMEOUT`
- `NOTIFICATION_MAIL_FROM_NAME`
- `NOTIFICATION_MAIL_FROM_ADDRESS`

### LINE Pay

- `LINE_PAY_ENABLED`
- `LINE_PAY_CHANNEL_ID`
- `LINE_PAY_CHANNEL_SECRET`
- `LINE_PAY_API_BASE_URL`
- `LINE_PAY_CONFIRM_URL_BASE`
- `LINE_PAY_CANCEL_URL`
- `LINE_PAY_DEVICE_PROFILE_ID`

### 綠界

- `ECPAY_ENABLED`
- `ECPAY_MERCHANT_ID`
- `ECPAY_HASH_KEY`
- `ECPAY_HASH_IV`
- `ECPAY_CHECKOUT_URL`
- `ECPAY_RETURN_URL`
- `ECPAY_ORDER_RESULT_URL`
- `ECPAY_RESULT_PAGE_BASE_URL`

## 本機 / 區網 / 正式網址原則

### 本機開發

- 前端：`http://127.0.0.1:5173`
- 後端：`http://127.0.0.1:9090/fadaiting-camp`

本機模式只適合：

- mock 流程驗證
- 串接前資料流開發
- 部分 provider sandbox 手動測試

### 區網開發

若要在同網域其他電腦開啟前台測試，請改成你這台電腦的 IPv4，例如：

- 前端：`http://localhost:5173`
- 後端 callback：`http://localhost:9090/fadaiting-camp/...`

注意：

- LINE Pay 與綠界正式 callback 通常更適合用可公開存取的 HTTPS 網址
- 若只用內網網址，常會受限於第三方平台無法回呼或無法從外部打進來

### 正式 / sandbox 對外測試

正式接金流時，建議至少準備一組可公開存取的 HTTPS 網址：

- 前端站點，例如 `https://camp.example.com`
- 後端 API，例如 `https://api.camp.example.com/fadaiting-camp`

## LINE Pay 設定建議

### sandbox 建議值

- `LINE_PAY_ENABLED=true`
- `LINE_PAY_API_BASE_URL=https://sandbox-api-pay.line.me`
- `LINE_PAY_CONFIRM_URL_BASE=https://你的前台網址/payment/linepay/confirm`
- `LINE_PAY_CANCEL_URL=https://你的前台網址/payment/cancel`

### 正式建議值

- `LINE_PAY_ENABLED=true`
- `LINE_PAY_API_BASE_URL=https://api-pay.line.me`
- `LINE_PAY_CONFIRM_URL_BASE=https://正式前台網址/payment/linepay/confirm`
- `LINE_PAY_CANCEL_URL=https://正式前台網址/payment/cancel`

### LINE Pay sandbox 檢查表

1. 已取得 sandbox `channelId` / `channelSecret`
2. 後端 `LINE_PAY_*` 已填入正確值
3. `confirmUrl` 可從外部開啟
4. `cancelUrl` 可從外部開啟
5. 使用者完成付款後，前台會收到：
   - `orderId`
   - `transactionId`
6. 前台 `/payment/linepay/confirm` 能成功呼叫：
   - `POST /api/public/payment/confirm`
7. 後台付款紀錄頁可看到：
   - provider = `linepay`
   - 正確 `provider_transaction_id`
   - 狀態變成 `paid`
8. 訂單 `paid_amount` 已同步增加
9. timeline 有新增付款確認紀錄

### LINE Pay 目前仍待完成

- sandbox 真實憑證實測
- 正式憑證實測
- 退款 / 查單 API
- 更完整錯誤碼對照與營運處理策略

## 綠界設定建議

### sandbox 建議值

- `ECPAY_ENABLED=true`
- `ECPAY_CHECKOUT_URL=https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5`
- `ECPAY_RETURN_URL=https://你的後端網址/fadaiting-camp/api/public/payment/ecpay/callback`
- `ECPAY_ORDER_RESULT_URL=https://你的後端網址/fadaiting-camp/api/public/payment/ecpay/return`
- `ECPAY_RESULT_PAGE_BASE_URL=https://你的前台網址`

### 正式建議值

- `ECPAY_ENABLED=true`
- `ECPAY_CHECKOUT_URL=https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5`
- `ECPAY_RETURN_URL=https://正式後端網址/fadaiting-camp/api/public/payment/ecpay/callback`
- `ECPAY_ORDER_RESULT_URL=https://正式後端網址/fadaiting-camp/api/public/payment/ecpay/return`
- `ECPAY_RESULT_PAGE_BASE_URL=https://正式前台網址`

### 綠界 sandbox 檢查表

1. 已取得 sandbox `MerchantID` / `HashKey` / `HashIV`
2. 後端 `ECPAY_*` 已填入正確值
3. `ReturnURL` 可被綠界 server 回打
4. `OrderResultURL` 可被瀏覽器導回
5. 後端 `/api/public/payment/ecpay/callback` 能驗證 `CheckMacValue`
6. 付款成功後：
   - `payment_record.status = paid`
   - `booking.paid_amount` 已同步
7. 非成功結果不會一直停在 `pending`
8. 前台成功 / 失敗頁可正常落回網站

### 綠界目前仍待完成

- sandbox 真實商家資料實測
- 正式商家資料實測
- 更完整付款失敗 / 取消情境比對
- 退款與對帳流程

## 推薦實測順序

1. 先跑 LINE Pay sandbox
2. 再跑綠界 sandbox
3. 至少完成一條正式可收款主線
4. 再補退款、查單、對帳

## 目前最需要你之後提供的資料

### LINE Pay

- `channelId`
- `channelSecret`
- sandbox / 正式各自的申請狀態
- 是否需要 `deviceProfileId`

### 綠界

- `MerchantID`
- `HashKey`
- `HashIV`
- sandbox / 正式商店資訊
- 正式可對外的 callback 網址
