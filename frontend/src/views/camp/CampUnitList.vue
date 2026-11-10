<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminUnitService,
  deleteCampAdminUnitService,
  getCampAdminUnitListService,
  getCampAdminZoneListService,
  type CampUnitAdminPayload,
  type CampZoneAdminPayload,
  updateCampAdminUnitService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const emptyForm = (): CampUnitAdminPayload => ({
  zoneId: null,
  name: "",
  unitType: "tent_site",
  description: "",
  maxGuests: 4,
  maxVehicles: 1,
  maxTents: 1,
  hasShelter: false,
  electricityAvailable: false,
  privateBathroom: false,
  quantity: 1,
  sortOrder: 0,
  isActive: true,
});

const unitTypeOptions = [
  { label: "自搭帳營位", value: "tent_site" },
  { label: "雨棚營位", value: "shelter_site" },
  { label: "免搭帳", value: "glamping_tent" },
  { label: "小木屋", value: "cabin" },
  { label: "其他", value: "other" },
];

const loading = ref(false);
const saving = ref(false);
const rows = ref<CampUnitAdminPayload[]>([]);
const zones = ref<CampZoneAdminPayload[]>([]);
const form = ref<CampUnitAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);

const activeCount = computed(() => rows.value.filter((item) => item.isActive).length);
const inactiveCount = computed(() => rows.value.length - activeCount.value);
const isEditing = computed(() => editingId.value !== null);

const unitTypeText = (value: string) => {
  const option = unitTypeOptions.find((item) => item.value === value);
  return option ? option.label : value;
};

const zoneName = (zoneId: number | null) => {
  const zone = zones.value.find((item) => item.id === zoneId);
  return zone ? zone.name : "-";
};

const formatDateTime = (value?: string) => value?.replace("T", " ") || "-";

