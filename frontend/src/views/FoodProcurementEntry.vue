<script setup lang="ts">
import StatusBadge from "@/components/StatusBadge.vue";
import { t } from "@/i18n";

const hasValidFoodQuotes = false;
</script>

<template>
  <main class="food-flow-page">
    <section class="food-flow">
      <header>
        <RouterLink class="link-button" to="/">{{ t("common.backHome") }}</RouterLink>
        <h1>{{ t("nav.food") }}</h1>
        <p>{{ t("compare.foodDisabled") }}</p>
      </header>

      <div class="food-steps">
        <article class="food-step">
          <strong>1. {{ t("action.createInquiry") }}</strong>
          <StatusBadge :label="t('status.inquiry')" variant="info" />
          <p>{{ t("page.inquiries.subtitle") }}</p>
          <RouterLink class="primary-button" to="/inquiries">{{ t("action.createInquiry") }}</RouterLink>
        </article>
        <article class="food-step">
          <strong>2. {{ t("nav.quotes") }}</strong>
          <StatusBadge :label="t('status.pending')" variant="warning" />
          <p>{{ t("page.quotes.subtitle") }}</p>
          <RouterLink class="ghost-button" to="/quotes">{{ t("action.sendQuote") }}</RouterLink>
        </article>
        <article class="food-step">
          <strong>3. {{ t("nav.compare") }}</strong>
          <StatusBadge :label="t('status.blocked')" variant="danger" />
          <p>{{ t("compare.foodDisabled") }}</p>
          <button type="button" class="ghost-button" :disabled="!hasValidFoodQuotes" :title="t('compare.foodDisabled')">
            {{ t("action.compare") }}
          </button>
        </article>
      </div>

      <aside class="food-guard">
        <div>
          <strong>{{ t("common.disabledReason") }}</strong>
          <p>{{ t("compare.foodDisabled") }}</p>
        </div>
        <RouterLink class="ghost-button" to="/procurement/requests">{{ t("nav.requests") }}</RouterLink>
      </aside>
    </section>
  </main>
</template>
