# fadaiting-line

`fadaiting-line` 是發呆亭露營區的獨立 LINE 專案骨架，負責：

- LINE Messaging API webhook
- LIFF / LINE MINI App 前端原型
- 呼叫主專案 `fadaiting-camp` 的 LINE 整合 API

正式 LINE 官方帳號、LIFF、Rich Menu 串接檢查請搭配：

- [../LINE_OFFICIAL_ACCOUNT_SETUP.md](C:/Users/SwaggyLaptop/Documents/Codex/2026-05-30/elpsy-deng-fadaiting-camp-git-https/LINE_OFFICIAL_ACCOUNT_SETUP.md)

目前定位：

- 獨立專案骨架
- 可本機啟動
- 可對接主專案已完成的：
  - `bind/request`
  - `bind/confirm`
  - `booking/list`
  - `booking/detail`

## 結構

```text
fadaiting-line/
  server/   Node.js / Express webhook 與 API proxy
  web/      Vue 3 / Vite LIFF 原型頁
```

## 啟動順序

### 1. 先確認主專案後端已啟動

預設：

```text
http://127.0.0.1:9090/fadaiting-camp
```

### 2. 啟動 server

```powershell
cd fadaiting-line\server
copy .env.example .env
npm install
npm run dev
```

預設：

```text
http://127.0.0.1:9191
```

### 3. 啟動 web

```powershell
cd fadaiting-line\web
copy .env.example .env
npm install
npm run dev
```

預設：

```text
http://127.0.0.1:5174
```

## 目前功能

### server

- `GET /api/health`
- `POST /webhook/line`
- `GET /api/line/logs`
- `GET /api/line/menu-preview`
- `GET /api/line/rich-menu/image`
- `POST /api/line/rich-menu/create`
- `POST /api/line/rich-menu/:richMenuId/image`
- `POST /api/line/rich-menu/default`
- `POST /api/line/rich-menu/publish`
- `POST /api/main/line/bind/request`
- `POST /api/main/line/bind/confirm`
- `POST /api/main/line/booking/list`
- `POST /api/main/line/booking/detail`

目前 webhook 支援：

- `follow`：回覆歡迎訊息與主選單 Flex Message
- 文字訊息：
  - `選單`
  - `營區資訊`
  - `我要預約`
  - `查我的訂單`
  - `公告`
  - `FAQ`

### web

- 綁定請求表單
- 綁定確認表單
- 我的訂單列表查詢
- 單筆訂單明細查詢
- LIFF SDK 初始化骨架
- 設定 `VITE_LIFF_ID` 後可自動取得 LINE profile，並帶入 `lineUserId / displayName / pictureUrl`
- 未設定 `VITE_LIFF_ID` 時保留本機手動測試模式

## 環境變數

### server

- `PORT`
- `MAIN_API_BASE_URL`
- `MAIN_FRONTEND_BASE_URL`
- `LINE_CHANNEL_SECRET`
- `LINE_CHANNEL_ACCESS_TOKEN`
- `LINE_API_BASE_URL`
- `LINE_DATA_API_BASE_URL`
- `WEB_ORIGIN`
- `WEB_BASE_URL`
- `LINE_LOG_DIR`

### web

- `VITE_LINE_API_BASE_URL`
- `VITE_LIFF_ID`

## LIFF 模式

本機測試時可以先不填 `VITE_LIFF_ID`，頁面會顯示「本機測試模式」，仍可手動輸入 `lineUserId` 測試主專案 API。

若要進入正式 LIFF 流程：

1. 在 LINE Developers 建立 LIFF app
2. 將 LIFF Endpoint URL 指向 `fadaiting-line/web` 對外網址
3. 將取得的 LIFF ID 填入 `VITE_LIFF_ID`
4. 重新啟動 web

## Rich Menu / Flex Message

目前 server 已有第一版 Flex Message 與 Rich Menu JSON 產生器。

本機查看：

```powershell
Invoke-RestMethod http://127.0.0.1:9191/api/line/menu-preview
```

本機查看 Rich Menu 圖片：

```text
http://127.0.0.1:9191/api/line/rich-menu/image
```

正式建立 Rich Menu 前，請先確認：

- `LINE_CHANNEL_ACCESS_TOKEN` 已填入
- `WEB_BASE_URL` 是可公開存取的 HTTPS LIFF 網址
- `MAIN_FRONTEND_BASE_URL` 是可公開存取的 HTTPS 官網網址

正式發布可用：

```powershell
Invoke-RestMethod `
  -Uri http://127.0.0.1:9191/api/line/rich-menu/publish `
  -Method Post `
  -ContentType 'application/json' `
  -Body '{"setDefault":true}'
```

這會依序：

1. 建立 Rich Menu
2. 產生並上傳 Rich Menu PNG 圖片
3. 將 Rich Menu 設為預設選單

若只要分步操作：

- `POST /api/line/rich-menu/create`
- `POST /api/line/rich-menu/{richMenuId}/image`
- `POST /api/line/rich-menu/default`

## LINE event / message log

server 會將 webhook、reply、Rich Menu 與主專案 API proxy 事件寫入 JSONL：

```text
fadaiting-line/server/logs/line-events.jsonl
```

查詢最近紀錄：

```powershell
Invoke-RestMethod http://127.0.0.1:9191/api/line/logs
```

查詢特定類型：

```powershell
Invoke-RestMethod "http://127.0.0.1:9191/api/line/logs?type=webhook&limit=20"
```

支援類型：

- `webhook`
- `reply`
- `rich-menu`
- `main-api`

log 會自動遮罩 `phone`、`token`、`secret`、`authorization` 相關欄位。

## 備註

- 這一版先是獨立專案骨架，不是正式 LIFF 完整版
- 目前 web 頁面已接 LIFF SDK 骨架，正式使用仍需要 LINE Developers 的 LIFF ID
- 目前 webhook 已可依 follow / 文字訊息回覆第一版 Flex Message
- Rich Menu 已有圖片產生、圖片上傳與一鍵發布骨架；正式使用仍需 LINE Channel Access Token 與對外 HTTPS URL
- LINE event / message log 已有 JSONL 骨架，正式部署時可再接 DB 或後台頁
