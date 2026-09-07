"use client";

import React, { useState, useMemo, useRef, useEffect } from "react";
import { User, UploadRecord, UserRole, RecordStatus, DocumentType } from "@/types";
import { RecordCard } from "./RecordCard";
import { RecordTable } from "./RecordTable";
import { RecordDetailModal } from "./RecordDetailModal";
import { EmptyState } from "../common/EmptyState";
import {
  Search,
  SlidersHorizontal,
  FolderOpen,
  Clock,
  UserCheck,
  X,
  Plus,
  Shield,
  FileCheck,
  Filter,
  Users,
  Calendar,
  ChevronDown,
  Check,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";

interface UploaderDropdownProps {
  uploaders: string[];
  selected: string;
  onSelect: (uploader: string) => void;
  className?: string;
  isMobile?: boolean;
}

const UploaderDropdown: React.FC<UploaderDropdownProps> = ({
  uploaders,
  selected,
  onSelect,
  className = "",
  isMobile = false,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent | TouchEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("touchstart", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("touchstart", handleClickOutside);
    };
  }, [isOpen]);

  const allOptions = ["All", ...uploaders];

  const getLabel = (opt: string) => {
    if (opt === "All") return `All Users (${uploaders.length})`;
    return opt;
  };

  const getInitials = (name: string) => {
    if (name === "All") return null;
    return name
      .split(" ")
      .map((n) => n[0])
      .join("")
      .substring(0, 2)
      .toUpperCase();
  };

  return (
    <div ref={dropdownRef} className={`relative ${className}`}>
      <button
        type="button"
        onClick={() => setIsOpen((prev) => !prev)}
        className={`w-full flex items-center justify-between gap-2 px-3 py-2 rounded-xl border text-xs font-semibold transition-all ${
          isOpen
            ? "border-indigo-500 ring-2 ring-indigo-500/20 bg-white text-indigo-950 shadow-sm"
            : "border-slate-200/90 bg-slate-50 hover:bg-slate-100 text-slate-800"
        }`}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
      >
        <div className="flex items-center gap-2 truncate min-w-0">
          <div className="w-5 h-5 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-[10px] font-bold shrink-0">
            {selected === "All" ? (
              <Users className="w-3 h-3" />
            ) : (
              getInitials(selected) || <UserCheck className="w-3 h-3" />
            )}
          </div>
          <span className="truncate">{getLabel(selected)}</span>
        </div>
        <ChevronDown
          className={`w-3.5 h-3.5 text-slate-400 transition-transform duration-200 shrink-0 ${
            isOpen ? "rotate-180 text-indigo-600" : ""
          }`}
        />
      </button>

      {isOpen && (
        <div
          className={`absolute left-0 right-0 top-full mt-1.5 z-40 bg-white border border-slate-200/90 rounded-2xl shadow-xl overflow-hidden py-1 animate-in fade-in zoom-in-95 duration-150 max-h-56 overflow-y-auto no-scrollbar ${
            isMobile ? "w-full" : "min-w-[190px] w-auto max-w-[260px]"
          }`}
          role="listbox"
        >
          {allOptions.map((opt) => {
            const isSelected = selected === opt;
            const initials = getInitials(opt);
            return (
              <button
                key={opt}
                type="button"
                role="option"
                aria-selected={isSelected}
                onClick={() => {
                  onSelect(opt);
                  setIsOpen(false);
                }}
                className={`w-full flex items-center justify-between px-3 py-2.5 text-xs text-left transition-colors ${
                  isSelected
                    ? "bg-indigo-50 text-indigo-800 font-bold"
                    : "text-slate-700 hover:bg-slate-50 font-medium"
                }`}
              >
                <div className="flex items-center gap-2.5 truncate min-w-0">
                  <div
                    className={`w-6 h-6 rounded-full flex items-center justify-center text-[10px] font-bold shrink-0 ${
                      isSelected ? "bg-indigo-600 text-white" : "bg-slate-100 text-slate-600"
                    }`}
                  >
                    {opt === "All" ? (
                      <Users className="w-3 h-3" />
                    ) : (
                      initials || <UserCheck className="w-3 h-3" />
                    )}
                  </div>
                  <span className="truncate">{getLabel(opt)}</span>
                </div>
                {isSelected && <Check className="w-4 h-4 text-indigo-600 shrink-0 ml-2" />}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

interface RecordsDashboardProps {
  currentUser: User;
  records: UploadRecord[];
  onNavigateToUpload: () => void;
}

export const RecordsDashboard: React.FC<RecordsDashboardProps> = ({
  currentUser,
  records,
  onNavigateToUpload,
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [uploaderFilter, setUploaderFilter] = useState<string>("All");
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [datePreset, setDatePreset] = useState<"all" | "today" | "yesterday" | "week">("all");
  const [selectedRecord, setSelectedRecord] = useState<UploadRecord | null>(null);
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);

  const isAdmin = currentUser.role === "Admin";

  // List of unique uploaders (for Admin filter)
  const uniqueUploaders = useMemo(() => {
    const names = new Set<string>();
    records.forEach((r) => {
      if (r.uploadedBy) names.add(r.uploadedBy);
    });
    return Array.from(names);
  }, [records]);

  // Key metrics: OCR Completed, OCR Pending, Failed
  const totalCount = records.length;
  const ocrCompletedCount = useMemo(() => {
    return records.filter((r) => {
      if (r.status === "Failed") return false;
      if (r.status === "Processing") return false;
      return (
        r.status === "Verified" ||
        r.status === "Uploaded" ||
        r.ocrStatus?.toUpperCase() === "COMPLETED" ||
        r.ocrStatus?.toUpperCase() === "SUCCESS" ||
        Boolean(r.extractedData)
      );
    }).length;
  }, [records]);

  const ocrPendingCount = useMemo(() => {
    return records.filter((r) => {
      if (r.status === "Failed") return false;
      return (
        r.status === "Processing" ||
        r.ocrStatus?.toUpperCase() === "PENDING" ||
        r.ocrStatus?.toUpperCase() === "PROCESSING" ||
        (!r.extractedData && r.status !== "Verified" && r.status !== "Uploaded")
      );
    }).length;
  }, [records]);

  const ocrFailedCount = useMemo(() => {
    return records.filter((r) => {
      return (
        r.status === "Failed" ||
        r.ocrStatus?.toUpperCase() === "FAILED" ||
        Boolean(r.errorMessage)
      );
    }).length;
  }, [records]);

  // Helper to extract YYYY-MM-DD from record.uploadedAt
  const getRecordDateString = (uploadedAt: string): string => {
    if (!uploadedAt) return "";
    const match = uploadedAt.match(/^(\d{4}-\d{2}-\d{2})/);
    if (match) return match[1];
    const d = new Date(uploadedAt);
    if (!isNaN(d.getTime())) {
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      return `${year}-${month}-${day}`;
    }
    return "";
  };

  const handleSetDatePreset = (preset: "all" | "today" | "yesterday" | "week") => {
    setDatePreset(preset);
    const now = new Date();
    const formatDate = (d: Date) => {
      const year = d.getFullYear();
      const month = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      return `${year}-${month}-${day}`;
    };

    if (preset === "all") {
      setStartDate("");
      setEndDate("");
    } else if (preset === "today") {
      const todayStr = formatDate(now);
      setStartDate(todayStr);
      setEndDate(todayStr);
    } else if (preset === "yesterday") {
      const yesterday = new Date(now);
      yesterday.setDate(yesterday.getDate() - 1);
      const yestStr = formatDate(yesterday);
      setStartDate(yestStr);
      setEndDate(yestStr);
    } else if (preset === "week") {
      const weekAgo = new Date(now);
      weekAgo.setDate(weekAgo.getDate() - 7);
      setStartDate(formatDate(weekAgo));
      setEndDate(formatDate(now));
    }
  };

  // Filter & Sort Logic
  const filteredRecords = useMemo(() => {
    return records
      .filter((record) => {
        // Search filter
        if (searchQuery.trim()) {
          const q = searchQuery.toLowerCase();
          const matchesId = record.id.toLowerCase().includes(q);
          const matchesName = record.uploadedBy.toLowerCase().includes(q);
          const matchesEmail = record.email.toLowerCase().includes(q);
          const matchesMobile = record.mobile.includes(q);
          const matchesDocType =
            record.extractedData?.documentType?.toLowerCase().includes(q) || false;
          const matchesDocNum =
            record.extractedData?.documentNumber?.toLowerCase().includes(q) || false;
          const matchesNotes = record.notes?.toLowerCase().includes(q) || false;

          if (
            !matchesId &&
            !matchesName &&
            !matchesEmail &&
            !matchesMobile &&
            !matchesDocType &&
            !matchesDocNum &&
            !matchesNotes
          ) {
            return false;
          }
        }

        // Admin: Filter by Uploader
        if (isAdmin && uploaderFilter !== "All" && record.uploadedBy !== uploaderFilter) {
          return false;
        }

        // Date filter
        if (startDate || endDate) {
          const recDate = getRecordDateString(record.uploadedAt);
          if (recDate) {
            if (startDate && endDate) {
              if (recDate < startDate || recDate > endDate) return false;
            } else if (startDate && !endDate) {
              if (recDate !== startDate) return false;
            } else if (!startDate && endDate) {
              if (recDate > endDate) return false;
            }
          } else {
            return false;
          }
        }

        return true;
      })
      .sort((a, b) => {
        return b.uploadedAt.localeCompare(a.uploadedAt);
      });
  }, [records, searchQuery, uploaderFilter, startDate, endDate, isAdmin]);

  const hasActiveFilters =
    (isAdmin && uploaderFilter !== "All") ||
    startDate !== "" ||
    endDate !== "" ||
    searchQuery.trim() !== "";

  const handleResetFilters = () => {
    setSearchQuery("");
    setUploaderFilter("All");
    setStartDate("");
    setEndDate("");
    setDatePreset("all");
  };

  return (
    <div className="w-full max-w-6xl mx-auto pb-24 md:pb-12 animate-in fade-in duration-300">
      {/* Top Admin Notice if logged in as Admin */}
      {isAdmin && (
        <div className="mb-5 bg-gradient-to-r from-purple-700 via-indigo-700 to-indigo-800 text-white rounded-2xl p-4 sm:p-5 shadow-lg shadow-indigo-200/50 flex items-center justify-between gap-3 animate-in slide-in-from-top-2">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center shrink-0">
              <Shield className="w-5 h-5 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm sm:text-base">Admin Master Console</span>
                <span className="bg-purple-900/60 px-2 py-0.5 rounded text-[10px] font-semibold">
                  All Records Access
                </span>
              </div>
              <p className="text-xs text-purple-100 mt-0.5">
                Viewing all uploaded records and extracted IRIS OCR data across Rahul Sharma, Priya Verma, and Admin.
              </p>
            </div>
          </div>
          <span className="hidden sm:inline-flex items-center px-3 py-1 bg-white/10 rounded-full text-xs font-semibold text-purple-100">
            {records.length} Total Records
          </span>
        </div>
      )}

      {/* 3 OCR KPI Cards: Completed, Pending, Failed */}
      <div className="grid grid-cols-3 gap-2.5 sm:gap-4 mb-5">
        {/* KPI 1: OCR Completed */}
        <div className="bg-white rounded-2xl p-2.5 sm:p-4 border border-slate-200/80 shadow-card hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between mb-1.5 sm:mb-2">
            <div className="flex items-center gap-1.5 sm:gap-2">
              <div className="w-6 h-6 sm:w-8 sm:h-8 rounded-lg sm:rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                <CheckCircle2 className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              </div>
              <span className="text-[10px] sm:text-xs font-bold uppercase tracking-wider text-slate-500 truncate">
                OCR Completed
              </span>
            </div>
            <span className="hidden sm:inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700">
              Done
            </span>
          </div>
          <div className="flex items-baseline gap-1.5">
            <div className="text-lg sm:text-2xl md:text-3xl font-black text-slate-900">
              {ocrCompletedCount}
            </div>
            <span className="text-[10px] sm:text-xs font-bold text-emerald-600">
              {totalCount > 0 ? `${Math.round((ocrCompletedCount / totalCount) * 100)}%` : "0%"}
            </span>
          </div>
          <div className="text-[10px] sm:text-xs text-slate-400 mt-0.5 truncate">
            Extracted & verified
          </div>
        </div>

        {/* KPI 2: OCR Pending */}
        <div className="bg-white rounded-2xl p-2.5 sm:p-4 border border-slate-200/80 shadow-card hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between mb-1.5 sm:mb-2">
            <div className="flex items-center gap-1.5 sm:gap-2">
              <div className="w-6 h-6 sm:w-8 sm:h-8 rounded-lg sm:rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
                <Clock className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              </div>
              <span className="text-[10px] sm:text-xs font-bold uppercase tracking-wider text-slate-500 truncate">
                OCR Pending
              </span>
            </div>
            <span className="hidden sm:inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-50 text-amber-700">
              Queue
            </span>
          </div>
          <div className="flex items-baseline gap-1.5">
            <div className="text-lg sm:text-2xl md:text-3xl font-black text-slate-900">
              {ocrPendingCount}
            </div>
            <span className="text-[10px] sm:text-xs font-bold text-amber-600">
              {ocrPendingCount > 0 ? "In progress" : "0 queue"}
            </span>
          </div>
          <div className="text-[10px] sm:text-xs text-slate-400 mt-0.5 truncate">
            Awaiting processing
          </div>
        </div>

        {/* KPI 3: Failed */}
        <div className="bg-white rounded-2xl p-2.5 sm:p-4 border border-slate-200/80 shadow-card hover:shadow-md transition-shadow">
          <div className="flex items-center justify-between mb-1.5 sm:mb-2">
            <div className="flex items-center gap-1.5 sm:gap-2">
              <div className="w-6 h-6 sm:w-8 sm:h-8 rounded-lg sm:rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center shrink-0">
                <AlertCircle className="w-3.5 h-3.5 sm:w-4 sm:h-4" />
              </div>
              <span className="text-[10px] sm:text-xs font-bold uppercase tracking-wider text-slate-500 truncate">
                Failed
              </span>
            </div>
            <span
              className={`hidden sm:inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                ocrFailedCount > 0 ? "bg-rose-50 text-rose-700" : "bg-slate-100 text-slate-500"
              }`}
            >
              {ocrFailedCount > 0 ? "Alert" : "Clean"}
            </span>
          </div>
          <div className="flex items-baseline gap-1.5">
            <div
              className={`text-lg sm:text-2xl md:text-3xl font-black ${
                ocrFailedCount > 0 ? "text-rose-600" : "text-slate-900"
              }`}
            >
              {ocrFailedCount}
            </div>
            <span className="text-[10px] sm:text-xs font-bold text-slate-400">
              {ocrFailedCount > 0 ? "Action needed" : "0 errors"}
            </span>
          </div>
          <div className="text-[10px] sm:text-xs text-slate-400 mt-0.5 truncate">
            Requires attention
          </div>
        </div>
      </div>

      {/* Controls Bar: Search & Filters */}
      <div className="bg-white rounded-2xl p-3 sm:p-4 border border-slate-200/80 shadow-card mb-5 space-y-3">
        <div className="flex items-center gap-2">
          {/* Search Input */}
          <div className="relative flex-1">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              <Search className="w-4 h-4" />
            </div>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by ID, name, doc number, email..."
              className="w-full pl-9 pr-8 py-2.5 bg-slate-50 hover:bg-slate-100/60 focus:bg-white border border-slate-200 rounded-xl text-xs font-medium text-slate-900 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 touch-target-min transition-colors"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery("")}
                className="absolute inset-y-0 right-0 pr-2.5 flex items-center text-slate-400 hover:text-slate-600"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* Mobile Filter Sheet Button */}
          <button
            type="button"
            onClick={() => setIsMobileFilterOpen(true)}
            className={`md:hidden flex items-center gap-1.5 px-3 py-2.5 rounded-xl border text-xs font-semibold touch-target-min transition-all shrink-0 ${
              hasActiveFilters
                ? "bg-indigo-50 border-indigo-300 text-indigo-700"
                : "bg-slate-50 border-slate-200 text-slate-700"
            }`}
          >
            <SlidersHorizontal className="w-4 h-4" />
            <span>Filters</span>
            {hasActiveFilters && <span className="w-2 h-2 rounded-full bg-indigo-600 ml-0.5" />}
          </button>

          {/* New Capture Action Button on Desktop */}
          <button
            type="button"
            onClick={onNavigateToUpload}
            className="hidden sm:inline-flex items-center gap-1.5 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-sm active:scale-95 transition-all touch-target-min shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>New Upload</span>
          </button>
        </div>

        {/* Desktop Filter Row */}
        <div className="hidden md:flex items-center justify-between pt-2.5 border-t border-slate-100 text-xs gap-3 flex-wrap">
          <div className="flex items-center gap-3 flex-wrap">
            {/* Admin: Filter by Uploader */}
            {isAdmin && uniqueUploaders.length > 0 && (
              <div className="flex items-center gap-1.5">
                <Users className="w-3.5 h-3.5 text-indigo-600" />
                <span className="text-slate-500 font-bold text-[11px] uppercase tracking-wider">
                  Uploaded By:
                </span>
                <UploaderDropdown
                  uploaders={uniqueUploaders}
                  selected={uploaderFilter}
                  onSelect={setUploaderFilter}
                  className="min-w-[170px]"
                />
                <div className="h-4 w-[1px] bg-slate-200 mx-1" />
              </div>
            )}

            {/* Date Filter (Both for Admin and Regular Users) */}
            <div className="flex items-center gap-2 flex-wrap">
              <div className="flex items-center gap-1.5 text-slate-500">
                <Calendar className="w-3.5 h-3.5 text-indigo-600" />
                <span className="font-bold text-[11px] uppercase tracking-wider">Date:</span>
              </div>

              {/* Quick Presets */}
              <div className="inline-flex items-center bg-slate-100 p-0.5 rounded-lg">
                {(
                  [
                    { id: "all", label: "All" },
                    { id: "today", label: "Today" },
                    { id: "yesterday", label: "Yesterday" },
                    { id: "week", label: "Last 7D" },
                  ] as const
                ).map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => handleSetDatePreset(p.id)}
                    className={`px-2.5 py-1 rounded-md text-[11px] font-semibold transition-all ${
                      datePreset === p.id && !startDate && !endDate && p.id === "all"
                        ? "bg-white text-indigo-600 shadow-xs"
                        : datePreset === p.id && p.id !== "all"
                        ? "bg-white text-indigo-600 shadow-xs"
                        : "text-slate-600 hover:text-slate-900"
                    }`}
                  >
                    {p.label}
                  </button>
                ))}
              </div>

              {/* Date Inputs */}
              <div className="flex items-center gap-1">
                <input
                  type="date"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    setDatePreset("all");
                  }}
                  aria-label="Filter from date"
                  className="bg-slate-100 border border-slate-200 rounded-lg py-1 px-2 text-xs font-medium text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
                <span className="text-slate-400 text-[11px]">to</span>
                <input
                  type="date"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    setDatePreset("all");
                  }}
                  aria-label="Filter to date"
                  className="bg-slate-100 border border-slate-200 rounded-lg py-1 px-2 text-xs font-medium text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                />
                {(startDate || endDate) && (
                  <button
                    type="button"
                    onClick={() => handleSetDatePreset("all")}
                    className="p-1 rounded-md text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors"
                    title="Clear date filter"
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                )}
              </div>
            </div>
          </div>

          {hasActiveFilters && (
            <button
              type="button"
              onClick={handleResetFilters}
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition-colors"
            >
              Reset Filters
            </button>
          )}
        </div>
      </div>

      {/* Mobile Filter Bottom Sheet */}
      {isMobileFilterOpen && (
        <div
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-end sm:items-center justify-center p-0 sm:p-4 md:hidden animate-in fade-in"
          onClick={() => setIsMobileFilterOpen(false)}
        >
          <div
            className="w-full sm:max-w-md bg-white rounded-t-3xl sm:rounded-3xl p-5 shadow-2xl space-y-4 animate-in slide-in-from-bottom-5 duration-200 max-h-[88vh] overflow-y-auto no-scrollbar safe-bottom border border-slate-100"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Mobile Drag Indicator */}
            <div className="w-12 h-1.5 bg-slate-200 rounded-full mx-auto -mt-1 mb-1 sm:hidden" />

            {/* Header */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center">
                  <SlidersHorizontal className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-slate-900 leading-tight">
                    {isAdmin ? "Filter Records" : "Filter by Date"}
                  </h3>
                  <p className="text-[11px] text-slate-400">Refine the displayed records</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsMobileFilterOpen(false)}
                className="p-1.5 rounded-full text-slate-400 hover:text-slate-700 hover:bg-slate-100 touch-target-min flex items-center justify-center transition-colors"
                aria-label="Close filters"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Admin: Filter by Uploader */}
            {isAdmin && uniqueUploaders.length > 0 && (
              <div className="space-y-1.5">
                <label className="text-xs font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
                  <Users className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Uploaded by Users</span>
                </label>
                <UploaderDropdown
                  uploaders={uniqueUploaders}
                  selected={uploaderFilter}
                  onSelect={setUploaderFilter}
                  isMobile
                />
              </div>
            )}

            {/* Date Filter (Both for Admin and Users) */}
            <div className="bg-slate-50/80 rounded-2xl p-3.5 border border-slate-200/70 space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Date Range</span>
                </label>
                {(startDate || endDate || datePreset !== "all") && (
                  <button
                    type="button"
                    onClick={() => handleSetDatePreset("all")}
                    className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800"
                  >
                    Reset Date
                  </button>
                )}
              </div>

              {/* Quick Presets */}
              <div className="grid grid-cols-4 gap-1.5">
                {(
                  [
                    { id: "all", label: "All" },
                    { id: "today", label: "Today" },
                    { id: "yesterday", label: "Yesterday" },
                    { id: "week", label: "Last 7D" },
                  ] as const
                ).map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => handleSetDatePreset(p.id)}
                    className={`py-2 px-1 text-center rounded-xl text-xs font-semibold transition-all ${
                      datePreset === p.id && !startDate && !endDate && p.id === "all"
                        ? "bg-indigo-600 text-white shadow-sm"
                        : datePreset === p.id && p.id !== "all"
                        ? "bg-indigo-600 text-white shadow-sm"
                        : "bg-white text-slate-700 border border-slate-200 hover:bg-slate-100"
                    }`}
                  >
                    {p.label}
                  </button>
                ))}
              </div>

              {/* Custom Date Range Inputs */}
              <div className="grid grid-cols-2 gap-2 pt-1">
                <div>
                  <span className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                    From Date
                  </span>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => {
                      setStartDate(e.target.value);
                      setDatePreset("all");
                    }}
                    className="w-full px-2.5 py-2 bg-white border border-slate-200 rounded-xl text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <span className="block text-[10px] font-bold text-slate-500 uppercase tracking-wider mb-1">
                    To Date
                  </span>
                  <input
                    type="date"
                    value={endDate}
                    onChange={(e) => {
                      setEndDate(e.target.value);
                      setDatePreset("all");
                    }}
                    className="w-full px-2.5 py-2 bg-white border border-slate-200 rounded-xl text-xs font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="pt-2 flex gap-2">
              <button
                type="button"
                onClick={handleResetFilters}
                className="flex-1 py-3 px-4 rounded-xl bg-slate-100 text-slate-700 text-xs font-bold hover:bg-slate-200 active:scale-[0.98] transition-all touch-target-min"
              >
                Reset All
              </button>
              <button
                type="button"
                onClick={() => setIsMobileFilterOpen(false)}
                className="flex-1 py-3 px-4 rounded-xl bg-indigo-600 text-white text-xs font-bold hover:bg-indigo-700 active:scale-[0.98] transition-all touch-target-min shadow-md shadow-indigo-200"
              >
                Apply Filters
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Results Header */}
      <div className="flex items-center justify-between mb-3 px-1">
        <div className="text-xs text-slate-500 font-medium">
          Showing <span className="font-bold text-slate-800">{filteredRecords.length}</span> of{" "}
          {totalCount} records
        </div>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={handleResetFilters}
            className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800"
          >
            Clear Filters
          </button>
        )}
      </div>

      {/* Records Display: Mobile Cards (< 768px) vs Responsive Table (>= 768px) */}
      {filteredRecords.length === 0 ? (
        <EmptyState
          type={records.length === 0 ? "no-records" : "no-search-results"}
          onAction={records.length === 0 ? onNavigateToUpload : handleResetFilters}
          actionLabel={records.length === 0 ? "Capture New Image" : "Reset Filters"}
        />
      ) : (
        <>
          {/* Mobile Card Layout (< 768px) */}
          <div className="grid grid-cols-1 gap-3 md:hidden">
            {filteredRecords.map((record) => (
              <RecordCard
                key={record.id}
                record={record}
                onSelect={(r) => setSelectedRecord(r)}
              />
            ))}
          </div>

          {/* Tablet/Desktop Table Layout (>= 768px) */}
          <div className="hidden md:block">
            <RecordTable
              records={filteredRecords}
              onSelect={(r) => setSelectedRecord(r)}
            />
          </div>
        </>
      )}

      {/* Record Detail Modal */}
      <RecordDetailModal record={selectedRecord} onClose={() => setSelectedRecord(null)} />
    </div>
  );
};
