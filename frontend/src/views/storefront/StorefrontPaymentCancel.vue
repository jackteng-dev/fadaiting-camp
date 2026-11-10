<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { cancelPublicPaymentService, type PublicPaymentConfirmResponse } from "@/api/camp";

const route = useRoute();
const bookingNo = computed(() => String(route.query.bookingNo || ""));
const paymentRecordId = computed(() => Number(route.query.paymentRecordId || 0));
const loading = ref(true);
const error = ref("");
const synced = ref(false);
const result = ref<PublicPaymentConfirmResponse | null>(null);
const resolvedBookingNo = computed(() => result.value?.bookingNo || bookingNo.value);
const resolvedBookingId = computed(() => result.value?.bookingId || Number(route.query.bookingId || 0));
const bookingResultRoute = computed(() => ({
  path: "/booking/success",
  query: {
    bookingId: resolvedBookingId.value ? String(resolvedBookingId.value) : undefined,
    bookingNo: resolvedBookingNo.value,
  },
}));

const syncCancelledPayment = async () => {
  if (!paymentRecordId.value) {
    loading.value = false;
    return;
  }

  loading.value = true;
  error.value = "";
  try {
    const response = await cancelPublicPaymentService({
      paymentRecordId: paymentRecordId.value,
    });
    result.value = response.data;
    synced.value = true;
  } catch (err: any) {
    error.value = err?.message || "付款取消狀態同步失敗，請稍後再查詢訂單狀態。";
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  void syncCancelledPayment();
});
</script>

<template>
  <div class="payment-cancel-page">
    <section class="cancel-card">
      <span class="section-kicker">PAYMENT CANCELLED</span>
      <h1>這次先不付款也沒關係</h1>
      <p>
        付款流程已取消，訂單本身不會因為這個畫面而消失。你可以回到預約結果頁，之後再重新建立付款請求。
      </p>

      <p v-if="loading" class="feedback">正在同步付款取消狀態...</p>
      <p v-else-if="error" class="feedback error">{{ error }}</p>
      <p v-else-if="synced" class="feedback success">付款取消狀態已同步完成。</p>

      <dl class="cancel-list">
        <div>
          <dt>訂單編號</dt>
          <dd>{{ resolvedBookingNo || "-" }}</dd>
        </div>
        <div>
          <dt>目前建議</dt>
          <dd>回到訂單結果頁，稍後再付款</dd>
        </div>
      </dl>

      <div class="actions">
        <RouterLink class="primary-cta" :to="bookingResultRoute">
          回訂單結果頁
        </RouterLink>
        <RouterLink class="secondary-cta" to="/booking">回預約頁</RouterLink>
        <RouterLink class="secondary-cta" to="/booking/lookup">查詢預約</RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.payment-cancel-page {
  width: min(960px, calc(100% - 32px));
  margin: 0 auto;
  padding-bottom: 32px;
}

.cancel-card {
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

.cancel-card h1 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
  font-size: clamp(2.2rem, 3.5vw, 3rem);
}

.cancel-card p {
  color: #61685d;
  line-height: 1.8;
}

.cancel-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.feedback {
  margin: 18px 0 0;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(111, 117, 64, 0.08);
  color: #596338;
}

.feedback.error {
  background: rgba(186, 79, 58, 0.08);
  color: #a44f39;
}

.feedback.success {
  background: rgba(86, 126, 76, 0.1);
  color: #4e713f;
}

.cancel-list div {
  padding: 18px;
  border-radius: 20px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
}

.cancel-list dt {
  color: #7d816d;
  font-size: 0.88rem;
}

.cancel-list dd {
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
  .payment-cancel-page {
    width: min(100%, calc(100% - 24px));
  }

  .cancel-card {
    padding: 24px;
  }

  .cancel-list {
    grid-template-columns: 1fr;
  }
}
</style>
