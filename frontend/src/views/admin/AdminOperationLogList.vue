<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import {
  getAdminOperationLogListService,
  type AdminOperationLog,
  type AdminOperationLogQuery,
} from "@/api/adminOperation";

const loading = ref(false);
const rows = ref<AdminOperationLog[]>([]);
const total = ref(0);
const filters = reactive<AdminOperationLogQuery>({
  pageNum: 1,
  pageSize: 20,
  username: "",
  actionType: "",
  success: "",
  requestPath: "",
  startDate: "",
  endDate: "",
});

const actionOptions = [
  { label: "登入", value: "login" },
  { label: "登出", value: "logout" },
  { label: "查看", value: "view" },
  { label: "新增 / 執行", value: "create" },
  { label: "更新", value: "update" },
  { label: "刪除", value: "delete" },
  { label: "操作", value: "operate" },
];

const maxPage = computed(() => Math.max(1, Math.ceil(total.value / filters.pageSize)));

const formatDateTime = (value?: string) => value?.replace("T", " ").slice(0, 19) || "-";

const resultText = (success: boolean) => (success ? "成功" : "失敗");

const methodClass = (method?: string) => {
  const normalized = (method || "").toLowerCase();
  return `method-badge method-badge--${normalized || "default"}`;
};

const actionText = (actionType: string, actionLabel: string) => {
  const match = actionOptions.find((item) => item.value === actionType);
  return actionLabel || match?.label || actionType;
};

const trimText = (value?: string, limit = 80) => {
  if (!value) return "-";
  return value.length > limit ? `${value.slice(0, limit)}...` : value;
};

const queryPayload = () => ({
  ...filters,
  success: filters.success === "" ? undefined : filters.success,
});

const loadLogs = async () => {
  loading.value = true;
  try {
    const response = await getAdminOperationLogListService(queryPayload());
    rows.value = response.data.items || [];
    total.value = response.data.total || 0;
  } finally {
    loading.value = false;
  }
};

const onSearch = async () => {
  filters.pageNum = 1;
  await loadLogs();
};

const resetFilters = async () => {
  filters.pageNum = 1;
  filters.pageSize = 20;
  filters.username = "";
  filters.actionType = "";
  filters.success = "";
  filters.requestPath = "";
  filters.startDate = "";
  filters.endDate = "";
  await loadLogs();
};

const goPage = async (direction: "prev" | "next") => {
  if (direction === "prev" && filters.pageNum > 1) {
    filters.pageNum -= 1;
  }
  if (direction === "next" && filters.pageNum < maxPage.value) {
    filters.pageNum += 1;
  }
  await loadLogs();
};

onMounted(loadLogs);
</script>

<template>
  <div class="row">
    <div class="col-12">
      <div class="card">
        <div class="card-header">
          <div class="operation-header">
            <div>
              <h4 class="mb-1">操作紀錄</h4>
              <p class="text-muted mb-0">查看後台使用者登入、登出，以及各管理功能的查詢、新增、更新與刪除紀錄。</p>
            </div>
            <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="loading" @click="loadLogs">
              重新整理
            </button>
          </div>
        </div>

        <div class="card-body">
          <div class="operation-filters">
            <input v-model="filters.username" class="form-control" type="text" placeholder="後台帳號" />
            <select v-model="filters.actionType" class="form-select">
              <option value="">全部操作</option>
              <option v-for="option in actionOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
            <select v-model="filters.success" class="form-select">
              <option value="">全部結果</option>
              <option :value="true">成功</option>
              <option :value="false">失敗</option>
            </select>
            <input v-model="filters.requestPath" class="form-control" type="text" placeholder="操作內容或 API 路徑" />
            <input v-model="filters.startDate" class="form-control" type="date" />
            <input v-model="filters.endDate" class="form-control" type="date" />
            <button class="btn btn-primary" type="button" :disabled="loading" @click="onSearch">查詢</button>
            <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="resetFilters">清空</button>
          </div>

          <div v-if="loading" class="text-muted py-4">操作紀錄載入中...</div>

          <div v-else class="table-responsive">
            <table class="table align-middle operation-table">
              <thead>
                <tr>
                  <th>時間</th>
                  <th>使用者</th>
                  <th>操作內容</th>
                  <th>API</th>
                  <th>來源</th>
                  <th>結果</th>
                  <th>訊息</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="7" class="text-center text-muted py-4">目前沒有操作紀錄</td>
                </tr>
                <tr v-for="row in rows" :key="row.id">
                  <td>
                    <strong>{{ formatDateTime(row.createdTime) }}</strong>
                    <small>#{{ row.id }}</small>
                  </td>
                  <td>
                    <strong>{{ row.username || "未登入" }}</strong>
                    <small v-if="row.userId">ID {{ row.userId }}</small>
                  </td>
                  <td>
                    <strong>{{ actionText(row.actionType, row.actionLabel) }}</strong>
                  </td>
                  <td>
                    <div class="api-cell">
                      <span :class="methodClass(row.requestMethod)">{{ row.requestMethod }}</span>
                      <span>{{ row.requestPath }}</span>
                    </div>
                    <small v-if="row.queryString">{{ trimText(row.queryString, 60) }}</small>
                  </td>
                  <td>
                    <span>{{ row.ipAddress || "-" }}</span>
                    <small>{{ trimText(row.userAgent, 48) }}</small>
                  </td>
                  <td>
                    <span class="result-chip" :class="{ 'result-chip--fail': !row.success }">
                      {{ resultText(row.success) }}
                    </span>
                    <small>HTTP {{ row.statusCode || "-" }}</small>
                  </td>
                  <td>{{ trimText(row.message, 100) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div class="operation-pager">
            <span>共 {{ total }} 筆，第 {{ filters.pageNum }} / {{ maxPage }} 頁</span>
            <div>
              <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="filters.pageNum <= 1 || loading" @click="goPage('prev')">
                上一頁
              </button>
              <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="filters.pageNum >= maxPage || loading" @click="goPage('next')">
                下一頁
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.operation-header,
.operation-pager,
.api-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.operation-header,
.operation-pager {
  justify-content: space-between;
}

.operation-filters {
  display: grid;
  grid-template-columns: 1.1fr 140px 120px 1.4fr 150px 150px auto auto;
  gap: 10px;
  margin-bottom: 18px;
}

.operation-table td {
  vertical-align: top;
}

.operation-table td strong,
.operation-table td small {
  display: block;
}

.operation-table td small {
  margin-top: 4px;
  color: #667085;
}

.result-chip,
.method-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.result-chip {
  background: #ecfdf3;
  color: #027a48;
}

.result-chip--fail {
  background: #fef3f2;
  color: #b42318;
}

.method-badge {
  min-width: 54px;
  justify-content: center;
  background: #f2f4f7;
  color: #344054;
}

.method-badge--post {
  background: #eff8ff;
  color: #175cd3;
}

.method-badge--put,
.method-badge--patch {
  background: #fffaeb;
  color: #b54708;
}

.method-badge--delete {
  background: #fef3f2;
  color: #b42318;
}

.operation-pager {
  margin-top: 16px;
}

.operation-pager div {
  display: flex;
  gap: 8px;
}

@media (max-width: 1200px) {
  .operation-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .operation-header,
  .operation-pager {
    display: grid;
    justify-content: stretch;
  }

  .operation-filters {
    grid-template-columns: 1fr;
  }
}
</style>
