<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
// import { useTokenStore } from "@/stores/token.ts";
// import { useUserInfoStore } from "@/stores/userInfo";
// import { addDynamicRoutes } from "@/router/index.ts";
import { useRouter } from "vue-router";
import { adminLoginService } from "@/api/user.ts";
import type { LoginData } from '@/types/user'
import { onMounted } from "vue";
onMounted(async () => {
  const { initPcoded } = await import('@/assets/js/pcoded.js')
  initPcoded()
})
// import { addDynamicRoutes } from "@/router/index.js";
const router = useRouter();
// const tokenStore = useTokenStore();
//定義數據模型
const registerData = ref<LoginData>({
  username: "",
  password: "",
  rememberMe: false,
  deviceId: ''
});

//帳號密碼校驗
function validateForm() {
  const { username, password } = registerData.value

  // 使用者驗證
  if (!username || username.trim() === '') {
    ElMessage.error("請輸入用戶名")
    return false
  }
  if (username.length < 8 || username.length > 24) {
    ElMessage.error("用戶名長度為 8~24 位非空字符")
    return false
  }

  // 密碼驗證
  if (!password || password.trim() === '') {
    ElMessage.error("請輸入密碼")
    return false
  }
  if (password.length < 8 || password.length > 24) {
    ElMessage.error("密碼長度為 8~24 位非空字符")
    return false
  }

  return true
}

function generateDeviceId() {
  if (typeof globalThis.crypto?.randomUUID === "function") {
    return globalThis.crypto.randomUUID();
  }

  if (typeof globalThis.crypto?.getRandomValues === "function") {
    const bytes = new Uint8Array(16);
    globalThis.crypto.getRandomValues(bytes);
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, "0"));
    return [
      hex.slice(0, 4).join(""),
      hex.slice(4, 6).join(""),
      hex.slice(6, 8).join(""),
      hex.slice(8, 10).join(""),
      hex.slice(10, 16).join(""),
    ].join("-");
  }

  return `device-${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
}


//---------登入--------------------------------------
const login = async () => {
  if (!validateForm()) return

  // 確保 localStorage 中已有 deviceId（用於第一次登入產生 UUID）
  let deviceId = localStorage.getItem("deviceId");
  if (!deviceId) {
    deviceId = generateDeviceId();
    localStorage.setItem("deviceId", deviceId);
  }
  registerData.value.deviceId = deviceId;

  
  //調用接口，完成登入
  await adminLoginService(registerData.value);
  ElMessage.success("登入成功");
  // console.log(result.data.refreshToken);
  //把登入的訪問token存到pinia
  // tokenStore.setToken(result.data.accessToken, registerData.value.rememberMe);
  // console.log(result.data.accessToken);


  //把戶資訊存到pinia
  // const userInfoStore = useUserInfoStore();
  // userInfoStore.setInfo(result.data.user);
  //把用戶權限存到pinia
  // userInfoStore.setPermissions(result.data.user.permissions);
  // console.log(result.data.user.permissions);
  // addDynamicRoutes(result.data.user.permissions);
  //跳轉進首頁 借助路由跳轉
  router.push("/admin/home");
};
</script>
<template>
  <div class="loader-bg">
    <div class="loader-track">
      <div class="loader-fill"></div>
    </div>
  </div>
  <div class="auth-main">
    <div class="auth-wrapper v3">
      <div class="auth-form">
        <div class="auth-header">
          <a href="#" class="login-brand">
            <img src="/src/assets/images/brand/fadaiting-logo-mark.png" alt="發呆亭露營區">
            <span>發呆亭露營區</span>
          </a>
        </div>
        <div class="card my-5">
          <div class="card-body">
            <div class="d-flex justify-content-center align-items-end mb-4">
              <h3 class="mb-0"><b>後台管理系統</b></h3>
            </div>
            <div class="form-group mb-3">
              <label class="form-label">使用者</label>
              <input type="text" class="form-control" placeholder="請輸入用戶名"
                :class="{ 'is-invalid': registerData.username && (registerData.username.length < 8 || registerData.username.length > 24) }"
                v-model="registerData.username">
              <div class="invalid-feedback">
                用戶名長度為 8~24 位非空字符
              </div>
            </div>
            <div class="form-group mb-3">
              <label class="form-label">密碼</label>
              <input type="password" class="form-control" placeholder="請輸入密碼"
                :class="{ 'is-invalid': registerData.password && (registerData.password.length < 8 || registerData.password.length > 24) }"
                v-model="registerData.password">
              <div class="invalid-feedback">
                密碼長度為 8~24 位非空字符
              </div>
            </div>
            <div class="d-flex mt-1 justify-content-between">
              <div class="form-check">
                <input class="form-check-input input-primary" type="checkbox" id="customCheckc1"
                  v-model="registerData.rememberMe">
                <label class="form-check-label text-muted" for="customCheckc1">保持登入</label>
              </div>
              <a href="#" class="link-primary">忘記密碼</a>
            </div>
            <div class="d-grid mt-4">
              <button type="button" class="btn btn-primary" @click="login">Login</button>
            </div>
          </div>
        </div>
        <div class="auth-footer row">
          <!-- <div class=""> -->
          <div class="col my-1">
            <p class="m-0">Copyright © 發呆亭露營區管理系統</p>
          </div>
          <div class="col-auto my-1">
            <ul class="list-inline footer-link mb-0">
              <li class="list-inline-item"><a href="#">Home</a></li>
              <li class="list-inline-item"><a href="#">Privacy Policy</a></li>
              <li class="list-inline-item"><a href="#">Contact us</a></li>
            </ul>
          </div>
          <!-- </div> -->
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #1f2937;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.login-brand img {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  object-fit: cover;
  background: #f4ecd6;
}
</style>
