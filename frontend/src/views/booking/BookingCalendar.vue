<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { getBookingOperationRowsService, type BookingAdminRow } from "@/api/booking";

import { operationDate, stayLabel, overlapsOperationDate, isOverdueStay } from "@/utils/bookingOperations";

type DayCell = {
  date: string;
  day: number;
  inCurrentMonth: boolean;
  bookings: BookingAdminRow[];
};

const router = useRouter();
const loading = ref(false);
const loadError = ref(false);
const rows = ref<BookingAdminRow[]>([]);
const selectedDate = ref(operationDate());
const currentMonth = ref(selectedDate.value.slice(0, 7));

const activeRows = computed(() => rows.value.filter((row) => row.status !== "cancelled"));

const monthLabel = computed(() => {
  const [year, month] = currentMonth.value.split("-");
  return `${year} 年 ${Number(month)} 月`;
});

const calendarDays = computed<DayCell[]>(() => {
  const [year, month] = currentMonth.value.split("-").map(Number);
  const firstDay = new Date(year, month - 1, 1);
  const start = new Date(firstDay);
  start.setDate(firstDay.getDate() - firstDay.getDay());

  return Array.from({ length: 42 }, (_, index) => {
    const date = new Date(start);
    date.setDate(start.getDate() + index);
    const dateText = formatDate(date);
    return {
      date: dateText,
      day: date.getDate(),
      inCurrentMonth: date.getMonth() === month - 1,
      bookings: bookingsForDate(dateText),
    };
  });
});

const selectedRows = computed(() => bookingsForDate(selectedDate.value));

const monthRows = computed(() =>
  activeRows.value.filter((row) => row.checkInDate.slice(0, 7) === currentMonth.value)
);

const monthStats = computed(() => ({
  checkIns: monthRows.value.length,
  guests: monthRows.value.reduce((sum, row) => sum + Number(row.guestCount || 0), 0),
  vehicles: monthRows.value.reduce((sum, row) => sum + Number(row.vehicleCount || 0), 0),
  paid: monthRows.value.reduce((sum, row) => sum + Number(row.paidAmount || 0), 0),
}));

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

const bookingsForDate = (dateText: string) =>
  activeRows.value
    .filter((row) => overlapsOperationDate(row, dateText))
    .sort((a, b) => a.checkInDate.localeCompare(b.checkInDate) || a.bookingNo.localeCompare(b.bookingNo));

const dateRoleCounts = (dateText: string) => {
  const dayRows = bookingsForDate(dateText);
  return {
    overdue: dayRows.filter(row => isOverdueStay(row, dateText)).length,
    checkIns: dayRows.filter((row) => row.checkInDate === dateText).length,
    staying: dayRows.filter((row) => row.checkInDate < dateText && row.checkOutDate > dateText).length,
    checkOuts: dayRows.filter((row) => row.checkOutDate === dateText).length,
  };
};

const setMonth = (offset: number) => {
  const [year, month] = currentMonth.value.split("-").map(Number);
  const date = new Date(year, month - 1 + offset, 1);
  currentMonth.value = formatDate(date).slice(0, 7);
  selectedDate.value = `${currentMonth.value}-01`;
};

const jumpToday = () => {
  selectedDate.value = operationDate();
  currentMonth.value = selectedDate.value.slice(0, 7);
};

