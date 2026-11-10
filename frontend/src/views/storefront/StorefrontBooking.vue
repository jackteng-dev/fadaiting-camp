<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import {
  createBookingService,
  getBookingQuoteService,
  getCampSiteDetailService,
  getCampZonesService,
  getPublicRentalItemsService,
  type BookingQuoteResponse,
  type PublicRentalItem,
} from "@/api/camp";
import { confirmAdminAction } from "@/utils/confirmAction";

type CampSite = {
  bookingNotice?: string;
  refundPolicy?: string;
  contactPhone?: string;
  contactEmail?: string;
  checkInTime?: string;
  checkOutTime?: string;
};

type CampZoneDetail = {
  zone: {
    id: number;
    name: string;
  };
  units: {
    id: number;
    name: string;
    description?: string;
    maxGuests: number;
    quantity: number;
    hasShelter: boolean;
    electricityAvailable: boolean;
  }[];
};

const loading = ref(true);
const error = ref("");
const site = ref<CampSite | null>(null);
const zones = ref<CampZoneDetail[]>([]);
const rentalItems = ref<PublicRentalItem[]>([]);
const rentalSelections = ref<Record<number, number>>({});

const selectedUnitId = ref<number | null>(null);
const checkInDate = ref("");
const checkOutDate = ref("");
const guestCount = ref(2);
const vehicleCount = ref(1);
const contactName = ref("");
const contactPhone = ref("");
const contactEmail = ref("");
const bookingNote = ref("");
const bookingSubmitting = ref(false);
const bookingQuoteLoading = ref(false);
const bookingQuote = ref<BookingQuoteResponse | null>(null);
const bookingError = ref("");
const bookingSuccess = ref("");
const router = useRouter();
const BOOKING_STORAGE_KEY = "storefront-last-booking";

