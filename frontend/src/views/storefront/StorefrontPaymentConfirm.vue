<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { confirmPublicPaymentService, type PublicPaymentConfirmResponse } from "@/api/camp";

const route = useRoute();
const loading = ref(true);
const error = ref("");
const result = ref<PublicPaymentConfirmResponse | null>(null);

const bookingNo = computed(() => String(route.query.bookingNo || ""));
const paymentRecordId = computed(() => Number(route.query.paymentRecordId || 0));
const merchantOrderNo = computed(() => String(route.query.orderId || route.query.merchantOrderNo || ""));
const transactionId = computed(() => String(route.query.transactionId || ""));
const resolvedBookingNo = computed(() => result.value?.bookingNo || bookingNo.value);
const resolvedBookingId = computed(() => result.value?.bookingId || Number(route.query.bookingId || 0));
const bookingResultRoute = computed(() => ({
  path: "/booking/success",
  query: {
    bookingId: resolvedBookingId.value ? String(resolvedBookingId.value) : undefined,
    bookingNo: resolvedBookingNo.value,
  },
}));

const loadConfirmResult = async () => {
  if (!paymentRecordId.value && !merchantOrderNo.value && !transactionId.value) {
    error.value = "缺少付款確認資訊，無法確認付款。";
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = "";
  try {
    const response = await confirmPublicPaymentService({
      paymentRecordId: paymentRecordId.value || undefined,
      merchantOrderNo: merchantOrderNo.value || undefined,
      transactionId: transactionId.value || undefined,
    });
    result.value = response.data;
  } catch (err: any) {
    error.value = err?.message || "付款確認失敗，請稍後再試。";
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  void loadConfirmResult();
});
</script>

<template>
  <div class="payment-result-page">
    <section class="result-card">
      <span class="section-kicker">PAYMENT RESULT</span>
      <h1 v-if="loading">正在確認付款結果...</h1>
      <h1 v-else-if="error">付款確認失敗</h1>
      <h1 v-else-if="result?.status === 'paid'">付款已確認</h1>
      <h1 v-else-if="result?.status === 'paid_review'">收款待人工核對</h1>
      <h1 v-else>付款尚未完成</h1>

      <p v-if="loading">系統正在與付款紀錄同步，請稍候一下。</p>
      <p v-else-if="error">{{ error }}</p>
      <p v-else>{{ result?.message }}</p>

      <dl v-if="result" class="result-list">
        <div>
          <dt>訂單編號</dt>
          <dd>{{ resolvedBookingNo || "-" }}</dd>
        </div>
        <div>
          <dt>付款紀錄</dt>
          <dd>#{{ result.paymentRecordId }}</dd>
        </div>
        <div>
          <dt>付款方式</dt>
          <dd>{{ result.provider }}</dd>
        </div>
        <div>
          <dt>付款金額</dt>
          <dd>NT$ {{ result.amount.toLocaleString() }}</dd>
        </div>
        <div>
          <dt>付款狀態</dt>
          <dd>{{ result.status }}</dd>
        </div>
        <div>
          <dt>交易編號</dt>
          <dd>{{ result.transactionId || transactionId || "-" }}</dd>
        </div>
      </dl>

      <div class="actions">
        <RouterLink class="primary-cta" :to="bookingResultRoute">
          回訂單結果頁
        </RouterLink>
        <RouterLink class="secondary-cta" to="/booking">再預約其他日期</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.payment-result-page {
  width: min(960px, calc(100% - 32px));
  margin: 0 auto;
  padding-bottom: 32px;
}

.result-card {
  padding: 32px;
  border-radius: 28px;
  border: 1px solid rgba(120, 111, 92, 0.1);
  background: rgba(255, 252, 247, 0.92);
  box-shadow: 0 12px 36px rgba(63, 52, 35, 0.05);
}

.section-kicker {
  display: inline-block;
  color: #8e927f;
  font-size: 0.78rem;
  letter-spacing: 0.14em;
}

.result-card h1 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
  font-size: clamp(2.2rem, 3.5vw, 3rem);
}

.result-card p {
  color: #61685d;
  line-height: 1.8;
}

.result-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.result-list div {
  padding: 18px;
  border-radius: 20px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
}

.result-list dt {
  color: #7d816d;
  font-size: 0.88rem;
}

.result-list dd {
  margin: 8px 0 0;
  color: #2f362b;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 24px;
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
  font-weight: 600;
}

.primary-cta {
  background: #6f7540;
  color: #fffdf7;
}

.secondary-cta {
  border: 1px solid rgba(99, 102, 84, 0.14);
  color: #2f362b;
  background: rgba(255, 252, 247, 0.72);
}

@media (max-width: 720px) {
  .payment-result-page {
    width: min(100%, calc(100% - 24px));
  }

  .result-card {
    padding: 24px;
  }

  .result-list {
    grid-template-columns: 1fr;
  }
}
</style>
