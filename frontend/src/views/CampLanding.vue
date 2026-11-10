<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import {
  createBookingService,
  getCampAnnouncementsService,
  getCampSiteDetailService,
  getCampZonesService,
} from "@/api/camp";
import { confirmAdminAction } from "@/utils/confirmAction";

type CampSite = {
  name?: string;
  shortDescription?: string;
  fullDescription?: string;
  addressLine?: string;
  contactPhone?: string;
  contactEmail?: string;
  facebookUrl?: string;
  checkInTime?: string;
  checkOutTime?: string;
  bookingNotice?: string;
};

type Facility = {
  id: number;
  name: string;
  description?: string;
};

type Faq = {
  id: number;
  question: string;
  answer: string;
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

type CampZone = {
  id: number;
  name: string;
  description?: string;
};

type CampZoneDetail = {
  zone: CampZone;
  units: CampUnit[];
};

type Announcement = {
  id: number;
  title: string;
  content: string;
};

const loading = ref(true);
const error = ref("");
const site = ref<CampSite | null>(null);
const facilities = ref<Facility[]>([]);
const faqs = ref<Faq[]>([]);
const zones = ref<CampZoneDetail[]>([]);
const announcements = ref<Announcement[]>([]);
const checkInDate = ref("");
const checkOutDate = ref("");
const selectedUnitId = ref<number | null>(null);
const guestCount = ref(2);
const vehicleCount = ref(1);
const contactName = ref("");
const contactPhone = ref("");
const contactEmail = ref("");
const bookingNote = ref("");
const bookingSubmitting = ref(false);
const bookingError = ref("");
const bookingSuccess = ref("");

const loadCampData = async () => {
  loading.value = true;
  error.value = "";
  try {
    const [siteRes, zoneRes, announcementRes] = await Promise.all([
      getCampSiteDetailService(),
      getCampZonesService(),
      getCampAnnouncementsService(),
    ]);
    site.value = siteRes.data?.site ?? null;
    facilities.value = siteRes.data?.facilities ?? [];
    faqs.value = siteRes.data?.faqs ?? [];
    zones.value = zoneRes.data ?? [];
    announcements.value = announcementRes.data ?? [];
  } catch (e: any) {
    error.value = e?.message || "資料載入失敗，請稍後重試。";
  } finally {
    loading.value = false;
  }
};

const totalUnitCount = computed(() =>
  zones.value.reduce((sum, zone) => {
    return (
      sum +
      zone.units.reduce((unitSum, unit) => unitSum + (unit.quantity || 0), 0)
    );
  }, 0)
);

const totalZoneCount = computed(() => zones.value.length);

const highlightedUnits = computed(() =>
  zones.value.flatMap((zone) =>
    zone.units.map((unit) => ({
      ...unit,
      zoneName: zone.zone.name,
    }))
  )
);

const bookingMessage = computed(() => {
  if (!checkInDate.value || !checkOutDate.value) {
    return "先挑日期，我們就能更快確認適合的營位。";
  }
  return `預計 ${checkInDate.value} 入住，${checkOutDate.value} 離營。可先電話聯絡營區確認空位。`;
});

const bookingOptions = computed(() =>
  zones.value.flatMap((zone) =>
    zone.units.map((unit) => ({
      id: unit.id,
      label: `${zone.zone.name} / ${unit.name}`,
    }))
  )
);

const submitBooking = async () => {
  bookingError.value = "";
  bookingSuccess.value = "";

  if (!selectedUnitId.value) {
    bookingError.value = "請先選擇營位方案。";
    return;
  }
  if (
    !(await confirmAdminAction({
      title: "確認送出預約",
      message: "確定要送出這筆預約申請嗎？送出後營區會依資料進行人工確認。",
      confirmButtonText: "送出預約",
    }))
  ) {
    return;
  }

  bookingSubmitting.value = true;
  try {
    const response = await createBookingService({
      campUnitId: selectedUnitId.value,
      checkInDate: checkInDate.value,
      checkOutDate: checkOutDate.value,
      guestCount: guestCount.value,
      vehicleCount: vehicleCount.value,
      contactName: contactName.value,
      contactPhone: contactPhone.value,
      contactEmail: contactEmail.value || undefined,
      note: bookingNote.value || undefined,
    });
    const bookingNo = response.data?.bookingNo;
    bookingSuccess.value = bookingNo
      ? `預約單已送出，訂單編號 ${bookingNo}。營區會再人工確認。`
      : "預約單已送出，營區會再人工確認。";
  } catch (e: any) {
    bookingError.value = e?.message || "送出失敗，請稍後再試。";
  } finally {
    bookingSubmitting.value = false;
  }
};

onMounted(loadCampData);
</script>

<template>
  <div class="camp-page">
    <section class="camp-hero">
      <div class="hero-shell">
        <div class="hero-copy">
          <p class="eyebrow">HSINCHU JIANSHI CAMPGROUND</p>
          <h1>{{ site?.name || "發呆亭露營區" }}</h1>
          <p class="hero-summary">
            {{ site?.shortDescription || "山、溪、吊橋、溫泉，留一晚給山谷裡的慢節奏。" }}
          </p>
          <div class="hero-actions">
            <a class="hero-btn primary" href="#reserve">開始規劃入住</a>
            <a class="hero-btn secondary" href="#zones">看營位方案</a>
          </div>
          <div class="hero-stats">
            <div class="stat-card">
              <span class="stat-value">{{ totalZoneCount }}</span>
              <span class="stat-label">營位區域</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ totalUnitCount }}</span>
              <span class="stat-label">可管理席位數</span>
            </div>
            <div class="stat-card">
              <span class="stat-value">{{ facilities.length }}</span>
              <span class="stat-label">特色亮點</span>
            </div>
          </div>
        </div>

        <aside id="reserve" class="booking-card">
          <p class="booking-kicker">預約入口</p>
          <h2>先選日期，再聯絡營區確認</h2>
          <label>
            營位方案
            <select v-model.number="selectedUnitId">
              <option :value="null">請選擇營位方案</option>
              <option v-for="option in bookingOptions" :key="option.id" :value="option.id">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            入住日
            <input v-model="checkInDate" type="date" />
          </label>
          <label>
            離營日
            <input v-model="checkOutDate" type="date" />
          </label>
          <div class="booking-grid">
            <label>
              入住人數
              <input v-model.number="guestCount" type="number" min="1" />
            </label>
            <label>
              車輛數
              <input v-model.number="vehicleCount" type="number" min="0" />
            </label>
          </div>
          <label>
            聯絡姓名
            <input v-model="contactName" type="text" placeholder="請填寫聯絡人姓名" />
          </label>
          <label>
            聯絡電話
            <input v-model="contactPhone" type="text" placeholder="請填寫聯絡電話" />
          </label>
          <label>
            Email
            <input v-model="contactEmail" type="email" placeholder="請填寫 Email" />
          </label>
          <label>
            備註
            <textarea v-model="bookingNote" rows="3" placeholder="例如預計抵達時間、同行需求"></textarea>
          </label>
          <p class="booking-note">{{ bookingMessage }}</p>
          <p v-if="bookingError" class="booking-feedback error">{{ bookingError }}</p>
          <p v-if="bookingSuccess" class="booking-feedback success">{{ bookingSuccess }}</p>
          <div class="booking-actions">
            <button class="contact-link phone" type="button" :disabled="bookingSubmitting" @click="submitBooking">
              {{ bookingSubmitting ? "送出中..." : "送出預約單" }}
            </button>
            <a class="contact-link phone" :href="`tel:${site?.contactPhone || ''}`">
              {{ site?.contactPhone || "聯絡營區" }}
            </a>
            <a
              v-if="site?.facebookUrl"
              class="contact-link social"
              :href="site.facebookUrl"
              target="_blank"
              rel="noreferrer"
            >
              Facebook 粉專
            </a>
          </div>
        </aside>
      </div>
    </section>

    <main class="camp-content">
      <section v-if="error" class="panel error-panel">
        {{ error }}
      </section>

      <section v-if="loading" class="panel loading-panel">
        <span class="loading-dot"></span>
        <span>資料載入中...</span>
      </section>

      <template v-else>
        <section class="story-grid">
          <article class="panel story-panel">
            <h2>山谷裡的慢節奏基地</h2>
            <p class="description">{{ site?.fullDescription }}</p>
            <div class="meta-grid">
              <div>
                <span class="meta-label">地址</span>
                <strong>{{ site?.addressLine }}</strong>
              </div>
              <div>
                <span class="meta-label">電話</span>
                <strong>{{ site?.contactPhone }}</strong>
              </div>
              <div>
                <span class="meta-label">Email</span>
                <strong>{{ site?.contactEmail }}</strong>
              </div>
              <div>
                <span class="meta-label">入住 / 退房</span>
                <strong>{{ site?.checkInTime }} / {{ site?.checkOutTime }}</strong>
              </div>
            </div>
          </article>

          <article class="panel notice-panel">
            <h2>入住提醒</h2>
            <p>{{ site?.bookingNotice }}</p>
            <div class="facility-stack">
              <span v-for="facility in facilities" :key="facility.id" class="facility-badge">
                {{ facility.name }}
              </span>
            </div>
          </article>
        </section>

        <section id="zones" class="panel">
          <div class="section-head">
            <div>
              <p class="eyebrow dark">STAY OPTIONS</p>
              <h2>營位與方案</h2>
            </div>
            <p class="section-copy">
              先從區域挑氛圍，再看每種席位的容納與設備。
            </p>
          </div>

          <div class="zone-grid">
            <article v-for="zone in zones" :key="zone.zone.id" class="zone-card">
              <div class="zone-title-row">
                <h3>{{ zone.zone.name }}</h3>
                <span>{{ zone.units.length }} 種方案</span>
              </div>
              <p class="zone-description">{{ zone.zone.description }}</p>
              <div class="unit-list">
                <div v-for="unit in zone.units" :key="unit.id" class="unit-card">
                  <div class="unit-top">
                    <strong>{{ unit.name }}</strong>
                    <span>{{ unit.quantity }} 席</span>
                  </div>
                  <p>{{ unit.description }}</p>
                  <div class="unit-tags">
                    <span>{{ unit.maxGuests }} 人</span>
                    <span v-if="unit.hasShelter">有遮蔽</span>
                    <span v-if="unit.electricityAvailable">供電</span>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="highlight-grid">
          <article class="panel">
            <div class="section-head compact">
              <div>
                <p class="eyebrow dark">WHAT TO EXPECT</p>
                <h2>特色整理</h2>
              </div>
            </div>
            <div class="highlight-list">
              <div
                v-for="unit in highlightedUnits"
                :key="`${unit.zoneName}-${unit.id}`"
                class="highlight-item"
              >
                <strong>{{ unit.name }}</strong>
                <span>{{ unit.zoneName }}</span>
              </div>
            </div>
          </article>

          <article class="panel">
            <div class="section-head compact">
              <div>
                <p class="eyebrow dark">LATEST</p>
                <h2>最新公告</h2>
              </div>
            </div>
            <ul class="announcement-list">
              <li v-for="announcement in announcements" :key="announcement.id">
                <strong>{{ announcement.title }}</strong>
                <p>{{ announcement.content }}</p>
              </li>
            </ul>
          </article>
        </section>

        <section class="panel faq-panel">
          <div class="section-head">
            <div>
              <p class="eyebrow dark">FAQ</p>
              <h2>常見問題</h2>
            </div>
          </div>
          <div class="faq-list">
            <article v-for="faq in faqs" :key="faq.id" class="faq-item">
              <h4>{{ faq.question }}</h4>
              <p>{{ faq.answer }}</p>
            </article>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
