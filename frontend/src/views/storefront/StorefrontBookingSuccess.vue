<script setup lang="ts">
import { formatStayTime } from "@/utils/bookingOperations";
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  createPublicPaymentService,
  getPublicBookingSummaryService,
  getPublicPaymentMethodsService,
  type PublicBookingLookupResponse,
  type PublicPaymentMethodConfig,
  type PublicPaymentCreateResponse,
} from "@/api/camp";
import { confirmAdminAction } from "@/utils/confirmAction";

type BookingSnapshot = {
  bookingId?: number;
  bookingNo?: string;
  status?: string;
  paymentDueAt?: string | null;
  expiredAt?: string | null;
  paidAmount?: number;
  refundedAmount?: number;
  totalAmount?: number;
  campUnitName?: string;
  checkInDate?: string;
  checkOutDate?: string;
  guestCount?: number;
  vehicleCount?: number;
  contactName?: string;
  contactPhone?: string;
  contactEmail?: string;
  note?: string;
  bookingNotice?: string;
  refundPolicy?: string;
  createdAt?: string;
};

const route = useRoute();
const router = useRouter();
const BOOKING_STORAGE_KEY = "storefront-last-booking";
const PAYMENT_STORAGE_KEY = "storefront-last-payment";
const paymentLoading = ref(false);
const activeProvider = ref("");
const paymentError = ref("");
const paymentMethods = ref<PublicPaymentMethodConfig[]>([]);
const paymentMethodsLoading = ref(false);
const paymentMethodsError = ref("");
const summaryLoading = ref(false);
const summaryError = ref("");

