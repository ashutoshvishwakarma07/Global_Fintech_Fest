"use client";

import React, { useState, useEffect, useRef, useCallback } from "react";
import { User, UploadRecord, CaptureMode } from "@/types";
import { authService } from "@/services/authService";
import { mockUploadService } from "@/services/mockUploadService";
import { LoginForm } from "@/components/auth/LoginForm";
import { AppHeader } from "@/components/layout/AppHeader";
import { BottomNav } from "@/components/layout/BottomNav";
import { UploadActionCards } from "@/components/upload/UploadActionCards";
import { UploadForm } from "@/components/upload/UploadForm";
import { UploadSuccessModal } from "@/components/upload/UploadSuccessModal";
import { RecordsDashboard } from "@/components/records/RecordsDashboard";
import { CameraCaptureModal } from "@/components/camera/CameraCaptureModal";
import { ToastContainer, ToastMessage } from "@/components/common/Toast";
import { CollageResult } from "@/services/collageService";

export default function Home() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  // Navigation & Flow State
  const [activeTab, setActiveTab] = useState<"upload" | "records">("upload");
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [captureMode, setCaptureMode] = useState<CaptureMode>("single");
  const [twoSideData, setTwoSideData] = useState<{
    frontUrl: string;
    backUrl: string;
  } | null>(null);

  // Camera State
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const [cameraTarget, setCameraTarget] = useState<"single" | "front" | "back">("single");
  const [twoSideCameraImage, setTwoSideCameraImage] = useState<{
    target: "front" | "back";
    dataUrl: string;
  } | null>(null);

  const [successRecord, setSuccessRecord] = useState<UploadRecord | null>(null);

  // Role-filtered Records Store
  const [records, setRecords] = useState<UploadRecord[]>([]);

  // Notifications
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const nativeInputRef = useRef<HTMLInputElement | null>(null);

  const addToast = (type: "success" | "error" | "info", title: string, message?: string) => {
    const id = Date.now().toString() + Math.random().toString(36).substring(2, 5);
    setToasts((prev) => [...prev, { id, type, title, message }]);
  };

  const removeToast = (id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const refreshRecords = useCallback((targetUser?: User | null) => {
    const userToQuery = targetUser !== undefined ? targetUser : currentUser;
    if (!userToQuery) {
      setRecords([]);
      return;
    }
    const userRecords = mockUploadService.getRecordsForUser(userToQuery);
    setRecords(userRecords);
  }, [currentUser]);

  // Initialize Auth & Records on mount
  useEffect(() => {
    let isMounted = true;
    async function initSession() {
      try {
        const user = await authService.getMe();
        if (isMounted && user) {
          setCurrentUser(user);
          const userRecords = mockUploadService.getRecordsForUser(user);
          setRecords(userRecords);
        }
      } catch {
        // User not authenticated
      } finally {
        if (isMounted) setIsInitializing(false);
      }
    }
    initSession();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleLoginSuccess = (user: User) => {
    setCurrentUser(user);
    const userRecords = mockUploadService.getRecordsForUser(user);
    setRecords(userRecords);
    addToast("success", `Welcome back, ${user.name}!`, `Signed in as ${user.role}`);
  };

  const handleLogout = async () => {
    await authService.logout();
    setCurrentUser(null);
    setRecords([]);
    setCapturedImage(null);
    setTwoSideData(null);
    setCaptureMode("single");
    setSuccessRecord(null);
    setActiveTab("upload");
    addToast("info", "Logged out successfully");
  };

  // Open camera with specific target (single, front, or back)
  const handleOpenCamera = (target: "single" | "front" | "back" = "single") => {
    setCameraTarget(target);
    setIsCameraOpen(true);
  };

  // Triggered when photo is snapped in live camera
  const handleCameraCapture = (dataUrl: string) => {
    setIsCameraOpen(false);
    if (cameraTarget === "single") {
      setCaptureMode("single");
      setTwoSideData(null);
      setCapturedImage(dataUrl);
      addToast("info", "Photo captured", "Select document type and verify details");
    } else {
      setTwoSideCameraImage({ target: cameraTarget, dataUrl });
      addToast(
        "info",
        `${cameraTarget === "front" ? "Front" : "Back"} Side Captured`,
        "Ready to compose two-sided document"
      );
    }
  };

  // Triggered when image is picked from gallery in single mode
  const handleSelectImageFile = (file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      if (e.target?.result) {
        setCaptureMode("single");
        setTwoSideData(null);
        setCapturedImage(e.target.result as string);
        addToast("info", "Image selected", "Select document type and verify details");
      }
    };
    reader.readAsDataURL(file);
  };

  // Triggered when TwoSideCapture produces a combined collage
  const handleTwoSideCollageReady = (
    collage: CollageResult,
    frontUrl: string,
    backUrl: string
  ) => {
    setCaptureMode("two-sided");
    setTwoSideData({ frontUrl, backUrl });
    setCapturedImage(collage.blobUrl);
    addToast(
      "success",
      "Two-Sided Collage Generated",
      "Review document details and submit for IRIS OCR"
    );
  };

  // Triggered when upload form submits successfully (online IRIS API completed)
  const handleUploadComplete = (newRecord: UploadRecord) => {
    if (currentUser) {
      const updated = mockUploadService.getRecordsForUser(currentUser);
      setRecords(updated);
    }
    setSuccessRecord(newRecord);
    setCapturedImage(null);
    setTwoSideData(null);
    setCaptureMode("single");

    const confVal = newRecord.extractedData?.confidence ?? 98.8;
    const confPct = Math.round(confVal <= 1 ? confVal * 100 : confVal);
    addToast(
      "success",
      `IRIS OCR Extracted: ${newRecord.extractedData?.documentType || "Document"}`,
      `Record ${newRecord.id} saved with ${confPct}% confidence`
    );
  };

  const handleViewRecordsFromSuccess = () => {
    setSuccessRecord(null);
    setActiveTab("records");
  };

  const handleUploadAnother = () => {
    setSuccessRecord(null);
    setCapturedImage(null);
    setTwoSideData(null);
    setCaptureMode("single");
    setActiveTab("upload");
  };

  if (isInitializing) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-3 border-indigo-600/30 border-t-indigo-600 rounded-full animate-spin" />
          <span className="text-xs font-semibold text-slate-500">Loading portal...</span>
        </div>
      </div>
    );
  }

  // Not logged in -> Show clean mobile-first login screen
  if (!currentUser) {
    return (
      <>
        <ToastContainer toasts={toasts} onDismiss={removeToast} />
        <LoginForm onLoginSuccess={handleLoginSuccess} />
      </>
    );
  }

  // Logged in -> Main Application Flow
  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <ToastContainer toasts={toasts} onDismiss={removeToast} />

      {/* Hidden native camera picker for fallback */}
      <input
        ref={nativeInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) handleSelectImageFile(file);
          e.target.value = "";
        }}
      />

      {/* Sticky Header with User Info & Logout */}
      <AppHeader
        user={currentUser}
        onLogout={handleLogout}
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setCapturedImage(null);
          setTwoSideData(null);
        }}
        recordsCount={records.length}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 pt-5 pb-16">
        {activeTab === "upload" ? (
          capturedImage ? (
            /* Upload Review & User Metadata Form */
            <UploadForm
              user={currentUser}
              imagePreviewUrl={capturedImage}
              captureMode={captureMode}
              frontImageUrl={twoSideData?.frontUrl}
              backImageUrl={twoSideData?.backUrl}
              onCancel={() => {
                setCapturedImage(null);
                setTwoSideData(null);
              }}
              onRetake={() => {
                setCapturedImage(null);
                if (captureMode === "single") {
                  setIsCameraOpen(true);
                }
              }}
              onUploadSuccess={handleUploadComplete}
            />
          ) : (
            /* Main Capture / Upload Action Screen */
            <div className="max-w-xl mx-auto space-y-6 pt-2 pb-24 md:pb-12 animate-in fade-in">
              {/* Action Cards supporting Single Side and Two-Sided Capture */}
              <UploadActionCards
                onOpenCamera={handleOpenCamera}
                onSelectImage={handleSelectImageFile}
                onDirectCameraInput={handleSelectImageFile}
                onTwoSideCollageReady={handleTwoSideCollageReady}
                capturedCameraImage={twoSideCameraImage}
                onCameraImageConsumed={() => setTwoSideCameraImage(null)}
              />
            </div>
          )
        ) : (
          /* Records Dashboard View */
          <RecordsDashboard
            currentUser={currentUser}
            records={records}
            onNavigateToUpload={() => {
              setActiveTab("upload");
              setCapturedImage(null);
              setTwoSideData(null);
            }}
          />
        )}
      </main>

      {/* Live Camera Viewfinder Modal */}
      <CameraCaptureModal
        isOpen={isCameraOpen}
        onClose={() => setIsCameraOpen(false)}
        onCapture={handleCameraCapture}
        onFallbackToFilePicker={() => {
          setIsCameraOpen(false);
          nativeInputRef.current?.click();
        }}
      />

      {/* Upload Success Modal */}
      {successRecord && (
        <UploadSuccessModal
          record={successRecord}
          onGoToRecords={handleViewRecordsFromSuccess}
          onUploadAnother={handleUploadAnother}
        />
      )}

      {/* Mobile Bottom Navigation (Hidden on Tablet/Desktop) */}
      <BottomNav
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setCapturedImage(null);
          setTwoSideData(null);
        }}
        recordCount={records.length}
      />
    </div>
  );
}
