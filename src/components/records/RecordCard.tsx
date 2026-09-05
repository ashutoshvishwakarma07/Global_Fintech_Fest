"use client";

import React from "react";
import { UploadRecord } from "@/types";
import {
  Clock,
  Phone,
  Mail,
  ChevronRight,
  Shield,
  CheckCircle2,
  FileText,
  Sparkles,
} from "lucide-react";

interface RecordCardProps {
  record: UploadRecord;
  onSelect: (record: UploadRecord) => void;
}

export const RecordCard: React.FC<RecordCardProps> = ({ record, onSelect }) => {
  const getStatusBadge = () => {
    switch (record.status) {
      case "Verified":
        return {
          pillClass: "bg-emerald-50 text-emerald-800 border-emerald-300",
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          label: "Verified",
        };
      case "Uploaded":
      default:
        return {
          pillClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          label: "Uploaded",
        };
    }
  };

  const badge = getStatusBadge();

  return (
    <div
      onClick={() => onSelect(record)}
      className="group bg-white rounded-2xl border border-slate-200/80 p-3.5 shadow-card hover:shadow-card-hover active:scale-[0.99] transition-all cursor-pointer flex flex-col gap-3"
    >
      {/* Top Row: Thumbnail + Key Meta */}
      <div className="flex items-start gap-3.5">
        <div className="relative w-20 h-20 sm:w-24 sm:h-24 rounded-xl overflow-hidden bg-slate-100 shrink-0 border border-slate-200">
          <img
            src={record.imageUrl}
            alt={record.id}
            className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            loading="lazy"
          />
          <div className="absolute top-1 left-1">
            <span className="bg-slate-900/80 backdrop-blur-sm text-white font-mono text-[9px] font-bold px-1.5 py-0.5 rounded">
              {record.id}
            </span>
          </div>
        </div>

        {/* Right Info */}
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between gap-1 mb-1">
            <h4 className="text-sm font-bold text-slate-900 truncate">
              {record.uploadedBy}
            </h4>
            <span
              className={`inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full border shrink-0 ${badge.pillClass}`}
            >
              {badge.icon}
              {badge.label}
            </span>
          </div>

          {/* Role badge */}
          <div className="inline-flex items-center gap-1 text-[10px] font-medium text-slate-500 bg-slate-100 px-2 py-0.5 rounded-md mb-1.5">
            <Shield className="w-3 h-3 text-slate-400" />
            <span>{record.role}</span>
          </div>

          {/* Contact Details */}
          <div className="space-y-0.5 text-[11px] text-slate-500">
            <div className="flex items-center gap-1.5 truncate">
              <Mail className="w-3 h-3 text-slate-400 shrink-0" />
              <span className="truncate">{record.email}</span>
            </div>
            <div className="flex items-center gap-1.5 truncate">
              <Phone className="w-3 h-3 text-slate-400 shrink-0" />
              <span className="font-mono">{record.mobile}</span>
            </div>
          </div>
        </div>
      </div>

      {/* IRIS OCR Extracted Document Snippet */}
      {record.extractedData && (
        <div className="bg-indigo-50/70 rounded-xl px-3 py-2 border border-indigo-100 flex items-center justify-between gap-2 text-xs">
          <div className="flex items-center gap-1.5 min-w-0">
            <FileText className="w-3.5 h-3.5 text-indigo-600 shrink-0" />
            <span className="font-semibold text-indigo-950 truncate">
              {record.extractedData.documentType}
            </span>
            {record.captureMode === "two-sided" && (
              <span className="text-[9px] font-bold text-purple-700 bg-purple-100 px-1 py-0.2 rounded shrink-0">
                2-Sided
              </span>
            )}
            <span className="font-mono text-[11px] text-indigo-700 bg-white/80 px-1.5 py-0.5 rounded border border-indigo-200/60 truncate">
              {record.extractedData.documentNumber}
            </span>
          </div>
          <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded-md shrink-0 border border-emerald-200">
            {Math.round(
              record.extractedData.confidence <= 1
                ? record.extractedData.confidence * 100
                : record.extractedData.confidence
            )}%
          </span>
        </div>
      )}

      {/* Card Footer: Timestamp & Tap Indicator */}
      <div className="pt-2 border-t border-slate-100 flex items-center justify-between text-slate-400 text-[11px]">
        <div className="flex items-center gap-1.5">
          <Clock className="w-3 h-3 text-slate-400" />
          <span className="font-mono text-slate-500">{record.uploadedAt}</span>
        </div>
        <div className="flex items-center gap-0.5 text-indigo-600 font-semibold text-xs group-hover:translate-x-0.5 transition-transform">
          <span>Details</span>
          <ChevronRight className="w-3.5 h-3.5" />
        </div>
      </div>
    </div>
  );
};
