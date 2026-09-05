"use client";

import React from "react";
import { UploadRecord } from "@/types";
import { ExternalLink, Shield, CheckCircle2, FileText } from "lucide-react";

interface RecordTableProps {
  records: UploadRecord[];
  onSelect: (record: UploadRecord) => void;
}

export const RecordTable: React.FC<RecordTableProps> = ({ records, onSelect }) => {
  const getStatusBadge = (status: UploadRecord["status"]) => {
    switch (status) {
      case "Verified":
        return {
          pillClass: "bg-emerald-50 text-emerald-800 border-emerald-300",
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          label: "Verified",
        };
      case "Uploaded":
      default:
        return {
          pillClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
          icon: <CheckCircle2 className="w-3 h-3 text-emerald-600" />,
          label: "Uploaded",
        };
    }
  };

  return (
    <div className="w-full bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="bg-slate-50/80 border-b border-slate-200 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
              <th className="py-3.5 px-4">Image</th>
              <th className="py-3.5 px-3">Record ID</th>
              <th className="py-3.5 px-4">Document Details</th>
              <th className="py-3.5 px-4">Uploaded By</th>
              <th className="py-3.5 px-3">Role</th>
              <th className="py-3.5 px-4">Contact</th>
              <th className="py-3.5 px-4">Upload Date / Time</th>
              <th className="py-3.5 px-3">Status</th>
              <th className="py-3.5 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-xs">
            {records.map((record) => {
              const badge = getStatusBadge(record.status);

              return (
                <tr
                  key={record.id}
                  onClick={() => onSelect(record)}
                  className="hover:bg-slate-50/80 cursor-pointer transition-colors"
                >
                  {/* Image */}
                  <td className="py-3 px-4">
                    <div className="w-12 h-12 rounded-lg overflow-hidden bg-slate-100 border border-slate-200 shrink-0">
                      <img
                        src={record.imageUrl}
                        alt={record.id}
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                    </div>
                  </td>

                  {/* Record ID */}
                  <td className="py-3 px-3">
                    <span className="font-mono font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100">
                      {record.id}
                    </span>
                  </td>

                  {/* Document Details (IRIS OCR) */}
                  <td className="py-3 px-4 whitespace-nowrap">
                    {record.extractedData ? (
                      <div>
                        <div className="flex items-center gap-1.5">
                          <span className="inline-flex items-center gap-1 font-semibold text-slate-900 bg-slate-100 px-2 py-0.5 rounded text-[11px]">
                            <FileText className="w-3 h-3 text-indigo-600" />
                            {record.extractedData.documentType}
                          </span>
                          {record.captureMode === "two-sided" && (
                            <span className="text-[9px] font-bold text-purple-700 bg-purple-100 px-1.5 py-0.2 rounded border border-purple-200">
                              2-Sided
                            </span>
                          )}
                        </div>
                        <div className="font-mono text-[11px] text-slate-600 mt-0.5">
                          {record.extractedData.documentNumber}
                        </div>
                      </div>
                    ) : (
                      <span className="text-slate-400 italic">No OCR Data</span>
                    )}
                  </td>

                  {/* Uploaded By */}
                  <td className="py-3 px-4 font-semibold text-slate-900 whitespace-nowrap">
                    {record.uploadedBy}
                  </td>

                  {/* Role */}
                  <td className="py-3 px-3 whitespace-nowrap">
                    <span
                      className={`inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full ${
                        record.role === "Admin"
                          ? "bg-purple-50 text-purple-800 border border-purple-200"
                          : record.role === "Supervisor"
                          ? "bg-amber-50 text-amber-800 border border-amber-200"
                          : "bg-blue-50 text-blue-800 border border-blue-200"
                      }`}
                    >
                      <Shield className="w-3 h-3 text-slate-400" />
                      {record.role}
                    </span>
                  </td>

                  {/* Contact */}
                  <td className="py-3 px-4 text-slate-500 whitespace-nowrap">
                    <div>{record.email}</div>
                    <div className="font-mono text-[11px] text-slate-400">{record.mobile}</div>
                  </td>

                  {/* Upload Date/Time */}
                  <td className="py-3 px-4 font-mono text-slate-600 whitespace-nowrap">
                    {record.uploadedAt}
                  </td>

                  {/* Status */}
                  <td className="py-3 px-3 whitespace-nowrap">
                    <span
                      className={`inline-flex items-center gap-1 text-[11px] font-bold px-2 py-0.5 rounded-full border ${badge.pillClass}`}
                    >
                      {badge.icon}
                      {badge.label}
                    </span>
                  </td>

                  {/* Actions */}
                  <td className="py-3 px-4 text-right whitespace-nowrap">
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onSelect(record);
                      }}
                      className="inline-flex items-center gap-1 text-xs font-semibold text-indigo-600 hover:text-indigo-800 bg-indigo-50 hover:bg-indigo-100 px-2.5 py-1 rounded-lg transition-colors"
                    >
                      <span>View</span>
                      <ExternalLink className="w-3.5 h-3.5" />
                    </button>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};
