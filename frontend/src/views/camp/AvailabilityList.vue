<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminAvailabilityService,
  deleteCampAdminAvailabilityService,
  getCampAdminAvailabilityListService,
  getCampAdminUnitListService,
  type AvailabilityAdminPayload,
  type CampUnitAdminPayload,
  updateCampAdminAvailabilityService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const todayText = () => new Date().toISOString().slice(0, 10);

const emptyForm = (): AvailabilityAdminPayload => ({
  campUnitId: null,
  availableDate: todayText(),
  totalQuantity: 1,
  reservedQuantity: 0,
  blockedQuantity: 0,
  priceOverride: null,
  isClosed: false,
});

const loading = ref(false);
const saving = ref(false);
const rows = ref<AvailabilityAdminPayload[]>([]);
const units = ref<CampUnitAdminPayload[]>([]);
const form = ref<AvailabilityAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);
const filters = ref({
  campUnitId: null as number | null,
  startDate: "",
  endDate: "",
});

const isEditing = computed(() => editingId.value !== null);
const closedCount = computed(() => rows.value.filter((item) => item.isClosed).length);

const unitName = (unitId: number | null) => {
  const unit = units.value.find((item) => item.id === unitId);
  return unit ? unit.name : "-";
};

const remainingQuantity = (row: AvailabilityAdminPayload) =>
  Math.max(0, row.totalQuantity - row.reservedQuantity - row.blockedQuantity);

const formatMoney = (value?: number | null) =>
  value === null || value === undefined
    ? "-"
    : Number(value).toLocaleString("zh-TW", {
        style: "currency",
        currency: "TWD",
        maximumFractionDigits: 0,
      });

const formatDateTime = (value?: string) => value?.replace("T", " ") || "-";

