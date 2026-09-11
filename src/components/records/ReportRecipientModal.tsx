"use client";

import React, { useState, useEffect, useRef, useMemo } from "react";
import {
  X,
  Mail,
  Users,
  Send,
  Calendar,
  FileSpreadsheet,
  AlertCircle,
  CheckCircle2,
  Plus,
  Loader2,
  Shield,
  User,
} from "lucide-react";
import { ManagedUser } from "@/types";
import { apiService } from "@/services/apiService";

interface ReportRecipientModalProps {
  isOpen: boolean;
  onClose: () => void;
  reportType: "today" | "all";
  onSendReport: (to: string[], cc: string[]) => Promise<void>;
  isSending: boolean;
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const ReportRecipientModal: React.FC<ReportRecipientModalProps> = ({
  isOpen,
  onClose,
  reportType,
  onSendReport,
  isSending,
}) => {
  const [toEmails, setToEmails] = useState<string[]>([]);
  const [ccEmails, setCcEmails] = useState<string[]>([]);
  const [toInput, setToInput] = useState("");
  const [ccInput, setCcInput] = useState("");
  const [availableUsers, setAvailableUsers] = useState<ManagedUser[]>([]);
  const [isLoadingUsers, setIsLoadingUsers] = useState(false);

  const [toDropdownOpen, setToDropdownOpen] = useState(false);
  const [ccDropdownOpen, setCcDropdownOpen] = useState(false);
  const [errorNotice, setErrorNotice] = useState<string | null>(null);

  const toInputRef = useRef<HTMLInputElement>(null);
  const ccInputRef = useRef<HTMLInputElement>(null);
  const toContainerRef = useRef<HTMLDivElement>(null);
  const ccContainerRef = useRef<HTMLDivElement>(null);

  // Fetch available users when modal opens
  useEffect(() => {
    if (isOpen) {
      setErrorNotice(null);
      setIsLoadingUsers(true);
      apiService
        .getAdminUsers()
        .then((users) => {
          setAvailableUsers(users || []);
        })
        .catch((err) => {
          console.warn("[ReportRecipientModal] Could not fetch user directory:", err);
        })
        .finally(() => {
          setIsLoadingUsers(false);
        });
    } else {
      setToEmails([]);
      setCcEmails([]);
      setToInput("");
      setCcInput("");
      setErrorNotice(null);
    }
  }, [isOpen]);

  // Click outside to close dropdowns
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (toContainerRef.current && !toContainerRef.current.contains(e.target as Node)) {
        setToDropdownOpen(false);
      }
      if (ccContainerRef.current && !ccContainerRef.current.contains(e.target as Node)) {
        setCcDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Filter autocomplete suggestions for "To" field
  const toSuggestions = useMemo(() => {
    const query = toInput.trim().toLowerCase();
    if (!query) return availableUsers.slice(0, 8);
    return availableUsers
      .filter(
        (u) =>
          (u.name && u.name.toLowerCase().includes(query)) ||
          (u.email && u.email.toLowerCase().includes(query))
      )
      .slice(0, 8);
  }, [toInput, availableUsers]);

  // Filter autocomplete suggestions for "CC" field
  const ccSuggestions = useMemo(() => {
    const query = ccInput.trim().toLowerCase();
    if (!query) return availableUsers.slice(0, 8);
    return availableUsers
      .filter(
        (u) =>
          (u.name && u.name.toLowerCase().includes(query)) ||
          (u.email && u.email.toLowerCase().includes(query))
      )
      .slice(0, 8);
  }, [ccInput, availableUsers]);

  // Add email to TO list
  const handleAddToEmail = (emailToAdd: string) => {
    const trimmed = emailToAdd.trim().toLowerCase();
    if (!trimmed) return;

    if (!EMAIL_REGEX.test(trimmed)) {
      setErrorNotice(`"${trimmed}" is not a valid email format.`);
      return;
    }

    if (toEmails.includes(trimmed)) {
      setErrorNotice(`"${trimmed}" is already added to the To list.`);
      return;
    }

    // Check CC overlap
    if (ccEmails.includes(trimmed)) {
      setCcEmails((prev) => prev.filter((e) => e !== trimmed));
    }

    setErrorNotice(null);
    setToEmails((prev) => [...prev, trimmed]);
    setToInput("");
    setToDropdownOpen(false);
    toInputRef.current?.focus();
  };

  // Remove email from TO list
  const handleRemoveToEmail = (emailToRemove: string) => {
    setToEmails((prev) => prev.filter((e) => e !== emailToRemove));
  };

  // Add email to CC list
  const handleAddCcEmail = (emailToAdd: string) => {
    const trimmed = emailToAdd.trim().toLowerCase();
    if (!trimmed) return;

    if (!EMAIL_REGEX.test(trimmed)) {
      setErrorNotice(`"${trimmed}" is not a valid email format.`);
      return;
    }

    if (toEmails.includes(trimmed)) {
      setErrorNotice(`"${trimmed}" is already in the To recipients list.`);
      return;
    }

    if (ccEmails.includes(trimmed)) {
      setErrorNotice(`"${trimmed}" is already added to the CC list.`);
      return;
    }

    setErrorNotice(null);
    setCcEmails((prev) => [...prev, trimmed]);
    setCcInput("");
    setCcDropdownOpen(false);
    ccInputRef.current?.focus();
  };

  // Remove email from CC list
  const handleRemoveCcEmail = (emailToRemove: string) => {
    setCcEmails((prev) => prev.filter((e) => e !== emailToRemove));
  };

  // Handle key down on TO input
  const handleToKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      if (toInput.trim()) {
        handleAddToEmail(toInput);
      }
    } else if (e.key === "Backspace" && !toInput && toEmails.length > 0) {
      handleRemoveToEmail(toEmails[toEmails.length - 1]);
    }
  };

