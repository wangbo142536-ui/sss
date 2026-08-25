import type { RouteRecordRaw } from "vue-router";
import RegistrationReviewStatusView from "@/views/RegistrationReviewStatusView.vue";

export const registrationReviewRoute: RouteRecordRaw = {
  path: "/onboarding/review-status",
  name: "registration-review-status",
  component: RegistrationReviewStatusView,
  meta: { title: "注册审核" }
};
