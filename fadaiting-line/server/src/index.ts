import cors from "cors";
import express from "express";
import { config } from "./config.js";
import { isValidLineSignature } from "./lineSignature.js";
import { createRichMenu, replyLineMessage, setDefaultRichMenu, uploadRichMenuImage } from "./lineClient.js";
import { appendLineLog, readLineLogs } from "./lineLogStore.js";
import { buildKeywordReply, buildRichMenuBody, buildWelcomeMessages } from "./lineMessages.js";
import { postMainApi } from "./mainApi.js";
import { buildRichMenuImageBuffer } from "./richMenuImage.js";

const app = express();

app.use(cors({
  origin: config.webOrigin,
}));

app.get("/api/health", (_req, res) => {
  res.json({
    ok: true,
    service: "fadaiting-line-server",
    mainApiBaseUrl: config.mainApiBaseUrl,
  });
});

app.post("/webhook/line", express.text({ type: "*/*" }), async (req, res) => {
  const signature = req.header("X-Line-Signature");
  const rawBody = typeof req.body === "string" ? req.body : "";
  const verified = config.lineChannelSecret
    ? isValidLineSignature(signature, rawBody, config.lineChannelSecret)
    : false;

  if (config.lineChannelSecret && !verified) {
    await appendLineLog({
      type: "webhook",
      direction: "inbound",
      status: "failed",
      summary: "LINE signature 驗證失敗",
      payload: safeJsonParse(rawBody),
    });
    res.status(401).json({ ok: false, message: "LINE signature 驗證失敗" });
    return;
  }

  console.log("[LINE webhook]", {
    verified,
    hasSignature: Boolean(signature),
    bodyPreview: rawBody.slice(0, 300),
  });

  try {
    const body = rawBody ? JSON.parse(rawBody) : { events: [] };
    await appendLineLog({
      type: "webhook",
      direction: "inbound",
      status: "received",
      summary: `收到 ${body.events?.length || 0} 筆 LINE webhook event`,
      payload: body,
    });
    await handleLineEvents(body.events || []);
    res.json({
      ok: true,
      verified,
    });
  } catch (error: any) {
    console.error("[LINE webhook error]", error?.message || error);
    await appendLineLog({
      type: "webhook",
      direction: "inbound",
      status: "failed",
      summary: "LINE webhook 處理失敗",
      payload: safeJsonParse(rawBody),
      error: error?.message,
    });
    res.status(500).json({ ok: false, message: "LINE webhook 處理失敗" });
  }
});

app.use(express.json());

app.get("/api/line/logs", async (req, res) => {
  try {
    const limit = Number(req.query.limit || 50);
    const type = String(req.query.type || "");
    const logs = await readLineLogs(limit, type);
    res.json({
      ok: true,
      total: logs.length,
      logs,
    });
  } catch (error: any) {
    res.status(500).json({ ok: false, message: error?.message || "讀取 LINE log 失敗" });
  }
});

app.get("/api/line/menu-preview", (_req, res) => {
  res.json({
    richMenu: buildRichMenuBody(),
    flexMenu: buildKeywordReply("選單")[0],
  });
});

app.get("/api/line/rich-menu/image", async (_req, res) => {
  try {
    const imageBuffer = await buildRichMenuImageBuffer();
    res.setHeader("Content-Type", "image/png");
    res.setHeader("Cache-Control", "no-store");
    res.send(imageBuffer);
  } catch (error: any) {
    res.status(500).json({ ok: false, message: error?.message || "產生 Rich Menu 圖片失敗" });
  }
});

app.post("/api/line/rich-menu/create", async (req, res) => {
  try {
    const richMenu = req.body?.richMenu || buildRichMenuBody();
    const result = await createRichMenu(richMenu);
    res.json({
      ok: true,
      result,
    });
  } catch (error: any) {
    res.status(error?.response?.status || 500).json(error?.response?.data || { ok: false, message: error?.message || "建立 Rich Menu 失敗" });
  }
});

app.post("/api/line/rich-menu/publish", async (req, res) => {
  try {
    const richMenu = req.body?.richMenu || buildRichMenuBody();
    const shouldSetDefault = req.body?.setDefault !== false;
    const result: any = await createRichMenu(richMenu);
    const richMenuId = result?.richMenuId;
    if (!richMenuId) {
      res.status(502).json({ ok: false, message: "LINE 未回傳 richMenuId", result });
      return;
    }

    const imageBuffer = await buildRichMenuImageBuffer();
    await uploadRichMenuImage(richMenuId, imageBuffer);
    if (shouldSetDefault) {
      await setDefaultRichMenu(richMenuId);
    }

    res.json({
      ok: true,
      richMenuId,
      imageUploaded: true,
      defaultSet: shouldSetDefault,
    });
  } catch (error: any) {
    res.status(error?.response?.status || 500).json(error?.response?.data || { ok: false, message: error?.message || "發布 Rich Menu 失敗" });
  }
});

