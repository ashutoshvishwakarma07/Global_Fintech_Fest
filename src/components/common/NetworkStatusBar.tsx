"use client";

import React from "react";
import { Wifi, WifiOff, RefreshCw } from "lucide-react";

interface NetworkStatusBarProps {
  isOnline: boolean;
  isSimulated: boolean;
  onToggleSimulation: () => void;
  pendingCount?: number;
}

export const NetworkStatusBar: React.FC<NetworkStatusBarProps> = ({
  isOnline,
  isSimulated,
  onToggleSimulation,
  pendingCount = 0,
}) => {
  return (
    <div
      className={`w-full py-2 px-4 transition-colors duration-300 text-xs flex items-center justify-between border-b ${
        isOnline
          ? "bg-emerald-50/90 border-emerald-200/80 text-emerald-800"
          : "bg-rose-50/95 border-rose-200 text-rose-900"
      }`}
    >
      <div className="flex items-center gap-2 max-w-2xl">
        {isOnline ? (
          <>
            <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse shrink-0" />
            <span className="font-bold flex items-center gap-1">
              <Wifi className="w-3.5 h-3.5 text-emerald-600" />
              Online
            </span>
            <span className="hidden sm:inline text-[11px] opacity-80">
              &bull; Connected to verification servers
            </span>
          </>
        ) : (
          <>
            <div className="w-2 h-2 rounded-full bg-rose-500 shrink-0" />
            <span className="font-bold flex items-center gap-1 text-rose-700">
              <WifiOff className="w-3.5 h-3.5 text-rose-600" />
              Offline
            </span>
            <span className="text-[11px] font-medium opacity-90 truncate">
              &bull; Uploads will be saved locally and synced automatically when connected
            </span>
          </>
        )}

        {pendingCount > 0 && (
          <span className="ml-1.5 px-2 py-0.5 rounded-full bg-amber-100 text-amber-900 border border-amber-300 text-[10px] font-bold">
            {pendingCount} queued
          </span>
        )}
      </div>

      {/* Developer / Tester Quick Toggle */}
      <button
        type="button"
        onClick={onToggleSimulation}
        className={`px-2.5 py-1 rounded-lg text-[11px] font-semibold border transition-all active:scale-95 touch-target-min flex items-center gap-1.5 shrink-0 shadow-xs ${
          isSimulated
            ? "bg-rose-600 hover:bg-rose-700 text-white border-rose-600"
            : "bg-white hover:bg-slate-100 text-slate-700 border-slate-300"
        }`}
        title="Toggle simulated offline mode for testing"
      >
        <RefreshCw className="w-3 h-3" />
        <span>{isSimulated ? "Restore Online" : "Simulate Offline"}</span>
      </button>
    </div>
  );
};
