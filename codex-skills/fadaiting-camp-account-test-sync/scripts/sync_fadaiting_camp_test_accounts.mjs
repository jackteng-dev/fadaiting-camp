import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) {
  const key = process.argv[index];
  const value = process.argv[index + 1];
  if (!key?.startsWith("--") || value === undefined) {
    throw new Error(`Invalid argument near ${key || "(empty)"}`);
  }
  args.set(key.slice(2), value);
}

const projectRoot = args.get("project") || "D:/Project/fadaiting-camp";
const username = args.get("username");
const password = args.get("password");
const artifactToolPath = path.join(
  projectRoot,
  ".codex-runtime",
  "node_modules",
  "@oai",
  "artifact-tool",
  "dist",
  "artifact_tool.mjs",
);
const { SpreadsheetFile, Workbook } = await import(pathToFileURL(artifactToolPath).href);

if (!username || !password) {
  throw new Error("Missing --username or --password");
}

const nowDate = new Date();
const now = nowDate.toISOString().slice(0, 10);
const versionTag = [
  nowDate.getFullYear(),
  String(nowDate.getMonth() + 1).padStart(2, "0"),
  String(nowDate.getDate()).padStart(2, "0"),
  String(nowDate.getHours()).padStart(2, "0"),
  String(nowDate.getMinutes()).padStart(2, "0"),
  String(nowDate.getSeconds()).padStart(2, "0"),
].join(".");
const account = {
  purpose: args.get("purpose") || "本機測試帳號",
  username,
  password,
  role: args.get("role") || "",
  loginUrl: args.get("login-url") || "http://localhost:5173/admin/login",
  status: args.get("status") || "啟用",
  note: args.get("note") || "本機開發測試帳號",
  updatedDate: now,
};
const loginUrl = new URL(account.loginUrl);
const loginOrigins = Array.from(
  new Set([
    loginUrl.origin,
    loginUrl.hostname === "localhost"
      ? `${loginUrl.protocol}//127.0.0.1${loginUrl.port ? `:${loginUrl.port}` : ""}`
      : loginUrl.hostname === "127.0.0.1"
        ? `${loginUrl.protocol}//localhost${loginUrl.port ? `:${loginUrl.port}` : ""}`
        : null,
  ].filter(Boolean)),
);
const userscriptBaseUrl = args.get("userscript-base-url") || loginUrl.origin;
const userscriptInstallUrl = `${userscriptBaseUrl}/local-userscripts/admin-login-autofill.user.js`;

const dbDir = path.join(projectRoot, "db");
const publicScriptDir = path.join(projectRoot, "frontend", "public", "local-userscripts");
const jsonPath = path.join(dbDir, "test-accounts-plaintext.json");
const xlsxPath = path.join(dbDir, "test-accounts-plaintext.xlsx");
const userscriptPath = path.join(publicScriptDir, "admin-login-autofill.user.js");
const runtimeScriptPath = path.join(publicScriptDir, "admin-login-autofill.runtime.js");
const runtimeScriptFileUrl = pathToFileURL(runtimeScriptPath).href;

await fs.mkdir(dbDir, { recursive: true });
await fs.mkdir(publicScriptDir, { recursive: true });

let accounts = [];
try {
  accounts = JSON.parse(await fs.readFile(jsonPath, "utf8"));
  if (!Array.isArray(accounts)) accounts = [];
} catch {
  accounts = [];
}

const existingIndex = accounts.findIndex((item) => item.username === username);
if (existingIndex >= 0) {
  accounts[existingIndex] = { ...accounts[existingIndex], ...account };
} else {
  accounts.push(account);
}
accounts.sort((left, right) => left.username.localeCompare(right.username));

await fs.writeFile(jsonPath, `${JSON.stringify(accounts, null, 2)}\n`, "utf8");

const workbook = Workbook.create();
const sheet = workbook.worksheets.add("測試帳號");
sheet.showGridLines = false;

const headers = ["用途", "帳號", "明文密碼", "角色", "登入入口", "狀態", "注意事項", "更新日期"];
const rows = accounts.map((item) => [
  item.purpose,
  item.username,
  item.password,
  item.role,
  item.loginUrl,
  item.status,
  item.note,
  item.updatedDate,
]);

sheet.getRangeByIndexes(0, 0, 1, headers.length).values = [headers];
if (rows.length > 0) {
  sheet.getRangeByIndexes(1, 0, rows.length, headers.length).values = rows;
}

