export type UserRole = "Field User" | "Supervisor" | "Admin";

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  avatar?: string;
  mobile?: string;
}

export type RecordStatus = "Uploaded" | "Verified" | "Processing" | "Failed";

export type DocumentType =
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
  documentNumber?: string;
  extractedName?: string;
  name?: string;
  issueDate?: string;
  confidence: number; // e.g. 98.6%
  rawText?: string;
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
  fileSize?: string;
  extractedData?: ExtractedData;
  captureMode?: CaptureMode;
  frontImageUrl?: string;
  backImageUrl?: string;
  s3Url?: string;
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
