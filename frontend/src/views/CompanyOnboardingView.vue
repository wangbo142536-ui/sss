<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import heroPortImage from "@/assets/home-port-command.png";
import LanguageSwitch from "@/components/LanguageSwitch.vue";
import LoadingOverlay from "@/components/LoadingOverlay.vue";
import { t } from "@/i18n";
import {
  clearAuthSession,
  getCompanyProfile,
  getRegisterOptions,
  getSafeRequestErrorKey,
  isAuthExpiredError,
  resolveAuthRoute,
  submitCompanyProfile,
  uploadQualificationFile
} from "@/services/authService";
import type { CompanyProfileStatus, QualificationFile, RegisterOption } from "@/types/auth";

type UploadStatus = "ready" | "uploading" | "uploaded" | "failed";

interface UploadItem extends QualificationFile {
  localId: string;
  status: UploadStatus;
  errorKey?: string;
}

const router = useRouter();
const loading = ref(false);
const profileLoading = ref(false);
const isNightMode = ref(false);
const companyTypeOptions = ref<RegisterOption[]>([]);
const companyType = ref("SUPPLIER");
const companyName = ref("");
const unifiedSocialCreditCode = ref("");
const contactName = ref("");
const contactPhone = ref("");
const contactEmail = ref("");
const files = ref<UploadItem[]>([]);
const profileStatus = ref<CompanyProfileStatus>("PROFILE_REQUIRED");
const reviewReason = ref("");
const statusMessageKey = ref("");
const fieldErrors = ref<Record<string, string>>({});
const reauthRequired = ref(false);
let uploadSequence = 0;

const activeStatuses = new Set<CompanyProfileStatus>(["PENDING_REVIEW", "ACTIVE"]);

const toggleSceneMode = () => {
  isNightMode.value = !isNightMode.value;
};

const getCompanyTypeLabel = (option: RegisterOption) => {
  if (option.labelKey) {
    return t(option.labelKey);
  }

  return option.label || option.value;
};

const setProfileOptions = async () => {
  const options = await getRegisterOptions();
  companyTypeOptions.value = options.filter((option) => option.value === "SHIP_AGENT" || option.value === "SUPPLIER");
  if (!companyTypeOptions.value.length) {
    companyTypeOptions.value = [
      { value: "SUPPLIER", labelKey: "page.register.companyTypeSupplier" },
      { value: "SHIP_AGENT", labelKey: "page.register.companyTypeShipAgent" }
    ];
  }

  if (!companyTypeOptions.value.some((option) => option.value === companyType.value)) {
    companyType.value = companyTypeOptions.value.find((option) => option.value === "SUPPLIER")?.value ?? companyTypeOptions.value[0]?.value ?? "SUPPLIER";
  }
};

const handleRequestError = (error: unknown, fallbackKey: string) => {
  const messageKey = getSafeRequestErrorKey(error, fallbackKey);
  statusMessageKey.value = messageKey;

  if (isAuthExpiredError(error)) {
    clearAuthSession();
    reauthRequired.value = true;
  }

  return messageKey;
};

const updateFile = (localId: string, patch: Partial<UploadItem>) => {
  const fileIndex = files.value.findIndex((file) => file.localId === localId);
  if (fileIndex < 0) return false;

  files.value = files.value.map((file, index) => (index === fileIndex ? { ...file, ...patch } : file));
  return true;
};

const isSupportedCompanyType = (value: string) => companyTypeOptions.value.some((option) => option.value === value);

const resolveCompanyTypeValue = (value: string, status: CompanyProfileStatus) => {
  if (status !== "PROFILE_REQUIRED" && value && isSupportedCompanyType(value)) {
    return value;
  }

  return isSupportedCompanyType("SUPPLIER") ? "SUPPLIER" : (companyTypeOptions.value[0]?.value ?? "SUPPLIER");
};

const isTemporaryCompanyName = (value: string) => /^待完善企业(?:[-_].*)?$/.test(value.trim());