const usedRows = Math.max(2, rows.length + 1);
sheet.getRangeByIndexes(0, 0, 1, headers.length).format = {
  fill: "#1F4E5F",
  font: { bold: true, color: "#FFFFFF" },
  wrapText: true,
};
sheet.getRangeByIndexes(1, 0, usedRows - 1, headers.length).format = {
  wrapText: true,
};
sheet.getRangeByIndexes(0, 0, usedRows, headers.length).format.borders = {
  insideHorizontal: { style: "continuous", color: "#D9E2E8" },
  insideVertical: { style: "continuous", color: "#D9E2E8" },
  edgeTop: { style: "continuous", color: "#9FB3BE" },
  edgeBottom: { style: "continuous", color: "#9FB3BE" },
  edgeLeft: { style: "continuous", color: "#9FB3BE" },
  edgeRight: { style: "continuous", color: "#9FB3BE" },
};

const widths = [150, 130, 130, 100, 230, 90, 360, 110];
widths.forEach((width, index) => {
  sheet.getRangeByIndexes(0, index, usedRows, 1).format.columnWidthPx = width;
});
sheet.getRange("1:1").format.rowHeightPx = 28;
sheet.freezePanes.freezeRows(1);

const passwordMap = Object.fromEntries(accounts.map((item) => [item.username, item.password]));
const matchLines = loginOrigins
  .map((origin) => `// @match        ${origin}/admin/login*`)
  .join("\n");
const userscript = `// ==UserScript==
// @name         發呆亭後台登入帳號自動填密碼
// @namespace    local.fa-dai-ting.admin
// @version      ${versionTag}
// @description  本機開發用：在後台登入頁輸入測試帳號後，自動填入對應密碼。
${matchLines}
// @require      ${runtimeScriptFileUrl}
// @downloadURL  ${userscriptInstallUrl}
// @updateURL    ${userscriptInstallUrl}
// @grant        none
// ==/UserScript==
`;

const runtimeScript = `(function () {
  "use strict";

  const ACCOUNT_PASSWORDS = ${JSON.stringify(passwordMap, null, 4)};
  const DEFAULT_USERNAME = "admin001";
  const LEGACY_DEFAULT_ACCOUNTS = new Set(["swaggydickson"]);

  const dispatchInput = (input) => {
    input.dispatchEvent(new Event("input", { bubbles: true }));
    input.dispatchEvent(new Event("change", { bubbles: true }));
  };

  const fillPassword = () => {
    const usernameInput = document.querySelector('input[placeholder="請輸入用戶名"]');
    const passwordInput = document.querySelector('input[placeholder="請輸入密碼"]');
    if (!usernameInput || !passwordInput) return;

    if (!usernameInput.value.trim()) {
      usernameInput.value = DEFAULT_USERNAME;
      dispatchInput(usernameInput);
    }

    const username = usernameInput.value.trim();
    if (LEGACY_DEFAULT_ACCOUNTS.has(username)) {
      usernameInput.value = "";
      passwordInput.value = "";
      dispatchInput(usernameInput);
      dispatchInput(passwordInput);
      return;
    }

    const password = ACCOUNT_PASSWORDS[username];
    if (!password) return;

    if (passwordInput.value !== password) {
      passwordInput.value = password;
      dispatchInput(passwordInput);
    }
  };

  const bind = () => {
    const usernameInput = document.querySelector('input[placeholder="請輸入用戶名"]');
    if (!usernameInput || usernameInput.dataset.accountAutofillBound === "true") return;

    usernameInput.dataset.accountAutofillBound = "true";
    usernameInput.addEventListener("input", fillPassword);
    usernameInput.addEventListener("change", fillPassword);
    usernameInput.addEventListener("blur", fillPassword);
    fillPassword();
  };

  bind();
  new MutationObserver(bind).observe(document.documentElement, {
    childList: true,
    subtree: true,
  });
})();
`;

await fs.writeFile(userscriptPath, userscript, "utf8");
await fs.writeFile(runtimeScriptPath, runtimeScript, "utf8");
const xlsx = await SpreadsheetFile.exportXlsx(workbook);
try {
  await xlsx.save(xlsxPath);
} catch (error) {
  if (error?.code === "EBUSY") {
    throw new Error(`Excel 檔案正在被其他程式使用，請關閉後重跑：${xlsxPath}`);
  }
  throw error;
}
console.log(JSON.stringify({ updated: account.username, count: accounts.length, xlsxPath, jsonPath, userscriptPath, runtimeScriptPath }, null, 2));