const loadData = async () => {
  loading.value = true;
  error.value = "";
  try {
    const [siteRes, zoneRes, rentalRes] = await Promise.all([
      getCampSiteDetailService(),
      getCampZonesService(),
      getPublicRentalItemsService(),
    ]);
    site.value = siteRes.data?.site ?? null;
    zones.value = zoneRes.data ?? [];
    rentalItems.value = rentalRes.data ?? [];
  } catch (err: any) {
    error.value = err?.message || "訂位資料載入失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

const bookingOptions = computed(() =>
  zones.value.flatMap((zone) =>
    zone.units.map((unit) => ({
      id: unit.id,
      zoneName: zone.zone.name,
      label: `${zone.zone.name} / ${unit.name}`,
      description: unit.description,
      maxGuests: unit.maxGuests,
      quantity: unit.quantity,
      hasShelter: unit.hasShelter,
      electricityAvailable: unit.electricityAvailable,
    }))
  )
);

const selectedUnit = computed(() =>
  bookingOptions.value.find((item) => item.id === selectedUnitId.value) || null
);

const selectedRentalItems = computed(() =>
  Object.entries(rentalSelections.value)
    .map(([rentalItemId, quantity]) => ({
      rentalItemId: Number(rentalItemId),
      quantity: Number(quantity || 0),
    }))
    .filter((item) => item.quantity > 0)
);

const selectedRentalCount = computed(() =>
  selectedRentalItems.value.reduce((total, item) => total + item.quantity, 0)
);

const quoteRentalAvailability = computed(() => {
  const map = new Map<number, { availableQuantity: number; reservedQuantity: number; totalQuantity: number }>();
  bookingQuote.value?.rentalItems?.forEach((item) => {
    map.set(item.rentalItemId, {
      availableQuantity: item.availableQuantity,
      reservedQuantity: item.reservedQuantity,
      totalQuantity: item.totalQuantity,
    });
  });
  return map;
});

const rentalCategoryText = (value: string) => {
  const map: Record<string, string> = {
    tent: "帳棚",
    mat: "睡墊 / 防水墊",
    cooking: "炊事用品",
    lighting: "照明 / 電源",
    other: "其他",
  };
  return map[value] || value;
};

const setRentalQuantity = (rentalItem: PublicRentalItem, quantity: number) => {
  const quoteAvailability = quoteRentalAvailability.value.get(rentalItem.id);
  const maxQuantity = quoteAvailability?.availableQuantity ?? rentalItem.totalQuantity ?? 0;
  const nextQuantity = Math.max(0, Math.min(Number(quantity || 0), maxQuantity));
  rentalSelections.value = {
    ...rentalSelections.value,
    [rentalItem.id]: nextQuantity,
  };
};

const canRequestQuote = computed(
  () =>
    !!selectedUnitId.value &&
    !!checkInDate.value &&
    !!checkOutDate.value &&
    guestCount.value >= 1 &&
    vehicleCount.value >= 0
);

const canSubmitBooking = computed(
  () => !bookingSubmitting.value && !bookingQuoteLoading.value && bookingQuote.value?.available === true
);

const bookingSummary = computed(() => {
  if (!checkInDate.value || !checkOutDate.value) {
    return "先選入住與離營日期，我們就能更快協助你確認空位。";
  }
  if (bookingQuoteLoading.value) {
    return "正在確認這段日期的可售名額與價格...";
  }
  if (bookingQuote.value?.message) {
    return bookingQuote.value.message;
  }
  return `預計 ${checkInDate.value} 入住，${checkOutDate.value} 離營，${guestCount.value} 位旅伴同行。`;
});

const bookingConfidenceItems = [
  {
    title: "送出後人工確認",
    description: "預約申請送出後，我們會依空位、日期與分區安排進行人工確認，再正式回覆你。",
  },
  {
    title: "建議留可聯絡電話",
    description: "若日期熱門或需要調整方案，營區會優先透過電話或訊息與你聯繫。",
  },
  {
    title: "確認後再安排行程",
    description: "在收到營區回覆前，建議先保留彈性，不要太早鎖死交通與同行安排。",
  },
];

const resetBookingQuote = () => {
  bookingQuote.value = null;
};

const loadBookingQuote = async () => {
  if (!canRequestQuote.value) {
    resetBookingQuote();
    return;
  }

  bookingQuoteLoading.value = true;
  bookingError.value = "";
  try {
    const response = await getBookingQuoteService({
      campUnitId: selectedUnitId.value as number,
      checkInDate: checkInDate.value,
      checkOutDate: checkOutDate.value,
      guestCount: guestCount.value,
      vehicleCount: vehicleCount.value,
      rentalItems: selectedRentalItems.value,
    });
    bookingQuote.value = response.data;
  } catch (err: any) {
    resetBookingQuote();
    bookingError.value = err?.message || "目前無法確認可售狀態，請稍後再試。";
  } finally {
    bookingQuoteLoading.value = false;
  }
};

const submitBooking = async () => {
  bookingError.value = "";
  bookingSuccess.value = "";

  if (!selectedUnitId.value || !checkInDate.value || !checkOutDate.value) {
    bookingError.value = "請先完成營位與日期選擇。";
    return;
  }
  if (!contactName.value.trim() || !contactPhone.value.trim()) {
    bookingError.value = "請填寫聯絡姓名與電話。";
    return;
  }
  if (bookingQuote.value?.available !== true) {
    bookingError.value = bookingQuote.value?.message || "請先確認日期與營位是否仍可預約。";
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
      rentalItems: selectedRentalItems.value,
    });
    const bookingSnapshot = {
      bookingId: response.data?.bookingId,
      bookingNo: response.data?.bookingNo,
      status: response.data?.status,
      totalAmount: response.data?.totalAmount ?? bookingQuote.value?.totalAmount ?? 0,
      campUnitId: selectedUnitId.value,
      campUnitName: selectedUnit.value?.label || "",
      checkInDate: checkInDate.value,
      checkOutDate: checkOutDate.value,
      guestCount: guestCount.value,
      vehicleCount: vehicleCount.value,
      contactName: contactName.value,
      contactPhone: contactPhone.value,
      contactEmail: contactEmail.value,
      note: bookingNote.value,
      rentalItems: selectedRentalItems.value,
      bookingNotice: site.value?.bookingNotice || "",
      refundPolicy: site.value?.refundPolicy || "",
      createdAt: new Date().toISOString(),
    };
    sessionStorage.setItem(BOOKING_STORAGE_KEY, JSON.stringify(bookingSnapshot));
    bookingSuccess.value = response.data?.bookingNo
      ? `預約單已送出，訂單編號 ${response.data.bookingNo}。`
      : "預約單已送出，營區會盡快與你確認。";
    resetBookingQuote();
    await router.push({
      path: "/booking/success",
      query: {
        bookingId: String(response.data?.bookingId || ""),
        bookingNo: response.data?.bookingNo || "",
      },
    });
  } catch (err: any) {
    bookingError.value = err?.message || "送出失敗，請稍後再試。";
  } finally {
    bookingSubmitting.value = false;
  }
};

