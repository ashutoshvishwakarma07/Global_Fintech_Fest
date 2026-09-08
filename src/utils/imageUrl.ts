import { API_BASE_URL } from "@/config/apiConfig";

/**
 * Resolves the optimal, fast-loading image URL for visiting cards.
 * If the image is stored in private AWS S3 or relative backend endpoint,
 * safely resolves to the backend streaming endpoint so the browser never suffers 403 Forbidden or 404s.
 */
export function getDisplayImageUrl(record?: { id?: string; imageUrl?: string } | null): string {
  if (!record) return "";

  const img = record.imageUrl;
  if (img) {
    // 1. Immediate base64/blob from local upload preview
    if (img.startsWith("data:") || img.startsWith("blob:")) {
      return img;
    }
    // 2. External CDN demo images
    if (img.includes("images.unsplash.com")) {
      return img;
    }
    // 3. If it's a relative URL to the backend streaming endpoint
    if (img.startsWith("/api/v1/")) {
      const origin = API_BASE_URL.replace(/\/api\/v1\/?$/, "");
      return `${origin}${img}`;
    }
    if (img.startsWith("/documents/")) {
      return `${API_BASE_URL}${img}`;
    }
    // 4. If it's already an absolute URL to our backend streaming endpoint
    if (img.includes("/documents/record/") && img.endsWith("/image")) {
      return img;
    }
    // 5. If it's a private AWS S3 URL, proxy via backend streaming endpoint
    if (img.includes(".amazonaws.com") && record.id) {
      return `${API_BASE_URL}/documents/record/${record.id}/image`;
    }
    // Other absolute URLs
    if (img.startsWith("http://") || img.startsWith("https://")) {
      return img;
    }
  }

  // Stream private S3 / locally saved asset securely through backend
  if (record.id) {
    return `${API_BASE_URL}/documents/record/${record.id}/image`;
  }

  return img || "";
}
