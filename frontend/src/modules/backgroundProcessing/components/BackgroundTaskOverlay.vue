<script setup lang="ts">
import { computed } from "vue";
import IconButton from "@/components/IconButton.vue";
import type { BackgroundTaskCounter, BackgroundTaskStage, BackgroundTaskStatus } from "../types/backgroundTask";
import "../styles/background-task-overlay.css";

const props = withDefaults(
  defineProps<{
    visible: boolean;
    kicker: string;
    title: string;
    fileName?: string;
    status: BackgroundTaskStatus;
    stages: readonly BackgroundTaskStage[];
    stageIndex: number;
    progressPercent?: number | null;
    elapsedSeconds: number;
    message?: string;
    contextItems?: string[];
    counters?: BackgroundTaskCounter[];
    errorCode?: string;
    allowBackground?: boolean;
    retryable?: boolean;
    downloadErrors?: boolean;
    primaryActionLabel?: string;
    closeLabel?: string;
    legacyPrefix?: string;
  }>(),
  {
    fileName: "",
    progressPercent: null,
    message: "",
    contextItems: () => [],
    counters: () => [],
    errorCode: "",
    allowBackground: false,
    retryable: false,
    downloadErrors: false,
    primaryActionLabel: "",
    closeLabel: "关闭任务进度",
    legacyPrefix: ""
  }
);

defineEmits<{
  background: [];
  retry: [];
  close: [];
  primary: [];
  "download-errors": [];
}>();

const terminal = computed(() => ["PARTIAL", "COMPLETED", "FAILED"].includes(props.status));
const determinate = computed(() => typeof props.progressPercent === "number" && Number.isFinite(props.progressPercent));
const normalizedPercent = computed(() => determinate.value ? Math.min(100, Math.max(0, Number(props.progressPercent))) : null);
const progressStyle = computed(() => ({ "--background-task-progress": `${normalizedPercent.value ?? 0}%` }));
const legacyClass = (element?: string) => props.legacyPrefix ? `${props.legacyPrefix}${element ? `__${element}` : ""}` : "";
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      :class="['background-task-overlay', legacyClass()]"
      role="dialog"
      aria-modal="true"
      aria-labelledby="background-task-overlay-title"
    >
      <section :class="['background-task-overlay__panel', legacyClass('panel'), `is-${status.toLowerCase()}`]">
        <div :class="['background-task-overlay__timer', legacyClass('timer')]" aria-label="处理用时">
          <span>处理用时</span>
          <strong>{{ elapsedSeconds }}</strong>
          <small>秒</small>
        </div>

        <header :class="['background-task-overlay__head', legacyClass('head')]">
          <span>{{ kicker }}</span>
          <h2 id="background-task-overlay-title">{{ title }}</h2>
          <p v-if="fileName" :title="fileName">{{ fileName }}</p>
        </header>

        <ol
          :class="['background-task-overlay__stages', legacyClass('stages')]"
          :style="{ '--background-task-stage-count': Math.max(1, stages.length) }"
        >
          <li
            v-for="(stage, index) in stages"
            :key="stage.code"
            :class="{
              active: index === stageIndex && !terminal,
              done: index < stageIndex || (terminal && status !== 'FAILED' && index === stageIndex),
              failed: index === stageIndex && status === 'FAILED'
            }"
          >
            <i>{{ index + 1 }}</i>
            <span>{{ stage.label }}</span>
          </li>
        </ol>

        <div
          :class="['background-task-overlay__progress', legacyClass('progress'), { 'is-indeterminate': !determinate }]"
          :style="progressStyle"
        >
          <div
            :class="['background-task-overlay__track', legacyClass('track')]"
            role="progressbar"
            :aria-valuenow="determinate ? normalizedPercent ?? undefined : undefined"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-label="determinate ? `任务进度 ${normalizedPercent}%` : '等待服务端返回真实进度'"
          >
            <div :class="['background-task-overlay__fill', legacyClass('fill')]"></div>
            <div :class="['background-task-overlay__ship', legacyClass('ship')]" aria-hidden="true">
              <svg viewBox="0 0 64 40">
                <path class="ship-flag" d="M35 5v13M36 7h15l-4 5 4 5H36" />
                <path class="ship-body" d="M8 21h45l-6 10H16L8 21Z" />
                <path class="ship-cabin" d="M23 13h18l5 8H18l5-8Z" />
                <path class="ship-wave" d="M6 34c5-3 9-3 14 0s9 3 14 0 9-3 14 0 8 3 12 0" />
              </svg>
            </div>
          </div>
          <strong>{{ determinate ? `${normalizedPercent}%` : "等待服务端返回真实进度" }}</strong>
        </div>

        <div :class="['background-task-overlay__live', legacyClass('live')]" role="status" aria-live="polite">
          <strong v-if="message">{{ message }}</strong>
          <span v-if="contextItems.length">{{ contextItems.join(" · ") }}</span>
        </div>

        <div v-if="counters.length" :class="['background-task-overlay__counters', legacyClass('counters')]">
          <div v-for="counter in counters" :key="counter.label">
            <span>{{ counter.label }}</span>
            <strong>{{ counter.value }}</strong>
          </div>
        </div>

        <footer :class="['background-task-overlay__footer', legacyClass('footer')]">
          <span v-if="errorCode" class="background-task-overlay__error-code">{{ errorCode }}</span>
          <div :class="['background-task-overlay__actions', legacyClass('actions')]">
            <IconButton v-if="!terminal && allowBackground" icon="Minimize2" label="转入后台运行" @click="$emit('background')" />
            <IconButton v-if="status === 'FAILED' && retryable" icon="RefreshCw" label="重试失败任务" variant="primary" @click="$emit('retry')" />
            <IconButton v-if="downloadErrors" icon="Download" label="下载失败明细" @click="$emit('download-errors')" />
            <IconButton v-if="terminal && status !== 'FAILED' && primaryActionLabel" icon="Eye" :label="primaryActionLabel" variant="primary" @click="$emit('primary')" />
            <IconButton v-if="terminal" icon="X" :label="closeLabel" @click="$emit('close')" />
          </div>
        </footer>
      </section>
    </div>
  </Teleport>
</template>
