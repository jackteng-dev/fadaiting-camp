import axios from "axios";
import request from "@/api/api";

const publicApi = axios.create({
  baseURL: "/api",
  withCredentials: true,
});

export const getCampSiteDetailService = () =>
  publicApi.get("/public/camp/site").then((res) => res.data);

export const getCampZonesService = () =>
  publicApi.get("/public/camp/zones").then((res) => res.data);

export const getCampAnnouncementsService = () =>
  publicApi.get("/public/camp/announcements").then((res) => res.data);

export type PublicRentalItem = {
  id: number;
  name: string;
  category: string;
  description: string;
  imageUrl: string;
  unitPrice: number;
  totalQuantity: number;
  sortOrder: number;
  isActive: boolean;
};

export const getPublicRentalItemsService = () =>
  publicApi.get("/public/camp/rental-items").then((res) => res.data as { code: number; data: PublicRentalItem[] });

export type BookingRentalItemPayload = {
  rentalItemId: number;
  quantity: number;
};

export type BookingQuotePayload = {
  campUnitId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  vehicleCount: number;
  rentalItems?: BookingRentalItemPayload[];
};

export type BookingQuoteNight = {
  date: string;
  nightlyPrice: number;
  availableQuantity: number;
  closed: boolean;
};

export type BookingQuoteResponse = {
  campUnitId: number;
  campUnitName: string;
  available: boolean;
  nights: number;
  guestCount: number;
  vehicleCount: number;
  siteAmount: number;
  rentalAmount: number;
  totalAmount: number;
  message: string;
  nightsDetail: BookingQuoteNight[];
  rentalItems: Array<{
    rentalItemId: number;
    name: string;
    category: string;
    imageUrl: string;
    quantity: number;
    totalQuantity: number;
    reservedQuantity: number;
    availableQuantity: number;
    unitPrice: number;
    lineTotal: number;
  }>;
};

export const getBookingQuoteService = (payload: BookingQuotePayload) =>
  publicApi.post("/public/booking/quote", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "報價查詢失敗");
    }
    return res.data as { code: number; data: BookingQuoteResponse; message?: string };
  });

export const createBookingService = (payload: {
  campUnitId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  vehicleCount: number;
  contactName: string;
  contactPhone: string;
  contactEmail?: string;
  note?: string;
  rentalItems?: BookingRentalItemPayload[];
  }) =>
  publicApi.post("/public/booking", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "預約送出失敗");
    }
    return res.data;
  });

export type PublicBookingLookupPayload = {
  bookingNo: string;
  contactPhone: string;
};

export type PublicBookingSummaryPayload = {
  bookingId: number;
  bookingNo: string;
};

export type PublicBookingLookupResponse = {
  bookingId: number;
  bookingNo: string;
  status: string;
  paymentDueAt: string | null;
  expiredAt: string | null;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  guestCount: number;
  vehicleCount: number;
  subtotalAmount: number;
  adjustmentAmount: number;
  totalAmount: number;
  paidAmount: number;
  refundedAmount: number;
  contactName: string;
  maskedContactPhone: string;
  createdTime?: string;
  items: Array<{
    itemName: string;
    quantity: number;
    nights: number;
    lineTotal: number;
  }>;
  payments: Array<{
    refundedAmount: number;
    provider: string;
    paymentMethod: string;
    status: string;
    amount: number;
    currency: string;
    paidTime?: string;
    createdTime?: string;
  }>;
  timeline: Array<{
    actionType: string;
    actionLabel: string;
    detail?: string;
    createdTime?: string;
  }>;
};

export const lookupPublicBookingService = (payload: PublicBookingLookupPayload) =>
  publicApi.post("/public/booking/lookup", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "預約查詢失敗");
    }
    return res.data as { code: number; data: PublicBookingLookupResponse; message?: string };
  });

export const getPublicBookingSummaryService = (payload: PublicBookingSummaryPayload) =>
  publicApi.post("/public/booking/summary", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "訂單摘要查詢失敗");
    }
    return res.data as { code: number; data: PublicBookingLookupResponse; message?: string };
  });

export type PublicPaymentCreatePayload = {
  bookingId: number;
  provider: "linepay" | "ecpay";
  paymentMethod: string;
};

export type PublicPaymentMethodConfig = {
  id: number;
  provider: "linepay" | "ecpay";
  displayName: string;
  description: string;
  paymentMethod: string;
  sortOrder: number;
  isActive: boolean;
};

