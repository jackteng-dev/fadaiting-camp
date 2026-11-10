<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import {
  createDepartureReminderService,
  getNotificationQueueService, getNotificationAttemptsService, simulateQueuedNotificationService,
  type NotificationQueueStatus, type NotificationAttempt,
  getNotificationListService,
  sendQueuedNotificationService,
  updateNotificationStatusService,
  type NotificationAdminQuery,
  type NotificationAdminRow,
} from "@/api/booking";
import { confirmAdminAction } from "@/utils/confirmAction";
import NotificationRetentionPreview from '@/components/booking/NotificationRetentionPreview.vue';

const router = useRouter();
const loading = ref(false);
const sending = ref(false);
const operatingId = ref<number>();
const queueStatus = ref<NotificationQueueStatus>();
const queueUnavailable = ref(false);
const listUnavailable = ref(false);
let loadVersion = 0;
const selectedAttempts = ref<NotificationAttempt[] | null>(null);
const attemptsLoading = ref(false);
const creatingReminders = ref(false);
const rows = ref<NotificationAdminRow[]>([]);
const total = ref(0);
const selectedPayload = ref("");
const selectedMessage = ref("");
const filters = reactive<NotificationAdminQuery>({
  pageNum: 1,
  pageSize: 10,
  bookingNo: "",
  eventType: "",
  channel: "",
  status: "",
});
const reminderForm = reactive({
  targetDate: "",
  daysBefore: 3,
});

const eventOptions = [
  { label: "預約送出", value: "booking_submitted" },
  { label: "後台新訂單提醒", value: "admin_new_booking" },
  { label: "付款成功", value: "payment_confirmed" },
  { label: "後台付款提醒", value: "admin_payment_confirmed" },
  { label: "出發前提醒", value: "departure_reminder" },
];

const channelOptions = [
  { label: "Email", value: "email" },
  { label: "站內提醒", value: "internal" },
  { label: "人工", value: "manual" },
];

const statusOptions = [
  { label: "待處理", value: "queued" },
  { label: "處理中", value: "processing" },
  { label: "等待重試", value: "retry_wait" },
  { label: "租約到期", value: "lease_expired" },
  { label: "模擬完成（未寄信）", value: "simulated" },
  { label: "站內已確認", value: "acknowledged" },
  { label: "已發送", value: "sent" },
  { label: "發送失敗", value: "failed" },
  { label: "略過", value: "skipped" },
];

const maxPage = computed(() => Math.max(1, Math.ceil(total.value / filters.pageSize)));

const eventText = (value: string) => eventOptions.find((item) => item.value === value)?.label || value;
const channelText = (value: string) => channelOptions.find((item) => item.value === value)?.label || value;
const formatDateTime = (value?: string) => {
  if (!value) return "-";
  if (/Z$|[+-]\d{2}:\d{2}$/.test(value)) return new Intl.DateTimeFormat("zh-TW", {timeZone:"Asia/Taipei",dateStyle:"short",timeStyle:"short",hour12:false}).format(new Date(value));
  return value.replace("T", " ").slice(0,16);
};
const statusText = (status: string) => statusOptions.find(item => item.value === status)?.label || status;
const errorText = (code?: string) => ({SMTP_UNAVAILABLE:"SMTP 暫時無法使用", SMTP_AUTH:"SMTP 身分設定錯誤", INVALID_RECIPIENT:"收件 Email 無效", INVALID_CONTENT:"通知內容無效", DELIVERY_UNCERTAIN:"寄送結果不明，請先人工核對再重試", LEASE_EXPIRED:"處理中斷，等待重新處理", OBSOLETE_BOOKING:"預約狀態已變更，通知略過"}[code || ""] || code || "");
const payloadPreview = (payload?: string) => payload ? (payload.length > 72 ? `${payload.slice(0, 72)}...` : payload) : "-";

const loadNotifications = async () => {
  const version = ++loadVersion;
  loading.value = true;
  queueStatus.value = undefined;
  queueUnavailable.value = false;
  listUnavailable.value = false;
  try {
    const [res, status] = await Promise.allSettled([getNotificationListService({...filters}), getNotificationQueueService()]);
    if (version !== loadVersion) return;
    if (status.status === "fulfilled") queueStatus.value = status.value.data;
    else queueUnavailable.value = true;
    if (res.status === "fulfilled") {
      rows.value = res.value.data.items || [];
      total.value = res.value.data.total || 0;
    } else {
      listUnavailable.value = true;
      rows.value = [];
      total.value = 0;
    }
  } finally {
    if (version === loadVersion) loading.value = false;
  }
};