watch([selectedUnitId, checkInDate, checkOutDate, guestCount, vehicleCount, selectedRentalItems], () => {
  bookingSuccess.value = "";
  void loadBookingQuote();
});

onMounted(loadData);
</script>

<template>
  <div class="booking-page">
    <section class="booking-hero">
      <div>
        <span class="section-kicker">BOOKING SYSTEM</span>
        <h1>線上預約</h1>
        <p>把想住的日期、營位和聯絡方式先送進來，後續由營區人工確認空位與細節。</p>
      </div>
      <div class="booking-steps">
        <span class="active">1 選日期</span>
        <span>2 選營位</span>
        <span>3 填資料</span>
        <span>4 人工確認</span>
      </div>
    </section>

    <section v-if="loading" class="status-card">資料載入中...</section>
    <section v-else-if="error" class="status-card error">{{ error }}</section>

    <section v-else class="booking-grid">
      <article class="booking-form-card">
        <div class="section-head">
          <div>
            <span class="section-kicker">FORM</span>
            <h2>填寫預約資料</h2>
          </div>
          <p>{{ site?.bookingNotice || "送出後由營區人工確認，請留意電話或訊息通知。" }}</p>
        </div>

        <div class="booking-form-grid">
          <label>
            <span>營位方案</span>
            <select v-model.number="selectedUnitId">
              <option :value="null">請選擇營位</option>
              <option v-for="option in bookingOptions" :key="option.id" :value="option.id">
                {{ option.label }}
              </option>
            </select>
          </label>
          <label>
            <span>入住日</span>
            <input v-model="checkInDate" type="date" />
          </label>
          <label>
            <span>離營日</span>
            <input v-model="checkOutDate" type="date" />
          </label>
          <label>
            <span>入住人數</span>
            <input v-model.number="guestCount" min="1" type="number" />
          </label>
          <label>
            <span>車輛數</span>
            <input v-model.number="vehicleCount" min="0" type="number" />
          </label>
          <label>
            <span>聯絡姓名</span>
            <input v-model="contactName" type="text" placeholder="請填寫聯絡人姓名" />
          </label>
          <label>
            <span>聯絡電話</span>
            <input v-model="contactPhone" type="text" placeholder="請填寫聯絡電話" />
          </label>
          <label>
            <span>Email</span>
            <input v-model="contactEmail" type="email" placeholder="選填" />
          </label>
        </div>

        <label class="full-width">
          <span>備註</span>
          <textarea v-model="bookingNote" rows="4" placeholder="例如抵達時間、同行需求、想安排的區域"></textarea>
        </label>

        <section v-if="rentalItems.length" class="rental-section">
          <div class="rental-section-head">
            <div>
              <span class="section-kicker">RENTAL ITEMS</span>
              <h3>租借用品</h3>
            </div>
            <p>可先加選帳棚、防水墊或其他用品，實際準備會由營區確認。</p>
          </div>
          <div class="rental-grid">
            <article v-for="item in rentalItems" :key="item.id" class="rental-card">
              <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.name" class="rental-card-image" />
              <div v-else class="rental-card-image rental-card-image--empty">無圖</div>
              <div class="rental-card-body">
                <div>
                  <span class="rental-category">{{ rentalCategoryText(item.category) }}</span>
                  <h4>{{ item.name }}</h4>
                  <p>{{ item.description || "營區提供的租借用品。" }}</p>
                </div>
                <div class="rental-card-foot">
                  <strong>NT$ {{ item.unitPrice.toLocaleString() }}</strong>
                  <div class="rental-stepper">
                    <button type="button" @click="setRentalQuantity(item, (rentalSelections[item.id] || 0) - 1)">-</button>
                    <span>{{ rentalSelections[item.id] || 0 }}</span>
                    <button type="button" @click="setRentalQuantity(item, (rentalSelections[item.id] || 0) + 1)">+</button>
                  </div>
                </div>
                <small v-if="quoteRentalAvailability.get(item.id)">
                  這段日期可租 {{ quoteRentalAvailability.get(item.id)?.availableQuantity }} 件，
                  已租 {{ quoteRentalAvailability.get(item.id)?.reservedQuantity }} 件
                </small>
                <small v-else>總庫存 {{ item.totalQuantity }} 件</small>
              </div>
            </article>
          </div>
        </section>

        <div class="form-actions">
          <button class="primary-cta" type="button" :disabled="!canSubmitBooking" @click="submitBooking">
            {{ bookingSubmitting ? "送出中..." : "送出預約單" }}
          </button>
          <a class="secondary-cta" :href="`tel:${site?.contactPhone || ''}`">
            <i class="bi bi-telephone"></i>
            {{ site?.contactPhone || "電話洽詢" }}
          </a>
        </div>

        <p class="policy-hint">
          送出預約代表你已了解
          <RouterLink to="/booking-guide">預約須知</RouterLink>、
          <RouterLink to="/refund-policy">退款政策</RouterLink>
          與
          <RouterLink to="/privacy-policy">隱私政策</RouterLink>。
        </p>

        <p v-if="bookingError" class="feedback error">{{ bookingError }}</p>
        <p v-if="bookingSuccess" class="feedback success">{{ bookingSuccess }}</p>
      </article>

      <aside class="booking-summary-card">
        <div class="summary-block">
          <span class="section-kicker">SUMMARY</span>
          <h3>預約摘要</h3>
          <p>{{ bookingSummary }}</p>
        </div>

        <div class="quote-card" :class="{ unavailable: bookingQuote && !bookingQuote.available }">
          <h4>日期與價格確認</h4>
          <p v-if="bookingQuoteLoading">正在確認日期與價格...</p>
          <template v-else-if="bookingQuote">
            <p>{{ bookingQuote.message }}</p>
            <div class="quote-amount-grid">
              <span>營位小計</span>
              <strong>NT$ {{ (bookingQuote.siteAmount ?? 0).toLocaleString() }}</strong>
              <span>租借小計</span>
              <strong>NT$ {{ (bookingQuote.rentalAmount ?? 0).toLocaleString() }}</strong>
            </div>
            <strong class="quote-total">預估總額 NT$ {{ bookingQuote.totalAmount.toLocaleString() }}</strong>
            <ul class="quote-night-list">
              <li v-for="night in bookingQuote.nightsDetail" :key="night.date">
                <span>{{ night.date }}</span>
                <span>NT$ {{ night.nightlyPrice.toLocaleString() }}</span>
                <span>{{ night.closed ? "已關閉" : `剩餘 ${night.availableQuantity} 組` }}</span>
              </li>
            </ul>
            <ul v-if="bookingQuote.rentalItems?.length" class="quote-rental-list">
              <li v-for="item in bookingQuote.rentalItems" :key="item.rentalItemId">
                <span>
                  {{ item.name }} x {{ item.quantity }}
                  <small>這段日期可租 {{ item.availableQuantity }} 件</small>
                </span>
                <strong>NT$ {{ item.lineTotal.toLocaleString() }}</strong>
              </li>
            </ul>
          </template>
          <p v-else>選好營位、日期與人數後，這裡會先幫你確認可售名額。</p>
        </div>

        <div class="selected-plan" v-if="selectedUnit">
          <div class="selected-plan-top">
            <strong>{{ selectedUnit.label }}</strong>
            <span>{{ selectedUnit.quantity }} 位</span>
          </div>
          <p>{{ selectedUnit.description || "適合想安排舒適山林住宿節奏的旅伴。" }}</p>
          <div class="plan-tags">
            <span><i class="bi bi-people"></i> {{ selectedUnit.maxGuests }} 人</span>
            <span>{{ selectedUnit.hasShelter ? "雨棚區" : "草地區" }}</span>
            <span>{{ selectedUnit.electricityAvailable ? "供電" : "自然區" }}</span>
          </div>
        </div>

        <div class="summary-note">
          <h4>入住資訊</h4>
          <p>{{ site?.checkInTime || "15:00" }} 入住 / {{ site?.checkOutTime || "11:00" }} 離營</p>
          <p v-if="selectedRentalCount">已加選 {{ selectedRentalCount }} 件租借用品</p>
        </div>

        <div class="summary-note">
          <h4>退款與改期</h4>
          <p>{{ site?.refundPolicy || "實際退款與改期規則，請以營區人工回覆為準。" }}</p>
        </div>

        <div class="summary-note">
          <h4>聯絡方式</h4>
          <p>{{ site?.contactPhone || "0900 000 000" }}</p>
          <p>{{ site?.contactEmail || "camp@example.com" }}</p>
        </div>

        <div class="summary-note trust-note">
          <h4>預約小提醒</h4>
          <div class="confidence-list">
            <article v-for="item in bookingConfidenceItems" :key="item.title" class="confidence-item">
              <strong>{{ item.title }}</strong>
              <p>{{ item.description }}</p>
            </article>
          </div>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.booking-page {
  width: min(1520px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  gap: 26px;
  padding-bottom: 28px;
}

.booking-hero {
  display: flex;
  justify-content: space-between;
  gap: 28px;
  align-items: end;
  padding: 32px;
  border-radius: 36px;
  background:
    linear-gradient(90deg, rgba(244, 239, 231, 0.96) 0%, rgba(244, 239, 231, 0.78) 42%, rgba(42, 53, 38, 0.14) 100%),
    url("https://images.unsplash.com/photo-1502082553048-f009c37129b9?auto=format&fit=crop&w=1800&q=80")
      center / cover no-repeat;
  border: 1px solid rgba(120, 111, 92, 0.12);
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

.booking-hero h1,
.section-head h2 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.booking-hero h1 {
  font-size: clamp(2.8rem, 4.4vw, 4.2rem);
}

.booking-hero p,
.section-head p,
.summary-block p,
.selected-plan p,
.summary-note p {
  color: #61685d;
  line-height: 1.8;
}

.booking-steps {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.booking-steps span {
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 252, 247, 0.72);
  color: #667046;
  font-size: 0.9rem;
}

.booking-steps .active {
  background: #6f7540;
  color: #fffdf7;
}

.status-card,
.booking-form-card,
.booking-summary-card {
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(120, 111, 92, 0.1);
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.status-card {
  padding: 20px 24px;
}

.status-card.error {
  color: #a44f39;
}

.booking-grid {
  display: grid;
  grid-template-columns: 1.06fr 0.94fr;
  gap: 24px;
}

.booking-form-card,
.booking-summary-card {
  padding: 28px;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
}

.section-head p {
  max-width: 430px;
  margin: 0;
}

.booking-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.booking-form-grid label,
.full-width {
  display: grid;
  gap: 8px;
  color: #485040;
  font-size: 0.92rem;
}

.booking-form-grid input,
.booking-form-grid select,
.full-width textarea {
  width: 100%;
  min-height: 46px;
  padding: 12px 14px;
  border: 1px solid rgba(126, 119, 97, 0.18);
  border-radius: 14px;
  background: #fffdf9;
  color: #20241d;
}

.full-width {
  margin-top: 14px;
}

.full-width textarea {
  min-height: 124px;
  resize: vertical;
}

.rental-section {
  margin-top: 22px;
  padding-top: 22px;
  border-top: 1px solid rgba(124, 116, 95, 0.12);
}

.rental-section-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: end;
  margin-bottom: 16px;
}

.rental-section-head h3 {
  margin: 8px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.rental-section-head p {
  max-width: 360px;
  margin: 0;
  color: #61685d;
  line-height: 1.7;
}

.rental-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.rental-card {
  display: grid;
  grid-template-columns: 124px minmax(0, 1fr);
  min-height: 156px;
  overflow: hidden;
  border: 1px solid rgba(124, 116, 95, 0.12);
  border-radius: 20px;
  background: #fbf8f1;
}

.rental-card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #f1ede2;
}

.rental-card-image--empty {
  display: grid;
  place-items: center;
  color: #8e927f;
  font-size: 0.86rem;
}

.rental-card-body {
  display: grid;
  gap: 12px;
  padding: 14px;
}

.rental-category {
  color: #8e927f;
  font-size: 0.78rem;
}

.rental-card h4 {
  margin: 4px 0 6px;
  font-size: 1rem;
}

.rental-card p {
  margin: 0;
  color: #61685d;
  line-height: 1.6;
  font-size: 0.88rem;
}

.rental-card-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.rental-stepper {
  display: grid;
  grid-template-columns: 34px 48px 34px;
  min-width: 116px;
  border: 1px solid rgba(126, 119, 97, 0.18);
  border-radius: 999px;
  overflow: hidden;
  background: #fffdf9;
}

.rental-stepper button,
.rental-stepper span {
  width: 100%;
  min-height: 34px;
  border: 0;
  background: transparent;
  text-align: center;
}

.rental-stepper span {
  display: grid;
  place-items: center;
  color: #20241d;
}

.rental-stepper button {
  color: #6f7540;
  font-weight: 700;
}

.rental-card small {
  color: #8e927f;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.primary-cta,
.secondary-cta {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 24px;
  border-radius: 999px;
  text-decoration: none;
  font-weight: 600;
}

.primary-cta {
  border: 0;
  background: #6f7540;
  color: #fffdf7;
}

.secondary-cta {
  border: 1px solid rgba(99, 102, 84, 0.12);
  color: #2f362b;
  background: rgba(255, 252, 247, 0.72);
}

.policy-hint {
  margin: 12px 0 0;
  color: #6b705f;
  font-size: 0.92rem;
  line-height: 1.7;
}

.policy-hint a {
  color: #5f6737;
  font-weight: 700;
  text-decoration: none;
}

.policy-hint a:hover,
.policy-hint a:focus-visible {
  text-decoration: underline;
}

.feedback {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
}

.feedback.error {
  background: rgba(186, 79, 58, 0.08);
  color: #a44f39;
}

.feedback.success {
  background: rgba(98, 121, 66, 0.1);
  color: #576a33;
}

.summary-block h3,
.summary-note h4 {
  margin: 10px 0 0;
}

.selected-plan,
.summary-note,
.quote-card {
  margin-top: 18px;
  padding: 20px;
  border-radius: 20px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
}

.quote-card.unavailable {
  background: rgba(186, 79, 58, 0.06);
  border-color: rgba(186, 79, 58, 0.22);
}

.quote-card h4 {
  margin: 0 0 10px;
}

.quote-card p {
  margin: 0;
}

.quote-total {
  display: block;
  margin-top: 12px;
  color: #2f362b;
}

.quote-amount-grid {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(124, 116, 95, 0.12);
  color: #596049;
}

.quote-night-list {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.quote-rental-list {
  display: grid;
  gap: 10px;
  margin: 14px 0 0;
  padding: 14px 0 0;
  border-top: 1px solid rgba(124, 116, 95, 0.12);
  list-style: none;
}

.quote-rental-list li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #596049;
  font-size: 0.9rem;
}

.quote-night-list li {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr 0.9fr;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid rgba(124, 116, 95, 0.12);
  color: #596049;
  font-size: 0.88rem;
}

.trust-note {
  background: linear-gradient(180deg, #fbf8f1 0%, #f8f4ea 100%);
}

.confidence-list {
  display: grid;
  gap: 12px;
}

.confidence-item {
  padding: 14px 0;
  border-top: 1px solid rgba(124, 116, 95, 0.12);
}

.confidence-item:first-child {
  padding-top: 0;
  border-top: 0;
}

.confidence-item strong {
  display: block;
  margin-bottom: 6px;
  color: #2f362b;
}

.confidence-item p {
  margin: 0;
}

.selected-plan-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.selected-plan-top span {
  color: #7d816d;
  font-size: 0.9rem;
}

.plan-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-tags span {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f1ede2;
  color: #596049;
  font-size: 0.82rem;
}

@media (max-width: 1024px) {
  .booking-hero,
  .booking-grid,
  .section-head {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: start;
  }
}

@media (max-width: 960px) {
  .booking-page {
    width: min(100%, calc(100% - 24px));
  }

  .booking-form-grid {
    grid-template-columns: 1fr;
  }

  .rental-section-head {
    flex-direction: column;
    align-items: start;
  }

  .rental-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 575px) {
  .rental-card {
    grid-template-columns: 1fr;
  }

  .rental-card-image {
    height: 180px;
  }
}
</style>
