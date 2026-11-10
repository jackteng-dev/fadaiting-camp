<script setup lang="ts">
import PaymentFinancialDialog from "@/components/booking/PaymentFinancialDialog.vue";
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import {
  getPaymentListService,
  getPaymentSummaryService,
  type PaymentAdminQuery,
  type PaymentAdminRow,
  type PaymentAdminSummary,
} from "@/api/booking";

const router = useRouter();
const loading = ref(false);
const financialPaymentId = ref<number | null>(null);
const exportLoading = ref(false);
const rows = ref<PaymentAdminRow[]>([]);
const total = ref(0);
const selectedPayload = ref("");
const summaryRows = ref<PaymentAdminSummary[]>([]);
const filters = reactive<PaymentAdminQuery>({
  pageNum: 1,
  pageSize: 10,
  bookingNo: "",
  provider: "",
  status: "",
  merchantOrderNo: "",
  startDate: "",
  endDate: "",
});

const providerOptions = [
  { label: "LINE Pay", value: "linepay" },
  { label: "綠界", value: "ecpay" },
  { label: "人工", value: "manual" },
];

const statusOptions = [
  { label: "待付款", value: "pending" },
  { label: "付款完成", value: "paid" },
  { label: "付款失敗", value: "failed" },
  { label: "付款取消", value: "cancelled" },
  { label: "付款逾時", value: "expired" },
  { label: "收款待人工核對", value: "paid_review" },
  { label: "已退款", value: "refunded" },
];

const maxPage = computed(() => Math.max(1, Math.ceil(total.value / filters.pageSize)));

const summaryCards = computed(() => {
  const summaryMap = new Map(summaryRows.value.map((item) => [item.status, item]));
  const getCard = (status: string, label: string, tone: string) => {
    const row = summaryMap.get(status);
    return {
      status,
      label,
      tone,
      count: Number(row?.recordCount || 0),
      amount: Number(row?.totalAmount || 0),
    };
  };

  return [
    getCard("pending", "待付款", "pending"),
    getCard("paid", "已付款", "paid"),
    getCard("failed", "付款失敗", "failed"),
    {
      status: "inactive",
      label: "取消 / 逾時",
      tone: "inactive",
      count: Number(summaryMap.get("cancelled")?.recordCount || 0) + Number(summaryMap.get("expired")?.recordCount || 0),
      amount: Number(summaryMap.get("cancelled")?.totalAmount || 0) + Number(summaryMap.get("expired")?.totalAmount || 0),
    },
  ];
});

const statusText = (status: string) => {
  const match = statusOptions.find((item) => item.value === status);
  return match ? match.label : status;
};

const providerText = (provider: string) => {
  const match = providerOptions.find((item) => item.value === provider);
  return match ? match.label : provider;
};

const formatCurrency = (value?: number | string, currency = "TWD") => {
  return `${currency} ${Number(value || 0).toLocaleString()}`;
};

const formatDateTime = (value?: string) => value?.replace("T", " ").slice(0, 16) || "-";

const payloadPreview = (payload?: string) => {
  if (!payload) return "-";
  return payload.length > 80 ? `${payload.slice(0, 80)}...` : payload;
};

const csvCell = (value?: string | number | null) => {
  const text = value === null || value === undefined ? "" : String(value);
  return `"${text.replace(/"/g, '""')}"`;
};

const buildCsv = (items: PaymentAdminRow[]) => {
  const headers = [
    "付款紀錄ID",
    "訂單編號",
    "訂單狀態",
    "聯絡人",
    "聯絡電話",
    "金流",
    "付款方式",
    "付款狀態",
    "金額",
    "幣別",
    "付款單號",
    "金流交易編號",
    "付款代碼",
    "建立時間",
    "付款時間",
    "更新時間",
  ];
  const lines = items.map((row) => [
    row.id,
    row.bookingNo,
    row.bookingStatus,
    row.contactName || "",
    row.contactPhone || "",
    providerText(row.provider),
    row.paymentMethod,
    statusText(row.status),
    row.amount,
    row.currency,
    row.merchantOrderNo,
    row.providerTransactionId || "",
    row.paymentNo || "",
    formatDateTime(row.createdTime),
    formatDateTime(row.paidTime),
    formatDateTime(row.modifiedTime),
  ].map(csvCell).join(","));
  return [headers.map(csvCell).join(","), ...lines].join("\r\n");
};