const loadData = async () => {
  loading.value = true;
  try {
    const [unitResponse, availabilityResponse] = await Promise.all([
      getCampAdminUnitListService(),
      getCampAdminAvailabilityListService({
        campUnitId: filters.value.campUnitId,
        startDate: filters.value.startDate || undefined,
        endDate: filters.value.endDate || undefined,
      }),
    ]);
    units.value = unitResponse.data || [];
    rows.value = availabilityResponse.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const editAvailability = (row: AvailabilityAdminPayload) => {
  form.value = cloneDeep(row);
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.campUnitId) {
    ElMessage.error("請選擇營位");
    return false;
  }
  if (!form.value.availableDate) {
    ElMessage.error("請選擇日期");
    return false;
  }
  if (form.value.totalQuantity < 0 || form.value.reservedQuantity < 0 || form.value.blockedQuantity < 0) {
    ElMessage.error("庫存數量不可小於 0");
    return false;
  }
  if (form.value.reservedQuantity + form.value.blockedQuantity > form.value.totalQuantity) {
    ElMessage.error("已保留與封鎖數量不可超過總量");
    return false;
  }
  if (form.value.priceOverride !== null && form.value.priceOverride !== undefined && form.value.priceOverride < 0) {
    ElMessage.error("覆寫價格不可小於 0");
    return false;
  }
  return true;
};

const saveAvailability = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("每日庫存", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminAvailabilityService(form.value);
      ElMessage.success("庫存資料已更新");
    } else {
      await createCampAdminAvailabilityService(form.value);
      ElMessage.success("庫存資料已新增");
    }
    await loadData();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeAvailability = async (row: AvailabilityAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(
    `確定要刪除「${unitName(row.campUnitId)} / ${row.availableDate}」的庫存資料嗎？`,
    "刪除庫存",
    {
      confirmButtonText: "刪除",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  await deleteCampAdminAvailabilityService(row.id);
  ElMessage.success("庫存資料已刪除");
  if (editingId.value === row.id) {
    resetForm();
  }
  await loadData();
};

onMounted(loadData);
</script>

<template>
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
        <li class="breadcrumb-item" aria-current="page">營區管理</li>
        <li class="breadcrumb-item"><router-link to="/admin/camp/availability">庫存管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">庫存管理</h4>
      <p class="text-muted mb-0">維護各營位每日可預約數量、封鎖數量與特殊日期價格。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header">
          <div class="availability-toolbar">
            <div>
              <h5 class="mb-1">每日庫存</h5>
              <p class="text-muted mb-0">共 {{ rows.length }} 筆資料，{{ closedCount }} 筆已關閉。</p>
            </div>
            <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="loading" @click="loadData">
              重新整理
            </button>
          </div>
          <div class="availability-filters mt-3">
            <select v-model.number="filters.campUnitId" class="form-select">
              <option :value="null">全部營位</option>
              <option v-for="unit in units" :key="unit.id" :value="unit.id">
                {{ unit.name }}
              </option>
            </select>
            <input v-model="filters.startDate" class="form-control" type="date" />
            <input v-model="filters.endDate" class="form-control" type="date" />
            <button class="btn btn-dark" type="button" :disabled="loading" @click="loadData">查詢</button>
          </div>
        </div>
        <div class="card-body">
          <div v-if="loading" class="text-muted py-4">資料載入中...</div>
          <div v-else class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>日期</th>
                  <th>營位</th>
                  <th>庫存</th>
                  <th>覆寫價格</th>
                  <th>狀態</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="6" class="text-center text-muted py-4">目前沒有庫存資料</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>{{ row.availableDate }}</td>
                  <td>{{ unitName(row.campUnitId) }}</td>
                  <td>
                    <div>可售 {{ remainingQuantity(row) }} / 總量 {{ row.totalQuantity }}</div>
                    <div class="small text-muted">
                      已保留 {{ row.reservedQuantity }}，封鎖 {{ row.blockedQuantity }}
                    </div>
                  </td>
                  <td>{{ formatMoney(row.priceOverride) }}</td>
                  <td>
                    <span class="badge" :class="row.isClosed ? 'text-bg-danger' : 'text-bg-success'">
                      {{ row.isClosed ? "關閉" : "開放" }}
                    </span>
                    <div class="small text-muted">{{ formatDateTime(row.modifiedTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editAvailability(row)">編輯</button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeAvailability(row)">刪除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <form class="card" @submit.prevent="saveAvailability">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯每日庫存" : "新增每日庫存" }}</h5>
          <p class="text-muted mb-0">同一營位同一天只能有一筆庫存資料。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="availability-unit">營位</label>
            <select id="availability-unit" v-model.number="form.campUnitId" class="form-select">
              <option :value="null">請選擇營位</option>
              <option v-for="unit in units" :key="unit.id" :value="unit.id">
                {{ unit.name }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="availability-date">日期</label>
            <input id="availability-date" v-model="form.availableDate" class="form-control" type="date" />
          </div>
          <div class="availability-number-grid">
            <div class="mb-3">
              <label class="form-label text-muted" for="availability-total">總量</label>
              <input id="availability-total" v-model.number="form.totalQuantity" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="availability-reserved">已保留</label>
              <input id="availability-reserved" v-model.number="form.reservedQuantity" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="availability-blocked">封鎖</label>
              <input id="availability-blocked" v-model.number="form.blockedQuantity" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="availability-price">覆寫價格</label>
              <input id="availability-price" v-model.number="form.priceOverride" class="form-control" type="number" min="0" />
            </div>
          </div>
          <div class="form-check form-switch">
            <input id="availability-closed" v-model="form.isClosed" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="availability-closed">關閉這一天</label>
          </div>
        </div>
        <div class="card-footer">
          <div class="availability-form-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">
              清空
            </button>
            <button class="btn btn-dark" type="submit" :disabled="saving || units.length === 0">
              {{ saving ? "儲存中..." : isEditing ? "儲存修改" : "新增庫存" }}
            </button>
          </div>
          <div v-if="units.length === 0" class="small text-danger mt-3">
            請先建立至少一種營位。
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.availability-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.availability-filters {
  display: grid;
  grid-template-columns: minmax(160px, 1.4fr) 1fr 1fr auto;
  gap: 10px;
}

.availability-number-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 10px;
}

.availability-form-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 768px) {
  .availability-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .availability-filters,
  .availability-number-grid,
  .availability-form-actions {
    grid-template-columns: 1fr;
  }
}
</style>
