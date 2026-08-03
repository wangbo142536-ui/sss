<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from "vue";

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    mode?: "date" | "datetime";
    disabled?: boolean;
    invalid?: boolean;
    placeholder?: string;
    step?: string;
  }>(),
  {
    modelValue: "",
    mode: "date",
    disabled: false,
    invalid: false,
    placeholder: "",
    step: "60"
  }
);

const emit = defineEmits<{
  "update:modelValue": [value: string];
  input: [event: Event];
  change: [event: Event];
}>();

const open = ref(false);
const draftYear = ref("");
const draftMonth = ref("");
const draftDay = ref("");
const draftHour = ref("09");
const draftMinute = ref("00");
const rootRef = ref<HTMLElement | null>(null);
const triggerRef = ref<HTMLButtonElement | null>(null);
const popoverRef = ref<HTMLElement | null>(null);
const popoverStyle = ref<Record<string, string>>({});

const labels = {
  date: "\u65e5\u671f",
  year: "\u5e74",
  month: "\u6708",
  day: "\u65e5",
  hour: "\u65f6",
  minute: "\u5206",
  clear: "\u6e05\u7a7a",
  confirm: "\u786e\u5b9a"
};

const currentYear = new Date().getFullYear();
const yearOptions = Array.from({ length: 9 }, (_, index) => String(currentYear - 2 + index));
const monthOptions = Array.from({ length: 12 }, (_, index) => String(index + 1).padStart(2, "0"));
const hourOptions = Array.from({ length: 24 }, (_, index) => String(index).padStart(2, "0"));
const minuteOptions = ["00", "15", "30", "45"];

const dayOptions = computed(() => {
  const year = Number(draftYear.value || currentYear);
  const month = Number(draftMonth.value || 1);
  const dayCount = new Date(year, month, 0).getDate();
  return Array.from({ length: dayCount }, (_, index) => String(index + 1).padStart(2, "0"));
});

function normalized(value?: string): string {
  const text = String(value ?? "").trim();
  if (!text) return "";
  const valueText = text.replace(" ", "T");
  return props.mode === "date" ? valueText.slice(0, 10) : valueText.slice(0, 16);
}

function syncDraft(value?: string): void {
  const text = normalized(value);
  const dateText = text.slice(0, 10);
  draftYear.value = dateText.slice(0, 4);
  draftMonth.value = dateText.slice(5, 7);
  draftDay.value = dateText.slice(8, 10);
  if (props.mode === "datetime" && text.length >= 16) {
    draftHour.value = text.slice(11, 13);
    draftMinute.value = text.slice(14, 16);
  }
}

watch(() => props.modelValue, syncDraft, { immediate: true });

watch([draftYear, draftMonth], () => {
  if (draftDay.value && !dayOptions.value.includes(draftDay.value)) {
    draftDay.value = dayOptions.value[dayOptions.value.length - 1] || "";
  }
});

const displayValue = computed(() => {
  const text = normalized(props.modelValue);
  return props.mode === "datetime" ? text.replace("T", " ") : text;
});

function emitValue(value: string): void {
  emit("update:modelValue", value);
  emit("input", new Event("input"));
  emit("change", new Event("change"));
}

function ensureDraftDefaults(): void {
  if (draftYear.value && draftMonth.value && draftDay.value) return;
  const now = new Date();
  draftYear.value = String(now.getFullYear());
  draftMonth.value = String(now.getMonth() + 1).padStart(2, "0");
  draftDay.value = String(now.getDate()).padStart(2, "0");
}

function toggle(): void {
  if (props.disabled) return;
  syncDraft(props.modelValue);
  ensureDraftDefaults();
  open.value = !open.value;
  if (open.value) {
    nextTick(updatePopoverPosition);
  }
}

function close(): void {
  open.value = false;
}

function updatePopoverPosition(): void {
  const trigger = triggerRef.value;
  if (!trigger) return;
  const rect = trigger.getBoundingClientRect();
  const gap = 6;
  const margin = 12;
  const width = Math.min(360, Math.max(260, window.innerWidth - margin * 2));
  const estimatedHeight = props.mode === "datetime" ? 274 : 218;
  const opensUp = rect.bottom + gap + estimatedHeight > window.innerHeight && rect.top > estimatedHeight;
  const left = Math.min(Math.max(rect.left, margin), window.innerWidth - width - margin);
  const top = opensUp
    ? Math.max(margin, rect.top - gap - estimatedHeight)
    : Math.min(rect.bottom + gap, window.innerHeight - estimatedHeight - margin);
  popoverStyle.value = {
    width: `${width}px`,
    left: `${left}px`,
    top: `${top}px`
  };
}

