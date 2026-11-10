<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { confirmLineBind, getLineBookingDetail, getLineBookings, requestLineBind } from "./api";
import { initializeLiff, loginWithLiff, logoutWithLiff, type LiffProfileState } from "./liffClient";

const liffState = reactive<LiffProfileState>({
  enabled: false,
  ready: false,
  loggedIn: false,
  userId: "",
  displayName: "",
  pictureUrl: "",
  errorMessage: "",
});

const bindRequestForm = reactive({
  bookingNo: "",
  contactPhone: "",
});

const bindConfirmForm = reactive({
  bindToken: "",
  lineUserId: "",
  displayName: "",
  pictureUrl: "",
});

const bookingListForm = reactive({
  lineUserId: "",
});

const bookingDetailForm = reactive({
  lineUserId: "",
  bookingId: 0,
});

const bindRequestResult = ref<any>(null);
const bindConfirmResult = ref<any>(null);
const bookingListResult = ref<any>(null);
const bookingDetailResult = ref<any>(null);
const errorMessage = ref("");
const loading = ref(false);
const liffLoading = ref(false);

onMounted(async () => {
  liffLoading.value = true;
  try {
    const state = await initializeLiff();
    Object.assign(liffState, state);
    if (state.userId) {
      applyLineProfile(state.userId, state.displayName, state.pictureUrl);
    }
  } finally {
    liffLoading.value = false;
  }
});

function applyLineProfile(userId: string, displayName = "", pictureUrl = "") {
  bindConfirmForm.lineUserId = userId;
  bindConfirmForm.displayName = displayName;
  bindConfirmForm.pictureUrl = pictureUrl;
  bookingListForm.lineUserId = userId;
  bookingDetailForm.lineUserId = userId;
}

async function runAction(action: () => Promise<void>) {
  errorMessage.value = "";
  loading.value = true;
  try {
    await action();
  } catch (error: any) {
    errorMessage.value = error?.response?.data?.message || error?.message || "操作失敗";
  } finally {
    loading.value = false;
  }
}

function handleBindRequest() {
  runAction(async () => {
    bindRequestResult.value = await requestLineBind({
      bookingNo: bindRequestForm.bookingNo,
      contactPhone: bindRequestForm.contactPhone,
    });
    if (bindRequestResult.value?.data?.bindToken) {
      bindConfirmForm.bindToken = bindRequestResult.value.data.bindToken;
      bookingDetailForm.bookingId = bindRequestResult.value.data.bookingId || 0;
    }
  });
}

function handleBindConfirm() {
  runAction(async () => {
    bindConfirmResult.value = await confirmLineBind({
      bindToken: bindConfirmForm.bindToken,
      lineUserId: bindConfirmForm.lineUserId,
      displayName: bindConfirmForm.displayName,
      pictureUrl: bindConfirmForm.pictureUrl,
    });
    bookingListForm.lineUserId = bindConfirmForm.lineUserId;
    bookingDetailForm.lineUserId = bindConfirmForm.lineUserId;
  });
}

function handleBookingList() {
  runAction(async () => {
    bookingListResult.value = await getLineBookings({
      lineUserId: bookingListForm.lineUserId,
    });
  });
}

function handleBookingDetail() {
  runAction(async () => {
    bookingDetailResult.value = await getLineBookingDetail({
      lineUserId: bookingDetailForm.lineUserId,
      bookingId: Number(bookingDetailForm.bookingId),
    });
  });
}

function handleLiffLogin() {
  loginWithLiff();
}

function handleLiffLogout() {
  logoutWithLiff();
  window.location.reload();
}
</script>

<template>
  <main class="page">
    <header class="hero">
      <p class="eyebrow">fadaiting-line</p>
      <h1>LINE 會員綁定與查單</h1>
      <p class="intro">
        這個獨立專案目前已可對接主專案的 LINE 綁定與查單 API，並在設定 LIFF ID 後自動帶入 LINE 使用者資料。
      </p>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <p v-if="loading" class="loading">處理中...</p>

    <section class="profile-panel">
      <div>
        <p class="section-label">LIFF 狀態</p>
        <h2>{{ liffState.enabled ? "LINE LIFF" : "本機測試模式" }}</h2>
        <p v-if="liffLoading" class="muted">正在初始化 LIFF...</p>
        <p v-else-if="!liffState.enabled" class="muted">尚未設定 `VITE_LIFF_ID`，目前可手動輸入測試用 lineUserId。</p>
        <p v-else-if="liffState.errorMessage" class="error inline">{{ liffState.errorMessage }}</p>
        <p v-else-if="liffState.loggedIn" class="muted">已取得 LINE profile，可直接完成綁定與查單。</p>
        <p v-else class="muted">LIFF 已初始化，尚未登入 LINE。</p>
      </div>

      <div v-if="liffState.loggedIn" class="profile-card">
        <img v-if="liffState.pictureUrl" :src="liffState.pictureUrl" alt="LINE 頭像" />
        <div>
          <strong>{{ liffState.displayName || "LINE 使用者" }}</strong>
          <span>{{ liffState.userId }}</span>
        </div>
      </div>

      <div class="profile-actions">
        <button v-if="liffState.enabled && !liffState.loggedIn" @click="handleLiffLogin">LINE 登入</button>
        <button v-if="liffState.enabled && liffState.loggedIn" class="secondary" @click="handleLiffLogout">登出</button>
      </div>
    </section>

    <section class="grid">
      <article class="panel">
        <h2>1. 綁定請求</h2>
        <label>
          訂單編號
          <input v-model="bindRequestForm.bookingNo" placeholder="BK20260606EDECCD" />
        </label>
        <label>
          聯絡電話
          <input v-model="bindRequestForm.contactPhone" placeholder="0900000000" />
        </label>
        <button @click="handleBindRequest">建立 bindToken</button>
        <pre>{{ bindRequestResult }}</pre>
      </article>

      <article class="panel">
        <h2>2. 綁定確認</h2>
        <label>
          bindToken
          <input v-model="bindConfirmForm.bindToken" placeholder="自動帶入或手動貼上" />
        </label>
        <label>
          lineUserId
          <input v-model="bindConfirmForm.lineUserId" :readonly="liffState.loggedIn" placeholder="U-demo-line-user-001" />
        </label>
        <label>
          displayName
          <input v-model="bindConfirmForm.displayName" :readonly="liffState.loggedIn" placeholder="LINE 測試使用者" />
        </label>
        <label>
          pictureUrl
          <input v-model="bindConfirmForm.pictureUrl" :readonly="liffState.loggedIn" placeholder="https://example.com/profile.png" />
        </label>
        <button @click="handleBindConfirm">完成綁定</button>
        <pre>{{ bindConfirmResult }}</pre>
      </article>

      <article class="panel">
        <h2>3. 我的訂單</h2>
        <label>
          lineUserId
          <input v-model="bookingListForm.lineUserId" placeholder="U-demo-line-user-001" />
        </label>
        <button @click="handleBookingList">查詢訂單列表</button>
        <pre>{{ bookingListResult }}</pre>
      </article>

      <article class="panel">
        <h2>4. 單筆訂單</h2>
        <label>
          lineUserId
          <input v-model="bookingDetailForm.lineUserId" placeholder="U-demo-line-user-001" />
        </label>
        <label>
          bookingId
          <input v-model="bookingDetailForm.bookingId" type="number" />
        </label>
        <button @click="handleBookingDetail">查詢訂單明細</button>
        <pre>{{ bookingDetailResult }}</pre>
      </article>
    </section>
  </main>
</template>
