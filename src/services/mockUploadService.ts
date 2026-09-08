import { UploadRecord, User } from "@/types";

const RECORDS_STORAGE_KEY = "gff_uploaded_records_online";

const INITIAL_SEED_RECORDS: UploadRecord[] = [];

let inMemoryRecords: UploadRecord[] | null = null;

export const mockUploadService = {
  getAllStoredRecords(): UploadRecord[] {
    if (inMemoryRecords && inMemoryRecords.length > 0) {
      return inMemoryRecords;
    }
    if (typeof window === "undefined") return [];
    try {
      const stored = localStorage.getItem(RECORDS_STORAGE_KEY);
      if (!stored) {
        return [];
      }
      const parsed: UploadRecord[] = JSON.parse(stored);
      const sanitized = (parsed || []).filter(
        (r) =>
          !r.cardHolderName?.includes("NONI SONANI") &&
          !r.uploadedBy?.includes("Rahul Sharma") &&
          !r.uploadedBy?.includes("Priya Verma") &&
          !r.extractedData?.rawText?.includes("NONI SONANI") &&
          !r.extractedData?.rawText?.includes("INCOME TAX")
      );
      if (sanitized.length !== (parsed || []).length) {
        localStorage.setItem(RECORDS_STORAGE_KEY, JSON.stringify(sanitized));
      }
      inMemoryRecords = sanitized;
      return sanitized;
    } catch {
      return [];
    }
  },

  /**
   * Enforces strict role-based access control:
   * - User 1 sees only User 1's records
   * - User 2 sees only User 2's records
   * - Admin sees records from ALL users (User 1 + User 2 + Admin)
   */
  getRecordsForUser(user: User): UploadRecord[] {
    const allRecords = this.getAllStoredRecords();
    if (user.role === "Admin") {
      return allRecords;
    }
    return allRecords.filter(
      (r) =>
        r.userId === user.id ||
        r.email.toLowerCase() === user.email.toLowerCase()
    );
  },

  generateNextRecordId(): string {
    const storedLast = typeof window !== "undefined" ? localStorage.getItem("gff_last_seq_id") : null;
    let nextNum = storedLast ? parseInt(storedLast, 10) + 1 : 1008;
    if (isNaN(nextNum) || nextNum <= 1007) nextNum = 1008;

    const allRecords = this.getAllStoredRecords();
    allRecords.forEach((r) => {
      const match = r.id.match(/IMG-(\d+)/);
      if (match) {
        const num = parseInt(match[1], 10);
        if (num >= nextNum) nextNum = num + 1;
      }
    });

    if (typeof window !== "undefined") {
      localStorage.setItem("gff_last_seq_id", String(nextNum));
    }
    return `IMG-${nextNum}`;
  },

  saveRecord(record: UploadRecord): void {
    const records = this.getAllStoredRecords();
    const existingIndex = records.findIndex((r) => r.id === record.id);
    let updated: UploadRecord[];
    if (existingIndex >= 0) {
      updated = [...records];
      updated[existingIndex] = record;
    } else {
      updated = [record, ...records];
    }
    inMemoryRecords = updated;

    if (typeof window === "undefined") return;

    // Safely save to localStorage without crashing if browser 5MB storage quota is reached
    try {
      localStorage.setItem(RECORDS_STORAGE_KEY, JSON.stringify(updated));
    } catch (quotaError) {
      console.warn("[mockUploadService] LocalStorage quota reached, pruning heavy base64 strings:", quotaError);
      try {
        const pruned = updated.map((r, idx) => {
          if (idx > 1 && r.imageUrl?.startsWith("data:")) {
            return { ...r, imageUrl: r.s3Url || "", frontImageUrl: undefined, backImageUrl: undefined };
          }
          return r;
        });
        localStorage.setItem(RECORDS_STORAGE_KEY, JSON.stringify(pruned));
      } catch {
        try {
          localStorage.removeItem(RECORDS_STORAGE_KEY);
          localStorage.setItem(RECORDS_STORAGE_KEY, JSON.stringify([record]));
        } catch {
          // In-memory cache will still keep all records active during the session
        }
      }
    }
  },

  deleteRecord(id: string): void {
    if (typeof window === "undefined") return;
    const records = this.getAllStoredRecords().filter((r) => r.id !== id);
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