const onSearch = async () => {
  filters.pageNum = 1;
  await loadNotifications();
};

const resetFilters = async () => {
  filters.pageNum = 1;
  filters.pageSize = 10;
  filters.bookingNo = "";
  filters.eventType = "";
  filters.channel = "";
  filters.status = "";
  await loadNotifications();
};

const goPage = async (direction: "prev" | "next") => {
  if (direction === "prev" && filters.pageNum > 1) {
    filters.pageNum -= 1;
  }
  if (direction === "next" && filters.pageNum < maxPage.value) {
    filters.pageNum += 1;
  }
  await loadNotifications();
};

const openBooking = (row: NotificationAdminRow) => {
  router.push(`/admin/booking/detail/${row.bookingId}`);
};

const updateStatus = async (row: NotificationAdminRow, status: string) => {
  if (operatingId.value !== undefined) return;
  let reason: string;
  try {
    const answer = await ElMessageBox.prompt(`通知 #${row.id}：${status === "queued" ? "重新排入佇列。若曾嘗試真實寄信，請先核對是否已寄達，避免重複通知。" : statusText(status)} 請填操作原因。`, "確認通知操作", {
      confirmButtonText: status === "queued" ? "重新排入" : "確認操作", cancelButtonText: "取消",
      inputValidator: value => !!value?.trim() && value.trim().length <= 300 || "請填 1 至 300 字的原因",
    });
    reason = answer.value.trim();
  } catch { return; }
  operatingId.value = row.id;
  try {
    await updateNotificationStatusService({notificationId:row.id, status, expectedVersion:row.operationVersion, reason});
    ElMessage.success("通知操作已完成");
  } finally { operatingId.value = undefined; await loadNotifications(); }
};
const viewAttempts = async (row: NotificationAdminRow) => {
  selectedAttempts.value = []; attemptsLoading.value = true;
  try { selectedAttempts.value = (await getNotificationAttemptsService(row.id)).data; }
  finally { attemptsLoading.value = false; }
};
const processQueued = async (simulate: boolean) => {
  const scope = filters.bookingNo?.trim();
  if (!(await confirmAdminAction({
    title: simulate ? "免費模擬處理" : "確認發送通知",
    message: `${simulate ? "模擬處理，不寄出任何 Email" : "透過設定的 SMTP 寄送真實 Email"}，最多 20 筆。${scope ? `僅處理完整訂單編號 ${scope}。` : "處理全部到期的通知。"}`,
    confirmButtonText: simulate ? "開始模擬" : "開始發送",
  }))) return;
  sending.value = true;
  try {
    const res = await (simulate ? simulateQueuedNotificationService : sendQueuedNotificationService)({limit:20, bookingNo:scope || undefined});
    const data = res.data;
    ElMessage.success(`處理 ${data.processedCount} 筆，已寄 ${data.sentCount}、模擬 ${data.simulatedCount}、待重試 ${data.retryCount}、失敗 ${data.failedCount}、略過 ${data.skippedCount}、待核對 ${data.unresolvedCount}`);
    await loadNotifications();
  } finally { sending.value = false; }
};

const createDepartureReminders = async () => {
  if (
    !(await confirmAdminAction({
      title: "確認產生出發前提醒",
      message: "確定要依目前條件產生出發前提醒通知嗎？",
      confirmButtonText: "產生提醒",
    }))
  ) {
    return;
  }

  creatingReminders.value = true;
  try {
    const res = await createDepartureReminderService({
      targetDate: reminderForm.targetDate || undefined,
      daysBefore: reminderForm.daysBefore,
    });
    const data = res.data;
    ElMessage.success(`已檢查 ${data.processedCount} 筆，新增 ${data.createdCount} 筆，略過 ${data.skippedCount} 筆`);
    await loadNotifications();
  } finally {
    creatingReminders.value = false;
  }
};

onMounted(loadNotifications);
</script>