function apply(): void {
  if (!draftYear.value || !draftMonth.value || !draftDay.value) {
    emitValue("");
    close();
    return;
  }
  const dateValue = `${draftYear.value}-${draftMonth.value}-${draftDay.value}`;
  const value = props.mode === "datetime" ? `${dateValue}T${draftHour.value}:${draftMinute.value}` : dateValue;
  emitValue(value);
  close();
  nextTick(close);
}

function clear(): void {
  draftYear.value = "";
  draftMonth.value = "";
  draftDay.value = "";
  emitValue("");
  close();
}

function handleDocumentPointer(event: PointerEvent): void {
  if (!open.value) return;
  const target = event.target;
  if (target instanceof Node && rootRef.value?.contains(target)) return;
  if (target instanceof Node && popoverRef.value?.contains(target)) return;
  close();
}

function handleKeydown(event: KeyboardEvent): void {
  if (event.key === "Escape") close();
}

document.addEventListener("pointerdown", handleDocumentPointer);
document.addEventListener("keydown", handleKeydown);
window.addEventListener("resize", updatePopoverPosition);
window.addEventListener("scroll", updatePopoverPosition, true);

onBeforeUnmount(() => {
  document.removeEventListener("pointerdown", handleDocumentPointer);
  document.removeEventListener("keydown", handleKeydown);
  window.removeEventListener("resize", updatePopoverPosition);
  window.removeEventListener("scroll", updatePopoverPosition, true);
});

defineExpose({
  focus: () => triggerRef.value?.focus()
});
</script>

<template>
  <span ref="rootRef" class="stable-datetime" :class="{ 'is-open': open, 'is-disabled': disabled, 'is-invalid': invalid }">
    <button ref="triggerRef" type="button" class="stable-datetime-trigger" :disabled="disabled" @click.stop="toggle">
      <span :class="{ 'stable-datetime-placeholder': !displayValue }">{{ displayValue || placeholder }}</span>
      <svg class="stable-datetime-icon" viewBox="0 0 24 24" aria-hidden="true">
        <path d="M7 3v4M17 3v4M4 9h16M6 5h12a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2Z" />
      </svg>
    </button>
    <Teleport to="body">
    <div v-if="open" ref="popoverRef" class="stable-datetime-popover" :style="popoverStyle" @click.stop>
      <div class="stable-datetime-row">
        <span>{{ labels.date }}</span>
        <div class="stable-datetime-date-grid">
          <label>
            <span>{{ labels.year }}</span>
            <select v-model="draftYear">
              <option v-for="year in yearOptions" :key="year" :value="year">{{ year }}</option>
            </select>
          </label>
          <label>
            <span>{{ labels.month }}</span>
            <select v-model="draftMonth">
              <option v-for="month in monthOptions" :key="month" :value="month">{{ month }}</option>
            </select>
          </label>
          <label>
            <span>{{ labels.day }}</span>
            <select v-model="draftDay">
              <option v-for="day in dayOptions" :key="day" :value="day">{{ day }}</option>
            </select>
          </label>
        </div>
      </div>
      <div v-if="mode === 'datetime'" class="stable-datetime-time">
        <label class="stable-datetime-row">
          <span>{{ labels.hour }}</span>
          <select v-model="draftHour">
            <option v-for="hour in hourOptions" :key="hour" :value="hour">{{ hour }}</option>
          </select>
        </label>
        <b>:</b>
        <label class="stable-datetime-row">
          <span>{{ labels.minute }}</span>
          <select v-model="draftMinute">
            <option v-for="minute in minuteOptions" :key="minute" :value="minute">{{ minute }}</option>
          </select>
        </label>
      </div>
      <footer class="stable-datetime-actions">
        <button type="button" @pointerdown.prevent.stop="clear">{{ labels.clear }}</button>
        <button type="button" class="is-primary" @pointerdown.prevent.stop="apply">{{ labels.confirm }}</button>
      </footer>
    </div>
    </Teleport>
  </span>
</template>
