<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import {
  getPaymentMethodConfigListService,
  updatePaymentMethodConfigListService,
  type PaymentMethodConfig,
} from "@/api/booking";
import { confirmAdminAction } from "@/utils/confirmAction";

const loading = ref(false);
const saving = ref(false);
const methods = ref<PaymentMethodConfig[]>([]);

const paymentMethodOptions: Record<string, Array<{ label: string; value: string }>> = {
  linepay: [
    { label: "網頁付款", value: "web" },
    { label: "APP 付款", value: "app" },
    { label: "通用連結", value: "universal" },
  ],
  ecpay: [
    { label: "綠界付款選擇頁", value: "all" },
    { label: "信用卡", value: "credit" },
    { label: "ATM", value: "atm" },
    { label: "超商代碼", value: "cvs" },
    { label: "超商條碼", value: "barcode" },
  ],
};

const activeCount = computed(() => methods.value.filter((item) => item.isActive).length);

const providerLabel = (provider: string) => {
  if (provider === "linepay") return "LINE Pay";
  if (provider === "ecpay") return "綠界 ECPay";
  return provider;
};

const normalizeMethod = (method: PaymentMethodConfig): PaymentMethodConfig => ({
  ...method,
  displayName: method.displayName?.trim() || providerLabel(method.provider),
  description: method.description?.trim() || "",
  paymentMethod: method.paymentMethod || (method.provider === "ecpay" ? "all" : "web"),
  sortOrder: Number(method.sortOrder || 0),
  isActive: Boolean(method.isActive),
});

const loadMethods = async () => {
  loading.value = true;
  try {
    const response = await getPaymentMethodConfigListService();
    methods.value = (response.data || []).map(normalizeMethod);
  } finally {
    loading.value = false;
  }
};

const saveMethods = async () => {
  if (
    !(await confirmAdminAction({
      title: "確認更新支付方式",
      message: "確定要套用目前的支付方式設定嗎？關閉後前台將不再顯示該付款方式。",
      confirmButtonText: "儲存設定",
    }))
  ) {
    return;
  }

  saving.value = true;
  try {
    const payload = methods.value.map(normalizeMethod);
    const response = await updatePaymentMethodConfigListService(payload);
    methods.value = (response.data || []).map(normalizeMethod);
    ElMessage.success("支付方式設定已更新");
  } finally {
    saving.value = false;
  }
};

onMounted(loadMethods);
</script>

<template>
  <div class="row">
    <div class="col-12">
      <div class="card">
        <div class="card-header">
          <div class="payment-method-header">
            <div>
              <h4 class="mb-1">支付方式管理</h4>
              <p class="text-muted mb-0">管理前台付款區塊會顯示哪些支付方式，以及各支付方式實際使用的付款模式。</p>
            </div>
            <div class="payment-method-actions">
              <span class="status-summary">啟用 {{ activeCount }} / {{ methods.length }} 種</span>
              <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="loading || saving" @click="loadMethods">
                重新整理
              </button>
              <button class="btn btn-primary btn-sm" type="button" :disabled="loading || saving" @click="saveMethods">
                {{ saving ? "儲存中..." : "儲存設定" }}
              </button>
            </div>
          </div>
        </div>

        <div class="card-body">
          <div v-if="loading" class="loading-state">支付方式載入中...</div>

          <div v-else class="payment-method-grid">
            <article v-for="method in methods" :key="method.provider" class="method-card">
              <div class="method-card__head">
                <div>
                  <span class="provider-chip">{{ providerLabel(method.provider) }}</span>
                  <h5>{{ method.displayName || providerLabel(method.provider) }}</h5>
                </div>
                <label class="method-active-switch form-check form-switch">
                  <input
                    :id="`payment-method-${method.provider}-active`"
                    v-model="method.isActive"
                    class="form-check-input input-primary"
                    type="checkbox"
                    role="switch"
                  />
                  <span class="form-check-label">{{ method.isActive ? "前台啟用" : "前台停用" }}</span>
                </label>
              </div>

              <div class="form-grid">
                <label>
                  顯示名稱
                  <input v-model="method.displayName" class="form-control" type="text" placeholder="例如：LINE Pay" />
                </label>
                <label>
                  排序
                  <input v-model.number="method.sortOrder" class="form-control" type="number" min="0" step="1" />
                </label>
              </div>

              <label>
                付款模式
                <select v-model="method.paymentMethod" class="form-select">
                  <option
                    v-for="option in paymentMethodOptions[method.provider] || []"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <label>
                前台說明
                <textarea
                  v-model="method.description"
                  class="form-control"
                  rows="3"
                  placeholder="例如：使用 LINE Pay 線上付款。"
                ></textarea>
              </label>

              <p class="method-note">
                {{ method.isActive ? "目前會出現在前台付款區塊。" : "目前不會出現在前台付款區塊，後端也會拒絕建立此付款請求。" }}
              </p>
            </article>
          </div>

          <div class="helper-panel">
            <strong>營運提醒</strong>
            <p>這裡只控制前台是否顯示與能否建立付款請求；正式商家代號、HashKey、LINE Pay Channel 等金流憑證仍維持在環境設定中管理。</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-method-header,
.payment-method-actions,
.method-card__head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.payment-method-header {
  justify-content: space-between;
  align-items: flex-start;
}

.payment-method-actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.status-summary {
  padding: 6px 10px;
  border-radius: 999px;
  background: #eef4ff;
  color: #175cd3;
  font-size: 13px;
  font-weight: 700;
}

.loading-state {
  padding: 28px 0;
  color: #667085;
}

.payment-method-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.method-card {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid #eaecf0;
  border-radius: 8px;
  background: #fff;
}

.method-card__head {
  justify-content: space-between;
  align-items: flex-start;
}

.method-card h5 {
  margin: 8px 0 0;
  color: #101828;
  font-weight: 800;
}

.provider-chip {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: #ecfdf3;
  color: #027a48;
  font-size: 12px;
  font-weight: 700;
}

.method-active-switch {
  min-width: 126px;
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  margin: 0;
  padding-left: 0;
  color: #344054;
  font-weight: 700;
}

.method-active-switch .form-check-input {
  float: none;
  width: 36px;
  height: 20px;
  margin: 0;
  cursor: pointer;
}

.method-active-switch .form-check-label {
  cursor: pointer;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
}

label {
  display: grid;
  gap: 8px;
  color: #475467;
  font-weight: 700;
}

.method-note,
.helper-panel p {
  margin: 0;
  color: #667085;
  line-height: 1.7;
}

.helper-panel {
  margin-top: 18px;
  padding: 16px;
  border-radius: 8px;
  background: #fffaeb;
  border: 1px solid #fedf89;
}

.helper-panel strong {
  display: block;
  margin-bottom: 6px;
  color: #93370d;
}

@media (max-width: 960px) {
  .payment-method-header,
  .payment-method-actions,
  .method-card__head {
    display: grid;
    justify-content: stretch;
  }

  .payment-method-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .method-active-switch {
    justify-content: flex-start;
  }
}
</style>
