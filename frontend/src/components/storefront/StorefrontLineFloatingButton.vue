<script setup lang="ts">
const lineBotUrl = import.meta.env.VITE_LINE_BOT_URL || "https://line.me/R/ti/p/@395pihez";
</script>

<template>
  <a
    class="line-floating-button"
    :href="lineBotUrl"
    target="_blank"
    rel="noopener noreferrer"
    aria-label="加入發呆亭露營區 LINE 官方帳號"
    title="加入 LINE 官方帳號"
  >
    <span class="line-floating-button__mark">LINE</span>
  </a>
</template>

<style scoped>
.line-floating-button {
  position: fixed;
  right: max(18px, env(safe-area-inset-right));
  bottom: max(18px, env(safe-area-inset-bottom));
  z-index: 80;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  color: #ffffff;
  text-decoration: none;
  background: #06c755;
  border: 3px solid #ffffff;
  border-radius: 50%;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.2), 0 4px 10px rgba(6, 199, 85, 0.28);
  transition: transform 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.line-floating-button:hover,
.line-floating-button:focus-visible {
  color: #ffffff;
  background: #05b84f;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.24), 0 6px 14px rgba(6, 199, 85, 0.34);
  transform: translateY(-2px);
}

.line-floating-button:focus-visible {
  outline: 3px solid rgba(6, 199, 85, 0.28);
  outline-offset: 4px;
}

.line-floating-button__mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  height: 24px;
  font-size: 11px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: 0;
  color: #06c755;
  background: #ffffff;
  border-radius: 6px;
}

@media (max-width: 640px) {
  .line-floating-button {
    right: max(14px, env(safe-area-inset-right));
    bottom: max(14px, env(safe-area-inset-bottom));
    width: 56px;
    height: 56px;
  }

  .line-floating-button__mark {
    min-width: 38px;
    height: 22px;
    font-size: 10px;
  }
}
</style>