:global(body) {
  margin: 0;
  background: #f3efdf;
}

.camp-page {
  --camp-ink: #1f2b1f;
  --camp-moss: #50663e;
  --camp-fern: #71884d;
  --camp-sand: #f1dfb5;
  --camp-paper: #fffaf0;
  --camp-panel: rgba(255, 250, 240, 0.92);
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(241, 223, 181, 0.62), transparent 28%),
    radial-gradient(circle at top right, rgba(120, 143, 79, 0.2), transparent 20%),
    linear-gradient(180deg, #f3efdf 0%, #eef1e2 48%, #fffdf7 100%);
  color: var(--camp-ink);
}

.camp-hero {
  min-height: 88vh;
  padding: 42px 0 24px;
  background:
    linear-gradient(120deg, rgba(23, 38, 25, 0.82), rgba(52, 81, 42, 0.58)),
    url("https://images.unsplash.com/photo-1508873699372-7aeab60b44ab?auto=format&fit=crop&w=1800&q=80")
      center / cover no-repeat;
}

.hero-shell,
.camp-content {
  width: min(1120px, calc(100vw - 32px));
  margin: 0 auto;
}

.hero-shell {
  min-height: calc(88vh - 66px);
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 380px);
  gap: 24px;
  align-items: end;
}

.hero-copy {
  color: #fff8e8;
  padding: 20px 0;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #edd9a2;
}

