import React from "react";
import { ImageOff, FilterX, Camera } from "lucide-react";

interface EmptyStateProps {
  type?: "no-records" | "no-search-results" | "upload-needed";
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  type = "no-records",
  title,
  description,
  actionLabel,
  onAction,
}) => {
  const defaults = {
    "no-records": {
      icon: <ImageOff className="w-10 h-10 text-slate-400" />,
      title: "No Uploads Found",
      description: "No verification images have been captured or submitted yet.",
      actionLabel: "Capture New Image",
    },
    "no-search-results": {
      icon: <FilterX className="w-10 h-10 text-slate-400" />,
      title: "No Matching Records",
      description: "Try adjusting your search terms or filters to find what you're looking for.",
      actionLabel: "Reset Filters",
    },
    "upload-needed": {
      icon: <Camera className="w-10 h-10 text-indigo-500" />,
      title: "Ready to Capture",
      description: "Take a photo or upload an image from your device gallery to begin verification.",
      actionLabel: "Start Capture",
    },
  };

  const current = defaults[type];

  return (
    <div className="flex flex-col items-center justify-center p-8 text-center bg-white rounded-2xl border border-dashed border-slate-200 shadow-sm my-4">
      <div className="w-16 h-16 rounded-full bg-slate-50 flex items-center justify-center mb-3">
        {current.icon}
      </div>
      <h3 className="text-base font-semibold text-slate-900">{title || current.title}</h3>
      <p className="text-sm text-slate-500 max-w-xs mt-1 mb-5">{description || current.description}</p>
      {actionLabel || current.actionLabel ? (
        <button
          type="button"
          onClick={onAction}
          className="inline-flex items-center justify-center px-4 py-2.5 rounded-xl bg-indigo-600 text-white text-sm font-medium hover:bg-indigo-700 active:scale-95 transition-all shadow-sm touch-target-min"
        >
          {actionLabel || current.actionLabel}
        </button>
      ) : null}
    </div>
  );
};
