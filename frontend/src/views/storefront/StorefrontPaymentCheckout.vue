<script setup lang="ts">
import { computed, ref } from "vue";
import { useRoute, useRouter } from "vue-router";

const route = useRoute();
const router = useRouter();
const PAYMENT_STORAGE_KEY = "storefront-last-payment";
const actionLoading = ref(false);

const paymentSnapshot = computed(() => {
  try {
    const raw = sessionStorage.getItem(PAYMENT_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
});

const provider = computed(() => String(route.params.provider || paymentSnapshot.value?.provider || "linepay"));
const paymentMethod = computed(() => String(route.params.method || paymentSnapshot.value?.paymentMethod || "app"));
const merchantOrderNo = computed(() => String(route.params.orderNo || paymentSnapshot.value?.merchantOrderNo || ""));
const paymentRecordId = computed(() => String(route.query.paymentRecordId || paymentSnapshot.value?.paymentRecordId || ""));
const bookingNo = computed(() => String(route.query.bookingNo || paymentSnapshot.value?.bookingNo || ""));
const amount = computed(() => Number(route.query.amount || paymentSnapshot.value?.amount || 0));

const providerLabel = computed(() => {
  if (provider.value === "linepay") return "LINE Pay";
  if (provider.value === "ecpay") return "綠界";
  return provider.value;
});

const confirmPayment = async () => {
  if (!paymentRecordId.value) return;
  actionLoading.value = true;
  await router.push({
    path: "/payment/confirm",
    query: {
      paymentRecordId: paymentRecordId.value,
      bookingNo: bookingNo.value,
      amount: String(amount.value),
    },
  });
};

const cancelPayment = async () => {
  await router.push({
    path: "/payment/cancel",
    query: {
      bookingNo: bookingNo.value,
      paymentRecordId: paymentRecordId.value,
    },
  });
};

const failPayment = async () => {
  await router.push({
    path: "/payment/fail",
    query: {
      bookingNo: bookingNo.value,
      paymentRecordId: paymentRecordId.value,
    },
  });
};
</script>

<template>
  <div class="payment-page">
    <section class="payment-shell">
      <div class="payment-brand">
        <span class="section-kicker">CHECKOUT</span>
        <h1>{{ providerLabel }} 付款頁</h1>
        <p>這是目前的前台 mock 付款頁。正式串接後，這裡會改成第三方導頁或 SDK 流程。</p>
      </div>

      <div class="payment-card">
        <dl class="payment-list">
          <div>
            <dt>訂單編號</dt>
            <dd>{{ bookingNo || "-" }}</dd>
          </div>
          <div>
            <dt>付款單號</dt>
            <dd>{{ merchantOrderNo || "-" }}</dd>
          </div>
          <div>
            <dt>付款方式</dt>
            <dd>{{ providerLabel }} / {{ paymentMethod }}</dd>
          </div>
          <div>
            <dt>付款金額</dt>
            <dd>NT$ {{ amount.toLocaleString() }}</dd>
          </div>
        </dl>

        <div class="tips-card">
          <strong>目前模擬項目</strong>
          <ul>
            <li>建立付款請求</li>
            <li>由付款頁進入付款確認</li>
            <li>回寫付款紀錄、訂單已收金額與 timeline</li>
          </ul>
        </div>

        <div class="actions">
          <button class="primary-cta" type="button" :disabled="actionLoading || !paymentRecordId" @click="confirmPayment">
            {{ actionLoading ? "處理中..." : "模擬付款成功" }}
          </button>
          <button class="secondary-cta" type="button" @click="cancelPayment">取消付款</button>
          <button class="danger-cta" type="button" @click="failPayment">模擬付款失敗</button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.payment-page {
  width: min(1080px, calc(100% - 32px));
  margin: 0 auto;
  padding-bottom: 32px;
}

.payment-shell {
  display: grid;
  gap: 24px;
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

.payment-brand h1 {
  margin: 10px 0 0;
  font-family: "Noto Serif TC", "Times New Roman", serif;
  font-size: clamp(2.2rem, 3.5vw, 3.2rem);
}

.payment-brand p,
.tips-card li {
  color: #61685d;
  line-height: 1.8;
}

.payment-card,
.tips-card {
  padding: 20px;
  border-radius: 22px;
  background: #fbf8f1;
  border: 1px solid rgba(124, 116, 95, 0.12);
}

.payment-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.payment-list dt {
  color: #7d816d;
  font-size: 0.88rem;
}

.payment-list dd {
  margin: 8px 0 0;
  color: #2f362b;
  font-weight: 600;
}

.tips-card {
  margin-top: 18px;
}

.tips-card ul {
  margin: 12px 0 0;
  padding-left: 18px;
  display: grid;
  gap: 8px;
}

.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.primary-cta,
.secondary-cta {
  min-height: 48px;
  padding: 0 24px;
  border-radius: 999px;
  font-weight: 600;
}

.danger-cta {
  min-height: 48px;
  padding: 0 24px;
  border: 1px solid rgba(164, 79, 57, 0.2);
  border-radius: 999px;
  background: rgba(186, 79, 58, 0.06);
  color: #a44f39;
  font-weight: 600;
}

.primary-cta {
  border: 0;
  background: #6f7540;
  color: #fffdf7;
}

.secondary-cta {
  border: 1px solid rgba(99, 102, 84, 0.14);
  background: rgba(255, 252, 247, 0.72);
  color: #2f362b;
}

@media (max-width: 720px) {
  .payment-page {
    width: min(100%, calc(100% - 24px));
  }

  .payment-shell {
    padding: 24px;
  }

  .payment-list {
    grid-template-columns: 1fr;
  }
}
</style>
