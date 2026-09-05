"use client";

import React from "react";
import { User } from "@/types";
import { LogOut, ShieldCheck, Shield } from "lucide-react";

interface AppHeaderProps {
  user: User;
  onLogout: () => void;
  activeTab: "upload" | "records";
  onTabChange: (tab: "upload" | "records") => void;
  recordsCount?: number;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  user,
  onLogout,
  activeTab,
  onTabChange,
  recordsCount = 0,
}) => {
  const getRoleBadge = (role: string) => {
    switch (role) {
      case "Admin":
        return "bg-purple-100 text-purple-800 border-purple-200";
      case "Supervisor":
        return "bg-amber-100 text-amber-800 border-amber-200";
      case "Field User":
      default:
        return "bg-blue-100 text-blue-800 border-blue-200";
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full glass-header border-b border-slate-200/80 shadow-xs">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-3">
        {/* Left: Brand / App Title */}
        <div className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-indigo-600 text-white flex items-center justify-center shadow-sm">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-sm sm:text-base font-bold text-slate-900 tracking-tight leading-none">
                FieldCapture
              </h1>
              <span className="inline-flex items-center text-[9px] font-bold px-1.5 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                IRIS OCR
              </span>
            </div>
            <span className="text-[10px] text-slate-500 font-medium">GFF Enterprise Portal</span>
          </div>
        </div>

        {/* Center: Desktop Navigation Tabs */}
        <nav className="hidden md:flex items-center bg-slate-100/90 p-1 rounded-xl border border-slate-200/60">
          <button
            type="button"
            onClick={() => onTabChange("upload")}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === "upload"
                ? "bg-white text-indigo-700 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            Capture & Upload
          </button>
          <button
            type="button"
            onClick={() => onTabChange("records")}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === "records"
                ? "bg-white text-indigo-700 shadow-sm"
                : "text-slate-600 hover:text-slate-900"
            }`}
          >
            <span>{user.role === "Admin" ? "All Records" : "My Records"}</span>
            {recordsCount > 0 && (
              <span className="bg-indigo-600 text-white text-[9px] font-bold px-1.5 py-0.2 rounded-full">
                {recordsCount}
              </span>
            )}
          </button>
        </nav>

        {/* Right: User Profile & Logout */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2.5">
            {user.avatar ? (
              <img
                src={user.avatar}
                alt={user.name}
                className="w-9 h-9 rounded-full object-cover ring-2 ring-indigo-500/20"
              />
            ) : (
              <div className="w-9 h-9 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-sm">
                {user.name.charAt(0)}
              </div>
            )}
            <div className="hidden sm:block text-left">
              <div className="text-xs font-bold text-slate-900 leading-tight">
                {user.name}
              </div>
              <div className="flex items-center gap-1 mt-0.5">
                <span
                  className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border ${getRoleBadge(
                    user.role
                  )}`}
                >
                  {user.role}
                </span>
              </div>
            </div>
          </div>

          {/* Logout Button */}
          <button
            type="button"
            onClick={onLogout}
            className="p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition-all touch-target-min flex items-center justify-center"
            title="Log out"
            aria-label="Logout"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
