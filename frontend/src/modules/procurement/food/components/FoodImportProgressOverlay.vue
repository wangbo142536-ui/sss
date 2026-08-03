<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  visible: boolean;
  fileName: string;
  percent: number;
  stageIndex: number;
  state: "running" | "success" | "failed";
  totalRows?: number;
}>();

const stages = ["读取", "识别", "导入"];
const progressStyle = computed(() => ({ "--progress": `${props.percent}%` }));
const countLabel = computed(() => (props.totalRows === undefined ? "读取中" : `${props.totalRows} 项`));
const title = computed(() => {
  if (props.state === "failed") return "伙食清单导入失败";
  if (props.state === "success") return "伙食清单导入完成";
  return "正在导入伙食清单";
});
</script>

<template>
  <Teleport to="body">
    <div v-if="visible" class="food-import-progress-backdrop" role="status" aria-live="polite">
      <section class="food-import-progress-panel" :class="`is-${state}`">
        <div class="food-import-progress-head">
          <span>伙食清单导入</span>
          <h2>{{ title }}</h2>
          <p>{{ fileName }}</p>
        </div>

        <ol class="food-import-progress-stages">
          <li
            v-for="(stage, index) in stages"
            :key="stage"
            :class="{ active: index === stageIndex, done: index < stageIndex || percent === 100 }"
          >
            <i>{{ index + 1 }}</i>
            <span>{{ stage }}</span>
          </li>
        </ol>

        <div class="food-ship-progress" :style="progressStyle">
          <div class="food-ship-progress-track">
            <div class="food-ship-progress-fill"></div>
            <div class="food-ship-runner" aria-hidden="true">
              <svg viewBox="0 0 64 40">
                <path class="food-ship-flag" d="M35 5v13M36 7h15l-4 5 4 5H36" />
                <path class="food-ship-body" d="M8 21h45l-6 10H16L8 21Z" />
                <path class="food-ship-cabin" d="M23 13h18l5 8H18l5-8Z" />
                <path class="food-ship-wave" d="M6 34c5-3 9-3 14 0s9 3 14 0 9-3 14 0 8 3 12 0" />
              </svg>
            </div>
          </div>
        </div>

        <div class="food-import-progress-counters">
          <div><span>伙食行数</span><strong>{{ countLabel }}</strong></div>
          <div><span>已识别</span><strong>{{ countLabel }}</strong></div>
          <div><span>已导入</span><strong>{{ countLabel }}</strong></div>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.food-import-progress-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(238, 248, 255, 0.68);
  backdrop-filter: blur(12px) saturate(1.08);
}

.food-import-progress-panel {
  width: min(680px, 100%);
  padding: 24px;
  border: 1px solid rgba(168, 207, 238, 0.9);
  border-radius: 24px;
  background:
    radial-gradient(circle at 14% 0%, rgba(29, 114, 210, 0.12), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 251, 255, 0.98));
  box-shadow: 0 30px 90px rgba(39, 94, 143, 0.2);
}

.food-import-progress-panel.is-failed {
  border-color: rgba(242, 178, 178, 0.9);
  background:
    radial-gradient(circle at 14% 0%, rgba(220, 74, 74, 0.1), transparent 36%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(255, 247, 247, 0.98));
}

.food-import-progress-head {
  display: grid;
  gap: 6px;
  text-align: center;
}

.food-import-progress-head span {
  color: #1d72d2;
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.food-import-progress-head h2 {
  margin: 0;
  color: #0f2c4c;
  font-size: 24px;
}

.food-import-progress-head p {
  min-height: 20px;
  margin: 0;
  overflow: hidden;
  color: #5d7288;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.food-import-progress-stages {
  margin: 22px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  list-style: none;
}

.food-import-progress-stages li {
  min-width: 0;
  display: grid;
  justify-items: center;
  gap: 8px;
  color: #6d879d;
  font-size: 12px;
  font-weight: 900;
  text-align: center;
}

.food-import-progress-stages i {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border: 1px solid #c6dff3;
  border-radius: 999px;
  color: #5790bd;
  background: #ffffff;
  font-style: normal;
}

.food-import-progress-stages li.active i,
.food-import-progress-stages li.done i {
  border-color: #1d72d2;
  color: #ffffff;
  background: #1d72d2;
}

.food-import-progress-stages li.active span,
.food-import-progress-stages li.done span {
  color: #0f2c4c;
}

.food-ship-progress {
  margin-top: 28px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 52px;
  gap: 14px;
  align-items: center;
}

.food-ship-progress-track {
  position: relative;
  height: 16px;
  border: 1px solid #b9d8f2;
  border-radius: 999px;
  background: #eaf6ff;
  box-shadow: inset 0 1px 2px rgba(56, 107, 152, 0.08);
}

.food-ship-progress-fill {
  width: var(--progress);
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #83d7ff 0%, #1d72d2 100%);
  transition: width 420ms cubic-bezier(0.22, 0.8, 0.22, 1);
}

.food-ship-runner {
  position: absolute;
  left: var(--progress);
  bottom: 5px;
  width: 58px;
  color: #1d72d2;
  transform: translateX(-50%);
  transition: left 420ms cubic-bezier(0.22, 0.8, 0.22, 1);
  animation: food-ship-bob 1.2s ease-in-out infinite;
}

.food-ship-runner svg {
  width: 100%;
  height: auto;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 3;
}

.food-ship-body,
.food-ship-cabin {
  fill: #e7f4ff;
}

.food-ship-flag {
  stroke: #0f80df;
}

.food-ship-wave {
  stroke: #7fc8f5;
  stroke-width: 2.4;
}

.food-import-progress-counters {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.food-import-progress-counters div {
  min-width: 0;
  padding: 12px;
  border: 1px solid #d6e9f8;
  border-radius: 14px;
  background: #ffffff;
}

.food-import-progress-counters span {
  display: block;
  color: #648198;
  font-size: 12px;
  font-weight: 800;
}

.food-import-progress-counters strong {
  display: block;
  margin-top: 6px;
  color: #0f2c4c;
  font-size: 18px;
  font-weight: 900;
}

@keyframes food-ship-bob {
  0%,
  100% {
    transform: translate(-50%, 0);
  }

  50% {
    transform: translate(-50%, -4px);
  }
}

@media (prefers-reduced-motion: reduce) {
  .food-ship-runner,
  .food-ship-progress-fill {
    animation: none;
    transition-duration: 0ms;
  }
}
</style>
