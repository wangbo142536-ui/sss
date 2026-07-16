<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import heroPortImage from "@/assets/home-port-command.png";
import bargeImage from "@/assets/home-service-barge.png";
import crewImage from "@/assets/home-service-crew.png";
import foodImage from "@/assets/home-service-food.png";
import materialsImage from "@/assets/home-service-materials.png";
import suppliersImage from "@/assets/home-service-suppliers.png";
import weatherImage from "@/assets/home-service-weather.png";
import { t } from "@/i18n";

type ServiceSlide = {
  key: string;
  titleKey: string;
  subtitleKey: string;
  actionKey: string;
  actionTo: string;
  secondaryKey: string;
  secondaryTo: string;
  tone: string;
  image: string;
  metrics: Array<{ labelKey: string; value: string; unitKey: string; noteKey: string }>;
};

const authActions = [
  { labelKey: "common.login", to: "/login" },
  { labelKey: "common.register", to: "/register" }
];

const serviceSlides: ServiceSlide[] = [
  {
    key: "platform",
    titleKey: "home.services.platform.title",
    subtitleKey: "home.services.platform.subtitle",
    actionKey: "home.services.platform.action",
    actionTo: "/services",
    secondaryKey: "home.actions.viewDashboard",
    secondaryTo: "/dashboard",
    tone: "blue",
    image: heroPortImage,
    metrics: [
      { labelKey: "home.metrics.supplierTotal", value: "186", unitKey: "home.units.companies", noteKey: "home.notes.localActive" },
      { labelKey: "home.metrics.matchRows", value: "1,260", unitKey: "home.units.rows", noteKey: "home.notes.matchPreview" },
      { labelKey: "home.metrics.bargeTasks", value: "12", unitKey: "home.units.trips", noteKey: "home.notes.dispatching" },
      { labelKey: "home.metrics.riskLevel", value: "II", unitKey: "home.units.level", noteKey: "home.notes.weatherRisk" }
    ]
  },
  {
    key: "suppliers",
    titleKey: "home.services.suppliers.title",
    subtitleKey: "home.services.suppliers.subtitle",
    actionKey: "home.services.suppliers.action",
    actionTo: "/suppliers",
    secondaryKey: "home.actions.enterServices",
    secondaryTo: "/services",
    tone: "cyan",
    image: suppliersImage,
    metrics: [
      { labelKey: "home.metrics.supplierTotal", value: "186", unitKey: "home.units.companies", noteKey: "home.notes.localActive" },
      { labelKey: "home.metrics.activeQuotes", value: "42", unitKey: "home.units.orders", noteKey: "home.notes.todayQuotes" },
      { labelKey: "home.metrics.coveredPorts", value: "7", unitKey: "home.units.ports", noteKey: "home.notes.zhoushanNingbo" },
      { labelKey: "home.metrics.avgResponse", value: "28", unitKey: "home.units.minutes", noteKey: "home.notes.fastResponse" }
    ]
  },
  {
    key: "materials",
    titleKey: "home.services.materials.title",
    subtitleKey: "home.services.materials.subtitle",
    actionKey: "home.services.materials.action",
    actionTo: "/procurement/materials",
    secondaryKey: "home.actions.viewImpa",
    secondaryTo: "/standard-library/impa",
    tone: "blue",
    image: materialsImage,
    metrics: [
      { labelKey: "home.metrics.matchRows", value: "1,260", unitKey: "home.units.rows", noteKey: "home.notes.matchPreview" },
      { labelKey: "home.metrics.preciseMatch", value: "76", unitKey: "home.units.percent", noteKey: "home.notes.impaCoverage" },
      { labelKey: "home.metrics.pendingConfirm", value: "18", unitKey: "home.units.items", noteKey: "home.notes.manualConfirm" },
      { labelKey: "home.metrics.generatedDemand", value: "24", unitKey: "home.units.orders", noteKey: "home.notes.demandReady" }
    ]
  },
  {
    key: "food",
    titleKey: "home.services.food.title",
    subtitleKey: "home.services.food.subtitle",
    actionKey: "home.services.food.action",
    actionTo: "/procurement/food",
    secondaryKey: "home.actions.foodInquiry",
    secondaryTo: "/food/inquiries",
    tone: "green",
    image: foodImage,
    metrics: [
      { labelKey: "home.metrics.foodSuppliers", value: "38", unitKey: "home.units.companies", noteKey: "home.notes.coldChain" },
      { labelKey: "home.metrics.mealPlans", value: "12", unitKey: "home.units.orders", noteKey: "home.notes.crewSupply" },
      { labelKey: "home.metrics.freshWindows", value: "6", unitKey: "home.units.hours", noteKey: "home.notes.deliveryWindow" },
      { labelKey: "home.metrics.stockBatches", value: "420", unitKey: "home.units.items", noteKey: "home.notes.foodSku" }
    ]
  },
  {
    key: "crew",
    titleKey: "home.services.crew.title",
    subtitleKey: "home.services.crew.subtitle",
    actionKey: "home.services.crew.action",
    actionTo: "/crew-services",
    secondaryKey: "home.actions.enterServices",
    secondaryTo: "/services",
    tone: "violet",
    image: crewImage,
    metrics: [
      { labelKey: "home.metrics.crewTasks", value: "16", unitKey: "home.units.tasks", noteKey: "home.notes.crewChange" },
      { labelKey: "home.metrics.boardingWindows", value: "5", unitKey: "home.units.windows", noteKey: "home.notes.boarding" },
      { labelKey: "home.metrics.documents", value: "32", unitKey: "home.units.files", noteKey: "home.notes.documents" },
      { labelKey: "home.metrics.serviceAgents", value: "9", unitKey: "home.units.companies", noteKey: "home.notes.localAgents" }
    ]
  },
  {
    key: "weather",
    titleKey: "home.services.weather.title",
    subtitleKey: "home.services.weather.subtitle",
    actionKey: "home.services.weather.action",
    actionTo: "/services",
    secondaryKey: "home.actions.viewDashboard",
    secondaryTo: "/dashboard",
    tone: "amber",
    image: weatherImage,
    metrics: [
      { labelKey: "home.metrics.windLevel", value: "5-6", unitKey: "home.units.level", noteKey: "home.notes.eastWind" },
      { labelKey: "home.metrics.visibility", value: "6", unitKey: "home.units.km", noteKey: "home.notes.outerAnchorage" },
      { labelKey: "home.metrics.tideWindow", value: "3", unitKey: "home.units.windows", noteKey: "home.notes.tide" },
      { labelKey: "home.metrics.riskLevel", value: "II", unitKey: "home.units.level", noteKey: "home.notes.weatherRisk" }
    ]
  },
  {
    key: "barge",
    titleKey: "home.services.barge.title",
    subtitleKey: "home.services.barge.subtitle",
    actionKey: "home.services.barge.action",
    actionTo: "/delivery-tasks",
    secondaryKey: "home.actions.viewSettlement",
    secondaryTo: "/settlements",
    tone: "indigo",
    image: bargeImage,
    metrics: [
      { labelKey: "home.metrics.bargeTasks", value: "12", unitKey: "home.units.trips", noteKey: "home.notes.dispatching" },
      { labelKey: "home.metrics.anchorages", value: "8", unitKey: "home.units.anchorages", noteKey: "home.notes.zhoushanAnchorage" },
      { labelKey: "home.metrics.nightRoutes", value: "4", unitKey: "home.units.routes", noteKey: "home.notes.nightRoutes" },
      { labelKey: "home.metrics.signedDocs", value: "26", unitKey: "home.units.files", noteKey: "home.notes.deliveryDocs" }
    ]
  }
];

