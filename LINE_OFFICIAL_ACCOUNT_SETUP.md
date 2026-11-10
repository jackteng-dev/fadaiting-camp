# LINE 官方帳號設定檢查表

本文件整理 `fadaiting-line` 從本機開發走到正式 LINE 官方帳號串接時，需要準備的設定、網址、檢查項目與發布順序。

## 目前專案定位

- 主專案：`fadaiting-camp`
  - 官網
  - 後台
  - 訂單 / 付款 / 通知 / 營區資料
  - 提供 LINE 整合 API
- LINE 專案：`fadaiting-line`
  - LINE webhook
  - Flex Message
  - Rich Menu
  - LIFF 原型頁
  - 呼叫主專案 API

## 目前本機網址

```text
主專案前端：http://127.0.0.1:5173
主專案後端：http://127.0.0.1:9090/fadaiting-camp
LINE server：http://127.0.0.1:9191
LINE web：http://127.0.0.1:5174
```

本機網址只能用於開發測試。正式 LINE webhook、LIFF、Rich Menu URI 都需要可公開存取的 HTTPS 網址。

## 正式需要的公開網址

正式上線前至少需要：

- 主官網 HTTPS URL
  - 範例：`https://camp.example.com`
- 主後端 HTTPS URL
  - 範例：`https://api.example.com/fadaiting-camp`
- LINE server HTTPS URL
  - 範例：`https://line.example.com`
- LIFF web HTTPS URL
  - 範例：`https://line-app.example.com`

對應用途：

- Webhook URL：`https://line.example.com/webhook/line`
- LIFF Endpoint URL：`https://line-app.example.com`
- Rich Menu 的預約 / 查單入口：`https://line-app.example.com`
- Rich Menu 的官網 / 公告 / FAQ 入口：`https://camp.example.com`

## LINE Developers 需要建立的項目

### Messaging API Channel

需要取得：

- Channel secret
- Channel access token

需要設定：

- Webhook URL：`https://line.example.com/webhook/line`
- 啟用 webhook
- 關閉或調整自動回應，避免和 webhook 回覆重疊

### LIFF App

需要取得：

- LIFF ID

需要設定：

- Endpoint URL：`https://line-app.example.com`
- Scope 建議至少包含：
  - `profile`
  - `openid`，若後續要更完整的登入驗證
- LIFF 尺寸可先用：
  - `Full`

## `fadaiting-line/server` 環境變數

正式 `.env` 建議：

```env
PORT=9191
MAIN_API_BASE_URL=https://api.example.com/fadaiting-camp
MAIN_FRONTEND_BASE_URL=https://camp.example.com
LINE_CHANNEL_SECRET=你的 Channel secret
LINE_CHANNEL_ACCESS_TOKEN=你的 Channel access token
LINE_API_BASE_URL=https://api.line.me
LINE_DATA_API_BASE_URL=https://api-data.line.me
WEB_ORIGIN=https://line-app.example.com
WEB_BASE_URL=https://line-app.example.com
LINE_LOG_DIR=logs
```

注意：

- `LINE_CHANNEL_SECRET` 用於 webhook signature 驗證
- `LINE_CHANNEL_ACCESS_TOKEN` 用於回覆訊息、建立 Rich Menu、上傳 Rich Menu 圖片
- `LINE_DATA_API_BASE_URL` 是 Rich Menu 圖片上傳用
- `WEB_BASE_URL` 會被寫進 Flex Message / Rich Menu 的 LIFF 入口
- `MAIN_FRONTEND_BASE_URL` 會被寫進營區資訊、公告、FAQ 入口

## `fadaiting-line/web` 環境變數

正式 `.env` 建議：

```env
VITE_LINE_API_BASE_URL=https://line.example.com
VITE_LIFF_ID=你的 LIFF ID
```

注意：

- `VITE_LIFF_ID` 未設定時，web 會保留本機手動測試模式
- `VITE_LIFF_ID` 設定後，web 會初始化 LIFF SDK 並嘗試取得 LINE profile

## 啟動與健康檢查

### server health

```powershell
Invoke-RestMethod https://line.example.com/api/health
```

期待：

```json
{
  "ok": true,
  "service": "fadaiting-line-server"
}
```

### Rich Menu preview

```powershell
Invoke-RestMethod https://line.example.com/api/line/menu-preview
```

期待：

- 回傳 `richMenu`
- 回傳 `flexMenu`
- URI 已經是正式 HTTPS 網址，不是 `127.0.0.1`

### Rich Menu image

```text
https://line.example.com/api/line/rich-menu/image
```

期待：

- 顯示 2500x843 PNG
- 六格選單文字正常
- 與 Rich Menu area 座標一致

### LINE event / message log

```powershell
Invoke-RestMethod https://line.example.com/api/line/logs
```

可加上：

