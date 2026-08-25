import { describe, expect, it } from "vitest";
import { registrationReviewRoute } from "@/router/registrationReviewRoute";
import { resolveAuthRoute } from "@/services/authService";
import type { AuthSession, CompanyProfileStatus } from "@/types/auth";

const createSession = (profileStatus: CompanyProfileStatus): AuthSession => ({
  token: "token",
  roles: [],
  permissions: [],
  menus: [],
  defaultRoute: "/dashboard-government",
  profileStatus
});

describe("registration review route", () => {
  it("registers a real page for the review-status URL", () => {
    expect(registrationReviewRoute.path).toBe("/onboarding/review-status");
    expect(registrationReviewRoute.name).toBe("registration-review-status");
    expect(registrationReviewRoute.component).toBeTruthy();
  });

  it("routes pending users to review status while keeping editable statuses on company profile", () => {
    expect(resolveAuthRoute(createSession("PENDING_REVIEW"))).toBe("/onboarding/review-status");
    expect(resolveAuthRoute(createSession("PROFILE_REQUIRED"))).toBe("/onboarding/company-profile");
    expect(resolveAuthRoute(createSession("REJECTED"))).toBe("/onboarding/company-profile");
  });
});
