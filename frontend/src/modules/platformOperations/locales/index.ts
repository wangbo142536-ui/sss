import zhCN from "./zh-CN";
import enUS from "./en-US";
import { useI18n } from "@/i18n";
import type { LocalizedText } from "../types/supplierAnalytics";

type MessageKey = keyof typeof zhCN;

export function usePlatformAnalyticsI18n() {
  const { language } = useI18n();
  const ta = (key: MessageKey, params: Record<string, string | number> = {}) => {
    const dictionary = language.value === "en-US" ? enUS : zhCN;
    return Object.entries(params).reduce(
      (value, [name, replacement]) => value.split(`{${name}}`).join(String(replacement)),
      dictionary[key] ?? zhCN[key]
    );
  };
  const localText = (value: LocalizedText) => language.value === "en-US" ? value.en : value.zh;
  return { language, ta, localText };
}

