<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import heroPortImage from "@/assets/home-port-command.png";
import LanguageSwitch from "@/components/LanguageSwitch.vue";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import { t } from "@/i18n";
import { ApiError, register as registerAccount, saveAuthSession } from "@/services/authService";

const router = useRouter();
const loading = ref(false);
const isNightMode = ref(false);
const account = ref("");
const password = ref("");
const confirmPassword = ref("");
const accountErrorKey = ref("");
const passwordErrorKey = ref("");
const confirmPasswordErrorKey = ref("");
const statusMessageKey = ref("");

const toggleSceneMode = () => {
  isNightMode.value = !isNightMode.value;
};

const submit = async () => {
  accountErrorKey.value = "";
  passwordErrorKey.value = "";
  confirmPasswordErrorKey.value = "";
  statusMessageKey.value = "";

  if (!account.value.trim()) {
    accountErrorKey.value = "page.register.accountRequired";
  }

  if (!password.value) {
    passwordErrorKey.value = "page.register.passwordRequired";
  }

  if (!confirmPassword.value) {
    confirmPasswordErrorKey.value = "page.register.confirmPasswordRequired";
  } else if (password.value !== confirmPassword.value) {
    confirmPasswordErrorKey.value = "page.register.passwordMismatch";
  }

  if (accountErrorKey.value || passwordErrorKey.value || confirmPasswordErrorKey.value) {
    return;
  }

  loading.value = true;
  try {
    const session = await registerAccount({
      account: account.value.trim(),
      password: password.value,
      confirmPassword: confirmPassword.value
    });
    saveAuthSession(session);
    statusMessageKey.value = "page.register.submitSuccess";
    await router.push("/onboarding/company-profile");
  } catch (error) {
    statusMessageKey.value = error instanceof ApiError && error.message === "AUTH_TOKEN_MISSING" ? "page.register.responseInvalid" : "page.register.requestFailed";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <main :class="['auth-page', 'register-auth-page', { 'is-night': isNightMode }]">
    <img class="register-bg-image" :src="heroPortImage" alt="" aria-hidden="true" />
    <div class="register-bg-shade" aria-hidden="true"></div>
    <section class="auth-panel register-shell" :aria-label="t('page.register.shellLabel')">
      <header class="register-topbar">
        <RouterLink to="/" class="register-brand">
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
        <div class="register-nav-actions">
          <button
            type="button"
            class="scene-toggle-button"
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
          <LanguageSwitch class="scene-toggle-button language-toggle-button" />
        </div>
      </header>

      <div class="register-layout">
        <LoadingOverlay :active="loading" :label="t('page.register.submitting')">
          <form class="auth-form register-form" novalidate @submit.prevent="submit">
            <label class="form-field" :class="{ 'has-error': accountErrorKey }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.register.account") }}</span>
              <input
                v-model="account"
                type="text"
                autocomplete="username"
                required
                :placeholder="t('page.register.accountPlaceholder')"
                :aria-invalid="Boolean(accountErrorKey)"
                :aria-describedby="accountErrorKey ? 'register-account-error' : undefined"
              />
              <small v-if="accountErrorKey" id="register-account-error">{{ t(accountErrorKey) }}</small>
            </label>

            <label class="form-field" :class="{ 'has-error': passwordErrorKey }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.register.password") }}</span>
              <input
                v-model="password"
                type="password"
                autocomplete="new-password"
                required
                :placeholder="t('page.register.passwordPlaceholder')"
                :aria-invalid="Boolean(passwordErrorKey)"
                :aria-describedby="passwordErrorKey ? 'register-password-error' : undefined"
              />
              <small v-if="passwordErrorKey" id="register-password-error">{{ t(passwordErrorKey) }}</small>
            </label>

            <label class="form-field" :class="{ 'has-error': confirmPasswordErrorKey }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.register.confirmPassword") }}</span>
              <input
                v-model="confirmPassword"
                type="password"
                autocomplete="new-password"
                required
                :placeholder="t('page.register.confirmPasswordPlaceholder')"
                :aria-invalid="Boolean(confirmPasswordErrorKey)"
                :aria-describedby="confirmPasswordErrorKey ? 'register-confirm-password-error' : undefined"
              />
              <small v-if="confirmPasswordErrorKey" id="register-confirm-password-error">{{ t(confirmPasswordErrorKey) }}</small>
            </label>

            <div class="form-options">
              <RouterLink to="/login" class="form-link-button">{{ t("common.login") }}</RouterLink>
            </div>

            <p v-if="statusMessageKey" class="form-notice" role="status">{{ t(statusMessageKey) }}</p>

            <button
              type="submit"
              class="primary-button register-submit"
              :disabled="loading"
              :aria-label="loading ? t('page.register.submitting') : t('page.register.submit')"
              :title="loading ? t('page.register.submitting') : t('page.register.submit')"
            >
              <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
                <path d="M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
                <path d="M19 8v6" />
                <path d="M22 11h-6" />
              </svg>
              <span>{{ loading ? t("page.register.submitting") : t("page.register.submit") }}</span>
            </button>
          </form>
        </LoadingOverlay>
      </div>
    </section>
  </main>
</template>

<style scoped>
.register-auth-page {
  position: relative;
  min-height: 100dvh;
  padding: 0;
  place-items: center;
  overflow: hidden;
  background: #f4f8fc;
  isolation: isolate;
}

.register-bg-image,
.register-bg-shade {
  position: absolute;
  inset: 0;
  z-index: -2;
}

.register-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 62% center;
  filter: saturate(0.98) contrast(1.08) brightness(1.03);
}

.register-bg-shade {
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(250, 253, 255, 0.66) 0%, rgba(247, 251, 255, 0.42) 36%, rgba(245, 249, 255, 0.12) 68%, rgba(248, 251, 255, 0.24) 100%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.38) 0%, rgba(248, 251, 255, 0.08) 42%, rgba(243, 248, 255, 0.46) 100%);
}