app.post("/api/line/rich-menu/:richMenuId/image", async (req, res) => {
  try {
    const richMenuId = req.params.richMenuId;
    if (!richMenuId) {
      res.status(400).json({ ok: false, message: "richMenuId 不得為空" });
      return;
    }
    const imageBuffer = await buildRichMenuImageBuffer();
    await uploadRichMenuImage(richMenuId, imageBuffer);
    res.json({ ok: true });
  } catch (error: any) {
    res.status(error?.response?.status || 500).json(error?.response?.data || { ok: false, message: error?.message || "上傳 Rich Menu 圖片失敗" });
  }
});

app.post("/api/line/rich-menu/default", async (req, res) => {
  try {
    const richMenuId = req.body?.richMenuId;
    if (!richMenuId) {
      res.status(400).json({ ok: false, message: "richMenuId 不得為空" });
      return;
    }
    await setDefaultRichMenu(richMenuId);
    res.json({ ok: true });
  } catch (error: any) {
    res.status(error?.response?.status || 500).json(error?.response?.data || { ok: false, message: error?.message || "設定預設 Rich Menu 失敗" });
  }
});

app.post("/api/main/line/bind/request", async (req, res) => {
  try {
    const data = await postMainApi("/api/public/line/bind/request", req.body);
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "success",
      summary: "主專案 bind/request 呼叫成功",
      payload: { request: req.body, response: data },
    });
    res.json(data);
  } catch (error: any) {
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "failed",
      summary: "主專案 bind/request 呼叫失敗",
      payload: { request: req.body },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    res.status(error?.response?.status || 500).json(error?.response?.data || { code: 1, message: "主專案 API 呼叫失敗" });
  }
});

app.post("/api/main/line/bind/confirm", async (req, res) => {
  try {
    const data = await postMainApi("/api/public/line/bind/confirm", req.body);
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "success",
      summary: "主專案 bind/confirm 呼叫成功",
      payload: { request: req.body, response: data },
    });
    res.json(data);
  } catch (error: any) {
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "failed",
      summary: "主專案 bind/confirm 呼叫失敗",
      payload: { request: req.body },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    res.status(error?.response?.status || 500).json(error?.response?.data || { code: 1, message: "主專案 API 呼叫失敗" });
  }
});

app.post("/api/main/line/booking/list", async (req, res) => {
  try {
    const data = await postMainApi("/api/public/line/booking/list", req.body);
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "success",
      summary: "主專案 booking/list 呼叫成功",
      payload: { request: req.body, response: data },
    });
    res.json(data);
  } catch (error: any) {
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "failed",
      summary: "主專案 booking/list 呼叫失敗",
      payload: { request: req.body },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    res.status(error?.response?.status || 500).json(error?.response?.data || { code: 1, message: "主專案 API 呼叫失敗" });
  }
});

app.post("/api/main/line/booking/detail", async (req, res) => {
  try {
    const data = await postMainApi("/api/public/line/booking/detail", req.body);
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "success",
      summary: "主專案 booking/detail 呼叫成功",
      payload: { request: req.body, response: data },
    });
    res.json(data);
  } catch (error: any) {
    await appendLineLog({
      type: "main-api",
      direction: "internal",
      status: "failed",
      summary: "主專案 booking/detail 呼叫失敗",
      payload: { request: req.body },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    res.status(error?.response?.status || 500).json(error?.response?.data || { code: 1, message: "主專案 API 呼叫失敗" });
  }
});

app.listen(config.port, () => {
  console.log(`fadaiting-line server listening on http://127.0.0.1:${config.port}`);
});

async function handleLineEvents(events: any[]) {
  for (const event of events) {
    const replyToken = event?.replyToken;
    await appendLineLog({
      type: "webhook",
      direction: "inbound",
      eventType: event?.type,
      sourceUserId: event?.source?.userId,
      replyToken,
      status: "received",
      summary: `處理 LINE event：${event?.type || "unknown"}`,
      payload: summarizeLineEvent(event),
    });
    if (!replyToken) {
      await appendLineLog({
        type: "reply",
        direction: "outbound",
        eventType: event?.type,
        sourceUserId: event?.source?.userId,
        status: "skipped",
        summary: "event 沒有 replyToken，略過回覆",
      });
      continue;
    }
    if (event.type === "follow") {
      await replyLineMessage(replyToken, buildWelcomeMessages());
      continue;
    }
    if (event.type === "message" && event.message?.type === "text") {
      await replyLineMessage(replyToken, buildKeywordReply(event.message.text || ""));
      continue;
    }
    if (event.type === "postback") {
      await replyLineMessage(replyToken, buildKeywordReply(event.postback?.data || "選單"));
    }
  }
}

function safeJsonParse(rawBody: string) {
  try {
    return rawBody ? JSON.parse(rawBody) : {};
  } catch {
    return { rawBodyPreview: rawBody.slice(0, 500) };
  }
}

function summarizeLineEvent(event: any) {
  return {
    type: event?.type,
    mode: event?.mode,
    timestamp: event?.timestamp,
    source: event?.source,
    message: event?.message,
    postback: event?.postback,
  };
}
