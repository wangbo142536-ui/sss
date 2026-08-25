<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from "vue";

import "../styles/enterprise-entry.css";

type ProviderKind = "business" | "data";

interface ProviderItem {
  key: string;
  title: string;
  summary: string;
  action?: string;
  label?: string;
}

const activeTopic = ref<ProviderKind>("business");
const activeBusinessProvider = ref(0);
const isBusinessRotationManuallyPaused = ref(false);
let topicObserver: IntersectionObserver | undefined;
let businessProviderTimer: number | undefined;

const businessProviders: ProviderItem[] = [
  {
    key: "ship-supply",
    title: "船供服务商",
    summary: "为船舶靠港与航行提供稳定的物料和伙食供应。",
    label: "SHIP SUPPLY"
  },
  {
    key: "crew",
    title: "船员服务商",
    summary: "围绕船员在船、靠港及职业周期提供专业保障。",
    label: "CREW SUPPORT"
  },
  {
    key: "finance",
    title: "金融服务商",
    summary: "为航运经营、采购交易和履约环节提供金融支持。",
    label: "MARITIME FINANCE"
  }
];

const dataProviders: ProviderItem[] = [
  { key: "maritime", title: "海事", summary: "船舶登记、监管及海事服务数据。", action: "对接海事数据" },
  { key: "port", title: "港航", summary: "港口作业、航道与运输协同数据。", action: "对接港航数据" },
  { key: "customs", title: "海关", summary: "通关、查验及进出口业务数据。", action: "对接海关数据" },
  { key: "border", title: "边检", summary: "船舶及人员出入境检查数据。", action: "对接边检数据" },
  { key: "weather", title: "气象", summary: "海洋气象、预警及港区环境数据。", action: "对接气象数据" },
  { key: "vessel-dynamics", title: "船舶动态", summary: "船位、航速、航向及轨迹数据。", action: "对接船舶动态数据" }
];

const dataProcess = [
  { title: "数据清单确认", text: "明确字段范围、更新频率、数据格式、使用范围和质量要求。" },
  { title: "签订数据接口协议", text: "确认数据授权、接口标准、使用规则、双方责任和服务保障。" },
  { title: "安全审核", text: "审核数据来源、敏感字段、传输方式、访问权限和存储机制。" },
  { title: "联调测试", text: "完成接口连通、字段映射、异常处理和数据质量测试。" },
  { title: "验收上线", text: "验证接口稳定性、数据完整性和安全要求后接入正式环境。" }
];

function registrationLink(providerType: ProviderKind, category: string) {
  return {
    path: "/register",
    query: {
      providerType,
      category,
      ...(providerType === "data" ? { flow: "data-connection" } : {})
    }
  };
}

function selectBusinessProvider(index: number) {
  activeBusinessProvider.value = (index + businessProviders.length) % businessProviders.length;
}

function stopBusinessProviderRotation() {
  if (businessProviderTimer !== undefined) {
    window.clearInterval(businessProviderTimer);
    businessProviderTimer = undefined;
  }
}

function startBusinessProviderRotation() {
  stopBusinessProviderRotation();
  if (isBusinessRotationManuallyPaused.value || window.matchMedia("(prefers-reduced-motion: reduce)").matches || document.hidden) return;
  businessProviderTimer = window.setInterval(() => selectBusinessProvider(activeBusinessProvider.value + 1), 4200);
}

function toggleBusinessProviderRotation() {
  isBusinessRotationManuallyPaused.value = !isBusinessRotationManuallyPaused.value;
  if (isBusinessRotationManuallyPaused.value) stopBusinessProviderRotation();
  else startBusinessProviderRotation();
}

function handleVisibilityChange() {
  if (document.hidden) stopBusinessProviderRotation();
  else startBusinessProviderRotation();
}

onMounted(() => {
  const sections = document.querySelectorAll<HTMLElement>("[data-entry-topic]");
  topicObserver = new IntersectionObserver(
    (entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
      const topic = (visible?.target as HTMLElement | undefined)?.dataset.entryTopic;
      if (topic) {
        activeTopic.value = topic as ProviderKind;
      }
    },
    { rootMargin: "-24% 0px -52%", threshold: [0.08, 0.3, 0.6] }
  );
  sections.forEach((section) => topicObserver?.observe(section));
  document.addEventListener("visibilitychange", handleVisibilityChange);
  startBusinessProviderRotation();
});

