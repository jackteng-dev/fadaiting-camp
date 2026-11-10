import { createRouter, createWebHistory } from 'vue-router'
import { useUserInfoStore } from '@/stores/userInfo'
import LayoutVue from '@/components/layout/BaseLayout.vue'
import StorefrontLayoutVue from '@/components/storefront/StorefrontLayout.vue'
import MainVue from '@/components/layout/Main.vue'
import NotFound from '@/views/NotFound.vue'
import BackupObservation from '@/views/operation/BackupObservation.vue'
import LoginVue from '@/views/Login.vue'
import UserResetPasswordVue from '@/views/user/UserResetPassword.vue'
import UserInfoVue from '@/views/user/UserInfo.vue'
import { getUserInfoService } from '@/api/user'
import StorefrontHomeVue from '@/views/storefront/StorefrontHome.vue'
import StorefrontZonesVue from '@/views/storefront/StorefrontZones.vue'
import StorefrontBookingVue from '@/views/storefront/StorefrontBooking.vue'
import StorefrontNewsVue from '@/views/storefront/StorefrontNews.vue'
import StorefrontFaqVue from '@/views/storefront/StorefrontFaq.vue'
import StorefrontMapVue from '@/views/storefront/StorefrontMap.vue'
import StorefrontBookingSuccessVue from '@/views/storefront/StorefrontBookingSuccess.vue'
import StorefrontBookingLookupVue from '@/views/storefront/StorefrontBookingLookup.vue'
import StorefrontPaymentCheckoutVue from '@/views/storefront/StorefrontPaymentCheckout.vue'
import StorefrontPaymentConfirmVue from '@/views/storefront/StorefrontPaymentConfirm.vue'
import StorefrontPaymentCancelVue from '@/views/storefront/StorefrontPaymentCancel.vue'
import StorefrontPaymentFailVue from '@/views/storefront/StorefrontPaymentFail.vue'
import StorefrontPolicyVue from '@/views/storefront/StorefrontPolicy.vue'
import BookingListVue from '@/views/booking/BookingList.vue'
import BookingDetailVue from '@/views/booking/BookingDetail.vue'
import CheckInListVue from '@/views/booking/CheckInList.vue'
import BookingCalendarVue from '@/views/booking/BookingCalendar.vue'
import PaymentListVue from '@/views/booking/PaymentList.vue'
import PaymentMethodListVue from '@/views/booking/PaymentMethodList.vue'
import NotificationListVue from '@/views/booking/NotificationList.vue'
import CampSiteProfileVue from '@/views/camp/CampSiteProfile.vue'
import CampUnitListVue from '@/views/camp/CampUnitList.vue'
import CampZoneListVue from '@/views/camp/CampZoneList.vue'
import RatePlanListVue from '@/views/camp/RatePlanList.vue'
import AvailabilityListVue from '@/views/camp/AvailabilityList.vue'
import RentalItemListVue from '@/views/camp/RentalItemList.vue'
import AnnouncementListVue from '@/views/camp/AnnouncementList.vue'
import FaqListVue from '@/views/camp/FaqList.vue'
import GalleryListVue from '@/views/camp/GalleryList.vue'
import StaffAccountListVue from '@/views/account/StaffAccountList.vue'
import AdminOperationLogListVue from '@/views/admin/AdminOperationLogList.vue'

const adminHomePath = '/admin/home'

const toAdminPath = (path: string) => {
  if (!path) return adminHomePath
  return path.startsWith('/admin') ? path : `/admin${path}`
}

