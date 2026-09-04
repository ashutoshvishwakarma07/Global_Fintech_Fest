"use client";

import React, { useState, useMemo } from "react";
import { User, UploadRecord, UserRole, RecordStatus } from "@/types";
import { RecordCard } from "./RecordCard";
import { RecordTable } from "./RecordTable";
import { RecordDetailModal } from "./RecordDetailModal";
import { EmptyState } from "../common/EmptyState";
import {
  Search,
  SlidersHorizontal,
  ArrowUpDown,
  FolderOpen,
  Clock,
  UserCheck,
  X,
  Plus,
  RefreshCw,
  HardDrive,
  AlertCircle,
  CheckCircle2,
} from "lucide-react";

interface RecordsDashboardProps {
  currentUser: User;
  records: UploadRecord[];
  isOnline: boolean;
  isSyncing: boolean;
  syncProgress: { current: number; total: number; message: string };
  onTriggerSync: () => void;
  onSyncSingle: (id: string) => void;
  onNavigateToUpload: () => void;
}

export const RecordsDashboard: React.FC<RecordsDashboardProps> = ({
  currentUser,
  records,
  isOnline,
  isSyncing,
  syncProgress,
  onTriggerSync,
  onSyncSingle,
  onNavigateToUpload,
}) => {
  const [searchQuery, setSearchQuery] = useState("");
  const [roleFilter, setRoleFilter] = useState<"All" | UserRole>("All");
  const [statusFilter, setStatusFilter] = useState<"All" | RecordStatus>("All");
  const [sortBy, setSortBy] = useState<"latest" | "oldest">("latest");
  const [selectedRecord, setSelectedRecord] = useState<UploadRecord | null>(null);
  const [isMobileFilterOpen, setIsMobileFilterOpen] = useState(false);

  // Compute pending items count
  const pendingRecords = useMemo(() => {
    return records.filter(
      (r) => r.status === "Pending Upload" || r.status === "Failed" || r.status === "Uploading"
    );
  }, [records]);

  const pendingCount = pendingRecords.length;
  const totalCount = records.length;
  const myUploadsCount = records.filter(
    (r) => r.email.toLowerCase() === currentUser.email.toLowerCase()
  ).length;

  const latestUploadTime = useMemo(() => {
    if (records.length === 0) return "None";
    return records[0]?.uploadedAt || "None";
  }, [records]);

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
          const matchesNotes = record.notes?.toLowerCase().includes(q) || false;
          if (!matchesId && !matchesName && !matchesEmail && !matchesMobile && !matchesNotes) {
            return false;
          }
        }

        // Role filter
        if (roleFilter !== "All" && record.role !== roleFilter) {
          return false;
        }

        // Status filter
        if (statusFilter !== "All" && record.status !== statusFilter) {
          return false;
        }

        return true;
      })
      .sort((a, b) => {
        if (sortBy === "latest") {
          return b.uploadedAt.localeCompare(a.uploadedAt);
        } else {
          return a.uploadedAt.localeCompare(b.uploadedAt);
        }
      });
  }, [records, searchQuery, roleFilter, statusFilter, sortBy]);

  const hasActiveFilters =
    roleFilter !== "All" || statusFilter !== "All" || searchQuery.trim() !== "";

  const handleResetFilters = () => {
    setSearchQuery("");
    setRoleFilter("All");
    setStatusFilter("All");
    setSortBy("latest");
  };

  return (
    <div className="w-full max-w-6xl mx-auto pb-24 md:pb-12 animate-in fade-in duration-300">
      {/* Offline Pending Uploads Alert Banner & Sync Control */}
      {pendingCount > 0 && (
        <div className="mb-5 bg-gradient-to-r from-amber-500 to-amber-600 text-white rounded-2xl p-4 sm:p-5 shadow-lg shadow-amber-200/50 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 animate-in slide-in-from-top-2">
          <div className="flex items-start gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center shrink-0">
              <HardDrive className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="font-bold text-sm sm:text-base">
                  Pending Uploads: {pendingCount}
                </span>
                <span className="bg-amber-800/60 px-2 py-0.5 rounded text-[10px] font-mono">
                  Stored in IndexedDB
                </span>
              </div>
              <p className="text-xs text-amber-50 mt-0.5 max-w-md">
                {syncProgress.message ||
                  (isOnline
                    ? "Ready to sync with verification cloud."
                    : "Saved safely on device. Will auto-upload when reconnected.")}
              </p>
            </div>
          </div>

          <div className="w-full sm:w-auto flex items-center gap-2">
            <button
              type="button"
              disabled={isSyncing || !isOnline}
              onClick={onTriggerSync}
              className="w-full sm:w-auto px-4 py-2.5 rounded-xl bg-white text-amber-900 hover:bg-amber-50 active:scale-95 font-bold text-xs sm:text-sm flex items-center justify-center gap-2 shadow-sm transition-all disabled:opacity-50 touch-target-min"
            >
              <RefreshCw className={`w-4 h-4 ${isSyncing ? "animate-spin" : ""}`} />
              <span>
                {isSyncing
                  ? `Syncing ${syncProgress.current} of ${syncProgress.total}...`
                  : isOnline
                  ? "Sync Now"
                  : "Offline (Will sync auto)"}
              </span>
            </button>
          </div>
        </div>
      )}

      {/* Top Metrics Row */}
      <div className="grid grid-cols-3 gap-2.5 sm:gap-4 mb-5">
        {/* Total Uploads */}
        <div className="bg-white rounded-2xl p-3 sm:p-4 border border-slate-200/80 shadow-card">
          <div className="flex items-center gap-1.5 sm:gap-2 text-slate-500 mb-1">
            <div className="w-6 h-6 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0">
              <FolderOpen className="w-3.5 h-3.5" />
            </div>
            <span className="text-[10px] sm:text-xs font-semibold uppercase tracking-wider truncate">
              Total Records
            </span>
          </div>
          <div className="text-lg sm:text-2xl font-black text-slate-900">{totalCount}</div>
        </div>

        {/* My Uploads */}
        <div className="bg-white rounded-2xl p-3 sm:p-4 border border-slate-200/80 shadow-card">
          <div className="flex items-center gap-1.5 sm:gap-2 text-slate-500 mb-1">
            <div className="w-6 h-6 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
              <UserCheck className="w-3.5 h-3.5" />
            </div>
            <span className="text-[10px] sm:text-xs font-semibold uppercase tracking-wider truncate">
              My Uploads
            </span>
          </div>
          <div className="text-lg sm:text-2xl font-black text-slate-900">{myUploadsCount}</div>
        </div>

        {/* Latest Activity */}
        <div className="bg-white rounded-2xl p-3 sm:p-4 border border-slate-200/80 shadow-card">
          <div className="flex items-center gap-1.5 sm:gap-2 text-slate-500 mb-1">
            <div className="w-6 h-6 rounded-lg bg-amber-50 text-amber-600 flex items-center justify-center shrink-0">
              <Clock className="w-3.5 h-3.5" />
            </div>
            <span className="text-[10px] sm:text-xs font-semibold uppercase tracking-wider truncate">
              Latest Activity
            </span>
          </div>
          <div className="text-xs sm:text-sm font-bold text-slate-800 truncate font-mono mt-1">
            {latestUploadTime}
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
              placeholder="Search by ID, name, email, phone..."
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
        <div className="hidden md:flex items-center justify-between pt-2 border-t border-slate-100 text-xs gap-3">
          <div className="flex items-center gap-2 flex-wrap">
            <span className="text-slate-400 font-medium text-[11px] uppercase tracking-wider">
              Role:
            </span>
            {(["All", "Field User", "Supervisor"] as const).map((r) => (
              <button
                key={r}
                type="button"
                onClick={() => setRoleFilter(r)}
                className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${
                  roleFilter === r
                    ? "bg-indigo-600 text-white shadow-xs"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                {r}
              </button>
            ))}

            <div className="h-4 w-[1px] bg-slate-200 mx-1" />

            <span className="text-slate-400 font-medium text-[11px] uppercase tracking-wider">
              Status:
            </span>
            {(["All", "Uploaded", "Pending Upload", "Verified", "Failed"] as const).map((s) => (
              <button
                key={s}
                type="button"
                onClick={() => setStatusFilter(s)}
                className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${
                  statusFilter === s
                    ? "bg-indigo-600 text-white shadow-xs"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                {s}
              </button>
            ))}
          </div>

          {/* Sort Dropdown */}
          <div className="flex items-center gap-1.5 shrink-0">
            <ArrowUpDown className="w-3.5 h-3.5 text-slate-400" />
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value as "latest" | "oldest")}
              aria-label="Sort records"
              className="bg-slate-100 border border-slate-200 rounded-lg py-1 px-2 text-xs font-medium text-slate-700 focus:outline-none focus:ring-1 focus:ring-indigo-500"
            >
              <option value="latest">Sort: Latest First</option>
              <option value="oldest">Sort: Oldest First</option>
            </select>
          </div>
        </div>
      </div>

      {/* Mobile Filter Bottom Sheet */}
      {isMobileFilterOpen && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-end justify-center md:hidden animate-in fade-in">
          <div
            className="w-full bg-white rounded-t-3xl p-5 shadow-2xl space-y-4 animate-in slide-in-from-bottom-5 duration-200 max-h-[85vh] overflow-y-auto safe-bottom"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <SlidersHorizontal className="w-4 h-4 text-indigo-600" />
                <h3 className="text-sm font-bold text-slate-900">Filter & Sort Records</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsMobileFilterOpen(false)}
                className="p-1 rounded-full text-slate-400 hover:text-slate-700 touch-target-min flex items-center justify-center"
                aria-label="Close filters"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Role Filter Options */}
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
                Filter By Role
              </label>
              <div className="grid grid-cols-3 gap-2">
                {(["All", "Field User", "Supervisor"] as const).map((r) => (
                  <button
                    key={r}
                    type="button"
                    onClick={() => setRoleFilter(r)}
                    className={`py-2 px-2 text-center rounded-xl text-xs font-semibold touch-target-min transition-all ${
                      roleFilter === r
                        ? "bg-indigo-600 text-white shadow-sm"
                        : "bg-slate-100 text-slate-700"
                    }`}
                  >
                    {r}
                  </button>
                ))}
              </div>
            </div>

            {/* Status Filter Options */}
            <div>
              <label className="block text-xs font-bold text-slate-600 uppercase tracking-wider mb-2">
                Filter By Status
              </label>
              <div className="grid grid-cols-2 gap-2">
                {(["All", "Uploaded", "Pending Upload", "Verified", "Failed"] as const).map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => setStatusFilter(s)}
                    className={`py-2 px-2 text-center rounded-xl text-xs font-semibold touch-target-min transition-all ${
                      statusFilter === s
                        ? "bg-indigo-600 text-white shadow-sm"
                        : "bg-slate-100 text-slate-700"
                    }`}
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>

            {/* Actions */}
            <div className="pt-3 flex gap-2">
              <button
                type="button"
                onClick={handleResetFilters}
                className="flex-1 py-3 px-4 rounded-xl bg-slate-100 text-slate-700 text-xs font-bold touch-target-min"
              >
                Reset
              </button>
              <button
                type="button"
                onClick={() => setIsMobileFilterOpen(false)}
                className="flex-1 py-3 px-4 rounded-xl bg-indigo-600 text-white text-xs font-bold touch-target-min shadow-md shadow-indigo-200"
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
                onSyncSingle={onSyncSingle}
                isSyncing={isSyncing}
              />
            ))}
          </div>

          {/* Tablet/Desktop Table Layout (>= 768px) */}
          <div className="hidden md:block">
            <RecordTable
              records={filteredRecords}
              onSelect={(r) => setSelectedRecord(r)}
              onSyncSingle={onSyncSingle}
              isSyncing={isSyncing}
            />
          </div>
        </>
      )}

      {/* Record Detail Modal */}
      <RecordDetailModal record={selectedRecord} onClose={() => setSelectedRecord(null)} />
    </div>
  );
};
