<script setup lang="ts">
import { onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getCampSiteDetailService } from "@/api/camp";

type CampSite = {
  name?: string;
  shortDescription?: string;
  fullDescription?: string;
};

type GalleryItem = {
  title: string;
  subtitle: string;
  image: string;
};

const heroImage =
  "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=crop&w=1800&q=80";

const fallbackGalleryItems: GalleryItem[] = [
  {
    title: "晨霧山景",
    subtitle: "清晨散步",
    image: "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=900&q=80",
  },
  {
    title: "營區草地",
    subtitle: "營位日景",
    image: "https://images.unsplash.com/photo-1523987355523-c7b5b0723c8a?auto=format&fit=crop&w=900&q=80",
  },
  {
    title: "午後步道",
    subtitle: "周邊散策",
    image: "https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&w=900&q=80",
  },
  {
    title: "遠山夕色",
    subtitle: "黃昏時段",
    image: "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=900&q=80",
  },
  {
    title: "森林呼吸",
    subtitle: "自然體驗",
    image: "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?auto=format&fit=crop&w=900&q=80",
  },
  {
    title: "溪谷綠意",
    subtitle: "山谷風景",
    image: "https://images.unsplash.com/photo-1501555088652-021faa106b9b?auto=format&fit=crop&w=900&q=80",
  },
];

const loading = ref(true);
const site = ref<CampSite | null>(null);
const galleryItems = ref<GalleryItem[]>(fallbackGalleryItems);
const error = ref("");

const loadStorefrontData = async () => {
  loading.value = true;
  error.value = "";
  try {
    const siteRes = await getCampSiteDetailService();
    site.value = siteRes.data?.site ?? null;
    const images = siteRes.data?.images ?? [];
    galleryItems.value = images.length > 0
      ? images.slice(0, 6).map((item: any, index: number) => ({
          title: item.title || `營區照片 ${index + 1}`,
          subtitle: item.zoneName || item.altText || "營區相簿",
          image: item.imageUrl,
        }))
      : fallbackGalleryItems;
  } catch (err: any) {
    error.value = err?.message || "前台資料暫時載入失敗，請稍後再試。";
    galleryItems.value = fallbackGalleryItems;
  } finally {
    loading.value = false;
  }
};

onMounted(loadStorefrontData);
</script>

<template>
  <div class="storefront-page">
    <div class="page-frame">
      <section
        id="home"
        class="hero"
        :style="{ backgroundImage: `linear-gradient(90deg, rgba(244,239,231,0.9) 0%, rgba(244,239,231,0.58) 38%, rgba(22,34,24,0.15) 100%), url(${heroImage})` }"
      >
        <div class="hero-copy">
          <span class="section-kicker">MOUNTAIN CAMP EXPERIENCE</span>
          <h1>山之間，<br />靜謐的美好時光</h1>
          <div class="hero-divider"></div>
          <p class="hero-text">
            {{ site?.shortDescription || "遠離城市喧囂，在山林、溪谷與夜空之間，留一晚給真正放鬆的自己。" }}
          </p>
          <div class="hero-actions">
            <RouterLink class="primary-cta" to="/booking"><i class="bi bi-calendar3"></i> 立即預約</RouterLink>
            <RouterLink class="secondary-cta" to="/zones">看營位方案</RouterLink>
          </div>
        </div>
      </section>

      <main class="content-stack">
        <section id="about" class="story-section">
          <span class="section-kicker">ABOUT MOUNTAIN CAMP</span>
          <div class="story-grid">
            <div class="story-copy">
              <h2>把山林、溪谷、夜色<br />留給真正想慢下來的人</h2>
              <p>
                {{
                  site?.fullDescription ||
                  "發呆亭露營區（虛構示範營區）位在山谷裡，離吊橋、溫泉和溪谷都很近。白天適合散步、拍照、親子活動，晚上就把營火、晚餐和聊天拉長一點。"
                }}
              </p>
            </div>
            <div class="story-aside">
              <article class="story-note">
                <strong>山裡的節奏</strong>
                <p>白天看景、傍晚看雲、晚上看火光，讓行程不需要排太滿也能很完整。</p>
              </article>
              <article class="story-note">
                <strong>保留空白</strong>
                <p>首頁只先介紹這裡的氣氛，營位、地圖、訂位和公告就交給各自的完整頁面。</p>
              </article>
            </div>
          </div>
        </section>

        <section class="gallery-section">
          <div class="section-heading compact">
            <div>
              <span class="section-kicker">PHOTO GALLERY</span>
              <h2>照片相簿</h2>
            </div>
            <p>先用幾張示意畫面把山裡的光線、空氣和停留感抓出來，之後再替換成你的真實營區照片。</p>
          </div>
          <div class="gallery-grid">
            <article
              v-for="(item, index) in galleryItems"
              :key="item.image + index"
              class="gallery-tile"
              :class="index === 0 ? 'featured' : ''"
              :style="{ backgroundImage: `linear-gradient(180deg, rgba(15, 24, 18, 0.08) 0%, rgba(15, 24, 18, 0.72) 100%), url(${item.image})` }"
            >
              <div class="gallery-overlay">
                <span>{{ item.subtitle }}</span>
                <strong>{{ item.title }}</strong>
              </div>
            </article>
          </div>
        </section>

        <section v-if="loading || error" class="status-section">
          <p v-if="loading">資料載入中...</p>
          <p v-else>{{ error }}</p>
        </section>
      </main>
    </div>
  </div>
