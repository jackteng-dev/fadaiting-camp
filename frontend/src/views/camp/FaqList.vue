<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  createCampAdminFaqService,
  deleteCampAdminFaqService,
  getCampAdminFaqListService,
  type FaqAdminPayload,
  updateCampAdminFaqService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const emptyForm = (): FaqAdminPayload => ({
  question: "",
  answer: "",
  sortOrder: 0,
  isActive: true,
});

const loading = ref(false);
const saving = ref(false);
const rows = ref<FaqAdminPayload[]>([]);
const form = ref<FaqAdminPayload>(emptyForm());
const editingId = ref<number | null>(null);

const isEditing = computed(() => editingId.value !== null);
const activeCount = computed(() => rows.value.filter((item) => item.isActive).length);

const formatDateTime = (value?: string) => value?.replace("T", " ") || "未設定";

const resetForm = () => {
  form.value = emptyForm();
  editingId.value = null;
};

const loadData = async () => {
  loading.value = true;
  try {
    const response = await getCampAdminFaqListService();
    rows.value = response.data || [];
  } finally {
    loading.value = false;
  }
};

const editFaq = (row: FaqAdminPayload) => {
  form.value = cloneDeep(row);
  editingId.value = row.id || null;
};

const validateForm = () => {
  if (!form.value.question.trim()) {
    ElMessage.error("請填寫 FAQ 問題");
    return false;
  }
  if (!form.value.answer.trim()) {
    ElMessage.error("請填寫 FAQ 回答");
    return false;
  }
  if (form.value.sortOrder < 0) {
    ElMessage.error("排序不可小於 0");
    return false;
  }
  return true;
};

const saveFaq = async () => {
  if (!validateForm()) return;
  if (!(await confirmSaveAction("FAQ", isEditing.value))) return;

  saving.value = true;
  try {
    if (isEditing.value) {
      await updateCampAdminFaqService(form.value);
      ElMessage.success("FAQ 已更新");
    } else {
      await createCampAdminFaqService(form.value);
      ElMessage.success("FAQ 已新增");
    }
    await loadData();
    resetForm();
  } finally {
    saving.value = false;
  }
};

const removeFaq = async (row: FaqAdminPayload) => {
  if (!row.id) return;

  await ElMessageBox.confirm(
    `確定要刪除這筆 FAQ 嗎？\n\n${row.question}`,
    "刪除 FAQ",
    {
      confirmButtonText: "刪除",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  await deleteCampAdminFaqService(row.id);
  ElMessage.success("FAQ 已刪除");
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
        <li class="breadcrumb-item"><router-link to="/admin/camp/faq">FAQ 管理</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">FAQ 管理</h4>
      <p class="text-muted mb-0">維護前台常見問題內容、排序與啟用狀態。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-8">
      <div class="card">
        <div class="card-header d-flex justify-content-between align-items-center">
          <div>
            <h5 class="mb-1">FAQ 列表</h5>
            <p class="text-muted mb-0">共 {{ rows.length }} 筆，{{ activeCount }} 筆啟用。</p>
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
                  <th>FAQ</th>
                  <th>排序</th>
                  <th>狀態</th>
                  <th class="text-end">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="rows.length === 0">
                  <td colspan="4" class="text-center text-muted py-4">目前沒有 FAQ 資料</td>
                </tr>
                <tr v-for="row in rows" :key="row.id" :class="{ 'table-active': editingId === row.id }">
                  <td>
                    <div class="fw-semibold mb-1">{{ row.question }}</div>
                    <div class="faq-answer text-muted">{{ row.answer }}</div>
                  </td>
                  <td>{{ row.sortOrder }}</td>
                  <td>
                    <span class="badge" :class="row.isActive ? 'text-bg-success' : 'text-bg-secondary'">
                      {{ row.isActive ? "啟用" : "停用" }}
                    </span>
                    <div class="small text-muted">更新：{{ formatDateTime(row.modifiedTime) }}</div>
                  </td>
                  <td class="text-end">
                    <button class="btn btn-outline-primary btn-sm me-2" type="button" @click="editFaq(row)">
                      編輯
                    </button>
                    <button class="btn btn-outline-danger btn-sm" type="button" @click="removeFaq(row)">
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
      <form class="card" @submit.prevent="saveFaq">
        <div class="card-header">
          <h5 class="mb-1">{{ isEditing ? "編輯 FAQ" : "新增 FAQ" }}</h5>
          <p class="text-muted mb-0">前台 FAQ 依排序由小到大顯示。</p>
        </div>
        <div class="card-body">
          <div class="mb-3">
            <label class="form-label text-muted" for="faq-question">問題</label>
            <input
              id="faq-question"
              v-model="form.question"
              class="form-control"
              type="text"
              placeholder="例如：是否可以攜帶寵物？"
            />
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="faq-answer">回答</label>
            <textarea
              id="faq-answer"
              v-model="form.answer"
              class="form-control"
              rows="7"
              placeholder="請輸入 FAQ 回答內容"
            ></textarea>
          </div>
          <div class="mb-3">
            <label class="form-label text-muted" for="faq-sort-order">排序</label>
            <input id="faq-sort-order" v-model.number="form.sortOrder" class="form-control" type="number" min="0" />
          </div>
          <div class="form-check form-switch">
            <input id="faq-active" v-model="form.isActive" class="form-check-input" type="checkbox" />
            <label class="form-check-label" for="faq-active">啟用這筆 FAQ</label>
          </div>
        </div>
        <div class="card-footer">
          <div class="faq-form-actions">
            <button class="btn btn-outline-secondary" type="button" :disabled="saving" @click="resetForm">
              清空
            </button>
            <button class="btn btn-dark" type="submit" :disabled="saving">
              {{ saving ? "儲存中..." : isEditing ? "儲存修改" : "新增 FAQ" }}
            </button>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.faq-answer {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
  white-space: pre-wrap;
}

.faq-form-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

@media (max-width: 768px) {
  .faq-form-actions {
    grid-template-columns: 1fr;
  }
}
</style>
