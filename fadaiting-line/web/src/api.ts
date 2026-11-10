import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_LINE_API_BASE_URL || "http://127.0.0.1:9191",
  headers: {
    "Content-Type": "application/json",
  },
});

export async function requestLineBind(payload: { bookingNo: string; contactPhone: string }) {
  const response = await api.post("/api/main/line/bind/request", payload);
  return response.data;
}

export async function confirmLineBind(payload: { bindToken: string; lineUserId: string; displayName: string; pictureUrl: string }) {
  const response = await api.post("/api/main/line/bind/confirm", payload);
  return response.data;
}

export async function getLineBookings(payload: { lineUserId: string }) {
  const response = await api.post("/api/main/line/booking/list", payload);
  return response.data;
}

export async function getLineBookingDetail(payload: { lineUserId: string; bookingId: number }) {
  const response = await api.post("/api/main/line/booking/detail", payload);
  return response.data;
}