.eyebrow.dark {
  color: var(--camp-fern);
}

.hero-copy h1 {
  margin: 0;
  max-width: 10ch;
  font-size: clamp(42px, 7vw, 74px);
  line-height: 0.95;
}

.hero-summary {
  max-width: 560px;
  margin: 18px 0 24px;
  font-size: clamp(16px, 2vw, 20px);
  line-height: 1.7;
  color: rgba(255, 248, 232, 0.92);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 26px;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 18px;
  border-radius: 8px;
  font-weight: 700;
  text-decoration: none;
}

.hero-btn.primary {
  background: var(--camp-sand);
  color: #332300;
}

.hero-btn.secondary {
  border: 1px solid rgba(255, 248, 232, 0.34);
  color: #fff8e8;
  background: rgba(255, 255, 255, 0.08);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 560px;
}

.stat-card,
.booking-card,
.panel {
  backdrop-filter: blur(8px);
}

.stat-card {
  padding: 14px;
  border-radius: 8px;
  background: rgba(255, 248, 232, 0.1);
  border: 1px solid rgba(255, 248, 232, 0.12);
}

.stat-value {
  display: block;
  font-size: 30px;
  font-weight: 700;
}

.stat-label {
  display: block;
  margin-top: 4px;
  color: rgba(255, 248, 232, 0.82);
}

