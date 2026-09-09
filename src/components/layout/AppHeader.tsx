"use client";

import React from "react";
import { User } from "@/types";
import { LogOut } from "lucide-react";

interface AppHeaderProps {
  user: User;
  onLogout: () => void;
  activeTab: "upload" | "records" | "users";
  onTabChange: (tab: "upload" | "records" | "users") => void;
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
        return "bg-purple-900/60 text-purple-200 border-purple-500/40";
      case "Supervisor":
        return "bg-amber-900/60 text-amber-200 border-amber-500/40";
      case "Operations":
        return "bg-cyan-900/60 text-cyan-200 border-cyan-500/40";
      case "Partner":
        return "bg-indigo-900/60 text-indigo-200 border-indigo-500/40";
      case "Lender":
        return "bg-emerald-900/60 text-emerald-200 border-emerald-500/40";
      case "Field User":
      default:
        return "bg-blue-900/60 text-blue-200 border-blue-500/40";
    }
  };

  return (
    <header className="sticky top-0 z-40 w-full bg-[#021329]/95 backdrop-blur-xl border-b border-cyan-500/20 shadow-lg shadow-cyan-950/40 transition-colors">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-3">
        {/* Left: Brand / App Title with Animated Qualtech Logo */}
        <div className="flex items-center gap-3 sm:gap-3.5 min-w-0">
          <div className="flex items-center justify-center shrink-0 py-1">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src="/qualtech-logo.png"
              alt="Qualtech Logo"
              className="h-7 sm:h-8.5 w-auto max-h-[34px] object-contain brightness-110 animate-logo-breathe hover:scale-110 transition-transform duration-300 drop-shadow-[0_2px_10px_rgba(56,162,194,0.45)]"
              style={{ maxHeight: "34px", width: "auto" }}
            />
          </div>
          <div className="min-w-0">
            <div className="flex items-center gap-1.5 sm:gap-2 flex-wrap sm:flex-nowrap">
              <h1 className="text-xs sm:text-[13px] font-bold text-white tracking-tight leading-none truncate">
                GFF Visiting Card <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-teal-300">OCR Portal</span>
              </h1>
              <span className="inline-flex items-center text-[8px] font-semibold px-1.5 py-0.5 rounded-full bg-cyan-950/80 text-cyan-300 border border-cyan-500/30 shrink-0">
                Smart OCR
              </span>
            </div>
            <span className="text-[9px] text-cyan-200/50 font-medium block truncate mt-0.5">Global Fintech Fest</span>
          </div>
        </div>

        {/* Center: Desktop Navigation Tabs */}
        <nav className="hidden md:flex items-center bg-[#031d38]/70 p-1 rounded-xl border border-cyan-500/20">
          <button
            type="button"
            onClick={() => onTabChange("upload")}
            className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === "upload"
                ? "bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-md shadow-cyan-500/25"
                : "text-slate-300 hover:text-white hover:bg-white/5"
            }`}
          >
            Capture & Upload
          </button>
          <button
            type="button"
            onClick={() => onTabChange("records")}
            className={`flex items-center gap-1.5 px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
              activeTab === "records"
                ? "bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-md shadow-cyan-500/25"
                : "text-slate-300 hover:text-white hover:bg-white/5"
            }`}
          >
            <span>{user.role === "Admin" ? "All Records" : "My Records"}</span>
            {recordsCount > 0 && (
              <span
                className={`text-[9px] font-bold px-1.5 py-0.2 rounded-full ${
                  activeTab === "records"
                    ? "bg-white text-slate-900"
                    : "bg-cyan-950 text-cyan-300 border border-cyan-500/30"
                }`}
              >
                {recordsCount}
              </span>
            )}
          </button>
          {user.role === "Admin" && (
            <button
              type="button"
              onClick={() => onTabChange("users")}
              className={`flex items-center gap-1.5 px-4 py-1.5 rounded-lg text-xs font-semibold transition-all ${
                activeTab === "users"
                  ? "bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-md shadow-cyan-500/25"
                  : "text-slate-300 hover:text-white hover:bg-white/5"
              }`}
            >
              <span>User Management</span>
            </button>
          )}
        </nav>

        {/* Right: User Profile & Logout */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2.5">
            {user.avatar ? (
              <img
                src={user.avatar}
                alt={user.name}
                className="w-9 h-9 rounded-full object-cover ring-2 ring-cyan-500/40"
              />
            ) : (
              <div className="w-9 h-9 rounded-full bg-[#031d38] text-cyan-300 border border-cyan-500/30 flex items-center justify-center font-bold text-sm shadow-sm">
                {user.name.charAt(0)}
              </div>
            )}
            <div className="hidden sm:block text-left">
              <div className="text-xs font-bold text-white leading-tight">
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
            className="p-2 text-slate-300 hover:text-rose-400 hover:bg-rose-950/40 border border-transparent hover:border-rose-800/40 rounded-xl transition-all touch-target-min flex items-center justify-center"
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