onMounted(async () => {
  profileLoading.value = true;
  await setProfileOptions();

  try {
    const profile = await getCompanyProfile();
    if (profile) {
      const nextProfileStatus = profile.status || "PROFILE_REQUIRED";
      const hasSubmittedProfile = nextProfileStatus !== "PROFILE_REQUIRED";
      const nextCompanyName = profile.companyName || "";

      companyType.value = resolveCompanyTypeValue(profile.companyType || "", nextProfileStatus);
      companyName.value = hasSubmittedProfile && !isTemporaryCompanyName(nextCompanyName) ? nextCompanyName : "";
      unifiedSocialCreditCode.value = hasSubmittedProfile ? (profile.unifiedSocialCreditCode || "") : "";
      contactName.value = profile.contactName || "";
      contactPhone.value = profile.contactPhone || "";
      contactEmail.value = profile.contactEmail || "";
      files.value = profile.qualificationFiles.map((file, index) => ({
        ...file,
        localId: `${Date.now()}-${index}`,
        status: "uploaded"
      }));
      profileStatus.value = nextProfileStatus;
      reviewReason.value = profile.reviewReason || "";
    }
  } catch (error) {
    handleRequestError(error, "page.onboarding.requestFailed");
    profileStatus.value = "PROFILE_REQUIRED";
  } finally {
    profileLoading.value = false;
  }
});

const addFiles = async (event: Event) => {
  const input = event.target as HTMLInputElement;
  const selectedFiles = Array.from(input.files ?? []);
  input.value = "";

  for (const selectedFile of selectedFiles) {
    uploadSequence += 1;
    const localId = `qualification-${uploadSequence}`;
    const localFile: UploadItem = {
      localId,
      name: selectedFile.name,
      status: "uploading"
    };
    files.value = [...files.value, localFile];

    try {
      const uploaded = await uploadQualificationFile(selectedFile);
      const updated = updateFile(localId, { ...uploaded, localId, status: "uploaded", errorKey: "" });
      if (!updated) {
        files.value = [...files.value, { ...uploaded, localId, status: "uploaded", errorKey: "" }];
      }
      if (fieldErrors.value.files) {
        fieldErrors.value = { ...fieldErrors.value, files: "" };
      }
    } catch (error) {
      updateFile(localId, {
        status: "failed",
        errorKey: handleRequestError(error, "page.onboarding.fileUploadFailed")
      });
      if (isAuthExpiredError(error)) return;
    }
  }
};

const removeFile = (localId: string) => {
  files.value = files.value.filter((file) => file.localId !== localId);
};

const validate = () => {
  const errors: Record<string, string> = {};

  if (!companyType.value) errors.companyType = "page.onboarding.companyTypeRequired";
  if (!companyName.value.trim()) errors.companyName = "page.onboarding.companyNameRequired";
  if (!unifiedSocialCreditCode.value.trim()) errors.creditCode = "page.onboarding.creditCodeRequired";
  if (!contactName.value.trim()) errors.contactName = "page.onboarding.contactNameRequired";
  if (!contactPhone.value.trim()) errors.contactPhone = "page.onboarding.contactPhoneRequired";
  if (!contactEmail.value.trim()) errors.contactEmail = "page.onboarding.contactEmailRequired";
  if (!files.value.length) errors.files = "page.onboarding.fileRequired";
  else if (files.value.some((file) => file.status === "uploading")) errors.files = "page.onboarding.fileUploading";
  else if (!files.value.some((file) => file.status === "uploaded")) errors.files = "page.onboarding.fileUploadRequired";

  fieldErrors.value = errors;
  return !Object.keys(errors).length;
};