.booking-card {
  align-self: center;
  padding: 22px;
  border-radius: 8px;
  background: rgba(255, 250, 240, 0.92);
  border: 1px solid rgba(255, 245, 226, 0.65);
  box-shadow: 0 20px 45px rgba(23, 34, 23, 0.18);
}

.booking-kicker,
.meta-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #7d745c;
}

.booking-card h2 {
  margin: 0 0 14px;
  font-size: 28px;
  line-height: 1.1;
}

.booking-card label {
  display: grid;
  gap: 6px;
  margin-bottom: 14px;
  font-weight: 600;
}

.booking-card select,
.booking-card input,
.booking-card textarea {
  width: 100%;
  min-height: 44px;
  border: 1px solid #d8cba9;
  border-radius: 8px;
  padding: 0 12px;
  background: #fffdf8;
  color: var(--camp-ink);
}

.booking-card textarea {
  min-height: 86px;
  padding: 10px 12px;
  resize: vertical;
}

.booking-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.booking-note {
  margin: 0 0 16px;
  line-height: 1.6;
  color: #5d5b50;
}

.booking-actions {
  display: grid;
  gap: 10px;
}

.contact-link {
  border: 0;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  border-radius: 8px;
  text-decoration: none;
  font-weight: 700;
}

.contact-link.phone {
  background: #324828;
  color: #fff8e8;
}

