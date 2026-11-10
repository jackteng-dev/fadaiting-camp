<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import cloneDeep from "lodash/cloneDeep";
import {
  getCampAdminSiteService,
  type CampSiteAdminPayload,
  updateCampAdminSiteService,
} from "@/api/camp";
import { confirmSaveAction } from "@/utils/confirmAction";

const loading = ref(false);
const saving = ref(false);
const siteForm = ref<CampSiteAdminPayload>({
  name: "",
  slug: "",
  shortDescription: "",
  fullDescription: "",
  addressLine: "",
  city: "",
  county: "",
  postalCode: "",
  latitude: null,
  longitude: null,
  contactName: "",
  contactPhone: "",
  contactEmail: "",
  facebookUrl: "",
  checkInTime: "14:00",
  checkOutTime: "11:00",
  bookingNotice: "",
  refundPolicy: "",
  isActive: true,
});
const originalForm = ref<CampSiteAdminPayload>(cloneDeep(siteForm.value));

const summaryLocation = computed(() =>
  [siteForm.value.county, siteForm.value.city, siteForm.value.addressLine]
    .filter(Boolean)
    .join(" ")
);

const loadSite = async () => {
  loading.value = true;
  try {
    const response = await getCampAdminSiteService();
    siteForm.value = {
      ...siteForm.value,
      ...response.data,
    };
    originalForm.value = cloneDeep(siteForm.value);
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  siteForm.value = cloneDeep(originalForm.value);
};

const saveSite = async () => {
  if (!siteForm.value.name.trim()) {
    ElMessage.error("請先填寫營區名稱");
    return;
  }
  if (!siteForm.value.contactPhone.trim()) {
    ElMessage.error("請先填寫聯絡電話");
    return;
  }

  if (!(await confirmSaveAction("營區基本資料", true))) return;

  saving.value = true;
  try {
    await updateCampAdminSiteService(siteForm.value);
    await loadSite();
    ElMessage.success("營區基本資料已更新");
  } finally {
    saving.value = false;
  }
};

onMounted(loadSite);
</script>

<template>
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
        <li class="breadcrumb-item" aria-current="page">營區管理</li>
        <li class="breadcrumb-item"><router-link to="/admin/camp/site">營區基本資料</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3">營區基本資料</h4>
      <p class="text-muted mb-0">這裡維護前台官網會直接使用的營區介紹、聯絡方式與入住說明。</p>
    </div>
  </div>

  <div class="row g-3">
    <div class="col-xl-4">
      <div class="card h-100">
        <div class="card-header">
          <h5 class="mb-0">目前摘要</h5>
        </div>
        <div class="card-body" v-if="!loading">
          <div class="summary-item">
            <span class="summary-label">營區名稱</span>
            <strong>{{ siteForm.name || "尚未設定" }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">聯絡窗口</span>
            <strong>{{ siteForm.contactName || "尚未設定" }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">電話 / Email</span>
            <strong>{{ siteForm.contactPhone || "尚未設定" }}</strong>
            <span class="summary-muted">{{ siteForm.contactEmail || "尚未設定" }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">位置</span>
            <strong>{{ summaryLocation || "尚未設定" }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">入住 / 退房</span>
            <strong>{{ siteForm.checkInTime || "--:--" }} / {{ siteForm.checkOutTime || "--:--" }}</strong>
          </div>
          <div class="summary-item">
            <span class="summary-label">前台狀態</span>
            <span class="badge" :class="siteForm.isActive ? 'text-bg-success' : 'text-bg-secondary'">
              {{ siteForm.isActive ? "啟用中" : "未啟用" }}
            </span>
          </div>
          <div class="summary-item">
            <span class="summary-label">最後更新</span>
            <span class="summary-muted">{{ siteForm.modifiedTime || "尚未更新" }}</span>
          </div>
        </div>
        <div v-else class="card-body text-muted">資料載入中...</div>
      </div>
    </div>

    <div class="col-xl-8">
      <form class="card" @submit.prevent="saveSite">
        <div class="card-header d-flex justify-content-between align-items-center">
          <h5 class="mb-0">內容編輯</h5>
          <span v-if="loading" class="text-muted small">讀取中...</span>
        </div>
        <div class="card-body">
          <div class="row g-3">
            <div class="col-md-8">
              <label class="form-label text-muted" for="site-name">營區名稱</label>
              <input id="site-name" v-model="siteForm.name" type="text" class="form-control" />
            </div>
            <div class="col-md-4">
              <label class="form-label text-muted" for="site-slug">Slug</label>
              <input id="site-slug" v-model="siteForm.slug" type="text" class="form-control" />
            </div>
            <div class="col-12">
              <label class="form-label text-muted" for="site-short-description">短描述</label>
              <textarea
                id="site-short-description"
                v-model="siteForm.shortDescription"
                rows="3"
                class="form-control"
              ></textarea>
            </div>
            <div class="col-12">
              <label class="form-label text-muted" for="site-full-description">完整介紹</label>
              <textarea
                id="site-full-description"
                v-model="siteForm.fullDescription"
                rows="5"
                class="form-control"
              ></textarea>
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-contact-name">聯絡人</label>
              <input id="site-contact-name" v-model="siteForm.contactName" type="text" class="form-control" />
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-contact-phone">聯絡電話</label>
              <input id="site-contact-phone" v-model="siteForm.contactPhone" type="text" class="form-control" />
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-contact-email">電子信箱</label>
              <input id="site-contact-email" v-model="siteForm.contactEmail" type="email" class="form-control" />
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-facebook-url">Facebook 粉專</label>
              <input id="site-facebook-url" v-model="siteForm.facebookUrl" type="url" class="form-control" />
            </div>
            <div class="col-12">
              <label class="form-label text-muted" for="site-address-line">地址</label>
              <input id="site-address-line" v-model="siteForm.addressLine" type="text" class="form-control" />
            </div>
            <div class="col-md-4">
              <label class="form-label text-muted" for="site-county">縣市</label>
              <input id="site-county" v-model="siteForm.county" type="text" class="form-control" />
            </div>
            <div class="col-md-4">
              <label class="form-label text-muted" for="site-city">鄉鎮區</label>
              <input id="site-city" v-model="siteForm.city" type="text" class="form-control" />
            </div>
            <div class="col-md-4">
              <label class="form-label text-muted" for="site-postal-code">郵遞區號</label>
              <input id="site-postal-code" v-model="siteForm.postalCode" type="text" class="form-control" />
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-latitude">緯度</label>
              <input id="site-latitude" v-model.number="siteForm.latitude" type="number" step="0.0000001" class="form-control" />
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-longitude">經度</label>
              <input id="site-longitude" v-model.number="siteForm.longitude" type="number" step="0.0000001" class="form-control" />
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-check-in">入住時間</label>
              <input id="site-check-in" v-model="siteForm.checkInTime" type="text" class="form-control" placeholder="14:00" />
            </div>
            <div class="col-md-6">
              <label class="form-label text-muted" for="site-check-out">退房時間</label>
              <input id="site-check-out" v-model="siteForm.checkOutTime" type="text" class="form-control" placeholder="11:00" />
            </div>
            <div class="col-12">
              <label class="form-label text-muted" for="site-booking-notice">訂位須知</label>
              <textarea
                id="site-booking-notice"
                v-model="siteForm.bookingNotice"
                rows="4"
                class="form-control"
              ></textarea>
            </div>
            <div class="col-12">
              <label class="form-label text-muted" for="site-refund-policy">退款政策</label>
              <textarea
                id="site-refund-policy"
                v-model="siteForm.refundPolicy"
                rows="4"
                class="form-control"
              ></textarea>
            </div>
            <div class="col-12">
              <div class="form-check form-switch">
                <input id="site-active" v-model="siteForm.isActive" class="form-check-input" type="checkbox" />
                <label class="form-check-label" for="site-active">前台啟用這個營區資料</label>
              </div>
            </div>
          </div>
        </div>
        <div class="card-footer">
          <div class="row g-2">
            <div class="col-md-6 d-grid">
              <button class="btn btn-outline-secondary" type="button" :disabled="loading || saving" @click="resetForm">
                還原目前資料
              </button>
            </div>
            <div class="col-md-6 d-grid">
              <button class="btn btn-dark" type="submit" :disabled="loading || saving">
                {{ saving ? "儲存中..." : "儲存變更" }}
              </button>
            </div>
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.summary-item + .summary-item {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #edf1f7;
}

.summary-label {
  display: block;
  margin-bottom: 0.35rem;
  color: #6c757d;
  font-size: 0.875rem;
}

.summary-muted {
  display: block;
  margin-top: 0.25rem;
  color: #6c757d;
  word-break: break-word;
}
</style>
