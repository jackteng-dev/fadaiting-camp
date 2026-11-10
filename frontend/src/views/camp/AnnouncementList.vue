<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminAnnouncementService,
  deleteCampAdminAnnouncementService,
  getCampAdminAnnouncementListService,
  type AnnouncementAdminPayload,
  updateCampAdminAnnouncementService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const emptyForm = (): AnnouncementAdminPayload => ({
  title: "",
  content: "",
  publishStartTime: null,
  publishEndTime: null,
  isPinned: false,
  isActive: true,
});

const loading = ref(false);
const saving = ref(false);
const rows = ref<AnnouncementAdminPayload[]>([]);
const form = ref<AnnouncementAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);

const isEditing = computed(() => editingId.value !== null);
const pinnedCount = computed(() => rows.value.filter((item) => item.isPinned).length);
const activeCount = computed(() => rows.value.filter((item) => item.isActive).length);

const formatDateTime = (value?: string | null) => value?.replace("T", " ") || "未設定";

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const loadData = async () => {
  loading.value = true;
  try {
    const response = await getCampAdminAnnouncementListService();
    rows.value = response.data || [];
  } finally {
    loading.value = false;
  }
};

const editAnnouncement = (row: AnnouncementAdminPayload) => {
  form.value = cloneDeep({
    ...row,
    publishStartTime: row.publishStartTime || null,
    publishEndTime: row.publishEndTime || null,
  });
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.title.trim()) {
    ElMessage.error("請填寫公告標題");
    return false;
  }
  if (!form.value.content.trim()) {
    ElMessage.error("請填寫公告內容");
    return false;
  }
  if (
    form.value.publishStartTime &&
    form.value.publishEndTime &&
    form.value.publishEndTime < form.value.publishStartTime
  ) {
    ElMessage.error("公告結束時間不可早於開始時間");
    return false;
  }
  return true;
};

const saveAnnouncement = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("公告", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminAnnouncementService(form.value);
      ElMessage.success("公告已更新");
    } else {
      await createCampAdminAnnouncementService(form.value);
      ElMessage.success("公告已新增");
    }
    await loadData();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeAnnouncement = async (row: AnnouncementAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(
    `確定要刪除「${row.title}」嗎？這會直接移除公告內容。`,
    "刪除公告",
    {
      confirmButtonText: "刪除",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  await deleteCampAdminAnnouncementService(row.id);
  ElMessage.success("公告已刪除");
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
        <li class="breadcrumb-item"><router-link to="/admin/camp/announcement">公告管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">公告管理</h4>
      <p class="text-muted mb-0">維護前台最新消息內容、上架時間與置頂狀態。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 class="mb-1">公告列表</h5>
            <p class="text-muted mb-0">共 {{ rows.length }} 筆，{{ activeCount }} 筆啟用，{{ pinnedCount }} 筆置頂。</p>
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
                  <th>公告</th>
                  <th>發佈區間</th>
                  <th>狀態</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="4" class="text-center text-muted py-4">目前沒有公告資料</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>
                    <div class="d-flex align-items-center gap-2 mb-1">
                      <strong>{{ row.title }}</strong>
                      <span v-if="row.isPinned" class="badge text-bg-warning">置頂</span>
                    </div>
                    <div class="announcement-content text-muted">{{ row.content }}</div>
                  </td>
                  <td>
                    <div>開始：{{ formatDateTime(row.publishStartTime) }}</div>
                    <div class="small text-muted">結束：{{ formatDateTime(row.publishEndTime) }}</div>
                  </td>
                  <td>
                    <span class="badge" :class="row.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ row.isActive ? "啟用" : "停用" }}
                    </span>
                    <div class="small text-muted">更新：{{ formatDateTime(row.modifiedTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editAnnouncement(row)">
                      編輯
                    </button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeAnnouncement(row)">
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
      <form class="card" @submit.prevent="saveAnnouncement">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯公告" : "新增公告" }}</h5>
          <p class="text-muted mb-0">可設定上架區間，未填表示不限時間。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="announcement-title">公告標題</label>
            <input
              id="announcement-title"
              v-model="form.title"
              class="form-control"
              type="text"
              placeholder="例如：六月連假營位調整通知"
            />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="announcement-content">公告內容</label>
            <textarea
              id="announcement-content"
              v-model="form.content"
              class="form-control"
              rows="7"
              placeholder="請輸入要顯示在前台最新消息的內容"
            ></textarea>
          </div>
          <div class="announcement-date-grid">
            <div class="mb-3">
              <label class="form-label text-muted" for="announcement-start">開始時間</label>
              <input id="announcement-start" v-model="form.publishStartTime" class="form-control" type="datetime-local" />
            </div>
            <div class="mb-3">
              <label class="form-label text-muted" for="announcement-end">結束時間</label>
              <input id="announcement-end" v-model="form.publishEndTime" class="form-control" type="datetime-local" />
            </div>
          </div>
          <div class="form-check form-switch mb-2">
            <input id="announcement-pinned" v-model="form.isPinned" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="announcement-pinned">置頂顯示這則公告</label>
          </div>
          <div class="form-check form-switch">
            <input id="announcement-active" v-model="form.isActive" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="announcement-active">啟用這則公告</label>
          </div>
        </div>
        <div class="card-footer">
          <div class="announcement-form-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">
              清空
            </button>
            <button class="btn btn-dark" type="submit" :disabled="saving">
              {{ saving ? "儲存中..." : isEditing ? "儲存修改" : "新增公告" }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.announcement-content {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  white-space: pre-wrap;
}

.announcement-date-grid,
.announcement-form-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 10px;
}

@media (max-width: 768px) {
  .announcement-date-grid,
  .announcement-form-actions {
    grid-template-columns: 1fr;
  }
}
</style>