.register-shell {
  width: min(430px, calc(100% - 32px));
  max-height: calc(100dvh - 32px);
  min-height: auto;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid rgba(185, 202, 216, 0.64);
  border-radius: 10px;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.48);
  backdrop-filter: blur(18px) saturate(1.22);
}

.register-topbar {
  min-height: 58px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(185, 202, 216, 0.5);
}

.register-brand,
.register-nav-actions,
.scene-toggle-button,
.language-toggle-button,
.register-submit,
.form-options {
  display: inline-flex;
  align-items: center;
}

.register-brand {
  min-width: 0;
  gap: 10px;
  color: var(--color-text-strong);
  text-decoration: none;
}

.register-brand > span {
  width: 34px;
  height: 34px;
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: var(--color-primary-700);
}

.register-brand svg,
.scene-toggle-button svg,
.register-submit svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.register-brand strong {
  overflow: hidden;
  color: #0f2c4c;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.register-nav-actions {
  gap: 10px;
  justify-content: flex-end;
}

.scene-toggle-button {
  width: 36px;
  height: 36px;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-primary-800);
  background: #ffffff;
  cursor: pointer;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    transform 160ms ease;
}

.language-toggle-button {
  color: var(--color-primary-800);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

.scene-toggle-button:hover,
.scene-toggle-button:focus-visible {
  border-color: var(--color-primary-500);
  color: #ffffff;
  background: var(--color-primary-700);
  outline: none;
}

.scene-toggle-button:active,
.register-submit:active {
  transform: translateY(1px);
}

.register-shell > .register-layout {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  display: grid;
  place-items: center;
  padding: 0;
  overflow: auto;
  justify-self: stretch;
}

.register-shell .register-layout .loading-host {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  flex: 0 0 auto;
  border: 0;
  border-radius: 0;
  background: transparent;
  backdrop-filter: none;
}

.auth-form.register-form {
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 28px 36px 36px;
  align-content: center;
}

.form-field {
  gap: 7px;
}

.form-field span {
  font-size: 13px;
}

.required-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.required-label i {
  color: var(--color-danger);
  font-style: normal;
  font-weight: 900;
  line-height: 1;
}

.form-field input,
.form-field select {
  width: 100%;
  height: 40px;
  border-color: var(--color-border);
  border-radius: 8px;
  color: var(--color-text-strong);
  background: #ffffff;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    background-color 160ms ease;
}

.form-field input::placeholder {
  color: #6d8395;
}

.form-field input:hover,
.form-field select:hover {
  border-color: var(--color-border-strong);
}

.form-field input:focus,
.form-field select:focus {
  border-color: var(--color-primary-600);
  box-shadow: 0 0 0 3px rgba(17, 116, 199, 0.14);
  outline: none;
}

.form-field select {
  padding: 0 12px;
}

.form-field small {
  color: var(--color-danger);
  font-size: 12px;
  font-weight: 600;
}

.form-field.has-error input,
.form-field.has-error select {
  border-color: var(--color-danger);
}

.form-options {
  justify-content: flex-end;
}

.form-link-button {
  border: 0;
  padding: 0;
  color: var(--color-primary-700);
  background: transparent;
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.form-link-button:hover,
.form-link-button:focus-visible {
  color: var(--color-primary-900);
}

.form-notice {
  margin: 0;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-primary-900);
  background: var(--color-primary-050);
  font-size: 13px;
  line-height: 1.5;
}

.register-submit {
  width: 100%;
  min-height: 40px;
  justify-content: center;
  gap: 8px;
}

.register-submit:disabled {
  color: #71879a;
  background: #dce7ef;
  opacity: 1;
}

.button-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.48);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: register-spin 780ms linear infinite;
}

.register-auth-page.is-night {
  background: #071d33;
}

.is-night .register-shell {
  border-color: rgba(185, 202, 216, 0.22);
  background: rgba(5, 20, 31, 0.62);
}

.is-night .register-bg-image {
  filter: saturate(1.08) contrast(1.06);
}

.is-night .register-bg-shade {
  background:
    linear-gradient(90deg, rgba(2, 12, 20, 0.92) 0%, rgba(4, 17, 27, 0.68) 34%, rgba(4, 18, 27, 0.14) 68%, rgba(4, 17, 27, 0.48) 100%),
    linear-gradient(180deg, rgba(2, 10, 16, 0.68) 0%, transparent 34%, rgba(2, 10, 16, 0.74) 100%);
}

.is-night .register-brand strong,
.is-night .form-field span {
  color: #eff8ff;
}

.is-night .form-field input,
.is-night .form-field select,
.is-night .scene-toggle-button {
  border-color: rgba(185, 202, 216, 0.26);
  background: rgba(255, 255, 255, 0.9);
}

@keyframes register-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .scene-toggle-button,
  .form-field input,
  .form-field select,
  .register-submit,
  .button-spinner {
    animation: none;
    transition: none;
  }
}

@media (max-width: 900px) {
  .register-auth-page {
    padding: 0;
    overflow: auto;
  }

  .register-shell {
    min-height: auto;
  }

  .register-layout {
    padding: 0;
  }

  .register-form {
    padding: 28px 24px 32px;
  }
}

@media (max-width: 560px) {
  .register-auth-page {
    padding: 0;
  }

  .register-shell {
    width: min(390px, calc(100% - 24px));
  }

  .register-topbar {
    min-height: 58px;
    padding: 12px;
    align-items: center;
    flex-direction: row;
  }

  .register-nav-actions {
    width: auto;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .register-form {
    padding: 24px 20px 30px;
  }
}
</style>
