"use client";

import React, { useState } from "react";
import { UploadRecord } from "@/types";
import {
  X,
  Copy,
  Check,
  Download,
  Clock,
  User,
  Mail,
  Phone,
  Briefcase,
  CheckCircle2,
  FileText,
  Shield,
  ArrowLeft,
  Sparkles,
  ShieldCheck,
  Calendar,
  Hash,
} from "lucide-react";

interface RecordDetailModalProps {
  record: UploadRecord | null;
  onClose: () => void;
}

export const RecordDetailModal: React.FC<RecordDetailModalProps> = ({ record, onClose }) => {
  const [copiedId, setCopiedId] = useState(false);
  const [copiedDocNum, setCopiedDocNum] = useState(false);
  const [activeImageView, setActiveImageView] = useState<"combined" | "front" | "back">("combined");

  React.useEffect(() => {
    setActiveImageView("combined");
  }, [record?.id]);

  if (!record) return null;

  const handleCopyId = () => {
    if (typeof navigator !== "undefined") {
      navigator.clipboard.writeText(record.id);
      setCopiedId(true);
      setTimeout(() => setCopiedId(false), 2000);
    }
  };

  const handleCopyDocNum = () => {
    if (typeof navigator !== "undefined" && record.extractedData?.documentNumber) {
      navigator.clipboard.writeText(record.extractedData.documentNumber);
      setCopiedDocNum(true);
      setTimeout(() => setCopiedDocNum(false), 2000);
    }
  };

  const currentImageToDisplay =
    record.captureMode === "two-sided"
      ? activeImageView === "front"
        ? record.frontImageUrl || record.imageUrl
        : activeImageView === "back"
        ? record.backImageUrl || record.imageUrl
        : record.imageUrl
      : record.imageUrl;

  const handleDownload = () => {
    const link = document.createElement("a");
    link.href = currentImageToDisplay;
    link.download = `${record.id}-${activeImageView}.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const statusStyles: Record<string, string> = {
    Uploaded: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Verified: "bg-emerald-50 text-emerald-800 border-emerald-300",
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4 animate-in fade-in duration-200">
      <div
        className="bg-white w-full sm:max-w-xl max-h-[92vh] sm:max-h-[85vh] rounded-t-3xl sm:rounded-3xl shadow-2xl flex flex-col overflow-hidden animate-in slide-in-from-bottom-6 sm:zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Top Header */}
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-100 shrink-0">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onClose}
              className="sm:hidden p-1.5 -ml-1.5 text-slate-500 hover:text-slate-800 touch-target-min"
              aria-label="Back"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono font-bold text-sm text-indigo-600">
                  {record.id}
                </span>
                <button
                  type="button"
                  onClick={handleCopyId}
                  className="text-slate-400 hover:text-slate-700 p-0.5"
                  title="Copy ID"
                  aria-label="Copy Record ID"
                >
                  {copiedId ? (
                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                </button>
                {record.captureMode === "two-sided" && (
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-purple-100 text-purple-800 border border-purple-200">
                    Two-Sided
                  </span>
                )}
              </div>
              <span className="text-[11px] text-slate-400">Record & Extraction Details</span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <span
              className={`text-[11px] font-bold px-2.5 py-0.5 rounded-full border ${
                statusStyles[record.status] || statusStyles.Uploaded
              }`}
            >
              {record.status}
            </span>

            <button
              type="button"
              onClick={onClose}
              className="w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 text-slate-500 flex items-center justify-center transition-colors touch-target-min"
              aria-label="Close modal"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Scrollable Body */}
        <div className="overflow-y-auto p-5 space-y-4 flex-1">
          {/* Two-Sided View Switcher Tabs */}
          {record.captureMode === "two-sided" && (
            <div className="flex items-center bg-slate-100 p-1 rounded-xl border border-slate-200/70 text-xs">
              <button
                type="button"
                onClick={() => setActiveImageView("combined")}
                className={`flex-1 py-1.5 px-2 rounded-lg font-semibold transition-all ${
                  activeImageView === "combined"
                    ? "bg-white text-indigo-700 shadow-xs"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                Combined Collage
              </button>
              <button
                type="button"
                onClick={() => setActiveImageView("front")}
                className={`flex-1 py-1.5 px-2 rounded-lg font-semibold transition-all ${
                  activeImageView === "front"
                    ? "bg-white text-indigo-700 shadow-xs"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                Front Side
              </button>
              <button
                type="button"
                onClick={() => setActiveImageView("back")}
                className={`flex-1 py-1.5 px-2 rounded-lg font-semibold transition-all ${
                  activeImageView === "back"
                    ? "bg-white text-indigo-700 shadow-xs"
                    : "text-slate-600 hover:text-slate-900"
                }`}
              >
                Back Side
              </button>
            </div>
          )}

          {/* Large Image Preview */}
          <div className="relative rounded-2xl overflow-hidden bg-slate-900 aspect-[4/3] w-full shadow-inner border border-slate-200">
            <img
              src={currentImageToDisplay}
              alt={record.id}
              className="w-full h-full object-contain"
            />
            <button
              type="button"
              onClick={handleDownload}
              className="absolute bottom-3 right-3 px-3 py-1.5 rounded-xl bg-black/60 hover:bg-black/80 backdrop-blur-md text-white text-xs font-semibold flex items-center gap-1.5 active:scale-95 transition-all shadow-md touch-target-min border border-white/20"
              title="Download image"
            >
              <Download className="w-3.5 h-3.5" />
              Download {activeImageView === "combined" ? "Collage" : activeImageView === "front" ? "Front" : "Back"}
            </button>
          </div>

          {/* IRIS OCR Extraction Section */}
          {record.extractedData ? (
            <div className="bg-gradient-to-br from-indigo-50/90 to-purple-50/70 rounded-2xl p-4 border border-indigo-200/80 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-indigo-900 font-bold text-xs">
                  <Sparkles className="w-4 h-4 text-indigo-600" />
                  <span>IRIS API Extraction Results</span>
                </div>
                <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                  <ShieldCheck className="w-3 h-3 text-emerald-600" />
                  {Math.round(
                    record.extractedData.confidence <= 1
                      ? record.extractedData.confidence * 100
                      : record.extractedData.confidence
                  )}% Confidence
                </span>
              </div>

              <div className="grid grid-cols-2 gap-2.5 pt-1">
                <div className="bg-white p-2.5 rounded-xl border border-indigo-100 shadow-2xs">
                  <span className="text-[10px] text-slate-400 uppercase font-semibold block">Document Type</span>
                  <span className="text-xs font-bold text-slate-800">{record.extractedData.documentType}</span>
                </div>

                <div className="bg-white p-2.5 rounded-xl border border-indigo-100 shadow-2xs">
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] text-slate-400 uppercase font-semibold block">Document No</span>
                    <button
                      type="button"
                      onClick={handleCopyDocNum}
                      className="text-slate-400 hover:text-indigo-600 p-0.5"
                      title="Copy Document Number"
                      aria-label="Copy Document Number"
                    >
                      {copiedDocNum ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                    </button>
                  </div>
                  <span className="font-mono text-xs font-bold text-indigo-700 truncate block">
                    {record.extractedData.documentNumber}
                  </span>
                </div>

                {(record.extractedData.extractedName || record.extractedData.name) && (
                  <div className="bg-white p-2.5 rounded-xl border border-indigo-100 shadow-2xs col-span-2">
                    <span className="text-[10px] text-slate-400 uppercase font-semibold block">Extracted Name</span>
                    <span className="text-xs font-semibold text-slate-800">
                      {record.extractedData.extractedName || record.extractedData.name}
                    </span>
                  </div>
                )}

                {record.extractedData.issueDate && (
                  <div className="bg-white p-2.5 rounded-xl border border-indigo-100 shadow-2xs">
                    <span className="text-[10px] text-slate-400 uppercase font-semibold block">Issue / Validity</span>
                    <span className="text-xs font-medium text-slate-700 font-mono">{record.extractedData.issueDate}</span>
                  </div>
                )}

                {record.extractedData.rawText && (
                  <div className="bg-white p-2.5 rounded-xl border border-indigo-100 shadow-2xs col-span-2">
                    <span className="text-[10px] text-slate-400 uppercase font-semibold block mb-1">OCR Raw Data</span>
                    <div className="font-mono text-[10px] text-slate-600 bg-slate-50 p-2 rounded-lg border border-slate-200/60 leading-relaxed max-h-24 overflow-y-auto">
                      {record.extractedData.rawText}
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200 text-center text-xs text-slate-500">
              No IRIS OCR data attached to this record.
            </div>
          )}

          {/* User & Uploader Details */}
          <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200/70 space-y-3 text-xs">
            <h4 className="font-bold text-slate-900 text-xs mb-1">Uploader & Contact Information</h4>

            {/* User */}
            <div className="flex items-center justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500 flex items-center gap-1.5">
                <User className="w-3.5 h-3.5 text-slate-400" />
                Uploaded By
              </span>
              <span className="font-bold text-slate-800">{record.uploadedBy}</span>
            </div>

            {/* Role */}
            <div className="flex items-center justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500 flex items-center gap-1.5">
                <Briefcase className="w-3.5 h-3.5 text-slate-400" />
                Role
              </span>
              <span className="font-semibold text-slate-700 bg-white px-2 py-0.5 rounded border border-slate-200">
                {record.role}
              </span>
            </div>

            {/* Email */}
            <div className="flex items-center justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500 flex items-center gap-1.5">
                <Mail className="w-3.5 h-3.5 text-slate-400" />
                Email
              </span>
              <span className="font-mono text-slate-700">{record.email}</span>
            </div>

            {/* Mobile */}
            <div className="flex items-center justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500 flex items-center gap-1.5">
                <Phone className="w-3.5 h-3.5 text-slate-400" />
                Mobile
              </span>
              <span className="font-mono font-semibold text-slate-800">{record.mobile}</span>
            </div>

            {/* Timestamp */}
            <div className="flex items-center justify-between pb-2 border-b border-slate-200/60">
              <span className="text-slate-500 flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-slate-400" />
                Timestamp
              </span>
              <span className="font-mono text-slate-700">{record.uploadedAt}</span>
            </div>

            {/* Notes if any */}
            {record.notes && (
              <div className="pt-1">
                <span className="text-slate-500 flex items-center gap-1.5 mb-1">
                  <FileText className="w-3.5 h-3.5 text-slate-400" />
                  Notes / Description
                </span>
                <p className="text-slate-700 bg-white p-2.5 rounded-xl border border-slate-200 leading-relaxed text-[11px]">
                  {record.notes}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Modal Footer */}
        <div className="p-4 bg-slate-50 border-t border-slate-100 flex items-center justify-end gap-2 shrink-0">
          <button
            type="button"
            onClick={onClose}
            className="w-full sm:w-auto px-5 py-2.5 rounded-xl bg-slate-200 hover:bg-slate-300 text-slate-800 text-xs font-semibold touch-target-min transition-colors"
          >
            Close / Back
          </button>
        </div>
      </div>
    </div>
  );
};
