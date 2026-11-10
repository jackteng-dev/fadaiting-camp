<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { getCampSiteDetailService } from "@/api/camp";

type Faq = {
  id: number;
  question: string;
  answer: string;
};

type Facility = {
  id: number;
  name: string;
};

const loading = ref(true);
const error = ref("");
const faqs = ref<Faq[]>([]);
const facilities = ref<Facility[]>([]);
const openFaqId = ref<number | null>(null);

const faqGroups = computed(() => {
  const list = faqs.value;
  return [
    { title: "預約與入住", items: list.slice(0, 3) },
    { title: "營位與設備", items: list.slice(3, 6) },
    { title: "其他說明", items: list.slice(6) },
  ].filter((group) => group.items.length > 0);
});

const toggleFaq = (faqId: number) => {
  openFaqId.value = openFaqId.value === faqId ? null : faqId;
};

const loadFaqs = async () => {
  loading.value = true;
  error.value = "";
  try {
    const res = await getCampSiteDetailService();
    faqs.value = res.data?.faqs ?? [];
    facilities.value = res.data?.facilities ?? [];
  } catch (err: any) {
    error.value = err?.message || "FAQ 資料暫時載入失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

onMounted(loadFaqs);
</script>

<template>
  <div class="faq-page">
    <section class="faq-hero">
      <div>
        <span class="section-kicker">FAQ</span>
        <h1>常見問題</h1>
        <p>把大家最常問的預約、入住、營位、設備與周邊問題整理在一起，讓你在出發前先把細節看清楚。</p>
      </div>
      <div class="hero-side">
        <strong>常見關鍵字</strong>
        <div class="facility-pills">
          <span v-for="facility in facilities.slice(0, 6)" :key="facility.id">{{ facility.name }}</span>
        </div>
      </div>
    </section>

    <section v-if="loading" class="status-card">資料載入中...</section>
    <section v-else-if="error" class="status-card error">{{ error }}</section>

    <section v-else class="faq-groups">
      <article v-for="group in faqGroups" :key="group.title" class="faq-group">
        <div class="faq-group-head">
          <span class="section-kicker">{{ group.title }}</span>
          <h2>{{ group.title }}</h2>
        </div>
        <div class="faq-list">
          <article
            v-for="faq in group.items"
            :key="faq.id"
            class="faq-item"
            :class="{ open: openFaqId === faq.id }"
            @click="toggleFaq(faq.id)"
          >
            <div class="faq-head">
              <h3>{{ faq.question }}</h3>
              <i class="bi" :class="openFaqId === faq.id ? 'bi-dash-lg' : 'bi-plus-lg'"></i>
            </div>
            <p v-show="openFaqId === faq.id">{{ faq.answer }}</p>
          </article>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.faq-page {
  width: min(1520px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  gap: 24px;
  padding-bottom: 28px;
}

.faq-hero,
.faq-group,
.status-card {
  border-radius: 32px;
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(120, 111, 92, 0.1);
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.faq-hero {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 24px;
  align-items: end;
  padding: 32px;
  background:
    linear-gradient(90deg, rgba(244, 239, 231, 0.96) 0%, rgba(244, 239, 231, 0.8) 42%, rgba(42, 53, 38, 0.12) 100%),
    url("https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=1800&q=80")
      center / cover no-repeat;
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

.faq-hero h1,
.faq-group-head h2 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.faq-hero h1 {
  font-size: clamp(2.8rem, 4.4vw, 4.2rem);
}

.faq-hero p,
.faq-item p {
  color: #61685d;
  line-height: 1.8;
}

.hero-side {
  padding: 22px;
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.8);
  border: 1px solid rgba(120, 111, 92, 0.12);
}

.hero-side strong {
  display: block;
  margin-bottom: 12px;
  color: #293025;
}

.facility-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.facility-pills span {
  padding: 8px 12px;
  border-radius: 999px;
  background: #f1ede2;
  color: #596049;
  font-size: 0.86rem;
}

.status-card {
  padding: 20px 24px;
}

.status-card.error {
  color: #a44f39;
}

.faq-groups {
  display: grid;
  gap: 18px;
}

.faq-group {
  padding: 24px;
}

.faq-group-head {
  margin-bottom: 14px;
}

.faq-list {
  display: grid;
  gap: 12px;
}

.faq-item {
  padding: 18px 20px;
  border-radius: 22px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
  cursor: pointer;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease,
    background 0.24s ease;
}

.faq-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 28px rgba(61, 52, 34, 0.1);
}

.faq-item.open {
  background: #f6f1e6;
  border-color: rgba(111, 117, 64, 0.22);
}

.faq-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.faq-head h3 {
  margin: 0;
  font-size: 1.08rem;
}

.faq-head i {
  color: #6f7540;
  transition: transform 0.24s ease;
}

.faq-item p {
  margin: 0;
  max-height: 0;
  overflow: hidden;
  transition:
    max-height 0.28s ease,
    margin-top 0.28s ease;
}

.faq-item.open p {
  max-height: 220px;
  margin-top: 12px;
}

.faq-item.open .faq-head i {
  transform: rotate(180deg);
}

@media (max-width: 1024px) {
  .faq-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .faq-page {
    width: min(100%, calc(100% - 24px));
  }
}
</style>
