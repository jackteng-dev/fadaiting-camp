<script setup lang="ts">
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import MobileHeader from '@/components/layout/MobileHeader.vue'
// import Main from '@/components/layout/Main.vue'
import Footer from '@/components/layout/Footer.vue'
// import { onMounted } from 'vue'
// import '@/assets/js/pages/dashboard-default.js'
// import '@/assets/js/plugins/apexcharts.min.js'
import { onMounted } from 'vue'
import '@/assets/js/pcoded.js'
onMounted(async () => {
  if (window.location.pathname.includes('/')) {
    // const { initDashboardCharts } = await import('@/assets/js/pages/dashboard-default.js')
    // initDashboardCharts()
    const { initPcoded } = await import('@/assets/js/pcoded.js')
    initPcoded()
  }
})
</script>




<template>
  <div class="admin-layout" data-pc-preset="preset-1" data-pc-direction="ltr" data-pc-theme="light">
    <div class="loader-bg">
      <div class="loader-track">
        <div class="loader-fill"></div>
      </div>
    </div>
    <Sidebar />
    <header class="pc-header">
      <div class="header-wrapper">
        <MobileHeader />
        <Header />
      </div>
    </header>
    <div class="pc-container">
      <div class="pc-content">
        <router-view></router-view>
        <!-- <Main /> -->
      </div>
    </div>
    <Footer />
  </div>
</template>

<style scoped></style>
