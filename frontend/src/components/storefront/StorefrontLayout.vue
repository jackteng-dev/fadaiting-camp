<script setup lang="ts">
import StorefrontHeader from "@/components/storefront/StorefrontHeader.vue";
import StorefrontFooter from "@/components/storefront/StorefrontFooter.vue";
import StorefrontLineFloatingButton from "@/components/storefront/StorefrontLineFloatingButton.vue";
</script>

<template>
  <div class="storefront-shell">
    <div class="shell-frame">
      <StorefrontHeader />
      <router-view />
      <StorefrontFooter />
      <StorefrontLineFloatingButton />
    </div>
  </div>
</template>

<style scoped>
.storefront-shell {
  min-height: 100vh;
  background: #f5f1e8;
}

.shell-frame {
  display: grid;
  gap: 18px;
  padding-top: 20px;
}
</style>
