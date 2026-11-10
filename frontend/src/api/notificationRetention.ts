import request from '@/api/api';

export type NotificationRetentionPreview = {
  policyVersion: string; dryRun: boolean; deletionEnabled: boolean;
  observedAt: string; cutoffAt: string; timeZone: string; previewDays: number;
  totalCount: number; candidateCount: number; protectedCount: number;
  buckets: {reason: string; count: number; simulatedCount: number; skippedCount: number;
    acknowledgedCount: number; oldestActivityAt?: string; newestActivityAt?: string}[];
};
export const getNotificationRetentionPreviewService = () => request.get('/notification/retention-preview');