const loadData = async () => {
  loading.value = true;
  try {
    const [zoneResponse, unitResponse] = await Promise.all([
      getCampAdminZoneListService(),
      getCampAdminUnitListService(),
    ]);
    zones.value = zoneResponse.data || [];
    rows.value = unitResponse.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const editUnit = (row: CampUnitAdminPayload) => {
  form.value = cloneDeep(row);
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.zoneId) {
    ElMessage.error("請選擇所屬區域");
    return false;
  }
  if (!form.value.name.trim()) {
    ElMessage.error("請填寫營位名稱");
    return false;
  }
  return true;
};

const saveUnit = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("營位", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminUnitService(form.value);
      ElMessage.success("營位資料已更新");
    } else {
      await createCampAdminUnitService(form.value);
      ElMessage.success("營位已新增");
    }
    await loadData();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeUnit = async (row: CampUnitAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(
    `確定要刪除「${row.name}」嗎？若此營位已有價格、庫存或預約資料，系統會阻止刪除。`,
    "刪除營位",
    {
      confirmButtonText: "刪除",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  await deleteCampAdminUnitService(row.id);
  ElMessage.success("營位已刪除");
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
        <li class="breadcrumb-item"><router-link to="/admin/camp/unit">營位管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">營位管理</h4>
      <p class="text-muted mb-0">維護各區域底下可被預約或展示的營位種類、容量、設備與前台狀態。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 class="mb-1">營位列表</h5>
            <p class="text-muted mb-0">共 {{ rows.length }} 種營位，{{ activeCount }} 種前台啟用。</p>
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
                  <th>排序</th>
                  <th>營位名稱</th>
                  <th>區域</th>
                  <th>容量</th>
                  <th>設備</th>
                  <th>數量</th>
                  <th>狀態</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="8" class="text-center text-muted py-4">目前沒有營位資料</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>{{ row.sortOrder }}</td>
                  <td>
                    <strong>{{ row.name }}</strong>
                    <div class="small text-muted">{{ unitTypeText(row.unitType) }}</div>
                    <div class="small text-muted">{{ row.description || "尚未填寫描述" }}</div>
                  </td>
                  <td>{{ zoneName(row.zoneId) }}</td>
                  <td>
                    {{ row.maxGuests }} 人 / {{ row.maxVehicles }} 車 / {{ row.maxTents }} 帳
                  </td>
                  <td>
                    <span v-if="row.hasShelter" class="badge text-bg-info me-1">雨棚</span>
                    <span v-if="row.electricityAvailable" class="badge text-bg-warning me-1">供電</span>
                    <span v-if="row.privateBathroom" class="badge text-bg-primary me-1">獨立衛浴</span>
                    <span v-if="!row.hasShelter && !row.electricityAvailable && !row.privateBathroom" class="text-muted">
                      -
                    </span>
                  </td>
                  <td>{{ row.quantity }}</td>
                  <td>
                    <span class="badge" :class="row.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ row.isActive ? "啟用" : "停用" }}
                    </span>
                    <div class="small text-muted">{{ formatDateTime(row.modifiedTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editUnit(row)">編輯</button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeUnit(row)">刪除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <form class="card" @submit.prevent="saveUnit">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯營位" : "新增營位" }}</h5>
          <p class="text-muted mb-0">已有價格、庫存或預約資料的營位不能刪除，可改為停用。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="unit-zone">所屬區域</label>
            <select id="unit-zone" v-model.number="form.zoneId" class="form-select">
              <option :value="null">請選擇區域</option>
              <option v-for="zone in zones" :key="zone.id" :value="zone.id">
                {{ zone.name }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="unit-name">營位名稱</label>
            <input id="unit-name" v-model="form.name" class="form-control" type="text" placeholder="例如：A 區草地營位" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="unit-type">營位類型</label>
            <select id="unit-type" v-model="form.unitType" class="form-select">
              <option v-for="option in unitTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
          <div class="unit-number-grid">
            <div class="mb-3">
              <label class="form-label text-muted" for="unit-guests">人數</label>
              <input id="unit-guests" v-model.number="form.maxGuests" class="form-control" type="number" min="1" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="unit-vehicles">車數</label>
              <input id="unit-vehicles" v-model.number="form.maxVehicles" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="unit-tents">帳數</label>
              <input id="unit-tents" v-model.number="form.maxTents" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="unit-quantity">數量</label>
              <input id="unit-quantity" v-model.number="form.quantity" class="form-control" type="number" min="1" />
            </div>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="unit-sort-order">排序</label>
            <input id="unit-sort-order" v-model.number="form.sortOrder" class="form-control" type="number" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="unit-description">描述</label>
            <textarea id="unit-description" v-model="form.description" class="form-control" rows="4"></textarea>
          </div>
          <div class="unit-switches">
            <div class="form-check form-switch">
              <input id="unit-shelter" v-model="form.hasShelter" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="unit-shelter">有雨棚</label>
            </div>
            <div class="form-check form-switch">
              <input id="unit-electricity" v-model="form.electricityAvailable" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="unit-electricity">可供電</label>
            </div>
            <div class="form-check form-switch">
              <input id="unit-bathroom" v-model="form.privateBathroom" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="unit-bathroom">獨立衛浴</label>
            </div>
            <div class="form-check form-switch">
              <input id="unit-active" v-model="form.isActive" class="form-check-input" type="checkbox" />
              <label class="form-check-label" for="unit-active">前台啟用</label>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <div class="unit-form-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">
              清空
            </button>
            <button class="btn btn-dark" type="submit" :disabled="saving || zones.length === 0">
              {{ saving ? "儲存中..." : isEditing ? "儲存修改" : "新增營位" }}
            </button>
          </div>
          <div v-if="zones.length === 0" class="small text-danger mt-3">
            請先建立至少一個營區區域。
          </div>
          <div v-else-if="inactiveCount > 0" class="small text-muted mt-3">
            目前有 {{ inactiveCount }} 種停用營位，前台不會顯示。
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.unit-number-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 10px;
}

.unit-switches {
  display: grid;
  gap: 10px;
}

.unit-form-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 768px) {
  .unit-number-grid,
  .unit-form-actions {
    grid-template-columns: 1fr;
  }
}
</style>
