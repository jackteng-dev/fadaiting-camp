<script setup lang="ts">
import { formatStayTime } from "@/utils/bookingOperations";
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import {
  lookupPublicBookingService,
  retryPublicPaymentService,
  type PublicBookingLookupResponse,
  type PublicPaymentCreateResponse,
} from "@/api/camp";

const router = useRouter();
const BOOKING_STORAGE_KEY = "storefront-last-booking";
const bookingNo = ref("");
const contactPhone = ref("");
const loading = ref(false);
const paymentLoading = ref(false);
const activeProvider = ref("");
const error = ref("");
const paymentError = ref("");
const result = ref<PublicBookingLookupResponse | null>(null);

const statusLabels: Record<string, string> = {
  pending: "等待營區確認",
  confirmed: "預約已確認",
  cancelled: "預約已取消",
  completed: "行程已完成",
};

const paymentStatusLabels: Record<string, string> = {
  paid_review: "收款待人工核對",
  pending: "等待付款",
  paid: "付款完成",
  failed: "付款失敗",
  cancelled: "付款取消",
  expired: "付款逾時",
  refunded: "已退款",
};

const outstandingAmount = computed(() => {
  if (!result.value) return 0;
  return Math.max(Number(result.value.totalAmount || 0) - Number(result.value.paidAmount || 0), 0);
});

const canPay = computed(() => {
  return Boolean(result.value && result.value.status !== "cancelled" && !result.value.refundedAmount && outstandingAmount.value > 0);
});

const formatMoney = (value?: number) => Number(value || 0).toLocaleString();

const formatDateTime = (value?: string) => {
  if (!value) return "-";
  return new Date(value).toLocaleString("zh-TW", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const lookupBooking = async () => {
  if (!bookingNo.value.trim() || !contactPhone.value.trim()) {
    error.value = "請填寫訂單編號與聯絡電話。";
    return;
  }

  loading.value = true;
  error.value = "";
  paymentError.value = "";
  result.value = null;
  try {
    const response = await lookupPublicBookingService({
      bookingNo: bookingNo.value.trim(),
      contactPhone: contactPhone.value.trim(),
    });
    result.value = response.data;
  } catch (err: any) {
    error.value = err?.message || "目前無法查詢預約，請稍後再試。";
  } finally {
    loading.value = false;
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
      bookingId: String(result.value?.bookingId || ""),
      bookingNo: result.value?.bookingNo || "",
      amount: String(payment.amount),
    },
  });
};

const writeBookingSnapshot = () => {
  if (!result.value) return;
  sessionStorage.setItem(
    BOOKING_STORAGE_KEY,
    JSON.stringify({
      bookingId: result.value.bookingId,
      bookingNo: result.value.bookingNo,
      status: result.value.status,
      totalAmount: result.value.totalAmount,
      campUnitName: result.value.items[0]?.itemName || "",
      checkInDate: result.value.checkInDate,
      checkOutDate: result.value.checkOutDate,
      guestCount: result.value.guestCount,
      vehicleCount: result.value.vehicleCount,
      contactName: result.value.contactName,
      contactPhone: result.value.maskedContactPhone,
    }),
  );
};

const retryPayment = async (provider: "linepay" | "ecpay") => {
  if (!result.value) return;
  paymentLoading.value = true;
  activeProvider.value = provider;
  paymentError.value = "";
  try {
    writeBookingSnapshot();
    const response = await retryPublicPaymentService({
      bookingNo: result.value.bookingNo,
      contactPhone: contactPhone.value.trim(),
      provider,
      paymentMethod: provider === "linepay" ? "web" : "all",
    });
    const payment = response.data;
    sessionStorage.setItem(
      "storefront-last-payment",
      JSON.stringify({
        ...payment,
        bookingId: result.value.bookingId,
        bookingNo: result.value.bookingNo,
        amount: payment.amount,
      }),
    );
    if (payment.redirectMethod === "post" && payment.paymentUrl) {
      submitPaymentForm(payment);
      return;
    }
    await redirectToPayment(payment);
  } catch (err: any) {
    paymentError.value = err?.message || "目前無法重新建立付款，請稍後再試。";
  } finally {
    paymentLoading.value = false;
    activeProvider.value = "";
  }
};

