<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getCampSiteDetailService, getCampZonesService } from "@/api/camp";

type CampSite = {
  name?: string;
  shortDescription?: string;
  checkInTime?: string;
  checkOutTime?: string;
};

type CampUnit = {
  id: number;
  name: string;
  description?: string;
  maxGuests: number;
  quantity: number;
  unitType: string;
  hasShelter: boolean;
  electricityAvailable: boolean;
};

type CampZoneDetail = {
  zone: {
    id: number;
    name: string;
    description?: string;
  };
  units: CampUnit[];
};

const zoneImages = [
  "https://images.unsplash.com/photo-1501785888041-af3ef285b470?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1523987355523-c7b5b0723c8a?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1500534314209-a25ddb2bd429?auto=format&fit=crop&w=1200&q=80",
  "https://images.unsplash.com/photo-1504851149312-7a075b496cc7?auto=format&fit=crop&w=1200&q=80",
];

const loading = ref(true);
const error = ref("");
const site = ref<CampSite | null>(null);
const zones = ref<CampZoneDetail[]>([]);

const loadData = async () => {
  loading.value = true;
  error.value = "";
  try {
    const [siteRes, zoneRes] = await Promise.all([getCampSiteDetailService(), getCampZonesService()]);
    site.value = siteRes.data?.site ?? null;
    zones.value = zoneRes.data ?? [];
  } catch (err: any) {
    error.value = err?.message || "營位資料暫時載入失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

const totalUnits = computed(() =>
  zones.value.reduce(
    (sum, zone) => sum + zone.units.reduce((unitSum, unit) => unitSum + (unit.quantity || 0), 0),
    0
  )
);

onMounted(loadData);
</script>

<template>
  <div class="zones-page">
    <section class="zones-hero">
      <div class="hero-copy">
        <span class="section-kicker">STAY OPTIONS</span>
        <h1>營位介紹</h1>
        <p>
          {{ site?.shortDescription || "先挑你想住的節奏，再安排日期和同行方式。" }}
        </p>
        <div class="hero-stats">
          <div>
            <strong>{{ zones.length }}</strong>
            <span>營區分區</span>
          </div>
          <div>
            <strong>{{ totalUnits }}</strong>
            <span>可安排席位</span>
          </div>
          <div>
            <strong>{{ site?.checkInTime || "15:00" }}</strong>
            <span>入住時間</span>
          </div>
        </div>
      </div>
      <div class="hero-card">
        <strong>入住 / 離營</strong>
        <p>{{ site?.checkInTime || "15:00" }} 入住，{{ site?.checkOutTime || "11:00" }} 離營</p>
        <RouterLink class="hero-cta" to="/booking">直接前往預約</RouterLink>
      </div>
    </section>

    <section v-if="loading" class="status-card">資料載入中...</section>
    <section v-else-if="error" class="status-card error">{{ error }}</section>

    <section v-else class="zones-grid">
      <article v-for="(zone, index) in zones" :key="zone.zone.id" class="zone-showcase">
        <div
          class="zone-visual"
          :style="{ backgroundImage: `linear-gradient(180deg, rgba(10, 18, 14, 0.08) 0%, rgba(10, 18, 14, 0.56) 100%), url(${zoneImages[index % zoneImages.length]})` }"
        >
          <div class="zone-visual-copy">
            <span>ZONE {{ String(index + 1).padStart(2, '0') }}</span>
            <h2>{{ zone.zone.name }}</h2>
            <p>{{ zone.zone.description || "保留山景、夜色與剛剛好的營火距離。" }}</p>
          </div>
        </div>
        <div class="zone-panel">
          <div class="zone-panel-head">
            <h3>營位方案</h3>
            <span>{{ zone.units.length }} 種配置</span>
          </div>
          <div class="unit-grid">
            <article v-for="unit in zone.units" :key="unit.id" class="unit-card">
              <div class="unit-topline">
                <strong>{{ unit.name }}</strong>
                <span>{{ unit.quantity }} 位</span>
              </div>
              <p>{{ unit.description || "適合想要保留舒適與彈性的露營方式。" }}</p>
              <div class="unit-meta">
                <span><i class="bi bi-people"></i> {{ unit.maxGuests }} 人</span>
                <span>{{ unit.hasShelter ? "雨棚區" : "草地區" }}</span>
                <span>{{ unit.electricityAvailable ? "供電" : "自然區" }}</span>
              </div>
            </article>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.zones-page {
  width: min(1520px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  gap: 28px;
  padding-bottom: 28px;
}

.zones-hero {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 24px;
  padding: 32px;
  border-radius: 36px;
  background:
    linear-gradient(90deg, rgba(244, 239, 231, 0.96) 0%, rgba(244, 239, 231, 0.78) 42%, rgba(42, 53, 38, 0.16) 100%),
    url("https://images.unsplash.com/photo-1508873699372-7aeab60b44ab?auto=format&fit=crop&w=1800&q=80")
      center / cover no-repeat;
  border: 1px solid rgba(120, 111, 92, 0.12);
}

.hero-copy h1,
.zone-visual-copy h2 {
  margin: 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

.hero-copy h1 {
  margin-top: 12px;
  font-size: clamp(2.8rem, 4.6vw, 4.3rem);
}

.hero-copy p {
  max-width: 560px;
  margin: 18px 0 0;
  color: #5e6659;
  line-height: 1.8;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.hero-stats div,
.hero-card,
.status-card,
.zone-panel {
  border-radius: 24px;
  background: rgba(255, 252, 247, 0.88);
  border: 1px solid rgba(120, 111, 92, 0.12);
}

.hero-stats div {
  padding: 18px;
}

.hero-stats strong,
.hero-card strong {
  display: block;
  font-size: 1.5rem;
  color: #293025;
}

.hero-stats span,
.hero-card p {
  color: #68705f;
}

.hero-card {
  align-self: end;
  padding: 24px;
  box-shadow: 0 18px 34px rgba(61, 52, 34, 0.08);
}

.hero-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 22px;
  margin-top: 18px;
  border-radius: 999px;
  background: #6f7540;
  color: #fffdf7;
  text-decoration: none;
  font-weight: 600;
}

.status-card {
  padding: 20px 24px;
}

.status-card.error {
  color: #a44f39;
}

.zones-grid {
  display: grid;
  gap: 24px;
}

.zone-showcase {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  gap: 18px;
}

.zone-visual,
.zone-panel {
  overflow: hidden;
  min-height: 320px;
}

.zone-visual {
  display: flex;
  align-items: end;
  padding: 24px;
  border-radius: 28px;
  color: #fffdf7;
  background-size: cover;
  background-position: center;
}

.zone-visual-copy span {
  display: inline-block;
  margin-bottom: 8px;
  font-size: 0.8rem;
  letter-spacing: 0.12em;
  color: rgba(255, 253, 247, 0.78);
}

.zone-visual-copy h2 {
  font-size: clamp(2rem, 3vw, 2.8rem);
}

.zone-visual-copy p {
  max-width: 420px;
  margin: 12px 0 0;
  line-height: 1.8;
  color: rgba(255, 253, 247, 0.88);
}

.zone-panel {
  padding: 22px;
}

.zone-panel-head,
.unit-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.zone-panel-head h3,
.unit-topline strong {
  margin: 0;
}

.zone-panel-head span,
.unit-topline span {
  color: #777d70;
  font-size: 0.9rem;
}

.unit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.unit-card {
  padding: 18px;
  border-radius: 20px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    border-color 0.24s ease;
}

.unit-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 28px rgba(61, 52, 34, 0.1);
  border-color: rgba(111, 117, 64, 0.18);
}

.unit-card p {
  margin: 10px 0 14px;
  color: #61685d;
  line-height: 1.7;
}

.unit-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.unit-meta span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f1ede2;
  color: #596049;
  font-size: 0.82rem;
}

@media (max-width: 1024px) {
  .zones-hero,
  .zone-showcase {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .zones-page {
    width: min(100%, calc(100% - 24px));
  }

  .hero-stats,
  .unit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
