export type UserRole = "Field User" | "Supervisor";

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  avatar?: string;
  mobile?: string;
}

export type RecordStatus =
  | "Uploaded"
  | "Pending Upload"
  | "Uploading"
  | "Failed"
  | "Verified"
  | "Pending";

export interface UploadRecord {
  id: string;
  imageUrl: string;
  uploadedBy: string;
  email: string;
  mobile: string;
  role: UserRole;
  uploadedAt: string; // e.g. "2026-09-04 18:30"
  status: RecordStatus;
  notes?: string;
  fileSize?: string;
  isOffline?: boolean;
  retryCount?: number;
  errorMessage?: string | null;
  ocrStatus?: string;
  cardHolderName?: string;
  companyName?: string;
  designation?: string;
  extractedEmail?: string;
  extractedMobile?: string;
  extractedAddress?: string;
  rawOcrText?: string;
}

export interface QueuedUploadItem {
  id: string;
  imageBlob: Blob;
  imageName: string;
  imageType: string;
  user: {
    id: string;
    name: string;
    email: string;
    mobile: string;
    role: UserRole;
  };
  capturedAt: string;
  status: "pending" | "uploading" | "failed" | "uploaded";
  retryCount: number;
  createdAt: string;
  lastAttemptAt: string | null;
  errorMessage: string | null;
  notes?: string;
  fileSize?: string;
}

export interface UploadFormData {
  fullName: string;
  email: string;
  mobile: string;
  role: UserRole;
  timestamp: string;
  imageBlobUrl: string;
  imageBlob?: Blob;
  notes?: string;
}

export interface FilterState {
  searchQuery: string;
  role: "All" | UserRole;
  status: "All" | RecordStatus;
  sortBy: "latest" | "oldest";
}

export interface SyncProgressState {
  isSyncing: boolean;
  current: number;
  total: number;
  message: string;
}
