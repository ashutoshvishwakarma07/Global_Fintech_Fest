"use client";

import React, { useState, useEffect } from "react";
import { User, UploadRecord, QueuedUploadItem } from "@/types";
import { mockUploadService } from "@/services/mockUploadService";
import { indexedDBService } from "@/services/indexedDBService";
import { apiService } from "@/services/apiService";
import { imageProcessing } from "@/utils/imageProcessing";
import { getNetworkIsOnline } from "@/hooks/useNetworkStatus";
import {
  RefreshCw,
  Clock,
  User as UserIcon,
  Mail,
  Phone,
  Briefcase,
  AlertCircle,
  FileText,
  ArrowLeft,
  CheckCircle2,
  WifiOff,
  CloudUpload,
} from "lucide-react";

interface UploadFormProps {
  user: User;
  imagePreviewUrl: string;
  onCancel: () => void;
  onUploadSuccess: (record: UploadRecord, isOfflineSaved: boolean) => void;
  onRetake: () => void;
}

export const UploadForm: React.FC<UploadFormProps> = ({
  user,
  imagePreviewUrl,
  onCancel,
  onUploadSuccess,
  onRetake,
}) => {
  const [fullName, setFullName] = useState(user.name);
  const [email, setEmail] = useState(user.email);
  const [mobile, setMobile] = useState(user.mobile || "9876543210");
  const [role] = useState(user.role);
  const [notes, setNotes] = useState("");
  const [timestampInfo, setTimestampInfo] = useState({ raw: "", display: "" });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isCurrentlyOnline, setIsCurrentlyOnline] = useState(true);

  useEffect(() => {
    const time = mockUploadService.formatTimestamp();
    setTimestampInfo(time);
    setIsCurrentlyOnline(getNetworkIsOnline());

    const updateNet = () => setIsCurrentlyOnline(getNetworkIsOnline());
    window.addEventListener("online", updateNet);
    window.addEventListener("offline", updateNet);
    window.addEventListener("app-network-change", updateNet);

    return () => {
      window.removeEventListener("online", updateNet);
      window.removeEventListener("offline", updateNet);
      window.removeEventListener("app-network-change", updateNet);
    };
  }, []);

  const validate = (): boolean => {
    if (!fullName.trim()) {
      setError("Full Name is required.");
      return false;
    }
    if (!email.trim() || !email.includes("@")) {
      setError("A valid Email address is required.");
      return false;
    }
    const cleanMobile = mobile.replace(/[^0-9]/g, "");
    if (cleanMobile.length < 10) {
      setError("Please enter a valid 10-digit mobile number.");
      return false;
    }
    if (!imagePreviewUrl) {
      setError("Please capture or select an image before submitting.");
      return false;
    }
    return true;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      // 1. Process and compress image into a high-quality JPEG Blob
      const processed = await imageProcessing.compressToBlob(imagePreviewUrl, 1200, 0.8);
      const nextId = await mockUploadService.generateNextRecordId();
      const capturedAtISO = new Date().toISOString();
      const displayTimestamp = timestampInfo.raw || mockUploadService.formatTimestamp().raw;

      const queuedItem: QueuedUploadItem = {
        id: nextId,
        imageBlob: processed.blob,
        imageName: `${nextId}.jpg`,
        imageType: "image/jpeg",
        user: {
          id: user.id,
          name: fullName.trim(),
          email: email.trim(),
          mobile: mobile.trim(),
          role: role,
        },
        capturedAt: displayTimestamp,
        status: "pending",
        retryCount: 0,
        createdAt: capturedAtISO,
        lastAttemptAt: null,
        errorMessage: null,
        notes: notes.trim() || undefined,
        fileSize: processed.sizeFormatted,
      };

      // 2. Check current connectivity
      const online = getNetworkIsOnline();

      if (!online) {
        // OFFLINE BEHAVIOUR:
        // Do not attempt API call. Save Blob directly to IndexedDB.
        await indexedDBService.saveToQueue(queuedItem);

        const offlineRecord: UploadRecord = {
          id: queuedItem.id,
          imageUrl: processed.blobUrl,
          uploadedBy: queuedItem.user.name,
          email: queuedItem.user.email,
          mobile: queuedItem.user.mobile,
          role: queuedItem.user.role,
          uploadedAt: queuedItem.capturedAt,
          status: "Pending Upload",
          notes: queuedItem.notes,
          fileSize: queuedItem.fileSize,
          isOffline: true,
        };

        onUploadSuccess(offlineRecord, true);
        return;
      }

      // ONLINE BEHAVIOUR:
      // Attempt upload to backend
      try {
        const response = await apiService.uploadImage(queuedItem);

        if (response.success && (response.status === 200 || response.status === 201)) {
          const uploadedRecord: UploadRecord = {
            id: nextId,
            imageUrl: response.serverUrl || processed.blobUrl,
            uploadedBy: queuedItem.user.name,
            email: queuedItem.user.email,
            mobile: queuedItem.user.mobile,
            role: queuedItem.user.role,
            uploadedAt: queuedItem.capturedAt,
            status: "Uploaded",
            notes: queuedItem.notes,
            fileSize: queuedItem.fileSize,
            isOffline: false,
          };

          await mockUploadService.saveRecordDirect(uploadedRecord);
          await indexedDBService.removeFromQueue(nextId);
          onUploadSuccess(uploadedRecord, false);
        } else {
          throw new Error(`Upload returned status ${response.status}`);
        }
      } catch (uploadError: any) {
        console.warn("Online upload failed, falling back safely to IndexedDB:", uploadError);

        // Fallback: Save to IndexedDB so photo is NEVER lost!
        queuedItem.errorMessage = uploadError.message || "Failed during initial transmission";
        await indexedDBService.saveToQueue(queuedItem);

        const fallbackRecord: UploadRecord = {
          id: nextId,
          imageUrl: processed.blobUrl,
          uploadedBy: queuedItem.user.name,
          email: queuedItem.user.email,
          mobile: queuedItem.user.mobile,
          role: queuedItem.user.role,
          uploadedAt: queuedItem.capturedAt,
          status: "Pending Upload",
          notes: queuedItem.notes,
          fileSize: queuedItem.fileSize,
          isOffline: true,
          errorMessage: queuedItem.errorMessage,
        };

        onUploadSuccess(fallbackRecord, true);
      }
    } catch (err: any) {
      console.error("Critical upload error:", err);
      setError("Failed to prepare and save photo. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="w-full max-w-xl mx-auto pb-24 md:pb-12 animate-in fade-in slide-in-from-bottom-3 duration-300">
      {/* Top Bar with back */}
      <div className="flex items-center justify-between mb-4">
        <button
          type="button"
          onClick={onCancel}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-600 hover:text-slate-900 bg-white border border-slate-200 px-3 py-1.5 rounded-xl touch-target-min active:scale-95 transition-all shadow-xs"
        >
          <ArrowLeft className="w-4 h-4" />
          Back
        </button>

        <div className="flex items-center gap-2">
          {!isCurrentlyOnline ? (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-rose-100 text-rose-800 text-[10px] font-bold">
              <WifiOff className="w-3 h-3" /> Offline Mode
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
              <CloudUpload className="w-3 h-3" /> Online Upload
            </span>
          )}
        </div>
      </div>

      <div className="bg-white rounded-3xl p-5 sm:p-7 shadow-card border border-slate-100">
        {/* Offline Banner alert inside form */}
        {!isCurrentlyOnline && (
          <div className="mb-4 p-3 rounded-2xl bg-amber-50 border border-amber-200/90 text-amber-900 flex items-start gap-2.5 text-xs">
            <WifiOff className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-bold">Offline Image Capture Active</p>
              <p className="text-[11px] opacity-90 mt-0.5">
                This image and your verification details will be saved to IndexedDB on this device and synced automatically once your connection returns.
              </p>
            </div>
          </div>
        )}

        {/* Image Preview Container */}
        <div className="relative rounded-2xl overflow-hidden bg-slate-900 aspect-[4/3] w-full shadow-inner mb-5">
          <img
            src={imagePreviewUrl}
            alt="Captured verification"
            className="w-full h-full object-contain"
          />

          {/* Retake Floating Action */}
          <button
            type="button"
            onClick={onRetake}
            className="absolute bottom-3 right-3 px-3.5 py-2 rounded-xl bg-black/60 hover:bg-black/80 backdrop-blur-md text-white text-xs font-semibold flex items-center gap-1.5 shadow-md active:scale-95 transition-all touch-target-min border border-white/20"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Retake Photo
          </button>

          {/* Captured Tag */}
          <div className="absolute top-3 left-3 px-2.5 py-1 rounded-lg bg-emerald-500/90 backdrop-blur-md text-white text-[11px] font-bold flex items-center gap-1">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Ready for {isCurrentlyOnline ? "Upload" : "Offline Save"}
          </div>
        </div>

        {error && (
          <div className="mb-5 p-3.5 rounded-xl bg-rose-50 border border-rose-200 flex items-start gap-2.5 text-rose-800 text-sm">
            <AlertCircle className="w-4 h-4 mt-0.5 shrink-0 text-rose-600" />
            <span className="text-xs font-medium">{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Timestamp Display */}
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/70 flex items-center justify-between text-xs">
            <div className="flex items-center gap-2 text-slate-500">
              <Clock className="w-4 h-4 text-indigo-500" />
              <span>Capture Timestamp:</span>
            </div>
            <span className="font-semibold text-slate-800 font-mono">
              {timestampInfo.display || "Just now"}
            </span>
          </div>

          {/* User Details Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3.5">
            {/* Full Name */}
            <div>
              <label htmlFor="fullName" className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                Full Name *
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <UserIcon className="w-4 h-4" />
                </div>
                <input
                  id="fullName"
                  type="text"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none touch-target-min"
                  required
                />
              </div>
            </div>

            {/* Role */}
            <div>
              <label htmlFor="role" className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                User Role *
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Briefcase className="w-4 h-4" />
                </div>
                <input
                  id="role"
                  type="text"
                  value={role}
                  readOnly
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-100 border border-slate-200 rounded-xl text-xs font-semibold text-slate-700 cursor-not-allowed touch-target-min"
                />
              </div>
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                Email Address *
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none touch-target-min"
                  required
                />
              </div>
            </div>

            {/* Mobile Number */}
            <div>
              <label htmlFor="mobile" className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
                Mobile Number *
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
                  <Phone className="w-4 h-4" />
                </div>
                <input
                  id="mobile"
                  type="tel"
                  value={mobile}
                  onChange={(e) => setMobile(e.target.value)}
                  placeholder="9876543210"
                  maxLength={15}
                  className="w-full pl-9 pr-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none touch-target-min"
                  required
                />
              </div>
            </div>
          </div>

          {/* Optional Notes */}
          <div>
            <label htmlFor="notes" className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1">
              Field Notes / Purpose (Optional)
            </label>
            <div className="relative">
              <div className="absolute top-2.5 left-3 pointer-events-none text-slate-400">
                <FileText className="w-4 h-4" />
              </div>
              <textarea
                id="notes"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Add verification notes or site location context..."
                className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-900 focus:bg-white focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Submit Action */}
          <div className="pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className={`w-full py-3.5 px-4 font-bold text-sm rounded-xl shadow-lg active:scale-[0.99] transition-all flex items-center justify-center gap-2 touch-target-min disabled:opacity-60 text-white ${
                !isCurrentlyOnline
                  ? "bg-amber-600 hover:bg-amber-700 shadow-amber-200"
                  : "bg-indigo-600 hover:bg-indigo-700 shadow-indigo-200"
              }`}
            >
              {isSubmitting ? (
                <>
                  <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>{isCurrentlyOnline ? "Uploading to Cloud..." : "Saving Locally..."}</span>
                </>
              ) : (
                <>
                  <span>
                    {isCurrentlyOnline ? "Submit / Upload Image" : "Save Image Offline"}
                  </span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