  // Handle key down on CC input
  const handleCcKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      if (ccInput.trim()) {
        handleAddCcEmail(ccInput);
      }
    } else if (e.key === "Backspace" && !ccInput && ccEmails.length > 0) {
      handleRemoveCcEmail(ccEmails[ccEmails.length - 1]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (toEmails.length === 0) {
      if (toInput.trim() && EMAIL_REGEX.test(toInput.trim().toLowerCase())) {
        const email = toInput.trim().toLowerCase();
        setToEmails([email]);
        await onSendReport([email], ccEmails);
        return;
      }
      setErrorNotice("Please add at least one recipient in the 'To' field.");
      toInputRef.current?.focus();
      return;
    }

    await onSendReport(toEmails, ccEmails);
  };

  if (!isOpen) return null;

  const isToday = reportType === "today";

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-2xl shadow-2xl border border-slate-200/80 w-full max-w-xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="bg-gradient-to-r from-purple-700 via-indigo-700 to-indigo-800 p-5 text-white relative">
          <button
            type="button"
            onClick={onClose}
            disabled={isSending}
            className="absolute top-4 right-4 p-1.5 rounded-full text-white/80 hover:text-white hover:bg-white/20 transition-all cursor-pointer disabled:opacity-50"
            title="Close"
          >
            <X className="w-5 h-5" />
          </button>

          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white/20 backdrop-blur-md flex items-center justify-center shrink-0 border border-white/20">
              {isToday ? (
                <Calendar className="w-5 h-5 text-purple-200" />
              ) : (
                <FileSpreadsheet className="w-5 h-5 text-emerald-300" />
              )}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="font-bold text-lg text-white tracking-tight">
                  {isToday ? "Send Today’s Report" : "Send Consolidated Report"}
                </h3>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-white/20 backdrop-blur-sm text-purple-100 border border-white/20">
                  {isToday ? "Today's Uploads" : "All Records"}
                </span>
              </div>
              <p className="text-xs text-purple-100/90 mt-0.5">
                {isToday
                  ? "Generate Excel spreadsheet of today's records and email summary to recipients."
                  : "Generate Excel spreadsheet of all completed OCR records across the platform."}
              </p>
            </div>
          </div>
        </div>

        {/* Error Notice */}
        {errorNotice && (
          <div className="px-5 pt-4">
            <div className="flex items-center gap-2 p-3 bg-red-50 text-red-700 rounded-xl text-xs border border-red-200 font-medium">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
              <span className="flex-1">{errorNotice}</span>
              <button
                type="button"
                onClick={() => setErrorNotice(null)}
                className="text-red-400 hover:text-red-700"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        )}

        {/* Form Body */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 overflow-y-auto flex-1">
          {/* TO Field (Mandatory) */}
          <div className="space-y-1.5" ref={toContainerRef}>
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                <Mail className="w-3.5 h-3.5 text-purple-600" />
                <span>To Recipients</span>
                <span className="text-red-500 font-bold">*</span>
              </label>
              <span className="text-[11px] text-slate-500 font-medium">
                {toEmails.length} {toEmails.length === 1 ? "recipient" : "recipients"}
              </span>
            </div>

            {/* Chips & Input Container */}
            <div
              className={`min-h-[46px] p-2 bg-slate-50 rounded-xl border transition-all flex flex-wrap items-center gap-1.5 focus-within:bg-white focus-within:ring-2 focus-within:ring-purple-500/20 ${
                toEmails.length === 0 && !toInput
                  ? "border-slate-300"
                  : "border-purple-300"
              }`}
              onClick={() => toInputRef.current?.focus()}
            >
              {toEmails.map((email) => (
                <span
                  key={email}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-purple-100 text-purple-800 border border-purple-200/80 animate-scale-in"
                >
                  <Mail className="w-3 h-3 text-purple-600 shrink-0" />
                  <span className="max-w-[200px] truncate">{email}</span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRemoveToEmail(email);
                    }}
                    disabled={isSending}
                    className="p-0.5 rounded-full hover:bg-purple-200 text-purple-700 transition-colors"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </span>
              ))}

              <input
                ref={toInputRef}
                type="text"
                value={toInput}
                onChange={(e) => {
                  setToInput(e.target.value);
                  setToDropdownOpen(true);
                  setErrorNotice(null);
                }}
                onFocus={() => setToDropdownOpen(true)}
                onKeyDown={handleToKeyDown}
                disabled={isSending}
                placeholder={
                  toEmails.length === 0
                    ? "Type user name, email, or enter manual email..."
                    : "Add another email..."
                }
                className="flex-1 min-w-[140px] bg-transparent text-xs text-slate-800 placeholder-slate-400 outline-none border-none p-1"
              />

              {toInput.trim() && (
                <button
                  type="button"
                  onClick={() => handleAddToEmail(toInput)}
                  className="px-2 py-1 rounded-md bg-purple-600 text-white text-[11px] font-bold hover:bg-purple-700 transition-colors shrink-0 cursor-pointer"
                >
                  Add
                </button>
              )}
            </div>

            {/* Autocomplete Dropdown for TO */}
            {toDropdownOpen && (toSuggestions.length > 0 || toInput.trim()) && (
              <div className="relative z-30">
                <div className="absolute top-1 left-0 right-0 bg-white rounded-xl shadow-xl border border-slate-200/90 overflow-hidden max-h-52 overflow-y-auto divide-y divide-slate-100 animate-slide-down">
                  {toInput.trim() &&
                    EMAIL_REGEX.test(toInput.trim().toLowerCase()) &&
                    !toSuggestions.some(
                      (u) => u.email.toLowerCase() === toInput.trim().toLowerCase()
                    ) && (
                      <button
                        type="button"
                        onClick={() => handleAddToEmail(toInput)}
                        className="w-full text-left px-3 py-2 hover:bg-purple-50 flex items-center gap-2 text-xs font-semibold text-purple-700 transition-colors"
                      >
                        <Plus className="w-3.5 h-3.5 text-purple-600 shrink-0" />
                        <span>Add external email: </span>
                        <span className="font-mono text-slate-800 underline">
                          {toInput.trim().toLowerCase()}
                        </span>
                      </button>
                    )}

                  {toSuggestions.map((user) => {
                    const isSelected = toEmails.includes(user.email.toLowerCase());
                    const isCc = ccEmails.includes(user.email.toLowerCase());
                    return (
                      <button
                        key={user.id || user.email}
                        type="button"
                        onClick={() => handleAddToEmail(user.email)}
                        disabled={isSelected}
                        className={`w-full text-left px-3 py-2 flex items-center justify-between gap-2 text-xs transition-colors ${
                          isSelected
                            ? "bg-slate-50 text-slate-400 cursor-not-allowed"
                            : "hover:bg-purple-50 text-slate-800"
                        }`}
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          <div className="w-6 h-6 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center shrink-0 text-[10px] font-bold">
                            {user.name ? user.name.charAt(0).toUpperCase() : "U"}
                          </div>
                          <div className="min-w-0">
                            <p className="font-semibold text-slate-900 truncate">
                              {user.name || "User"}
                            </p>
                            <p className="text-[11px] text-slate-500 font-mono truncate">
                              {user.email}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          {user.role && (
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 font-medium">
                              {user.role}
                            </span>
                          )}
                          {isSelected && (
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          )}
                          {isCc && (
                            <span className="text-[10px] text-amber-600 font-medium">
                              (in CC)
                            </span>
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* CC Field (Optional) */}
          <div className="space-y-1.5" ref={ccContainerRef}>
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center gap-1.5">
                <Users className="w-3.5 h-3.5 text-indigo-600" />
                <span>CC Recipients</span>
                <span className="text-slate-400 font-normal text-[11px] lowercase">
                  (optional)
                </span>
              </label>
              <span className="text-[11px] text-slate-500 font-medium">
                {ccEmails.length} {ccEmails.length === 1 ? "recipient" : "recipients"}
              </span>
            </div>

            {/* CC Chips & Input Container */}
            <div
              className="min-h-[46px] p-2 bg-slate-50 rounded-xl border border-slate-300 transition-all flex flex-wrap items-center gap-1.5 focus-within:bg-white focus-within:ring-2 focus-within:ring-indigo-500/20 focus-within:border-indigo-300"
              onClick={() => ccInputRef.current?.focus()}
            >
              {ccEmails.map((email) => (
                <span
                  key={email}
                  className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold bg-indigo-100 text-indigo-800 border border-indigo-200/80 animate-scale-in"
                >
                  <Mail className="w-3 h-3 text-indigo-600 shrink-0" />
                  <span className="max-w-[200px] truncate">{email}</span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleRemoveCcEmail(email);
                    }}
                    disabled={isSending}
                    className="p-0.5 rounded-full hover:bg-indigo-200 text-indigo-700 transition-colors"
                  >
                    <X className="w-3 h-3" />
                  </button>
                </span>
              ))}

              <input
                ref={ccInputRef}
                type="text"
                value={ccInput}
                onChange={(e) => {
                  setCcInput(e.target.value);
                  setCcDropdownOpen(true);
                  setErrorNotice(null);
                }}
                onFocus={() => setCcDropdownOpen(true)}
                onKeyDown={handleCcKeyDown}
                disabled={isSending}
                placeholder={
                  ccEmails.length === 0
                    ? "Type user name, email, or enter manual CC email..."
                    : "Add another CC email..."
                }
                className="flex-1 min-w-[140px] bg-transparent text-xs text-slate-800 placeholder-slate-400 outline-none border-none p-1"
              />

              {ccInput.trim() && (
                <button
                  type="button"
                  onClick={() => handleAddCcEmail(ccInput)}
                  className="px-2 py-1 rounded-md bg-indigo-600 text-white text-[11px] font-bold hover:bg-indigo-700 transition-colors shrink-0 cursor-pointer"
                >
                  Add
                </button>
              )}
            </div>

            {/* Autocomplete Dropdown for CC */}
            {ccDropdownOpen && (ccSuggestions.length > 0 || ccInput.trim()) && (
              <div className="relative z-20">
                <div className="absolute top-1 left-0 right-0 bg-white rounded-xl shadow-xl border border-slate-200/90 overflow-hidden max-h-52 overflow-y-auto divide-y divide-slate-100 animate-slide-down">
                  {ccInput.trim() &&
                    EMAIL_REGEX.test(ccInput.trim().toLowerCase()) &&
                    !ccSuggestions.some(
                      (u) => u.email.toLowerCase() === ccInput.trim().toLowerCase()
                    ) && (
                      <button
                        type="button"
                        onClick={() => handleAddCcEmail(ccInput)}
                        className="w-full text-left px-3 py-2 hover:bg-indigo-50 flex items-center gap-2 text-xs font-semibold text-indigo-700 transition-colors"
                      >
                        <Plus className="w-3.5 h-3.5 text-indigo-600 shrink-0" />
                        <span>Add external CC email: </span>
                        <span className="font-mono text-slate-800 underline">
                          {ccInput.trim().toLowerCase()}
                        </span>
                      </button>
                    )}

                  {ccSuggestions.map((user) => {
                    const isTo = toEmails.includes(user.email.toLowerCase());
                    const isSelected = ccEmails.includes(user.email.toLowerCase());
                    return (
                      <button
                        key={user.id || user.email}
                        type="button"
                        onClick={() => handleAddCcEmail(user.email)}
                        disabled={isSelected || isTo}
                        className={`w-full text-left px-3 py-2 flex items-center justify-between gap-2 text-xs transition-colors ${
                          isSelected || isTo
                            ? "bg-slate-50 text-slate-400 cursor-not-allowed"
                            : "hover:bg-indigo-50 text-slate-800"
                        }`}
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          <div className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center shrink-0 text-[10px] font-bold">
                            {user.name ? user.name.charAt(0).toUpperCase() : "U"}
                          </div>
                          <div className="min-w-0">
                            <p className="font-semibold text-slate-900 truncate">
                              {user.name || "User"}
                            </p>
                            <p className="text-[11px] text-slate-500 font-mono truncate">
                              {user.email}
                            </p>
                          </div>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          {user.role && (
                            <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 font-medium">
                              {user.role}
                            </span>
                          )}
                          {isTo && (
                            <span className="text-[10px] text-purple-600 font-medium">
                              (in To)
                            </span>
                          )}
                          {isSelected && (
                            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>
            )}
          </div>

          {/* Helper hint */}
          <div className="p-3 bg-purple-50/70 rounded-xl border border-purple-100 text-[11px] text-purple-900 flex items-start gap-2">
            <Shield className="w-4 h-4 text-purple-600 shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              Press <span className="font-bold">Enter</span> or{" "}
              <span className="font-bold">Comma</span> after typing an email to add it as a recipient chip. Selected recipients will receive the generated Excel report attachment.
            </p>
          </div>
        </form>

        {/* Footer */}
        <div className="p-4 bg-slate-50 border-t border-slate-200/80 flex items-center justify-between gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={isSending}
            className="px-4 py-2 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-200/70 transition-colors cursor-pointer disabled:opacity-50"
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={handleSubmit}
            disabled={
              isSending ||
              (toEmails.length === 0 &&
                (!toInput.trim() || !EMAIL_REGEX.test(toInput.trim().toLowerCase())))
            }
            className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-indigo-600 hover:from-purple-700 hover:to-indigo-700 text-white text-xs font-bold shadow-md shadow-purple-500/20 hover:shadow-lg hover:shadow-purple-500/30 transition-all flex items-center gap-2 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer"
          >
            {isSending ? (
              <>
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
                <span>Generating & Dispatching...</span>
              </>
            ) : (
              <>
                <Send className="w-3.5 h-3.5" />
                <span>Send {isToday ? "Today’s Report" : "Consolidated Report"}</span>
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
