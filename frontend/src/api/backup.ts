import request from '@/api/api';

export type BackupRunSummary = {
  status: 'running' | 'success' | 'failed'; startedAt: string; finishedAt?: string;
  archiveSha256?: string; errorCode?: string;
};
export type BackupMonitorStatus = {
  observedAt: string; configured: boolean; journalAvailable: boolean; sourceKey: string;
  health: 'unknown' | 'healthy' | 'info' | 'warning' | 'critical'; recordCount: number;
  maxBackupAgeSeconds: number; maxRestoreAgeSeconds: number; runTimeoutSeconds: number;
  lastBackupAttempt?: BackupRunSummary; lastRestoreAttempt?: BackupRunSummary;
  lastBackupSuccess?: BackupRunSummary; lastRestoreSuccess?: BackupRunSummary;
  latestBackupVerified: boolean; latestBackupVerifiedAt?: string;
  alerts: {code: string; severity: string; message: string; action: string}[];
};
export const getBackupMonitorService = () => request.get('/admin/backup/status');
