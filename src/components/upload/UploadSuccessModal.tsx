"use client";

import React from "react";
import { UploadRecord } from "@/types";
import { CheckCircle2, ArrowRight, Camera, Copy, Check, WifiOff, HardDriveDownload } from "lucide-react";

interface UploadSuccessModalProps {
  record: UploadRecord;
  isOfflineSaved?: boolean;
  onGoToRecords: () => void;
  onUploadAnother: () => void;
}

export const UploadSuccessModal: React.FC<UploadSuccessModalProps> = ({
  record,
  isOfflineSaved = false,
  onGoToRecords,
  onUploadAnother,
}) => {
  const [copied, setCopied] = React.useState(false);

  const handleCopyId = () => {
    if (typeof navigator !== "undefined") {
      navigator.clipboard.writeText(record.id);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-sm w-full p-6 text-center shadow-2xl border border-slate-100 animate-in zoom-in-95 duration-200">
        {/* Check / Offline Icon */}
        <div
          className={`w-16 h-16 rounded-full mx-auto flex items-center justify-center mb-4 shadow-sm ${
            isOfflineSaved
              ? "bg-amber-100 text-amber-600"
              : "bg-emerald-100 text-emerald-600"
          }`}
        >
          {isOfflineSaved ? (
            <HardDriveDownload className="w-8 h-8" />
          ) : (
            <CheckCircle2 className="w-9 h-9" />
          )}
        </div>

        <h3 className="text-xl font-bold text-slate-900">
          {isOfflineSaved ? "Saved on Device" : "Upload Successful!"}
        </h3>

        <div className="text-xs text-slate-600 mt-1 mb-5 leading-relaxed">
          {isOfflineSaved ? (
            <div className="p-2.5 rounded-xl bg-amber-50 border border-amber-200/80 text-amber-900 text-left flex items-start gap-2">
              <WifiOff className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold">Image saved locally.</p>
                <p className="text-[11px] opacity-90 mt-0.5">
                  It will be uploaded automatically when the internet connection is restored.
                </p>
              </div>
            </div>
          ) : (
            <p>Your image and verification data have been securely uploaded to the cloud.</p>
          )}
        </div>

        {/* Mini Preview Box */}
        <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-3 mb-6 text-left flex items-center gap-3">
          <img
            src={record.imageUrl}
            alt="Uploaded preview"
            className="w-14 h-14 rounded-xl object-cover border border-slate-200 shrink-0"
          />
          <div className="flex-1 min-w-0">
            <div className="flex items-center justify-between">
              <span className="font-mono text-xs font-bold text-indigo-600">{record.id}</span>
              <button
                type="button"
                onClick={handleCopyId}
                className="text-slate-400 hover:text-slate-600 p-1"
                title="Copy Record ID"
                aria-label="Copy Record ID"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
              </button>
            </div>
            <p className="text-xs font-semibold text-slate-800 truncate">{record.uploadedBy}</p>
            <div className="flex items-center gap-1.5 mt-0.5">
              <span
                className={`text-[9px] font-bold px-1.5 py-0.2 rounded-full ${
                  isOfflineSaved
                    ? "bg-amber-100 text-amber-800"
                    : "bg-emerald-100 text-emerald-800"
                }`}
              >
                {isOfflineSaved ? "Pending Upload" : "Uploaded"}
              </span>
              <span className="text-[10px] text-slate-400 font-mono truncate">{record.uploadedAt}</span>
            </div>
          </div>
        </div>

        {/* Primary Action Buttons */}
        <div className="space-y-2.5">
          <button
            type="button"
            onClick={onGoToRecords}
            className="w-full py-3.5 px-4 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-indigo-200 flex items-center justify-center gap-2 touch-target-min transition-all"
          >
            <span>View in Records Dashboard</span>
            <ArrowRight className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={onUploadAnother}
            className="w-full py-3 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs rounded-xl flex items-center justify-center gap-2 touch-target-min transition-all"
          >
            <Camera className="w-4 h-4 text-slate-500" />
            <span>Capture Another Photo</span>
          </button>
        </div>
      </div>
    </div>
  );
};
