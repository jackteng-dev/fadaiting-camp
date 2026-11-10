export interface LoginData {
    username: string
    password: string
    rememberMe: boolean
    deviceId: string
  }

/** 權限資料型別 */
export interface Permission {
  id: number
  name: string
  parentId: number
  level: number
  url: string
  method: string
  adminRouter: string
}

export interface Role {
  id: number
  name: string
  description: number
  permissions: Permission[]
}

/** 使用者資訊型別 */
export interface UserInfo {
  createdTime: string
  email: string
  firstName: string
  id: number
  isStop: string
  lastLoginTime: string
  lastName: string
  modifiedTime: string
  permissions: Permission[]
  role: Role[]
  telephone: string
  userPic: string
  userRole: string
  username: string
  campRole?: 'ADMIN' | 'STAFF'
}