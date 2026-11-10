<script setup lang="ts">
import { RouterLink, useRoute } from "vue-router";

const route = useRoute();

const isActive = (path: string) => {
  if (path === "/") {
    return route.path === "/";
  }
  return route.path.startsWith(path);
};
</script>

<template>
  <header class="topbar">
    <RouterLink class="brand" to="/">
      <span class="brand-mark">
        <img src="@/assets/images/brand/fadaiting-logo-mark.png" alt="發呆亭露營區">
      </span>
      <span>
        <strong>發呆亭露營區</strong>
        <small>MOUNTAIN CAMP</small>
      </span>
    </RouterLink>

    <nav class="topnav">
      <div class="nav-group">
        <RouterLink :class="{ active: isActive('/') }" to="/">首頁</RouterLink>
        <RouterLink :to="{ path: '/', hash: '#about' }">關於我們</RouterLink>
      </div>
      <span class="nav-divider" aria-hidden="true"></span>
      <div class="nav-group">
        <RouterLink :class="{ active: isActive('/zones') }" to="/zones">營區介紹</RouterLink>
        <RouterLink :class="{ active: isActive('/map') }" to="/map">營區地圖</RouterLink>
        <RouterLink :class="{ active: isActive('/news') }" to="/news">最新消息</RouterLink>
        <RouterLink :class="{ active: isActive('/faq') }" to="/faq">FAQ</RouterLink>
        <RouterLink :class="{ active: isActive('/booking/lookup') }" to="/booking/lookup">查詢預約</RouterLink>
      </div>
    </nav>

    <RouterLink class="reserve-btn" to="/booking">
      <i class="bi bi-calendar3"></i>
      立即預訂
    </RouterLink>
  </header>
</template>

<style scoped>
.topbar {
  position: sticky;
  top: 12px;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 16px 28px;
  border: 1px solid rgba(106, 111, 85, 0.14);
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.84);
  backdrop-filter: blur(18px);
  box-shadow: 0 10px 28px rgba(50, 42, 29, 0.08);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  overflow: hidden;
  border-radius: 16px;
  background: #f4ecd6;
}

.brand-mark img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 1rem;
}

.brand small {
  color: #7d816d;
  font-size: 0.75rem;
  letter-spacing: 0.12em;
}

.topnav {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
  justify-content: center;
}

.nav-group {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
}

.nav-divider {
  width: 1px;
  height: 22px;
  background: rgba(111, 117, 64, 0.18);
}

.topnav a {
  position: relative;
  color: #2f362b;
  font-size: 0.96rem;
  text-decoration: none;
  transition: color 0.24s ease;
}

.topnav a::after {
  content: "";
  position: absolute;
  left: 0;
  bottom: -8px;
  width: 100%;
  height: 2px;
  border-radius: 999px;
  background: #6f7540;
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.24s ease;
}

.topnav a:hover,
.topnav a:focus-visible,
.topnav a.active {
  color: #5f6737;
}

.topnav a:hover::after,
.topnav a:focus-visible::after,
.topnav a.active::after {
  transform: scaleX(1);
}

.reserve-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 24px;
  border-radius: 999px;
  background: #6f7540;
  color: #fffdf7;
  text-decoration: none;
  font-weight: 600;
  transition:
    transform 0.26s ease,
    box-shadow 0.26s ease,
    background 0.26s ease;
}

.reserve-btn:hover,
.reserve-btn:focus-visible {
  background: #5f6737;
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(88, 96, 49, 0.22);
}

@media (max-width: 960px) {
  .topbar {
    flex-direction: column;
    align-items: stretch;
    padding: 18px;
  }

  .topnav {
    gap: 14px;
    justify-content: flex-start;
  }

  .nav-group {
    gap: 14px;
  }

  .nav-divider {
    width: 100%;
    height: 1px;
  }
}
</style>