export type PublicPaymentRetryPayload = {
  bookingNo: string;
  contactPhone: string;
  provider: "linepay" | "ecpay";
  paymentMethod: string;
};

export type PublicPaymentCreateResponse = {
  paymentRecordId: number;
  provider: string;
  paymentMethod: string;
  status: string;
  merchantOrderNo: string;
  amount: number;
  currency: string;
  paymentUrl?: string;
  redirectMethod?: string;
  formFields?: Record<string, string>;
  message: string;
};

export type PublicPaymentConfirmPayload = {
  paymentRecordId?: number;
  merchantOrderNo?: string;
  transactionId?: string;
};

export type PublicPaymentConfirmResponse = {
  paymentRecordId: number;
  bookingId: number;
  bookingNo: string;
  provider: string;
  status: string;
  transactionId?: string;
  amount: number;
  message: string;
};

export const createPublicPaymentService = (payload: PublicPaymentCreatePayload) =>
  publicApi.post("/public/payment/request", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "建立付款請求失敗");
    }
    return res.data as { code: number; data: PublicPaymentCreateResponse; message?: string };
  });

export const getPublicPaymentMethodsService = () =>
  publicApi.get("/public/payment/methods").then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "付款方式載入失敗");
    }
    return res.data as { code: number; data: PublicPaymentMethodConfig[]; message?: string };
  });

export const retryPublicPaymentService = (payload: PublicPaymentRetryPayload) =>
  publicApi.post("/public/payment/retry", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "重新建立付款請求失敗");
    }
    return res.data as { code: number; data: PublicPaymentCreateResponse; message?: string };
  });

export const confirmPublicPaymentService = (payload: PublicPaymentConfirmPayload) =>
  publicApi.post("/public/payment/confirm", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "付款確認失敗");
    }
    return res.data as { code: number; data: PublicPaymentConfirmResponse; message?: string };
  });

export const cancelPublicPaymentService = (payload: PublicPaymentConfirmPayload) =>
  publicApi.post("/public/payment/cancel", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "取消付款失敗");
    }
    return res.data as { code: number; data: PublicPaymentConfirmResponse; message?: string };
  });

export const failPublicPaymentService = (payload: PublicPaymentConfirmPayload) =>
  publicApi.post("/public/payment/fail", payload).then((res) => {
    if (res.data?.code !== 0) {
      throw new Error(res.data?.message || "付款失敗狀態同步失敗");
    }
    return res.data as { code: number; data: PublicPaymentConfirmResponse; message?: string };
  });