const selectDate = (dateText: string) => {
  selectedDate.value = dateText;
  currentMonth.value = dateText.slice(0, 7);
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

const dateRoleLabel = (row: BookingAdminRow) => {
  if (isOverdueStay(row, selectedDate.value)) return "逾期未退房";
  if (row.checkInDate === selectedDate.value) return "預計入住";
  if (row.checkOutDate === selectedDate.value) return "預計退房";
  return "預計住宿";
};

const formatCurrency = (value?: number | string) =>
  `NT$ ${Number(value || 0).toLocaleString("zh-TW")}`;

const openBooking = (row: BookingAdminRow) => {
  router.push(`/admin/booking/detail/${row.id}`);
};

let loadGeneration = 0;
const loadRows = async () => {
  const generation = ++loadGeneration;
  loading.value = true;
  loadError.value = false;
  try {
    const dates = calendarDays.value;
    const items = await getBookingOperationRowsService(dates[0]!.date, dates[dates.length - 1]!.date);
    if (generation === loadGeneration) rows.value = items;
  } catch { if (generation === loadGeneration) { rows.value = []; loadError.value = true; } }
  finally { if (generation === loadGeneration) loading.value = false; }
};
watch(currentMonth, loadRows);

onMounted(loadRows);
</script>

<template>
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
        <li class="breadcrumb-item" aria-current="page">預約管理</li>
        <li class="breadcrumb-item" aria-current="page">訂房日曆</li>
      </ul>
      <div class="calendar-heading">
        <div>
          <h4 class="pb-2 border-bottom mb-2">訂房日曆</h4>
          <p class="text-muted mb-0">查看預計行程與實際登記；點選訂單可登記入住／退房。</p>
        </div>
        <div class="calendar-heading__actions">
          <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="loadRows">重新整理</button>
          <button class="btn btn-outline-primary" type="button" @click="router.push('/admin/booking/check-ins')">
            入住名單
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-sm-6 col-xl-3">
      <div class="calendar-stat">
        <span>本月預計入住訂單</span>
        <strong>{{ monthStats.checkIns }}</strong>
      </div>
    </div>
    <div class="col-sm-6 col-xl-3">
      <div class="calendar-stat">
        <span>本月預計入住人數</span>
        <strong>{{ monthStats.guests }}</strong>
      </div>
    </div>
    <div class="col-sm-6 col-xl-3">
      <div class="calendar-stat">
        <span>本月車輛數</span>
        <strong>{{ monthStats.vehicles }}</strong>
      </div>
    </div>
    <div class="col-sm-6 col-xl-3">
      <div class="calendar-stat">
        <span>本月已收</span>
        <strong>{{ formatCurrency(monthStats.paid) }}</strong>
      </div>
    </div>
  </div>

  <p v-if="loadError" class="alert alert-danger" role="alert">訂房日曆載入失敗，請重新整理。</p>
  <div class="row g-3 mt-1">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header calendar-toolbar">
          <button class="btn btn-outline-secondary" type="button" @click="setMonth(-1)">上個月</button>
          <div class="calendar-title">
            <h5 class="mb-0">{{ monthLabel }}</h5>
            <button class="btn btn-link p-0" type="button" @click="jumpToday">回今天</button>
          </div>
          <button class="btn btn-outline-secondary" type="button" @click="setMonth(1)">下個月</button>
        </div>
        <div class="card-body">
          <div v-if="loading" class="text-muted py-4">訂房日曆載入中...</div>
          <div v-else class="calendar-grid">
            <div v-for="weekday in ['日', '一', '二', '三', '四', '五', '六']" :key="weekday" class="calendar-weekday">
              {{ weekday }}
            </div>
            <button
              v-for="day in calendarDays"
              :key="day.date"
              class="calendar-day"
              :class="{
                'calendar-day--muted': !day.inCurrentMonth,
                'calendar-day--selected': day.date === selectedDate,
                'calendar-day--today': day.date === operationDate(),
              }"
              type="button"
              @click="selectDate(day.date)"
            >
              <span class="calendar-day__number">{{ day.day }}</span>
              <span v-if="day.bookings.length > 0" class="calendar-day__total">{{ day.bookings.length }} 筆</span>
              <span class="calendar-day__badges">
                <span v-if="dateRoleCounts(day.date).overdue > 0">逾 {{ dateRoleCounts(day.date).overdue }}</span>
                <span v-if="dateRoleCounts(day.date).checkIns > 0">入 {{ dateRoleCounts(day.date).checkIns }}</span>
                <span v-if="dateRoleCounts(day.date).staying > 0">住 {{ dateRoleCounts(day.date).staying }}</span>
                <span v-if="dateRoleCounts(day.date).checkOuts > 0">退 {{ dateRoleCounts(day.date).checkOuts }}</span>
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <div class="card">
        <div class="card-header selected-header">
          <div>
            <h5 class="mb-1">{{ selectedDate }}</h5>
            <p class="text-muted mb-0">當日訂單 {{ selectedRows.length }} 筆</p>
          </div>
          <button class="btn btn-outline-primary btn-sm" type="button" @click="router.push('/admin/booking/list')">
            預約列表
          </button>
        </div>
        <div class="card-body">
          <div v-if="selectedRows.length === 0" class="empty-state">這一天沒有入住、住宿中或退房訂單。</div>
          <div v-else class="selected-list">
            <button
              v-for="row in selectedRows"
              :key="row.id"
              class="selected-item"
              type="button"
              @click="openBooking(row)"
            >
              <span class="selected-item__top">
                <strong>{{ row.bookingNo }}</strong>
                <span>{{ dateRoleLabel(row) }}</span>
              </span>
              <span>{{ row.contactName }} / {{ row.contactPhone }}</span>
              <small>{{ row.itemName }}，{{ row.guestCount }} 人 / {{ row.vehicleCount }} 車</small>
              <span class="selected-item__bottom">
                <small>{{ statusLabel(row.status) }} · {{ stayLabel(row) }}</small>
                <strong>{{ formatCurrency(row.totalAmount) }}</strong>
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.calendar-heading,
.calendar-heading__actions,
.calendar-toolbar,
.selected-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.calendar-stat {
  min-height: 112px;
  border: 1px solid #e4e9f0;
  border-radius: 8px;
  background: #fff;
  padding: 18px;
  display: grid;
  gap: 8px;
}

