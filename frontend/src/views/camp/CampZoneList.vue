<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminZoneService,
  deleteCampAdminZoneService,
  getCampAdminZoneListService,
  type CampZoneAdminPayload,
  updateCampAdminZoneService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const emptyForm = (): CampZoneAdminPayload => ({
  name: "",
  slug: "",
  zoneType: "tent",
  description: "",
  sortOrder: 0,
  isActive: true,
});

const zoneTypeOptions = [
  { label: "自搭帳區", value: "tent" },
  { label: "雨棚區", value: "shelter" },
  { label: "免搭帳區", value: "glamping" },
  { label: "其他區域", value: "other" },
];

const loading = ref(false);
const saving = ref(false);
const rows = ref<CampZoneAdminPayload[]>([]);
const form = ref<CampZoneAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);

const activeCount = computed(() => rows.value.filter((item) => item.isActive).length);
const inactiveCount = computed(() => rows.value.length - activeCount.value);
const isEditing = computed(() => editingId.value !== null);

const zoneTypeText = (value: string) => {
  const option = zoneTypeOptions.find((item) => item.value === value);
  return option ? option.label : value;
};

const formatDateTime = (value?: string) => value?.replace("T", " ") || "-";

const loadZones = async () => {
  loading.value = true;
  try {
    const response = await getCampAdminZoneListService();
    rows.value = response.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const editZone = (row: CampZoneAdminPayload) => {
  form.value = cloneDeep(row);
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.name.trim()) {
    ElMessage.error("請填寫區域名稱");
    return false;
  }
  if (!form.value.slug.trim()) {
    ElMessage.error("請填寫區域 slug");
    return false;
  }
  return true;
};

const saveZone = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("區域", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminZoneService(form.value);
      ElMessage.success("區域資料已更新");
    } else {
      await createCampAdminZoneService(form.value);
      ElMessage.success("區域已新增");
    }
    await loadZones();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeZone = async (row: CampZoneAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(
    `確定要刪除「${row.name}」嗎？若此區域已有營位，系統會阻止刪除。`,
    "刪除區域",
    {
      confirmButtonText: "刪除",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  await deleteCampAdminZoneService(row.id);
  ElMessage.success("區域已刪除");
  if (editingId.value === row.id) {
    resetForm();
  }
  await loadZones();
};

onMounted(loadZones);
</script>

<template>
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
        <li class="breadcrumb-item" aria-current="page">營區管理</li>
        <li class="breadcrumb-item"><router-link to="/admin/camp/zone">營區區域管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">營區區域管理</h4>
      <p class="text-muted mb-0">維護前台營位介紹會使用的區域，例如雨棚區、草地營位區、免搭帳區。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 class="mb-1">區域列表</h5>
            <p class="text-muted mb-0">共 {{ rows.length }} 個區域，{{ activeCount }} 個前台啟用。</p>
          </div>
          <button class="btn btn-outline-secondary btn-sm" type="button" :disabled="loading" @click="loadZones">
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
                  <th>區域名稱</th>
                  <th>類型</th>
                  <th>Slug</th>
                  <th>狀態</th>
                  <th>最後更新</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="7" class="text-center text-muted py-4">目前沒有區域資料</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>{{ row.sortOrder }}</td>
                  <td>
                    <strong>{{ row.name }}</strong>
                    <div class="small text-muted">{{ row.description || "尚未填寫描述" }}</div>
                  </td>
                  <td>{{ zoneTypeText(row.zoneType) }}</td>
                  <td><code>{{ row.slug }}</code></td>
                  <td>
                    <span class="badge" :class="row.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ row.isActive ? "啟用" : "停用" }}
                    </span>
                  </td>
                  <td>{{ formatDateTime(row.modifiedTime) }}</td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editZone(row)">編輯</button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeZone(row)">刪除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <form class="card" @submit.prevent="saveZone">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯區域" : "新增區域" }}</h5>
          <p class="text-muted mb-0">停用後不會出現在前台營位介紹，但資料仍保留在後台。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="zone-name">區域名稱</label>
            <input id="zone-name" v-model="form.name" class="form-control" type="text" placeholder="例如：雨棚區" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="zone-slug">Slug</label>
            <input id="zone-slug" v-model="form.slug" class="form-control" type="text" placeholder="例如：canopy-zone" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="zone-type">區域類型</label>
            <select id="zone-type" v-model="form.zoneType" class="form-select">
              <option v-for="option in zoneTypeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="zone-sort-order">排序</label>
            <input id="zone-sort-order" v-model.number="form.sortOrder" class="form-control" type="number" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="zone-description">描述</label>
            <textarea id="zone-description" v-model="form.description" class="form-control" rows="4"></textarea>
          </div>
          <div class="form-check form-switch">
            <input id="zone-active" v-model="form.isActive" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="zone-active">前台啟用這個區域</label>
          </div>
        </div>
        <div class="card-footer">
          <div class="zone-form-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">
              清空
            </button>
            <button class="btn btn-dark" type="submit" :disabled="saving">
              {{ saving ? "儲存中..." : isEditing ? "儲存修改" : "新增區域" }}
            </button>
          </div>
          <div v-if="inactiveCount > 0" class="small text-muted mt-3">
            目前有 {{ inactiveCount }} 個停用區域，前台不會顯示。
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.zone-form-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

code {
  color: #34543c;
}

@media (max-width: 768px) {
  .zone-form-actions {
    grid-template-columns: 1fr;
  }
}
</style>