const sceneMode = ref<"night" | "day">("night");
const trackIndex = ref(0);
const isTrackResetting = ref(false);
const isPaused = ref(false);
let carouselTimer: number | undefined;
let trackResetTimer: number | undefined;

const visibleServiceSlides = computed(() => [...serviceSlides, ...serviceSlides, ...serviceSlides]);
const normalizeSlideIndex = (index: number) => ((index % serviceSlides.length) + serviceSlides.length) % serviceSlides.length;
const activeSlideIndex = computed(() => normalizeSlideIndex(trackIndex.value));
const activeSlide = computed(() => serviceSlides[activeSlideIndex.value]);
const heroTitleChars = computed(() => Array.from(t(activeSlide.value.titleKey)));
const isDayMode = () => sceneMode.value === "day";

const toggleSceneMode = () => {
  sceneMode.value = isDayMode() ? "night" : "day";
};

const clearCarouselTimer = () => {
  if (carouselTimer) {
    window.clearInterval(carouselTimer);
    carouselTimer = undefined;
  }
};

const clearTrackResetTimer = () => {
  if (trackResetTimer) {
    window.clearTimeout(trackResetTimer);
    trackResetTimer = undefined;
  }
};

const startCarouselTimer = () => {
  clearCarouselTimer();
  carouselTimer = window.setInterval(() => {
    if (!isPaused.value) {
      nextSlide(false);
    }
  }, 5000);
};

