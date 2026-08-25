import { flushPromises, mount } from "@vue/test-utils";
import { beforeEach, describe, expect, it, vi } from "vitest";
import WorkbenchLayout from "@/components/WorkbenchLayout.vue";
import type { AuthSession } from "@/types/auth";
import type { WorkbenchMenuItem } from "@/types/workbench";

const mocks = vi.hoisted(() => ({
  getAuthMenus: vi.fn(),
  getAuthSession: vi.fn()
}));

vi.mock("@/services/permissionService", () => ({
  getAuthMenus: mocks.getAuthMenus
}));

vi.mock("@/services/authService", () => ({
  getAuthSession: mocks.getAuthSession
}));

vi.mock("vue-router", () => ({
  useRoute: () => ({ path: "/suppliers" }),
  useRouter: () => ({ push: vi.fn() })
}));

const createSession = (overrides: Partial<AuthSession>): AuthSession => ({
  token: "test-token",
  roles: [],
  permissions: [],
  menus: [],
  defaultRoute: "/dashboard-government",
  profileStatus: "ACTIVE",
  ...overrides
});

const sidebarStub = {
  name: "CollapsibleSidebar",
  props: ["items", "role", "expanded"],
  template: '<aside data-test="sidebar" :data-role="role" />'
};

function mountLayout() {
  return mount(WorkbenchLayout, {
    global: {
      stubs: {
        CollapsibleSidebar: sidebarStub,
        IconButton: true,
        LanguageSwitch: true,
        RouterLink: { template: "<a><slot /></a>" }
      }
    }
  });
}

describe("WorkbenchLayout authenticated role", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.getAuthMenus.mockResolvedValue([]);
  });

  it("passes the supplier company type to the sidebar for a registered company administrator", async () => {
    mocks.getAuthSession.mockReturnValue(createSession({
      company: { companyType: "SUPPLIER" },
      roles: ["COMPANY_ADMIN_35"]
    }));

    const wrapper = mountLayout();
    await flushPromises();

    expect(wrapper.get('[data-test="sidebar"]').attributes("data-role")).toBe("supplier");
  });

  it("keeps platform administrators on the admin sidebar role", async () => {
    mocks.getAuthSession.mockReturnValue(createSession({ roles: ["PLATFORM_ADMIN"] }));

    const wrapper = mountLayout();
    await flushPromises();

    expect(wrapper.get('[data-test="sidebar"]').attributes("data-role")).toBe("admin");
  });

  it("passes through only the menu tree returned for an ordinary authenticated user", async () => {
    const authorizedMenus: WorkbenchMenuItem[] = [{
      key: "companyMembers",
      labelKey: "nav.companyMembers",
      route: "/company/members",
      icon: "CM",
      roles: ["supplier"]
    }];
    mocks.getAuthSession.mockReturnValue(createSession({
      company: { companyType: "SUPPLIER" },
      roles: ["PRODUCT_QA"]
    }));
    mocks.getAuthMenus.mockResolvedValue(authorizedMenus);

    const wrapper = mountLayout();
    await flushPromises();

    expect(wrapper.getComponent({ name: "CollapsibleSidebar" }).props("items")).toEqual(authorizedMenus);
  });

  it("does not fall back to an admin role when the authenticated identity is unknown", async () => {
    mocks.getAuthSession.mockReturnValue(createSession({ roles: ["UNRECOGNIZED_ROLE"] }));

    const wrapper = mountLayout();
    await flushPromises();

    expect(wrapper.getComponent({ name: "CollapsibleSidebar" }).props("role")).toBeNull();
  });
});