const submit = async () => {
  statusMessageKey.value = "";
  reauthRequired.value = false;

  if (!validate()) {
    return;
  }

  loading.value = true;
  try {
    const submittedProfile = await submitCompanyProfile({
      companyType: companyType.value,
      companyName: companyName.value.trim(),
      unifiedSocialCreditCode: unifiedSocialCreditCode.value.trim(),
      contactName: contactName.value.trim(),
      contactPhone: contactPhone.value.trim(),
      contactEmail: contactEmail.value.trim(),
      qualificationFileIds: files.value.filter((file) => file.status === "uploaded").map((file) => file.fileId ?? file.id ?? file.name),
      qualificationFiles: files.value.filter((file) => file.status === "uploaded")
    });
    profileStatus.value = submittedProfile?.status || "PENDING_REVIEW";
    reviewReason.value = submittedProfile?.reviewReason || "";
    statusMessageKey.value = "page.onboarding.submitSuccess";
  } catch (error) {
    handleRequestError(error, "page.onboarding.requestFailed");
  } finally {
    loading.value = false;
  }
};

const enterWorkbench = async () => {
  await router.push(resolveAuthRoute({ token: "", roles: [], permissions: [], menus: [], defaultRoute: "/dashboard-government", profileStatus: "ACTIVE" }));
};
</script>

