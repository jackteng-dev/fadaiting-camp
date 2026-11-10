import sharp from "sharp";

const WIDTH = 2500;
const HEIGHT = 843;

const items = [
  { x: 0, y: 0, width: 833, height: 421, title: "營區資訊", subtitle: "環境 / 交通 / 設施", icon: "i" },
  { x: 833, y: 0, width: 834, height: 421, title: "我要預約", subtitle: "LINE 會員入口", icon: "+" },
  { x: 1667, y: 0, width: 833, height: 421, title: "查我的訂單", subtitle: "付款 / 狀態 / 明細", icon: "?" },
  { x: 0, y: 421, width: 833, height: 422, title: "最新公告", subtitle: "活動 / 入住提醒", icon: "!" },
  { x: 833, y: 421, width: 834, height: 422, title: "常見問題", subtitle: "規定 / 退款 / 用品", icon: "Q" },
  { x: 1667, y: 421, width: 833, height: 422, title: "服務選單", subtitle: "回到主選單", icon: "M" },
];

export async function buildRichMenuImageBuffer(): Promise<Buffer> {
  const svg = buildRichMenuSvg();
  return sharp(Buffer.from(svg)).png().toBuffer();
}

function buildRichMenuSvg() {
  const cells = items.map(buildCell).join("");
  return `
    <svg width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}" xmlns="http://www.w3.org/2000/svg">
      <rect width="${WIDTH}" height="${HEIGHT}" fill="#f8fafc"/>
      <rect x="0" y="0" width="${WIDTH}" height="${HEIGHT}" fill="#ffffff"/>
      ${cells}
    </svg>
  `;
}

function buildCell(item: (typeof items)[number]) {
  const centerX = item.x + item.width / 2;
  const iconY = item.y + 132;
  const titleY = item.y + 248;
  const subtitleY = item.y + 314;
  return `
    <g>
      <rect x="${item.x}" y="${item.y}" width="${item.width}" height="${item.height}" fill="#ffffff"/>
      <rect x="${item.x + 32}" y="${item.y + 32}" width="${item.width - 64}" height="${item.height - 64}" rx="36" fill="#f1f5f9"/>
      <circle cx="${centerX}" cy="${iconY}" r="56" fill="#2563eb"/>
      <text x="${centerX}" y="${iconY + 19}" text-anchor="middle" font-size="58" font-weight="700" fill="#ffffff" font-family="Segoe UI, Microsoft JhengHei, sans-serif">${escapeXml(item.icon)}</text>
      <text x="${centerX}" y="${titleY}" text-anchor="middle" font-size="54" font-weight="700" fill="#0f172a" font-family="Segoe UI, Microsoft JhengHei, sans-serif">${escapeXml(item.title)}</text>
      <text x="${centerX}" y="${subtitleY}" text-anchor="middle" font-size="32" fill="#475569" font-family="Segoe UI, Microsoft JhengHei, sans-serif">${escapeXml(item.subtitle)}</text>
      <line x1="${item.x + item.width}" y1="${item.y + 56}" x2="${item.x + item.width}" y2="${item.y + item.height - 56}" stroke="#e2e8f0" stroke-width="3"/>
      <line x1="${item.x + 56}" y1="${item.y + item.height}" x2="${item.x + item.width - 56}" y2="${item.y + item.height}" stroke="#e2e8f0" stroke-width="3"/>
    </g>
  `;
}

function escapeXml(value: string) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}