</template>

<style scoped>
.storefront-page {
  color: #1f261d;
  background:
    radial-gradient(circle at top, rgba(255, 255, 255, 0.86), rgba(255, 255, 255, 0.74)),
    #f5f1e8;
}

.page-frame {
  width: min(1520px, calc(100% - 32px));
  margin: 0 auto;
  padding: 20px 0 48px;
}

.hero {
  min-height: 660px;
  display: flex;
  align-items: center;
  padding: 88px 88px 94px;
  border-radius: 36px;
  background-size: cover;
  background-position: center;
  box-shadow: 0 20px 50px rgba(63, 52, 35, 0.08);
}

.hero-copy {
  max-width: 560px;
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

.hero h1,
.story-copy h2,
.section-heading h2 {
  margin: 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.hero h1 {
  margin-top: 18px;
  font-size: clamp(3.3rem, 5vw, 5.2rem);
  line-height: 1.08;
}

.hero-divider {
  width: 72px;
  height: 2px;
  margin: 28px 0;
  background: #b3a06d;
}

.hero-text,
.story-copy p,
.story-note p,
.section-heading p,
.status-section p {
  color: #62685f;
  line-height: 1.8;
}

.hero-text {
  max-width: 460px;
  font-size: 1.15rem;
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.primary-cta,
.secondary-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 24px;
  border-radius: 999px;
  text-decoration: none;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    background 0.24s ease,
    border-color 0.24s ease;
}

.primary-cta {
  gap: 10px;
  background: #6f7540;
  color: #fffdf7;
  font-weight: 600;
}

.secondary-cta {
  border: 1px solid rgba(99, 102, 84, 0.12);
  background: rgba(255, 252, 247, 0.72);
  color: #2f362b;
}

.content-stack {
  display: grid;
  gap: 32px;
  padding-top: 28px;
}

.story-section,
.gallery-section,
.status-section {
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(120, 111, 92, 0.1);
  border-radius: 28px;
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.story-section,
.gallery-section {
  padding: 34px;
}

.story-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 28px;
  align-items: start;
  margin-top: 18px;
}

.story-copy h2,
.section-heading h2 {
  margin-top: 10px;
  font-size: clamp(2.2rem, 3vw, 3.2rem);
  line-height: 1.18;
}

.story-aside {
  display: grid;
  gap: 16px;
}

.story-note {
  padding: 22px 24px;
  border-radius: 22px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.story-note strong {
  display: block;
  margin-bottom: 10px;
  font-size: 1.08rem;
}

.story-note p,
.section-heading p {
  margin: 0;
}

.section-heading {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.section-heading.compact {
  align-items: start;
}

.section-heading p {
  max-width: 520px;
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  grid-auto-rows: 142px;
  gap: 12px;
}

.gallery-tile {
  position: relative;
  display: flex;
  align-items: end;
  overflow: hidden;
  padding: 16px;
  border-radius: 20px;
  background-size: cover;
  background-position: center;
  color: #fffdf7;
  transition:
    transform 0.28s ease,
    box-shadow 0.28s ease,
    filter 0.28s ease;
}

.gallery-tile.featured {
  grid-column: span 2;
  grid-row: span 2;
}

.gallery-overlay {
  display: grid;
  gap: 4px;
  transform: translateY(12px);
  opacity: 0.86;
  transition:
    transform 0.28s ease,
    opacity 0.28s ease;
}

.gallery-overlay span {
  font-size: 0.78rem;
  letter-spacing: 0.08em;
  color: rgba(255, 253, 247, 0.76);
}

.gallery-overlay strong {
  font-size: 1rem;
}

.status-section {
  padding: 18px 22px;
}

.primary-cta:hover,
.primary-cta:focus-visible {
  background: #5f6737;
  transform: translateY(-2px);
  box-shadow: 0 16px 30px rgba(88, 96, 49, 0.22);
}

.secondary-cta:hover,
.secondary-cta:focus-visible {
  transform: translateY(-2px);
  background: rgba(255, 252, 247, 0.92);
  border-color: rgba(99, 102, 84, 0.22);
}

.story-note:hover,
.story-note:focus-within,
.gallery-tile:hover,
.gallery-tile:focus-within {
  transform: translateY(-6px);
  box-shadow: 0 18px 32px rgba(61, 52, 34, 0.12);
}

.story-note:hover,
.story-note:focus-within {
  border-color: rgba(111, 117, 64, 0.2);
}

.gallery-tile:hover,
.gallery-tile:focus-within {
  filter: saturate(1.08);
}

.gallery-tile:hover .gallery-overlay,
.gallery-tile:focus-within .gallery-overlay {
  transform: translateY(0);
  opacity: 1;
}

@media (max-width: 960px) {
  .page-frame {
    width: min(100%, calc(100% - 24px));
  }

  .hero {
    min-height: auto;
    padding: 56px 24px 68px;
    border-radius: 30px;
  }

  .hero h1 {
    font-size: clamp(2.7rem, 12vw, 3.7rem);
  }

  .hero-text {
    font-size: 1rem;
  }

  .hero-actions {
    flex-direction: column;
  }

  .primary-cta,
  .secondary-cta {
    width: 100%;
  }

  .story-section,
  .gallery-section {
    padding: 24px 18px;
  }

  .story-grid,
  .gallery-grid {
    grid-template-columns: 1fr;
  }

  .gallery-tile.featured {
    grid-column: span 1;
    grid-row: span 1;
  }

  .section-heading {
    flex-direction: column;
    align-items: start;
  }
}

@media (max-width: 640px) {
  .page-frame {
    width: min(100%, calc(100% - 18px));
    padding: 12px 0 32px;
  }

  .hero {
    padding: 44px 18px 52px;
    border-radius: 24px;
  }

  .hero-divider {
    margin: 22px 0;
  }

  .content-stack {
    gap: 20px;
    padding-top: 20px;
  }

  .story-copy h2,
  .section-heading h2 {
    font-size: clamp(1.9rem, 10vw, 2.5rem);
  }

  .gallery-grid {
    grid-auto-rows: 170px;
  }

  .gallery-tile {
    min-height: 170px;
  }
}
</style>
