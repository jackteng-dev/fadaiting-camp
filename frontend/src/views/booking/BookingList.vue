<script setup lang="ts">
import BookingStayActions from "@/components/booking/BookingStayActions.vue";
import { editableBookingStatuses, formatStayTime } from "@/utils/bookingOperations";
import { nextTick, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import {
  getBookingListService,
  expireUnpaidBookingsService,
  updateBookingStatusService,
  type BookingAdminQuery,
  type BookingAdminRow,
} from "@/api/booking";
import { confirmAdminAction } from "@/utils/confirmAction";

import { useUserInfoStore } from "@/stores/userInfo";
const userInfoStore = useUserInfoStore();
const router = useRouter();
const loading = ref(false);
const expiring = ref(false);
const expireUnpaid = async () => {
  if (!(await confirmAdminAction({ title: "處理未付款逾時", message: "僅取消已達付款期限且尚未收款的待確認預約，並釋放保留庫存。已確認或已收款訂單不受影響。", confirmButtonText: "處理逾時預約" }))) return;
  expiring.value = true;
  try {
    const response = await expireUnpaidBookingsService();
    const result = response.data;
    if (result.failedCount) ElMessage.warning(`已取消 ${result.expiredCount} 筆，${result.failedCount} 筆失敗，請核對庫存與營運錯誤紀錄`);
    else ElMessage.success(`本批已取消 ${result.expiredCount} 筆逾時預約，略過 ${result.skippedCount} 筆處理中的訂單`);
    await loadBookings();
  } finally { expiring.value = false; }
};
const rows = ref<BookingAdminRow[]>([]);
const total = ref(0);
const filters = reactive<BookingAdminQuery>({
  pageNum: 1,
  pageSize: 10,
  bookingNo: "",
  status: "",
  contactName: "",
});

const statusOptions = [
  { label: "待確認", value: "pending" },
  { label: "已確認", value: "confirmed" },
  { label: "已取消", value: "cancelled" },
  { label: "已完成", value: "completed" },
];

const editableStatuses = (row: BookingAdminRow) => statusOptions.filter(option => editableBookingStatuses(row, userInfoStore.isCampAdmin).includes(option.value));

const statusText = (status: string) => {
  const match = statusOptions.find((item) => item.value === status);
  return match ? match.label : status;
};

const getPaymentStatusMeta = (row: BookingAdminRow) => {
  const total = Number(row.totalAmount || 0);
  const paid = Number(row.paidAmount || 0);

  if (paid <= 0) {
    return { label: "未收款", tone: "unpaid" };
  }
  if (total > 0 && paid >= total) {
    return { label: paid > total ? "超收款" : "已收清", tone: paid > total ? "overpaid" : "paid" };
  }
  return { label: "已收訂金", tone: "partial" };
};

const formatCurrency = (value?: number | string) => `NT$ ${Number(value || 0)}`;

const loadBookings = async () => {
  loading.value = true;
  try {
    const res = await getBookingListService(filters);
    rows.value = res.data.items || [];
    total.value = res.data.total || 0;
  } finally {
    loading.value = false;
  }
};

const resetFilters = async () => {
  filters.pageNum = 1;
  filters.pageSize = 10;
  filters.bookingNo = "";
  filters.status = "";
  filters.contactName = "";
  await loadBookings();
};

const openBooking = (row: BookingAdminRow) => {
  router.push(`/admin/booking/detail/${row.id}`);
};

const formatDateTime = (value?: string) => value?.replace("T", " ") || "-";

const changeStatus = async (row: BookingAdminRow, status: string, selectEl?: HTMLSelectElement) => {
  const originalStatus = row.status;
  if (status === originalStatus) return;

  const confirmed = await confirmAdminAction({
    title: "確認更新訂單狀態",
    message: `確定要將訂單「${row.bookingNo}」狀態改為「${statusText(status)}」嗎？`,
    confirmButtonText: "更新狀態",
  });
  if (!confirmed) {
    row.status = status;
    await nextTick();
    row.status = originalStatus;
    await nextTick();
    if (selectEl) {
      selectEl.value = originalStatus;
      window.setTimeout(() => {
        selectEl.value = originalStatus;
      });
    }
    await loadBookings();
    return;
  }

  try {
  await updateBookingStatusService({
    bookingId: row.id,
    status,
    expectedVersion: row.operationVersion,
  });
  ElMessage.success("訂單狀態已更新");
  await loadBookings();
  } catch { if (selectEl) selectEl.value = originalStatus; }
};

const onSearch = async () => {
  filters.pageNum = 1;
  await loadBookings();
};

const goPage = async (direction: "prev" | "next") => {
  const maxPage = Math.max(1, Math.ceil(total.value / filters.pageSize));
  if (direction === "prev" && filters.pageNum > 1) {
    filters.pageNum -= 1;
  }
  if (direction === "next" && filters.pageNum < maxPage) {
    filters.pageNum += 1;
  }
  await loadBookings();
};

onMounted(loadBookings);
</script>

<template>
  <div class="row">
    <div class="col-12">
      <div class="card">
        <div class="card-header">
          <div class="booking-header">
            <div>
              <h4 class="mb-1">預約管理</h4>
              <p class="text-muted mb-0">查看前台送進來的預約單，並更新人工確認狀態。</p>
            </div>
            <div class="booking-actions">
              <button v-if="userInfoStore.isCampAdmin" :disabled="expiring" class="btn btn-outline-warning btn-sm" type="button" @click="expireUnpaid">處理未付款逾時</button>
              <button class="btn btn-outline-primary btn-sm" type="button" @click="router.push('/admin/booking/check-ins')">
                入住名單
              </button>
              <button class="btn btn-outline-primary btn-sm" type="button" @click="router.push('/admin/booking/calendar')">
                訂房日曆
              </button>
              <button v-if="userInfoStore.isCampAdmin" class="btn btn-outline-primary btn-sm" type="button" @click="router.push('/admin/booking/notifications')">
                通知紀錄
              </button>
              <button v-if="userInfoStore.isCampAdmin" class="btn btn-outline-primary btn-sm" type="button" @click="router.push('/admin/booking/payments')">
                付款紀錄
              </button>
              <div class="booking-total">共 {{ total }} 筆</div>
            </div>
          </div>
        </div>
        <div class="card-body">
          <div class="booking-filters">
            <input v-model="filters.bookingNo" class="form-control" type="text" placeholder="訂單編號" />
            <input v-model="filters.contactName" class="form-control" type="text" placeholder="聯絡人" />
            <select v-model="filters.status" class="form-select">
              <option value="">全部狀態</option>
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <button class="btn btn-primary" type="button" @click="onSearch">查詢</button>
            <button class="btn btn-outline-secondary" type="button" @click="resetFilters">清空</button>
          </div>

          <div v-if="loading" class="booking-loading">資料載入中...</div>

          <div v-else class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>訂單編號</th>
                  <th>聯絡人</th>
                  <th>方案</th>
                  <th>日期</th>
                  <th>人/車</th>
                  <th>金額</th>
                  <th>狀態</th>
                  <th>建立時間</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="9" class="text-center text-muted py-4">目前沒有預約資料</td>
                </tr>
                <tr v-for="row in rows" :key="row.id">
                  <td>
                    <button class="booking-link" type="button" @click="openBooking(row)">{{ row.bookingNo }}</button>
                    <div class="small text-muted">{{ row.contactEmail || "未填 Email" }}</div>
                    <div v-if="row.expiredAt" class="small text-danger">付款逾時取消</div>
                    <div v-else-if="row.status === 'pending' && row.paymentDueAt && !row.paidAmount" class="small text-muted">付款期限 {{ formatStayTime(row.paymentDueAt) }}</div>
                  </td>
                  <td>
                    <div>{{ row.contactName }}</div>
                    <div class="small text-muted">{{ row.contactPhone }}</div>
                  </td>
                  <td>{{ row.itemName }}</td>
                  <td>{{ row.checkInDate }} - {{ row.checkOutDate }}<br /><span class="small text-muted">{{ row.nights }} 晚</span></td>
                  <td>{{ row.guestCount }} 人 / {{ row.vehicleCount }} 車</td>
                  <td>
                    <div>{{ formatCurrency(row.totalAmount) }}</div>
                    <div class="small text-muted">已收 {{ formatCurrency(row.paidAmount) }}</div>
                  </td>
                  <td>
                    <select
                      class="form-select form-select-sm"
                      :value="row.status"
                      :disabled="!userInfoStore.isCampAdmin || editableStatuses(row).length < 2"
                      @change="changeStatus(row, ($event.target as HTMLSelectElement).value, $event.target as HTMLSelectElement)"
                    >
                      <option v-for="option in editableStatuses(row)" :key="option.value" :value="option.value">
                        {{ option.label }}
                      </option>
                    </select>
                    <BookingStayActions :booking="row" @updated="loadBookings" />
                    <div class="status-meta mt-1">
                      <span class="small text-muted">{{ statusText(row.status) }}</span>
                      <span class="payment-badge" :class="`payment-badge--${getPaymentStatusMeta(row).tone}`">
                        {{ getPaymentStatusMeta(row).label }}
                      </span>
                    </div>
                  </td>
                  <td>{{ formatDateTime(row.createdTime) }}</td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm" type="button" @click="openBooking(row)">查看</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="booking-pagination">
            <button class="btn btn-outline-secondary btn-sm" type="button" @click="goPage('prev')">上一頁</button>
            <span>第 {{ filters.pageNum }} 頁</span>
            <button class="btn btn-outline-secondary btn-sm" type="button" @click="goPage('next')">下一頁</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.booking-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.booking-total {
  min-height: 40px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f5f7ef;
  color: #50663e;
  font-weight: 700;
}

.booking-filters {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr auto auto;
  gap: 12px;
  margin-bottom: 18px;
}

.booking-loading {
  padding: 24px 0;
  color: #6b7280;
}

.booking-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: end;
  align-items: center;
  gap: 12px;
}

.status-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.payment-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.booking-actions {
  display: flex;
  gap: 10px;
  align-items: center;
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

@media (max-width: 960px) {
  .booking-header,
  .booking-actions,
  .booking-pagination {
    display: grid;
    justify-content: stretch;
  }

  .booking-filters {
    grid-template-columns: 1fr;
  }
}
</style>
