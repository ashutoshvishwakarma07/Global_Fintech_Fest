import { UploadRecord, User } from "@/types";

const RECORDS_STORAGE_KEY = "gff_uploaded_records_online";

const INITIAL_SEED_RECORDS: UploadRecord[] = [
  {
    id: "IMG-1001",
    imageUrl: "https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Rahul Sharma",
    userId: "usr_1",
    email: "user1@demo.com",
    mobile: "9876543210",
    role: "Field User",
    uploadedAt: "2026-09-04 18:30",
    status: "Uploaded",
    notes: "Merchant onboarding document verification at North Mumbai hub",
    fileSize: "1.4 MB",
    extractedData: {
      documentType: "PAN Card",
      documentNumber: "ABCDE1234F",
      extractedName: "Rahul Sharma",
      issueDate: "2021-08-12",
      confidence: 99.2,
      rawText: "INCOME TAX DEPARTMENT, GOVT OF INDIA\nPermanent Account Number: ABCDE1234F\nName: Rahul Sharma",
    },
  },
  {
    id: "IMG-1002",
    imageUrl: "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Priya Verma",
    userId: "usr_2",
    email: "user2@demo.com",
    mobile: "9812345678",
    role: "Supervisor",
    uploadedAt: "2026-09-04 16:15",
    status: "Verified",
    notes: "POS terminal hardware deployment certificate check",
    fileSize: "2.1 MB",
    extractedData: {
      documentType: "POS Certificate",
      documentNumber: "POS-TID-884920",
      extractedName: "Priya Verma",
      issueDate: "2024-02-19",
      confidence: 98.7,
      rawText: "MERCHANT ONBOARDING TERMINAL CERTIFICATION\nTID: 884920\nHardware ID: HW-2918\nSupervisor: Priya Verma",
    },
  },
  {
    id: "IMG-1003",
    imageUrl: "https://images.unsplash.com/photo-1563986768609-322da13575f3?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Rahul Sharma",
    userId: "usr_1",
    email: "user1@demo.com",
    mobile: "9876543210",
    role: "Field User",
    uploadedAt: "2026-09-03 11:45",
    status: "Verified",
    notes: "Storefront KYC photo compliance check",
    fileSize: "980 KB",
    extractedData: {
      documentType: "Aadhaar Card",
      documentNumber: "4812-9901-4421",
      extractedName: "Rahul Sharma",
      issueDate: "2019-11-04",
      confidence: 97.9,
      rawText: "UNIQUE IDENTIFICATION AUTHORITY OF INDIA\nAadhaar: 4812 9901 4421\nDOB: 14/05/1992",
    },
  },
  {
    id: "IMG-1004",
    imageUrl: "https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?w=600&auto=format&fit=crop&q=80",
    uploadedBy: "Priya Verma",
    userId: "usr_2",
    email: "user2@demo.com",
    mobile: "9812345678",
    role: "Supervisor",
    uploadedAt: "2026-09-02 14:20",
    status: "Verified",
    notes: "Quarterly audit documentation",
    fileSize: "1.8 MB",
    extractedData: {
      documentType: "Invoice / Receipt",
      documentNumber: "INV-2026-0891",
      extractedName: "Priya Verma",
      issueDate: "2026-08-30",
      confidence: 99.4,
      rawText: "TAX INVOICE - GFF FIELD SERVICES\nInvoice #: INV-2026-0891\nAmount: INR 4,250.00",
    },
  },
];

export const mockUploadService = {
  getAllStoredRecords(): UploadRecord[] {
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
    const allRecords = this.getAllStoredRecords();
    const maxNum = allRecords.reduce((max, r) => {
      const match = r.id.match(/IMG-(\d+)/);
      if (match) {
        const num = parseInt(match[1], 10);
        return num > max ? num : max;
      }
      return max;
    }, 1000);

    return `IMG-${maxNum + 1}`;
  },

  saveRecord(record: UploadRecord): void {
    if (typeof window === "undefined") return;
    const records = this.getAllStoredRecords();
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