<template>
  <main :class="['auth-page', 'onboarding-auth-page', { 'is-night': isNightMode }]">
    <img class="onboarding-bg-image" :src="heroPortImage" alt="" aria-hidden="true" />
    <div class="onboarding-bg-shade" aria-hidden="true"></div>
    <section class="auth-panel onboarding-shell" :aria-label="t('page.onboarding.shellLabel')">
      <header class="onboarding-topbar">
        <RouterLink to="/" class="onboarding-brand">
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
        <div class="onboarding-nav-actions">
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

      <div class="onboarding-layout">
        <LoadingOverlay :active="loading || profileLoading" :label="loading ? t('page.onboarding.submitting') : t('common.loading')">
          <form class="auth-form onboarding-form" novalidate @submit.prevent="submit">
            <div v-if="profileStatus === 'PENDING_REVIEW'" class="review-panel" role="status">
              <strong>{{ t("page.onboarding.pendingTitle") }}</strong>
              <span>{{ t("page.onboarding.pendingDescription") }}</span>
            </div>
            <div v-else-if="profileStatus === 'REJECTED'" class="review-panel is-rejected" role="status">
              <strong>{{ t("page.onboarding.rejectedTitle") }}</strong>
              <span>{{ reviewReason || t("page.onboarding.rejectedDescription") }}</span>
            </div>
            <div v-else-if="profileStatus === 'ACTIVE'" class="review-panel" role="status">
              <strong>{{ t("page.onboarding.activeTitle") }}</strong>
              <span>{{ t("page.onboarding.activeDescription") }}</span>
            </div>

            <label class="form-field" :class="{ 'has-error': fieldErrors.companyType }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.onboarding.companyType") }}</span>
              <select
                v-model="companyType"
                :disabled="activeStatuses.has(profileStatus) || reauthRequired"
                required
                :aria-invalid="Boolean(fieldErrors.companyType)"
                :aria-describedby="fieldErrors.companyType ? 'onboarding-company-type-error' : undefined"
              >
                <option v-for="option in companyTypeOptions" :key="option.value" :value="option.value">
                  {{ getCompanyTypeLabel(option) }}
                </option>
              </select>
              <small v-if="fieldErrors.companyType" id="onboarding-company-type-error">{{ t(fieldErrors.companyType) }}</small>
            </label>

            <label class="form-field" :class="{ 'has-error': fieldErrors.companyName }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.onboarding.companyName") }}</span>
              <input
                v-model="companyName"
                :disabled="activeStatuses.has(profileStatus) || reauthRequired"
                type="text"
                autocomplete="organization"
                required
                :placeholder="t('page.onboarding.companyNamePlaceholder')"
                :aria-invalid="Boolean(fieldErrors.companyName)"
                :aria-describedby="fieldErrors.companyName ? 'onboarding-company-name-error' : undefined"
              />
              <small v-if="fieldErrors.companyName" id="onboarding-company-name-error">{{ t(fieldErrors.companyName) }}</small>
            </label>

            <label class="form-field" :class="{ 'has-error': fieldErrors.creditCode }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.onboarding.creditCode") }}</span>
              <input
                v-model="unifiedSocialCreditCode"
                :disabled="activeStatuses.has(profileStatus) || reauthRequired"
                type="text"
                required
                :placeholder="t('page.onboarding.creditCodePlaceholder')"
                :aria-invalid="Boolean(fieldErrors.creditCode)"
                :aria-describedby="fieldErrors.creditCode ? 'onboarding-credit-code-error' : undefined"
              />
              <small v-if="fieldErrors.creditCode" id="onboarding-credit-code-error">{{ t(fieldErrors.creditCode) }}</small>
            </label>

            <div class="form-grid">
              <label class="form-field" :class="{ 'has-error': fieldErrors.contactName }">
                <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.onboarding.contactName") }}</span>
                <input v-model="contactName" :disabled="activeStatuses.has(profileStatus) || reauthRequired" type="text" autocomplete="name" required :placeholder="t('page.onboarding.contactNamePlaceholder')" />
                <small v-if="fieldErrors.contactName">{{ t(fieldErrors.contactName) }}</small>
              </label>

              <label class="form-field" :class="{ 'has-error': fieldErrors.contactPhone }">
                <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.onboarding.contactPhone") }}</span>
                <input v-model="contactPhone" :disabled="activeStatuses.has(profileStatus) || reauthRequired" type="tel" autocomplete="tel" required :placeholder="t('page.onboarding.contactPhonePlaceholder')" />
                <small v-if="fieldErrors.contactPhone">{{ t(fieldErrors.contactPhone) }}</small>
              </label>
            </div>

            <label class="form-field" :class="{ 'has-error': fieldErrors.contactEmail }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.onboarding.contactEmail") }}</span>
              <input
                v-model="contactEmail"
                :disabled="activeStatuses.has(profileStatus) || reauthRequired"
                type="email"
                autocomplete="email"
                required
                :placeholder="t('page.onboarding.contactEmailPlaceholder')"
              />
              <small v-if="fieldErrors.contactEmail">{{ t(fieldErrors.contactEmail) }}</small>
            </label>

            <div class="upload-field" :class="{ 'has-error': fieldErrors.files }">
              <span class="required-label"><i aria-hidden="true">*</i>{{ t("page.onboarding.qualificationFiles") }}</span>
              <label class="upload-drop">
                <input type="file" :disabled="activeStatuses.has(profileStatus) || reauthRequired" multiple @change="addFiles" />
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M12 3v12" />
                  <path d="m7 8 5-5 5 5" />
                  <path d="M5 21h14" />
                </svg>
                <strong>{{ t("page.onboarding.uploadButton") }}</strong>
                <small>{{ t("page.onboarding.uploadHint") }}</small>
              </label>
              <small v-if="fieldErrors.files" class="field-error">{{ t(fieldErrors.files) }}</small>

              <div v-if="files.length" class="file-list">
                <div v-for="file in files" :key="file.localId" class="file-row" :class="`is-${file.status}`">
                  <span>{{ file.name }}</span>
                  <small>{{ t(`page.onboarding.fileStatus.${file.status}`) }}</small>
                  <button
                    v-if="!activeStatuses.has(profileStatus) && !reauthRequired"
                    type="button"
                    :aria-label="t('page.onboarding.removeFile')"
                    :title="t('page.onboarding.removeFile')"
                    @click="removeFile(file.localId)"
                  >
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M18 6 6 18" />
                      <path d="m6 6 12 12" />
                    </svg>
                  </button>
                  <small v-if="file.errorKey" class="field-error">{{ t(file.errorKey) }}</small>
                </div>
              </div>
            </div>

            <p v-if="statusMessageKey" class="form-notice" role="status">{{ t(statusMessageKey) }}</p>

            <RouterLink v-if="reauthRequired" to="/login" class="primary-button onboarding-submit">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                <path d="m10 17 5-5-5-5" />
                <path d="M15 12H3" />
              </svg>
              <span>{{ t("page.onboarding.relogin") }}</span>
            </RouterLink>

            <button
              v-if="profileStatus !== 'ACTIVE'"
              type="submit"
              class="primary-button onboarding-submit"
              :disabled="loading || profileStatus === 'PENDING_REVIEW' || reauthRequired"
              :aria-label="loading ? t('page.onboarding.submitting') : t('page.onboarding.submit')"
              :title="loading ? t('page.onboarding.submitting') : t('page.onboarding.submit')"
            >
              <span v-if="loading" class="button-spinner" aria-hidden="true"></span>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M20 6 9 17l-5-5" />
              </svg>
              <span>{{ loading ? t("page.onboarding.submitting") : t("page.onboarding.submit") }}</span>
            </button>

            <button v-else type="button" class="primary-button onboarding-submit" @click="enterWorkbench">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
                <path d="m10 17 5-5-5-5" />
                <path d="M15 12H3" />
              </svg>
              <span>{{ t("page.onboarding.enterWorkbench") }}</span>
            </button>

            <RouterLink to="/login" class="form-link-button">{{ t("common.login") }}</RouterLink>
          </form>
        </LoadingOverlay>
      </div>
    </section>
  </main>
