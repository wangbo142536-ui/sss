export type BackgroundTaskRecoverySnapshot = {
  jobId: string;
  fileName: string;
  startedAt: number;
};

export function readBackgroundTaskRecovery(storageKey: string): BackgroundTaskRecoverySnapshot | null {
  try {
    const raw = window.localStorage.getItem(storageKey);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<BackgroundTaskRecoverySnapshot>;
    const jobId = String(parsed.jobId || "").trim();
    const startedAt = Number(parsed.startedAt);
    if (!jobId || !Number.isFinite(startedAt) || startedAt <= 0) return null;
    return {
      jobId,
      fileName: String(parsed.fileName || "").trim(),
      startedAt
    };
  } catch {
    return null;
  }
}

export function writeBackgroundTaskRecovery(storageKey: string, snapshot: BackgroundTaskRecoverySnapshot): void {
  try {
    window.localStorage.setItem(storageKey, JSON.stringify(snapshot));
  } catch {
    // Storage may be unavailable in private or restricted browser contexts.
  }
}

export function clearBackgroundTaskRecovery(storageKey: string): void {
  try {
    window.localStorage.removeItem(storageKey);
  } catch {
    // Recovery is best-effort and must never block the background task itself.
  }
}
