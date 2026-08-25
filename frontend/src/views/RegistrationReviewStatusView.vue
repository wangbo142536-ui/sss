<script setup lang="ts">
import { ref } from "vue";
import heroPortImage from "@/assets/home-port-command.png";
import LanguageSwitch from "@/components/LanguageSwitch.vue";
import { t } from "@/i18n";

const isNightMode = ref(false);

const toggleSceneMode = () => {
  isNightMode.value = !isNightMode.value;
};
</script>

<template>
  <main :class="['auth-page', 'review-auth-page', { 'is-night': isNightMode }]">
    <img class="review-bg-image" :src="heroPortImage" alt="" aria-hidden="true" />
    <div class="review-bg-shade" aria-hidden="true"></div>

    <section class="auth-panel review-shell" :aria-label="t('page.onboarding.reviewShellLabel')">
      <header class="review-topbar">
        <RouterLink to="/" class="review-brand">
          <span aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M3 17h18" />
              <path d="M6 17V7l6-3 6 3v10" />
              <path d="M8 11h8" />
              <path d="M9 21h6" />
            </svg>
          </span>
          <strong>{{ t("app.name") }}</strong>
        </RouterLink>

        <div class="review-nav-actions">
          <button
            type="button"
            class="review-scene-button"
            :aria-label="isNightMode ? t('page.login.switchDay') : t('page.login.switchNight')"
            :title="isNightMode ? t('page.login.switchDay') : t('page.login.switchNight')"
            @click="toggleSceneMode"
          >
            <svg v-if="!isNightMode" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3v2" />
              <path d="M12 19v2" />
              <path d="M4.22 4.22 5.64 5.64" />
              <path d="m18.36 18.36 1.42 1.42" />
              <path d="M3 12h2" />
              <path d="M19 12h2" />
              <path d="m4.22 19.78 1.42-1.42" />
              <path d="m18.36 5.64 1.42-1.42" />
              <path d="M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" aria-hidden="true">
              <path d="M20.7 15.5A8.2 8.2 0 0 1 8.5 3.3 7 7 0 1 0 20.7 15.5Z" />
            </svg>
          </button>
          <LanguageSwitch class="review-scene-button review-language-button" />
        </div>
      </header>

      <div class="onboarding-review-complete" role="status" aria-live="polite">
        <span class="review-success-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24">
            <path d="m7 12 3 3 7-7" />
          </svg>
        </span>
        <strong>{{ t("page.onboarding.registrationSuccessTitle") }}</strong>
        <h1>{{ t("page.onboarding.platformReviewTitle") }}</h1>
        <p>{{ t("page.onboarding.platformReviewDescription") }}</p>
      </div>
    </section>
  </main>
</template>

<style scoped>
.review-auth-page {
  position: relative;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #f4f8fc;
  isolation: isolate;
}

.review-bg-image,
.review-bg-shade {
  position: absolute;
  inset: 0;
  z-index: -2;
}

.review-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 62% center;
  filter: saturate(0.98) contrast(1.08) brightness(1.03);
}

.review-bg-shade {
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(250, 253, 255, 0.66) 0%, rgba(247, 251, 255, 0.42) 36%, rgba(245, 249, 255, 0.12) 68%, rgba(248, 251, 255, 0.24) 100%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.38) 0%, rgba(248, 251, 255, 0.08) 42%, rgba(243, 248, 255, 0.46) 100%);
}

.review-shell {
  width: min(560px, calc(100% - 32px));
  overflow: hidden;
  border: 1px solid rgba(185, 202, 216, 0.64);
  border-radius: 10px;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.48);
  backdrop-filter: blur(18px) saturate(1.22);
}

.review-topbar {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(185, 202, 216, 0.5);
}

.review-brand,
.review-nav-actions,
.review-scene-button {
  display: inline-flex;
  align-items: center;
}

.review-brand {
  min-width: 0;
  gap: 10px;
  color: var(--color-text-strong);
  text-decoration: none;
}

.review-brand > span {
  width: 34px;
  height: 34px;
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: var(--color-primary-700);
}

.review-brand svg,
.review-scene-button svg,
.review-success-icon svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.review-brand strong {
  overflow: hidden;
  color: #0f2c4c;
  font-size: 22px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-nav-actions {
  gap: 10px;
  justify-content: flex-end;
}

.review-scene-button {
  width: 36px;
  height: 36px;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-primary-800);
  background: #ffffff;
  cursor: pointer;
}

.review-language-button {
  font-size: 12px;
  font-weight: 900;
}

.review-scene-button:hover,
.review-scene-button:focus-visible {
  border-color: var(--color-primary-500);
  color: #ffffff;
  background: var(--color-primary-700);
  outline: none;
}

.onboarding-review-complete {
  min-height: 360px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  padding: 56px 48px 64px;
  color: var(--color-text-main);
  text-align: center;
}

.review-success-icon {
  width: 72px;
  height: 72px;
  display: inline-grid;
  place-items: center;
  margin-bottom: 22px;
  border: 1px solid rgba(19, 122, 91, 0.24);
  border-radius: 50%;
  color: #0f805f;
  background: rgba(226, 246, 239, 0.9);
}

.review-success-icon svg {
  width: 34px;
  height: 34px;
  stroke-width: 2.4;
}

.onboarding-review-complete > strong {
  color: #0f805f;
  font-size: 14px;
  font-weight: 800;
}

.onboarding-review-complete h1 {
  margin: 10px 0 12px;
  color: #0f2c4c;
  font-size: clamp(28px, 5vw, 36px);
  line-height: 1.2;
}

.onboarding-review-complete p {
  max-width: 390px;
  margin: 0;
  color: var(--color-text-muted);
  font-size: 15px;
  line-height: 1.8;
}

.is-night .review-bg-image {
  filter: saturate(0.82) contrast(1.08) brightness(0.56);
}

.is-night .review-bg-shade {
  background: linear-gradient(120deg, rgba(4, 18, 34, 0.76), rgba(8, 31, 54, 0.44));
}

.is-night .review-shell {
  border-color: rgba(159, 188, 212, 0.28);
  background: rgba(9, 28, 47, 0.66);
}

.is-night .review-topbar {
  border-color: rgba(159, 188, 212, 0.22);
}

.is-night .review-brand strong,
.is-night .onboarding-review-complete h1 {
  color: #eef7ff;
}

.is-night .onboarding-review-complete p {
  color: #bad0df;
}

@media (max-width: 640px) {
  .review-shell {
    width: calc(100% - 24px);
  }

  .review-brand strong {
    font-size: 18px;
  }

  .onboarding-review-complete {
    min-height: 320px;
    padding: 44px 24px 52px;
  }
}
</style>
