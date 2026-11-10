import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { UserInfo,Permission } from '@/types/user'

// 預設空值
// const emptyUser: UserInfo = {
//     createdTime: '',
//     email: '',
//     firstName: '',
//     id: 0,
//     isStop: '',
//     lastLoginTime: '',
//     lastName: '',
//     modifiedTime: '',
//     permissions: [],
//     role: '',
//     telephone: '',
//     userPic: '',
//     userRole: '',
//     username: ''
// }

export const useUserInfoStore = defineStore('userInfo', () => {
    // 狀態變數
    const info = ref<UserInfo | null>(null)
    const permissions = ref<Permission[]>([])

    const isCampAdmin = computed(() => info.value?.campRole === 'ADMIN')

    // 方法
    const setInfo = (newInfo: UserInfo): void => {
        info.value = newInfo
    }

    const setPermissions = (newPermissions: Permission[]): void => {
        permissions.value = newPermissions.map(p => ({
            id: p.id,
            name: p.name,
            parentId: p.parentId,
            level: p.level,
            url: p.url,
            method: p.method,
            adminRouter: p.adminRouter
        }))
    }

    const removeInfo = (): void => {
        info.value = null
        permissions.value = []
    }

    return {
        info,
        isCampAdmin,
        permissions,
        setInfo,
        setPermissions,
        removeInfo
    }
}, {
    persist: true
})

