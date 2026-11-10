<script setup lang="ts">
import PaymentFinancialDialog from "@/components/booking/PaymentFinancialDialog.vue";
import BookingStayActions from "@/components/booking/BookingStayActions.vue";
import { editableBookingStatuses, formatStayTime } from "@/utils/bookingOperations";
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import {
  createManualPaymentService,
  getBookingDetailService,
  updateBookingDetailService,
  updateBookingNoteService,
  updateBookingStatusService,
  type BookingDetailResponse,
} from "@/api/booking";
import { confirmAdminAction } from "@/utils/confirmAction";

import { useUserInfoStore } from "@/stores/userInfo";
const userInfoStore = useUserInfoStore();
const route = useRoute();
const loading = ref(false);
const financialPaymentId = ref<number | null>(null);
const savingStatus = ref(false);
const savingAmounts = ref(false);
const savingManualPayment = ref(false);
const detail = ref<BookingDetailResponse | null>(null);
const amountForm = ref({
  paidAmount: 0,
  adjustmentAmount: 0,
  adminNote: "",
  confirmNote: "",
  cancelReason: "",
});
const manualPaymentForm = ref({
  amount: 0,
  paymentMethod: "cash",
  note: "",
});

const bookingId = computed(() => Number(route.params.id));

const statusOptions = [
  { label: "待確認", value: "pending" },
  { label: "已確認", value: "confirmed" },
  { label: "已取消", value: "cancelled" },
  { label: "已完成", value: "completed" },
];

const editableStatuses = computed(() => detail.value ? statusOptions.filter(option => editableBookingStatuses(detail.value!.booking, userInfoStore.isCampAdmin).includes(option.value)) : []);

const saveHandoffNote = async () => {
  if (!detail.value) return;
  savingAmounts.value = true;
  try {
    await updateBookingNoteService({ bookingId: bookingId.value, adminNote: amountForm.value.adminNote });
    ElMessage.success('交接備註已儲存');
    await loadDetail();
  } finally { savingAmounts.value = false; }
};

const statusText = (status?: string) => {
  const match = statusOptions.find((item) => item.value === status);
  return match ? match.label : status || "-";
};

const paymentStatusMeta = computed(() => {
  const total = Number(previewTotalAmount.value || 0);
  const paid = Number(amountForm.value.paidAmount || 0);

  if (paid <= 0) {
    return { label: "未收款", tone: "unpaid", hint: "尚未記錄任何收款。" };
  }
  if (total > 0 && paid >= total) {
    return {
      label: paid > total ? "超收款" : "已收清",
      tone: paid > total ? "overpaid" : "paid",
      hint: paid > total ? "已收金額高於應收總額，請再確認。" : "這筆訂單目前已收清。",
    };
  }
  return { label: "已收訂金", tone: "partial", hint: "已收部分款項，仍有尾款待收。" };
});

const paymentBalance = computed(() => {
  const booking = detail.value?.booking;
  if (!booking) return 0;
  return Number(booking.totalAmount || 0) - Number(booking.paidAmount || 0);
});

const previewTotalAmount = computed(() => {
  const subtotal = Number(detail.value?.booking?.subtotalAmount || 0);
  return subtotal + Number(amountForm.value.adjustmentAmount || 0);
});

const previewBalance = computed(() => previewTotalAmount.value - Number(amountForm.value.paidAmount || 0));
const depositSuggestion = computed(() => Math.max(Math.round(previewTotalAmount.value * 0.5), 0));

const formatDateTime = (value?: string) => value?.replace("T", " ") || "-";
const formatCurrency = (value?: number | string) => `NT$ ${Number(value || 0)}`;

const syncAmountForm = () => {
  const booking = detail.value?.booking;
  if (!booking) return;
  amountForm.value = {
    paidAmount: Number(booking.paidAmount || 0),
    adjustmentAmount: Number(booking.adjustmentAmount || 0),
    adminNote: booking.adminNote || "",
    confirmNote: booking.confirmNote || "",
    cancelReason: booking.cancelReason || "",
  };
};