export type CampSiteAdminPayload = {
  id?: number;
  name: string;
  slug: string;
  shortDescription: string;
  fullDescription: string;
  addressLine: string;
  city: string;
  county: string;
  postalCode: string;
  latitude?: number | null;
  longitude?: number | null;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  facebookUrl: string;
  checkInTime: string;
  checkOutTime: string;
  bookingNotice: string;
  refundPolicy: string;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminSiteService = () => request.get("/camp/site");

export const updateCampAdminSiteService = (payload: CampSiteAdminPayload) =>
  request.put("/camp/site", payload);

export type CampZoneAdminPayload = {
  id?: number;
  campSiteId?: number;
  name: string;
  slug: string;
  zoneType: string;
  description: string;
  sortOrder: number;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminZoneListService = () => request.get("/camp/zone/list");

export const createCampAdminZoneService = (payload: CampZoneAdminPayload) =>
  request.post("/camp/zone", payload);

export const updateCampAdminZoneService = (payload: CampZoneAdminPayload) =>
  request.put("/camp/zone", payload);

export const deleteCampAdminZoneService = (id: number) =>
  request.delete(`/camp/zone/${id}`);

export type CampUnitAdminPayload = {
  id?: number;
  campSiteId?: number;
  zoneId: number | null;
  name: string;
  unitType: string;
  description: string;
  maxGuests: number;
  maxVehicles: number;
  maxTents: number;
  hasShelter: boolean;
  electricityAvailable: boolean;
  privateBathroom: boolean;
  quantity: number;
  sortOrder: number;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminUnitListService = () => request.get("/camp/unit/list");

export const createCampAdminUnitService = (payload: CampUnitAdminPayload) =>
  request.post("/camp/unit", payload);

export const updateCampAdminUnitService = (payload: CampUnitAdminPayload) =>
  request.put("/camp/unit", payload);

export const deleteCampAdminUnitService = (id: number) =>
  request.delete(`/camp/unit/${id}`);

export type RatePlanAdminPayload = {
  id?: number;
  campSiteId?: number;
  campUnitId: number | null;
  name: string;
  dayType: string;
  basePrice: number;
  extraPersonPrice: number;
  cleaningFee: number;
  description: string;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminRatePlanListService = () =>
  request.get("/camp/rate-plan/list");

export const createCampAdminRatePlanService = (payload: RatePlanAdminPayload) =>
  request.post("/camp/rate-plan", payload);

export const updateCampAdminRatePlanService = (payload: RatePlanAdminPayload) =>
  request.put("/camp/rate-plan", payload);

export const deleteCampAdminRatePlanService = (id: number) =>
  request.delete(`/camp/rate-plan/${id}`);

export type AvailabilityAdminPayload = {
  id?: number;
  campSiteId?: number;
  campUnitId: number | null;
  availableDate: string;
  totalQuantity: number;
  reservedQuantity: number;
  blockedQuantity: number;
  priceOverride?: number | null;
  isClosed: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export type AvailabilityAdminQuery = {
  campUnitId?: number | null;
  startDate?: string;
  endDate?: string;
};

export const getCampAdminAvailabilityListService = (query: AvailabilityAdminQuery = {}) =>
  request.get("/camp/availability/list", { params: query });

export const createCampAdminAvailabilityService = (payload: AvailabilityAdminPayload) =>
  request.post("/camp/availability", payload);

export const updateCampAdminAvailabilityService = (payload: AvailabilityAdminPayload) =>
  request.put("/camp/availability", payload);

export const deleteCampAdminAvailabilityService = (id: number) =>
  request.delete(`/camp/availability/${id}`);

export type RentalItemAdminPayload = {
  id?: number;
  campSiteId?: number;
  name: string;
  category: string;
  description: string;
  imageUrl: string;
  unitPrice: number;
  totalQuantity: number;
  sortOrder: number;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminRentalItemListService = () =>
  request.get("/camp/rental-item/list");

export const createCampAdminRentalItemService = (payload: RentalItemAdminPayload) =>
  request.post("/camp/rental-item", payload);

export const updateCampAdminRentalItemService = (payload: RentalItemAdminPayload) =>
  request.put("/camp/rental-item", payload);

export const deleteCampAdminRentalItemService = (id: number) =>
  request.delete(`/camp/rental-item/${id}`);

export const uploadCampAdminImageService = (formData: FormData) =>
  request.post("/upload/admin-image", formData, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
  });

export type AnnouncementAdminPayload = {
  id?: number;
  campSiteId?: number;
  title: string;
  content: string;
  publishStartTime?: string | null;
  publishEndTime?: string | null;
  isPinned: boolean;
  isActive: boolean;
  createdBy?: number | null;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminAnnouncementListService = () =>
  request.get("/camp/announcement");

export const createCampAdminAnnouncementService = (payload: AnnouncementAdminPayload) =>
  request.post("/camp/announcement", payload);

export const updateCampAdminAnnouncementService = (payload: AnnouncementAdminPayload) =>
  request.put("/camp/announcement", payload);

export const deleteCampAdminAnnouncementService = (id: number) =>
  request.delete(`/camp/announcement/${id}`);

export type FaqAdminPayload = {
  id?: number;
  campSiteId?: number;
  question: string;
  answer: string;
  sortOrder: number;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminFaqListService = () => request.get("/camp/faq");

export const createCampAdminFaqService = (payload: FaqAdminPayload) =>
  request.post("/camp/faq", payload);

export const updateCampAdminFaqService = (payload: FaqAdminPayload) =>
  request.put("/camp/faq", payload);

export const deleteCampAdminFaqService = (id: number) =>
  request.delete(`/camp/faq/${id}`);

export type SiteImageAdminPayload = {
  id?: number;
  campSiteId?: number;
  zoneId?: number | null;
  zoneName?: string;
  title: string;
  imageUrl: string;
  altText: string;
  sortOrder: number;
  isCover: boolean;
  isActive: boolean;
  createdTime?: string;
  modifiedTime?: string;
};

export const getCampAdminGalleryListService = () => request.get("/camp/gallery");

export const createCampAdminGalleryService = (payload: SiteImageAdminPayload) =>
  request.post("/camp/gallery", payload);

export const updateCampAdminGalleryService = (payload: SiteImageAdminPayload) =>
  request.put("/camp/gallery", payload);

export const deleteCampAdminGalleryService = (id: number) =>
  request.delete(`/camp/gallery/${id}`);
