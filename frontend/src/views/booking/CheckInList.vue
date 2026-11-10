<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { getBookingOperationRowsService, type BookingAdminRow } from "@/api/booking";

import BookingStayActions from "@/components/booking/BookingStayActions.vue";
import { operationDate, stayLabel, formatStayTime, overlapsOperationDate, isOverdueStay, stayDate } from "@/utils/bookingOperations";

type StayType = "check-in" | "staying" | "check-out";

const router = useRouter();
const loading = ref(false);
const loadError = ref(false);
const rows = ref<BookingAdminRow[]>([]);
const total = ref(0);

const filters = reactive({
  targetDate: operationDate(),
  keyword: "",
});

const activeRows = computed(() => rows.value.filter((row) => row.status !== "cancelled"));

const targetRows = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  return activeRows.value
    .filter((row) => overlapsOperationDate(row, filters.targetDate))
    .filter((row) => {
      if (!keyword) return true;
      return [
        row.bookingNo,
        row.contactName,
        row.contactPhone,
        row.contactEmail,
        row.itemName,
        row.arrivalNote,
      ]
        .filter(Boolean)
        .some((value) => `${value}`.toLowerCase().includes(keyword));
    })
    .sort((a, b) => {
      const typeOrder = stayTypeOrder(getStayType(a)) - stayTypeOrder(getStayType(b));
      if (typeOrder !== 0) return typeOrder;
      return a.checkInDate.localeCompare(b.checkInDate) || a.bookingNo.localeCompare(b.bookingNo);
    });
});

const checkInRows = computed(() => targetRows.value.filter((row) => row.checkInDate === filters.targetDate));
const stayingRows = computed(() => targetRows.value.filter((row) => row.checkInDate < filters.targetDate && row.checkOutDate > filters.targetDate));
const checkOutRows = computed(() => targetRows.value.filter((row) => row.checkOutDate === filters.targetDate));

const summaryCards = computed(() => [
  {
    label: "預計入住",
    value: checkInRows.value.length,
    hint: "需要聯絡開門與確認抵達時間",
    tone: "olive",
  },
  {
    label: "預計住宿",
    value: stayingRows.value.length,
    hint: "依預約日期推算，實際抵達看登記狀態",
    tone: "blue",
  },
  {
    label: "預計退房",
    value: checkOutRows.value.length,
    hint: "可安排清潔與檢查營位",
    tone: "amber",
  },
  {
    label: "已入住未退房",
    value: targetRows.value.filter(row => row.stayStatus === "checked_in").length,
    hint: "目前登記狀態，包含逾期住宿",
    tone: "blue",
  },
  {
    label: "車輛數",
    value: targetRows.value.reduce((sum, row) => sum + Number(row.vehicleCount || 0), 0),
    hint: "用於停車與動線安排",
    tone: "slate",
  },
]);

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

const addDays = (days: number) => {
  const date = new Date(`${filters.targetDate}T00:00:00`);
  date.setDate(date.getDate() + days);
  filters.targetDate = formatDate(date);
};

const getStayType = (row: BookingAdminRow): StayType => {
  if (row.checkedOutAt && stayDate(row.checkedOutAt) === filters.targetDate) return "check-out";
  if (row.checkInDate === filters.targetDate) return "check-in";
  if (row.checkOutDate === filters.targetDate) return "check-out";
  return "staying";
};

const stayTypeOrder = (type: StayType) => {
  if (type === "check-in") return 1;
  if (type === "staying") return 2;
  return 3;
};

const stayTypeLabel = (row: BookingAdminRow) => {
  const type = getStayType(row);
  if (isOverdueStay(row, filters.targetDate)) return "逾期未退房";
  if (row.checkedOutAt && stayDate(row.checkedOutAt) === filters.targetDate) return "實際退房";
  if (type === "check-in") return "預計入住";
  if (type === "check-out") return "預計退房";
  return "預計住宿";
};

const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    pending: "待確認",
    confirmed: "已確認",
    cancelled: "已取消",
    completed: "已完成",
  };
  return map[status] || status || "-";
};

const paymentLabel = (row: BookingAdminRow) => {
  const totalAmount = Number(row.totalAmount || 0);
  const paidAmount = Number(row.paidAmount || 0);
  if (paidAmount <= 0) return "未收款";
  if (totalAmount > 0 && paidAmount >= totalAmount) return "已收清";
  return "已收訂金";
};

const formatCurrency = (value?: number | string) =>
  `NT$ ${Number(value || 0).toLocaleString("zh-TW")}`;