const toAdminChildPath = (path: string) => {
  const normalized = toAdminPath(path)
  return normalized.replace(/^\/admin\//, '')
}

const baseRoutes = [
  {
    path: '/',
    component: StorefrontLayoutVue,
    children: [
      {
        path: '',
        name: 'storefront-home',
        component: StorefrontHomeVue,
        meta: {
          title: '發呆亭露營區 | 山之間，靜謐的美好時光',
          description: '發呆亭露營區是虛構示範營區，提供山林露營、營位介紹、活動體驗與線上預約資訊。'
        }
      },
      {
        path: 'zones',
        name: 'storefront-zones',
        component: StorefrontZonesVue,
        meta: {
          title: '發呆亭露營區 | 營位介紹',
          description: '查看發呆亭露營區的分區配置、營位種類、入住人數與設備條件。'
        }
      },
      {
        path: 'booking',
        name: 'storefront-booking',
        component: StorefrontBookingVue,
        meta: {
          title: '發呆亭露營區 | 線上預約',
          description: '透過線上預約頁送出入住日期、營位與聯絡資料，由營區人工確認空位與安排。'
        }
      },
      {
        path: 'booking/success',
        name: 'storefront-booking-success',
        component: StorefrontBookingSuccessVue,
        meta: {
          title: '發呆亭露營區 | 預約結果',
          description: '查看最新送出的預約單摘要，並進入付款流程或保留訂單資訊。'
        }
      },
      {
        path: 'booking/lookup',
        name: 'storefront-booking-lookup',
        component: StorefrontBookingLookupVue,
        meta: {
          title: '發呆亭露營區 | 查詢預約',
          description: '使用訂單編號與聯絡電話查詢發呆亭露營區預約狀態、付款進度與處理歷程。'
        }
      },
      {
        path: 'map',
        name: 'storefront-map',
        component: StorefrontMapVue,
        meta: {
          title: '發呆亭露營區 | 營區地圖',
          description: '用地圖方式認識發呆亭露營區各分區位置、住宿節奏與營位摘要。'
        }
      },
      {
        path: 'news',
        name: 'storefront-news',
        component: StorefrontNewsVue,
        meta: {
          title: '發呆亭露營區 | 最新消息',
          description: '查看發呆亭露營區的最新公告、入住提醒、活動調整與季節建議。'
        }
      },
      {
        path: 'faq',
        name: 'storefront-faq',
        component: StorefrontFaqVue,
        meta: {
          title: '發呆亭露營區 | 常見問題',
          description: '整理預約、入住、營位、設備與周邊資訊的常見問題，出發前先看清楚。'
        }
      },
      {
        path: 'booking-guide',
        name: 'storefront-booking-guide',
        component: StorefrontPolicyVue,
        meta: {
          title: '發呆亭露營區 | 預約須知',
          description: '預約流程、入住時間、租借用品與出發前注意事項。'
        }
      },
      {
        path: 'refund-policy',
        name: 'storefront-refund-policy',
        component: StorefrontPolicyVue,
        meta: {
          title: '發呆亭露營區 | 退款與取消政策',
          description: '了解發呆亭露營區取消、改期、退款與未付款訂單處理方式。'
        }
      },
      {
        path: 'privacy-policy',
        name: 'storefront-privacy-policy',
        component: StorefrontPolicyVue,
        meta: {
          title: '發呆亭露營區 | 隱私政策',
          description: '說明預約、付款與通知資料的蒐集、使用與保存原則。'
        }
      },
      {
        path: 'payment/checkout/:provider/:method/:orderNo',
        name: 'storefront-payment-checkout',
        component: StorefrontPaymentCheckoutVue,
        meta: {
          title: '發呆亭露營區 | 付款頁',
          description: '進入付款頁並確認付款資訊。'
        }
      },
      {
        path: 'payment/linepay/confirm',
        name: 'storefront-payment-confirm',
        component: StorefrontPaymentConfirmVue,
        meta: {
          title: '發呆亭露營區 | 付款結果',
          description: '確認付款結果並同步顯示最新訂單狀態。'
        }
      },
      {
        path: 'payment/confirm',
        name: 'storefront-payment-confirm-generic',
        component: StorefrontPaymentConfirmVue,
        meta: {
          title: '發呆亭露營區 | 付款結果',
          description: '確認付款結果並同步顯示最新訂單狀態。'
        }
      },
      {
        path: 'payment/cancel',
        name: 'storefront-payment-cancel',
        component: StorefrontPaymentCancelVue,
        meta: {
          title: '發呆亭露營區 | 付款取消',
          description: '付款取消後，可回到訂單結果頁重新建立付款請求。'
        }
      },
      {
        path: 'payment/fail',
        name: 'storefront-payment-fail',
        component: StorefrontPaymentFailVue,
        meta: {
          title: '發呆亭露營區 | 付款失敗',
          description: '付款未完成時，可查看狀態並重新建立付款請求。'
        }
      }
    ]
  },
  {
    path: '/camp',
    redirect: '/'
  },
  {
    path: '/home',
    redirect: adminHomePath
  },
  {
    path: '/camp/site',
    redirect: '/admin/camp/site'
  },
  {
    path: '/camp/zone',
    redirect: '/admin/camp/zone'
  },
  {
    path: '/camp/unit',
    redirect: '/admin/camp/unit'
  },
  {
    path: '/camp/rate-plan',
    redirect: '/admin/camp/rate-plan'
  },
  {
    path: '/camp/availability',
    redirect: '/admin/camp/availability'
  },
  {
    path: '/camp/announcement',
    redirect: '/admin/camp/announcement'
  },
  {
    path: '/camp/faq',
    redirect: '/admin/camp/faq'
  },
  {
    path: '/camp/gallery',
    redirect: '/admin/camp/gallery'
  },
  {
    path: '/booking/list',
    redirect: '/admin/booking/list'
  },
  {
    path: '/booking/detail/:id',
    redirect: (to: any) => `/admin/booking/detail/${to.params.id}`
  },
  {
    path: '/admin/login',
    name: 'login',
    component: LoginVue,
    meta: { title: '登入' }
  },
  {
    path: '/admin',
    component: LayoutVue,
    name: 'AdminLayout',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'home',
        component: MainVue,
        meta: { title: '管理首頁' }
      },
      {
        path: 'user/resetPassword',
        component: UserResetPasswordVue,
        meta: { title: '修改密碼' }
      },
      {
        path: 'user/info',
        component: UserInfoVue,
        meta: { title: '基本資料' }
      },
      {
        path: 'camp/site',
        component: CampSiteProfileVue,
        meta: {
          title: '營區基本資料',
          requiresAuth: true,
          permission: '營區基本資料'
        }
      },
      {
        path: 'camp/zone',
        component: CampZoneListVue,
        meta: {
          title: '營區區域管理',
          requiresAuth: true,
          permission: '營區區域管理'
        }
      },
      {
        path: 'camp/unit',
        component: CampUnitListVue,
        meta: {
          title: '營位管理',
          requiresAuth: true,
          permission: '營位管理'
        }
      },
      {
        path: 'camp/rate-plan',
        component: RatePlanListVue,
        meta: {
          title: '價格管理',
          requiresAuth: true,
          permission: '價格管理'
        }
      },
      {
        path: 'camp/availability',
        component: AvailabilityListVue,
        meta: {
          title: '庫存管理',
          requiresAuth: true,
          permission: '庫存管理'
        }
      },
      {
        path: 'camp/rental-item',
        component: RentalItemListVue,
        meta: {
          title: '租借品管理',
          requiresAuth: true,
          permission: '租借品管理'
        }
      },
      {
        path: 'camp/announcement',
        component: AnnouncementListVue,
        meta: {
          title: '公告管理',
          requiresAuth: true,
          permission: '公告管理'
        }
      },
      {
        path: 'camp/faq',
        component: FaqListVue,
        meta: {
          title: 'FAQ 管理',
          requiresAuth: true,
          permission: 'FAQ 管理'
        }
      },
      {
        path: 'camp/gallery',
        component: GalleryListVue,
        meta: {
          title: '相簿管理',
          requiresAuth: true,
          permission: '相簿管理'
        }
      },
      {
        path: 'booking/payments',
        component: PaymentListVue,
        meta: {
          title: '付款紀錄',
          requiresAuth: true,
          permission: '查詢預約'
        }
      },
      {
        path: 'backup-observation',
        component: BackupObservation,
        meta: { title: '備份與還原觀測', requiresAuth: true }
      },
      {
        path: 'booking/payment-methods',
        component: PaymentMethodListVue,
        meta: {
          title: '支付方式管理',
          requiresAuth: true,
          permission: '支付方式管理'
        }
      },
      {
        path: 'booking/notifications',
        component: NotificationListVue,
        meta: {
          title: '通知紀錄',
          requiresAuth: true,
          permission: '查詢預約'
        }
      },
      {
        path: 'booking/calendar',
        component: BookingCalendarVue,
        meta: {
          title: '訂房日曆',
          requiresAuth: true,
          permission: '查詢預約'
        }
      },
      {
        path: 'booking/check-ins',
        component: CheckInListVue,
        meta: {
          title: '入住名單',
          requiresAuth: true,
          permission: '查詢預約'
        }
      },
      {
        path: 'booking/list',
        component: BookingListVue,
        meta: {
          title: '查詢預約',
          requiresAuth: true,
          permission: '查詢預約'
        }
      },
      {
        path: 'booking/detail/:id',
        component: BookingDetailVue,
        meta: {
          title: '預約明細',
          requiresAuth: true,
          permission: '查詢預約'
        }
      },
      {
        path: 'staff',
        component: StaffAccountListVue,
        meta: { title: '員工帳號', requiresAuth: true, permission: '員工帳號' }
      },
      {
        path: 'operation-log/list',
        component: AdminOperationLogListVue,
        meta: {
          title: '操作紀錄',
          requiresAuth: true,
          permission: '操作紀錄'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: NotFound,
    meta: { title: '錯誤頁' }
  }
]

const dynamicRouteMap = new Map<string, any>([
  ['查詢預約', BookingListVue],
  ['訂房日曆', BookingCalendarVue],
  ['入住名單', CheckInListVue],
  ['付款紀錄', PaymentListVue],
  ['支付方式管理', PaymentMethodListVue],
  ['營區區域管理', CampZoneListVue],
  ['營位管理', CampUnitListVue],
  ['價格管理', RatePlanListVue],
  ['庫存管理', AvailabilityListVue],
  ['租借品管理', RentalItemListVue],
  ['公告管理', AnnouncementListVue],
  ['FAQ 管理', FaqListVue],
  ['相簿管理', GalleryListVue],
  ['操作紀錄', AdminOperationLogListVue]
])

const router = createRouter({
  history: createWebHistory(),
  routes: baseRoutes,
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    if (to.hash) {
      return {
        el: to.hash,
        top: 120,
        behavior: 'smooth'
      }
    }

    return {
      top: 0,
      behavior: 'smooth'
    }
  }
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : '發呆亭露營區'
  const description =
    typeof to.meta.description === 'string'
      ? to.meta.description
      : '發呆亭露營區提供營位介紹、營區地圖、線上預約、最新消息與常見問題。'
  document.title = title

  const descriptionMeta = document.querySelector('meta[name="description"]')
  if (descriptionMeta) {
    descriptionMeta.setAttribute('content', description)
  }

  const ogTitleMeta = document.querySelector('meta[property="og:title"]')
  if (ogTitleMeta) {
    ogTitleMeta.setAttribute('content', title)
  }

  const ogDescriptionMeta = document.querySelector('meta[property="og:description"]')
  if (ogDescriptionMeta) {
    ogDescriptionMeta.setAttribute('content', description)
  }
})

export function addDynamicRoutes(permissions: any[]) {
  permissions.forEach((item) => {
    const component = dynamicRouteMap.get(item.name)
    const adminRouter = item.adminRouter ? toAdminChildPath(item.adminRouter) : ''
    const fullPath = adminRouter ? toAdminPath(item.adminRouter) : ''
    const routeExists = fullPath
      ? router.getRoutes().some((route) => route.path === fullPath || route.name === item.name)
      : false

    if (component && adminRouter && !routeExists) {
      router.addRoute('AdminLayout', {
        path: adminRouter,
        name: item.name,
        component,
        meta: {
          title: item.name,
          requiresAuth: true,
          permission: item.name
        }
      })
    }
  })
}

let hasAddedDynamicRoutes = false

router.beforeEach(async (_to, _from, next) => {
  const userInfoStore = useUserInfoStore()
  const isAdminRoute = _to.path.startsWith('/admin')

  if (!isAdminRoute) {
    return next()
  }

  if (_to.path === '/admin/login') {
    if (userInfoStore.info) {
      return next(adminHomePath)
    }
    return next()
  }

  try {
    const res = await getUserInfoService()
    const user = res.data
    userInfoStore.setInfo(user)
    userInfoStore.setPermissions(user.permissions)

    if (!hasAddedDynamicRoutes && userInfoStore.permissions.length > 0) {
      addDynamicRoutes(userInfoStore.permissions)
      hasAddedDynamicRoutes = true
      return next({ ..._to, replace: true })
    }

    const adminOnly = _to.path === '/admin/staff' || _to.path === '/admin/backup-observation' || _to.path.startsWith('/admin/camp/') ||
      _to.path.startsWith('/admin/operation-log/') ||
      ['/admin/booking/payments', '/admin/booking/payment-methods', '/admin/booking/notifications'].includes(_to.path)
    if (!['ADMIN', 'STAFF'].includes(user.campRole) || (adminOnly && user.campRole !== 'ADMIN')) {
      return next('/not-found')
    }

    const routePermission = _to.meta.permission as string | undefined
    const userPermissions = userInfoStore.permissions.map((p) => p.name)
    if (routePermission && !userPermissions.includes(routePermission)) {
      return next('/not-found')
    }

    next()
  } catch (error) {
    userInfoStore.removeInfo()
    hasAddedDynamicRoutes = false
    return next('/admin/login')
  }
})

export { toAdminPath }
export default router
