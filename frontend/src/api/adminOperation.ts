import request from "@/api/api";

export type AdminOperationLogQuery = {
  pageNum: number;
  pageSize: number;
  username?: string;
  actionType?: string;
  success?: boolean | "";
  requestPath?: string;
  startDate?: string;
  endDate?: string;
};

export type AdminOperationLog = {
  id: number;
  userId?: number | null;
  username: string;
  actionType: string;
  actionLabel: string;
  requestMethod: string;
  requestPath: string;
  queryString?: string;
  statusCode?: number;
  success: boolean;
  message?: string;
  ipAddress?: string;
  userAgent?: string;
  createdTime?: string;
};

export const getAdminOperationLogListService = (payload: AdminOperationLogQuery) =>
  request.post("/operation-log/list", payload);
