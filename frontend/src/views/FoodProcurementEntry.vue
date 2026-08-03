<script setup lang="ts">
import { computed } from "vue";
import { useRoute } from "vue-router";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import FoodAfterSalesPage from "@/modules/procurement/food/pages/FoodAfterSalesPage.vue";
import FoodComparisonPage from "@/modules/procurement/food/pages/FoodComparisonPage.vue";
import FoodDemandPage from "@/modules/procurement/food/pages/FoodDemandPage.vue";
import FoodInquiryPage from "@/modules/procurement/food/pages/FoodInquiryPage.vue";
import FoodOrderPage from "@/modules/procurement/food/pages/FoodOrderPage.vue";
import FoodQuotePage from "@/modules/procurement/food/pages/FoodQuotePage.vue";
import "@/modules/procurement/food/styles/food-procurement.css";
import "@/modules/procurement/food/styles/food-quote-glass.css";
import "@/modules/procurement/food/styles/food-material-ui.css";

const route = useRoute();
const page = computed(() => String(route.meta.foodPage || "demand"));
</script>

<template>
  <WorkbenchLayout>
    <FoodDemandPage v-if="page === 'demand'" />
    <FoodInquiryPage v-else-if="page === 'inquiry'" />
    <FoodQuotePage v-else-if="page === 'quote'" />
    <FoodComparisonPage v-else-if="page === 'comparison'" />
    <FoodOrderPage v-else-if="page === 'order'" />
    <FoodAfterSalesPage v-else-if="page === 'settlement'" mode="settlement" />
    <FoodAfterSalesPage v-else mode="evaluation" />
  </WorkbenchLayout>
</template>
