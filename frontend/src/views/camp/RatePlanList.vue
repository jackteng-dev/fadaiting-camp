<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminRatePlanService,
  deleteCampAdminRatePlanService,
  getCampAdminRatePlanListService,
  getCampAdminUnitListService,
  type CampUnitAdminPayload,
  type RatePlanAdminPayload,
  updateCampAdminRatePlanService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const emptyForm = (): RatePlanAdminPayload => ({
  campUnitId: null,
  name: "",
  dayType: "weekday",
  basePrice: 0,
  extraPersonPrice: 0,
  cleaningFee: 0,
  description: "",
  isActive: true,
});

const dayTypeOptions = [
  { label: "平日", value: "weekday" },
  { label: "假日", value: "holiday" },
  { label: "旺季", value: "peak" },
  { label: "特殊日", value: "special" },
];

const loading = ref(false);
const saving = ref(false);
const rows = ref<RatePlanAdminPayload[]>([]);
const units = ref<CampUnitAdminPayload[]>([]);
const form = ref<RatePlanAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);

const activeCount = computed(() => rows.value.filter((item) => item.isActive).length);
const inactiveCount = computed(() => rows.value.length - activeCount.value);
const isEditing = computed(() => editingId.value !== null);

const dayTypeText = (value: string) => {
  const option = dayTypeOptions.find((item) => item.value === value);
  return option ? option.label : value;
};

const unitName = (unitId: number | null) => {
  const unit = units.value.find((item) => item.id === unitId);
  return unit ? unit.name : "-";
};

const formatMoney = (value?: number | null) =>
  Number(value || 0).toLocaleString("zh-TW", {
    style: "currency",
    currency: "TWD",
    maximumFractionDigits: 0,
  });

const formatDateTime = (value?: string) => value?.replace("T", " ") || "-";

const loadData = async () => {
  loading.value = true;
  try {
    const [unitResponse, ratePlanResponse] = await Promise.all([
      getCampAdminUnitListService(),
      getCampAdminRatePlanListService(),
    ]);
    units.value = unitResponse.data || [];
    rows.value = ratePlanResponse.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const editRatePlan = (row: RatePlanAdminPayload) => {
  form.value = cloneDeep(row);
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.campUnitId) {
    ElMessage.error("請選擇營位");
    return false;
  }
  if (!form.value.name.trim()) {
    ElMessage.error("請填寫價格方案名稱");
    return false;
  }
  if (form.value.basePrice < 0 || form.value.extraPersonPrice < 0 || form.value.cleaningFee < 0) {
    ElMessage.error("價格不可小於 0");
    return false;
  }
  return true;
};

const saveRatePlan = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("價格方案", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminRatePlanService(form.value);
      ElMessage.success("價格方案已更新");
    } else {
      await createCampAdminRatePlanService(form.value);
      ElMessage.success("價格方案已新增");
    }
    await loadData();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeRatePlan = async (row: RatePlanAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(
    `確定要刪除「${row.name}」嗎？若此價格方案已有預約明細，系統會阻止刪除。`,
    "刪除價格方案",
    {
      confirmButtonText: "刪除",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  await deleteCampAdminRatePlanService(row.id);
  ElMessage.success("價格方案已刪除");
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
        <li class="breadcrumb-item"><router-link to="/admin/camp/rate-plan">價格管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">價格管理</h4>
      <p class="text-muted mb-0">維護各營位的平日、假日與特殊日價格，供預約流程計價使用。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 class="mb-1">價格方案列表</h5>
            <p class="text-muted mb-0">共 {{ rows.length }} 筆方案，{{ activeCount }} 筆啟用。</p>
          </div>
          <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="loading" @click="loadData">
            重新整理
          </button>
        </div>
        <div class="card-body">
          <div v-if="loading" class="text-muted py-4">資料載入中...</div>
          <div v-else class="table-responsive">
            <table class="table align-middle">
              <thead>
                <tr>
                  <th>營位</th>
                  <th>方案</th>
                  <th>類型</th>
                  <th>價格</th>
                  <th>狀態</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="6" class="text-center text-muted py-4">目前沒有價格方案</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>{{ unitName(row.campUnitId) }}</td>
                  <td>
                    <strong>{{ row.name }}</strong>
                    <div class="small text-muted">{{ row.description || "尚未填寫描述" }}</div>
                  </td>
                  <td>{{ dayTypeText(row.dayType) }}</td>
                  <td>
                    <div>{{ formatMoney(row.basePrice) }}</div>
                    <div class="small text-muted">
                      加人 {{ formatMoney(row.extraPersonPrice) }} / 清潔 {{ formatMoney(row.cleaningFee) }}
                    </div>
                  </td>
                  <td>
                    <span class="badge" :class="row.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ row.isActive ? "啟用" : "停用" }}
                    </span>
                    <div class="small text-muted">{{ formatDateTime(row.modifiedTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editRatePlan(row)">編輯</button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeRatePlan(row)">刪除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <form class="card" @submit.prevent="saveRatePlan">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯價格方案" : "新增價格方案" }}</h5>
          <p class="text-muted mb-0">已有預約使用的價格方案不能刪除，可改為停用。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="rate-unit">營位</label>
            <select id="rate-unit" v-model.number="form.campUnitId" class="form-select">
              <option :value="null">請選擇營位</option>
              <option v-for="unit in units" :key="unit.id" :value="unit.id">
                {{ unit.name }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="rate-name">方案名稱</label>
            <input id="rate-name" v-model="form.name" class="form-control" type="text" placeholder="例如：草地假日價" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="rate-day-type">日別</label>
            <select id="rate-day-type" v-model="form.dayType" class="form-select">
              <option v-for="option in dayTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
          <div class="rate-number-grid">
            <div class="mb-3">
              <label class="form-label text-muted" for="rate-base">基本價格</label>
              <input id="rate-base" v-model.number="form.basePrice" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="rate-extra">加人費</label>
              <input id="rate-extra" v-model.number="form.extraPersonPrice" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="rate-cleaning">清潔費</label>
              <input id="rate-cleaning" v-model.number="form.cleaningFee" class="form-control" type="number" min="0" />
            </div>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="rate-description">描述</label>
            <textarea id="rate-description" v-model="form.description" class="form-control" rows="4"></textarea>
          </div>
          <div class="form-check form-switch">
            <input id="rate-active" v-model="form.isActive" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="rate-active">啟用這個價格方案</label>
          </div>
        </div>
        <div class="card-footer">
          <div class="rate-form-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">
              清空
            </button>
            <button class="btn btn-dark" type="submit" :disabled="saving || units.length === 0">
              {{ saving ? "儲存中..." : isEditing ? "儲存修改" : "新增方案" }}
            </button>
          </div>
          <div v-if="units.length === 0" class="small text-danger mt-3">
            請先建立至少一種營位。
          </div>
          <div v-else-if="inactiveCount > 0" class="small text-muted mt-3">
            目前有 {{ inactiveCount }} 筆停用方案。
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.rate-number-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 10px;
}

.rate-form-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 768px) {
  .rate-number-grid,
  .rate-form-actions {
    grid-template-columns: 1fr;
  }
}
</style>