const readBookingSnapshot = (): BookingSnapshot | null => {
  try {
    const raw = sessionStorage.getItem(BOOKING_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const bookingSnapshot = ref<BookingSnapshot | null>(readBookingSnapshot());

const bookingId = computed(() => {
  const fromQuery = Number(route.query.bookingId || bookingSnapshot.value?.bookingId || 0);
  return Number.isFinite(fromQuery) && fromQuery > 0 ? fromQuery : null;
});

const bookingNo = computed(() => {
  return String(route.query.bookingNo || bookingSnapshot.value?.bookingNo || "");
});

const bookingAmount = computed(() => Number(bookingSnapshot.value?.totalAmount || 0));

const toBookingSnapshot = (summary: PublicBookingLookupResponse): BookingSnapshot => ({
  bookingId: summary.bookingId,
  bookingNo: summary.bookingNo,
  status: summary.status,
  paymentDueAt: summary.paymentDueAt,
  expiredAt: summary.expiredAt,
  paidAmount: summary.paidAmount,
  refundedAmount: summary.refundedAmount,
  totalAmount: summary.totalAmount,
  campUnitName: summary.items?.[0]?.itemName || "",
  checkInDate: summary.checkInDate,
  checkOutDate: summary.checkOutDate,
  guestCount: summary.guestCount,
  vehicleCount: summary.vehicleCount,
  contactName: summary.contactName,
  contactPhone: summary.maskedContactPhone,
});

const loadBookingSummary = async () => {
  const queryBookingId = Number(route.query.bookingId || 0);
  const queryBookingNo = String(route.query.bookingNo || "");
  if (!queryBookingId || !queryBookingNo) {
    return;
  }

  summaryLoading.value = true;
  summaryError.value = "";
  try {
    const response = await getPublicBookingSummaryService({
      bookingId: queryBookingId,
      bookingNo: queryBookingNo,
    });
    const nextSnapshot = toBookingSnapshot(response.data);
    bookingSnapshot.value = {
      ...(bookingSnapshot.value || {}),
      ...nextSnapshot,
    };
    sessionStorage.setItem(BOOKING_STORAGE_KEY, JSON.stringify(bookingSnapshot.value));
  } catch (err: any) {
    summaryError.value = err?.message || "訂單摘要載入失敗，請改用查詢預約頁確認。";
  } finally {
    summaryLoading.value = false;
  }
};

const submitPaymentForm = (payment: PublicPaymentCreateResponse) => {
  const form = document.createElement("form");
  form.method = "POST";
  form.action = payment.paymentUrl || "";
  form.style.display = "none";
  Object.entries(payment.formFields || {}).forEach(([name, value]) => {
    const input = document.createElement("input");
    input.type = "hidden";
    input.name = name;
    input.value = value;
    form.appendChild(input);
  });
  document.body.appendChild(form);
  form.submit();
};

const loadPaymentMethods = async () => {
  paymentMethodsLoading.value = true;
  paymentMethodsError.value = "";
  try {
    const response = await getPublicPaymentMethodsService();
    paymentMethods.value = response.data || [];
  } catch (err: any) {
    paymentMethodsError.value = err?.message || "付款方式載入失敗，請稍後再試。";
  } finally {
    paymentMethodsLoading.value = false;
  }
};

const redirectToPayment = async (payment: PublicPaymentCreateResponse) => {
  if (!payment.paymentUrl) {
    await router.push("/payment/cancel");
    return;
  }

  if (/^https?:\/\//i.test(payment.paymentUrl) || /^line:\/\//i.test(payment.paymentUrl)) {
    window.location.href = payment.paymentUrl;
    return;
  }

  await router.push({
    path: payment.paymentUrl,
    query: {
      paymentRecordId: String(payment.paymentRecordId),
      bookingId: String(bookingId.value),
      bookingNo: bookingNo.value,
      amount: String(payment.amount),
    },
  });
};

const providerFeatures = (method: PublicPaymentMethodConfig) => {
  if (method.provider === "ecpay") {
    return ["建立綠界付款請求", "導向綠界付款頁", "付款完成後同步後台付款紀錄"];
  }
  return ["建立付款請求", "導向付款頁", "付款確認後同步回寫訂單已收金額"];
};

const createPayment = async (method: PublicPaymentMethodConfig) => {
  if (!bookingId.value) {
    paymentError.value = "目前找不到可付款的訂單編號，請回到預約頁重新送單。";
    return;
  }
  if (
    !(await confirmAdminAction({
      title: "確認建立付款請求",
      message: `確定要使用 ${method.displayName} 建立付款請求嗎？`,
      confirmButtonText: "前往付款",
    }))
  ) {
    return;
  }

  paymentLoading.value = true;
  activeProvider.value = method.provider;
  paymentError.value = "";
  try {
    const response = await createPublicPaymentService({
      bookingId: bookingId.value,
      provider: method.provider,
      paymentMethod: method.paymentMethod,
    });
    const payment = response.data as PublicPaymentCreateResponse;
    sessionStorage.setItem(
      PAYMENT_STORAGE_KEY,
      JSON.stringify({
        ...payment,
        bookingId: bookingId.value,
        bookingNo: bookingNo.value,
        amount: payment.amount,
      }),
    );
    if (payment.redirectMethod === "post" && payment.paymentUrl) {
      submitPaymentForm(payment);
      return;
    }
    await redirectToPayment(payment);
  } catch (err: any) {
    paymentError.value = err?.message || "目前無法建立付款請求，請稍後再試。";
  } finally {
    paymentLoading.value = false;
    activeProvider.value = "";
  }
};

onMounted(() => {
  void loadBookingSummary();
  void loadPaymentMethods();
});
</script>

<template>
  <div v-if="bookingSnapshot?.paymentDueAt" class="container mt-3" role="status">
    <p v-if="bookingSnapshot.expiredAt" class="text-danger">未於付款期限內完成付款，預約已取消。請重新查詢日期。</p>
    <p v-else-if="bookingSnapshot.status === 'pending' && !bookingSnapshot.paidAmount">付款期限（台北時間）：{{ formatStayTime(bookingSnapshot.paymentDueAt) }}</p>
  </div>
  <div class="booking-success-page">
    <section class="hero-card">
      <div>
        <span class="section-kicker">BOOKING RECEIVED</span>
        <h1>預約單已送出</h1>
        <p>
          我們已收到你的入住需求，接下來可以先保留訂單編號，或直接往下一步建立付款請求。
        </p>
      </div>
      <div class="status-pill">
        <strong>{{ bookingNo || "待產生編號" }}</strong>
        <span>目前狀態 {{ bookingSnapshot?.status || "pending" }}</span>
      </div>
    </section>

    <section class="content-grid">
      <article class="summary-card">
        <div class="section-head">
          <div>
            <span class="section-kicker">SUMMARY</span>
            <h2>訂單摘要</h2>
          </div>
          <RouterLink class="text-link" to="/booking">回預約頁</RouterLink>
        </div>

        <p v-if="summaryLoading" class="summary-feedback">正在載入訂單摘要...</p>
        <p v-else-if="summaryError" class="summary-feedback error">{{ summaryError }}</p>

        <dl class="summary-list">
          <div>
            <dt>訂單編號</dt>
            <dd>{{ bookingNo || "請回前頁重新送出" }}</dd>
          </div>
          <div>
            <dt>營位方案</dt>
            <dd>{{ bookingSnapshot?.campUnitName || "未提供" }}</dd>
          </div>
          <div>
            <dt>入住日期</dt>
            <dd>{{ bookingSnapshot?.checkInDate || "-" }} 到 {{ bookingSnapshot?.checkOutDate || "-" }}</dd>
          </div>
          <div>
            <dt>人數 / 車輛</dt>
            <dd>{{ bookingSnapshot?.guestCount || 0 }} 人 / {{ bookingSnapshot?.vehicleCount || 0 }} 車</dd>
          </div>
          <div>
            <dt>聯絡人</dt>
            <dd>{{ bookingSnapshot?.contactName || "-" }} / {{ bookingSnapshot?.contactPhone || "-" }}</dd>
          </div>
          <div>
            <dt>預估金額</dt>
            <dd>NT$ {{ bookingAmount.toLocaleString() }}</dd>
          </div>
        </dl>

        <RouterLink class="lookup-link" to="/booking/lookup">之後可用訂單編號與聯絡電話再次查詢</RouterLink>

        <div class="note-block">
          <h3>後續說明</h3>
          <p>{{ bookingSnapshot?.bookingNotice || "預約送出後，營區會再人工確認與聯繫。" }}</p>
          <p>{{ bookingSnapshot?.refundPolicy || "付款與退款細節，會依實際聯繫結果安排。" }}</p>
        </div>
      </article>

      <aside class="payment-card">
        <div class="section-head">
          <div>
            <span class="section-kicker">PAYMENT</span>
            <h2>下一步付款</h2>
          </div>
        </div>

        <p v-if="paymentMethodsLoading" class="summary-feedback">付款方式載入中...</p>
        <p v-else-if="paymentMethodsError" class="summary-feedback error">{{ paymentMethodsError }}</p>
        <p v-else-if="paymentMethods.length === 0" class="summary-feedback">
          目前尚未開放線上支付方式，請先保留訂單編號並等待營區聯繫。
        </p>

        <template v-else>
          <div v-for="(method, index) in paymentMethods" :key="method.provider" class="provider-block">
            <div class="provider-card">
              <strong>{{ method.displayName }}</strong>
              <p>{{ method.description || "營區提供的線上付款方式。" }}</p>
              <ul>
                <li v-for="feature in providerFeatures(method)" :key="feature">{{ feature }}</li>
              </ul>
            </div>

            <button
              :class="index === 0 ? 'primary-cta' : 'secondary-payment-cta'"
              type="button"
              :disabled="paymentLoading || summaryLoading || !bookingId || Boolean(bookingSnapshot?.refundedAmount) || bookingSnapshot?.status === 'cancelled' || (bookingSnapshot?.paidAmount || 0) >= bookingAmount"
              @click="createPayment(method)"
            >
              {{ paymentLoading && activeProvider === method.provider ? "建立付款中..." : `使用 ${method.displayName} 付款` }}
            </button>
            <p v-if="index === 0" class="payment-hint">
              請於上方預約付款期限內完成付款；重新建立付款連結不會延長預約期限。逾時取消後請重新查詢日期。
            </p>
          </div>
        </template>

        <p v-if="paymentError" class="feedback error">{{ paymentError }}</p>

        <div class="helper-card">
          <h3>現在這版會怎麼跑</h3>
          <p>如果金流正式憑證尚未設定，系統會走 mock 付款頁，方便你先確認前台與後台資料流。</p>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.booking-success-page {
  width: min(1360px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  gap: 24px;
  padding-bottom: 32px;
}

.hero-card,
.summary-card,
.payment-card {
  border-radius: 28px;
  border: 1px solid rgba(120, 111, 92, 0.1);
  background: rgba(255, 252, 247, 0.92);
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 32px;
  background:
    linear-gradient(90deg, rgba(244, 239, 231, 0.96) 0%, rgba(244, 239, 231, 0.82) 45%, rgba(42, 53, 38, 0.12) 100%),
    url("https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1800&q=80")
      center / cover no-repeat;
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

.hero-card h1,
.section-head h2,
.note-block h3,
.helper-card h3 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

.hero-card h1 {
  font-size: clamp(2.4rem, 4vw, 3.6rem);
}

.hero-card p,
.provider-card p,
.note-block p,
.helper-card p {
  color: #61685d;
  line-height: 1.8;
}

.status-pill {
  min-width: 280px;
  align-self: flex-start;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 252, 247, 0.82);
  display: grid;
  gap: 6px;
}

.status-pill strong {
  color: #2f362b;
  font-size: 1.05rem;
}

.status-pill span {
  color: #667046;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 24px;
}

.summary-card,
.payment-card {
  padding: 28px;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 20px;
}

.text-link {
  color: #667046;
  text-decoration: none;
}

.lookup-link {
  display: inline-flex;
  margin-top: 18px;
  color: #667046;
  text-decoration: none;
}

.summary-feedback {
  margin: 16px 0 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(111, 117, 64, 0.08);
  color: #596338;
}

.summary-feedback.error {
  background: rgba(186, 79, 58, 0.08);
  color: #a44f39;
}

.summary-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 22px 0 0;
}

.summary-list div,
.provider-card,
.note-block,
.helper-card {
  padding: 18px;
  border-radius: 20px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
}

.summary-list dt {
  color: #7d816d;
  font-size: 0.88rem;
}

.summary-list dd {
  margin: 8px 0 0;
  color: #2f362b;
  font-weight: 600;
}

.note-block,
.provider-card,
.helper-card {
  margin-top: 18px;
}

.provider-block:first-of-type .provider-card {
  margin-top: 0;
}

.provider-card ul {
  margin: 14px 0 0;
  padding-left: 18px;
  color: #596049;
  display: grid;
  gap: 8px;
}

.primary-cta {
  width: 100%;
  min-height: 50px;
  margin-top: 18px;
  border: 0;
  border-radius: 999px;
  background: #6f7540;
  color: #fffdf7;
  font-weight: 600;
}

.secondary-payment-cta {
  width: 100%;
  min-height: 50px;
  margin-top: 14px;
  border: 1px solid rgba(99, 102, 84, 0.2);
  border-radius: 999px;
  background: #fffdf9;
  color: #3e4932;
  font-weight: 600;
}

.primary-cta:disabled,
.secondary-payment-cta:disabled {
  opacity: 0.66;
}

.feedback.error {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(186, 79, 58, 0.08);
  color: #a44f39;
}

.payment-hint {
  margin: 10px 2px 0;
  color: #7d816d;
  font-size: 0.88rem;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .booking-success-page {
    width: min(100%, calc(100% - 24px));
  }

  .hero-card,
  .content-grid,
  .section-head {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: start;
  }

  .summary-list {
    grid-template-columns: 1fr;
  }

  .status-pill {
    min-width: 0;
    width: 100%;
  }
}
</style>
