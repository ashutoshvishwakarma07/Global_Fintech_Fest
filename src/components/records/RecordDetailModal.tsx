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
} from "lucide-react";

interface RecordDetailModalProps {
  record: UploadRecord | null;
  onClose: () => void;
}

export const RecordDetailModal: React.FC<RecordDetailModalProps> = ({ record, onClose }) => {
  const [copied, setCopied] = useState(false);

  if (!record) return null;

  const handleCopyId = () => {
    if (typeof navigator !== "undefined") {
      navigator.clipboard.writeText(record.id);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleDownload = () => {
    const link = document.createElement("a");
    link.href = record.imageUrl;
    link.download = `${record.id}-verification.jpg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const statusStyles: Record<string, string> = {
    Uploaded: "bg-emerald-50 text-emerald-700 border-emerald-200",
    Verified: "bg-emerald-50 text-emerald-700 border-emerald-200",
    "Pending Upload": "bg-amber-50 text-amber-800 border-amber-300",
    Uploading: "bg-blue-50 text-blue-700 border-blue-200",
    Failed: "bg-rose-50 text-rose-700 border-rose-200",
    Pending: "bg-amber-50 text-amber-700 border-amber-200",
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
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                </button>
              </div>
              <span className="text-[11px] text-slate-400">Record Details</span>
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
        <div className="overflow-y-auto p-5 space-y-5 flex-1">
          {/* Large Image Preview */}
          <div className="relative rounded-2xl overflow-hidden bg-slate-900 aspect-[4/3] w-full shadow-inner border border-slate-200">
            <img
              src={record.imageUrl}
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
              Download
            </button>
          </div>

          {/* Details List */}
          <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200/70 space-y-3 text-xs">
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
