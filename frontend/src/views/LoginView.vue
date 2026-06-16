<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
import heroPortImage from "@/assets/home-port-command.png";
import LanguageSwitch from "@/components/LanguageSwitch.vue";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import { t } from "@/i18n";
import { getCurrentAuth, login as loginWithPassword, resolveAuthRoute, saveAuthSession } from "@/services/authService";

const router = useRouter();
const loading = ref(false);
const account = ref("");
const password = ref("");
const remember = ref(true);
const showPassword = ref(false);
const isNightMode = ref(false);
const accountErrorKey = ref("");
const passwordErrorKey = ref("");
const statusMessageKey = ref("");

const toggleSceneMode = () => {
  isNightMode.value = !isNightMode.value;
};

const togglePassword = () => {
  showPassword.value = !showPassword.value;
};

const showRecoveryHint = () => {
  statusMessageKey.value = "page.login.recoveryHint";
};

const submit = async () => {
  accountErrorKey.value = "";
  passwordErrorKey.value = "";
  statusMessageKey.value = "";

  if (!account.value.trim()) {
    accountErrorKey.value = "page.login.accountRequired";
  }

  if (!password.value) {
    passwordErrorKey.value = "page.login.passwordRequired";
  }

  if (accountErrorKey.value || passwordErrorKey.value) {
    return;
  }

  loading.value = true;
  try {
    const session = await loginWithPassword({
      account: account.value.trim(),
      password: password.value,
      remember: remember.value
    });
    saveAuthSession(session);
    let nextSession = session;
    try {
      nextSession = await getCurrentAuth();
      saveAuthSession(nextSession);
    } catch {
      nextSession = session;
    }
    await router.push(resolveAuthRoute(nextSession));
  } catch {
    statusMessageKey.value = "page.login.requestFailed";
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <main :class="['auth-page', 'login-auth-page', { 'is-night': isNightMode }]">
    <img class="login-bg-image" :src="heroPortImage" alt="" aria-hidden="true" />
    <div class="login-bg-shade" aria-hidden="true"></div>
    <section class="auth-panel login-shell" :aria-label="t('page.login.shellLabel')">
      <header class="login-topbar">
        <RouterLink to="/" class="login-brand">
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
        <div class="login-nav-actions">
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

      <div class="login-layout">
        <LoadingOverlay :active="loading" :label="t('page.login.loggingIn')">
          <form class="auth-form login-form" @submit.prevent="submit">
            <label class="form-field" :class="{ 'has-error': accountErrorKey }">
              <span>{{ t("page.login.account") }}</span>
              <input
                v-model="account"
                type="text"
                autocomplete="username"
                :placeholder="t('page.login.accountPlaceholder')"
                :aria-invalid="Boolean(accountErrorKey)"
                :aria-describedby="accountErrorKey ? 'login-account-error' : undefined"
              />
              <small v-if="accountErrorKey" id="login-account-error">{{ t(accountErrorKey) }}</small>
            </label>

            <label class="form-field password-field" :class="{ 'has-error': passwordErrorKey }">
              <span>{{ t("page.login.password") }}</span>
              <div class="password-control">
                <input
                  v-model="password"
                  :type="showPassword ? 'text' : 'password'"
                  autocomplete="current-password"
                  :placeholder="t('page.login.passwordPlaceholder')"
                  :aria-invalid="Boolean(passwordErrorKey)"
                  :aria-describedby="passwordErrorKey ? 'login-password-error' : undefined"
                />
                <button
                  type="button"
                  class="password-toggle"
                  :aria-label="showPassword ? t('page.login.hidePassword') : t('page.login.showPassword')"
                  :title="showPassword ? t('page.login.hidePassword') : t('page.login.showPassword')"
                  @click="togglePassword"
                >
                  <svg v-if="!showPassword" viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z" />
                    <path d="M12 15.2a3.2 3.2 0 1 0 0-6.4 3.2 3.2 0 0 0 0 6.4Z" />
                  </svg>
                  <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m3 3 18 18" />
                    <path d="M10.6 10.6a3.2 3.2 0 0 0 3.8 3.8" />
                    <path d="M7.4 7.8C4.3 9.2 2.5 12 2.5 12s3.5 6 9.5 6c1.8 0 3.3-.5 4.6-1.2" />
                    <path d="M13.8 6.2C18.5 7.1 21.5 12 21.5 12a16.1 16.1 0 0 1-2.4 3" />
                  </svg>
                </button>
              </div>
              <small v-if="passwordErrorKey" id="login-password-error">{{ t(passwordErrorKey) }}</small>
            </label>

            <div class="form-options">
              <label class="check-row">
                <input v-model="remember" type="checkbox" />
                <span>{{ t("page.login.remember") }}</span>
              </label>
              <div class="form-links">
                <RouterLink to="/register" class="form-link-button">{{ t("common.register") }}</RouterLink>
                <button type="button" class="forgot-button" @click="showRecoveryHint">
                  {{ t("page.login.forgotPassword") }}
                </button>
              </div>
            </div>

            <p v-if="statusMessageKey" class="form-notice" role="status">{{ t(statusMessageKey) }}</p>

            <button
              type="submit"
              class="primary-button login-submit"
              :disabled="loading"
              :aria-label="loading ? t('page.login.loggingIn') : t('page.login.submit')"
              :title="loading ? t('page.login.loggingIn') : t('page.login.submit')"
            >
              <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                <path d="m10 17 5-5-5-5" />
                <path d="M15 12H3" />
              </svg>
              <span>{{ loading ? t("page.login.loggingIn") : t("page.login.submit") }}</span>
            </button>
          </form>
        </LoadingOverlay>
      </div>
    </section>
  </main>
</template>

<style scoped>
.login-auth-page {
  position: relative;
  min-height: 100dvh;
  padding: 0;
  place-items: center;
  overflow: hidden;
  background: #f4f8fc;
  isolation: isolate;
}

.login-bg-image,
.login-bg-shade {
  position: absolute;
  inset: 0;
  z-index: -2;
}

.login-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 62% center;
  filter: saturate(0.98) contrast(1.08) brightness(1.03);
}

.login-bg-shade {
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(250, 253, 255, 0.66) 0%, rgba(247, 251, 255, 0.42) 36%, rgba(245, 249, 255, 0.12) 68%, rgba(248, 251, 255, 0.24) 100%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.38) 0%, rgba(248, 251, 255, 0.08) 42%, rgba(243, 248, 255, 0.46) 100%);
}

