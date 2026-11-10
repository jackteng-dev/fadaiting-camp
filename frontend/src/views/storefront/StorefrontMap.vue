<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { getCampSiteDetailService, getCampZonesService } from "@/api/camp";

type CampSite = {
  name?: string;
  shortDescription?: string;
  addressLine?: string;
  checkInTime?: string;
  checkOutTime?: string;
};

type CampUnit = {
  id: number;
  name: string;
  description?: string;
  maxGuests: number;
  quantity: number;
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

const loading = ref(true);
const error = ref("");
const site = ref<CampSite | null>(null);
const zones = ref<CampZoneDetail[]>([]);
const selectedZoneId = ref<number | null>(null);

const zonePositions = [
  { top: "14%", left: "12%" },
  { top: "30%", left: "48%" },
  { top: "58%", left: "24%" },
  { top: "66%", left: "66%" },
  { top: "18%", left: "72%" },
];

const loadData = async () => {
  loading.value = true;
  error.value = "";
  try {
    const [siteRes, zoneRes] = await Promise.all([getCampSiteDetailService(), getCampZonesService()]);
    site.value = siteRes.data?.site ?? null;
    zones.value = zoneRes.data ?? [];
    selectedZoneId.value = zoneRes.data?.[0]?.zone.id ?? null;
  } catch (err: any) {
    error.value = err?.message || "地圖資料暫時載入失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

const selectedZone = computed(() =>
  zones.value.find((zone) => zone.zone.id === selectedZoneId.value) || zones.value[0] || null
);

const zoneMarkers = computed(() =>
  zones.value.map((zone, index) => {
    const totalUnits = zone.units.reduce((sum, unit) => sum + (unit.quantity || 0), 0);
    const maxGuests = Math.max(...zone.units.map((unit) => unit.maxGuests || 0), 0);
    return {
      id: zone.zone.id,
      name: zone.zone.name,
      description: zone.zone.description || "保留山景與剛剛好的露營距離。",
      totalUnits,
      maxGuests,
      unitTypes: zone.units.length,
      position: zonePositions[index % zonePositions.length],
    };
  })
);

onMounted(loadData);
</script>

<template>
  <div class="map-page">
    <section class="map-hero">
      <div>
        <span class="section-kicker">CAMP MAP</span>
        <h1>營區地圖</h1>
        <p>
          用比較直覺的方式先認識各區位置、動線和住宿節奏。正式空位仍以營區人工確認與訂位頁資料為準。
        </p>
      </div>
      <div class="hero-notes">
        <div>
          <strong>{{ site?.checkInTime || "14:00" }}</strong>
          <span>入住時間</span>
        </div>
        <div>
          <strong>{{ site?.checkOutTime || "11:00" }}</strong>
          <span>離營時間</span>
        </div>
        <div>
          <strong>{{ zones.length }}</strong>
          <span>營區分區</span>
        </div>
      </div>
    </section>

    <section v-if="loading" class="status-card">資料載入中...</section>
    <section v-else-if="error" class="status-card error">{{ error }}</section>

    <section v-else class="map-layout">
      <div class="map-board">
        <div class="map-road road-a"></div>
        <div class="map-road road-b"></div>
        <div class="map-road road-c"></div>
        <div class="map-road road-d"></div>
        <div class="map-water"></div>
        <div class="map-woods woods-a"></div>
        <div class="map-woods woods-b"></div>
        <div class="map-gate">入口</div>
        <div class="map-service parking">停車區</div>
        <div class="map-service bath">衛浴區</div>
        <div class="map-service lounge">交誼區</div>

        <button
          v-for="marker in zoneMarkers"
          :key="marker.id"
          class="map-marker"
          :class="{ active: selectedZoneId === marker.id }"
          :style="{ top: marker.position.top, left: marker.position.left }"
          type="button"
          @click="selectedZoneId = marker.id"
        >
          <strong>{{ marker.name }}</strong>
          <span>{{ marker.totalUnits }} 位</span>
        </button>
      </div>

      <aside class="map-sidebar">
        <div class="sidebar-head">
          <span class="section-kicker">ZONE DETAILS</span>
          <h2>{{ selectedZone?.zone.name || "選擇分區" }}</h2>
          <p>{{ selectedZone?.zone.description || "點選地圖上的分區，查看營位摘要與入住條件。" }}</p>
        </div>

        <div v-if="selectedZone" class="sidebar-stats">
          <div>
            <strong>{{ selectedZone.units.reduce((sum, unit) => sum + (unit.quantity || 0), 0) }}</strong>
            <span>席位總數</span>
          </div>
          <div>
            <strong>{{ selectedZone.units.length }}</strong>
            <span>營位種類</span>
          </div>
          <div>
            <strong>{{ Math.max(...selectedZone.units.map((unit) => unit.maxGuests || 0), 0) }}</strong>
            <span>單位上限</span>
          </div>
        </div>

        <div class="unit-stack">
          <article v-for="unit in selectedZone?.units || []" :key="unit.id" class="unit-row">
            <div class="unit-row-top">
              <strong>{{ unit.name }}</strong>
              <span>{{ unit.quantity }} 位</span>
            </div>
            <p>{{ unit.description || "適合想保留舒適與彈性的露營方式。" }}</p>
            <div class="unit-tags">
              <span><i class="bi bi-people"></i> {{ unit.maxGuests }} 人</span>
              <span>{{ unit.hasShelter ? "雨棚區" : "草地區" }}</span>
              <span>{{ unit.electricityAvailable ? "供電" : "自然區" }}</span>
            </div>
          </article>
        </div>

        <div class="sidebar-actions">
          <RouterLink class="primary-cta" to="/booking">前往預約</RouterLink>
          <RouterLink class="secondary-cta" to="/zones">查看完整營位介紹</RouterLink>
        </div>

        <p class="address-line">{{ site?.addressLine || "山嵐縣雲霧鄉清水村 8 號（虛構地址）" }}</p>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.map-page {
  width: min(1520px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  gap: 24px;
  padding-bottom: 28px;
}

.map-hero,
.map-sidebar,
.status-card {
  border-radius: 32px;
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(120, 111, 92, 0.1);
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.map-hero {
  display: grid;
  grid-template-columns: 1.02fr 0.98fr;
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

.map-hero h1,
.sidebar-head h2 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.map-hero h1 {
  font-size: clamp(2.8rem, 4.4vw, 4.2rem);
}

.map-hero p,
.sidebar-head p,
.unit-row p,
.address-line {
  color: #61685d;
  line-height: 1.8;
}

.hero-notes {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hero-notes div,
.sidebar-stats div {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 252, 247, 0.8);
  border: 1px solid rgba(120, 111, 92, 0.12);
}

.hero-notes strong,
.sidebar-stats strong {
  display: block;
  font-size: 1.5rem;
  color: #293025;
}

.hero-notes span,
.sidebar-stats span {
  color: #68705f;
}

.status-card {
  padding: 20px 24px;
}

.status-card.error {
  color: #a44f39;
}

.map-layout {
  display: grid;
  grid-template-columns: 1.18fr 0.82fr;
  gap: 18px;
}

.map-board {
  position: relative;
  min-height: 760px;
  overflow: hidden;
  border-radius: 32px;
  background:
    radial-gradient(circle at 18% 16%, rgba(255, 255, 255, 0.54), transparent 26%),
    radial-gradient(circle at 78% 28%, rgba(255, 255, 255, 0.34), transparent 24%),
    #dfe8c9;
  border: 1px solid rgba(120, 111, 92, 0.1);
}

.map-road,
.map-water,
.map-woods,
.map-gate,
.map-service,
.map-marker {
  position: absolute;
}

.map-road {
  background: rgba(255, 249, 239, 0.94);
  border-radius: 999px;
}

.road-a {
  top: 52px;
  left: 36px;
  width: 76%;
  height: 18px;
}

.road-b {
  top: 160px;
  left: 112px;
  width: 20px;
  height: 420px;
}

.road-c {
  top: 318px;
  left: 104px;
  width: 64%;
  height: 18px;
}

.road-d {
  top: 504px;
  left: 268px;
  width: 18px;
  height: 170px;
}

.map-water {
  right: 0;
  top: 0;
  width: 20%;
  height: 100%;
  background: linear-gradient(180deg, rgba(126, 160, 176, 0.72), rgba(152, 177, 189, 0.86));
}

.map-woods {
  background: radial-gradient(circle, rgba(137, 164, 105, 0.8), rgba(124, 153, 92, 0.96));
  border-radius: 999px;
  opacity: 0.85;
}

.woods-a {
  top: 100px;
  left: 18px;
  width: 110px;
  height: 110px;
}

.woods-b {
  bottom: 54px;
  right: 160px;
  width: 136px;
  height: 136px;
}

.map-gate,
.map-service {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 0.9rem;
}

.map-gate {
  top: 70px;
  left: 44px;
  background: #826b4d;
  color: #fffdf8;
}

.map-service {
  background: rgba(255, 252, 245, 0.94);
  color: #4d5640;
  box-shadow: 0 12px 22px rgba(68, 76, 55, 0.12);
}

.parking {
  top: 128px;
  left: 206px;
}

.bath {
  top: 402px;
  left: 154px;
}

.lounge {
  top: 350px;
  right: 166px;
}

.map-marker {
  transform: translate(-50%, -50%);
  display: grid;
  gap: 4px;
  min-width: 128px;
  padding: 16px 18px;
  border: 0;
  border-radius: 20px;
  background: rgba(255, 252, 245, 0.95);
  box-shadow: 0 14px 26px rgba(68, 76, 55, 0.12);
  text-align: left;
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    background 0.24s ease;
}

.map-marker:hover,
.map-marker.active {
  transform: translate(-50%, calc(-50% - 6px));
  background: rgba(255, 253, 248, 1);
  box-shadow: 0 20px 34px rgba(68, 76, 55, 0.18);
}

.map-marker strong {
  color: #2f362b;
}

.map-marker span {
  color: #737a6e;
  font-size: 0.86rem;
}

.map-sidebar {
  padding: 24px;
}

.sidebar-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin: 20px 0 18px;
}

.unit-stack {
  display: grid;
  gap: 12px;
}

.unit-row {
  padding: 18px;
  border-radius: 22px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
}

.unit-row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.unit-row-top span {
  color: #7b8174;
  font-size: 0.9rem;
}

.unit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.unit-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f1ede2;
  color: #596049;
  font-size: 0.82rem;
}

.sidebar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.primary-cta,
.secondary-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 48px;
  padding: 0 22px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 600;
}

.primary-cta {
  background: #6f7540;
  color: #fffdf7;
}

.secondary-cta {
  border: 1px solid rgba(99, 102, 84, 0.14);
  background: rgba(255, 252, 247, 0.8);
  color: #2f362b;
}

.address-line {
  margin: 18px 0 0;
}

@media (max-width: 1180px) {
  .map-layout,
  .map-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .map-page {
    width: min(100%, calc(100% - 24px));
  }

  .hero-notes,
  .sidebar-stats {
    grid-template-columns: 1fr;
  }

  .map-board {
    min-height: 620px;
  }
}
</style>