const loadDetail = async () => {
  if (!bookingId.value) return;
  loading.value = true;
  try {
    const response = await getBookingDetailService(bookingId.value);
    detail.value = response.data || null;
    syncAmountForm();
    manualPaymentForm.value.amount = Math.max(paymentBalance.value, 0);
  } finally {
    loading.value = false;
  }
};

const updateStatus = async (status: string, selectEl?: HTMLSelectElement) => {
  if (!detail.value?.booking) return;
  const originalStatus = detail.value.booking.status;
  if (status === originalStatus) return;

  const confirmed = await confirmAdminAction({
    title: "確認更新訂單狀態",
    message: `確定要將訂單「${detail.value.booking.bookingNo}」狀態改為「${statusText(status)}」嗎？`,
    confirmButtonText: "更新狀態",
  });
  if (!confirmed) {
    detail.value.booking.status = status;
    await nextTick();
    detail.value.booking.status = originalStatus;
    await nextTick();
    if (selectEl) {
      selectEl.value = originalStatus;
      window.setTimeout(() => {
        selectEl.value = originalStatus;
      });
    }
    await loadDetail();
    return;
  }

  savingStatus.value = true;
  try {
    await updateBookingStatusService({
      bookingId: detail.value.booking.id,
      status,
      expectedVersion: detail.value.booking.operationVersion,
    });
    ElMessage.success("訂單狀態已更新");
    await loadDetail();
  } catch { if (selectEl) selectEl.value = originalStatus; }
  finally {
    savingStatus.value = false;
  }
};

const saveBookingAmounts = async () => {
  if (!detail.value?.booking) return;
  if (
    !(await confirmAdminAction({
      title: "確認儲存訂單處理資訊",
      message: `確定要儲存訂單「${detail.value.booking.bookingNo}」的金額與備註修改嗎？`,
      confirmButtonText: "儲存處理資訊",
    }))
  ) {
    return;
  }

  savingAmounts.value = true;
  try {
    await updateBookingDetailService({
      bookingId: detail.value.booking.id,
      expectedVersion: detail.value.booking.operationVersion,
      paidAmount: Number(amountForm.value.paidAmount || 0),
      adjustmentAmount: Number(amountForm.value.adjustmentAmount || 0),
      adminNote: amountForm.value.adminNote,
      confirmNote: amountForm.value.confirmNote,
      cancelReason: amountForm.value.cancelReason,
    });
    await loadDetail();
    ElMessage.success("訂單處理資訊已更新");
  } finally {
    savingAmounts.value = false;
  }
};

const applySuggestedPaidAmount = (amount: number) => {
  amountForm.value.paidAmount = amount;
};

const createManualPayment = async () => {
  if (!detail.value?.booking) return;
  if (
    !(await confirmAdminAction({
      title: "確認補登付款",
      message: `確定要為訂單「${detail.value.booking.bookingNo}」補登 ${formatCurrency(manualPaymentForm.value.amount)} 嗎？`,
      confirmButtonText: "補登付款",
    }))
  ) {
    return;
  }

  savingManualPayment.value = true;
  try {
    await createManualPaymentService({
      bookingId: detail.value.booking.id,
      amount: Number(manualPaymentForm.value.amount || 0),
      paymentMethod: manualPaymentForm.value.paymentMethod,
      note: manualPaymentForm.value.note,
    });
    ElMessage.success("補登付款已建立");
    manualPaymentForm.value.note = "";
    await loadDetail();
  } finally {
    savingManualPayment.value = false;
  }
};

onMounted(loadDetail);
</script>

