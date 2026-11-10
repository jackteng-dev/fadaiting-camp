<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { useUserInfoStore } from "@/stores/userInfo";
import { adminLogoutService } from "@/api/user";
import { getBookingListService, type BookingAdminRow } from "@/api/booking";
import router from '@/router';
const userInfoStore = useUserInfoStore();
const recentBookings = ref<BookingAdminRow[]>([]);
const recentBookingTotal = ref(0);
const bookingMessageLoading = ref(false);

const statusOptions = [
  { label: "待確認", value: "pending" },
  { label: "已確認", value: "confirmed" },
  { label: "已取消", value: "cancelled" },
  { label: "已完成", value: "completed" },
];

const statusText = (status: string) => {
  const match = statusOptions.find((item) => item.value === status);
  return match ? match.label : status;
};

const paymentText = (row: BookingAdminRow) => {
  const total = Number(row.totalAmount || 0);
  const paid = Number(row.paidAmount || 0);
  if (paid <= 0) return "未收款";
  if (total > 0 && paid >= total) return "已收清";
  return "已收訂金";
};

const formatDateTime = (value?: string) => value?.replace("T", " ").slice(0, 16) || "-";
const formatCurrency = (value?: number | string) => `NT$ ${Number(value || 0).toLocaleString("zh-TW")}`;

const loadRecentBookings = async () => {
  bookingMessageLoading.value = true;
  try {
    const res = await getBookingListService({
      pageNum: 1,
      pageSize: 5,
      bookingNo: "",
      status: "",
      contactName: "",
    });
    recentBookings.value = res.data.items || [];
    recentBookingTotal.value = res.data.total || 0;
  } finally {
    bookingMessageLoading.value = false;
  }
};

const logout = () => {
  ElMessageBox.confirm("你確認要登出嗎?", "溫馨提示", {
    confirmButtonText: "確定",
    cancelButtonText: "取消",
    type: "warning",
  })
    .then(async () => {
      //登出
      await adminLogoutService();
      localStorage.removeItem("deviceId");
      //1.清空pinia中儲存的token及個人訊息
      userInfoStore.removeInfo();
      //2.跳轉道登入頁面
      router.push("/admin/login");
      //調用接口
      ElMessage({
        type: "success",
        message: "登出成功",
      });
    })
    .catch(() => {
      // 用戶取消了操作，不需要做事，但避免 Promise 拋錯
      // console.log("使用者取消登出");
    });
}

onMounted(loadRecentBookings);
</script>
<template>
  <div class="ms-auto">
    <ul class="list-unstyled">
      <li class="dropdown pc-h-item">
        <a class="pc-head-link dropdown-toggle arrow-none me-0" data-bs-toggle="dropdown" href="#" role="button"
          aria-haspopup="false" aria-expanded="false" @click="loadRecentBookings">
          <i class="ti ti-mail"></i>
          <span v-if="recentBookingTotal > 0" class="booking-message-badge">{{ recentBookingTotal > 9 ? "9+" : recentBookingTotal }}</span>
        </a>
        <div class="dropdown-menu dropdown-notification dropdown-menu-end pc-h-dropdown">
          <div class="dropdown-header d-flex align-items-center justify-content-between">
            <div>
              <h5 class="m-0">最近預約</h5>
              <span class="text-muted small">共 {{ recentBookingTotal }} 筆訂單</span>
            </div>
            <a href="#!" @click.prevent="loadRecentBookings" class="pc-head-link bg-transparent">
              <i class="ti ti-refresh text-primary"></i>
            </a>
          </div>
          <div class="dropdown-divider"></div>
          <div class="dropdown-header px-0 text-wrap header-notification-scroll position-relative"
            style="max-height: calc(100vh - 215px)">
            <div class="list-group list-group-flush w-100">
              <div v-if="bookingMessageLoading" class="booking-message-empty">預約資料載入中...</div>
              <div v-else-if="recentBookings.length === 0" class="booking-message-empty">目前沒有預約資料</div>
              <router-link
                v-for="booking in recentBookings"
                v-else
                :key="booking.id"
                class="list-group-item list-group-item-action booking-message-item"
                :to="`/admin/booking/detail/${booking.id}`"
              >
                <div class="booking-message-top">
                  <strong>{{ booking.bookingNo }}</strong>
                  <span>{{ formatDateTime(booking.createdTime) }}</span>
                </div>
                <div class="booking-message-main">
                  {{ booking.contactName || "未填聯絡人" }} / {{ booking.checkInDate }} - {{ booking.checkOutDate }}
                </div>
                <div class="booking-message-meta">
                  <span>{{ statusText(booking.status) }}</span>
                  <span>{{ paymentText(booking) }}</span>
                  <span>{{ formatCurrency(booking.totalAmount) }}</span>
                </div>
              </router-link>
            </div>
          </div>
          <div class="dropdown-divider"></div>
          <div class="text-center py-2">
            <router-link to="/admin/booking/list" class="link-primary">查看全部預約</router-link>
          </div>
        </div>
      </li>
      <li class="dropdown pc-h-item header-user-profile">
        <a class="pc-head-link dropdown-toggle arrow-none me-0" data-bs-toggle="dropdown" href="#" role="button"
          aria-haspopup="false" data-bs-auto-close="outside" aria-expanded="false">
          <img src="@/assets/images/user/avatar-2.jpg" alt="user-image" class="user-avtar">
          <span>{{ userInfoStore.info?.firstName
                  }}{{ userInfoStore.info?.lastName }}</span>
        </a>
        <div class="dropdown-menu dropdown-user-profile dropdown-menu-end pc-h-dropdown">
          <div class="dropdown-header">
            <div class="d-flex mb-1">
              <div class="flex-shrink-0">
                <img src="@/assets/images/user/avatar-2.jpg" alt="user-image" class="user-avtar wid-35">
              </div>
              <div class="flex-grow-1 ms-3">
                <h6 class="mb-1">{{ userInfoStore.info?.firstName
                  }}{{ userInfoStore.info?.lastName }}</h6>
                <span>{{ userInfoStore.isCampAdmin ? '營區管理員' : '現場人員' }} · 後台帳號設定</span>
              </div>
            </div>
          </div>
          <div class="tab-content" id="mysrpTabContent">
            <div class="tab-pane fade show active" id="drp-tab-1" role="tabpanel" aria-labelledby="drp-t1" tabindex="0">
              <router-link to="/admin/user/info" class="dropdown-item">
                <i class="ti ti-edit-circle"></i>
                <span>我的資料</span>
              </router-link>
              <router-link to="/admin/user/resetPassword" class="dropdown-item">
                <i class="ti ti-lock"></i>
                <span>修改密碼</span>
              </router-link>
              <a href="#!" @click.prevent="logout" class="dropdown-item">
                <i class="ti ti-power"></i>
                <span>登出</span>
              </a>
            </div>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.booking-message-badge {
  position: absolute;
  top: 6px;
  right: 5px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.booking-message-empty {
  padding: 24px 18px;
  color: #6b7280;
  text-align: center;
}

.booking-message-item {
  width: 100%;
  border: 0;
  text-align: left;
}

.booking-message-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #1f2937;
}

.booking-message-top span,
.booking-message-main,
.booking-message-meta {
  color: #6b7280;
  font-size: 12px;
}

.booking-message-main {
  margin-top: 4px;
}

.booking-message-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 6px;
}

.booking-message-meta span {
  padding: 2px 7px;
  border-radius: 999px;
  background: #f3f4f6;
}
</style>
