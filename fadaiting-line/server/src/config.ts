import dotenv from "dotenv";
import path from "node:path";

dotenv.config();

export const config = {
  port: Number(process.env.PORT || 9191),
  mainApiBaseUrl: (process.env.MAIN_API_BASE_URL || "http://127.0.0.1:9090/fadaiting-camp").replace(/\/$/, ""),
  lineChannelSecret: process.env.LINE_CHANNEL_SECRET || "",
  lineChannelAccessToken: process.env.LINE_CHANNEL_ACCESS_TOKEN || "",
  lineApiBaseUrl: (process.env.LINE_API_BASE_URL || "https://api.line.me").replace(/\/$/, ""),
  lineDataApiBaseUrl: (process.env.LINE_DATA_API_BASE_URL || "https://api-data.line.me").replace(/\/$/, ""),
  webOrigin: process.env.WEB_ORIGIN || "http://127.0.0.1:5174",
  webBaseUrl: (process.env.WEB_BASE_URL || process.env.WEB_ORIGIN || "http://127.0.0.1:5174").replace(/\/$/, ""),
  mainFrontendBaseUrl: (process.env.MAIN_FRONTEND_BASE_URL || "http://127.0.0.1:5173").replace(/\/$/, ""),
  lineLogDir: process.env.LINE_LOG_DIR || path.resolve("logs"),
};