```text
?type=webhook&limit=20
?type=reply&limit=20
?type=rich-menu&limit=20
?type=main-api&limit=20
```

期待：

- webhook event 有 inbound log
- reply 有 outbound log
- `LINE_CHANNEL_ACCESS_TOKEN` 未填時，reply 會記錄為 `skipped`
- 主專案 API proxy 成功 / 失敗會記錄為 `main-api`
- `phone`、`token`、`secret`、`authorization` 相關欄位會被遮罩

## Rich Menu 發布流程

正式 token 與 HTTPS URL 都設定完成後，可用：

```powershell
Invoke-RestMethod `
  -Uri https://line.example.com/api/line/rich-menu/publish `
  -Method Post `
  -ContentType 'application/json' `
  -Body '{"setDefault":true}'
```

這會依序：

1. 呼叫 LINE API 建立 Rich Menu
2. 產生本專案的 Rich Menu PNG
3. 上傳圖片到 LINE Data API
4. 設為官方帳號預設 Rich Menu

若只想先建立但不設成預設：

```powershell
Invoke-RestMethod `
  -Uri https://line.example.com/api/line/rich-menu/publish `
  -Method Post `
  -ContentType 'application/json' `
  -Body '{"setDefault":false}'
```

## Webhook 測試流程

### LINE Developers 測試

在 LINE Developers 後台按下 Verify。

期待：

- webhook 回應 200
- server log 可看到 webhook request
- 若 `LINE_CHANNEL_SECRET` 正確，signature 驗證應通過

### 實際聊天室測試

加入官方帳號後測：

- follow 事件
  - 應收到歡迎訊息與主選單 Flex Message
- 輸入 `選單`
  - 應收到主選單 Flex Message
- 輸入 `我要預約`
  - 應收到預約入口
- 輸入 `查我的訂單`
  - 應收到查單入口
- 輸入 `FAQ`
  - 應收到主選單或 FAQ 入口

## LIFF 測試流程

1. 從 LINE 內開啟 LIFF URL
2. 確認 LIFF 可以登入
3. 確認頁面能取得：
   - `lineUserId`
   - `displayName`
   - `pictureUrl`
4. 用既有訂單編號與聯絡電話建立綁定請求
5. 確認 `bindToken` 自動帶入綁定確認表單
6. 完成綁定
7. 查詢我的訂單
8. 查詢單筆訂單明細

## 主專案 API 檢查

`fadaiting-line` 目前會呼叫主專案：

- `POST /api/public/line/bind/request`
- `POST /api/public/line/bind/confirm`
- `POST /api/public/line/booking/list`
- `POST /api/public/line/booking/detail`

正式部署時要確認：

- 主後端可從 LINE server 存取
- CORS 不阻擋 `fadaiting-line/web`
- `line_user_binding` 資料表存在
- 主專案後端與 DB 時區設定正確

## 安全注意事項

- Channel secret / access token 不要提交到 Git
- Webhook 必須驗證 `X-Line-Signature`
- `lineUserId` 不應單獨視為已完成真人驗證
- 目前第一版綁定用 `bookingNo + contactPhone`，正式版建議再補：
  - 手機驗證碼
  - 訂單編號 + 手機末碼
  - LIFF 登入後二次驗證
- Rich Menu / Flex Message 裡不要放內部後台 URL

## 常見問題

### LINE webhook Verify 失敗

檢查：

- LINE server 是否可公開連線
- Webhook URL 是否為 HTTPS
- `/webhook/line` 是否回應 200
- `LINE_CHANNEL_SECRET` 是否正確
- 反向代理是否保留原始 request body
- `/api/line/logs?type=webhook` 是否有收到事件紀錄

### LIFF 無法取得 profile

檢查：

- `VITE_LIFF_ID` 是否正確
- LIFF Endpoint URL 是否和目前網址一致
- 是否從 LINE 內開啟
- Scope 是否包含 `profile`

### Rich Menu 按鈕開不了

檢查：

- `WEB_BASE_URL` 是否為 HTTPS
- `MAIN_FRONTEND_BASE_URL` 是否為 HTTPS
- Rich Menu 是否重新發布
- Rich Menu area URI 是否仍是本機 `127.0.0.1`

### Rich Menu 圖片上傳失敗

檢查：

- `LINE_CHANNEL_ACCESS_TOKEN` 是否有效
- `LINE_DATA_API_BASE_URL` 是否為 `https://api-data.line.me`
- 圖片是否為 PNG
- 圖片尺寸是否為 `2500x843`

## 目前仍待完成

- 正式 LINE Channel 實測
- 正式 LIFF ID 實測
- Rich Menu 正式發布測試
- LINE user 綁定二次驗證策略
- LIFF 內預約流程正式頁
- LINE 推播通知
- webhook event log / message log 後台查詢頁
