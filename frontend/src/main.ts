import { createApp } from 'vue'
import '@/style.css'
import App from '@/App.vue'
import router from '@/router/index'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
// import 'bootstrap/dist/css/bootstrap.min.css'
// import 'bootstrap/dist/js/bootstrap.bundle.min.js'
// // 後台模板字體與樣式
import '@/assets/fonts/tabler-icons.min.css'
import '@/assets/fonts/feather.css'
import '@/assets/fonts/fontawesome.css'
import '@/assets/fonts/material.css'
import '@/assets/css/style.css'
import '@/assets/css/style-preset.css'
import '@/assets/css/admin-density.css'

// // JS 插件（只在特定頁面需要的話，建議移到 mounted 中載入）


// import '@/assets/js/plugins/simplebar.min.js'
import '@/assets/js/plugins/popper.min.js'
// import '@/assets/js/pcoded.js'
import 'bootstrap-icons/font/bootstrap-icons.css'


// 使用 npm 的 ESM 入口，避免本地 UMD 在正式打包後留下 require 呼叫。
import '@/assets/js/bootstrap-runtime.js'
// import '@/assets/js/fonts/custom-font.js'
import { injectCustomFontSvg } from '@/assets/js/fonts/custom-font.ts';
injectCustomFontSvg()
import '@/assets/js/plugins/feather.min.js'
// import VueApexCharts from "vue3-apexcharts"
// 這是 ElMessage 依賴的 CSS 變數與結構
import 'element-plus/theme-chalk/index.css'              // ✅ 全部 Element Plus 样式（最重要）
// import 'element-plus/theme-chalk/el-message.css'
// import 'element-plus/theme-chalk/el-message-box.css'
// import 'element-plus/theme-chalk/el-overlay.css'

// // 如果 dashboard-default.js 是首頁才用，建議不要這裡 import
// import '@/assets/js/pages/dashboard-default.js'
const app = createApp(App)
// app.use(VueApexCharts)
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
// 監聽頁面有無刷新
// window.addEventListener('beforeunload', () => {
//     sessionStorage.setItem('isPageRefresh', 'true')  // 用字串 'true'
//   });
app.use(pinia)
app.use(router)
app.mount('#app')