const continuePayment = async () => {
  if (!result.value) return;
  writeBookingSnapshot();
  await router.push({
    path: "/booking/success",
    query: {
      bookingId: String(result.value.bookingId),
      bookingNo: result.value.bookingNo,
    },
  });
};
</script>

<template>
  <div class="lookup-page">
    <section class="lookup-hero">
      <div>
        <span class="section-kicker">BOOKING LOOKUP</span>
        <h1>查詢預約</h1>
        <p>輸入訂單編號與預約時留下的聯絡電話，查看目前預約狀態與付款進度。</p>
      </div>
      <form class="lookup-form" @submit.prevent="lookupBooking">
        <label>
          <span>訂單編號</span>
          <input v-model="bookingNo" autocomplete="off" placeholder="例如 BK20260531XXXXXX" type="text" />
        </label>
        <label>
          <span>聯絡電話</span>
          <input v-model="contactPhone" autocomplete="tel" inputmode="tel" placeholder="預約時填寫的電話" type="tel" />
        </label>
        <button type="submit" :disabled="loading">
          <i class="bi bi-search"></i>
          {{ loading ? "查詢中..." : "查詢預約" }}
        </button>
        <p v-if="error" class="feedback error">{{ error }}</p>
      </form>
    </section>

    <section v-if="result" class="lookup-result">
      <div class="result-heading">
        <div>
          <span class="section-kicker">BOOKING STATUS</span>
          <h2>{{ result.bookingNo }}</h2>
          <p>{{ statusLabels[result.status] || result.status }}</p>
          <p v-if="result.status === 'pending' && result.paymentDueAt && !result.paidAmount">付款期限（台北時間）：{{ formatStayTime(result.paymentDueAt) }}</p>
          <p v-if="result.refundedAmount">這筆訂單已有退款，餘額請洽營區確認。</p>
          <p v-if="result.expiredAt">未於付款期限內完成付款，預約已取消。請重新查詢日期。</p>
        </div>
        <button v-if="canPay" class="payment-action" type="button" @click="continuePayment">
          <i class="bi bi-receipt"></i>
          查看付款頁
        </button>
      </div>

      <dl class="summary-grid">
        <div>
          <dt>聯絡人</dt>
          <dd>{{ result.contactName }} / {{ result.maskedContactPhone }}</dd>
        </div>
        <div>
          <dt>入住日期</dt>
          <dd>{{ result.checkInDate }} 到 {{ result.checkOutDate }}</dd>
        </div>
        <div>
          <dt>營位方案</dt>
          <dd>{{ result.items.map((item) => item.itemName).join("、") || "-" }}</dd>
        </div>
        <div>
          <dt>人數 / 車輛</dt>
          <dd>{{ result.guestCount }} 人 / {{ result.vehicleCount }} 車</dd>
        </div>
        <div>
          <dt>訂單總額</dt>
          <dd>NT$ {{ formatMoney(result.totalAmount) }}</dd>
        </div>
        <div>
          <dt>尚待付款</dt>
          <dd>{{ result.refundedAmount ? "退款後餘額請洽營區核對" : `NT$ ${formatMoney(outstandingAmount)}` }}</dd>
        </div>
      </dl>

      <div class="detail-grid">
        <section class="detail-section">
          <div class="section-heading">
            <span class="section-kicker">PAYMENTS</span>
            <h3>付款紀錄</h3>
          </div>
          <p v-if="result.payments.length === 0" class="empty-state">目前尚未建立付款紀錄。</p>
          <div v-else class="record-list">
            <article v-for="(payment, index) in result.payments" :key="`${payment.provider}-${index}`">
              <div>
                <strong>{{ payment.provider.toUpperCase() }}</strong>
                <span>{{ paymentStatusLabels[payment.status] || payment.status }}<span v-if="payment.refundedAmount"> · 已實退 NT$ {{ formatMoney(payment.refundedAmount) }}</span></span>
              </div>
              <b>NT$ {{ formatMoney(payment.amount) }}</b>
            </article>
          </div>
          <div v-if="canPay" class="retry-actions">
            <button type="button" :disabled="paymentLoading" @click="retryPayment('linepay')">
              <i class="bi bi-credit-card"></i>
              {{ paymentLoading && activeProvider === "linepay" ? "建立中..." : "LINE Pay 重新付款" }}
            </button>
            <button type="button" :disabled="paymentLoading" @click="retryPayment('ecpay')">
              <i class="bi bi-bank"></i>
              {{ paymentLoading && activeProvider === "ecpay" ? "建立中..." : "綠界重新付款" }}
            </button>
          </div>
          <p v-if="canPay" class="payment-hint">付款連結建立後將保留 30 分鐘，若逾時未完成，可再次重新建立。</p>
          <p v-if="paymentError" class="feedback error">{{ paymentError }}</p>
        </section>

        <section class="detail-section">
          <div class="section-heading">
            <span class="section-kicker">TIMELINE</span>
            <h3>處理進度</h3>
          </div>
          <p v-if="result.timeline.length === 0" class="empty-state">目前尚無處理紀錄。</p>
          <div v-else class="timeline-list">
            <article v-for="(entry, index) in result.timeline" :key="`${entry.actionType}-${index}`">
              <span class="timeline-dot"></span>
              <div>
                <strong>{{ entry.actionLabel }}</strong>
                <small>{{ formatDateTime(entry.createdTime) }}</small>
                <p v-if="entry.detail">{{ entry.detail }}</p>
              </div>
            </article>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<style scoped>
