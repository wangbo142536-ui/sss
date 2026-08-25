import { flushPromises, mount } from "@vue/test-utils";
import { createMemoryHistory, createRouter } from "vue-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import CompanyOnboardingView from "@/views/CompanyOnboardingView.vue";
import RegistrationReviewStatusView from "@/views/RegistrationReviewStatusView.vue";

const authMocks = vi.hoisted(() => ({
  getCompanyProfile: vi.fn(),
  getRegisterConfiguration: vi.fn(),
  registerAndSubmit: vi.fn(),
  resolveAuthRoute: vi.fn(),
  saveAuthSession: vi.fn()
}));

vi.mock("@/services/authService", () => ({
  clearAuthSession: vi.fn(),
  getCompanyProfile: authMocks.getCompanyProfile,
  getRegisterConfiguration: authMocks.getRegisterConfiguration,
  getSafeRequestErrorKey: vi.fn((_error: unknown, fallbackKey: string) => fallbackKey),
  isAuthExpiredError: vi.fn(() => false),
  registerAndSubmit: authMocks.registerAndSubmit,
  resolveAuthRoute: authMocks.resolveAuthRoute,
  saveAuthSession: authMocks.saveAuthSession,
  submitCompanyProfile: vi.fn(),
  uploadQualificationFile: vi.fn()
}));

const pendingSession = {
  token: "registration-token",
  roles: [],
  permissions: [],
  menus: [],
  defaultRoute: "/dashboard-government",
  profileStatus: "PENDING_REVIEW" as const
};

const pendingProfile = {
  companyType: "SHIP_AGENT",
  companyName: "测试船舶代理有限公司",
  unifiedSocialCreditCode: "91310000TEST000001",
  contactName: "张三",
  contactPhone: "13800000000",
  contactEmail: "contact@example.com",
  qualificationFiles: [{ fileId: 1, name: "营业执照.pdf" }],
  supplierServiceTypes: [],
  status: "PENDING_REVIEW" as const
};

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: "/register", component: { template: "<div />" } },
      { path: "/onboarding/company-profile", component: { template: "<div />" } },
      { path: "/onboarding/review-status", component: RegistrationReviewStatusView }
    ]
  });
}

describe("company onboarding review status", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    authMocks.getRegisterConfiguration.mockResolvedValue({
      companyTypes: [
        { value: "SHIP_AGENT", label: "船舶代理" },
        { value: "SUPPLIER", label: "供应服务商" },
        { value: "BARGE_AGENT", label: "驳船服务商" }
      ],
      supplierServiceTypes: []
    });
    authMocks.registerAndSubmit.mockResolvedValue(pendingSession);
    authMocks.resolveAuthRoute.mockReturnValue("/onboarding/review-status");
    authMocks.getCompanyProfile.mockResolvedValue(pendingProfile);
  });

  it("routes a completed registration to the canonical pending-review page", async () => {
    const router = createTestRouter();
    await router.push("/register");
    await router.isReady();
    const wrapper = mount(CompanyOnboardingView, {
      props: { registrationMode: true },
      global: { plugins: [router] }
    });
    await flushPromises();

    await wrapper.get('input[autocomplete="organization"]').setValue(pendingProfile.companyName);
    await wrapper.findAll(".form-grid")[0].findAll("input")[1].setValue(pendingProfile.unifiedSocialCreditCode);
    await wrapper.get('input[autocomplete="name"]').setValue(pendingProfile.contactName);
    await wrapper.get('input[autocomplete="tel"]').setValue(pendingProfile.contactPhone);
    await wrapper.get('input[autocomplete="email"]').setValue(pendingProfile.contactEmail);
    await wrapper.get('input[autocomplete="username"]').setValue("review-user");
    const passwordInputs = wrapper.findAll('input[type="password"]');
    await passwordInputs[0].setValue("Review123!");
    await passwordInputs[1].setValue("Review123!");
    const fileInput = wrapper.get<HTMLInputElement>('input[type="file"]');
    Object.defineProperty(fileInput.element, "files", {
      configurable: true,
      value: [new File(["license"], "营业执照.pdf", { type: "application/pdf" })]
    });
    await fileInput.trigger("change");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(authMocks.saveAuthSession).toHaveBeenCalledWith(pendingSession);
    expect(authMocks.resolveAuthRoute).toHaveBeenCalledWith(pendingSession);
    expect(router.currentRoute.value.path).toBe("/onboarding/review-status");
  });

  it("rejects an obvious file path used as the contact name before submission", async () => {
    const router = createTestRouter();
    await router.push("/register");
    await router.isReady();
    const wrapper = mount(CompanyOnboardingView, {
      props: { registrationMode: true },
      global: { plugins: [router] }
    });
    await flushPromises();

    await wrapper.get('input[autocomplete="organization"]').setValue(pendingProfile.companyName);
    await wrapper.findAll(".form-grid")[0].findAll("input")[1].setValue(pendingProfile.unifiedSocialCreditCode);
    await wrapper.get('input[autocomplete="name"]').setValue("C:\\fakepath\\license.pdf");
    await wrapper.get('input[autocomplete="tel"]').setValue(pendingProfile.contactPhone);
    await wrapper.get('input[autocomplete="email"]').setValue(pendingProfile.contactEmail);
    await wrapper.get('input[autocomplete="username"]').setValue("review-user");
    const passwordInputs = wrapper.findAll('input[type="password"]');
    await passwordInputs[0].setValue("Review123!");
    await passwordInputs[1].setValue("Review123!");
    const fileInput = wrapper.get<HTMLInputElement>('input[type="file"]');
    Object.defineProperty(fileInput.element, "files", {
      configurable: true,
      value: [new File(["license"], "营业执照.pdf", { type: "application/pdf" })]
    });
    await fileInput.trigger("change");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(wrapper.get('input[autocomplete="name"]').attributes("aria-invalid")).toBe("true");
    expect(wrapper.text()).toContain("联系人不能是文件路径");
    expect(authMocks.registerAndSubmit).not.toHaveBeenCalled();
  });

  it("renders the pending-review completion message when opened directly", async () => {
    const router = createTestRouter();
    await router.push("/onboarding/review-status");
    await router.isReady();
    const wrapper = mount(RegistrationReviewStatusView, { global: { plugins: [router] } });
    await flushPromises();

    const status = wrapper.get(".onboarding-review-complete");
    expect(status.text()).toContain("已注册成功");
    expect(status.text()).toContain("平台审核中");
    expect(status.text()).toContain("一个工作日完成审核，请注意邮箱/短信消息提醒。");
    expect(wrapper.find("form").exists()).toBe(false);
  });
});
