<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { getCampAnnouncementsService } from "@/api/camp";

type Announcement = {
  id: number;
  title: string;
  content: string;
};

const newsImages = [
  "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1501555088652-021faa106b9b?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&w=1200&q=80",
];

const loading = ref(true);
const error = ref("");
const announcements = ref<Announcement[]>([]);

const featuredAnnouncement = computed(() => announcements.value[0] || null);
const secondaryAnnouncements = computed(() => announcements.value.slice(1));

const loadAnnouncements = async () => {
  loading.value = true;
  error.value = "";
  try {
    const res = await getCampAnnouncementsService();
    announcements.value = res.data ?? [];
  } catch (err: any) {
    error.value = err?.message || "最新消息暫時載入失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

onMounted(loadAnnouncements);
</script>

<template>
  <div class="news-page">
    <section class="news-hero">
      <div>
        <span class="section-kicker">LATEST NEWS</span>
        <h1>最新消息</h1>
        <p>把營區公告、活動調整、季節建議和入住提醒整理在一起，出發前先看一眼會安心很多。</p>
      </div>
      <div class="hero-badges">
        <span>營區公告</span>
        <span>活動資訊</span>
        <span>入住提醒</span>
      </div>
    </section>

    <section v-if="loading" class="status-card">資料載入中...</section>
    <section v-else-if="error" class="status-card error">{{ error }}</section>

    <template v-else>
      <section v-if="featuredAnnouncement" class="featured-card">
        <div
          class="featured-image"
          :style="{ backgroundImage: `linear-gradient(180deg, rgba(16, 23, 17, 0.12) 0%, rgba(16, 23, 17, 0.52) 100%), url(${newsImages[0]})` }"
        ></div>
        <div class="featured-copy">
          <span class="section-kicker">FEATURED</span>
          <h2>{{ featuredAnnouncement.title }}</h2>
          <p>{{ featuredAnnouncement.content }}</p>
        </div>
      </section>

      <section class="news-grid">
        <article
          v-for="(announcement, index) in secondaryAnnouncements"
          :key="announcement.id"
          class="news-card"
        >
          <div
            class="news-card-image"
            :style="{ backgroundImage: `linear-gradient(180deg, rgba(15, 24, 18, 0.08) 0%, rgba(15, 24, 18, 0.36) 100%), url(${newsImages[(index + 1) % newsImages.length]})` }"
          ></div>
          <div class="news-card-copy">
            <span>營區公告</span>
            <h3>{{ announcement.title }}</h3>
            <p>{{ announcement.content }}</p>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.news-page {
  width: min(1520px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  gap: 24px;
  padding-bottom: 28px;
}

.news-hero,
.featured-card,
.news-card,
.status-card {
  border-radius: 32px;
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(120, 111, 92, 0.1);
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.news-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
  padding: 32px;
  background:
    linear-gradient(90deg, rgba(244, 239, 231, 0.96) 0%, rgba(244, 239, 231, 0.78) 42%, rgba(42, 53, 38, 0.14) 100%),
    url("https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1800&q=80")
      center / cover no-repeat;
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

.news-hero h1,
.featured-copy h2 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.news-hero h1 {
  font-size: clamp(2.8rem, 4.4vw, 4.2rem);
}

.news-hero p,
.featured-copy p,
.news-card-copy p {
  color: #61685d;
  line-height: 1.8;
}

.hero-badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-badges span,
.news-card-copy span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 252, 247, 0.78);
  color: #667046;
  font-size: 0.88rem;
}

.status-card {
  padding: 20px 24px;
}

.status-card.error {
  color: #a44f39;
}

.featured-card {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  overflow: hidden;
}

.featured-image,
.news-card-image {
  min-height: 320px;
  background-size: cover;
  background-position: center;
}

.featured-copy {
  padding: 30px;
}

.featured-copy h2 {
  font-size: clamp(2rem, 3vw, 2.8rem);
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.news-card {
  overflow: hidden;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.news-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 18px 32px rgba(61, 52, 34, 0.12);
  border-color: rgba(111, 117, 64, 0.2);
}

.news-card-image {
  min-height: 220px;
}

.news-card-copy {
  padding: 22px;
}

.news-card-copy h3 {
  margin: 14px 0 10px;
  font-size: 1.3rem;
}

@media (max-width: 1024px) {
  .news-hero,
  .featured-card,
  .news-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: start;
  }
}

@media (max-width: 960px) {
  .news-page {
    width: min(100%, calc(100% - 24px));
  }
}
</style>
