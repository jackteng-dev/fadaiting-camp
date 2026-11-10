//訂製請求的實例
import axios from "axios";
import { ElMessage } from "element-plus";
// import { useTokenStore } from "@/stores/token.ts";
import router from "@/router/index.ts";
import { clearCsrfToken, getCsrfToken, postSession } from "@/api/csrf";
// import { useUserInfoStore } from "@/stores/userInfo.ts";
// const userInfoStore = useUserInfoStore();
// const baseURL = 'http://localhost:9090/api';
const baseURL = "/api";
const instance = axios.create({ baseURL, withCredentials: true });
let refreshing: Promise<boolean> | undefined;
//添加請求攔截器
instance.interceptors.request.use(
  async (config) => {
    const path = config.url || "";
    if (!["get", "head", "options"].includes((config.method || "get").toLowerCase()) && !path.startsWith("/public/")) {
      config.headers.set("X-CSRF-TOKEN", await getCsrfToken());
    }
    return config;
  },
  (err) => {
    return Promise.reject(err);
  }
);
//添加響應攔截器
instance.interceptors.response.use(
  (res) => {
    //判斷狀態碼
    if (res.data.code === 0) {
      if (["/admin/login", "/admin/logout"].includes(res.config.url || "")) clearCsrfToken();
      return res.data;
    }
    ElMessage.error(res.data.message || "服務異常");
    return Promise.reject(res.data); //異步狀態轉換成失敗的狀態
  },
  async (err) => {
    const config = err.config;
    if (err.response?.status === 403 && err.response?.data?.errorCode === "CSRF_INVALID") {
      if (config && !config._csrfRetry) {
        config._csrfRetry = true;
        clearCsrfToken();
        return instance(config);
      }
      ElMessage.error("操作驗證已失效，請重新整理頁面後再試");
      return Promise.reject(err);
    }
    if (err.response?.status === 403) {
      ElMessage.error(err.response?.data?.message || err.response?.data?.error || "無使用權限，請聯絡營區管理員");
      return Promise.reject(err);
    }
    if (err.response?.status === 401) {
      if (config?.url?.startsWith("/public/")) {
        ElMessage.error(err.response?.data?.message || "身分驗證失敗，請重新操作");
        return Promise.reject(err);
      }
      if (!config._retry) {
        config._retry = true;
        try {
          if (!refreshing) refreshing = postSession("/api/auth/refresh").then(response => response.data.code === 0).finally(() => { refreshing = undefined; });
          if (await refreshing) return instance(config);
        } catch { /* 登入失效時交由下方清除狀態。 */ }
      }
      const { useUserInfoStore } = await import("@/stores/userInfo");
      useUserInfoStore().removeInfo();
      ElMessage.error("登入逾時，請重新登入");
      router.push("/admin/login");
    } else {
      ElMessage.error(err.response?.data?.message || "發生錯誤，請稍後再試");
    }
    return Promise.reject(err);
  }
)

export default instance;
