"use client";

import React from "react";
import { Camera, FolderKanban, ShieldCheck } from "lucide-react";

interface BottomNavProps {
  activeTab: "upload" | "records";
  onTabChange: (tab: "upload" | "records") => void;
  recordCount?: number;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  activeTab,
  onTabChange,
  recordCount = 0,
}) => {
  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-lg border-t border-slate-200/90 safe-bottom">
      <div className="grid grid-cols-2 max-w-md mx-auto px-4 py-1.5">
        <button
          type="button"
          onClick={() => onTabChange("upload")}
          className={`flex flex-col items-center justify-center py-2 px-3 rounded-xl transition-all touch-target-min ${
            activeTab === "upload"
              ? "text-indigo-600 font-bold"
              : "text-slate-400 hover:text-slate-600 font-medium"
          }`}
        >
          <div className="relative">
            <Camera className={`w-5 h-5 ${activeTab === "upload" ? "stroke-[2.5]" : ""}`} />
            {activeTab === "upload" && (
              <span className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-indigo-600" />
            )}
          </div>
          <span className="text-[11px] mt-1 tracking-tight">Capture & Upload</span>
        </button>

        <button
          type="button"
          onClick={() => onTabChange("records")}
          className={`flex flex-col items-center justify-center py-2 px-3 rounded-xl transition-all touch-target-min ${
            activeTab === "records"
              ? "text-indigo-600 font-bold"
              : "text-slate-400 hover:text-slate-600 font-medium"
          }`}
        >
          <div className="relative">
            <FolderKanban className={`w-5 h-5 ${activeTab === "records" ? "stroke-[2.5]" : ""}`} />
            {recordCount > 0 && (
              <span className="absolute -top-1 -right-2.5 bg-indigo-600 text-white text-[9px] font-bold px-1.5 py-0.2 rounded-full">
                {recordCount}
              </span>
            )}
            {activeTab === "records" && (
              <span className="absolute -bottom-1 left-1/2 -translate-x-1/2 w-1 h-1 rounded-full bg-indigo-600" />
            )}
          </div>
          <span className="text-[11px] mt-1 tracking-tight">Records</span>
        </button>
      </div>
    </nav>
  );
};
