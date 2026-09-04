"use client";

import React from "react";
import { UploadRecord } from "@/types";
import {
  Clock,
  Phone,
  Mail,
  ChevronRight,
  Shield,
  RefreshCw,
  HardDrive,
  AlertTriangle,
  CheckCircle2,
} from "lucide-react";

interface RecordCardProps {
  record: UploadRecord;
  onSelect: (record: UploadRecord) => void;
  onSyncSingle?: (id: string) => void;
  isSyncing?: boolean;
}

export const RecordCard: React.FC<RecordCardProps> = ({
  record,
  onSelect,
  onSyncSingle,
  isSyncing = false,
}) => {
  const getStatusBadge = () => {
    switch (record.status) {
      case "Pending Upload":
        return {
          pillClass: "bg-amber-50 text-amber-800 border-amber-300",
          icon: <HardDrive className="w-3 h-3 text-amber-600" />,
          label: "Pending Upload",
          subtext: "Saved on device",
        };
      case "Uploading":
        return {
          pillClass: "bg-blue-50 text-blue-700 border-blue-200 animate-pulse",
          icon: <RefreshCw className="w-3 h-3 text-blue-600 animate-spin" />,
          label: "Uploading...",
          subtext: "Transmitting data",
        };
      case "Failed":
        return {
          pillClass: "bg-rose-50 text-rose-700 border-rose-200",
          icon: <AlertTriangle className="w-3 h-3 text-rose-600" />,
          label: "Failed",
          subtext: "Retry scheduled",
        };
      case "Verified":
        return {
          pillClass: "bg-emerald-50 text-emerald-800 border-emerald-300",
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          label: "Verified",
          subtext: null,
        };
      case "Uploaded":
      default:
        return {
          pillClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          label: "Uploaded",
          subtext: null,
        };
    }
  };

  const badge = getStatusBadge();
  const canManualSync =
    (record.status === "Pending Upload" || record.status === "Failed") && onSyncSingle;

  return (
    <div
      onClick={() => onSelect(record)}
      className={`group bg-white rounded-2xl border p-3.5 shadow-card hover:shadow-card-hover active:scale-[0.99] transition-all cursor-pointer flex flex-col gap-3 ${
        record.status === "Pending Upload"
          ? "border-amber-200/90 bg-amber-50/20"
          : record.status === "Failed"
          ? "border-rose-200 bg-rose-50/20"
          : "border-slate-200/80"
      }`}
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

      {/* Subtext / Manual Sync Row for Offline Records */}
      {badge.subtext && (
        <div className="px-3 py-1.5 rounded-xl bg-amber-50/80 border border-amber-200/60 flex items-center justify-between text-xs">
          <span className="text-[11px] text-amber-900 font-medium">
            {badge.subtext}
          </span>
          {canManualSync && (
            <button
              type="button"
              disabled={isSyncing}
              onClick={(e) => {
                e.stopPropagation();
                onSyncSingle(record.id);
              }}
              className="px-2.5 py-1 rounded-lg bg-amber-600 hover:bg-amber-700 active:scale-95 text-white font-bold text-[10px] flex items-center gap-1 touch-target-min"
            >
              <RefreshCw className={`w-2.5 h-2.5 ${isSyncing ? "animate-spin" : ""}`} />
              Sync Now
            </button>
          )}
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
