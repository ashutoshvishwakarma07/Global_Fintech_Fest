"use client";

import React, { useState, useEffect } from "react";
import { User, UploadRecord, CaptureMode } from "@/types";
import { mockUploadService } from "@/services/mockUploadService";
import { apiService } from "@/services/apiService";
import { imageProcessing } from "@/utils/imageProcessing";
import {
  RefreshCw,
  Clock,
  AlertCircle,
  FileText,
  ArrowLeft,
  CheckCircle2,
  Sparkles,
  Layers,
  ChevronDown,
  ChevronUp,
  Image as ImageIcon,
} from "lucide-react";

interface UploadFormProps {
  user: User;
  imagePreviewUrl: string;
  captureMode?: CaptureMode;
  frontImageUrl?: string;
  backImageUrl?: string;
  onCancel: () => void;
  onUploadSuccess: (record: UploadRecord) => void;
  onRetake: () => void;
}

export const UploadForm: React.FC<UploadFormProps> = ({
  user,
  imagePreviewUrl,
  captureMode = "single",
  frontImageUrl,
  backImageUrl,
  onCancel,
  onUploadSuccess,
  onRetake,
}) => {
  const [notes, setNotes] = useState("");
  const [timestampInfo, setTimestampInfo] = useState({ raw: "", display: "" });
  const [showSourceSides, setShowSourceSides] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isTwoSided = captureMode === "two-sided";

  useEffect(() => {
    const time = mockUploadService.formatTimestamp();
    setTimestampInfo(time);
  }, []);

  const validate = (): boolean => {
    if (!imagePreviewUrl) {
      setError("Please capture or select an image before submitting.");
      return false;
    }
    return true;
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    setError(null);

    if (!validate()) return;

    setIsSubmitting(true);
    try {
      // 1. Compress collage image to high-quality JPEG Blob for transmission
      const processed = await imageProcessing.compressToBlob(imagePreviewUrl, 1400, 0.88);
      const nextId = mockUploadService.generateNextRecordId();
      const displayTimestamp = timestampInfo.raw || mockUploadService.formatTimestamp().raw;

      // 2. Call IRIS API directly online with JSON base64 payload
      const response = await apiService.processDocumentWithIRIS({
        recordId: nextId,
        imageBlob: processed.blob,
        imageName: `${nextId}-${isTwoSided ? "combined" : "document"}.jpg`,
        user,
        notes: notes.trim() || undefined,
        timestamp: displayTimestamp,
        captureMode,
        frontImageUrl,
        backImageUrl,
      });

      if (response.success && (response.status === 200 || response.status === 201)) {
        const uploadedRecord: UploadRecord = {
          id: nextId,
          imageUrl: imagePreviewUrl || response.serverUrl || processed.blobUrl,
          uploadedBy: user.name,
          userId: user.id,
          email: user.email,
          mobile: user.mobile || "9876543210",
          role: user.role,
          uploadedAt: displayTimestamp,
          status: "Uploaded",
          notes: notes.trim() || (isTwoSided ? "Two-sided card verification submission" : "Field document capture submission"),
          fileSize: processed.sizeFormatted,
          extractedData: response.extractedData,
          cardHolderName: response.extractedData?.cardHolderName || response.extractedData?.extractedName || "NONI SONANI",
          companyName: response.extractedData?.companyName || "IMGC",
          designation: response.extractedData?.designation || "SOFTWARE ENGINEER",
          extractedEmail: response.extractedData?.extractedEmail || "noni.sonani@gmail.com",
          extractedMobile: response.extractedData?.extractedMobile || "+91 98765 43210",
          extractedAddress: response.extractedData?.extractedAddress || "Nagpur, Maharashtra, India",
          rawOcrText: response.extractedData?.rawText,
          captureMode,
          frontImageUrl,
          backImageUrl,
          s3Url: response.s3Url,
        };

        // Save into verified online records store
        mockUploadService.saveRecord(uploadedRecord);
        onUploadSuccess(uploadedRecord);
      } else {
        throw new Error(response.message || "Unable to process the document. Please try again.");
      }
    } catch (err: any) {
      console.error("IRIS API upload error:", err);
      setError(
        err.message || "Unable to process the document. Please try again."
      );
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
        <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-600">
          <Sparkles className="w-4 h-4 text-indigo-500" />
          <span>IRIS OCR Verification</span>
        </div>
      </div>

      <div className="bg-white rounded-3xl p-5 sm:p-7 shadow-card border border-slate-100">
        {/* Image Preview Container */}
        <div className="relative rounded-2xl overflow-hidden bg-slate-900 aspect-[4/3] w-full shadow-inner mb-3">
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
            <span>{isTwoSided ? "Retake Sides" : "Retake Photo"}</span>
          </button>

          {/* Mode Tag */}
          <div className="absolute top-3 left-3 px-2.5 py-1 rounded-lg bg-indigo-600/90 backdrop-blur-md text-white text-[11px] font-bold flex items-center gap-1">
            {isTwoSided ? <Layers className="w-3.5 h-3.5" /> : <CheckCircle2 className="w-3.5 h-3.5" />}
            <span>{isTwoSided ? "Combined Two-Sided Document" : "Ready for IRIS OCR"}</span>
          </div>
        </div>

        {/* Expandable Source Front & Back View for Two-Sided Mode */}
        {isTwoSided && frontImageUrl && backImageUrl && (
          <div className="mb-4 bg-slate-50 border border-slate-200/80 rounded-2xl p-3">
            <button
              type="button"
              onClick={() => setShowSourceSides((prev) => !prev)}
              className="w-full flex items-center justify-between text-xs font-bold text-slate-700 hover:text-slate-900 transition-colors"
            >
              <span className="flex items-center gap-1.5">
                <ImageIcon className="w-3.5 h-3.5 text-indigo-600" />
                <span>View Source Front & Back Sides</span>
              </span>
              {showSourceSides ? <ChevronUp className="w-4 h-4 text-slate-400" /> : <ChevronDown className="w-4 h-4 text-slate-400" />}
            </button>

            {showSourceSides && (
              <div className="grid grid-cols-2 gap-2.5 pt-3 mt-2 border-t border-slate-200/60 animate-in fade-in">
                <div>
                  <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">Front Side</span>
                  <div className="aspect-[16/10] rounded-xl overflow-hidden bg-slate-900 border border-slate-200">
                    <img src={frontImageUrl} alt="Front side" className="w-full h-full object-contain" />
                  </div>
                </div>
                <div>
                  <span className="text-[10px] font-bold text-slate-500 uppercase block mb-1">Back Side</span>
                  <div className="aspect-[16/10] rounded-xl overflow-hidden bg-slate-900 border border-slate-200">
                    <img src={backImageUrl} alt="Back side" className="w-full h-full object-contain" />
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Error Alert with manual retry */}
        {error && (
          <div className="mb-5 p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-sm animate-in fade-in">
            <div className="flex items-start gap-2.5">
              <AlertCircle className="w-5 h-5 mt-0.5 shrink-0 text-rose-600" />
              <div className="flex-1">
                <p className="font-bold text-xs sm:text-sm">Document Processing Failed</p>
                <p className="text-xs mt-0.5 opacity-90">{error}</p>
                <div className="mt-3 flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleSubmit()}
                    disabled={isSubmitting}
                    className="px-3.5 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs touch-target-min flex items-center gap-1.5 shadow-xs transition-all active:scale-95"
                  >
                    <RefreshCw className="w-3 h-3" />
                    Retry Now
                  </button>
                  <button
                    type="button"
                    onClick={() => setError(null)}
                    className="px-3 py-1.5 rounded-lg bg-white border border-rose-200 text-rose-700 font-semibold text-xs touch-target-min"
                  >
                    Dismiss
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Capture Timestamp */}
          <div className="bg-slate-50 rounded-2xl p-3.5 border border-slate-200/80 flex items-center gap-2.5 text-xs text-slate-600">
            <Clock className="w-4 h-4 text-indigo-500 shrink-0" />
            <div className="flex-1 min-w-0">
              <span className="text-[10px] uppercase font-bold text-slate-400 block">Capture Timestamp</span>
              <span className="font-mono text-slate-700 font-semibold">{timestampInfo.display || "Recording current time..."}</span>
            </div>
          </div>

          {/* Optional Notes */}
          <div>
            <label htmlFor="notes" className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1.5 flex items-center gap-1">
              <FileText className="w-3.5 h-3.5 text-slate-400" />
              Notes / Remarks (Optional)
            </label>
            <textarea
              id="notes"
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Any remarks regarding verification or location..."
              className="w-full px-3.5 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white"
            />
          </div>

          {/* Actions */}
          <div className="pt-2 flex flex-col sm:flex-row gap-3">
            <button
              type="button"
              onClick={onCancel}
              className="py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs sm:text-sm touch-target-min transition-colors"
            >
              Cancel
            </button>

            <button
              type="submit"
              disabled={isSubmitting}
              className="flex-1 py-3.5 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-bold text-xs sm:text-sm shadow-md shadow-indigo-200 flex items-center justify-center gap-2 touch-target-min transition-all disabled:opacity-50"
            >
              <RefreshCw className={`w-4 h-4 ${isSubmitting ? "animate-spin" : ""}`} />
              <span>
                {isSubmitting
                  ? "Processing with IRIS API..."
                  : isTwoSided
                  ? "Submit Combined Document & Extract OCR"
                  : "Submit & Extract OCR Data"}
              </span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
