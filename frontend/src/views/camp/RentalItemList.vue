<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminRentalItemService,
  deleteCampAdminRentalItemService,
  getCampAdminRentalItemListService,
  type RentalItemAdminPayload,
  updateCampAdminRentalItemService,
  uploadCampAdminImageService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const emptyForm = (): RentalItemAdminPayload => ({
  name: "",
  category: "tent",
  description: "",
  imageUrl: "",
  unitPrice: 0,
  totalQuantity: 1,
  sortOrder: 0,
  isActive: true,
});

const categoryOptions = [
  { label: "帳棚", value: "tent" },
  { label: "睡墊 / 防水墊", value: "mat" },
  { label: "炊事用品", value: "cooking" },
  { label: "照明 / 電源", value: "lighting" },
  { label: "其他", value: "other" },
];

const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const rows = ref<RentalItemAdminPayload[]>([]);
const form = ref<RentalItemAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);

const activeCount = computed(() => rows.value.filter((item) => item.isActive).length);
const stockCount = computed(() => rows.value.reduce((total, item) => total + Number(item.totalQuantity || 0), 0));
const isEditing = computed(() => editingId.value !== null);

const categoryText = (value: string) => {
  const option = categoryOptions.find((item) => item.value === value);
  return option ? option.label : value;
};

const formatMoney = (value: number) => `NT$ ${Number(value || 0).toLocaleString("zh-TW")}`;
const formatDateTime = (value?: string) => value?.replace("T", " ") || "-";

const handleImageUpload = async (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (!file.type.startsWith("image/")) {
    ElMessage.error("請選擇圖片檔案");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);
  formData.append("bucket", "rental-item-images");

  uploading.value = true;
  try {
    const response = await uploadCampAdminImageService(formData);
    form.value.imageUrl = response.data;
    ElMessage.success("圖片已上傳");
  } finally {
    uploading.value = false;
    (event.target as HTMLInputElement).value = "";
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    const response = await getCampAdminRentalItemListService();
    rows.value = response.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const editRentalItem = (row: RentalItemAdminPayload) => {
  form.value = cloneDeep(row);
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.name.trim()) {
    ElMessage.error("請填寫租借品名稱");
    return false;
  }
  if (Number(form.value.unitPrice) < 0) {
    ElMessage.error("租借單價不可小於 0");
    return false;
  }
  if (Number(form.value.totalQuantity) < 0) {
    ElMessage.error("庫存數量不可小於 0");
    return false;
  }
  return true;
};

const saveRentalItem = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("租借品", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminRentalItemService(form.value);
      ElMessage.success("租借品資料已更新");
    } else {
      await createCampAdminRentalItemService(form.value);
      ElMessage.success("租借品已新增");
    }
    await loadData();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeRentalItem = async (row: RentalItemAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(
    `確定要刪除「${row.name}」嗎？之後若已接到訂單明細，會改成只能停用保留歷史資料。`,
    "刪除租借品",
    {
      confirmButtonText: "刪除",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  await deleteCampAdminRentalItemService(row.id);
  ElMessage.success("租借品已刪除");
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
        <li class="breadcrumb-item"><router-link to="/admin/camp/rental-item">租借品管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">租借品管理</h4>
      <p class="text-muted mb-0">維護帳棚、防水墊、炊事用品等可加購或租借的品項與數量。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 class="mb-1">租借品列表</h5>
            <p class="text-muted mb-0">
              共 {{ rows.length }} 個品項，{{ activeCount }} 個前台啟用，總庫存 {{ stockCount }} 件。
            </p>
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
                  <th>圖片</th>
                  <th>排序</th>
                  <th>品項</th>
                  <th>類別</th>
                  <th>單價</th>
                  <th>庫存</th>
                  <th>狀態</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="8" class="text-center text-muted py-4">目前沒有租借品資料</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>
                    <img v-if="row.imageUrl" :src="row.imageUrl" :alt="row.name" class="rental-thumb" />
                    <div v-else class="rental-thumb rental-thumb--empty">無圖</div>
                  </td>
                  <td>{{ row.sortOrder }}</td>
                  <td>
                    <strong>{{ row.name }}</strong>
                    <div class="small text-muted">{{ row.description || "尚未填寫描述" }}</div>
                  </td>
                  <td>{{ categoryText(row.category) }}</td>
                  <td>{{ formatMoney(row.unitPrice) }}</td>
                  <td>{{ row.totalQuantity }}</td>
                  <td>
                    <span class="badge" :class="row.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ row.isActive ? "啟用" : "停用" }}
                    </span>
                    <div class="small text-muted">{{ formatDateTime(row.modifiedTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editRentalItem(row)">
                      編輯
                    </button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeRentalItem(row)">
                      刪除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <form class="card" @submit.prevent="saveRentalItem">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯租借品" : "新增租借品" }}</h5>
          <p class="text-muted mb-0">先建立可租借品項，下一步可接到前台預約選配。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="rental-name">品項名稱</label>
            <input id="rental-name" v-model="form.name" class="form-control" type="text" placeholder="例如：四人帳棚" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="rental-image">用品圖片</label>
            <div v-if="form.imageUrl" class="rental-preview mb-2">
              <img :src="form.imageUrl" :alt="form.name || '租借品圖片'" />
            </div>
            <input id="rental-image" class="form-control" type="file" accept="image/*" :disabled="uploading" @change="handleImageUpload" />
            <input v-model="form.imageUrl" class="form-control mt-2" type="url" placeholder="或貼上圖片網址" />
            <div class="form-text">{{ uploading ? "圖片上傳中..." : "可上傳圖片，也可直接貼圖片網址。" }}</div>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="rental-category">類別</label>
            <select id="rental-category" v-model="form.category" class="form-select">
              <option v-for="option in categoryOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
          <div class="rental-number-grid">
            <div class="mb-3">
              <label class="form-label text-muted" for="rental-price">單價</label>
              <input id="rental-price" v-model.number="form.unitPrice" class="form-control" type="number" min="0" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="rental-stock">庫存</label>
              <input id="rental-stock" v-model.number="form.totalQuantity" class="form-control" type="number" min="0" />
            </div>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="rental-sort">排序</label>
            <input id="rental-sort" v-model.number="form.sortOrder" class="form-control" type="number" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="rental-description">描述</label>
            <textarea
              id="rental-description"
              v-model="form.description"
              class="form-control"
              rows="3"
              placeholder="例如：含營釘、營繩，需自行搭設。"
            />
          </div>
          <div class="form-check form-switch mb-3">
            <input id="rental-active" v-model="form.isActive" class="form-check-input" type="checkbox" role="switch" />
            <label class="form-check-label" for="rental-active">前台啟用</label>
          </div>
        </div>
        <div class="card-footer d-flex justify-content-between">
          <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">清空</button>
          <button class="btn btn-primary" type="submit" :disabled="saving">
            {{ saving ? "儲存中..." : isEditing ? "更新租借品" : "新增租借品" }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.rental-number-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.rental-thumb {
  width: 72px;
  height: 54px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  object-fit: cover;
  background: #f8fafc;
}

.rental-thumb--empty {
  display: grid;
  place-items: center;
  color: #94a3b8;
  font-size: 12px;
}

.rental-preview {
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f8fafc;
}

.rental-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 575.98px) {
  .rental-number-grid {
    grid-template-columns: 1fr;
  }
}
</style>
