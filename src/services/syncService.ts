import { QueuedUploadItem, SyncProgressState, UploadRecord } from "@/types";
import { indexedDBService } from "./indexedDBService";
import { apiService } from "./apiService";
import { getNetworkIsOnline } from "@/hooks/useNetworkStatus";
import { mockUploadService } from "./mockUploadService";

type SyncListener = (state: SyncProgressState) => void;
type RecordSyncedCallback = (record: UploadRecord) => void;

class SyncService {
  private isSyncing = false;
  private listeners: SyncListener[] = [];
  private onRecordSyncedCallbacks: RecordSyncedCallback[] = [];
  private retryTimers = new Map<string, NodeJS.Timeout>();

  /**
   * Subscribe to sync progress updates
   */
  subscribe(listener: SyncListener): () => void {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener);
    };
  }

  /**
   * Subscribe to individual record sync completion
   */
  onRecordSynced(callback: RecordSyncedCallback): () => void {
    this.onRecordSyncedCallbacks.push(callback);
    return () => {
      this.onRecordSyncedCallbacks = this.onRecordSyncedCallbacks.filter((c) => c !== callback);
    };
  }

  private notifyProgress(state: SyncProgressState) {
    this.listeners.forEach((l) => l(state));
  }

  /**
   * Calculate next retry delay based on attempt count
   * Retry 1 -> 5s
   * Retry 2 -> 15s
   * Retry 3 -> 30s
   * Retry 4+ -> 60s
   */
  getRetryDelayMs(retryCount: number): number {
    switch (retryCount) {
      case 0:
      case 1:
        return 5000;
      case 2:
        return 15000;
      case 3:
        return 30000;
      default:
        return 60000;
    }
  }

  /**
   * Main sequential synchronization loop
   * Guarantees that items are deleted from IndexedDB ONLY after confirmed 200/201 response.
   */
  async syncPendingUploads(): Promise<{ total: number; synced: number; failed: number }> {
    // 1. Strict synchronization lock to prevent duplicate sync loops
    if (this.isSyncing) {
      console.log("[SyncService] Sync already in progress, ignoring concurrent trigger.");
      return { total: 0, synced: 0, failed: 0 };
    }

    if (!getNetworkIsOnline()) {
      console.log("[SyncService] Offline: Cannot initiate sync.");
      return { total: 0, synced: 0, failed: 0 };
    }

    const pendingItems = await indexedDBService.getPendingQueue();
    if (pendingItems.length === 0) {
      this.notifyProgress({ isSyncing: false, current: 0, total: 0, message: "Queue is clean." });
      return { total: 0, synced: 0, failed: 0 };
    }

    this.isSyncing = true;
    let synced = 0;
    let failed = 0;
    const total = pendingItems.length;

    this.notifyProgress({
      isSyncing: true,
      current: 0,
      total,
      message: `Starting sync of ${total} pending upload${total === 1 ? "" : "s"}...`,
    });

    // 2. Process queued items strictly sequentially
    for (let i = 0; i < total; i++) {
      const item = pendingItems[i];

      // Re-check network before each queued item
      if (!getNetworkIsOnline()) {
        console.warn("[SyncService] Network disconnected during sequential sync. Halting.");
        break;
      }

      this.notifyProgress({
        isSyncing: true,
        current: i + 1,
        total,
        message: `Syncing ${i + 1} of ${total}: ${item.id}...`,
      });

      // Mark status as uploading in IndexedDB during transmission
      await indexedDBService.updateQueueItem(item.id, {
        status: "uploading",
        lastAttemptAt: new Date().toISOString(),
      });

      try {
        const response = await this.uploadQueuedImage(item);

        // ONLY delete after confirmed successful response (200 or 201)
        if (response.success && (response.status === 200 || response.status === 201)) {
          synced++;

          // Clear any scheduled retry timer
          if (this.retryTimers.has(item.id)) {
            clearTimeout(this.retryTimers.get(item.id)!);
            this.retryTimers.delete(item.id);
          }

          // A. Save finalized record to permanent storage
          const uploadedRecord = await this.markAsUploaded(item, response.serverUrl);

          // B. Delete item from IndexedDB queue
          await indexedDBService.removeFromQueue(item.id);

          // C. Verify deletion was committed
          const isDeleted = await indexedDBService.verifyDeleted(item.id);
          if (!isDeleted) {
            console.warn(`[SyncService] Verification failed for ${item.id}, retrying delete...`);
            await indexedDBService.removeFromQueue(item.id);
          }

          // D. Revoke blob object URL
          mockUploadService.revokeBlobUrl(item.id);

          // E. Notify callbacks to refresh UI immediately
          this.onRecordSyncedCallbacks.forEach((cb) => cb(uploadedRecord));
        } else {
          throw new Error(`Unexpected server response code: ${response.status}`);
        }
      } catch (err: any) {
        failed++;
        console.warn(`[SyncService] Upload failed for ${item.id}:`, err);

        const newRetryCount = (item.retryCount || 0) + 1;
        const errorMsg = err.message || "Network error during upload";

        // Keep the record in IndexedDB with failed status so it can be retried later
        await indexedDBService.updateQueueItem(item.id, {
          status: "failed",
          retryCount: newRetryCount,
          errorMessage: errorMsg,
          lastAttemptAt: new Date().toISOString(),
        });

        // Schedule delayed retry
        this.scheduleRetry(item.id, newRetryCount);
      }
    }

    this.isSyncing = false;

    const summaryMessage =
      failed === 0
        ? `${synced} upload${synced === 1 ? "" : "s"} synced successfully`
        : `${synced} synced, ${failed} still pending`;

    this.notifyProgress({
      isSyncing: false,
      current: synced,
      total,
      message: summaryMessage,
    });

    return { total, synced, failed };
  }

  /**
   * Upload an individual queued item to API
   */
  async uploadQueuedImage(item: QueuedUploadItem) {
    return await apiService.uploadImage(item);
  }

  /**
   * Converts a successfully synced QueuedUploadItem into a finalized UploadRecord
   */
  private async markAsUploaded(item: QueuedUploadItem, serverUrl?: string): Promise<UploadRecord> {
    const rawTime = item.capturedAt || mockUploadService.formatTimestamp().raw;
    const imageUrl = serverUrl || URL.createObjectURL(item.imageBlob);

    const record: UploadRecord = {
      id: item.id,
      imageUrl: imageUrl,
      uploadedBy: item.user.name,
      email: item.user.email,
      mobile: item.user.mobile,
      role: item.user.role,
      uploadedAt: rawTime,
      status: "Uploaded",
      notes: item.notes || "Field capture submission",
      fileSize: item.fileSize || "1.2 MB",
      isOffline: false,
      retryCount: item.retryCount,
    };

    // Save directly to permanent records repository
    await mockUploadService.saveRecordDirect(record);
    return record;
  }

  /**
   * Schedules delayed exponential retry
   */
  private scheduleRetry(itemId: string, retryCount: number) {
    if (this.retryTimers.has(itemId)) {
      clearTimeout(this.retryTimers.get(itemId)!);
    }

    const delay = this.getRetryDelayMs(retryCount);
    console.log(`[SyncService] Scheduling retry #${retryCount} for ${itemId} in ${delay / 1000}s`);

    const timer = setTimeout(async () => {
      this.retryTimers.delete(itemId);
      if (getNetworkIsOnline()) {
        const item = await indexedDBService.getQueueItem(itemId);
        if (item && (item.status === "failed" || item.status === "pending")) {
          console.log(`[SyncService] Executing scheduled retry for ${itemId}`);
          this.syncPendingUploads();
        }
      }
    }, delay);

    this.retryTimers.set(itemId, timer);
  }

  /**
   * Manually trigger sync for a specific single item
   */
  async syncSingleItem(itemId: string): Promise<boolean> {
    if (this.isSyncing) return false;

    const item = await indexedDBService.getQueueItem(itemId);
    if (!item || !getNetworkIsOnline()) return false;

    try {
      this.isSyncing = true;
      await indexedDBService.updateQueueItem(itemId, { status: "uploading" });
      const res = await this.uploadQueuedImage(item);

      // ONLY delete after confirmed 200/201
      if (res.success && (res.status === 200 || res.status === 201)) {
        const uploadedRecord = await this.markAsUploaded(item, res.serverUrl);

        // Delete from IndexedDB and verify
        await indexedDBService.removeFromQueue(itemId);
        await indexedDBService.verifyDeleted(itemId);
        mockUploadService.revokeBlobUrl(itemId);

        this.onRecordSyncedCallbacks.forEach((cb) => cb(uploadedRecord));
        this.isSyncing = false;
        return true;
      }
      this.isSyncing = false;
      return false;
    } catch (e: any) {
      this.isSyncing = false;
      await indexedDBService.updateQueueItem(itemId, {
        status: "failed",
        retryCount: (item.retryCount || 0) + 1,
        errorMessage: e.message,
      });
      return false;
    }
  }

  /**
   * Setup automated hooks: browser online event and startup check
   */
  initAutoSync() {
    if (typeof window === "undefined") return;

    window.addEventListener("online", () => {
      console.log("[SyncService] Online event received, triggering auto sync...");
      setTimeout(() => {
        this.syncPendingUploads();
      }, 800);
    });

    window.addEventListener("app-network-change", (e: any) => {
      if (e.detail?.isOnline) {
        console.log("[SyncService] app-network-change online, triggering auto sync...");
        setTimeout(() => {
          this.syncPendingUploads();
        }, 500);
      }
    });

    // Initial startup check
    setTimeout(() => {
      if (getNetworkIsOnline()) {
        this.syncPendingUploads();
      }
    }, 1500);
  }

  getIsSyncing() {
    return this.isSyncing;
  }
}

export const syncService = new SyncService();