.calendar-stat span {
  color: #667085;
}

.calendar-stat strong {
  color: #1f2937;
  font-size: 28px;
  line-height: 1.1;
}

.calendar-title {
  text-align: center;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(92px, 1fr));
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: auto;
}

.calendar-weekday {
  min-height: 42px;
  padding: 10px;
  background: #f8fafc;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  color: #475467;
  font-weight: 800;
  text-align: center;
}

.calendar-day {
  min-height: 118px;
  border: 0;
  border-right: 1px solid #e5e7eb;
  border-bottom: 1px solid #e5e7eb;
  background: #fff;
  padding: 10px;
  display: grid;
  align-content: start;
  gap: 7px;
  text-align: left;
}

.calendar-day:hover {
  background: #fbfcf6;
}

.calendar-day--muted {
  background: #f8fafc;
  color: #98a2b3;
}

.calendar-day--selected {
  outline: 3px solid rgba(116, 128, 63, 0.28);
  outline-offset: -3px;
}

.calendar-day--today .calendar-day__number {
  background: #74803f;
  color: #fff;
}

.calendar-day__number {
  width: 30px;
  height: 30px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.calendar-day__total {
  color: #1f2937;
  font-weight: 800;
}

.calendar-day__badges {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.calendar-day__badges span {
  min-height: 22px;
  padding: 2px 7px;
  border-radius: 999px;
  background: #eef3de;
  color: #566127;
  font-size: 12px;
  font-weight: 800;
}

.empty-state {
  padding: 24px;
  border: 1px dashed #d9dee7;
  border-radius: 8px;
  color: #6b7280;
  background: #fbfcfd;
}

.selected-list {
  display: grid;
  gap: 10px;
}

.selected-item {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
  display: grid;
  gap: 6px;
  text-align: left;
}

.selected-item:hover {
  background: #fafbf6;
}

.selected-item strong {
  color: #243118;
}

.selected-item small,
.selected-item > span {
  color: #667085;
}

.selected-item__top,
.selected-item__bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.selected-item__top span {
  min-height: 24px;
  padding: 3px 8px;
  border-radius: 999px;
  background: #eef3de;
  color: #566127;
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 960px) {
  .calendar-heading,
  .calendar-heading__actions,
  .calendar-toolbar,
  .selected-header {
    display: grid;
    justify-content: stretch;
  }

  .calendar-grid {
    grid-template-columns: repeat(7, 112px);
  }
}
</style>
