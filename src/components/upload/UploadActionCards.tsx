"use client";

import React, { useState, useRef } from "react";
import { Camera, ImageUp, Sparkles, Layers, FileText } from "lucide-react";
import { TwoSideCapture } from "./TwoSideCapture";
import { CollageResult } from "@/services/collageService";
import { CaptureMode } from "@/types";
import { imageProcessing, ACCEPTED_FILE_INPUT_TYPES } from "@/utils/imageProcessing";

interface UploadActionCardsProps {
  onOpenCamera: (target?: "single" | "front" | "back") => void;
  onSelectImage: (file: File) => void;
  onDirectCameraInput: (file: File) => void;
  onTwoSideCollageReady: (collage: CollageResult, frontUrl: string, backUrl: string) => void;
  capturedCameraImage?: { target: "front" | "back"; dataUrl: string } | null;
  onCameraImageConsumed?: () => void;
}

export const UploadActionCards: React.FC<UploadActionCardsProps> = ({
  onOpenCamera,
  onSelectImage,
  onDirectCameraInput,
  onTwoSideCollageReady,
  capturedCameraImage,
  onCameraImageConsumed,
}) => {
  const [captureMode, setCaptureMode] = useState<CaptureMode>("single");

  const galleryInputRef = useRef<HTMLInputElement | null>(null);
  const nativeCameraInputRef = useRef<HTMLInputElement | null>(null);

  const handleGalleryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const validation = imageProcessing.validateFile(file);
      if (!validation.valid) {
        alert(validation.error || "Unsupported file format. Please upload PNG, JPG/JPEG, Word (.doc, .docx), or PDF files only.");
        e.target.value = "";
        return;
      }
      onSelectImage(file);
      e.target.value = "";
    }
  };

  const handleNativeCameraChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const validation = imageProcessing.validateFile(file);
      if (!validation.valid) {
        alert(validation.error || "Unsupported file format.");
        e.target.value = "";
        return;
      }
      onDirectCameraInput(file);
      e.target.value = "";
    }
  };

  return (
    <div className="w-full space-y-4">
      {/* Hidden file inputs for Single Side mode */}
      <input
        ref={galleryInputRef}
        type="file"
        accept={ACCEPTED_FILE_INPUT_TYPES}
        onChange={handleGalleryChange}
        className="hidden"
      />
      <input
        ref={nativeCameraInputRef}
        type="file"
        accept={ACCEPTED_FILE_INPUT_TYPES}
        capture="environment"
        onChange={handleNativeCameraChange}
        className="hidden"
      />

      {/* Mode Switcher Pills */}
      <div className="flex p-1 bg-slate-100/90 rounded-2xl border border-slate-200/80 shadow-inner">
        <button
          type="button"
          onClick={() => setCaptureMode("single")}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-semibold transition-all touch-target-min ${
            captureMode === "single"
              ? "bg-white text-indigo-700 shadow-sm"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          <FileText className="w-4 h-4" />
          Single Side (Front)
        </button>

        <button
          type="button"
          onClick={() => setCaptureMode("two-sided")}
          className={`flex-1 flex items-center justify-center gap-2 py-2.5 px-3 rounded-xl text-xs sm:text-sm font-semibold transition-all touch-target-min ${
            captureMode === "two-sided"
              ? "bg-white text-indigo-700 shadow-sm"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          <Layers className="w-4 h-4" />
          Two Sides (Front + Back)
        </button>
      </div>

      {captureMode === "single" ? (
        /* ================= SINGLE SIDE FLOW ================= */
        <div className="space-y-3.5">
          {/* Hero Action 1: Open Camera */}
          <button
            type="button"
            onClick={() => {
              if (
                typeof window !== "undefined" &&
                !window.isSecureContext &&
                window.location.hostname !== "localhost" &&
                window.location.hostname !== "127.0.0.1"
              ) {
                nativeCameraInputRef.current?.click();
                return;
              }
              onOpenCamera("single");
            }}
            className="group relative w-full p-5 sm:p-6 bg-gradient-to-r from-indigo-600 via-indigo-700 to-indigo-800 text-white rounded-2xl sm:rounded-3xl shadow-lg shadow-indigo-500/25 hover:shadow-indigo-500/40 active:scale-[0.98] transition-all flex items-center justify-between text-left overflow-hidden touch-target-min"
          >
            <div className="flex items-center gap-4 z-10">
              <div className="w-14 h-14 rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center shrink-0 border border-white/20 group-hover:scale-105 transition-transform">
                <Camera className="w-7 h-7 text-white" />
              </div>
              <div>
                <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-indigo-500/40 text-[10px] font-bold text-indigo-100 uppercase tracking-wider mb-1">
                  <Sparkles className="w-3 h-3 text-amber-300" /> Recommended
                </div>
                <h2 className="text-lg sm:text-xl font-bold tracking-tight">Open Camera</h2>
                <p className="text-xs sm:text-sm text-indigo-100/90 mt-0.5">
                  Launch live viewfinder to capture instant verification photo
                </p>
              </div>
            </div>
          </button>

          {/* Hero Action 2: Upload from Gallery */}
          <button
            type="button"
            onClick={() => galleryInputRef.current?.click()}
            className="group relative w-full p-5 sm:p-6 bg-white hover:bg-slate-50 border-2 border-dashed border-slate-300 hover:border-indigo-400 rounded-2xl sm:rounded-3xl shadow-sm active:scale-[0.98] transition-all flex items-center justify-between text-left touch-target-min"
          >
            <div className="flex items-center gap-4">
              <div className="w-14 h-14 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center shrink-0 group-hover:scale-105 transition-transform">
                <ImageUp className="w-7 h-7" />
              </div>
              <div>
                <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                  From Device Storage
                </span>
                <h2 className="text-base sm:text-lg font-bold text-slate-900 tracking-tight">
                  Upload from Gallery
                </h2>
                <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
                  Select existing photos, documents, or screenshots
                </p>
              </div>
            </div>

            <div className="hidden sm:flex items-center justify-center px-3 py-1.5 rounded-xl bg-slate-100 text-slate-700 text-xs font-semibold">
              Browse Files
            </div>
          </button>
        </div>
      ) : (
        /* ================= TWO SIDES FLOW ================= */
        <TwoSideCapture
          onCollageReady={onTwoSideCollageReady}
          onTriggerCamera={(target) => onOpenCamera(target)}
          capturedCameraImage={capturedCameraImage}
          onCameraImageConsumed={onCameraImageConsumed}
        />
      )}
    </div>
  );
};
