<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { getBookingListService, type BookingAdminRow } from "@/api/booking";

const router = useRouter();
const loading = ref(false);
const bookings = ref<BookingAdminRow[]>([]);
const total = ref(0);

const statusOptions = [
  { label: "待確認", value: "pending" },
  { label: "已確認", value: "confirmed" },
  { label: "已取消", value: "cancelled" },
  { label: "已完成", value: "completed" },
];

const statusLabel = (status: string) => {
  const match = statusOptions.find((item) => item.value === status);
  return match ? match.label : status || "-";
};

const today = computed(() => formatDate(new Date()));

const addDays = (dateText: string, days: number) => {
  const date = new Date(`${dateText}T00:00:00`);
  date.setDate(date.getDate() + days);
  return formatDate(date);
};

const weekEnd = computed(() => addDays(today.value, 7));

const activeBookings = computed(() => bookings.value.filter((booking) => booking.status !== "cancelled"));

const todayCheckIns = computed(() =>
  activeBookings.value.filter((booking) => booking.checkInDate === today.value)
);

const todayCheckOuts = computed(() =>
  activeBookings.value.filter((booking) => booking.checkOutDate === today.value)
);

const stayingToday = computed(() =>
  activeBookings.value.filter(
    (booking) => booking.checkInDate < today.value && booking.checkOutDate > today.value
  )
);

const pendingBookings = computed(() =>
  activeBookings.value.filter((booking) => booking.status === "pending")
);

const unpaidBookings = computed(() =>
  activeBookings.value.filter((booking) => {
    const totalAmount = Number(booking.totalAmount || 0);
    const paidAmount = Number(booking.paidAmount || 0);
    return totalAmount > 0 && paidAmount < totalAmount;
  })
);

const upcomingBookings = computed(() =>
  activeBookings.value
    .filter((booking) => booking.checkInDate >= today.value && booking.checkInDate <= weekEnd.value)
    .sort((a, b) => a.checkInDate.localeCompare(b.checkInDate) || a.createdTime.localeCompare(b.createdTime))
);

const latestBookings = computed(() =>
  [...bookings.value].sort((a, b) => b.createdTime.localeCompare(a.createdTime)).slice(0, 6)
);

const actionBookings = computed(() => {
  const map = new Map<number, BookingAdminRow>();
  pendingBookings.value.forEach((booking) => map.set(booking.id, booking));
  unpaidBookings.value.forEach((booking) => map.set(booking.id, booking));
  return [...map.values()].slice(0, 5);
});

const monthlyRevenue = computed(() => {
  const month = today.value.slice(0, 7);
  return bookings.value
    .filter((booking) => booking.createdTime?.startsWith(month))
    .reduce((sum, booking) => sum + Number(booking.paidAmount || 0), 0);
});

const dashboardCards = computed(() => [
  {
    label: "今日入住",
    value: todayCheckIns.value.length,
    hint: `${today.value} 需要開門接待`,
    icon: "ti ti-tent",
    tone: "olive",
    path: "/admin/booking/list",
  },
  {
    label: "待確認",
    value: pendingBookings.value.length,
    hint: "需回覆客人是否保留營位",
    icon: "ti ti-clipboard-check",
    tone: "amber",
    path: "/admin/booking/list",
  },
  {
    label: "未收清",
    value: unpaidBookings.value.length,
    hint: "包含未付款與訂金未補足",
    icon: "ti ti-cash",
    tone: "rose",
    path: "/admin/booking/payments",
  },
  {
    label: "近 7 日入住",
    value: upcomingBookings.value.length,
    hint: `${today.value} 到 ${weekEnd.value}`,
    icon: "ti ti-calendar-event",
    tone: "blue",
    path: "/admin/booking/list",
  },
]);

function formatDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

const formatCurrency = (value?: number | string) =>
  `NT$ ${Number(value || 0).toLocaleString("zh-TW")}`;

const formatDateTime = (value?: string) => {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
};

const paymentLabel = (booking: BookingAdminRow) => {
  const totalAmount = Number(booking.totalAmount || 0);
  const paidAmount = Number(booking.paidAmount || 0);

  if (paidAmount <= 0) return "未收款";
  if (totalAmount > 0 && paidAmount >= totalAmount) return "已收清";
  return "已收訂金";
};

