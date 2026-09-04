"use client";

import React, { useRef } from "react";
import { Camera, ImageUp, UploadCloud, Sparkles, AlertCircle } from "lucide-react";

interface UploadActionCardsProps {
  onOpenCamera: () => void;
  onSelectImage: (file: File) => void;
  onDirectCameraInput: (file: File) => void;
}

export const UploadActionCards: React.FC<UploadActionCardsProps> = ({
  onOpenCamera,
  onSelectImage,
  onDirectCameraInput,
}) => {
  const galleryInputRef = useRef<HTMLInputElement | null>(null);
  const nativeCameraInputRef = useRef<HTMLInputElement | null>(null);

  const handleGalleryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onSelectImage(file);
      e.target.value = "";
    }
  };

  const handleNativeCameraChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onDirectCameraInput(file);
      e.target.value = "";
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    const file = e.dataTransfer.files?.[0];
    if (file && file.type.startsWith("image/")) {
      onSelectImage(file);
    }
  };

  return (
    <div className="w-full space-y-4">
      {/* Hidden file inputs for maximum platform compatibility */}
      <input
        ref={galleryInputRef}
        type="file"
        accept="image/*"
        onChange={handleGalleryChange}
        className="hidden"
      />
      <input
        ref={nativeCameraInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        onChange={handleNativeCameraChange}
        className="hidden"
      />

      {/* Hero Action 1: Open Camera (Primary Action) */}
      <button
        type="button"
        onClick={onOpenCamera}
        className="group relative w-full p-5 sm:p-6 bg-gradient-to-br from-indigo-600 to-indigo-700 hover:from-indigo-700 hover:to-indigo-800 text-white rounded-2xl sm:rounded-3xl shadow-lg shadow-indigo-200 active:scale-[0.98] transition-all flex items-center justify-between text-left touch-target-min overflow-hidden border border-indigo-500/30"
      >
        <div className="relative z-10 flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-white/15 backdrop-blur-md flex items-center justify-center text-white shrink-0 group-hover:scale-105 transition-transform shadow-inner">
            <Camera className="w-7 h-7" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-white/20 text-white text-[10px] font-semibold uppercase tracking-wider mb-1">
              <Sparkles className="w-3 h-3" /> Recommended
            </div>
            <h2 className="text-lg sm:text-xl font-bold tracking-tight">Open Camera</h2>
            <p className="text-xs sm:text-sm text-indigo-100/90 mt-0.5">
              Launch live viewfinder to capture instant verification photo
            </p>
          </div>
        </div>

        {/* Subtle decorative glow */}
        <div className="absolute -right-8 -bottom-8 w-32 h-32 rounded-full bg-white/10 blur-xl pointer-events-none" />
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

      {/* Desktop Drag & Drop Area (Progressive enhancement for tablet/desktop) */}
      <div
        onDragOver={(e) => e.preventDefault()}
        onDrop={handleDrop}
        className="hidden md:flex flex-col items-center justify-center p-6 border border-slate-200 bg-slate-50/50 rounded-2xl text-center cursor-pointer hover:bg-slate-50 transition-colors"
        onClick={() => galleryInputRef.current?.click()}
      >
        <UploadCloud className="w-8 h-8 text-slate-400 mb-1" />
        <span className="text-xs font-medium text-slate-700">Or drag and drop image files here</span>
        <span className="text-[11px] text-slate-400">Supports JPG, PNG, WEBP up to 15MB</span>
      </div>
    </div>
  );
};