const resetTrackAfterLoop = (targetIndex = normalizeSlideIndex(trackIndex.value)) => {
  clearTrackResetTimer();
  trackResetTimer = window.setTimeout(() => {
    isTrackResetting.value = true;
    trackIndex.value = targetIndex;
    window.requestAnimationFrame(() => {
      isTrackResetting.value = false;
    });
  }, 580);
};

const selectSlide = (index: number, restartTimer = true) => {
  clearTrackResetTimer();
  isTrackResetting.value = false;
  trackIndex.value = index;
  if (index >= serviceSlides.length * 2) {
    resetTrackAfterLoop(serviceSlides.length + normalizeSlideIndex(index));
  }
  if (restartTimer) {
    startCarouselTimer();
  }
};

const nextSlide = (restartTimer = true) => {
  clearTrackResetTimer();
  isTrackResetting.value = false;
  trackIndex.value += 1;
  if (trackIndex.value >= serviceSlides.length * 2) {
    resetTrackAfterLoop(serviceSlides.length + activeSlideIndex.value);
  }
  if (restartTimer) {
    startCarouselTimer();
  }
};

const previousSlide = (restartTimer = true) => {
  clearTrackResetTimer();
  if (activeSlideIndex.value === 0) {
    isTrackResetting.value = true;
    trackIndex.value = serviceSlides.length;
    window.requestAnimationFrame(() => {
      isTrackResetting.value = false;
      trackIndex.value = serviceSlides.length - 1;
    });
  } else {
    isTrackResetting.value = false;
    trackIndex.value -= 1;
  }
  if (restartTimer) {
    startCarouselTimer();
  }
};

const pauseCarousel = () => {
  isPaused.value = true;
};

const resumeCarousel = () => {
  isPaused.value = false;
};

onMounted(startCarouselTimer);
onBeforeUnmount(() => {
  clearCarouselTimer();
  clearTrackResetTimer();
});
</script>

