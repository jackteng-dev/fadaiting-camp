import request from '@/api/api'

export type CampRole = 'ADMIN' | 'STAFF'
export type StaffAccount = {
  id: number
  username: string
  firstName: string
  lastName: string
  campRole: CampRole
  enabled: boolean
  authVersion: number
  createdTime: string
  lastLoginTime: string | null
}
export type StaffCreate = {
  username: string
  password: string
  confirmPassword: string
  firstName: string
  lastName: string
  campRole: CampRole
}
export const getStaffAccountListService = (params: { username: string; pageNum: number; pageSize: number }) =>
  request.get('/admin/staff', { params })
export const createStaffAccountService = (payload: StaffCreate) => request.post('/admin/staff', payload)
export const updateStaffAccountService = (id: number, payload: { campRole: CampRole; enabled: boolean; expectedVersion: number }) =>
  request.patch(`/admin/staff/${id}`, payload)
