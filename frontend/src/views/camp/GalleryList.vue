<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminGalleryService,
  deleteCampAdminGalleryService,
  getCampAdminGalleryListService,
  getCampAdminZoneListService,
  type CampZoneAdminPayload,
  type SiteImageAdminPayload,
  updateCampAdminGalleryService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const emptyForm = (): SiteImageAdminPayload => ({
  zoneId: null,
  title: "",
  imageUrl: "",
  altText: "",
  sortOrder: 0,
  isCover: false,
  isActive: true,
});

const loading = ref(false);
const saving = ref(false);
const rows = ref<SiteImageAdminPayload[]>([]);
const zones = ref<CampZoneAdminPayload[]>([]);
const form = ref<SiteImageAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);

const isEditing = computed(() => editingId.value !== null);
const activeCount = computed(() => rows.value.filter((item) => item.isActive).length);
const coverCount = computed(() => rows.value.filter((item) => item.isCover).length);

const formatDateTime = (value?: string) => value?.replace("T", " ") || "未設定";

const zoneName = (zoneId?: number | null, zoneNameText?: string) => {
  if (zoneNameText) return zoneNameText;
  const zone = zones.value.find((item) => item.id === zoneId);
  return zone ? zone.name : "全營區";
};

const loadData = async () => {
  loading.value = true;
  try {
    const [zoneResponse, galleryResponse] = await Promise.all([
      getCampAdminZoneListService(),
      getCampAdminGalleryListService(),
    ]);
    zones.value = zoneResponse.data || [];
    rows.value = galleryResponse.data || [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const editImage = (row: SiteImageAdminPayload) => {
  form.value = cloneDeep(row);
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.title.trim()) {
    ElMessage.error("請填寫圖片標題");
    return false;
  }
  if (!form.value.imageUrl.trim()) {
    ElMessage.error("請填寫圖片網址");
    return false;
  }
  return true;
};

const saveImage = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("圖片", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminGalleryService(form.value);
      ElMessage.success("圖片資料已更新");
    } else {
      await createCampAdminGalleryService(form.value);
      ElMessage.success("圖片已新增");
    }
    await loadData();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeImage = async (row: SiteImageAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(`確定要刪除「${row.title}」嗎？`, "刪除圖片", {
    confirmButtonText: "刪除",
    cancelButtonText: "取消",
    type: "warning",
  });

  await deleteCampAdminGalleryService(row.id);
  ElMessage.success("圖片已刪除");
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
        <li class="breadcrumb-item"><router-link to="/admin/camp/gallery">相簿管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">相簿管理</h4>
      <p class="text-muted mb-0">維護首頁照片相簿所使用的圖片、排序、封面與前台顯示狀態。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 class="mb-1">圖片列表</h5>
            <p class="text-muted mb-0">共 {{ rows.length }} 張，{{ activeCount }} 張啟用，{{ coverCount }} 張封面。</p>
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
                  <th>預覽</th>
                  <th>圖片資訊</th>
                  <th>區域 / 排序</th>
                  <th>狀態</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="5" class="text-center text-muted py-4">目前沒有相簿圖片</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>
                    <img :src="row.imageUrl" :alt="row.altText || row.title" class="gallery-thumb" />
                  </td>
                  <td>
                    <div class="fw-semibold d-flex align-items-center gap-2">
                      <span>{{ row.title }}</span>
                      <span v-if="row.isCover" class="badge text-bg-warning">封面</span>
                    </div>
                    <div class="small text-muted">{{ row.altText || "未設定 alt 文案" }}</div>
                    <div class="small text-muted text-break">{{ row.imageUrl }}</div>
                  </td>
                  <td>
                    <div>{{ zoneName(row.zoneId, row.zoneName) }}</div>
                    <div class="small text-muted">排序：{{ row.sortOrder }}</div>
                  </td>
                  <td>
                    <span class="badge" :class="row.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ row.isActive ? "啟用" : "停用" }}
                    </span>
                    <div class="small text-muted">更新：{{ formatDateTime(row.modifiedTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editImage(row)">編輯</button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeImage(row)">刪除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <div class="col-xl-4">
      <form class="card" @submit.prevent="saveImage">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯圖片" : "新增圖片" }}</h5>
          <p class="text-muted mb-0">若設為封面，系統會自動取消其他圖片的封面狀態。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="gallery-zone">所屬區域</label>
            <select id="gallery-zone" v-model.number="form.zoneId" class="form-select">
              <option :value="null">全營區通用</option>
              <option v-for="zone in zones" :key="zone.id" :value="zone.id">
                {{ zone.name }}
              </option>
            </select>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="gallery-title">圖片標題</label>
            <input id="gallery-title" v-model="form.title" class="form-control" type="text" placeholder="例如：草地營位晨景" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="gallery-url">圖片網址</label>
            <input id="gallery-url" v-model="form.imageUrl" class="form-control" type="url" placeholder="https://..." />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="gallery-alt-text">Alt 文案</label>
            <input id="gallery-alt-text" v-model="form.altText" class="form-control" type="text" placeholder="供前台與 SEO 使用" />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="gallery-sort-order">排序</label>
            <input id="gallery-sort-order" v-model.number="form.sortOrder" class="form-control" type="number" />
          </div>
          <div class="form-check form-switch mb-2">
            <input id="gallery-cover" v-model="form.isCover" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="gallery-cover">設為封面圖片</label>
          </div>
          <div class="form-check form-switch">
            <input id="gallery-active" v-model="form.isActive" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="gallery-active">前台啟用這張圖片</label>
          </div>
        </div>
        <div class="card-footer">
          <div class="gallery-form-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">
              清空
            </button>
            <button class="btn btn-dark" type="submit" :disabled="saving">
              {{ saving ? "儲存中..." : isEditing ? "儲存修改" : "新增圖片" }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.gallery-thumb {
  width: 96px;
  height: 72px;
  object-fit: cover;
  border-radius: 10px;
  border: 1px solid #e9ecef;
}

.gallery-form-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 768px) {
  .gallery-form-actions {
    grid-template-columns: 1fr;
  }
}
</style>
