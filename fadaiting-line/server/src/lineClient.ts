import axios from "axios";
import { config } from "./config.js";
import { appendLineLog } from "./lineLogStore.js";
import type { LineReplyMessage } from "./lineMessages.js";

export async function replyLineMessage(replyToken: string, messages: LineReplyMessage[]) {
  if (!config.lineChannelAccessToken) {
    console.log("[LINE reply skipped] LINE_CHANNEL_ACCESS_TOKEN is empty", { replyToken, messages });
    await appendLineLog({
      type: "reply",
      direction: "outbound",
      replyToken,
      status: "skipped",
      summary: "LINE_CHANNEL_ACCESS_TOKEN 尚未設定，略過 reply",
      payload: { messages },
    });
    return;
  }
  try {
    await axios.post(
      `${config.lineApiBaseUrl}/v2/bot/message/reply`,
      {
        replyToken,
        messages,
      },
      {
        headers: {
          Authorization: `Bearer ${config.lineChannelAccessToken}`,
          "Content-Type": "application/json",
        },
        timeout: 10000,
      },
    );
    await appendLineLog({
      type: "reply",
      direction: "outbound",
      replyToken,
      status: "success",
      summary: `已回覆 ${messages.length} 則 LINE 訊息`,
      payload: { messages },
    });
  } catch (error: any) {
    await appendLineLog({
      type: "reply",
      direction: "outbound",
      replyToken,
      status: "failed",
      summary: "LINE reply 發送失敗",
      payload: { messages },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    throw error;
  }
}

export async function createRichMenu(richMenuBody: unknown) {
  if (!config.lineChannelAccessToken) {
    throw new Error("LINE_CHANNEL_ACCESS_TOKEN 尚未設定");
  }
  try {
    const response = await axios.post(
      `${config.lineApiBaseUrl}/v2/bot/richmenu`,
      richMenuBody,
      {
        headers: {
          Authorization: `Bearer ${config.lineChannelAccessToken}`,
          "Content-Type": "application/json",
        },
        timeout: 10000,
      },
    );
    await appendLineLog({
      type: "rich-menu",
      direction: "outbound",
      status: "success",
      summary: "已建立 LINE Rich Menu",
      payload: { request: richMenuBody, response: response.data },
    });
    return response.data;
  } catch (error: any) {
    await appendLineLog({
      type: "rich-menu",
      direction: "outbound",
      status: "failed",
      summary: "建立 LINE Rich Menu 失敗",
      payload: { request: richMenuBody },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    throw error;
  }
}

export async function setDefaultRichMenu(richMenuId: string) {
  if (!config.lineChannelAccessToken) {
    throw new Error("LINE_CHANNEL_ACCESS_TOKEN 尚未設定");
  }
  try {
    await axios.post(
      `${config.lineApiBaseUrl}/v2/bot/user/all/richmenu/${richMenuId}`,
      {},
      {
        headers: {
          Authorization: `Bearer ${config.lineChannelAccessToken}`,
        },
        timeout: 10000,
      },
    );
    await appendLineLog({
      type: "rich-menu",
      direction: "outbound",
      status: "success",
      summary: "已設定預設 LINE Rich Menu",
      payload: { richMenuId },
    });
  } catch (error: any) {
    await appendLineLog({
      type: "rich-menu",
      direction: "outbound",
      status: "failed",
      summary: "設定預設 LINE Rich Menu 失敗",
      payload: { richMenuId },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    throw error;
  }
}

export async function uploadRichMenuImage(richMenuId: string, imageBuffer: Buffer) {
  if (!config.lineChannelAccessToken) {
    throw new Error("LINE_CHANNEL_ACCESS_TOKEN 尚未設定");
  }
  try {
    await axios.post(
      `${config.lineDataApiBaseUrl}/v2/bot/richmenu/${richMenuId}/content`,
      imageBuffer,
      {
        headers: {
          Authorization: `Bearer ${config.lineChannelAccessToken}`,
          "Content-Type": "image/png",
        },
        timeout: 15000,
      },
    );
    await appendLineLog({
      type: "rich-menu",
      direction: "outbound",
      status: "success",
      summary: "已上傳 LINE Rich Menu 圖片",
      payload: { richMenuId, imageBytes: imageBuffer.length },
    });
  } catch (error: any) {
    await appendLineLog({
      type: "rich-menu",
      direction: "outbound",
      status: "failed",
      summary: "上傳 LINE Rich Menu 圖片失敗",
      payload: { richMenuId, imageBytes: imageBuffer.length },
      error: error?.response?.data ? JSON.stringify(error.response.data) : error?.message,
    });
    throw error;
  }
}
