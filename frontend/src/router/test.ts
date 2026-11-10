// import { createRouter, createWebHistory } from 'vue-router'
// import { useUserInfoStore } from '@/stores/userInfo'
// import {useTokenStore} from '@/stores/token'
// import LayoutVue from '@/components/layout/BaseLayout.vue'
// import MainVue from '@/components/layout/Main.vue'
// // import Home from '../views/Home.vue'
// // import About from '../views/About.vue'
// import NotFound from '../views/NotFound.vue'
// import LoginVue from '@/views/Login.vue'
// import UserResetPasswordVue from '@/views/user/UserResetPassword.vue'
// import UserInfoVue from '@/views/user/UserInfo.vue'
// import UserAccountVue from '@/views/account/UserAccount.vue'
// import RoleListVue from '@/views/role/RoleList.vue'
// import PermissionListVue from '@/views/role/PermissionList.vue'

// //公用路由
// const baseRoutes = [
//   {
//     path: '/admin/login',
//     name: 'login',
//     component: LoginVue,
//     meta: { title: '登入' }
//   },
//   {
//     path: '/',
//     component: LayoutVue,
//     name: 'Layout',
//     meta: { requiresAuth: true },
//     children: [
//       {
//         path: '/home',
//         component: MainVue,
//         meta: { title: '首頁' }
//       },
//       {
//         path: '/admin/user/resetPassword',
//         component: UserResetPasswordVue,
//         meta: { title: '修改密碼' }
//       },
//       {
//         path: '/admin/user/info',
//         component: UserInfoVue,
//         meta: { title: '基本資料' }
//       }
//     ]
//   },
//   {
//     path: '/:pathMatch(.*)*',
//     name: 'NotFound',
//     component: NotFound,
//     meta: { title: '錯誤' }
//   }
// ]

// //動態路由對應Vue組件表
// const dynamicRouteMap = new Map<string, any>([
//   ['用戶列表', UserAccountVue],
//   ['角色列表', RoleListVue],
//   ['權限列表', PermissionListVue]
// ])


// const router = createRouter({
//   history: createWebHistory(),
//   routes: baseRoutes
// })

// //依照權限分配給他動態路由
// export function addDynamicRoutes(permissions: any[]) {
//   permissions.forEach((item) => {
//     const component = dynamicRouteMap.get(item.name)

//     if (component && item.adminRouter) {
//       router.addRoute('Layout', {
//         path: item.adminRouter,
//         name: item.name,
//         component: component,
//         meta: {
//           title: item.name,
//           requiresAuth: true
//         }
//       })
//     //   console.log([動態路由] 找到對應的 component 或 adminRouter：${item.name})
//     } else {
//       // console.warn([動態路由] 找不到對應的 component 或 adminRouter：${item.name})
//     }
//   })
// }

// router.beforeEach( (_to, _from, next) => {
//   const tokenStore = useTokenStore();
//   const userInfoStore = useUserInfoStore()
//   tokenStore.initToken();
//   const token = tokenStore.accessToken;
//   if (_to.path === '/admin/login') {
//     if (token) {
//       // 用戶已登入，回到首頁
//       return next('/');
//     } else {
//       //登入頁
//       return next();
//     }
//   } 

//   if (!token) {
//     return next('/admin/login'); // 未登入導回登入頁
//   }
//   // console.log('ppp:::'+userInfoStore.permissions)
//   // ✅ 已登入但重整過 → 確保補上動態路由
//   const isPageRefresh = sessionStorage.getItem('isPageRefresh') === 'true'
//   if (isPageRefresh && userInfoStore.permissions.length > 0) {
//     addDynamicRoutes(userInfoStore.permissions);
//     sessionStorage.removeItem('isPageRefresh');
//     return next({ ..._to, replace: true });
//   }
//   console.log('目前所有路由：', router.getRoutes())
//   return next();
// })


// export default router