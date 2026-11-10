import { config } from "./config.js";

type LineTextMessage = {
  type: "text";
  text: string;
  quickReply?: {
    items: Array<{
      type: "action";
      action: Record<string, string>;
    }>;
  };
};

type LineFlexMessage = {
  type: "flex";
  altText: string;
  contents: Record<string, unknown>;
};

export type LineReplyMessage = LineTextMessage | LineFlexMessage;

export function buildWelcomeMessages(): LineReplyMessage[] {
  return [
    buildMainMenuFlexMessage(),
    buildTextMessage("可以直接點選選單，也可以輸入：營區資訊、我要預約、查我的訂單、公告、FAQ。"),
  ];
}

export function buildKeywordReply(input: string): LineReplyMessage[] {
  const text = input.trim().toLowerCase();
  if (!text) {
    return [buildMainMenuFlexMessage()];
  }
  if (matches(text, ["選單", "menu", "開始", "功能"])) {
    return [buildMainMenuFlexMessage()];
  }
  if (matches(text, ["預約", "訂位", "我要預約", "我要訂位"])) {
    return [buildBookingEntryFlexMessage()];
  }
  if (matches(text, ["查詢", "查訂單", "我的訂單", "訂單"])) {
    return [buildLookupEntryFlexMessage()];
  }
  if (matches(text, ["資訊", "營區資訊", "介紹", "露營區", "營位", "公告", "faq", "問題"])) {
    return [buildMainMenuFlexMessage()];
  }
  return [
    buildTextMessage("我目前可以協助你查看營區資訊、進入預約、查詢訂單。", true),
  ];
}

export function buildMainMenuFlexMessage(): LineFlexMessage {
  return {
    type: "flex",
    altText: "發呆亭露營區選單",
    contents: {
      type: "bubble",
      size: "mega",
      header: {
        type: "box",
        layout: "vertical",
        contents: [
          {
            type: "text",
            text: "發呆亭露營區",
            weight: "bold",
            size: "xl",
            color: "#0f172a",
          },
          {
            type: "text",
            text: "LINE 服務選單",
            size: "sm",
            color: "#64748b",
            margin: "sm",
          },
        ],
      },
      body: {
        type: "box",
        layout: "vertical",
        spacing: "md",
        contents: [
          buildMenuButton("營區資訊", `${config.mainFrontendBaseUrl}/`),
          buildMenuButton("我要預約", `${config.webBaseUrl}`),
          buildMenuButton("查我的訂單", `${config.webBaseUrl}`),
          buildMenuButton("最新公告", `${config.mainFrontendBaseUrl}/news`),
          buildMenuButton("常見問題", `${config.mainFrontendBaseUrl}/faq`),
        ],
      },
    },
  };
}

export function buildBookingEntryFlexMessage(): LineFlexMessage {
  return {
    type: "flex",
    altText: "我要預約",
    contents: {
      type: "bubble",
      body: {
        type: "box",
        layout: "vertical",
        spacing: "md",
        contents: [
          {
            type: "text",
            text: "我要預約",
            weight: "bold",
            size: "xl",
          },
          {
            type: "text",
            text: "可先在 LINE 內完成會員綁定，再進入預約流程。",
            wrap: true,
            color: "#475569",
          },
          buildMenuButton("開啟 LINE 預約入口", `${config.webBaseUrl}`),
          buildMenuButton("開啟官網預約頁", `${config.mainFrontendBaseUrl}/booking`),
        ],
      },
    },
  };
}

export function buildLookupEntryFlexMessage(): LineFlexMessage {
  return {
    type: "flex",
    altText: "查我的訂單",
    contents: {
      type: "bubble",
      body: {
        type: "box",
        layout: "vertical",
        spacing: "md",
        contents: [
          {
            type: "text",
            text: "查我的訂單",
            weight: "bold",
            size: "xl",
          },
          {
            type: "text",
            text: "完成 LINE 綁定後，就能用 LINE 身分查詢訂單與付款狀態。",
            wrap: true,
            color: "#475569",
          },
          buildMenuButton("開啟 LINE 查單入口", `${config.webBaseUrl}`),
          buildMenuButton("用訂單編號查詢", `${config.mainFrontendBaseUrl}/booking/lookup`),
        ],
      },
    },
  };
}

export function buildRichMenuBody() {
  return {
    size: {
      width: 2500,
      height: 843,
    },
    selected: true,
    name: "發呆亭露營區主選單",
    chatBarText: "發呆亭選單",
    areas: [
      buildRichMenuArea(0, 0, 833, 421, "營區資訊", `${config.mainFrontendBaseUrl}/`),
      buildRichMenuArea(833, 0, 834, 421, "我要預約", `${config.webBaseUrl}`),
      buildRichMenuArea(1667, 0, 833, 421, "查我的訂單", `${config.webBaseUrl}`),
      buildRichMenuArea(0, 421, 833, 422, "最新公告", `${config.mainFrontendBaseUrl}/news`),
      buildRichMenuArea(833, 421, 834, 422, "常見問題", `${config.mainFrontendBaseUrl}/faq`),
      {
        bounds: { x: 1667, y: 421, width: 833, height: 422 },
        action: {
          type: "message",
          text: "選單",
        },
      },
    ],
  };
}

function buildTextMessage(text: string, withQuickReply = false): LineTextMessage {
  const message: LineTextMessage = {
    type: "text",
    text,
  };
  if (withQuickReply) {
    message.quickReply = {
      items: [
        { type: "action", action: { type: "message", label: "選單", text: "選單" } },
        { type: "action", action: { type: "uri", label: "我要預約", uri: `${config.webBaseUrl}` } },
        { type: "action", action: { type: "uri", label: "查訂單", uri: `${config.webBaseUrl}` } },
      ],
    };
  }
  return message;
}

function buildMenuButton(label: string, uri: string) {
  return {
    type: "button",
    style: "primary",
    color: "#2563eb",
    height: "sm",
    action: {
      type: "uri",
      label,
      uri,
    },
  };
}

function buildRichMenuArea(x: number, y: number, width: number, height: number, label: string, uri: string) {
  return {
    bounds: { x, y, width, height },
    action: {
      type: "uri",
      label,
      uri,
    },
  };
}

function matches(input: string, keywords: string[]) {
  return keywords.some((keyword) => input.includes(keyword.toLowerCase()));
}
