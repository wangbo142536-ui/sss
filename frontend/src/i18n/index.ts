import { computed, reactive } from "vue";
import enUS from "@/locales/en-US";
import zhCN from "@/locales/zh-CN";

export type Language = "zh-CN" | "en-US";

const messages = {
  "zh-CN": zhCN,
  "en-US": enUS
} as const;

const storageKey = "ship-supply-language";

const state = reactive({
  language:
    typeof window !== "undefined" && window.localStorage.getItem(storageKey) === "en-US" ? "en-US" : ("zh-CN" as Language)
});

const resolve = (key: string, dict: Record<string, unknown>): string | undefined => {
  const value = key.split(".").reduce<unknown>((source, segment) => {
    if (!source || typeof source !== "object") {
      return undefined;
    }
    return (source as Record<string, unknown>)[segment];
  }, dict);

  return typeof value === "string" ? value : undefined;
};

export const setLanguage = (language: Language) => {
  state.language = language;
  if (typeof window !== "undefined") {
    window.localStorage.setItem(storageKey, language);
  }
};

export const t = (key: string, params: Record<string, string | number> = {}) => {
  const message = resolve(key, messages[state.language]) ?? resolve(key, messages["zh-CN"]) ?? key;
  return Object.entries(params).reduce((text, [name, value]) => text.split(`{${name}}`).join(String(value)), message);
};

export const useI18n = () => ({
  language: computed(() => state.language),
  setLanguage,
  t
});
