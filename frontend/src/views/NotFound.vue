<template>
  <main class="not-found-page">
    <section class="not-found-panel">
      <span>404</span>
      <h1>找不到這個頁面</h1>
      <p>這個連結可能已經移動，或是網址輸入有誤。你可以回到首頁，或直接查詢既有預約。</p>
      <div class="not-found-actions">
        <router-link class="primary-action" to="/">返回首頁</router-link>
        <router-link class="secondary-action" to="/booking">前往預約</router-link>
        <router-link class="secondary-action" to="/booking/lookup">查詢預約</router-link>
      </div>
    </section>
  </main>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  background: #f5f1e8;
  color: #202821;
}

.not-found-panel {
  width: min(720px, 100%);
  padding: 42px;
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(111, 117, 64, 0.14);
  box-shadow: 0 22px 60px rgba(70, 59, 39, 0.12);
}

.not-found-panel span {
  display: block;
  margin-bottom: 12px;
  color: #8b8f77;
  font-size: 0.8rem;
  letter-spacing: 0.18em;
}

.not-found-panel h1 {
  margin: 0 0 14px;
  font-size: clamp(2.3rem, 6vw, 4.8rem);
  line-height: 1.05;
}

.not-found-panel p {
  max-width: 560px;
  margin: 0;
  color: #66705e;
  line-height: 1.85;
}

.not-found-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 28px;
}

.primary-action,
.secondary-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 22px;
  border-radius: 999px;
  font-weight: 700;
  text-decoration: none;
}

.primary-action {
  background: #6f7540;
  color: #fffdf7;
}

.secondary-action {
  border: 1px solid rgba(111, 117, 64, 0.24);
  color: #4f583f;
}

@media (max-width: 640px) {
  .not-found-page {
    padding: 18px;
  }

  .not-found-panel {
    padding: 30px 22px;
    border-radius: 22px;
  }
}
</style>