<template>
  <div class="row">
    <div class="col-12">
      <div class="card">
        <div class="card-header">
          <div class="notification-header">
            <div>
              <h4 class="mb-1">通知紀錄</h4>
              <p class="text-muted mb-0">查看預約與付款通知、處理進度及重試紀錄。模擬完成不代表已寄出 Email。</p>
            </div>
            <div class="notification-actions">
              <input
                v-model="reminderForm.targetDate"
                class="form-control form-control-sm reminder-date"
                type="date"
                title="指定入住日期，空白時使用天數"
              />
              <button
                class="btn btn-outline-warning btn-sm"
                type="button"
                :disabled="creatingReminders"
                @click="createDepartureReminders"
              >
                {{ creatingReminders ? "產生中..." : "產生出發前提醒" }}
              </button>
              <button class="btn btn-outline-success btn-sm" type="button" :disabled="sending" @click="processQueued(true)">
                {{ sending ? "處理中..." : "免費模擬處理" }}
              </button>
              <button v-if="queueStatus?.deliveryMode === 'smtp'" class="btn btn-outline-danger btn-sm" type="button" :disabled="sending" @click="processQueued(false)">發送待寄 Email</button>
              <button class="btn btn-outline-primary btn-sm" type="button" @click="router.push('/admin/booking/list')">
                回預約管理
              </button>
              <div class="notification-total">{{ loading ? '載入中...' : listUnavailable ? '列表未載入' : `共 ${total} 筆` }}</div>
            </div>
          </div>
        </div>

        <div class="card-body">
          <NotificationRetentionPreview />
          <section class="queue-observation" aria-label="通知佇列營運觀測">
            <div class="queue-observation__head">
              <h5 class="mb-0">佇列營運觀測</h5>
              <button class="btn btn-outline-primary btn-sm" type="button" :disabled="loading" @click="loadNotifications">{{ loading ? '更新中...' : '更新觀測' }}</button>
            </div>
            <p class="small text-muted">全站新版 Email 佇列，各處理模式合計；不受下方列表篩選影響。更新觀測只讀取資料。</p>
            <p v-if="loading" role="status">正在取得最新觀測...</p>
            <p v-else-if="queueUnavailable" class="text-danger" role="alert">觀測暫時無法取得，請重新更新；目前不能判定佇列狀態。</p>
            <template v-else-if="queueStatus">
              <div class="queue-summary" role="status">
                <strong>{{ queueStatus.deliveryMode === 'smtp' ? 'SMTP 模式（送達仍需核驗）' : queueStatus.deliveryMode === 'simulate' ? '模擬模式（未寄信）' : '真實寄信關閉，可免費模擬' }}</strong>
                <span>自動處理{{ queueStatus.workerEnabled && queueStatus.deliveryMode !== 'disabled' ? '開啟' : '關閉' }} · 等待重試 {{ queueStatus.waitingCount }} · 處理中 {{ queueStatus.processingCount }}</span>
                <span>觀測時間 {{ formatDateTime(queueStatus.observedAt) }}（台北）</span>
              </div>
              <div class="queue-metrics">
                <div><span>到期待處理</span><strong>{{ queueStatus.dueCount }}</strong></div>
                <div><span>等待過久</span><strong>{{ queueStatus.overdueCount }}</strong></div>
                <div><span>租約到期</span><strong>{{ queueStatus.expiredLeaseCount }}</strong></div>
                <div><span>寄送結果不明</span><strong>{{ queueStatus.uncertainCount }}</strong></div>
                <div><span>發送失敗（含結果不明）</span><strong>{{ queueStatus.failedCount }}</strong></div>
                <div><span>無法自動領取</span><strong>{{ queueStatus.blockedCount }}</strong></div>
              </div>
              <p class="small mt-2">最早到期 {{ formatDateTime(queueStatus.oldestDueAt) }} · 已等待 {{ queueStatus.oldestDueAt ? `${Math.floor(queueStatus.oldestDueAgeSeconds / 60)} 分鐘` : '—' }} · 等待告警門檻 {{ queueStatus.overdueThresholdSeconds / 60 }} 分鐘<br />最早逾期租約 {{ formatDateTime(queueStatus.oldestExpiredLeaseAt) }}。各指標可能重疊，合計不代表任務總數。</p>
              <p v-if="queueStatus.health === 'healthy'" class="text-success" role="status">目前沒有佇列告警；不代表 Email 已送達或外部服務可用。</p>
              <div v-for="alert in queueStatus.alerts" :key="alert.code" class="queue-alert" :class="`queue-alert--${alert.severity}`" :role="alert.severity === 'info' ? 'status' : 'alert'">
                <strong>{{ alert.severity === 'critical' ? '需優先核對' : alert.severity === 'warning' ? '需注意' : '狀態提示' }} · {{ alert.message }}（{{ alert.count }} 筆）</strong>
                <p class="mb-0">{{ alert.action }}</p>
              </div>
            </template>
          </section>
          <div class="notification-filters">
            <input v-model="filters.bookingNo" class="form-control" type="text" placeholder="訂單編號" />
            <select v-model="filters.eventType" class="form-select">
              <option value="">全部事件</option>
              <option v-for="option in eventOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <select v-model="filters.channel" class="form-select">
              <option value="">全部通道</option>
              <option v-for="option in channelOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <select v-model="filters.status" class="form-select">
              <option value="">全部狀態</option>
              <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <button class="btn btn-primary" type="button" @click="onSearch">查詢</button>
            <button class="btn btn-outline-secondary" type="button" @click="resetFilters">清空</button>
          </div>

          <div v-if="loading" class="notification-loading">通知資料載入中...</div>

          <div v-else class="table-responsive">
            <div class="small text-muted mb-2">表格可左右捲動，查看完整狀態、內容與操作。</div>
            <table class="table align-middle notification-table">
              <thead>
                <tr>
                  <th>通知</th>
                  <th>訂單</th>
                  <th>事件 / 通道</th>
                  <th>收件對象</th>
                  <th>狀態</th>
                  <th>內容</th>
                  <th>時間</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="listUnavailable">
                  <td colspan="8" class="text-center text-danger py-4" role="alert">通知列表暫時無法取得，請重新查詢。</td>
                </tr>
                <tr v-else-if="rows.length === 0">
                  <td colspan="8" class="text-center text-muted py-4">目前沒有通知紀錄</td>
                </tr>
                <tr v-for="row in rows" :key="row.id">
                  <td>
                    <strong>#{{ row.id }}</strong>
                    <div class="small text-muted">{{ row.subject || "-" }}</div>
                  </td>
                  <td>
                    <button class="booking-link" type="button" @click="openBooking(row)">{{ row.bookingNo }}</button>
                    <div class="small text-muted">{{ row.contactName || "-" }} / {{ row.contactPhone || "-" }}</div>
                  </td>
                  <td>
                    <div class="event-chip">{{ eventText(row.eventType) }}</div>
                    <div class="small text-muted">{{ channelText(row.channel) }}</div>
                  </td>
                  <td>
                    <div>{{ row.recipientName || row.contactName || "-" }}</div>
                    <div class="small text-muted">{{ row.recipient || "-" }}</div>
                  </td>
                  <td>
                    <strong>{{ statusText(row.status) }}</strong>
                    <div class="small text-muted">嘗試 {{ row.attemptCount }} / {{ row.attemptLimit }}</div>
                    <div v-if="row.nextAttemptAt" class="small">下次 {{ formatDateTime(row.nextAttemptAt) }}</div>
                    <div v-if="row.errorCode" class="small text-danger">{{ errorText(row.errorCode) }}</div>
                    <div v-if="!row.queueManaged" class="small text-muted">舊紀錄，未自動排入</div>
                    <button v-if="row.queueManaged && row.channel === 'email' && row.status === 'failed'" class="btn btn-outline-warning btn-sm mt-2" :disabled="operatingId !== undefined" @click="updateStatus(row, 'queued')">重新排入</button>
                    <button v-if="row.channel === 'internal' && row.status === 'queued'" class="btn btn-outline-primary btn-sm mt-2" :disabled="operatingId !== undefined" @click="updateStatus(row, 'acknowledged')">確認站內提醒</button>
                    <button v-if="['queued', 'retry_wait', 'failed'].includes(row.status)" class="btn btn-outline-secondary btn-sm mt-2 ms-1" :disabled="operatingId !== undefined" @click="updateStatus(row, 'skipped')">略過</button>
                  </td>
                  <td>
                    <div class="message-preview">{{ row.message ? `${row.message.slice(0, 48)}${row.message.length > 48 ? '...' : ''}` : "-" }}</div>
                    <div class="payload-preview">{{ payloadPreview(row.payload) }}</div>
                  </td>
                  <td>
                    <div>建立 {{ formatDateTime(row.createdTime) }}</div>
                    <div class="small text-muted">發送 {{ formatDateTime(row.sentTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm mb-1" type="button" @click="viewAttempts(row)">嘗試紀錄</button>
                    <button class="btn btn-outline-secondary btn-sm" type="button" @click="selectedMessage = row.message || ''">
                      內容
                    </button>
                    <button class="btn btn-outline-secondary btn-sm ms-2" type="button" @click="selectedPayload = row.payload || ''">
                      payload
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="!listUnavailable" class="notification-pagination">
            <span>共 {{ total }} 筆，第 {{ filters.pageNum }} / {{ maxPage }} 頁</span>
            <div>
              <button class="btn btn-outline-secondary btn-sm" type="button" @click="goPage('prev')">上一頁</button>
              <button class="btn btn-outline-secondary btn-sm ms-2" type="button" @click="goPage('next')">下一頁</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selectedAttempts !== null" class="payload-modal" role="dialog" aria-label="通知嘗試紀錄">
      <div class="payload-dialog">
        <div class="payload-dialog__head"><h5 class="mb-0">通知嘗試紀錄</h5><button class="btn-close" type="button" aria-label="關閉" @click="selectedAttempts = null"></button></div>
        <div class="attempt-body">
          <p v-if="attemptsLoading">載入中...</p><p v-else-if="!selectedAttempts.length">尚無處理嘗試；舊紀錄不補寫歷史。</p>
          <div v-for="attempt in selectedAttempts" :key="attempt.id" class="attempt-row">
            <strong>第 {{ attempt.attemptNumber }} 次 · {{ attempt.deliveryMode === 'simulate' ? '模擬（未寄信）' : 'SMTP' }} · {{ statusText(attempt.status) }}</strong>
            <div>{{ formatDateTime(attempt.startedAt) }} → {{ formatDateTime(attempt.finishedAt) }} · {{ attempt.actorName }}</div>
            <div v-if="attempt.errorCode" class="text-danger">{{ errorText(attempt.errorCode) }}</div>
          </div>
        </div>
      </div>
    </div>
    <div v-if="selectedMessage" class="payload-modal">
      <div class="payload-dialog">
        <div class="payload-dialog__head">
          <h5 class="mb-0">通知內容</h5>
          <button class="btn-close" type="button" aria-label="關閉" @click="selectedMessage = ''"></button>
        </div>
        <pre>{{ selectedMessage }}</pre>
      </div>
    </div>

    <div v-if="selectedPayload" class="payload-modal">
      <div class="payload-dialog">
        <div class="payload-dialog__head">
          <h5 class="mb-0">通知 Payload</h5>
          <button class="btn-close" type="button" aria-label="關閉" @click="selectedPayload = ''"></button>
        </div>
        <pre>{{ selectedPayload }}</pre>
      </div>
    </div>
  </div>
</template>

<style scoped>
.queue-observation { margin-bottom: 22px; padding: 16px; border: 1px solid #dce3d7; border-radius: 10px; }
.queue-observation__head { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 8px; }
.queue-metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; }
.queue-metrics > div { padding: 12px; background: #f5f7f2; border-radius: 8px; }
.queue-metrics span { display: block; font-size: 13px; }
.queue-metrics strong { display: block; font-size: 24px; }
.queue-alert { margin-top: 10px; padding: 12px; border-left: 4px solid #56809a; background: #eef5f9; overflow-wrap: anywhere; }
.queue-alert--warning { border-color: #9b710c; background: #fff7df; }
.queue-alert--critical { border-color: #b43434; background: #fff0f0; }
.queue-summary { display: flex; flex-wrap: wrap; gap: 8px 20px; padding: 14px; margin-bottom: 16px; background: #f5f7ef; border-radius: 8px; }
.attempt-body { padding: 18px; overflow: auto; }
.attempt-row { padding: 12px 0; border-bottom: 1px solid #eaecf0; }
.notification-table { min-width: 1420px; table-layout: fixed; }
.notification-table th:nth-child(1) { width: 130px; }
.notification-table th:nth-child(2) { width: 150px; }
.notification-table th:nth-child(3) { width: 120px; }
.notification-table th:nth-child(4) { width: 150px; }
.notification-table th:nth-child(5) { width: 220px; }
.notification-table th:nth-child(6) { width: 240px; }
.notification-table th:nth-child(7) { width: 165px; }
.notification-table th:nth-child(8) { width: 245px; }
.notification-table td { overflow-wrap: anywhere; }
.notification-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.notification-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.notification-total {
  min-height: 40px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f5f7ef;
  color: #50663e;
  font-weight: 700;
}

.reminder-date {
  width: 150px;
}

.notification-filters {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr 1fr auto auto;
  gap: 12px;
  margin-bottom: 18px;
}

.notification-loading {
  padding: 24px 0;
  color: #6b7280;
}

.event-chip {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #eef4ff;
  color: #175cd3;
  font-size: 12px;
  font-weight: 700;
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

.message-preview,
.payload-preview {
  max-width: 280px;
  color: #667085;
  font-size: 12px;
  word-break: break-all;
}

.notification-pagination {
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
  .notification-filters {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .notification-header,
  .notification-actions,
  .notification-pagination {
    display: grid;
    justify-content: stretch;
  }

  .notification-filters {
    grid-template-columns: 1fr;
  }

  .reminder-date {
    width: 100%;
  }
}
</style>