</template>

<style scoped>
.onboarding-auth-page {
  position: relative;
  min-height: 100dvh;
  padding: 0;
  place-items: center;
  overflow: hidden;
  background: #f4f8fc;
  isolation: isolate;
}

.onboarding-bg-image,
.onboarding-bg-shade {
  position: absolute;
  inset: 0;
  z-index: -2;
}

.onboarding-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: 62% center;
  filter: saturate(0.98) contrast(1.08) brightness(1.03);
}

.onboarding-bg-shade {
  z-index: -1;
  background:
    linear-gradient(90deg, rgba(250, 253, 255, 0.66) 0%, rgba(247, 251, 255, 0.42) 36%, rgba(245, 249, 255, 0.12) 68%, rgba(248, 251, 255, 0.24) 100%),
    linear-gradient(180deg, rgba(248, 251, 255, 0.38) 0%, rgba(248, 251, 255, 0.08) 42%, rgba(243, 248, 255, 0.46) 100%);
}

.onboarding-shell {
  width: min(560px, calc(100% - 32px));
  max-height: calc(100dvh - 32px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid rgba(185, 202, 216, 0.64);
  border-radius: 10px;
  box-shadow: none;
  background: rgba(255, 255, 255, 0.48);
  backdrop-filter: blur(18px) saturate(1.22);
}

.onboarding-topbar {
  min-height: 58px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(185, 202, 216, 0.5);
}

.onboarding-brand,
.onboarding-nav-actions,
.scene-toggle-button,
.language-toggle-button,
.onboarding-submit,
.form-link-button {
  display: inline-flex;
  align-items: center;
}

.onboarding-brand {
  min-width: 0;
  gap: 10px;
  color: var(--color-text-strong);
  text-decoration: none;
}

.onboarding-brand > span {
  width: 34px;
  height: 34px;
  display: inline-grid;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: var(--color-primary-700);
}

.onboarding-brand svg,
.scene-toggle-button svg,
.onboarding-submit svg,
.upload-drop svg,
.file-row button svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.onboarding-brand strong {
  overflow: hidden;
  color: #0f2c4c;
  font-size: 22px;
  font-weight: 900;
  letter-spacing: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.onboarding-nav-actions {
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

.onboarding-layout {
  min-height: 0;
  overflow: auto;
}

.onboarding-layout .loading-host {
  border: 0;
  border-radius: 0;
  background: transparent;
  backdrop-filter: none;
}

.auth-form.onboarding-form {
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 26px 34px 34px;
  align-content: center;
}

.review-panel {
  display: grid;
  gap: 4px;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-primary-900);
  background: rgba(232, 243, 252, 0.82);
  font-size: 13px;
  line-height: 1.5;
}

.review-panel.is-rejected {
  border-color: rgba(183, 28, 28, 0.24);
  color: var(--color-danger);
  background: rgba(255, 241, 241, 0.86);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-field {
  gap: 7px;
}

.form-field span,
.upload-field > span {
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

.form-field select {
  padding: 0 12px;
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

.form-field small,
.field-error {
  color: var(--color-danger);
  font-size: 12px;
  font-weight: 600;
}

.form-field.has-error input,
.form-field.has-error select,
.upload-field.has-error .upload-drop {
  border-color: var(--color-danger);
}

.upload-field {
  display: grid;
  gap: 8px;
}

.upload-drop {
  min-height: 86px;
  display: grid;
  place-items: center;
  gap: 4px;
  padding: 14px;
  border: 1px dashed var(--color-border-strong);
  border-radius: 8px;
  color: var(--color-primary-800);
  background: rgba(255, 255, 255, 0.74);
  cursor: pointer;
  text-align: center;
  transition:
    border-color 160ms ease,
    background-color 160ms ease,
    color 160ms ease;
}

.upload-drop input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}

.upload-drop:hover,
.upload-drop:focus-within {
  border-color: var(--color-primary-600);
  color: var(--color-primary-900);
  background: rgba(232, 243, 252, 0.86);
}

.upload-drop small {
  color: #526a7d;
  font-size: 12px;
}

.file-list {
  display: grid;
  gap: 6px;
}

.file-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto 28px;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.file-row span {
  overflow: hidden;
  color: var(--color-text-strong);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-row small {
  color: var(--color-text-main);
  font-size: 12px;
}

.file-row.is-failed small {
  color: var(--color-danger);
}

.file-row button {
  width: 28px;
  height: 28px;
  display: inline-grid;
  place-items: center;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  color: var(--color-primary-800);
  background: #ffffff;
  cursor: pointer;
}

.file-row .field-error {
  grid-column: 1 / -1;
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

.onboarding-submit {
  width: 100%;
  min-height: 40px;
  justify-content: center;
  gap: 8px;
}

.onboarding-submit:disabled {
  color: #71879a;
  background: #dce7ef;
  opacity: 1;
}

.form-link-button {
  justify-content: center;
  color: var(--color-primary-700);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.button-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.48);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: onboarding-spin 780ms linear infinite;
}

.onboarding-auth-page.is-night {
  background: #071d33;
}

.is-night .onboarding-shell {
  border-color: rgba(185, 202, 216, 0.22);
  background: rgba(5, 20, 31, 0.62);
}

.is-night .onboarding-bg-image {
  filter: saturate(1.08) contrast(1.06);
}

.is-night .onboarding-bg-shade {
  background:
    linear-gradient(90deg, rgba(2, 12, 20, 0.92) 0%, rgba(4, 17, 27, 0.68) 34%, rgba(4, 18, 27, 0.14) 68%, rgba(4, 17, 27, 0.48) 100%),
    linear-gradient(180deg, rgba(2, 10, 16, 0.68) 0%, transparent 34%, rgba(2, 10, 16, 0.74) 100%);
}

.is-night .onboarding-brand strong,
.is-night .form-field span,
.is-night .upload-field > span {
  color: #eff8ff;
}

.is-night .form-field input,
.is-night .form-field select,
.is-night .scene-toggle-button,
.is-night .upload-drop,
.is-night .file-row,
.is-night .file-row button {
  border-color: rgba(185, 202, 216, 0.26);
  background: rgba(255, 255, 255, 0.9);
}

@keyframes onboarding-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .scene-toggle-button,
  .form-field input,
  .form-field select,
  .upload-drop,
  .onboarding-submit,
  .button-spinner {
    animation: none;
    transition: none;
  }
}

@media (max-width: 680px) {
  .onboarding-auth-page {
    overflow: auto;
  }

  .onboarding-shell {
    width: min(390px, calc(100% - 24px));
    max-height: calc(100dvh - 24px);
  }

  .onboarding-topbar {
    min-height: 58px;
    padding: 12px;
    align-items: center;
    flex-direction: row;
  }

  .onboarding-form {
    padding: 22px 20px 28px;
  }

  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .file-row {
    grid-template-columns: minmax(0, 1fr) auto;
  }
}
</style>
