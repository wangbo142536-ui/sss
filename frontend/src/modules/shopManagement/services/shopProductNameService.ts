const LEADING_SIZE_PATTERN = /^\s*(?:(?:\d+(?:\.\d+)?\s*(?:"{1,2}|'{1,2}|inch|in)\s*\/\s*)?\d+(?:\.\d+)?\s*(?:mm|cm|m)\b|\d+(?:\.\d+)?\s*(?:ml|l|kg|g|pcs|pc|box|ctn)\b)\s*/i;

export function extractShopProductName(value: unknown): string {
  const source = typeof value === "string" || typeof value === "number" ? String(value).trim() : "";
  if (!source) return "";

  const firstLine = source
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find(Boolean) || "";
  const withoutLeadingSize = firstLine
    .replace(LEADING_SIZE_PATTERN, "")
    .replace(/^\s*[/,:：;；-]+\s*/, "")
    .replace(/\s+/g, " ")
    .trim();
  return withoutLeadingSize || firstLine.replace(/\s+/g, " ").trim();
}
