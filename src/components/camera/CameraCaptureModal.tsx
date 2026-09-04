"use client";

import React, { useState, useEffect, useRef } from "react";
import { X, Camera, RefreshCw, AlertCircle, Image as ImageIcon } from "lucide-react";

interface CameraCaptureModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCapture: (imageDataUrl: string) => void;
  onFallbackToFilePicker: () => void;
}

export const CameraCaptureModal: React.FC<CameraCaptureModalProps> = ({
  isOpen,
  onClose,
  onCapture,
  onFallbackToFilePicker,
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const [facingMode, setFacingMode] = useState<"environment" | "user">("environment");
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [isStarting, setIsStarting] = useState(true);
  const [isFlashing, setIsFlashing] = useState(false);

  const stopStream = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((t) => t.stop());
      streamRef.current = null;
    }
  };

  const startCamera = async (facing: "environment" | "user") => {
    stopStream();
    setIsStarting(true);
    setCameraError(null);

    // Verify browser support
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      setCameraError("Your browser or device does not support direct live camera streaming.");
      setIsStarting(false);
      return;
    }

    try {
      const constraints: MediaStreamConstraints = {
        video: {
          facingMode: { ideal: facing },
          width: { ideal: 1920 },
          height: { ideal: 1080 },
        },
        audio: false,
      };

      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      streamRef.current = stream;

      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setIsStarting(false);
    } catch (err: any) {
      console.warn("Camera access failed:", err);
      if (err.name === "NotAllowedError" || err.name === "PermissionDeniedError") {
        setCameraError("Camera permission was denied. Please allow camera access in browser settings or use the native camera picker.");
      } else if (err.name === "NotFoundError" || err.name === "DevicesNotFoundError") {
        setCameraError("No camera hardware detected on this device.");
      } else {
        setCameraError("Unable to initialize camera stream. You can capture using the native device camera instead.");
      }
      setIsStarting(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      startCamera(facingMode);
    } else {
      stopStream();
    }
    return () => {
      stopStream();
    };
  }, [isOpen, facingMode]);

  const toggleFacingMode = () => {
    setFacingMode((prev) => (prev === "environment" ? "user" : "environment"));
  };

  const handleTakePhoto = () => {
    if (!videoRef.current) return;

    // Trigger flash animation
    setIsFlashing(true);
    setTimeout(() => setIsFlashing(false), 200);

    const video = videoRef.current;
    const canvas = document.createElement("canvas");
    canvas.width = video.videoWidth || 1280;
    canvas.height = video.videoHeight || 720;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // If front camera, mirror image
    if (facingMode === "user") {
      ctx.translate(canvas.width, 0);
      ctx.scale(-1, 1);
    }

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const dataUrl = canvas.toDataURL("image/jpeg", 0.85);

    stopStream();
    onCapture(dataUrl);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black flex flex-col justify-between animate-in fade-in duration-200">
      {/* Flash overlay */}
      {isFlashing && <div className="absolute inset-0 bg-white z-50 pointer-events-none transition-opacity duration-150" />}

      {/* Top Controls Bar */}
      <div className="relative z-10 flex items-center justify-between p-4 bg-gradient-to-b from-black/80 via-black/40 to-transparent">
        <button
          type="button"
          onClick={onClose}
          className="w-11 h-11 rounded-full bg-white/20 backdrop-blur-md text-white flex items-center justify-center active:scale-95 transition-all touch-target-min"
          aria-label="Close camera"
        >
          <X className="w-6 h-6" />
        </button>

        <div className="text-white text-xs font-semibold tracking-wide uppercase px-3 py-1 rounded-full bg-white/10 backdrop-blur-md">
          {facingMode === "environment" ? "Rear Camera" : "Front Camera"}
        </div>

        <button
          type="button"
          onClick={toggleFacingMode}
          className="w-11 h-11 rounded-full bg-white/20 backdrop-blur-md text-white flex items-center justify-center active:scale-95 transition-all touch-target-min"
          aria-label="Switch camera"
          title="Switch camera"
        >
          <RefreshCw className="w-5 h-5" />
        </button>
      </div>

      {/* Center Viewport / Live Video */}
      <div className="relative flex-1 flex items-center justify-center overflow-hidden bg-black">
        {cameraError ? (
          <div className="max-w-sm mx-auto p-6 text-center text-white space-y-4">
            <div className="w-16 h-16 rounded-2xl bg-rose-500/20 text-rose-400 mx-auto flex items-center justify-center border border-rose-500/30">
              <AlertCircle className="w-8 h-8" />
            </div>
            <div>
              <h3 className="text-base font-bold">Camera Access Issue</h3>
              <p className="text-xs text-slate-300 mt-1.5 leading-relaxed">{cameraError}</p>
            </div>
            <div className="pt-2 space-y-2">
              <button
                type="button"
                onClick={() => {
                  stopStream();
                  onFallbackToFilePicker();
                }}
                className="w-full py-3 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold flex items-center justify-center gap-2 touch-target-min"
              >
                <Camera className="w-4 h-4" />
                Use Native Device Camera
              </button>
              <button
                type="button"
                onClick={() => startCamera(facingMode)}
                className="w-full py-2.5 px-4 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-medium touch-target-min"
              >
                Retry Live Viewfinder
              </button>
            </div>
          </div>
        ) : (
          <>
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className={`w-full h-full object-cover ${facingMode === "user" ? "scale-x-[-1]" : ""}`}
            />

            {/* Viewfinder Target Grid */}
            <div className="absolute inset-8 sm:inset-16 pointer-events-none border border-white/20 rounded-2xl">
              <div className="absolute top-0 left-0 w-6 h-6 border-t-2 border-l-2 border-white rounded-tl-lg" />
              <div className="absolute top-0 right-0 w-6 h-6 border-t-2 border-r-2 border-white rounded-tr-lg" />
              <div className="absolute bottom-0 left-0 w-6 h-6 border-b-2 border-l-2 border-white rounded-bl-lg" />
              <div className="absolute bottom-0 right-0 w-6 h-6 border-b-2 border-r-2 border-white rounded-br-lg" />
            </div>

            {isStarting && (
              <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/60 text-white gap-2">
                <div className="w-8 h-8 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                <span className="text-xs font-medium">Initializing camera...</span>
              </div>
            )}
          </>
        )}
      </div>

      {/* Bottom Shutter & Controls Bar */}
      <div className="relative z-10 p-6 pb-8 bg-gradient-to-t from-black via-black/80 to-transparent flex items-center justify-around safe-bottom">
        {/* Switch to native file picker button */}
        <button
          type="button"
          onClick={() => {
            stopStream();
            onFallbackToFilePicker();
          }}
          className="flex flex-col items-center gap-1 text-white/80 hover:text-white touch-target-min"
          title="Open Device Gallery"
        >
          <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center">
            <ImageIcon className="w-5 h-5" />
          </div>
          <span className="text-[10px]">Gallery</span>
        </button>

        {/* Big Shutter Button */}
        <button
          type="button"
          disabled={Boolean(cameraError) || isStarting}
          onClick={handleTakePhoto}
          className="w-20 h-20 rounded-full border-4 border-white flex items-center justify-center active:scale-90 transition-transform disabled:opacity-40 disabled:pointer-events-none p-1"
          aria-label="Take picture"
        >
          <div className="w-full h-full rounded-full bg-white hover:bg-slate-200 transition-colors shadow-inner" />
        </button>

        {/* Empty balancing spacer */}
        <div className="w-10" />
      </div>
    </div>
  );
};