const paymentTone = (booking: BookingAdminRow) => {
  const totalAmount = Number(booking.totalAmount || 0);
  const paidAmount = Number(booking.paidAmount || 0);

  if (paidAmount <= 0) return "unpaid";
  if (totalAmount > 0 && paidAmount >= totalAmount) return "paid";
  return "partial";
};

const openBooking = (booking: BookingAdminRow) => {
  router.push(`/admin/booking/detail/${booking.id}`);
};

const loadDashboard = async () => {
  loading.value = true;
  try {
    const res = await getBookingListService({
      pageNum: 1,
      pageSize: 200,
      bookingNo: "",
      status: "",
      contactName: "",
    });
    bookings.value = res.data.items || [];
    total.value = res.data.total || 0;
  } catch (error) {
    ElMessage.error("首頁資料載入失敗");
  } finally {
    loading.value = false;
  }
};

onMounted(loadDashboard);
</script>

<template>
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
      </ul>
      <div class="home-heading">
        <div>
          <h4 class="pb-2 border-bottom mb-2"><i class="bi bi-feather me-2"></i>今日營運</h4>
          <p class="text-muted mb-0">快速查看今日入住、待處理預約與近期收入狀態。</p>
        </div>
        <div class="home-heading__actions">
          <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="loadDashboard">
            重新整理
          </button>
          <button class="btn btn-outline-primary" type="button" @click="router.push('/admin/booking/check-ins')">
            入住名單
          </button>
          <button class="btn btn-outline-primary" type="button" @click="router.push('/admin/booking/calendar')">
            訂房日曆
          </button>
          <button class="btn btn-primary" type="button" @click="router.push('/admin/booking/list')">
            查看全部預約
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="row g-3">
    <div v-for="card in dashboardCards" :key="card.label" class="col-sm-6 col-xl-3">
      <button class="operation-card" :class="`operation-card--${card.tone}`" type="button" @click="router.push(card.path)">
        <span class="operation-card__icon"><i :class="card.icon"></i></span>
        <span class="operation-card__content">
          <span class="operation-card__label">{{ card.label }}</span>
          <strong>{{ card.value }}</strong>
          <span>{{ card.hint }}</span>
        </span>
      </button>
    </div>
  </div>

  <div v-if="loading" class="card mt-3">
    <div class="card-body text-muted">首頁資料載入中...</div>
  </div>

  <div v-else class="row g-3 mt-1">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header home-section-header">
          <div>
            <h5 class="mb-1">今日行程</h5>
            <p class="text-muted mb-0">入住、住宿中與退房訂單集中在這裡。</p>
          </div>
          <span class="home-total">{{ today }}</span>
        </div>
        <div class="card-body">
          <div v-if="todayCheckIns.length === 0 && stayingToday.length === 0 && todayCheckOuts.length === 0" class="empty-state">
            今天沒有入住、住宿中或退房訂單。
          </div>
          <div v-else class="today-board">
            <section class="today-board__group">
              <h6>今日入住</h6>
              <button
                v-for="booking in todayCheckIns"
                :key="`in-${booking.id}`"
                class="booking-row"
                type="button"
                @click="openBooking(booking)"
              >
                <span>
                  <strong>{{ booking.bookingNo }}</strong>
                  <small>{{ booking.contactName }} / {{ booking.contactPhone }}</small>
                </span>
                <span>
                  {{ booking.itemName }}
                  <small>{{ booking.guestCount }} 人 / {{ booking.vehicleCount }} 車</small>
                </span>
                <span class="status-pill status-pill--pending">{{ statusLabel(booking.status) }}</span>
              </button>
              <p v-if="todayCheckIns.length === 0" class="text-muted mb-0">無</p>
            </section>

            <section class="today-board__group">
              <h6>住宿中</h6>
              <button
                v-for="booking in stayingToday"
                :key="`stay-${booking.id}`"
                class="booking-row"
                type="button"
                @click="openBooking(booking)"
              >
                <span>
                  <strong>{{ booking.bookingNo }}</strong>
                  <small>{{ booking.checkInDate }} - {{ booking.checkOutDate }}</small>
                </span>
                <span>
                  {{ booking.contactName }}
                  <small>{{ booking.itemName }}</small>
                </span>
                <span class="status-pill status-pill--confirmed">{{ statusLabel(booking.status) }}</span>
              </button>
              <p v-if="stayingToday.length === 0" class="text-muted mb-0">無</p>
            </section>

            <section class="today-board__group">
              <h6>今日退房</h6>
              <button
                v-for="booking in todayCheckOuts"
                :key="`out-${booking.id}`"
                class="booking-row"
                type="button"
                @click="openBooking(booking)"
              >
                <span>
                  <strong>{{ booking.bookingNo }}</strong>
                  <small>{{ booking.contactName }} / {{ booking.contactPhone }}</small>
                </span>
                <span>{{ booking.itemName }}</span>
                <span class="status-pill status-pill--completed">退房</span>
              </button>
              <p v-if="todayCheckOuts.length === 0" class="text-muted mb-0">無</p>
            </section>
          </div>
        </div>
      </div>

      <div class="card mt-3">
        <div class="card-header home-section-header">
          <div>
            <h5 class="mb-1">近期入住</h5>
            <p class="text-muted mb-0">接下來 7 天需要準備的訂單。</p>
          </div>
          <span class="home-total">{{ upcomingBookings.length }} 筆</span>
        </div>
        <div class="card-body">
          <div v-if="upcomingBookings.length === 0" class="empty-state">近 7 天沒有入住訂單。</div>
          <div v-else class="table-responsive">
            <table class="table align-middle home-table">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>訂單</th>
                  <th>聯絡人</th>
                  <th>營位</th>
                  <th>人 / 車</th>
                  <th class="text-end">金額</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="booking in upcomingBookings" :key="booking.id" @click="openBooking(booking)">
                  <td>
                    <strong>{{ booking.checkInDate }}</strong>
                    <div class="text-muted small">{{ booking.nights }} 晚</div>
                  </td>
                  <td>
                    <strong class="booking-no">{{ booking.bookingNo }}</strong>
                    <div>
                      <span class="status-pill" :class="`status-pill--${booking.status}`">
                        {{ statusLabel(booking.status) }}
                      </span>
                    </div>
                  </td>
                  <td>
                    {{ booking.contactName }}
                    <div class="text-muted small">{{ booking.contactPhone }}</div>
                  </td>
                  <td>{{ booking.itemName }}</td>
                  <td>{{ booking.guestCount }} 人 / {{ booking.vehicleCount }} 車</td>
                  <td class="text-end">{{ formatCurrency(booking.totalAmount) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <div class="card">
        <div class="card-header home-section-header">
          <div>
            <h5 class="mb-1">需要處理</h5>
            <p class="text-muted mb-0">待確認或尚未收清的訂單。</p>
          </div>
          <span class="home-total">{{ actionBookings.length }} 筆</span>
        </div>
        <div class="card-body">
          <div v-if="actionBookings.length === 0" class="empty-state">目前沒有急需處理的訂單。</div>
          <div v-else class="action-list">
            <button
              v-for="booking in actionBookings"
              :key="booking.id"
              class="action-item"
              type="button"
              @click="openBooking(booking)"
            >
              <span>
                <strong>{{ booking.bookingNo }}</strong>
                <small>{{ booking.contactName }} / {{ booking.checkInDate }}</small>
              </span>
              <span class="action-item__badges">
                <span class="status-pill" :class="`status-pill--${booking.status}`">
                  {{ statusLabel(booking.status) }}
                </span>
                <span class="payment-pill" :class="`payment-pill--${paymentTone(booking)}`">
                  {{ paymentLabel(booking) }}
                </span>
              </span>
            </button>
          </div>
        </div>
      </div>

      <div class="card mt-3">
        <div class="card-header">
          <h5 class="mb-1">本月已收</h5>
          <p class="text-muted mb-0">依目前載入的預約付款金額統計。</p>
        </div>
        <div class="card-body">
          <div class="revenue-box">
            <strong>{{ formatCurrency(monthlyRevenue) }}</strong>
            <span>總預約 {{ total }} 筆，首頁載入 {{ bookings.length }} 筆供快速檢視。</span>
          </div>
        </div>
      </div>

      <div class="card mt-3">
        <div class="card-header home-section-header">
          <div>
            <h5 class="mb-1">最新預約</h5>
            <p class="text-muted mb-0">最近送出的訂單。</p>
          </div>
        </div>
        <div class="card-body">
          <div v-if="latestBookings.length === 0" class="empty-state">目前沒有預約資料。</div>
          <div v-else class="latest-list">
            <button
              v-for="booking in latestBookings"
              :key="`latest-${booking.id}`"
              class="latest-item"
              type="button"
              @click="openBooking(booking)"
            >
              <span>
                <strong>{{ booking.bookingNo }}</strong>
                <small>{{ booking.contactName }} / {{ booking.checkInDate }}</small>
              </span>
              <span>
                <small>{{ formatDateTime(booking.createdTime) }}</small>
                <strong>{{ formatCurrency(booking.totalAmount) }}</strong>
              </span>
            </button>
          </div>
          <button class="btn btn-outline-primary w-100 mt-3" type="button" @click="router.push('/admin/booking/list')">
            查看全部預約
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.home-heading__actions,
.home-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.operation-card {
  width: 100%;
  min-height: 132px;
  border: 1px solid #e4e9f0;
  border-radius: 8px;
  background: #fff;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.operation-card:hover {
  border-color: #7a8341;
  box-shadow: 0 12px 30px rgba(34, 40, 49, 0.08);
  transform: translateY(-1px);
}

.operation-card__icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex: 0 0 46px;
}

.operation-card__content {
  display: grid;
  gap: 4px;
}

.operation-card__label,
.operation-card__content span:last-child {
  color: #6b7280;
}

.operation-card__content strong {
  color: #1f2937;
  font-size: 30px;
  line-height: 1.1;
}

.operation-card--olive .operation-card__icon {
  background: #eef3de;
  color: #697438;
}

.operation-card--amber .operation-card__icon {
  background: #fff7e6;
  color: #b54708;
}

.operation-card--rose .operation-card__icon {
  background: #fef3f2;
  color: #b42318;
}

.operation-card--blue .operation-card__icon {
  background: #eff8ff;
  color: #175cd3;
}

.home-total {
  min-height: 34px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #f5f7ef;
  color: #50663e;
  font-weight: 700;
  white-space: nowrap;
}

.empty-state {
  padding: 24px;
  border: 1px dashed #d9dee7;
  border-radius: 8px;
  color: #6b7280;
  background: #fbfcfd;
}

.today-board {
  display: grid;
  gap: 18px;
}

.today-board__group {
  display: grid;
  gap: 10px;
}

.today-board__group h6 {
  margin: 0;
  color: #374151;
  font-weight: 800;
}

.booking-row,
.action-item,
.latest-item {
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px 16px;
  display: grid;
  align-items: center;
  gap: 12px;
  text-align: left;
}

.booking-row {
  grid-template-columns: minmax(170px, 1.2fr) minmax(150px, 1fr) auto;
}

.booking-row:hover,
.action-item:hover,
.latest-item:hover,
.home-table tbody tr:hover {
  background: #fafbf6;
}

.booking-row strong,
.action-item strong,
.latest-item strong,
.booking-no {
  color: #243118;
}

.booking-row small,
.action-item small,
.latest-item small {
  display: block;
  color: #6b7280;
  margin-top: 4px;
}

.status-pill,
.payment-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 24px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.status-pill--pending {
  background: #fff7e6;
  color: #b54708;
}

.status-pill--confirmed {
  background: #ecfdf3;
  color: #027a48;
}

.status-pill--cancelled {
  background: #f2f4f7;
  color: #667085;
}

.status-pill--completed {
  background: #eff8ff;
  color: #175cd3;
}

.payment-pill--unpaid {
  background: #fef3f2;
  color: #b42318;
}

.payment-pill--partial {
  background: #fff7e6;
  color: #b54708;
}

.payment-pill--paid {
  background: #ecfdf3;
  color: #027a48;
}

.home-table tbody tr {
  cursor: pointer;
}

.action-list,
.latest-list {
  display: grid;
  gap: 10px;
}

.action-item,
.latest-item {
  grid-template-columns: minmax(0, 1fr) auto;
}

.action-item__badges {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.revenue-box {
  display: grid;
  gap: 6px;
}

.revenue-box strong {
  font-size: 34px;
  line-height: 1.1;
  color: #1f2937;
}

.revenue-box span {
  color: #6b7280;
}

@media (max-width: 768px) {
  .home-heading,
  .home-heading__actions,
  .home-section-header {
    display: grid;
    justify-content: stretch;
  }

  .booking-row,
  .action-item,
  .latest-item {
    grid-template-columns: 1fr;
  }

  .action-item__badges {
    justify-content: flex-start;
  }
}
</style>
