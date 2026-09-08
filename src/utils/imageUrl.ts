import { API_BASE_URL } from "@/config/apiConfig";

/**
 * Resolves the optimal, fast-loading image URL for visiting cards.
 * If the image is stored in private AWS S3, proxies via backend streaming endpoint
 * so the browser never suffers 403 Forbidden or timeouts.
 */
export function getDisplayImageUrl(record?: { id?: string; imageUrl?: string } | null): string {
  if (!record) return "";
  if (record.imageUrl) {
    // Immediate base64/blob from local upload preview
    if (record.imageUrl.startsWith("data:") || record.imageUrl.startsWith("blob:")) {
      return record.imageUrl;
    }
    // External CDN demo images
    if (record.imageUrl.includes("images.unsplash.com")) {
      return record.imageUrl;
    }
  }
  // Stream private S3 asset securely through backend
  if (record.id) {
    return `${API_BASE_URL}/documents/record/${record.id}/image`;
  }
  return record.imageUrl || "";
}