.lookup-page {
  width: min(1280px, calc(100% - 32px));
  margin: 0 auto;
  display: grid;
  gap: 24px;
  padding-bottom: 28px;
}

.lookup-hero,
.lookup-result {
  padding: 30px;
  border: 1px solid rgba(120, 111, 92, 0.1);
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.92);
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.lookup-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 0.72fr);
  gap: 28px;
  align-items: center;
  background:
    linear-gradient(90deg, rgba(244, 239, 231, 0.96) 0%, rgba(244, 239, 231, 0.82) 48%, rgba(42, 53, 38, 0.12) 100%),
    url("https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?auto=format&fit=crop&w=1800&q=80")
      center / cover no-repeat;
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

h1,
h2,
h3 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
}

h1 {
  font-size: clamp(2.5rem, 4vw, 3.8rem);
}

.lookup-hero p,
.result-heading p,
.timeline-list p,
.empty-state {
  color: #61685d;
  line-height: 1.8;
}

.lookup-form {
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid rgba(120, 111, 92, 0.12);
  border-radius: 20px;
  background: rgba(255, 252, 247, 0.88);
}

label {
  display: grid;
  gap: 7px;
  color: #4f583f;
  font-size: 0.9rem;
}

input {
  min-height: 46px;
  padding: 0 13px;
  border: 1px solid rgba(99, 102, 84, 0.22);
  border-radius: 8px;
  background: #fffdf9;
  color: #2f362b;
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  min-height: 46px;
  border: 0;
  border-radius: 8px;
  background: #6f7540;
  color: #fffdf7;
  font-weight: 600;
}

button:disabled {
  opacity: 0.66;
}

.feedback.error {
  margin: 0;
  color: #a44f39;
}

.payment-hint {
  margin: 12px 0 0;
  color: #7d816d;
  font-size: 0.88rem;
  line-height: 1.7;
}

.result-heading {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
}

.payment-action {
  padding: 0 18px;
}

.retry-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.retry-actions button {
  min-height: 42px;
  padding: 0 16px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin: 24px 0;
}

.summary-grid div {
  padding: 16px;
  border-left: 3px solid #8c9458;
  background: #fbf8f1;
}

dt {
  color: #7d816d;
  font-size: 0.86rem;
}

dd {
  margin: 7px 0 0;
  color: #2f362b;
  font-weight: 600;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.detail-section {
  padding-top: 18px;
  border-top: 1px solid rgba(120, 111, 92, 0.14);
}

.record-list,
.timeline-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.record-list article {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid rgba(120, 111, 92, 0.12);
}

.record-list div,
.timeline-list div {
  display: grid;
  gap: 4px;
}

.record-list span,
.timeline-list small {
  color: #7d816d;
  font-size: 0.84rem;
}

.timeline-list article {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr);
  gap: 10px;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 50%;
  background: #8c9458;
}

.timeline-list p {
  margin: 2px 0 0;
  font-size: 0.9rem;
}

@media (max-width: 900px) {
  .lookup-hero,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .lookup-page {
    width: min(100%, calc(100% - 18px));
  }

  .lookup-hero,
  .lookup-result {
    padding: 20px 16px;
    border-radius: 22px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .result-heading {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
