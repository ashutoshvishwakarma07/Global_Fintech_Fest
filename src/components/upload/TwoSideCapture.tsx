"use client";

import React, { useState, useRef } from "react";
import { collageService, CollageResult } from "@/services/collageService";
import {
  Camera,
  ImageUp,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  Sparkles,
  ArrowRight,
  Layers,
  FileCheck,
  RefreshCw,
  Eye,
  Sliders,
} from "lucide-react";

interface TwoSideCaptureProps {
  onCollageReady: (collage: CollageResult, frontUrl: string, backUrl: string) => void;
  onCancel?: () => void;
  onTriggerCamera: (target: "front" | "back") => void;
  capturedCameraImage?: { target: "front" | "back"; dataUrl: string } | null;
  onCameraImageConsumed?: () => void;
}

export const TwoSideCapture: React.FC<TwoSideCaptureProps> = ({
  onCollageReady,
  onCancel,
  onTriggerCamera,
  capturedCameraImage,
  onCameraImageConsumed,
}) => {
  const [frontImage, setFrontImage] = useState<string | null>(null);
  const [backImage, setBackImage] = useState<string | null>(null);
  const [frontConfirmed, setFrontConfirmed] = useState(false);
  const [backConfirmed, setBackConfirmed] = useState(false);

  const [isGeneratingCollage, setIsGeneratingCollage] = useState(false);
  const [collageResult, setCollageResult] = useState<CollageResult | null>(null);
  const [collageError, setCollageError] = useState<string | null>(null);

  const frontFileInputRef = useRef<HTMLInputElement | null>(null);
  const backFileInputRef = useRef<HTMLInputElement | null>(null);

  // Consume camera capture if available
  React.useEffect(() => {
    if (capturedCameraImage) {
      if (capturedCameraImage.target === "front") {
        setFrontImage(capturedCameraImage.dataUrl);
        setFrontConfirmed(true);
        setCollageResult(null); // Invalidate previous collage if retaken
      } else if (capturedCameraImage.target === "back") {
        setBackImage(capturedCameraImage.dataUrl);
        setBackConfirmed(true);
        setCollageResult(null); // Invalidate previous collage if retaken
      }
      onCameraImageConsumed?.();
    }
  }, [capturedCameraImage, onCameraImageConsumed]);

  const handleFrontFileSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setFrontImage(event.target.result as string);
          setFrontConfirmed(true);
          setCollageResult(null);
        }
      };
      reader.readAsDataURL(file);
      e.target.value = "";
    }
  };

  const handleBackFileSelected = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (event) => {
        if (event.target?.result) {
          setBackImage(event.target.result as string);
          setBackConfirmed(true);
          setCollageResult(null);
        }
      };
      reader.readAsDataURL(file);
      e.target.value = "";
    }
  };

  const handleGenerateCollage = async () => {
    if (!frontImage || !backImage) return;

    setIsGeneratingCollage(true);
    setCollageError(null);

    try {
      const result = await collageService.createDocumentCollage(frontImage, backImage, {
        maxWidth: 1200,
        gap: 28,
        quality: 0.88,
        addLabels: true,
      });
      setCollageResult(result);
    } catch (err: any) {
      console.error("Collage generation failed:", err);
      setCollageError(
        err?.message ||
          "Failed to merge Front and Back images into a collage. Please ensure images are valid and retry."
      );
    } finally {
      setIsGeneratingCollage(false);
    }
  };

  const handleConfirmAndProceed = () => {
    if (collageResult && frontImage && backImage) {
      onCollageReady(collageResult, frontImage, backImage);
    }
  };

  // Stepper calculations
  const isFrontDone = Boolean(frontImage);
  const isBackDone = Boolean(backImage);
  const isCollageDone = Boolean(collageResult);

  return (
    <div className="w-full space-y-5 animate-in fade-in duration-300">
      {/* Hidden File Inputs */}
      <input
        ref={frontFileInputRef}
        type="file"
        accept="image/*"
        onChange={handleFrontFileSelected}
        className="hidden"
      />
      <input
        ref={backFileInputRef}
        type="file"
        accept="image/*"
        onChange={handleBackFileSelected}
        className="hidden"
      />

      {/* Progress Stepper Bar */}
      <div className="bg-white rounded-2xl p-3.5 border border-slate-200/80 shadow-card">
        <div className="flex items-center justify-between text-xs font-semibold text-slate-500 mb-2">
          <span className="flex items-center gap-1.5 text-indigo-700 font-bold">
            <Layers className="w-4 h-4 text-indigo-600" />
            Two-Sided Card Capture
          </span>
          <span className="text-[11px] font-mono text-slate-400">
            {isCollageDone ? "3 / 3 Ready" : isBackDone ? "2 / 3 Both Captured" : isFrontDone ? "1 / 3 Front Done" : "0 / 3 Pending"}
          </span>
        </div>

        {/* 3 Step Badges */}
        <div className="grid grid-cols-3 gap-2">
          {/* Step 1 */}
          <div
            className={`p-2 rounded-xl border text-center transition-all ${
              isFrontDone
                ? "bg-emerald-50 border-emerald-300 text-emerald-800"
                : "bg-indigo-50/70 border-indigo-200 text-indigo-900"
            }`}
          >
            <div className="flex items-center justify-center gap-1 text-[11px] font-bold">
              {isFrontDone ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <span className="w-3.5 h-3.5 rounded-full bg-indigo-600 text-white text-[9px] flex items-center justify-center font-bold">1</span>}
              <span>Front Side</span>
            </div>
            <span className="text-[9px] block mt-0.5 opacity-80 font-medium">
              {isFrontDone ? "Captured" : "Pending"}
            </span>
          </div>

          {/* Step 2 */}
          <div
            className={`p-2 rounded-xl border text-center transition-all ${
              isBackDone
                ? "bg-emerald-50 border-emerald-300 text-emerald-800"
                : isFrontDone
                ? "bg-indigo-50/70 border-indigo-200 text-indigo-900"
                : "bg-slate-50 border-slate-200 text-slate-400"
            }`}
          >
            <div className="flex items-center justify-center gap-1 text-[11px] font-bold">
              {isBackDone ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <span className="w-3.5 h-3.5 rounded-full bg-slate-300 text-slate-700 text-[9px] flex items-center justify-center font-bold">2</span>}
              <span>Back Side</span>
            </div>
            <span className="text-[9px] block mt-0.5 opacity-80 font-medium">
              {isBackDone ? "Captured" : isFrontDone ? "Action Needed" : "Waiting"}
            </span>
          </div>

          {/* Step 3 */}
          <div
            className={`p-2 rounded-xl border text-center transition-all ${
              isCollageDone
                ? "bg-emerald-50 border-emerald-300 text-emerald-800"
                : isFrontDone && isBackDone
                ? "bg-indigo-50/70 border-indigo-200 text-indigo-900"
                : "bg-slate-50 border-slate-200 text-slate-400"
            }`}
          >
            <div className="flex items-center justify-center gap-1 text-[11px] font-bold">
              {isCollageDone ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <span className="w-3.5 h-3.5 rounded-full bg-slate-300 text-slate-700 text-[9px] flex items-center justify-center font-bold">3</span>}
              <span>Collage</span>
            </div>
            <span className="text-[9px] block mt-0.5 opacity-80 font-medium">
              {isCollageDone ? "Ready" : isFrontDone && isBackDone ? "Generate" : "Waiting"}
            </span>
          </div>
        </div>
      </div>

      {/* Error Alert if Collage Failed */}
      {collageError && (
        <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 flex items-start gap-2.5 text-xs animate-in fade-in">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          <div className="flex-1">
            <span className="font-bold block">Collage Creation Failed</span>
            <span className="text-[11px] text-rose-700">{collageError}</span>
          </div>
          <button
            type="button"
            onClick={handleGenerateCollage}
            className="px-2.5 py-1 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-[10px] font-bold shrink-0"
          >
            Retry
          </button>
        </div>
      )}

      {/* View Mode A: Final Collage Preview (Ready to Submit) */}
      {collageResult ? (
        <div className="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/80 shadow-card space-y-4 animate-in zoom-in-95 duration-200">
          <div className="flex items-center justify-between">
            <div>
              <div className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold mb-1">
                <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                Combined Document Ready
              </div>
              <h3 className="text-base font-bold text-slate-900">Final Collage Preview</h3>
              <p className="text-xs text-slate-500">
                Front and back sides stacked into a single high-resolution image for IRIS OCR.
              </p>
            </div>
            <span className="text-xs font-mono text-slate-400 bg-slate-50 px-2 py-1 rounded-lg border border-slate-200">
              {collageResult.sizeFormatted}
            </span>
          </div>

          {/* Collage Full View */}
          <div className="relative rounded-2xl overflow-hidden bg-slate-900 border border-slate-200 max-h-[480px] flex items-center justify-center p-2">
            <img
              src={collageResult.blobUrl}
              alt="Combined Document Collage"
              className="w-full h-auto max-h-[460px] object-contain rounded-xl"
            />
          </div>

          {/* Actions Bar for Collage */}
          <div className="pt-2 flex flex-col sm:flex-row gap-2.5">
            <button
              type="button"
              onClick={() => {
                setCollageResult(null);
                setBackConfirmed(false);
              }}
              className="flex-1 py-3 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors touch-target-min"
            >
              <RotateCcw className="w-4 h-4 text-slate-500" />
              <span>Retake Back</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setCollageResult(null);
                setFrontConfirmed(false);
              }}
              className="flex-1 py-3 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors touch-target-min"
            >
              <RotateCcw className="w-4 h-4 text-slate-500" />
              <span>Retake Front</span>
            </button>

            <button
              type="button"
              onClick={handleGenerateCollage}
              disabled={isGeneratingCollage}
              className="flex-1 py-3 px-3 rounded-xl bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors touch-target-min"
            >
              <RefreshCw className={`w-4 h-4 ${isGeneratingCollage ? "animate-spin" : ""}`} />
              <span>Recreate Collage</span>
            </button>
          </div>

          {/* Primary Submit Button */}
          <button
            type="button"
            onClick={handleConfirmAndProceed}
            className="w-full py-3.5 px-4 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-bold text-sm rounded-xl shadow-md shadow-indigo-200 flex items-center justify-center gap-2 touch-target-min transition-all"
          >
            <span>Proceed to Document Details & Submit</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      ) : (
        /* View Mode B: Dual Capture & Preview Cards */
        <div className="space-y-4">
          {/* Card 1: Front Side */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/80 shadow-card">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="w-6 h-6 rounded-lg bg-indigo-50 text-indigo-700 font-bold text-xs flex items-center justify-center">
                  1
                </span>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Front Side of Document</h3>
                  <p className="text-[11px] text-slate-500">
                    Includes document photo, name, and primary ID number
                  </p>
                </div>
              </div>
              {frontImage && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  Captured
                </span>
              )}
            </div>

            {frontImage ? (
              /* Front Preview & Controls */
              <div className="space-y-3">
                <div className="relative aspect-[16/10] sm:aspect-[16/9] w-full rounded-xl overflow-hidden bg-slate-900 border border-slate-200 shadow-inner">
                  <img
                    src={frontImage}
                    alt="Front side preview"
                    className="w-full h-full object-contain"
                  />
                  <span className="absolute top-2 left-2 bg-black/60 backdrop-blur-xs text-white text-[10px] font-mono px-2 py-0.5 rounded-md">
                    FRONT
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setFrontImage(null);
                      setFrontConfirmed(false);
                      setCollageResult(null);
                    }}
                    className="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors touch-target-min"
                  >
                    <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
                    <span>Retake Front</span>
                  </button>
                  <span className="text-[11px] text-emerald-600 font-medium">
                    ✓ Front ready for collage
                  </span>
                </div>
              </div>
            ) : (
              /* Front Capture Options */
              <div className="grid grid-cols-2 gap-2.5 pt-1">
                <button
                  type="button"
                  onClick={() => onTriggerCamera("front")}
                  className="py-3 px-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-sm transition-all touch-target-min"
                >
                  <Camera className="w-4 h-4" />
                  <span>Open Camera</span>
                </button>
                <button
                  type="button"
                  onClick={() => frontFileInputRef.current?.click()}
                  className="py-3 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-xs flex items-center justify-center gap-2 transition-all touch-target-min border border-slate-200"
                >
                  <ImageUp className="w-4 h-4 text-slate-500" />
                  <span>From Gallery</span>
                </button>
              </div>
            )}
          </div>

          {/* Card 2: Back Side */}
          <div className="bg-white rounded-2xl p-4 sm:p-5 border border-slate-200/80 shadow-card">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <span className="w-6 h-6 rounded-lg bg-indigo-50 text-indigo-700 font-bold text-xs flex items-center justify-center">
                  2
                </span>
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Back Side of Document</h3>
                  <p className="text-[11px] text-slate-500">
                    Includes address, issue details, or secondary barcodes
                  </p>
                </div>
              </div>
              {backImage && (
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                  <CheckCircle2 className="w-3 h-3 text-emerald-600" />
                  Captured
                </span>
              )}
            </div>

            {backImage ? (
              /* Back Preview & Controls */
              <div className="space-y-3">
                <div className="relative aspect-[16/10] sm:aspect-[16/9] w-full rounded-xl overflow-hidden bg-slate-900 border border-slate-200 shadow-inner">
                  <img
                    src={backImage}
                    alt="Back side preview"
                    className="w-full h-full object-contain"
                  />
                  <span className="absolute top-2 left-2 bg-black/60 backdrop-blur-xs text-white text-[10px] font-mono px-2 py-0.5 rounded-md">
                    BACK
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => {
                      setBackImage(null);
                      setBackConfirmed(false);
                      setCollageResult(null);
                    }}
                    className="py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors touch-target-min"
                  >
                    <RotateCcw className="w-3.5 h-3.5 text-slate-500" />
                    <span>Retake Back</span>
                  </button>
                  <span className="text-[11px] text-emerald-600 font-medium">
                    ✓ Back ready for collage
                  </span>
                </div>
              </div>
            ) : (
              /* Back Capture Options */
              <div className="grid grid-cols-2 gap-2.5 pt-1">
                <button
                  type="button"
                  onClick={() => onTriggerCamera("back")}
                  disabled={!frontImage}
                  className={`py-3 px-3 rounded-xl font-bold text-xs flex items-center justify-center gap-2 transition-all touch-target-min ${
                    frontImage
                      ? "bg-indigo-600 hover:bg-indigo-700 text-white shadow-sm active:scale-95"
                      : "bg-slate-100 text-slate-400 cursor-not-allowed"
                  }`}
                >
                  <Camera className="w-4 h-4" />
                  <span>Open Camera</span>
                </button>
                <button
                  type="button"
                  onClick={() => backFileInputRef.current?.click()}
                  disabled={!frontImage}
                  className={`py-3 px-3 rounded-xl font-semibold text-xs flex items-center justify-center gap-2 transition-all touch-target-min border ${
                    frontImage
                      ? "bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200"
                      : "bg-slate-50 text-slate-300 border-slate-100 cursor-not-allowed"
                  }`}
                >
                  <ImageUp className="w-4 h-4" />
                  <span>From Gallery</span>
                </button>
              </div>
            )}
          </div>

          {/* Action to Create Collage when both are present */}
          {frontImage && backImage && (
            <div className="p-4 bg-gradient-to-r from-indigo-50 to-purple-50 rounded-2xl border border-indigo-200/80 shadow-sm animate-in slide-in-from-bottom-2">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-indigo-600" />
                  <span className="text-xs font-bold text-indigo-950">
                    Both sides captured successfully!
                  </span>
                </div>
                <span className="text-[11px] text-indigo-700 font-semibold">Step 3 of 3</span>
              </div>

              <button
                type="button"
                onClick={handleGenerateCollage}
                disabled={isGeneratingCollage}
                className="w-full py-3.5 px-4 bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 text-white font-bold text-xs sm:text-sm rounded-xl shadow-md shadow-indigo-200 flex items-center justify-center gap-2 touch-target-min transition-all"
              >
                {isGeneratingCollage ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Stitching Front & Back Collage...</span>
                  </>
                ) : (
                  <>
                    <Layers className="w-4 h-4" />
                    <span>Create Combined Collage Image</span>
                    <ArrowRight className="w-4 h-4 ml-1" />
                  </>
                )}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
