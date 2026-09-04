"use client";

import React, { useState, useEffect, useRef, useCallback } from "react";
import { User, UploadRecord } from "@/types";
import { mockAuthService } from "@/services/mockAuthService";
import { mockUploadService } from "@/services/mockUploadService";
import { syncService } from "@/services/syncService";
import { useNetworkStatus } from "@/hooks/useNetworkStatus";
import { LoginForm } from "@/components/auth/LoginForm";
import { AppHeader } from "@/components/layout/AppHeader";
import { BottomNav } from "@/components/layout/BottomNav";
import { NetworkStatusBar } from "@/components/common/NetworkStatusBar";
import { UploadActionCards } from "@/components/upload/UploadActionCards";
import { UploadForm } from "@/components/upload/UploadForm";
import { UploadSuccessModal } from "@/components/upload/UploadSuccessModal";
import { RecordsDashboard } from "@/components/records/RecordsDashboard";
import { CameraCaptureModal } from "@/components/camera/CameraCaptureModal";
import { ToastContainer, ToastMessage } from "@/components/common/Toast";

export default function Home() {
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [isInitializing, setIsInitializing] = useState(true);

  // Network State
  const { isOnline, isSimulatedOffline, toggleSimulation } = useNetworkStatus();

  // Navigation & Flow State
  const [activeTab, setActiveTab] = useState<"upload" | "records">("upload");
  const [capturedImage, setCapturedImage] = useState<string | null>(null);
  const [isCameraOpen, setIsCameraOpen] = useState(false);
  const [successRecord, setSuccessRecord] = useState<UploadRecord | null>(null);
  const [isSuccessOffline, setIsSuccessOffline] = useState(false);

  // Data Store (Merged uploaded & offline queue)
  const [records, setRecords] = useState<UploadRecord[]>([]);
  const [isSyncing, setIsSyncing] = useState(false);
  const [syncProgress, setSyncProgress] = useState({ current: 0, total: 0, message: "" });

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

  const refreshRecords = useCallback(async () => {
    const merged = await mockUploadService.getAllRecordsMerged();
    setRecords(merged);
  }, []);

  // Initialize Auth & Records from LocalStorage and IndexedDB
  useEffect(() => {
    const user = mockAuthService.getCurrentUser();
    if (user) {
      setCurrentUser(user);
    }
    refreshRecords();
    setIsInitializing(false);

    // Initialize auto sync on network recovery
    syncService.initAutoSync();

    // Subscribe to sync progress events
    const unsubProgress = syncService.subscribe((state) => {
      setIsSyncing(state.isSyncing);
      setSyncProgress({ current: state.current, total: state.total, message: state.message });
      if (!state.isSyncing && state.total > 0) {
        refreshRecords();
        if (state.current > 0) {
          addToast("success", "Sync Completed", state.message);
        }
      }
    });

    // Subscribe to individual record sync completion
    const unsubRecord = syncService.onRecordSynced((syncedRecord) => {
      refreshRecords();
      addToast("success", `Uploaded ${syncedRecord.id}`, "Synchronized to cloud backend");
    });

    return () => {
      unsubProgress();
      unsubRecord();
    };
  }, [refreshRecords]);

  // Track pending offline uploads count
  const pendingCount = records.filter(
    (r) => r.status === "Pending Upload" || r.status === "Failed" || r.status === "Uploading"
  ).length;

  const handleLoginSuccess = (user: User) => {
    setCurrentUser(user);
    addToast("success", `Welcome back, ${user.name}!`, `Signed in as ${user.role}`);
  };

  const handleLogout = () => {
    mockAuthService.logout();
    setCurrentUser(null);
    setCapturedImage(null);
    setSuccessRecord(null);
    setActiveTab("upload");
    addToast("info", "Logged out successfully");
  };

  // Triggered when photo is snapped in live camera
  const handleCameraCapture = (dataUrl: string) => {
    setIsCameraOpen(false);
    setCapturedImage(dataUrl);
    addToast("info", "Photo captured", "Review details before submitting");
  };

  // Triggered when image is picked from gallery
  const handleSelectImageFile = (file: File) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      if (e.target?.result) {
        setCapturedImage(e.target.result as string);
        addToast("info", "Image selected", "Review details before submitting");
      }
    };
    reader.readAsDataURL(file);
  };

  // Triggered when upload form submits successfully (online or offline)
  const handleUploadComplete = async (newRecord: UploadRecord, isOfflineSaved: boolean) => {
    await refreshRecords();
    setSuccessRecord(newRecord);
    setIsSuccessOffline(isOfflineSaved);
    setCapturedImage(null);

    if (isOfflineSaved) {
      addToast(
        "info",
        `Record ${newRecord.id} Saved Locally`,
        "Will be uploaded automatically when connection is restored."
      );
    } else {
      addToast("success", `Record ${newRecord.id} Uploaded`, "Data stored in local records");
    }
  };

  const handleTriggerSync = async () => {
    if (!isOnline) {
      addToast("error", "Cannot Sync Offline", "Please restore internet connection to upload.");
      return;
    }
    addToast("info", "Sync Started", "Uploading pending records to server...");
    await syncService.syncPendingUploads();
    await refreshRecords();
  };

  const handleSyncSingle = async (id: string) => {
    if (!isOnline) {
      addToast("error", "Offline", "Please restore connection before syncing.");
      return;
    }
    const success = await syncService.syncSingleItem(id);
    await refreshRecords();
    if (success) {
      addToast("success", `Synced ${id}`, "Successfully uploaded to backend.");
    } else {
      addToast("error", `Sync Failed for ${id}`, "Could not upload image right now.");
    }
  };

  const handleViewRecordsFromSuccess = () => {
    setSuccessRecord(null);
    setActiveTab("records");
  };

  const handleUploadAnother = () => {
    setSuccessRecord(null);
    setCapturedImage(null);
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

      {/* Network Status Banner & Offline Simulation Switch */}
      <NetworkStatusBar
        isOnline={isOnline}
        isSimulated={isSimulatedOffline}
        onToggleSimulation={toggleSimulation}
        pendingCount={pendingCount}
      />

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
        isOnline={isOnline}
        pendingCount={pendingCount}
        onLogout={handleLogout}
        activeTab={activeTab}
        onTabChange={(tab) => {
          setActiveTab(tab);
          setCapturedImage(null);
        }}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 pt-5 pb-16">
        {activeTab === "upload" ? (
          capturedImage ? (
            /* Upload Review & User Metadata Form */
            <UploadForm
              user={currentUser}
              imagePreviewUrl={capturedImage}
              onCancel={() => setCapturedImage(null)}
              onRetake={() => {
                setCapturedImage(null);
                setIsCameraOpen(true);
              }}
              onUploadSuccess={handleUploadComplete}
            />
          ) : (
            /* Main Capture / Upload Action Screen */
            <div className="max-w-xl mx-auto space-y-6 pt-2 pb-24 md:pb-12 animate-in fade-in">
              {/* Two Prominent Action Cards: Open Camera & Upload from Gallery */}
              <UploadActionCards
                onOpenCamera={() => setIsCameraOpen(true)}
                onSelectImage={handleSelectImageFile}
                onDirectCameraInput={handleSelectImageFile}
              />
            </div>
          )
        ) : (
          /* Records Dashboard View */
          <RecordsDashboard
            currentUser={currentUser}
            records={records}
            isOnline={isOnline}
            isSyncing={isSyncing}
            syncProgress={syncProgress}
            onTriggerSync={handleTriggerSync}
            onSyncSingle={handleSyncSingle}
            onNavigateToUpload={() => {
              setActiveTab("upload");
              setCapturedImage(null);
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
          isOfflineSaved={isSuccessOffline}
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
        }}
        recordCount={records.length}
      />
    </div>
  );
}
