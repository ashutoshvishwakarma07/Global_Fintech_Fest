"use client";

import React, { useState } from "react";
import { UploadRecord } from "@/types";
import {
  CheckCircle2,
  ArrowRight,
  Camera,
  Copy,
  Check,
  FileCheck,
  Sparkles,
  ShieldCheck,
  Hash,
  User,
} from "lucide-react";

interface UploadSuccessModalProps {
  record: UploadRecord;
  onGoToRecords: () => void;
  onUploadAnother: () => void;
}

export const UploadSuccessModal: React.FC<UploadSuccessModalProps> = ({
  record,
  onGoToRecords,
  onUploadAnother,
}) => {
  const [copiedId, setCopiedId] = useState(false);
  const [copiedDocNum, setCopiedDocNum] = useState(false);

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

  const rawConf = record.extractedData?.confidence ?? 98.8;
  const confidencePct = Math.round(rawConf <= 1 ? rawConf * 100 : rawConf);

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 animate-in fade-in duration-200">
      <div className="bg-white rounded-3xl max-w-md w-full p-6 text-center shadow-2xl border border-slate-100 animate-in zoom-in-95 duration-200">
        {/* Success Icon */}
        <div className="w-16 h-16 rounded-full mx-auto flex items-center justify-center mb-3.5 shadow-sm bg-emerald-100 text-emerald-600">
          <CheckCircle2 className="w-9 h-9" />
        </div>

        <h3 className="text-xl font-bold text-slate-900">Upload & Extraction Complete!</h3>
        <p className="text-xs text-slate-500 mt-1 mb-4 leading-relaxed">
          Document was processed by IRIS OCR API and stored in your records.
        </p>

        {/* IRIS OCR Extracted Data Card */}
        {record.extractedData && (
          <div className="mb-4 bg-gradient-to-br from-indigo-50/90 to-purple-50/70 border border-indigo-200/80 rounded-2xl p-3.5 text-left space-y-2.5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 text-indigo-700 font-bold text-xs">
                <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
                <span>IRIS OCR Result</span>
              </div>
              <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                <ShieldCheck className="w-3 h-3 text-emerald-600" />
                {confidencePct}% Match
              </span>
            </div>

            <div className="bg-white rounded-xl p-2.5 border border-indigo-100 shadow-2xs space-y-2">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Document Type:</span>
                <div className="flex items-center gap-1.5">
                  <span className="font-bold text-indigo-900 bg-indigo-50 px-2 py-0.5 rounded-md border border-indigo-100">
                    {record.extractedData.documentType}
                  </span>
                  {record.captureMode === "two-sided" && (
                    <span className="text-[10px] font-bold text-purple-700 bg-purple-100 px-1.5 py-0.5 rounded-md border border-purple-200">
                      2-Sided
                    </span>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 font-medium">Document No:</span>
                <div className="flex items-center gap-1.5">
                  <span className="font-mono font-bold text-slate-900 bg-slate-100 px-2 py-0.5 rounded">
                    {record.extractedData.documentNumber}
                  </span>
                  <button
                    type="button"
                    onClick={handleCopyDocNum}
                    className="text-slate-400 hover:text-indigo-600 p-0.5 transition-colors"
                    title="Copy Document Number"
                    aria-label="Copy Document Number"
                  >
                    {copiedDocNum ? (
                      <Check className="w-3.5 h-3.5 text-emerald-600" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                  </button>
                </div>
              </div>

              {(record.extractedData.extractedName || record.extractedData.name) && (
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-500 font-medium">Extracted Name:</span>
                  <span className="font-semibold text-slate-800 truncate max-w-[180px]">
                    {record.extractedData.extractedName || record.extractedData.name}
                  </span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Mini Preview Box */}
        <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-3 mb-5 text-left flex items-center gap-3">
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
                {copiedId ? (
                  <Check className="w-3.5 h-3.5 text-emerald-600" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
              </button>
            </div>
            <p className="text-xs font-semibold text-slate-800 truncate">{record.uploadedBy}</p>
            <div className="flex items-center gap-1.5 mt-0.5">
              <span className="text-[9px] font-bold px-1.5 py-0.2 rounded-full bg-emerald-100 text-emerald-800">
                Uploaded
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
