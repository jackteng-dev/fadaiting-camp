# LINE 專案拆分規劃

本文件定義「LINE 互動端獨立成另一個專案，但後台維護仍保留在主專案」的切分方式。

## 目標

- 主專案持續作為營運核心系統
- LINE 專案獨立負責 webhook、LIFF、訊息互動與 LINE 使用者流程
- 後台只保留一套，仍在主專案維護
- 核心訂單、付款、通知、營區資料只保留單一資料來源

## 專案切分

### 主專案：`fadaiting-camp`

保留：

- 官網前台
- 後台管理
- 營區資料
- 區域 / 營位 / 價格 / 庫存
- 訂單
- 付款
- 通知紀錄
- 後台報表與營運工具
- 提供給 LINE 專案使用的 API

不負責：

- LINE webhook 實際接收
- LINE Flex Message 組裝
- LIFF 頁面流程

### LINE 專案：`fadaiting-line`

負責：

- LINE Messaging API webhook
- 訊息指令路由
- Flex Message / Quick Reply / Rich Menu
- LINE user 綁定
- LIFF / LINE MINI App 頁面
- LINE 內查訂單
- LINE 內導到預約 / 付款 / 查詢流程

不負責：

- 後台營運管理
- 核心訂單規則維護
- 核心付款資料維護
- 核心營區資料維護

## 核心原則

### 1. 主專案是主資料來源

以下資料只應由主專案維護：

- `booking`
- `payment_record`
- `notification_record`
- `camp_site`
- `camp_zone`
- `camp_unit`
- `rate_plan`
- `availability`

LINE 專案不直接改寫這些核心表。

### 2. LINE 專案優先透過 API 串接

建議：

- LINE 專案呼叫主專案 HTTP API
- 不要讓兩個專案共同直接寫核心 DB

這樣比較容易：

- 維護權限
- 控制資料一致性
- 保持訂單與付款規則單一
- 分開部署與除錯

### 3. 後台仍只保留在主專案

營運人員仍只使用：

- 主專案後台

不另外建立 LINE 專案自己的營運後台。

## 建議架構

```text
使用者
  -> LINE 官方帳號
  -> fadaiting-line
      - webhook
      - LIFF
      - LINE user binding
      - message builder
  -> 呼叫 fadaiting-camp API
      - public API
      - line integration API
      - booking / payment / lookup API
  -> PostgreSQL / Redis / MinIO
```

## API 邊界建議

### 主專案保留的既有 API

- `POST /api/public/booking`
- `POST /api/public/booking/quote`
- `POST /api/public/booking/lookup`
- `POST /api/public/booking/summary`
- `POST /api/public/payment/request`
- `POST /api/public/payment/retry`
- `POST /api/public/payment/confirm`

這些 API 未來仍可作為 LINE 專案的底層能力。

### 建議新增的 LINE 整合 API

主專案可新增一組較穩定的 API，專門提供 LINE 專案使用：

- `POST /api/public/line/bind/request`
  - 用手機或驗證碼發起 LINE user 綁定
- `POST /api/public/line/bind/confirm`
  - 完成 LINE user 與旅客資料綁定
- `POST /api/public/line/booking/list`
  - 依 LINE user 查詢可見訂單摘要
- `POST /api/public/line/booking/detail`
  - 查詢單筆訂單公開資訊
- `POST /api/public/line/payment/request`
  - 從 LINE 專案重新建立付款
- `POST /api/public/line/profile/sync`
  - 同步 LINE 顯示名稱、頭像等外部資料

這樣可以把 LINE 專案與一般網站訪客 API 稍微分流。

## 資料表建議

### 保留在主專案 DB

建議新增：

- `line_user_binding`

欄位可先規劃：

- `id`
- `line_user_id`
- `display_name`
- `picture_url`
- `phone`
- `contact_email`
- `booking_guest_id` 或對應旅客識別欄位
- `status`
- `bound_at`
- `last_interaction_at`
- `created_at`
- `updated_at`

用途：

- 把 LINE 身分對應到主專案旅客資料
- 支援 LINE 查訂單
- 支援後續 LINE 通知推播

### 不建議的做法

不建議讓 LINE 專案自己維護另一份訂單或付款主表。

可以接受的只有：

- webhook event log
- message send log
- LIFF session / nonce
- LINE 專案自己的暫存快取

## 推薦開發順序

### Phase 1：完成拆分設計

- 定案主專案與 LINE 專案責任邊界
- 定案 API 串接方式
- 定案綁定流程

### Phase 2：主專案先補 LINE 整合能力

- 新增 `line_user_binding` schema
- 新增 LINE 綁定 API
- 新增 LINE 查單 API
- 新增 LINE 專用通知 payload 結構

### Phase 3：建立 `fadaiting-line` 新專案

第一版最小功能：

- webhook 接收
- 關鍵字回覆
- 導去現有官網頁面
- LIFF 登入骨架

### Phase 4：LINE 查訂單

- 綁定 LINE user
- 查詢我的訂單
- 查看付款狀態
- 重新付款入口

### Phase 5：LINE 內預約流程

- LIFF 預約頁
- 訂單摘要
- 付款導頁

## 第一版最低可行範圍

如果要先做可 demo 版本，我建議 LINE 專案第一版只做：

- 歡迎訊息
- 營區資訊
- 營位資訊
- 最新公告
- FAQ
- 進入預約頁
- 進入查訂單頁

先不要一開始就做：

- 全聊天式下訂
- 直接在 Bot 內完成全部付款
- 複雜 AI 對話客服

## 風險與注意事項

### 1. 資料一致性

若 LINE 專案直接改主表，未來很容易出現：

- 訂單狀態不同步
- 付款流程重複入帳
- 通知重送混亂

所以核心狀態變更最好仍由主專案 API 統一處理。

### 2. 身分驗證

LINE userId 不能直接等於「已完成旅客驗證」。

仍建議搭配：

- 手機驗證碼
- 訂單編號 + 手機末碼
- 或 LIFF 登入後的二次綁定

### 3. 金流回跳

即使從 LINE 進來，正式付款結果仍建議回到：

- 主專案前台頁
- 或主專案提供的 LIFF 專用結果頁

不要讓 LINE 專案自己另外維護一套付款結果規則。

## 建議定案

目前建議正式採用：

- 主專案：`fadaiting-camp`
- LINE 專案：`fadaiting-line`
- 後台維護：只留在 `fadaiting-camp`
- 核心資料：只留在 `fadaiting-camp`
- 串接方式：`fadaiting-line` 呼叫 `fadaiting-camp` API

## 下一步

接下來最合理的實作順序是：

1. 先在主專案新增 `line_user_binding` 規劃
2. 定義主專案要提供給 LINE 專案的 API
3. 再建立 `fadaiting-line` 專案骨架
