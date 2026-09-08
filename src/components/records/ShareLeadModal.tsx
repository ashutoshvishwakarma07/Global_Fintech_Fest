"use client";

import React, { useState, useEffect } from "react";
import { UploadRecord } from "@/types";
import { apiService } from "@/services/apiService";
import { getDisplayImageUrl } from "@/utils/imageUrl";
import {
  X,
  Send,
  Mail,
  User,
  Building2,
  Briefcase,
  Phone,
  AlertCircle,
  Loader2,
  CheckCircle2,
  Sparkles,
} from "lucide-react";

interface ShareLeadModalProps {
  isOpen: boolean;
  record: UploadRecord | null;
  onClose: () => void;
  onSuccess: (message: string) => void;
}

export const ShareLeadModal: React.FC<ShareLeadModalProps> = ({
  isOpen,
  record,
  onClose,
  onSuccess,
}) => {
  const [leadEmail, setLeadEmail] = useState("");
  const [subject, setSubject] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Extract display values from record
  const cardName =
    record?.cardHolderName ||
    record?.extractedData?.cardHolderName ||
    record?.extractedData?.name ||
    record?.extractedData?.extractedName ||
    record?.uploadedBy ||
    "Lead";

  const company =
    record?.companyName ||
    record?.extractedData?.companyName ||
    "N/A";

  const designation =
    record?.designation ||
    record?.extractedData?.designation ||
    "N/A";

  const phone =
    record?.extractedMobile ||
    record?.extractedData?.extractedMobile ||
    record?.mobile ||
    "N/A";

  const contactEmail =
    record?.extractedEmail ||
    record?.extractedData?.extractedEmail ||
    record?.email ||
    "N/A";

  // Pre-fill default subject whenever a new record is selected
  useEffect(() => {
    if (record) {
      const defaultSubject = `Visiting Card Details - ${cardName}${company !== "N/A" ? ` (${company})` : ""}`;
      setSubject(defaultSubject);
      setLeadEmail("");
      setErrorMessage(null);
    }
  }, [record, cardName, company]);

  if (!isOpen || !record) return null;

  const validateEmail = (email: string): boolean => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const trimmedEmail = leadEmail.trim();
    if (!trimmedEmail) {
      setErrorMessage("Please enter the recipient lead email address.");
      return;
    }

    if (!validateEmail(trimmedEmail)) {
      setErrorMessage("Please enter a valid email address (e.g. lead@company.com).");
      return;
    }

    setIsSubmitting(true);

    try {
      // Use record.id (e.g. REC-1001) or numericId if available
      const identifier = record.id;
      const result = await apiService.shareVisitingCard(identifier, {
        leadEmail: trimmedEmail,
        subject: subject.trim() || undefined,
      });

      onSuccess(`Visiting card for "${cardName}" successfully shared with ${trimmedEmail}!`);
      onClose();
    } catch (err: any) {
      setErrorMessage(err.message || "Failed to share visiting card. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-in fade-in duration-200">
      <div
        className="bg-white w-full max-w-lg rounded-3xl shadow-2xl border border-slate-200 overflow-hidden flex flex-col animate-in zoom-in-95 duration-200"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div className="px-6 py-5 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
              <Mail className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
                Share Visiting Card with Lead
              </h3>
              <p className="text-xs text-slate-500">
                Send verified contact details directly via email
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="w-8 h-8 rounded-full flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors disabled:opacity-50"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5 overflow-y-auto max-h-[80vh]">
          {/* Error Banner */}
          {errorMessage && (
            <div className="flex items-start gap-3 p-3.5 bg-rose-50 border border-rose-200 rounded-xl text-rose-700 text-xs">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              <div className="flex-1 font-medium">{errorMessage}</div>
            </div>
          )}

          {/* Selected Card Preview Box */}
          <div className="bg-slate-50 border border-slate-200/80 rounded-2xl p-4">
            <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider mb-2.5 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5 text-indigo-600" />
              <span>Card Details to be Shared</span>
            </div>

            <div className="flex items-start gap-3.5">
              {/* Thumbnail */}
              <div className="w-16 h-16 rounded-xl overflow-hidden bg-white border border-slate-200 shrink-0">
                <img
                  src={getDisplayImageUrl(record)}
                  alt={cardName}
                  className="w-full h-full object-cover"
                />
              </div>

              {/* Card Meta */}
              <div className="flex-1 min-w-0 space-y-1">
                <div className="font-bold text-slate-900 text-sm truncate flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                  <span className="truncate">{cardName}</span>
                </div>

                <div className="text-xs text-indigo-600 font-semibold flex items-center gap-1.5 truncate">
                  <Building2 className="w-3.5 h-3.5 text-indigo-500 shrink-0" />
                  <span className="truncate">{company}</span>
                  {designation !== "N/A" && (
                    <>
                      <span className="text-slate-300">•</span>
                      <span className="text-slate-500 font-normal truncate">{designation}</span>
                    </>
                  )}
                </div>

                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-[11px] text-slate-500 pt-0.5 font-mono">
                  {phone !== "N/A" && (
                    <span className="flex items-center gap-1">
                      <Phone className="w-3 h-3 text-slate-400" />
                      {phone}
                    </span>
                  )}
                  {contactEmail !== "N/A" && (
                    <span className="flex items-center gap-1">
                      <Mail className="w-3 h-3 text-slate-400" />
                      {contactEmail}
                    </span>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Lead Email Input */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5">
              Lead Email Address <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <input
                type="email"
                required
                value={leadEmail}
                onChange={(e) => setLeadEmail(e.target.value)}
                placeholder="e.g. client@example.com"
                disabled={isSubmitting}
                className="w-full pl-10 pr-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-colors disabled:bg-slate-50 disabled:text-slate-500"
              />
            </div>
            <p className="text-[11px] text-slate-400 mt-1">
              The verified visiting card details and company contact will be emailed to this recipient.
            </p>
          </div>

          {/* Subject Input */}
          <div>
            <label className="block text-xs font-bold text-slate-700 mb-1.5">
              Email Subject
            </label>
            <input
              type="text"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              placeholder="Visiting Card Details"
              disabled={isSubmitting}
              className="w-full px-4 py-2.5 bg-white border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-colors disabled:bg-slate-50 disabled:text-slate-500"
            />
          </div>

          {/* Actions */}
          <div className="pt-2 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 text-xs font-semibold text-slate-600 hover:text-slate-900 bg-slate-100 hover:bg-slate-200 rounded-xl transition-colors disabled:opacity-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !leadEmail.trim()}
              className="inline-flex items-center gap-2 px-5 py-2.5 text-xs font-bold text-white bg-gradient-to-r from-indigo-600 to-blue-600 hover:from-indigo-700 hover:to-blue-700 rounded-xl shadow-md shadow-indigo-200 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Sending Email...</span>
                </>
              ) : (
                <>
                  <Send className="w-4 h-4" />
                  <span>Send Email</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