.contact-link.social {
  border: 1px solid #b9c89a;
  color: #324828;
  background: #f4f8eb;
}

.booking-feedback {
  margin: 0 0 12px;
  line-height: 1.6;
}

.booking-feedback.error {
  color: #8d1f1f;
}

.booking-feedback.success {
  color: #2f5d25;
}

.camp-content {
  display: grid;
  gap: 18px;
  margin-top: -42px;
  padding-bottom: 42px;
}

.panel {
  border: 1px solid rgba(207, 201, 179, 0.7);
  border-radius: 8px;
  padding: 20px;
  background: var(--camp-panel);
  box-shadow: 0 12px 24px rgba(53, 65, 40, 0.06);
}

.loading-panel,
.error-panel {
  display: flex;
  align-items: center;
  gap: 10px;
}

.loading-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--camp-fern);
  animation: pulse 1.2s infinite;
}

.story-grid,
.highlight-grid {
  display: grid;
  grid-template-columns: 1.3fr 0.9fr;
  gap: 18px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  margin-bottom: 14px;
}

.section-head.compact {
  align-items: start;
}

.section-head h2,
.story-panel h2,
.notice-panel h2 {
  margin: 0;
  font-size: 28px;
}

.section-copy,
.description {
  line-height: 1.75;
  color: #495643;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.facility-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}

.facility-badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eef3e3;
  border: 1px solid #d2ddbd;
  color: #3d4d2d;
}

.zone-grid {
  display: grid;
  gap: 16px;
}

.zone-card {
  padding: 18px;
  border: 1px solid #d8decf;
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(247, 250, 241, 0.92));
}

.zone-title-row,
.unit-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.zone-title-row h3,
.faq-item h4 {
  margin: 0;
}

.zone-title-row span,
.unit-top span {
  color: #6f775f;
}

.zone-description {
  margin: 8px 0 14px;
  line-height: 1.65;
  color: #4e5947;
}

.unit-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.unit-card {
  border-radius: 8px;
  padding: 14px;
  background: #fffef9;
  border: 1px solid #e4e5d9;
}

.unit-card p {
  margin: 8px 0 10px;
  color: #4c5643;
}

.unit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.unit-tags span,
.highlight-item span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f0ead5;
  color: #5a4e2d;
}

.highlight-list,
.faq-list {
  display: grid;
  gap: 12px;
}

.highlight-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #e5dfc9;
}

.announcement-list {
  margin: 0;
  padding-left: 18px;
}

.announcement-list li + li {
  margin-top: 12px;
}

.announcement-list p,
.faq-item p {
  margin: 6px 0 0;
  line-height: 1.7;
  color: #4c5643;
}

.faq-item {
  padding: 14px 0;
  border-top: 1px solid #e4ddc7;
}

.faq-item:first-child {
  border-top: 0;
  padding-top: 0;
}

.error-panel {
  border-color: #efc8c8;
  color: #8d1f1f;
  background: #fff3f3;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.65;
  }
  50% {
    transform: scale(1.45);
    opacity: 1;
  }
}

@media (max-width: 960px) {
  .hero-shell,
  .story-grid,
  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .hero-shell {
    align-items: start;
  }

  .hero-copy h1 {
    max-width: none;
  }

  .hero-stats,
  .meta-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .camp-hero {
    min-height: auto;
    padding-top: 24px;
  }

  .hero-shell,
  .camp-content {
    width: min(100vw - 20px, 1120px);
  }

  .hero-stats,
  .meta-grid,
  .unit-list,
  .booking-grid {
    grid-template-columns: 1fr;
  }

  .section-head,
  .zone-title-row,
  .unit-top,
  .highlight-item {
    display: block;
  }

  .section-copy,
  .zone-title-row span,
  .unit-top span,
  .highlight-item span {
    margin-top: 8px;
  }
}
</style>
