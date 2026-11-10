# Cookie、CSRF 與 LINE 身分驗證

2026-10-06，沿用 JWT／Redis、固定 ADMIN／STAFF 與 Vue API proxy。全部使用免費工具；LINE 官方帳號與手機實測仍待補。

## 後台登入

- JWT、CSRF cookie 都為 host-only、`Path=/`、`HttpOnly`、`SameSite=Lax`，不設定 Domain。
- `AUTH_COOKIE_SECURE` 預設 true。HTTPS 使用 `__Host-camp_access / __Host-camp_refresh / __Host-camp_csrf`，瀏覽器禁止子網域注入這些 cookie；本機 HTTP 明確設定 false，改用 `camp_access / camp_refresh / camp_csrf`。
- 舊 `accessToken / refreshToken` 不再接受，升級後重新登入。登出清除目前及舊 cookie 名稱，並輪替 CSRF token；舊或已被輪替的 refresh 不撤銷新 session。
- Access JWT 簽章效期為 15 分鐘，refresh 為 7 天。修正舊程式簽章時間與 cookie 時間不一致的問題；沒有保持登入時 cookie 為 browser session，JWT 到期仍不能使用。
- Refresh JTI 透過 Redis compare-and-set 原子輪替，舊 token 只可成功刷新一次；DB `auth_version`、帳號狀態與固定角色檢查維持。
- Spring Security 使用 stateless 模式，不新增 HttpSession；API 失敗不自動呼叫登出撤銷其他分頁的 session。

這些 cookie 屬性是後台安全設定，不代表目前已在使用者正式網域驗收 TLS。不要靠自行傳入 `X-Forwarded-Proto` 改變 Secure；本程式只使用明確設定。正式環境需 HTTPS 反向代理並封鎖可繞過代理的後端入口。Nginx 目前的 HTTP 8080 仍是開發／展示入口，Secure=true 時不能在普通 HTTP 登入。

## CSRF

所有非公開的 POST／PUT／PATCH／DELETE，含登入、refresh 與 logout，都須提交 cookie 對應的 `X-CSRF-TOKEN`。固定角色檢查仍獨立執行，CSRF token 不授予權限。

1. 同源呼叫 `GET /api/auth/csrf`，取得 `data.token / data.headerName`；回應 `Cache-Control: no-store`，並建立 HttpOnly CSRF cookie。
2. 保留 cookie，寫入請求帶 `X-CSRF-TOKEN`。跨站表單、只複製 header 未帶 cookie、只帶 cookie 未帶正確 header 都會被拒絕。
3. 成功登入會輪替 token。Vue 只在記憶體保存 token，登入／登出後清快取；多個請求共用一次載入及 refresh。
4. 若 CSRF 失效回 HTTP 403、`errorCode=CSRF_INVALID`，前端取得新 token 後最多重試一次。此失敗發生在商業寫入前；一般角色 403 不自動重試、不轉到登入頁。

`/api/public/**`（以及既有 `/public/**` 別名）不靠後台 cookie 授權，因此不套用後台 CSRF。綠界 callback 仍核對簽章／商店／金額，LINE 身分接口仍核對 ID token，LINE webhook 保留原簽章策略。公開預約、mock 付款與電話查單的限流及正式金流模式仍有自己的待補事項，不能因加上後台 CSRF 而視為已完成。

## LINE API 合約

`/api/public/line/bind/request`、`bind/confirm`、`booking/list`、`booking/detail` 全部新增必填 `idToken`（最多 8192 字元）。不再接受前端自報的 `lineUserId / displayName / pictureUrl` 作為身分或 profile。伺服器將 ID token 送至固定 HTTPS 官方 `/oauth2/v2.1/verify`，再核對 `iss / aud / sub / exp / iat`；不在本機只 decode JWT 後放行，不提供線上展示的偽身分 fallback。

需 `LINE_IDENTITY_ENABLED=true` 與 `LINE_LOGIN_CHANNEL_ID`。這是 **LINE Login／LIFF 的 Login Channel ID**，不是 Messaging API 的 Channel ID 或 Channel Access Token。不需要購買 LLM 或第三方身分服務。未啟用或未設定回 503；無效／過期／錯誤 audience 回 401；provider 無法使用時回 503，訊息不包含 token 或 provider 原始回應。

網路 client 沿用環境 HTTPS proxy 與 Java CA 信任，固定官方目的地、禁止 redirect，配置連線／請求 timeout 與回應大小限制；不將 token 放在 URL、DB、操作歷程或錯誤訊息。

| 路徑（POST） | JSON |
| --- | --- |
| `/public/line/bind/request` | `idToken, bookingNo, contactPhone` |
| `/public/line/bind/confirm` | `idToken, bindToken` |
| `/public/line/booking/list` | `idToken` |
| `/public/line/booking/detail` | `idToken, bookingId` |

LIFF 呼叫方需要用 `liff.getIDToken()`，開啟 `openid` scope 並透過 POST body 傳送；不要傳 `liff.getProfile().userId` 代替 token，也不要將 ID token 放進 query string。本輪只修改 `fadaiting-camp`；獨立 `fadaiting-line` 呼叫方還需在其後續工作中更新，舊合約會被主服務拒絕。

## 綁定與查單權限

- 申請先核驗 token，再核對預約編號及聯絡電話；10 分鐘綁定 token 固定給已驗證的 LINE subject。
- 確認時重新核驗，同一 subject 才能消耗 token；訂單與綁定 row lock、交易及唯一索引防止重複確認或兩個帳號競爭。
- 每個預約只能有一筆已核驗的 active 綁定；已綁定其他帳號時拒絕靜默換綁，需要營區協助。管理端解除／移轉流程尚未實作。
- 同一 LINE 帳號可以逐筆明確綁定多個預約。列表只回傳這些預約；不再以聯絡電話擴大到其他未綁定訂單，即使電話相同也不能讀其明細。
- 007 migration 新增 `identity_verified_at timestamptz`。舊綁定資料保留、不追認身分，缺少核驗時間者不授予查單權力，需重新綁定。成功綁定同訂單時舊未核驗 active 記錄改 inactive，同一交易失敗則全部回滾。
- ID token 每次重新核驗，不快取成只靠 userId 的伺服器 session。旅客只取得安全公開摘要，不取得退款憑證或內部歷程。

## 升級、金鑰與驗收

既有 DB 先備份，依序補缺少的 `002` 到 `007_line_identity.sql`，再部署新版前後端並重新登入；不刪 volume。007 可重跑，不補驗證時間、不重配帳號。

Dev Compose 預設 `AUTH_COOKIE_SECURE=false`，prod／直接 Java 預設 true；`.env` 的明確設定優先。從 `.env.example` 複製後，HTTP 開發要自行改 false。正式 HTTPS 必須 true。

`JWT_SECRET` 仍要求私有至少 32 字元金鑰。輪替時設定新金鑰並重啟全部後端，同時更新所有部署副本；舊簽章無效，使用者重新登入。此版沒有多金鑰平滑輪替，不能混用新舊金鑰啟動多個副本。

Cloud 使用真實隔離 PostgreSQL／Redis 驗證，LINE provider 結果由 **只存在 test classpath 的合成 adapter** 提供；正式程式沒有測試身分開關或可替換驗證網址。這些結果證明接口、claim 檢查與綁定交易，不代表已用真實 LINE token／手機驗收。紀錄見 [本輪測試](docs/verification/auth-line-security-2026-10-06.json)，真實帳號、手機、TLS 網域及本機升級見 [待補文件](CLOUD_LOCAL_FOLLOWUP.md)。
