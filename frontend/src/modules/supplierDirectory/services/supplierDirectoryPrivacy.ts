const NAME_MASK = "********";

function maskMiddle(value: string, preferredPrefix: number, preferredSuffix: number, mask: string): string {
  const characters = Array.from(String(value || "").trim());
  if (!characters.length) return "--";
  if (characters.length === 1) return `${characters[0]}${mask}`;

  let prefixLength = preferredPrefix;
  let suffixLength = preferredSuffix;
  if (characters.length <= preferredPrefix + preferredSuffix) {
    prefixLength = Math.max(1, Math.min(preferredPrefix, Math.floor((characters.length - 1) / 2)));
    suffixLength = Math.max(1, Math.min(preferredSuffix, characters.length - prefixLength - 1));
  }
  return `${characters.slice(0, prefixLength).join("")}${mask}${characters.slice(-suffixLength).join("")}`;
}

export function maskSupplierName(value: string): string {
  const normalized = String(value || "").trim();
  const containsChinese = /[\u3400-\u9fff]/u.test(normalized);
  return maskMiddle(normalized, containsChinese ? 3 : 4, containsChinese ? 4 : 8, NAME_MASK);
}

export function maskSupplierPhone(value: string): string {
  return maskMiddle(String(value || "").trim(), 3, 4, "****");
}

export function maskSupplierContactName(value: string): string {
  const characters = Array.from(String(value || "").trim());
  if (!characters.length) return "--";
  if (characters.length === 1) return "****";
  if (characters.length === 2) return `${characters[0]}****`;
  return `${characters[0]}****${characters[characters.length - 1]}`;
}

export function maskSupplierEmail(value: string): string {
  const normalized = String(value || "").trim();
  const separatorIndex = normalized.lastIndexOf("@");
  if (separatorIndex <= 0 || separatorIndex === normalized.length - 1) {
    if (!normalized) return "--";
    return `${Array.from(normalized).slice(0, 2).join("")}****`;
  }
  const localPart = normalized.slice(0, separatorIndex);
  const domain = normalized.slice(separatorIndex + 1);
  const visiblePrefix = Array.from(localPart).slice(0, Math.min(2, Math.max(1, localPart.length))).join("");
  return `${visiblePrefix}****@${domain}`;
}