<template>
  <main :class="['home-page', `service-tone-${activeSlide.tone}`, { 'day-mode': isDayMode() }]">
    <section
      class="home-stage"
      aria-labelledby="home-title"
      @mouseenter="pauseCarousel"
      @mouseleave="resumeCarousel"
    >
      <Transition name="stage-image-fade">
        <img
          :key="activeSlide.key"
          class="stage-image"
          :src="activeSlide.image"
          :alt="t(activeSlide.titleKey)"
        />
      </Transition>
      <div class="stage-shade"></div>

      <header class="stage-nav" :aria-label="t('home.navLabel')">
        <RouterLink class="stage-brand" to="/">
          <strong>{{ t("app.name") }}</strong>
        </RouterLink>

        <nav>
          <button
            class="scene-toggle"
            type="button"
            :aria-label="isDayMode() ? t('home.switchNight') : t('home.switchDay')"
            :aria-pressed="isDayMode()"
            :title="isDayMode() ? t('home.switchNight') : t('home.switchDay')"
            @click="toggleSceneMode"
          >
            <svg v-if="isDayMode()" viewBox="0 0 24 24" aria-hidden="true">
              <path
                d="M20.2 14.7A7.6 7.6 0 0 1 9.3 3.8a8.7 8.7 0 1 0 10.9 10.9Z"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
              />
            </svg>
            <svg v-else viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="12" cy="12" r="4.2" fill="none" stroke="currentColor" stroke-width="2" />
              <path
                d="M12 2.8v2.1m0 14.2v2.1M4.5 4.5 6 6m12 12 1.5 1.5M2.8 12h2.1m14.2 0h2.1M4.5 19.5 6 18m12-12 1.5-1.5"
                fill="none"
                stroke="currentColor"
                stroke-linecap="round"
                stroke-width="2"
              />
            </svg>
          </button>
          <RouterLink v-for="action in authActions" :key="action.labelKey" :to="action.to">
            {{ t(action.labelKey) }}
          </RouterLink>
        </nav>
      </header>

      <div class="stage-content" :key="activeSlide.key">
        <div class="stage-copy">
          <h1 id="home-title" :class="{ 'is-platform-title': activeSlide.key === 'platform' }" :aria-label="t(activeSlide.titleKey)">
            <span
              v-for="(char, index) in heroTitleChars"
              :key="`${activeSlide.key}-${char}-${index}`"
              class="title-char"
              :style="{ '--char-index': index }"
              aria-hidden="true"
            >
              {{ char }}
            </span>
          </h1>
          <p class="hero-lead">{{ t(activeSlide.subtitleKey) }}</p>

          <div class="hero-actions" :aria-label="t('home.quickActions')">
            <RouterLink class="primary-action" :to="activeSlide.actionTo">{{ t(activeSlide.actionKey) }}</RouterLink>
            <RouterLink class="secondary-action" :to="activeSlide.secondaryTo">{{ t(activeSlide.secondaryKey) }}</RouterLink>
          </div>
        </div>

        <aside class="command-panel" :aria-label="t('home.commandLabel')">
          <div class="panel-heading">
            <strong>{{ t("home.commandTitle") }}</strong>
          </div>

          <dl class="command-stats">
            <div v-for="metric in activeSlide.metrics" :key="metric.labelKey">
              <dt>{{ t(metric.labelKey) }}</dt>
              <dd>
                {{ metric.value }}<span>{{ t(metric.unitKey) }}</span>
              </dd>
              <small>{{ t(metric.noteKey) }}</small>
            </div>
          </dl>
        </aside>
      </div>

      <div class="stage-bottom" :aria-label="t('home.carouselLabel')">
        <div class="supplier-heading">
          <strong>{{ t("home.carouselTitle") }}</strong>
          <span>{{ activeSlideIndex + 1 }} / {{ serviceSlides.length }}</span>
        </div>
        <div class="service-carousel">
          <div class="service-window">
            <div
              :class="['service-track', { 'no-transition': isTrackResetting }]"
              role="tablist"
              :aria-label="t('home.carouselLabel')"
              :style="{ '--active-index': String(trackIndex) }"
            >
              <button
                v-for="(slide, index) in visibleServiceSlides"
                :key="`${slide.key}-${index}`"
                type="button"
                role="tab"
                :aria-selected="index === trackIndex"
                :aria-label="t(slide.titleKey)"
                :class="['supplier-card', 'service-card', { active: index === trackIndex }]"
                @click="selectSlide(index)"
              >
                <img :src="slide.image" :alt="t(slide.titleKey)" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>
