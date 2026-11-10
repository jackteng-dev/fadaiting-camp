import fs from "node:fs/promises";
import path from "node:path";
import { config } from "./config.js";

export type LineLogType = "webhook" | "reply" | "rich-menu" | "main-api";

export interface LineLogEntry {
  id?: string;
  type: LineLogType;
  direction: "inbound" | "outbound" | "internal";
  eventType?: string;
  sourceUserId?: string;
  replyToken?: string;
  status: "received" | "skipped" | "success" | "failed";
  summary: string;
  payload?: unknown;
  error?: string;
  createdAt?: string;
}

export async function appendLineLog(entry: LineLogEntry) {
  const now = new Date();
  const normalized: LineLogEntry = {
    ...entry,
    payload: sanitizePayload(entry.payload),
    id: `${now.getTime()}-${Math.random().toString(36).slice(2, 10)}`,
    createdAt: now.toISOString(),
  };
  await fs.mkdir(config.lineLogDir, { recursive: true });
  await fs.appendFile(getLogFilePath(), `${JSON.stringify(normalized)}\n`, "utf8");
}

export async function readLineLogs(limit = 50, type = "") {
  try {
    const content = await fs.readFile(getLogFilePath(), "utf8");
    return content
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => JSON.parse(line) as LineLogEntry)
      .filter((entry) => !type || entry.type === type)
      .slice(-Math.max(1, Math.min(limit, 200)))
      .reverse();
  } catch (error: any) {
    if (error?.code === "ENOENT") {
      return [];
    }
    throw error;
  }
}

function getLogFilePath() {
  return path.join(config.lineLogDir, "line-events.jsonl");
}

function sanitizePayload(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(sanitizePayload);
  }
  if (!value || typeof value !== "object") {
    return value;
  }
  const result: Record<string, unknown> = {};
  for (const [key, item] of Object.entries(value)) {
    if (isSensitiveKey(key)) {
      result[key] = maskValue(item);
    } else {
      result[key] = sanitizePayload(item);
    }
  }
  return result;
}

function isSensitiveKey(key: string) {
  const normalized = key.toLowerCase();
  return normalized.includes("phone")
    || normalized.includes("token")
    || normalized.includes("secret")
    || normalized.includes("authorization");
}

function maskValue(value: unknown) {
  if (typeof value !== "string") {
    return value == null ? "" : "***";
  }
  if (value.length <= 4) {
    return "***";
  }
  return `${value.slice(0, 4)}***${value.slice(-2)}`;
}