onBeforeUnmount(() => {
  topicObserver?.disconnect();
  document.removeEventListener("visibilitychange", handleVisibilityChange);
  stopBusinessProviderRotation();
});
</script>

<template>
  <div class="enterprise-entry-page">
    <header class="site-header">
      <div class="site-topbar">
        <div class="entry-shell site-topbar__inner">
          <div class="site-topbar__brand">
            <img class="topbar-brand-logo" src="/prototypes/assets/zhoushan-maritime-ecosystem-logo-dark-bg.png" alt="" aria-hidden="true">
            <span>海事服务生态 · 保障最后一公里</span>
          </div>
          <nav aria-label="辅助导航">
            <a href="/prototypes/maritime_integrated_service_home.html#policy">政策发放</a>
            <a href="/prototypes/maritime_integrated_service_home.html#publish">最新动态</a>
            <a href="/prototypes/maritime_about.html">关于我们</a>
            <a href="/prototypes/maritime_plan.html">平台规划</a>
          </nav>
        </div>
      </div>
    </header>

    <main id="entry-top">
      <section class="entry-hero" aria-labelledby="entry-title">
        <img
          class="entry-hero__media"
          src="/prototypes/assets/maritime-enterprise-entry-cover.png"
          alt="智慧港口内，船舶、供应车辆与海事服务人员协同作业"
        >
        <div class="entry-hero__wash" aria-hidden="true"></div>
        <div class="entry-hero__network" aria-hidden="true"><i></i><i></i><i></i><i></i><i></i></div>
        <div class="entry-shell entry-hero__content">
          <div class="entry-hero__copy">
            <p class="entry-hero__label">ENTERPRISE ONBOARDING</p>
            <h1 id="entry-title">企业入驻</h1>
            <p class="entry-hero__lead">连接海事服务与数据资源，共建可信、开放、协同的海事服务生态。</p>
            <p class="entry-hero__description">平台面向具备专业服务能力的企业及权威数据机构，提供服务商注册、资质核验、服务类目配置与数据接口对接入口。</p>
            <div class="entry-hero__actions" aria-label="选择入驻类型">
              <a class="entry-button entry-button--primary" href="#business-provider">了解经营服务商 <span>→</span></a>
              <a class="entry-button entry-button--dark" href="#data-provider">了解数据服务商 <span>→</span></a>
            </div>
          </div>
          <div class="entry-hero__signal" aria-hidden="true">
            <span class="signal-core"></span>
            <span class="signal-ring signal-ring--one"></span>
            <span class="signal-ring signal-ring--two"></span>
            <span class="signal-caption">SERVICE × DATA</span>
          </div>
        </div>
      </section>

      <nav class="entry-topic-nav" :data-active-topic="activeTopic" aria-label="企业入驻专题导航">
        <a :class="{ 'is-active': activeTopic === 'business' }" href="#business-provider"><span>01</span>经营服务商</a>
        <a :class="{ 'is-active': activeTopic === 'data' }" href="#data-provider"><span>02</span>数据服务商</a>
      </nav>

      <section id="business-provider" class="business-section" data-entry-topic="business" aria-labelledby="business-title">
        <div class="entry-shell">
          <div class="section-heading section-heading--business">
            <div>
              <h2 id="business-title">经营服务商</h2>
            </div>
            <div class="section-introduction">
              <h3>什么是经营服务商</h3>
              <p>经营服务商是为船舶、船员及航运企业提供专业服务的企业。平台通过企业资质核验、服务类目配置和服务能力审核，将合规服务资源接入海事服务体系。</p>
            </div>
          </div>

          <div
            class="business-journey"
            aria-label="经营服务商类型轮询展示"
            @mouseenter="stopBusinessProviderRotation"
            @mouseleave="startBusinessProviderRotation"
            @focusin="stopBusinessProviderRotation"
            @focusout="startBusinessProviderRotation"
          >
            <div class="business-journey__ambient" aria-hidden="true"><i></i><i></i><i></i></div>
            <div class="business-journey__cards">
              <article
                v-for="(provider, index) in businessProviders"
                :key="provider.key"
                class="business-station"
                :class="{ 'is-active': activeBusinessProvider === index }"
                :aria-current="activeBusinessProvider === index ? 'true' : undefined"
                tabindex="0"
                @mouseenter="selectBusinessProvider(index)"
                @focus="selectBusinessProvider(index)"
              >
                <div class="business-station__topline"><span>0{{ index + 1 }}</span><small>{{ provider.label }}</small></div>
                <div class="business-station__node" aria-hidden="true"><span></span><i></i></div>
                <div class="business-station__copy"><h3>{{ provider.title }}</h3><p>{{ provider.summary }}</p></div>
                <div class="business-station__status"><i></i><span>服务能力接入</span></div>
              </article>
            </div>
            <div class="business-journey__controls" aria-label="切换经营服务商">
              <button type="button" aria-label="上一个服务商" @click="selectBusinessProvider(activeBusinessProvider - 1)">←</button>
              <div class="business-journey__dots">
                <button
                  v-for="(provider, index) in businessProviders"
                  :key="provider.key"
                  type="button"
                  :class="{ 'is-active': activeBusinessProvider === index }"
                  :aria-label="`展示${provider.title}`"
                  :aria-pressed="activeBusinessProvider === index"
                  @click="selectBusinessProvider(index)"
                ><span></span></button>
              </div>
              <button type="button" aria-label="下一个服务商" @click="selectBusinessProvider(activeBusinessProvider + 1)">→</button>
              <button
                class="business-journey__pause"
                type="button"
                :aria-label="isBusinessRotationManuallyPaused ? '继续经营服务商轮询' : '暂停经营服务商轮询'"
                :aria-pressed="isBusinessRotationManuallyPaused"
                @click="toggleBusinessProviderRotation"
              >{{ isBusinessRotationManuallyPaused ? '▶' : 'Ⅱ' }}</button>
            </div>
          </div>

          <div class="qualification-band">
            <div class="qualification-band__title">
              <span>注册前准备</span>
              <h3>资质与资料</h3>
              <p>具体材料会根据服务类型进一步核验，先准备以下四类基础内容。</p>
            </div>
            <ol class="qualification-list">
              <li><span>01</span><div><strong>企业资质</strong><p>营业执照、法定代表人、联系人，以及对应行业资质或许可证。</p></div></li>
              <li><span>02</span><div><strong>服务能力</strong><p>服务区域、覆盖港口、团队配置、履约保障和应急响应能力。</p></div></li>
              <li><span>03</span><div><strong>SKU类目</strong><p>选择服务类目，维护商品、服务项目、规格和供应范围。</p></div></li>
              <li><span>04</span><div><strong>资料核验</strong><p>平台核验企业信息、行业资质、服务范围与类目内容。</p></div></li>
            </ol>
            <RouterLink class="entry-button entry-button--primary qualification-band__action" :to="registrationLink('business', 'all')">注册服务商 <span>→</span></RouterLink>
          </div>
        </div>
      </section>

      <section id="data-provider" class="data-section" data-entry-topic="data" aria-labelledby="data-title">
        <img class="data-section__media" src="/prototypes/assets/home-port-command.png" alt="数字航线连接港口、船舶与海事数据节点">
        <div class="data-section__overlay" aria-hidden="true"></div>
        <div class="entry-shell data-section__content">
          <div class="section-heading section-heading--data">
            <div>
              <p class="section-kicker">接入权威数据资源</p>
              <h2 id="data-title">数据服务商</h2>
            </div>
            <div class="section-introduction">
              <h3>什么是数据服务商</h3>
              <p>数据服务商是向平台提供权威海事、港航及船舶相关数据的政府部门、行业机构或专业数据企业。平台通过标准化接口接入数据，用于船舶服务、航运分析、风险识别和海事生态协同。</p>
            </div>
          </div>

          <div class="data-network" aria-label="可对接的数据类型">
            <svg class="data-network__routes" viewBox="0 0 1280 720" preserveAspectRatio="none" aria-hidden="true">
              <defs>
                <linearGradient id="data-route-gradient" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0" stop-color="#14b8d6" stop-opacity="0"></stop>
                  <stop offset=".44" stop-color="#2bc9e4" stop-opacity=".5"></stop>
                  <stop offset="1" stop-color="#8cecff" stop-opacity=".9"></stop>
                </linearGradient>
              </defs>
              <g class="data-route-lines">
                <path id="data-route-1" d="M218 116 C398 116 432 246 640 350"></path>
                <path id="data-route-2" d="M1062 116 C882 116 848 246 640 350"></path>
                <path id="data-route-3" d="M1162 330 C962 330 878 350 640 350"></path>
                <path id="data-route-4" d="M1018 610 C846 590 814 452 640 350"></path>
                <path id="data-route-5" d="M262 610 C434 590 466 452 640 350"></path>
                <path id="data-route-6" d="M118 330 C318 330 402 350 640 350"></path>
              </g>
              <g class="data-route-packets">
                <circle r="4"><animateMotion dur="4.6s" begin="-1.2s" repeatCount="indefinite"><mpath href="#data-route-1"></mpath></animateMotion></circle>
                <circle r="3"><animateMotion dur="5.1s" begin="-3.4s" repeatCount="indefinite"><mpath href="#data-route-2"></mpath></animateMotion></circle>
                <circle r="4"><animateMotion dur="4.2s" begin="-2.2s" repeatCount="indefinite"><mpath href="#data-route-3"></mpath></animateMotion></circle>
                <circle r="3"><animateMotion dur="5.4s" begin="-4.1s" repeatCount="indefinite"><mpath href="#data-route-4"></mpath></animateMotion></circle>
                <circle r="4"><animateMotion dur="4.9s" begin="-.6s" repeatCount="indefinite"><mpath href="#data-route-5"></mpath></animateMotion></circle>
                <circle r="3"><animateMotion dur="4.4s" begin="-2.9s" repeatCount="indefinite"><mpath href="#data-route-6"></mpath></animateMotion></circle>
              </g>
            </svg>
            <div class="data-network__rings" aria-hidden="true"><i></i><i></i><i></i></div>
            <div class="data-network__hub">
              <span>MARITIME DATA CORE</span>
              <strong>海事数据枢纽</strong>
              <small><i>标准接口</i><i>安全传输</i><i>持续更新</i></small>
            </div>
            <article v-for="(provider, index) in dataProviders" :key="provider.key" class="data-node" :style="{ '--node-index': index }">
              <span class="data-node__index">0{{ index + 1 }}</span>
              <div><h3>{{ provider.title }}</h3><p>{{ provider.summary }}</p></div>
              <RouterLink :to="registrationLink('data', provider.key)">{{ provider.action }} <span>→</span></RouterLink>
            </article>
          </div>

          <div class="connection-process">
            <div class="connection-process__heading">
              <span>DATA CONNECTION</span>
              <h3>如何完成数据对接</h3>
              <p>从数据范围确认到正式上线，每一步都保留明确的责任边界和验收依据。</p>
            </div>
            <RouterLink class="entry-button entry-button--light connection-process__action" :to="registrationLink('data', 'all')">开始数据对接 <span>→</span></RouterLink>
            <ol class="process-list">
              <li v-for="(step, index) in dataProcess" :key="step.title">
                <span>{{ String(index + 1).padStart(2, "0") }}</span>
                <strong>{{ step.title }}</strong>
                <p>{{ step.text }}</p>
              </li>
            </ol>
          </div>
        </div>
      </section>
    </main>

    <footer class="entry-footer">
      <div class="entry-shell entry-footer__inner">
        <div>
          <div class="entry-footer__brand">
            <img class="entry-footer__brand-logo" src="/prototypes/assets/zhoushan-maritime-ecosystem-logo-dark-bg.png" alt="" aria-hidden="true">
            <strong>海事一站式综合服务平台</strong>
          </div>
          <p>连接服务能力与数据资源，服务航运产业协同。</p>
        </div>
        <nav aria-label="页脚导航"><a href="/">返回官网</a><a href="/prototypes/maritime_about.html">关于我们</a><a href="/prototypes/maritime_plan.html">平台规划</a><RouterLink to="/login">登录平台</RouterLink></nav>
      </div>
    </footer>
  </div>
</template>
