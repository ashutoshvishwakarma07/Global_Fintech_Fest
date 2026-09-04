import { UploadRecord, QueuedUploadItem } from "@/types";
import { indexedDBService } from "./indexedDBService";

const RECORDS_STORAGE_KEY = "gff_uploaded_records";

const INITIAL_SEED_RECORDS: UploadRecord[] = [
  {
    id: "IMG-1001",
    imageUrl: "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Rahul Sharma",
    email: "user1@demo.com",
    mobile: "9876543210",
    role: "Field User",
    uploadedAt: "2026-09-04 18:30",
    status: "Uploaded",
    notes: "Merchant onboarding document verification at North Mumbai hub",
    fileSize: "1.4 MB",
  },
  {
    id: "IMG-1002",
    imageUrl: "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Priya Verma",
    email: "user2@demo.com",
    mobile: "9812345678",
    role: "Supervisor",
    uploadedAt: "2026-09-04 16:15",
    status: "Verified",
    notes: "POS terminal hardware deployment certificate check",
    fileSize: "2.1 MB",
  },
  {
    id: "IMG-1003",
    imageUrl: "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Rahul Sharma",
    email: "user1@demo.com",
    mobile: "9876543210",
    role: "Field User",
    uploadedAt: "2026-09-03 11:45",
    status: "Pending",
    notes: "Storefront KYC photo compliance check",
    fileSize: "980 KB",
  },
  {
    id: "IMG-1004",
    imageUrl: "https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Priya Verma",
    email: "user2@demo.com",
    mobile: "9812345678",
    role: "Supervisor",
    uploadedAt: "2026-09-02 14:20",
    status: "Verified",
    notes: "Quarterly audit documentation",
    fileSize: "1.8 MB",
  },
];

// Active object URLs cache for IndexedDB blobs to prevent memory leaks
const blobUrlCache = new Map<string, string>();

export const mockUploadService = {
  getStoredUploadedRecords(): UploadRecord[] {
    if (typeof window === "undefined") return INITIAL_SEED_RECORDS;
    try {
      const stored = localStorage.getItem(RECORDS_STORAGE_KEY);
      if (!stored) {
        localStorage.setItem(RECORDS_STORAGE_KEY, JSON.stringify(INITIAL_SEED_RECORDS));
        return INITIAL_SEED_RECORDS;
      }
      return JSON.parse(stored);
    } catch {
      return INITIAL_SEED_RECORDS;
    }
  },

  /**
   * Returns merged list of both permanently uploaded records AND
   * active offline queued items from IndexedDB.
   */
  async getAllRecordsMerged(): Promise<UploadRecord[]> {
    const uploaded = this.getStoredUploadedRecords();

    let queued: QueuedUploadItem[] = [];
    try {
      queued = await indexedDBService.getAllQueue();
    } catch (e) {
      console.warn("Could not read IndexedDB queue:", e);
    }

    // Convert queued items into UploadRecord format for unified UI display
    const queuedRecords: UploadRecord[] = queued.map((q) => {
      let previewUrl = blobUrlCache.get(q.id);
      if (!previewUrl) {
        previewUrl = URL.createObjectURL(q.imageBlob);
        blobUrlCache.set(q.id, previewUrl);
      }

      let statusDisplay: UploadRecord["status"] = "Pending Upload";
      if (q.status === "uploading") statusDisplay = "Uploading";
      else if (q.status === "failed") statusDisplay = "Failed";
      else if (q.status === "uploaded") statusDisplay = "Uploaded";

      return {
        id: q.id,
        imageUrl: previewUrl,
        uploadedBy: q.user.name,
        email: q.user.email,
        mobile: q.user.mobile,
        role: q.user.role,
        uploadedAt: q.capturedAt || q.createdAt,
        status: statusDisplay,
        notes: q.notes,
        fileSize: q.fileSize || "1.2 MB",
        isOffline: true,
        retryCount: q.retryCount,
        errorMessage: q.errorMessage,
      };
    });

    // Merge: only keep queued items that are NOT already in the uploaded repository
    const uploadedIds = new Set(uploaded.map((u) => u.id));
    const uniqueQueued = queuedRecords.filter(
      (q) => !uploadedIds.has(q.id) && q.status !== "Uploaded"
    );

    return [...uniqueQueued, ...uploaded];
  },

  revokeBlobUrl(id: string): void {
    const url = blobUrlCache.get(id);
    if (url) {
      try {
        URL.revokeObjectURL(url);
      } catch {}
      blobUrlCache.delete(id);
    }
  },

  async generateNextRecordId(): Promise<string> {
    const uploaded = this.getStoredUploadedRecords();
    let queued: QueuedUploadItem[] = [];
    try {
      queued = await indexedDBService.getAllQueue();
    } catch {}

    const allIds = [...uploaded.map((r) => r.id), ...queued.map((q) => q.id)];
    const maxNum = allIds.reduce((max, id) => {
      const match = id.match(/IMG-(\d+)/);
      if (match) {
        const num = parseInt(match[1], 10);
        return num > max ? num : max;
      }
      return max;
    }, 1000);

    return `IMG-${maxNum + 1}`;
  },

  async saveRecordDirect(record: UploadRecord): Promise<void> {
    if (typeof window === "undefined") return;
    const records = this.getStoredUploadedRecords();
    const existingIndex = records.findIndex((r) => r.id === record.id);
    let updated: UploadRecord[];
    if (existingIndex >= 0) {
      updated = [...records];
      updated[existingIndex] = record;
    } else {
      updated = [record, ...records];
    }
    localStorage.setItem(RECORDS_STORAGE_KEY, JSON.stringify(updated));
  },

  deleteRecord(id: string): void {
    if (typeof window === "undefined") return;
    const records = this.getStoredUploadedRecords().filter((r) => r.id !== id);
    localStorage.setItem(RECORDS_STORAGE_KEY, JSON.stringify(records));
  },

  formatTimestamp(date: Date = new Date()): { raw: string; display: string } {
    const pad = (n: number) => n.toString().padStart(2, "0");
    const yyyy = date.getFullYear();
    const mm = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const min = pad(date.getMinutes());

    const raw = `${yyyy}-${mm}-${dd} ${hh}:${min}`;
    const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
    const hours12 = date.getHours() % 12 || 12;
    const ampm = date.getHours() >= 12 ? "PM" : "AM";
    const display = `${dd} ${months[date.getMonth()]} ${yyyy}, ${pad(hours12)}:${min} ${ampm}`;

    return { raw, display };
  },
};
