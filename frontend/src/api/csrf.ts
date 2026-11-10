import axios from "axios";

let token: string | undefined;
let loading: Promise<string> | undefined;
export const clearCsrfToken = () => { token = undefined; };

export async function getCsrfToken(): Promise<string> {
  if (token) return token;
  if (!loading) {
    loading = axios.get("/api/auth/csrf", { withCredentials: true }).then(response => {
      const value = response.data?.data?.token;
      if (response.data?.code !== 0 || typeof value !== "string" || !value) throw new Error("操作驗證載入失敗，請重新整理頁面");
      token = value;
      return value;
    }).finally(() => { loading = undefined; });
  }
  return loading;
}

export async function postSession(path: string) {
  const send = async () => axios.post(path, null, { withCredentials: true, headers: { "X-CSRF-TOKEN": await getCsrfToken() } });
  try { return await send(); }
  catch (error: any) {
    if (error.response?.status !== 403 || error.response?.data?.errorCode !== "CSRF_INVALID") throw error;
    clearCsrfToken();
    return send();
  }
}