let loadGeneration = 0;
const loadRows = async () => {
  const generation = ++loadGeneration;
  const date = filters.targetDate;
  if (!date) { rows.value = []; total.value = 0; loading.value = false; loadError.value = false; return; }
  loading.value = true;
  loadError.value = false;
  try {
    const items = await getBookingOperationRowsService(date, date);
    if (generation === loadGeneration) { rows.value = items; total.value = items.length; }
  } catch { if (generation === loadGeneration) { rows.value = []; total.value = 0; loadError.value = true; } }
  finally { if (generation === loadGeneration) loading.value = false; }
};
watch(() => filters.targetDate, loadRows);

const openBooking = (row: BookingAdminRow) => {
  router.push(`/admin/booking/detail/${row.id}`);
};

const csvEscape = (value?: string | number) => {
  const text = `${value ?? ""}`.replace(/"/g, '""');
  return `"${text}"`;
};

const exportCsv = () => {
  if (targetRows.value.length === 0) {
    ElMessage.warning("目前沒有可匯出的入住資料");
    return;
  }

  const headers = [
    "類型",
    "訂單編號",
    "狀態",
    "實際住宿狀態",
    "實際入住時間（台北）",
    "實際退房時間（台北）",
    "付款",
    "聯絡人",
    "電話",
    "Email",
    "營位/項目",
    "入住日",
    "離營日",
    "晚數",
    "人數",
    "車數",
    "金額",
    "已收",
    "抵達/備註",
  ];
  const lines = targetRows.value.map((row) =>
    [
      stayTypeLabel(row),
      row.bookingNo,
      statusLabel(row.status),
      stayLabel(row),
      row.checkedInAt ? formatStayTime(row.checkedInAt) : "",
      row.checkedOutAt ? formatStayTime(row.checkedOutAt) : "",
      paymentLabel(row),
      row.contactName,
      row.contactPhone,
      row.contactEmail,
      row.itemName,
      row.checkInDate,
      row.checkOutDate,
      row.nights,
      row.guestCount,
      row.vehicleCount,
      row.totalAmount,
      row.paidAmount,
      row.arrivalNote || row.bookingNote || "",
    ]
      .map(csvEscape)
      .join(",")
  );

  const csv = `\uFEFF${headers.map(csvEscape).join(",")}\r\n${lines.join("\r\n")}`;
  const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `fadaiting-check-ins-${filters.targetDate}.csv`;
  link.click();
  URL.revokeObjectURL(url);
};

onMounted(loadRows);
</script>

<template>
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
        <li class="breadcrumb-item" aria-current="page">預約管理</li>
        <li class="breadcrumb-item" aria-current="page">入住名單</li>
      </ul>
      <div class="checkin-heading">
        <div>
          <h4 class="pb-2 border-bottom mb-2">入住名單</h4>
          <p class="text-muted mb-0">依預約日期整理預計行程，實際入住／退房另行登記（台北時間）。</p>
        </div>
        <div class="checkin-heading__actions">
          <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="loadRows">重新整理</button>
          <button class="btn btn-primary" type="button" @click="exportCsv">匯出 CSV</button>
        </div>
      </div>
    </div>
  </div>

  <div class="card">
    <div class="card-body">
      <div class="checkin-filters">
        <div class="date-control">
          <button class="btn btn-outline-secondary" type="button" @click="addDays(-1)">前一天</button>
          <input v-model="filters.targetDate" class="form-control" type="date" />
          <button class="btn btn-outline-secondary" type="button" @click="addDays(1)">後一天</button>
        </div>
        <input v-model="filters.keyword" class="form-control" type="text" placeholder="搜尋訂單、姓名、電話、營位" />
        <button class="btn btn-outline-primary" type="button" @click="router.push('/admin/booking/list')">
          回預約列表
        </button>
      </div>
    </div>
  </div>

  <div class="row g-3 mt-1">
    <div v-for="card in summaryCards" :key="card.label" class="col-sm-6 col-xl-3">
      <div class="checkin-card" :class="`checkin-card--${card.tone}`">
        <span>{{ card.label }}</span>
        <strong>{{ card.value }}</strong>
        <small>{{ card.hint }}</small>
      </div>
    </div>
  </div>

  <p v-if="loadError" class="alert alert-danger" role="alert">入住名單載入失敗，請重新整理。</p>
  <div class="card mt-3">
    <div class="card-header checkin-section-header">
      <div>
        <h5 class="mb-1">{{ filters.targetDate }} 名單</h5>
        <p class="text-muted mb-0">已載入 {{ rows.length }} / {{ total }} 筆預約，符合日期 {{ targetRows.length }} 筆。</p>
      </div>
    </div>
    <div class="card-body">
      <div v-if="loading" class="text-muted py-4">入住名單載入中...</div>
      <div v-else-if="targetRows.length === 0" class="empty-state">這一天沒有入住、住宿中或退房資料。</div>
      <div v-else class="table-responsive">
        <table class="table align-middle checkin-table">
          <thead>
            <tr>
              <th>類型</th>
              <th>訂單</th>
              <th>聯絡人</th>
              <th>營位/項目</th>
              <th>實際登記</th>
              <th>預約日期</th>
              <th>人 / 車</th>
              <th>付款</th>
              <th>備註</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in targetRows" :key="row.id" :data-booking-no="row.bookingNo">
              <td>
                <span class="stay-pill" :class="`stay-pill--${getStayType(row)}`">{{ stayTypeLabel(row) }}</span>
              </td>
              <td>
                <button class="booking-link" type="button" @click="openBooking(row)">{{ row.bookingNo }}</button>
                <div class="small text-muted">{{ statusLabel(row.status) }}</div>
              </td>
              <td>
                <strong>{{ row.contactName }}</strong>
                <div class="small text-muted">{{ row.contactPhone }}</div>
                <div class="small text-muted">{{ row.contactEmail || "未填 Email" }}</div>
              </td>
              <td>{{ row.itemName }}</td>
              <td><BookingStayActions :booking="row" @updated="loadRows" /></td>
              <td>
                {{ row.checkInDate }} - {{ row.checkOutDate }}
                <div class="small text-muted">{{ row.nights }} 晚</div>
              </td>
              <td>{{ row.guestCount }} 人 / {{ row.vehicleCount }} 車</td>
              <td>
                <strong>{{ paymentLabel(row) }}</strong>
                <div class="small text-muted">
                  {{ formatCurrency(row.paidAmount) }} / {{ formatCurrency(row.totalAmount) }}
                </div>
              </td>
              <td class="checkin-note">{{ row.arrivalNote || row.bookingNote || "無" }}</td>
              <td class="text-end">
                <button class="btn btn-outline-primary btn-sm" type="button" @click="openBooking(row)">查看</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.checkin-heading,
.checkin-heading__actions,
.checkin-section-header,
.checkin-filters,
.date-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.checkin-filters {
  display: grid;
  grid-template-columns: auto minmax(260px, 1fr) auto;
  align-items: stretch;
}

.date-control {
  display: grid;
  grid-template-columns: auto 180px auto;
  align-items: stretch;
}

.date-control .btn,
.checkin-filters > .btn {
  min-width: 86px;
  white-space: nowrap;
}

.checkin-filters > .form-control {
  min-width: 240px;
}

.checkin-card {
  min-height: 126px;
  border: 1px solid #e4e9f0;
  border-radius: 8px;
  background: #fff;
  padding: 18px;
  display: grid;
  gap: 6px;
}

.checkin-card span,
.checkin-card small {
  color: #667085;
}

.checkin-card strong {
  color: #1f2937;
  font-size: 30px;
  line-height: 1.1;
}

.checkin-card--olive {
  border-left: 5px solid #74803f;
}

.checkin-card--blue {
  border-left: 5px solid #2b6cb0;
}

.checkin-card--amber {
  border-left: 5px solid #d97706;
}

.checkin-card--slate {
  border-left: 5px solid #64748b;
}

.empty-state {
  padding: 24px;
  border: 1px dashed #d9dee7;
  border-radius: 8px;
  color: #6b7280;
  background: #fbfcfd;
}

.booking-link {
  border: 0;
  background: transparent;
  padding: 0;
  color: #2f5d22;
  font-weight: 800;
  text-align: left;
}

.booking-link:hover {
  color: #1d3d16;
  text-decoration: underline;
}

.stay-pill {
  display: inline-flex;
  justify-content: center;
  min-width: 58px;
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.stay-pill--check-in {
  background: #ecfdf3;
  color: #027a48;
}

.stay-pill--staying {
  background: #eff8ff;
  color: #175cd3;
}

.stay-pill--check-out {
  background: #fff7e6;
  color: #b54708;
}

.checkin-note {
  max-width: 260px;
  white-space: normal;
}

@media (max-width: 960px) {
  .checkin-heading,
  .checkin-heading__actions,
  .checkin-section-header,
  .checkin-filters,
  .date-control {
    display: grid;
    justify-content: stretch;
  }

  .checkin-filters {
    grid-template-columns: 1fr;
  }

  .date-control {
    grid-template-columns: auto minmax(0, 1fr) auto;
  }

  .checkin-filters > .form-control {
    min-width: 0;
  }
}
</style>
