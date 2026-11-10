import axios from "axios";
import { config } from "./config.js";

export async function postMainApi<T>(path: string, payload: unknown): Promise<T> {
  const response = await axios.post<T>(`${config.mainApiBaseUrl}${path}`, payload, {
    headers: {
      "Content-Type": "application/json",
    },
    timeout: 10000,
  });
  return response.data;
}
