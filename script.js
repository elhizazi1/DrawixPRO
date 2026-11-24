document.addEventListener("DOMContentLoaded", () => {
  const root = document.documentElement;
  const toggle = document.getElementById("theme-toggle");
  const system = matchMedia("(prefers-color-scheme: dark)");
  const reduced = matchMedia("(prefers-reduced-motion: reduce)");
  let choice = null;
  try { choice = localStorage.getItem("theme"); } catch (_) {}
  function applyTheme() {
    const theme = choice === "light" || choice === "dark" ? choice : system.matches ? "dark" : "light";
    root.dataset.theme = theme;
    if (toggle) {
      toggle.textContent = theme === "dark" ? "☀" : "☾";
      toggle.setAttribute("aria-label", theme === "dark" ? "الوضع الفاتح" : "الوضع الداكن");
    }
  }
  applyTheme();
  system.addEventListener("change", applyTheme);
  toggle?.addEventListener("click", () => {
    choice = root.dataset.theme === "dark" ? "light" : "dark";
    try { localStorage.setItem("theme", choice); } catch (_) {}
    applyTheme();
  });
  const reveals = document.querySelectorAll(".reveal");
  if (reduced.matches || !("IntersectionObserver" in window)) reveals.forEach(el => el.classList.add("active"));
  else {
    const observer = new IntersectionObserver(entries => entries.forEach(entry => {
      if (entry.isIntersecting) { entry.target.classList.add("active"); observer.unobserve(entry.target); }
    }), {threshold: 0.15});
    reveals.forEach(el => observer.observe(el));
  }
  const slides = [...document.querySelectorAll(".slide")];
  let slide = 0, timer;
  slides.forEach((el, index) => el.classList.toggle("active", index === 0));
  function schedule() {
    clearInterval(timer);
    if (slides.length < 2 || document.hidden || reduced.matches) return;
    timer = setInterval(() => {
      slides[slide].classList.remove("active");
      slide = (slide + 1) % slides.length;
      slides[slide].classList.add("active");
    }, 3500);
  }
  document.addEventListener("visibilitychange", schedule);
  reduced.addEventListener("change", schedule);
  schedule();
  document.getElementById("contact-form")?.addEventListener("submit", event => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    const data = new FormData(form);
    const body = `Name: ${data.get("name")}\nEmail: ${data.get("email")}\n\n${data.get("message")}`;
    location.href = `mailto:jamal@elhizazi.me?subject=${encodeURIComponent("Drawix Pro enquiry")}&body=${encodeURIComponent(body)}`;
  });
});