.login-shell {
  width: min(430px, calc(100% - 32px));
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

.login-topbar {
  min-height: 58px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(185, 202, 216, 0.5);
}

.login-brand,
.login-nav-actions,
.scene-toggle-button,
.language-toggle-button,
.password-toggle,
.login-submit,
.form-links {
  display: inline-flex;
  align-items: center;
}

.login-brand {
  min-width: 0;
  gap: 10px;
  color: var(--color-text-strong);
  text-decoration: none;
}

.login-brand > span {
  width: 34px;
  height: 34px;
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: var(--color-primary-700);
}

.login-brand svg,
.scene-toggle-button svg,
.password-toggle svg,
.login-submit svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.login-brand strong {
  overflow: hidden;
  color: #0f2c4c;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-nav-actions {
  gap: 10px;
  justify-content: flex-end;
}

.scene-toggle-button,
.password-toggle {
  justify-content: center;
  border: 1px solid var(--color-border);
  color: var(--color-primary-800);
  background: #ffffff;
  cursor: pointer;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    color 160ms ease,
    transform 160ms ease;
}

.scene-toggle-button {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

.language-toggle-button {
  color: var(--color-primary-800);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0;
}

.scene-toggle-button:hover,
.scene-toggle-button:focus-visible,
.password-toggle:hover,
.password-toggle:focus-visible {
  border-color: var(--color-primary-500);
  color: #ffffff;
  background: var(--color-primary-700);
  outline: none;
}

.scene-toggle-button:active,
.password-toggle:active,
.login-submit:active {
  transform: translateY(1px);
}

.login-shell > .login-layout {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
  display: grid;
  place-items: center;
  padding: 0;
  overflow: hidden;
  justify-self: stretch;
}

.login-shell .login-layout .loading-host {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  flex: 0 0 auto;
  border: 0;
  border-radius: 0;
  background: transparent;
  backdrop-filter: none;
}

.auth-form.login-form {
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 32px 36px 36px;
  align-content: center;
}

.form-heading {
  display: grid;
  gap: 8px;
}

.form-heading h2 {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 24px;
  line-height: 1.3;
}

.form-field {
  gap: 7px;
}

.form-field span {
  font-size: 13px;
}

.form-field input {
  width: 100%;
  height: 40px;
  border-color: var(--color-border);
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

.form-field input:hover {
  border-color: var(--color-border-strong);
}

.form-field input:focus {
  border-color: var(--color-primary-600);
  box-shadow: 0 0 0 3px rgba(17, 116, 199, 0.14);
  outline: none;
}

.form-field small {
  color: var(--color-danger);
  font-size: 12px;
  font-weight: 600;
}

.form-field.has-error input,
.form-field.has-error .password-control {
  border-color: var(--color-danger);
}

.password-control {
  height: 40px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 36px;
  align-items: center;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #ffffff;
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.password-control:focus-within {
  border-color: var(--color-primary-600);
  box-shadow: 0 0 0 3px rgba(17, 116, 199, 0.14);
}

.password-control input {
  height: 38px;
  border: 0;
  box-shadow: none;
}

.password-control input:focus {
  box-shadow: none;
}

.password-toggle {
  width: 30px;
  height: 30px;
  margin-right: 4px;
  border-radius: 6px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.login-form .check-row {
  color: var(--color-text-main);
  font-size: 13px;
}

.login-form .check-row input {
  accent-color: var(--color-primary-700);
}

.form-links {
  gap: 14px;
  justify-content: flex-end;
}

.forgot-button,
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

.forgot-button:hover,
.forgot-button:focus-visible,
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

.login-submit {
  width: 100%;
  min-height: 40px;
  justify-content: center;
  gap: 8px;
}

.login-submit:disabled {
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
  animation: login-spin 780ms linear infinite;
}

.login-auth-page.is-night {
  background: #071d33;
}

.is-night .login-shell {
  border-color: rgba(185, 202, 216, 0.22);
  background: rgba(5, 20, 31, 0.62);
}

.is-night .login-bg-image {
  filter: saturate(1.08) contrast(1.06);
}

.is-night .login-bg-shade {
  background:
    linear-gradient(90deg, rgba(2, 12, 20, 0.92) 0%, rgba(4, 17, 27, 0.68) 34%, rgba(4, 18, 27, 0.14) 68%, rgba(4, 17, 27, 0.48) 100%),
    linear-gradient(180deg, rgba(2, 10, 16, 0.68) 0%, transparent 34%, rgba(2, 10, 16, 0.74) 100%);
}

.is-night .login-brand strong,
.is-night .form-field span,
.is-night .login-form .check-row {
  color: #eff8ff;
}

.is-night .form-field input,
.is-night .password-control,
.is-night .scene-toggle-button,
.is-night .password-toggle {
  border-color: rgba(185, 202, 216, 0.26);
  background: rgba(255, 255, 255, 0.9);
}

@keyframes login-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .scene-toggle-button,
  .password-toggle,
  .form-field input,
  .password-control,
  .login-submit,
  .button-spinner {
    animation: none;
    transition: none;
  }
}

@media (max-width: 900px) {
  .login-auth-page {
    padding: 0;
    overflow: auto;
  }

  .login-shell {
    min-height: auto;
  }

  .login-layout {
    padding: 0;
  }

  .login-form {
    padding: 28px 24px 32px;
  }
}

@media (max-width: 560px) {
  .login-auth-page {
    padding: 0;
  }

  .login-shell {
    width: min(390px, calc(100% - 24px));
  }

  .login-topbar {
    min-height: 58px;
    padding: 12px;
    align-items: center;
    flex-direction: row;
  }

  .login-nav-actions {
    width: auto;
    justify-content: flex-end;
    flex-wrap: wrap;
  }

  .login-form {
    padding: 24px 20px 30px;
  }

  .form-options {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
