const planSections = Array.from(document.querySelectorAll("[data-plan-section]"));
const planLinks = Array.from(document.querySelectorAll("[data-plan-link]"));
const planNav = document.querySelector("[data-plan-nav]");
const backToTop = document.querySelector("[data-back-to-top]");

function setActivePlan(sectionId) {
  planLinks.forEach((link) => {
    const active = link.dataset.planLink === sectionId;
    link.classList.toggle("is-active", active);
    if (active) link.setAttribute("aria-current", "true");
    else link.removeAttribute("aria-current");
  });

  if (planNav) planNav.dataset.activeSection = sectionId;
}

if ("IntersectionObserver" in window) {
  const sectionObserver = new IntersectionObserver((entries) => {
    const current = entries
      .filter((entry) => entry.isIntersecting)
      .sort((first, second) => second.intersectionRatio - first.intersectionRatio)[0];

    if (current?.target.id) setActivePlan(current.target.id);
  }, {
    rootMargin: "-20% 0px -52% 0px",
    threshold: [0.04, 0.18, 0.4, 0.68]
  });

  planSections.forEach((section) => sectionObserver.observe(section));
}

planLinks.forEach((link) => {
  link.addEventListener("click", () => {
    if (link.dataset.planLink) setActivePlan(link.dataset.planLink);
  });
});

function updateBackToTop() {
  backToTop?.classList.toggle("is-visible", window.scrollY > 560);
}

backToTop?.addEventListener("click", () => {
  window.scrollTo({ top: 0, behavior: "smooth" });
});

window.addEventListener("scroll", updateBackToTop, { passive: true });
updateBackToTop();

window.addEventListener("load", () => {
  const sectionId = decodeURIComponent(window.location.hash.slice(1));
  const section = document.getElementById(sectionId);
  if (!section?.matches("[data-plan-section]")) return;

  window.requestAnimationFrame(() => {
    section.scrollIntoView({ block: "start", behavior: "auto" });
  });
}, { once: true });
