export type UserRole = "Field User" | "Supervisor" | "Admin" | "Operations" | "Partner" | "Lender";

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  avatar?: string;
  mobile?: string;
}

export interface ManagedUser {
  id: number;
  email: string;
  name: string;
  role: UserRole;
  mobile?: string;
  active: boolean;
  createdAt: string;
  updatedAt?: string;
}

export type RecordStatus = "Uploaded" | "Verified" | "Processing" | "Failed";

export type DocumentType =
  | "Visiting Card"
  | "Business Card"
  | "PAN Card"
  | "Aadhaar Card"
  | "Driving License"
  | "Passport"
  | "Voter ID"
  | "POS Certificate"
  | "Invoice / Receipt"
  | "General KYC";

export interface ExtractedData {
  documentType: DocumentType;
  documentNumber?: string | null;
  // 14 Standardized Visiting Card Fields (Explicitly null if not detected)
  name: string | null;
  jobTitle: string | null;
  companyName: string | null;
  department: string | null;
  emailAddress: string | null;
  mobileNumber: string | null;
  workNumber: string | null;
  websiteUrl: string | null;
  city: string | null;
  state: string | null;
  postalZipCode: string | null;
  country: string | null;
  linkedIn: string | null;
  twitter: string | null;
  // Compatibility & metadata fields
  cardHolderName?: string | null;
  extractedName?: string | null;
  designation?: string | null;
  extractedEmail?: string | null;
  extractedMobile?: string | null;
  extractedAddress?: string | null;
  website?: string | null;
  issueDate?: string | null;
  confidence: number; // e.g. 98.6%
  rawText?: string | null;
}

export type CaptureMode = "single" | "two-sided";

export interface UploadRecord {
  id: string;
  imageUrl: string;
  uploadedBy: string;
  userId: string;
  email: string;
  mobile: string;
  role: UserRole;
  uploadedAt: string; // e.g. "2026-09-04 18:30"
  status: RecordStatus;
  notes?: string;
  isOffline?: boolean;
  retryCount?: number;
  errorMessage?: string | null;
  ocrStatus?: string;
  // 14 Standardized Fields
  name?: string | null;
  jobTitle?: string | null;
  companyName?: string | null;
  department?: string | null;
  emailAddress?: string | null;
  mobileNumber?: string | null;
  workNumber?: string | null;
  websiteUrl?: string | null;
  city?: string | null;
  state?: string | null;
  postalZipCode?: string | null;
  country?: string | null;
  linkedIn?: string | null;
  twitter?: string | null;
  // Compatibility aliases
  cardHolderName?: string | null;
  designation?: string | null;
  extractedEmail?: string | null;
  extractedMobile?: string | null;
  extractedAddress?: string | null;
  rawOcrText?: string | null;
  extractedData?: ExtractedData;
  captureMode?: CaptureMode;
  frontImageUrl?: string;
  backImageUrl?: string;
  fileSize?: string;
  s3Url?: string;
  numericId?: number;
  emailSentAt?: string;
}

export interface ShareCardPayload {
  leadEmail: string;
  subject?: string;
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
  documentTypeHint?: DocumentType;
  notes?: string;
  captureMode?: CaptureMode;
  frontImageUrl?: string;
  backImageUrl?: string;
}

export interface FilterState {
  searchQuery: string;
  role: "All" | UserRole;
  status: "All" | RecordStatus;
  uploader: "All" | string;
  documentType: "All" | DocumentType;
  sortBy: "latest" | "oldest";
}