<template>
  <PaymentFinancialDialog :payment-id="financialPaymentId" @close="financialPaymentId = null" @updated="loadDetail" />
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
        <li class="breadcrumb-item" aria-current="page">預約管理</li>
        <li class="breadcrumb-item"><router-link to="/admin/booking/list">查詢預約</router-link></li>
        <li class="breadcrumb-item" aria-current="page">預約明細</li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">預約明細</h4>
      <p class="text-muted mb-0">查看單筆預約的入住資訊、聯絡資料、金額與各項目明細。</p>
    </div>
  </div>

  <div v-if="loading" class="card">
    <div class="card-body text-muted py-4">資料載入中...</div>
  </div>

  <div v-else-if="!detail?.booking" class="card">
    <div class="card-body text-muted py-4">找不到預約資料</div>
  </div>

  <div v-else class="row g-3">
    <div class="col-xl-4">
      <div class="card h-100">
        <div class="card-header">
          <h5 class="mb-0">訂單摘要</h5>
        </div>
        <div class="card-body">
          <div class="summary-item">
            <span class="summary-label">訂單編號</span>
            <strong>{{ detail.booking.bookingNo }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">狀態</span>
            <select
              class="form-select"
              :value="detail.booking.status"
              :disabled="savingStatus || !userInfoStore.isCampAdmin || editableStatuses.length < 2"
              @change="updateStatus(($event.target as HTMLSelectElement).value, $event.target as HTMLSelectElement)"
            >
              <option v-for="option in editableStatuses" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <div class="summary-muted mt-2">{{ statusText(detail.booking.status) }}</div>
          </div>
          <div class="summary-item">
            <span class="summary-label">實際住宿登記（台北時間）</span>
            <BookingStayActions :booking="detail.booking" @updated="loadDetail" />
          </div>
          <div class="summary-item">
            <span class="summary-label">付款期限（台北時間）</span>
            <strong>{{ detail.booking.paymentDueAt ? formatStayTime(detail.booking.paymentDueAt) : '無自動期限' }}</strong>
            <p v-if="detail.booking.expiredAt" class="text-danger mt-2">已於 {{ formatStayTime(detail.booking.expiredAt) }} 因付款逾時取消</p>
          </div>
          <div class="summary-item">
            <span class="summary-label">來源</span>
            <strong>{{ detail.booking.source }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">建立時間</span>
            <strong>{{ formatDateTime(detail.booking.createdTime) }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">最後更新</span>
            <strong>{{ formatDateTime(detail.booking.modifiedTime) }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">客人備註</span>
            <p class="summary-note">{{ detail.booking.note || "無" }}</p>
          </div>
          <div class="summary-item">
            <span class="summary-label">人工確認備註</span>
            <p class="summary-note">{{ detail.booking.confirmNote || "無" }}</p>
          </div>
          <div class="summary-item">
            <span class="summary-label">取消原因</span>
            <p class="summary-note">{{ detail.booking.cancelReason || "無" }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-8">
      <div class="card mb-3">
        <div class="card-header">
          <h5 class="mb-0">入住資訊</h5>
        </div>
        <div class="card-body">
          <dl class="detail-grid">
            <div>
              <dt>入住日期</dt>
              <dd>{{ detail.booking.checkInDate }}</dd>
            </div>
            <div>
              <dt>離營日期</dt>
              <dd>{{ detail.booking.checkOutDate }}</dd>
            </div>
            <div>
              <dt>晚數</dt>
              <dd>{{ detail.booking.nights }} 晚</dd>
            </div>
            <div>
              <dt>人數 / 車數</dt>
              <dd>{{ detail.booking.guestCount }} 人 / {{ detail.booking.vehicleCount }} 車</dd>
            </div>
          </dl>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-header">
          <h5 class="mb-0">聯絡資訊</h5>
        </div>
        <div class="card-body">
          <dl class="detail-grid">
            <div>
              <dt>聯絡人</dt>
              <dd>{{ detail.guest?.contactName || "未填寫" }}</dd>
            </div>
            <div>
              <dt>電話</dt>
              <dd>{{ detail.guest?.contactPhone || "未填寫" }}</dd>
            </div>
            <div>
              <dt>Email</dt>
              <dd>{{ detail.guest?.contactEmail || "未填寫" }}</dd>
            </div>
            <div>
              <dt>緊急聯絡人</dt>
              <dd>{{ detail.guest?.emergencyContactName || "未填寫" }}</dd>
            </div>
            <div>
              <dt>緊急聯絡電話</dt>
              <dd>{{ detail.guest?.emergencyContactPhone || "未填寫" }}</dd>
            </div>
            <div>
              <dt>抵達說明</dt>
              <dd>{{ detail.guest?.arrivalNote || "無" }}</dd>
            </div>
          </dl>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-header">
          <h5 class="mb-0">預約項目</h5>
        </div>
        <div class="card-body">
          <div class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>項目</th>
                  <th>方案</th>
                  <th>單價</th>
                  <th>數量</th>
                  <th>晚數</th>
                  <th>小計</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!detail.items || detail.items.length === 0">
                  <td colspan="6" class="text-center text-muted py-4">沒有預約項目</td>
                </tr>
                <tr v-for="item in detail.items" :key="item.id">
                  <td>
                    <div class="fw-semibold">{{ item.itemName }}</div>
                    <div class="small text-muted">{{ item.campUnitName || "未指定營位" }}</div>
                  </td>
                  <td>{{ item.ratePlanName || "未指定方案" }}</td>
                  <td>NT$ {{ item.unitPrice }}</td>
                  <td>{{ item.quantity }}</td>
                  <td>{{ item.nights }}</td>
                  <td>NT$ {{ item.lineTotal }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-header">
          <h5 class="mb-0">處理歷程</h5>
        </div>
        <div class="card-body">
          <div v-if="!detail.timeline || detail.timeline.length === 0" class="text-muted">目前尚無歷程紀錄</div>
          <div v-else class="timeline-list">
            <div v-for="entry in detail.timeline" :key="entry.id" class="timeline-item">
              <div class="timeline-item__dot"></div>
              <div class="timeline-item__content">
                <div class="timeline-item__head">
                  <strong>{{ entry.actionLabel }}</strong>
                  <span class="text-muted">{{ formatDateTime(entry.createdTime) }}</span>
                </div>
                <div class="timeline-item__meta">
                  {{ entry.actorName || "系統" }}
                </div>
                <p v-if="entry.detail" class="timeline-item__detail">{{ entry.detail }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card mb-3">
        <div class="card-header">
          <div class="finance-header">
            <h5 class="mb-0">付款紀錄</h5>
            <span class="text-muted small">未收 {{ formatCurrency(paymentBalance) }}</span>
          </div>
        </div>
        <div class="card-body">
          <div v-if="userInfoStore.isCampAdmin" class="manual-payment-box">
            <div>
              <label class="summary-label" for="manual-payment-amount">補登付款金額</label>
              <input
                id="manual-payment-amount"
                v-model.number="manualPaymentForm.amount"
                class="form-control"
                type="number"
                min="1"
                step="1"
                :max="Math.max(paymentBalance, 0)"
              />
            </div>
            <div>
              <label class="summary-label" for="manual-payment-method">收款方式</label>
              <select id="manual-payment-method" v-model="manualPaymentForm.paymentMethod" class="form-select">
                <option value="cash">現金</option>
                <option value="transfer">轉帳</option>
                <option value="manual">其他</option>
              </select>
            </div>
            <div>
              <label class="summary-label" for="manual-payment-note">備註</label>
              <input
                id="manual-payment-note"
                v-model.trim="manualPaymentForm.note"
                class="form-control"
                type="text"
                placeholder="例如尾款、現場收款、匯款末五碼"
              />
            </div>
            <button
              class="btn btn-primary"
              type="button"
              :disabled="savingManualPayment || paymentBalance <= 0"
              @click="createManualPayment"
            >
              {{ savingManualPayment ? "補登中..." : "補登付款" }}
            </button>
          </div>
          <div v-if="!detail.payments || detail.payments.length === 0" class="text-muted">
            目前尚未建立付款請求
          </div>
          <div v-else class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>服務商</th>
                  <th>方式</th>
                  <th>狀態</th>
                  <th>金額</th>
                  <th>商店單號</th>
                  <th>第三方單號</th>
                  <th>建立時間</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="payment in detail.payments" :key="payment.id">
                  <td>{{ payment.provider }}</td>
                  <td>{{ payment.paymentMethod }}</td>
                  <td>{{ payment.status }}<br /><button v-if="userInfoStore.isCampAdmin && ['paid', 'paid_review'].includes(payment.status)" class="btn btn-sm btn-outline-primary mt-1" @click="financialPaymentId = payment.id">退款／對帳</button></td>
                  <td>{{ formatCurrency(payment.amount) }}<div v-if="payment.refundedAmount" class="small text-muted">已實退 {{ formatCurrency(payment.refundedAmount) }}</div></td>
                  <td>
                    <div>{{ payment.merchantOrderNo }}</div>
                    <div v-if="payment.paymentUrl" class="small text-muted">{{ payment.paymentUrl }}</div>
                  </td>
                  <td>{{ payment.providerTransactionId || payment.paymentNo || "-" }}</td>
                  <td>{{ formatDateTime(payment.createdTime) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-header">
          <div class="finance-header">
            <h5 class="mb-0">金額資訊</h5><span v-if="detail.payments.some(payment => payment.refundedAmount)" class="small text-warning">已收金額為退款後淨額；原訂單總額保留，後續餘額需人工核對。</span>
            <button v-if="userInfoStore.isCampAdmin" class="btn btn-primary btn-sm" type="button" :disabled="savingAmounts" @click="saveBookingAmounts">
              {{ savingAmounts ? "儲存中..." : "儲存處理資訊" }}
            </button>
            <button v-else class="btn btn-primary btn-sm" type="button" :disabled="savingAmounts" @click="saveHandoffNote">
              {{ savingAmounts ? "儲存中..." : "儲存交接備註" }}
            </button>
          </div>
        </div>
        <div class="card-body">
          <div class="finance-overview">
            <div class="finance-kpi">
              <span class="finance-kpi__label">付款狀態</span>
              <span class="payment-badge" :class="`payment-badge--${paymentStatusMeta.tone}`">
                {{ paymentStatusMeta.label }}
              </span>
              <p class="finance-kpi__hint">{{ paymentStatusMeta.hint }}</p>
            </div>
            <div v-if="userInfoStore.isCampAdmin" class="finance-kpi">
              <span class="finance-kpi__label">快速帶入</span>
              <div class="finance-actions">
                <button class="btn btn-outline-secondary btn-sm" type="button" @click="applySuggestedPaidAmount(0)">
                  未收
                </button>
                <button
                  class="btn btn-outline-secondary btn-sm"
                  type="button"
                  @click="applySuggestedPaidAmount(depositSuggestion)"
                >
                  50% 訂金
                </button>
                <button
                  class="btn btn-outline-secondary btn-sm"
                  type="button"
                  @click="applySuggestedPaidAmount(previewTotalAmount)"
                >
                  收清
                </button>
              </div>
            </div>
          </div>

          <dl class="detail-grid finance-grid">
            <div>
              <dt>小計</dt>
              <dd>{{ formatCurrency(detail.booking.subtotalAmount) }}</dd>
            </div>
            <div>
              <dt>調整金額</dt>
              <dd>
                <input :disabled="!userInfoStore.isCampAdmin" v-model.number="amountForm.adjustmentAmount" class="form-control" type="number" step="1" />
              </dd>
            </div>
            <div>
              <dt>應收總額</dt>
              <dd>{{ formatCurrency(previewTotalAmount) }}</dd>
            </div>
            <div>
              <dt>已收金額</dt>
              <dd>
                <input :disabled="!userInfoStore.isCampAdmin" v-model.number="amountForm.paidAmount" class="form-control" type="number" min="0" step="1" />
              </dd>
            </div>
            <div>
              <dt>未收金額</dt>
              <dd :class="{ 'text-danger': previewBalance < 0 }">{{ formatCurrency(previewBalance) }}</dd>
            </div>
          </dl>
          <div class="finance-notes-grid">
            <div class="finance-note">
              <label class="summary-label" for="booking-admin-note">內部備註</label>
              <textarea
                id="booking-admin-note"
                maxlength="2000"
                v-model="amountForm.adminNote"
                class="form-control"
                rows="4"
                placeholder="例如訂單處理紀錄、現場交接事項"
              ></textarea>
            </div>
            <div class="finance-note">
              <label class="summary-label" for="booking-confirm-note">人工確認備註</label>
              <textarea
                id="booking-confirm-note"
                :disabled="!userInfoStore.isCampAdmin"
                v-model="amountForm.confirmNote"
                class="form-control"
                rows="4"
                placeholder="例如已電話確認、保留到款期限、提醒事項"
              ></textarea>
            </div>
            <div class="finance-note finance-note--full">
              <label class="summary-label" for="booking-cancel-reason">取消原因</label>
              <textarea
                id="booking-cancel-reason"
                :disabled="!userInfoStore.isCampAdmin"
                v-model="amountForm.cancelReason"
                class="form-control"
                rows="3"
                placeholder="若訂單取消，可記錄客人原因或營區端取消說明"
              ></textarea>
            </div>
          </div>
          <div class="finance-tip">
            目前資料：應收 {{ formatCurrency(detail.booking.totalAmount) }} / 已收
            {{ formatCurrency(detail.booking.paidAmount) }} / 未收 {{ formatCurrency(paymentBalance) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.finance-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.finance-overview {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 18px;
}

.finance-kpi {
  padding: 14px 16px;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fafbfc;
}

.manual-payment-box {
  display: grid;
  grid-template-columns: minmax(130px, 0.8fr) minmax(140px, 0.8fr) minmax(220px, 1.4fr) auto;
  gap: 12px;
  align-items: end;
  margin-bottom: 18px;
  padding: 14px;
  border: 1px solid #edf1f7;
  border-radius: 8px;
  background: #fafbfc;
}

.finance-kpi__label {
  display: block;
  margin-bottom: 8px;
  color: #6b7280;
  font-size: 12px;
}

.finance-kpi__hint {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.summary-item + .summary-item {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #edf1f7;
}

.summary-label {
  display: block;
  margin-bottom: 0.35rem;
  color: #6c757d;
  font-size: 0.875rem;
}

.summary-muted {
  display: block;
  color: #6c757d;
}

.summary-note {
  margin: 0;
  color: #374151;
  white-space: pre-wrap;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 16px;
  margin: 0;
}

.detail-grid dt {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.detail-grid dd {
  margin: 0;
  color: #111827;
  font-weight: 500;
  word-break: break-word;
}

.finance-grid dd {
  display: flex;
  align-items: center;
}

.finance-note {
  margin-top: 20px;
}

.finance-notes-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 20px;
}

.finance-note {
  margin-top: 0;
}

.finance-note--full {
  grid-column: 1 / -1;
}

.finance-tip {
  margin-top: 14px;
  color: #6b7280;
  font-size: 13px;
}

.timeline-list {
  display: grid;
  gap: 16px;
}

.timeline-item {
  display: grid;
  grid-template-columns: 16px 1fr;
  gap: 12px;
}

.timeline-item__dot {
  width: 10px;
  height: 10px;
  margin-top: 6px;
  border-radius: 999px;
  background: #3b82f6;
}

.timeline-item__content {
  padding-bottom: 16px;
  border-bottom: 1px solid #edf1f7;
}

.timeline-item:last-child .timeline-item__content {
  padding-bottom: 0;
  border-bottom: 0;
}

.timeline-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.timeline-item__meta {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.timeline-item__detail {
  margin: 8px 0 0;
  color: #374151;
  white-space: pre-wrap;
}

.finance-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.payment-badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.payment-badge--unpaid {
  background: #fef3f2;
  color: #b42318;
}

.payment-badge--partial {
  background: #fff7e6;
  color: #b54708;
}

.payment-badge--paid {
  background: #ecfdf3;
  color: #027a48;
}

.payment-badge--overpaid {
  background: #eff8ff;
  color: #175cd3;
}

@media (max-width: 768px) {
  .finance-header {
    align-items: stretch;
    flex-direction: column;
  }

  .finance-overview {
    grid-template-columns: 1fr;
  }

  .manual-payment-box {
    grid-template-columns: 1fr;
  }

  .finance-notes-grid {
    grid-template-columns: 1fr;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .timeline-item__head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
