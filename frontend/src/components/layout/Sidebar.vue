<script setup lang="ts">
import { computed } from "vue";
import { useUserInfoStore } from "@/stores/userInfo.js";
import type { Permission } from "@/types/user";

type SidebarPermission = Permission & {
  children: SidebarPermission[];
};


const hasVisibleRoute = (item: Permission) => {
  return Boolean(item.adminRouter?.trim());
};

const transferDataToTree = (data: Permission[]) => {
  const itemsMap = new Map<number, SidebarPermission>();
  const rootItems: SidebarPermission[] = [];

  data.forEach((item) => {
    itemsMap.set(item.id, { ...item, children: [] });
  });

  data.forEach((item) => {
    const current = itemsMap.get(item.id);
    if (!current) return;

    const parent = itemsMap.get(item.parentId);
    if (parent) {
      parent.children.push(current);
    } else {
      rootItems.push(current);
    }
  });

  return rootItems
    .map((item) => ({
      ...item,
      children: item.children.filter(hasVisibleRoute),
    }))
    .filter((item) => item.children.length > 0);
};
const userInfoStore = useUserInfoStore();
const leftbarData = computed(() => transferDataToTree(userInfoStore.permissions));

const resolveAdminRoute = (adminRouter: string) => {
  if (!adminRouter) return "/admin/home";
  return adminRouter.startsWith("/admin") ? adminRouter : `/admin${adminRouter}`;
};
</script>
<template>
  <nav class="pc-sidebar">
    <div class="navbar-wrapper">
      <div class="m-header">
        <router-link to="/admin/home" class="b-brand text-primary">
          <img src="@/assets/images/brand/fadaiting-logo-mark.png" class="brand-icon" alt="發呆亭露營區">
          <span class="brand-title">發呆亭露營區</span>
        </router-link>
      </div>
      <div class="navbar-content">
        <ul class="pc-navbar">
          <li v-if="userInfoStore.isCampAdmin" class="pc-item">
            <router-link class="pc-link" to="/admin/backup-observation"><span class="pc-micon"><i class="ti ti-database"></i></span><span class="pc-mtext">備份與還原觀測</span></router-link>
          </li>
          <!-- <li class="pc-item">
          <a href="/index.html" class="pc-link">
            <span class="pc-micon"><i class="ti ti-dashboard"></i></span>
            <span class="pc-mtext">Dashboard</span>
          </a>
        </li>

        <li class="pc-item pc-caption">
          <label>UI Components</label>
          <i class="ti ti-dashboard"></i>
        </li>
        <li class="pc-item">
          <a href="../elements/bc_typography.html" class="pc-link">
            <span class="pc-micon"><i class="ti ti-typography"></i></span>
            <span class="pc-mtext">Typography</span>
          </a>
        </li>
        <li class="pc-item">
          <a href="../elements/bc_color.html" class="pc-link">
            <span class="pc-micon"><i class="ti ti-color-swatch"></i></span>
            <span class="pc-mtext">Color</span>
          </a>
        </li>
        <li class="pc-item">
          <a href="../elements/icon-tabler.html" class="pc-link">
            <span class="pc-micon"><i class="ti ti-plant-2"></i></span>
            <span class="pc-mtext">Icons</span>
          </a>
        </li>

        <li class="pc-item pc-caption">
          <label>Pages</label>
          <i class="ti ti-news"></i>
        </li>
        <li class="pc-item">
          <a href="../pages/login.html" class="pc-link">
            <span class="pc-micon"><i class="ti ti-lock"></i></span>
            <span class="pc-mtext">Login</span>
          </a>
        </li>
        <li class="pc-item">
          <a href="../pages/register.html" class="pc-link">
            <span class="pc-micon"><i class="ti ti-user-plus"></i></span>
            <span class="pc-mtext">Register</span>
          </a>
        </li>

        <li class="pc-item pc-caption">
          <label>Other</label>
          <i class="ti ti-brand-chrome"></i>
        </li> -->
          <li class="pc-item pc-hasmenu" v-for="list1 in leftbarData" :key="list1.id">
            <a href="#!" @click.prevent class="pc-link"><span class="pc-micon"><i class="ti ti-menu"></i></span><span
                class="pc-mtext">{{ list1.name }}</span><span class="pc-arrow"><i
                  data-feather="chevron-right"></i></span></a>
            <ul class="pc-submenu">
              <li class="pc-item" v-for="list2 in list1.children" :key="list2.id">
                <router-link class="pc-link" :to="resolveAdminRoute(list2.adminRouter)">{{ list2.name }}</router-link>
              </li>
              <!-- <li class="pc-item pc-hasmenu">
                <a href="#!" class="pc-link">Level 2.2<span class="pc-arrow"><i data-feather="chevron-right"></i></span></a>
                <ul class="pc-submenu">
                  <li class="pc-item"><a class="pc-link" href="#!">Level 3.1</a></li>
                  <li class="pc-item"><a class="pc-link" href="#!">Level 3.2</a></li>
                  <li class="pc-item pc-hasmenu">
                    <a href="#!" class="pc-link">Level 3.3<span class="pc-arrow"><i data-feather="chevron-right"></i></span></a>
                    <ul class="pc-submenu">
                      <li class="pc-item"><a class="pc-link" href="#!">Level 4.1</a></li>
                      <li class="pc-item"><a class="pc-link" href="#!">Level 4.2</a></li>
                    </ul>
                  </li>
                </ul>
              </li> -->
              <!-- <li class="pc-item pc-hasmenu">
                <a href="#!" class="pc-link">Level 2.3<span class="pc-arrow"><i data-feather="chevron-right"></i></span></a>
                <ul class="pc-submenu">
                  <li class="pc-item"><a class="pc-link" href="#!">Level 3.1</a></li>
                  <li class="pc-item"><a class="pc-link" href="#!">Level 3.2</a></li>
                  <li class="pc-item pc-hasmenu">
                    <a href="#!" class="pc-link">Level 3.3<span class="pc-arrow"><i data-feather="chevron-right"></i></span></a>
                    <ul class="pc-submenu">
                      <li class="pc-item"><a class="pc-link" href="#!">Level 4.1</a></li>
                      <li class="pc-item"><a class="pc-link" href="#!">Level 4.2</a></li>
                    </ul>
                  </li>
                </ul>
              </li> -->
            </ul>
          </li>
          <!-- <li class="pc-item">
          <a href="../other/sample-page.html" class="pc-link">
            <span class="pc-micon"><i class="ti ti-brand-chrome"></i></span>
            <span class="pc-mtext">Sample page</span>
          </a>
        </li> -->
        </ul>
        <!-- <div class="card text-center">
        <div class="card-body">
          <img src="@/assets/images/img-navbar-card.png" alt="images" class="img-fluid mb-2">
          <h5>Upgrade To Pro</h5>
          <p>To get more features and components</p>
          <a href="https://codedthemes.com/item/berry-bootstrap-5-admin-template/" target="_blank"
          class="btn btn-success">Buy Now</a>
        </div>
      </div> -->
      </div>
    </div>
  </nav>
</template>

<style scoped>
.brand-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  object-fit: cover;
  background: #f4ecd6;
}

.brand-title {
  margin-left: 10px;
  color: #1f2937;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0;
  white-space: nowrap;
}
</style>