const downloadCsv = (csv: string) => {
  const today = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const blob = new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `fadaiting-payments-${today}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const loadPayments = async () => {
  loading.value = true;
  try {
    const [listRes, summaryRes] = await Promise.all([
      getPaymentListService(filters),
      getPaymentSummaryService(filters),
    ]);
    rows.value = listRes.data.items || [];
    total.value = listRes.data.total || 0;
    summaryRows.value = summaryRes.data || [];
  } finally {
    loading.value = false;
  }
};

const onSearch = async () => {
  filters.pageNum = 1;
  await loadPayments();
};

const resetFilters = async () => {
  filters.pageNum = 1;
  filters.pageSize = 10;
  filters.bookingNo = "";
  filters.provider = "";
  filters.status = "";
  filters.merchantOrderNo = "";
  filters.startDate = "";
  filters.endDate = "";
  await loadPayments();
};

const goPage = async (direction: "prev" | "next") => {
  if (direction === "prev" && filters.pageNum > 1) {
    filters.pageNum -= 1;
  }
  if (direction === "next" && filters.pageNum < maxPage.value) {
    filters.pageNum += 1;
  }
  await loadPayments();
};

const openBooking = (row: PaymentAdminRow) => {
  router.push(`/admin/booking/detail/${row.bookingId}`);
};

const showPayload = (row: PaymentAdminRow) => {
  selectedPayload.value = row.callbackPayload || "";
};

const exportCsv = async () => {
  if (total.value === 0) {
    ElMessage.info("目前沒有可匯出的付款紀錄");
    return;
  }

  exportLoading.value = true;
  try {
    const response = await getPaymentListService({
      ...filters,
      pageNum: 1,
      pageSize: Math.min(Math.max(total.value, 1), 10000),
    });
    const items = response.data.items || [];
    downloadCsv(buildCsv(items));
    ElMessage.success(`已匯出 ${items.length} 筆付款紀錄`);
  } finally {
    exportLoading.value = false;
  }
};

onMounted(loadPayments);
</script>

<template>
  <PaymentFinancialDialog :payment-id="financialPaymentId" @close="financialPaymentId = null" @updated="loadPayments" />
  <div class="row">
    <div class="col-12">
      <div class="card">
        <div class="card-header">
          <div class="payment-header">
            <div>
              <h4 class="mb-1">付款紀錄</h4>
              <p class="text-muted mb-0">集中查看 LINE Pay、綠界與 mock 付款請求，協助查帳與金流除錯。</p>
            </div>
            <div class="payment-actions">
              <button class="btn btn-outline-success btn-sm" type="button" :disabled="exportLoading" @click="exportCsv">
                <i class="ti ti-file-export"></i>
                {{ exportLoading ? "匯出中..." : "匯出 CSV" }}
              </button>
              <button class="btn btn-outline-primary btn-sm" type="button" @click="router.push('/admin/booking/list')">
                回預約管理
              </button>
            </div>
          </div>
        </div>

        <div class="card-body">
          <div class="summary-grid">
            <article v-for="card in summaryCards" :key="card.status" class="summary-card" :class="`summary-card--${card.tone}`">
              <span>{{ card.label }}</span>
              <strong>{{ card.count }} 筆</strong>
              <small>{{ formatCurrency(card.amount) }}</small>
            </article>
          </div>

          <div class="payment-filters">
            <input v-model="filters.bookingNo" class="form-control" type="text" placeholder="訂單編號" />
            <input v-model="filters.merchantOrderNo" class="form-control" type="text" placeholder="付款單號" />
            <select v-model="filters.provider" class="form-select">
              <option value="">全部金流</option>
              <option v-for="option in providerOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <select v-model="filters.status" class="form-select">
              <option value="">全部狀態</option>
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <input v-model="filters.startDate" class="form-control" type="date" />
            <input v-model="filters.endDate" class="form-control" type="date" />
            <button class="btn btn-primary" type="button" @click="onSearch">查詢</button>
            <button class="btn btn-outline-secondary" type="button" @click="resetFilters">清空</button>
          </div>

          <div v-if="loading" class="payment-loading">付款資料載入中...</div>

          <div v-else class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>付款紀錄</th>
                  <th>訂單</th>
                  <th>金流 / 方式</th>
                  <th>狀態</th>
                  <th>金額</th>
                  <th>交易資訊</th>
                  <th>時間</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="8" class="text-center text-muted py-4">目前沒有付款紀錄</td>
                </tr>
                <tr v-for="row in rows" :key="row.id">
                  <td>
                    <strong>#{{ row.id }}</strong>
                    <div class="small text-muted">{{ row.merchantOrderNo }}</div>
                  </td>
                  <td>
                    <button class="booking-link" type="button" @click="openBooking(row)">{{ row.bookingNo }}</button>
                    <div class="small text-muted">{{ row.contactName || "-" }} / {{ row.contactPhone || "-" }}</div>
                  </td>
                  <td>
                    <span class="provider-badge">{{ providerText(row.provider) }}</span>
                    <div class="small text-muted">{{ row.paymentMethod }}</div>
                  </td>
                  <td>
                    <span class="payment-status" :class="`payment-status--${row.status}`">
                      {{ statusText(row.status) }}
                    </span>
                  </td>
                  <td>{{ formatCurrency(row.amount, row.currency) }}</td>
                  <td>
                    <div class="small">交易：{{ row.providerTransactionId || row.paymentNo || "-" }}</div>
                    <div class="payload-preview">{{ payloadPreview(row.callbackPayload) }}</div>
                  </td>
                  <td>
                    <div>建立 {{ formatDateTime(row.createdTime) }}</div>
                    <div class="small text-muted">付款 {{ formatDateTime(row.paidTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button v-if="['paid', 'paid_review'].includes(row.status)" class="btn btn-outline-primary btn-sm" @click="financialPaymentId = row.id">退款／對帳</button>
                    <button class="btn btn-outline-secondary btn-sm" type="button" @click="showPayload(row)">
                      payload
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="payment-pagination">
            <span>共 {{ total }} 筆，第 {{ filters.pageNum }} / {{ maxPage }} 頁</span>
            <div>
              <button class="btn btn-outline-secondary btn-sm" type="button" @click="goPage('prev')">上一頁</button>
              <button class="btn btn-outline-secondary btn-sm ms-2" type="button" @click="goPage('next')">下一頁</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selectedPayload" class="payload-modal">
      <div class="payload-dialog">
        <div class="payload-dialog__head">
          <h5 class="mb-0">Callback / Request Payload</h5>
          <button class="btn-close" type="button" aria-label="關閉" @click="selectedPayload = ''"></button>
        </div>
        <pre>{{ selectedPayload }}</pre>
      </div>
    </div>
  </div>
</template>

<style scoped>
.payment-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.payment-filters {
  display: grid;
  grid-template-columns: 1.2fr 1.2fr 1fr 1fr 1fr 1fr auto auto;
  gap: 12px;
  margin-bottom: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.summary-card {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #eaecf0;
  border-radius: 8px;
  background: #fff;
}

.summary-card span {
  color: #667085;
  font-size: 12px;
  font-weight: 600;
}

.summary-card strong {
  color: #101828;
  font-size: 22px;
}

.summary-card small {
  color: #475467;
  font-size: 13px;
}

.summary-card--pending {
  background: #fffaeb;
}

.summary-card--paid {
  background: #ecfdf3;
}

.summary-card--failed {
  background: #fef3f2;
}

.summary-card--inactive {
  background: #f8f9fc;
}

.payment-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: end;
}

.payment-loading {
  padding: 24px 0;
  color: #6b7280;
}

.provider-badge,
.payment-status {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.provider-badge {
  background: #eef4ff;
  color: #175cd3;
}

.payment-status--pending {
  background: #fff7e6;
  color: #b54708;
}

.payment-status--paid {
  background: #ecfdf3;
  color: #027a48;
}

.payment-status--failed,
.payment-status--cancelled,
.payment-status--expired {
  background: #fef3f2;
  color: #b42318;
}

.payment-status--refunded {
  background: #f4f3ff;
  color: #5925dc;
}

.booking-link {
  border: 0;
  background: transparent;
  padding: 0;
  color: #2f5d22;
  font-weight: 700;
  text-align: left;
}

.booking-link:hover {
  color: #1d3d16;
  text-decoration: underline;
}

.payload-preview {
  max-width: 280px;
  color: #667085;
  font-size: 12px;
  word-break: break-all;
}

.payment-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.payload-modal {
  position: fixed;
  inset: 0;
  z-index: 1050;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
}

.payload-dialog {
  width: min(900px, 100%);
  max-height: min(720px, calc(100vh - 48px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.22);
}

.payload-dialog__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-bottom: 1px solid #eaecf0;
}

pre {
  margin: 0;
  padding: 18px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  color: #344054;
}

@media (max-width: 1100px) {
  .summary-grid,
  .payment-filters {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .payment-header,
  .payment-actions,
  .payment-pagination {
    display: grid;
    justify-content: stretch;
  }

  .payment-filters {
    grid-template-columns: 1fr;
  }
}
</style>
