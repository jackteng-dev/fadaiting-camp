<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import router from "@/router";
import { adminLogoutService, updatePasswordService } from "@/api/user";
import { useUserInfoStore } from "@/stores/userInfo";

const userInfoStore = useUserInfoStore();
const submitting = ref(false);
const showPassword = ref(false);

const form = reactive({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});

const passwordRules = computed(() => {
  const value = form.newPassword;
  return {
    length: value.length >= 8 && value.length <= 24,
    upper: /[A-Z]/.test(value),
    lower: /[a-z]/.test(value),
    number: /\d/.test(value),
    symbol: /[^A-Za-z0-9]/.test(value),
  };
});

const passwordStrength = computed(() => {
  const rules = passwordRules.value;
  return [rules.upper, rules.lower, rules.number, rules.symbol].filter(Boolean).length;
});

const canSubmit = computed(() => {
  return (
    form.currentPassword.length > 0 &&
    passwordRules.value.length &&
    passwordStrength.value >= 3 &&
    form.newPassword === form.confirmPassword &&
    form.newPassword !== form.currentPassword
  );
});

const resetForm = () => {
  form.currentPassword = "";
  form.newPassword = "";
  form.confirmPassword = "";
};

const logoutToLogin = async () => {
  await adminLogoutService();
  localStorage.removeItem("deviceId");
  userInfoStore.removeInfo();
  router.push("/admin/login");
};

const submit = async () => {
  if (!canSubmit.value) {
    ElMessage.warning("請確認密碼規則與兩次輸入的新密碼");
    return;
  }

  await ElMessageBox.confirm(
    "修改密碼後會登出目前帳號，請使用新密碼重新登入。",
    "確認修改密碼",
    {
      confirmButtonText: "確認修改",
      cancelButtonText: "取消",
      type: "warning",
    }
  );

  submitting.value = true;
  try {
    await updatePasswordService(form);
    ElMessage.success("密碼已更新，請重新登入");
    await logoutToLogin();
  } finally {
    submitting.value = false;
  }
};
</script>

<template>
  <div class="page-header">
    <div class="page-block">
      <ul class="breadcrumb">
        <li class="breadcrumb-item"><router-link to="/admin/home">首頁</router-link></li>
        <li class="breadcrumb-item" aria-current="page">帳號設定</li>
        <li class="breadcrumb-item"><router-link to="/admin/user/resetPassword">修改密碼</router-link></li>
      </ul>
      <h4 class="pb-2 border-bottom mb-3"><i class="bi bi-lock me-2"></i>修改密碼</h4>
    </div>
  </div>

  <div class="password-layout">
    <form class="card password-card" @submit.prevent="submit">
      <div class="card-header">
        <h5 class="mb-1">更新登入密碼</h5>
        <p class="text-muted mb-0">為了保護後台帳號，修改後需要重新登入。</p>
      </div>
      <div class="card-body">
        <div class="mb-3">
          <label for="currentPassword" class="form-label">目前密碼</label>
          <input
            id="currentPassword"
            v-model.trim="form.currentPassword"
            :type="showPassword ? 'text' : 'password'"
            class="form-control"
            autocomplete="current-password"
            placeholder="請輸入目前使用的密碼"
          />
        </div>

        <div class="mb-3">
          <label for="newPassword" class="form-label">新密碼</label>
          <input
            id="newPassword"
            v-model.trim="form.newPassword"
            :type="showPassword ? 'text' : 'password'"
            class="form-control"
            autocomplete="new-password"
            placeholder="8 到 24 碼，至少包含三種字元類型"
          />
        </div>

        <div class="mb-3">
          <label for="confirmPassword" class="form-label">再次輸入新密碼</label>
          <input
            id="confirmPassword"
            v-model.trim="form.confirmPassword"
            :type="showPassword ? 'text' : 'password'"
            class="form-control"
            autocomplete="new-password"
            placeholder="請再次輸入新密碼"
          />
          <div v-if="form.confirmPassword && form.newPassword !== form.confirmPassword" class="form-text text-danger">
            兩次輸入的新密碼不一致。
          </div>
        </div>

        <label class="password-toggle">
          <input v-model="showPassword" type="checkbox" />
          顯示密碼
        </label>

        <div class="password-rules">
          <div class="rule" :class="{ 'rule--ok': passwordRules.length }">長度 8 到 24 碼</div>
          <div class="rule" :class="{ 'rule--ok': passwordStrength >= 3 }">
            至少包含三種：英文大寫、英文小寫、數字、符號
          </div>
          <div class="rule" :class="{ 'rule--ok': form.newPassword && form.newPassword !== form.currentPassword }">
            新密碼不可與目前密碼相同
          </div>
        </div>
      </div>

      <div class="card-footer password-actions">
        <button class="btn btn-outline-secondary" type="button" :disabled="submitting" @click="resetForm">清除重填</button>
        <button class="btn btn-dark" type="submit" :disabled="submitting || !canSubmit">
          {{ submitting ? "更新中..." : "修改密碼" }}
        </button>
      </div>
    </form>

    <aside class="card password-help">
      <div class="card-body">
        <h5>建議做法</h5>
        <p>密碼建議使用不容易被猜到的組合，並避免和其他網站重複。</p>
        <ul>
          <li>不要使用露營區名稱、生日或電話。</li>
          <li>不要和常用信箱、社群帳號共用密碼。</li>
          <li>修改後請用新密碼重新登入後台。</li>
        </ul>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.password-layout {
  display: grid;
  grid-template-columns: minmax(0, 640px) minmax(260px, 360px);
  gap: 20px;
  align-items: start;
}

.password-card,
.password-help {
  border-radius: 8px;
}

.password-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.password-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #4b5563;
}

.password-rules {
  display: grid;
  gap: 8px;
}

.rule {
  padding: 8px 10px;
  border-radius: 8px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
}

.rule--ok {
  background: #ecfdf3;
  color: #027a48;
}

.password-help p,
.password-help li {
  color: #64748b;
}

.password-help ul {
  margin-bottom: 0;
  padding-left: 18px;
}

@media (max-width: 960px) {
  .password-layout {
    grid-template-columns: 1fr;
  }

  .password-actions {
    display: grid;
  }
}
</style>
