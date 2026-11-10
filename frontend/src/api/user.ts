import request from '@/api/api.ts'
import type { UserInfo } from '@/types/user'
//登入
import type { LoginData } from '@/types/user.ts'
export const adminLoginService = function (loginData: LoginData) {
  const params = new URLSearchParams();
  console.log('remeberMe:::' + loginData.rememberMe + "")
  params.append('username', loginData.username);
  params.append('password', loginData.password);
  params.append('rememberMe', loginData.rememberMe + "");
  params.append('deviceId', loginData.deviceId + "");
  return request.post('/admin/login', params);
}

//登出
export function adminLogoutService() {
  return request.post('/admin/logout');
}

//驗證是否登入
export function getUserInfoService() {
  return request.get('/auth/me');
}

//驗證是否登入
export function getBackendUser(username: string) {
  return request.get(`/admin/user/${username}`);
}

//更新用戶訊息
export const userInfoUpdateService = function (userInfoData: UserInfo) {
  return request.put('/admin/user/update', userInfoData)
}

//更新用戶頭像
export const userAvatarUpdateService = function (formData: FormData) {
  // const params = new URLSearchParams();
  // params.append('avatarUrl',avatarUrl)
  return request.patch('/upload/userAvatar', formData, {
      headers: {
          'Content-Type': 'multipart/form-data'
      }
  }
  )
}

export type PasswordUpdatePayload = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

export const updatePasswordService = function (payload: PasswordUpdatePayload) {
  return request.patch('/admin/user/password', payload);
}
