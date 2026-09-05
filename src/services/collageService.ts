/**
 * Client-Side Image Collage Service
 * Combines Front and Back document captures into a single vertical stacked collage
 * using an HTML5 Canvas, normalizing dimensions and preserving full aspect ratios.
 */

export interface CollageResult {
  blob: Blob;
  blobUrl: string;
  base64: string;
  width: number;
  height: number;
  sizeFormatted: string;
}

export interface CollageOptions {
  maxWidth?: number;
  gap?: number;
  quality?: number;
  backgroundColor?: string;
  dividerColor?: string;
  addLabels?: boolean;
}

/**
 * Loads an image from a string URL (data/blob/http) or Blob/File into an HTMLImageElement.
 */
function loadImage(source: string | Blob | File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    let src = "";
    let revokeNeeded = false;

    if (typeof source === "string") {
      src = source;
    } else if (source instanceof Blob) {
      src = URL.createObjectURL(source);
      revokeNeeded = true;
    } else {
      return reject(new Error("Invalid image source provided for collage generation."));
    }

    const img = new Image();
    img.crossOrigin = "anonymous";

    img.onload = () => {
      if (revokeNeeded) {
        URL.revokeObjectURL(src);
      }
      resolve(img);
    };

    img.onerror = (err) => {
      if (revokeNeeded) {
        URL.revokeObjectURL(src);
      }
      reject(new Error("Failed to load image for collage composition. The file may be corrupt or inaccessible."));
    };

    img.src = src;
  });
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return "0 Bytes";
  const k = 1024;
  const sizes = ["Bytes", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
}

export const collageService = {
  /**
   * Combines Front and Back document images into a single vertical stacked collage.
   *
   * @param frontSource Front image as DataURL, Blob URL, Blob, or File
   * @param backSource Back image as DataURL, Blob URL, Blob, or File
   * @param options Customization options (maxWidth, gap, quality, etc.)
   */
  async createDocumentCollage(
    frontSource: string | Blob | File,
    backSource: string | Blob | File,
    options: CollageOptions = {}
  ): Promise<CollageResult> {
    if (typeof document === "undefined") {
      throw new Error("createDocumentCollage must be executed in a browser client environment.");
    }

    const {
      maxWidth = 1200,
      gap = 28,
      quality = 0.88,
      backgroundColor = "#F8FAFC", // Tailwind slate-50
      dividerColor = "#CBD5E1", // Tailwind slate-300
      addLabels = true,
    } = options;

    // 1. Load both images concurrently
    const [frontImg, backImg] = await Promise.all([
      loadImage(frontSource),
      loadImage(backSource),
    ]);

    if (!frontImg.width || !frontImg.height) {
      throw new Error("Front side image has invalid dimensions (width or height is 0).");
    }
    if (!backImg.width || !backImg.height) {
      throw new Error("Back side image has invalid dimensions (width or height is 0).");
    }

    // 2. Normalize to consistent target width
    // Pick the larger width or cap at maxWidth for optimal OCR resolution and manageable payload size
    const rawTargetWidth = Math.max(frontImg.width, backImg.width, 800);
    const targetWidth = Math.min(rawTargetWidth, maxWidth);

    // 3. Proportional height scaling (zero distortion, no stretching, full preservation)
    const frontScaledHeight = Math.round(frontImg.height * (targetWidth / frontImg.width));
    const backScaledHeight = Math.round(backImg.height * (targetWidth / backImg.width));

    // Optional label header heights
    const labelHeight = addLabels ? 36 : 0;
    const padding = 16; // Outer edge margin for a clean presentation

    // 4. Calculate total canvas dimensions
    const canvasWidth = targetWidth + padding * 2;
    const totalHeight =
      padding +
      labelHeight +
      frontScaledHeight +
      gap +
      labelHeight +
      backScaledHeight +
      padding;

    // 5. Initialize Canvas
    const canvas = document.createElement("canvas");
    canvas.width = canvasWidth;
    canvas.height = totalHeight;

    const ctx = canvas.getContext("2d", { willReadFrequently: false });
    if (!ctx) {
      throw new Error("Browser failed to initialize 2D canvas context for collage processing.");
    }

    // Enable high-quality image smoothing
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = "high";

    // Fill background with neutral tone
    ctx.fillStyle = backgroundColor;
    ctx.fillRect(0, 0, canvasWidth, totalHeight);

    let currentY = padding;

    // 6. Draw Front Side Label (if enabled)
    if (addLabels) {
      ctx.fillStyle = "#1E293B"; // slate-800
      ctx.font = "bold 13px -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";
      ctx.fillText("FRONT SIDE", padding + 4, currentY + 20);

      // Subtext
      ctx.fillStyle = "#64748B"; // slate-500
      ctx.font = "normal 11px -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";
      ctx.fillText("Primary Document Details", padding + 95, currentY + 20);

      currentY += labelHeight;
    }

    // 7. Draw Front Image
    ctx.drawImage(frontImg, padding, currentY, targetWidth, frontScaledHeight);
    currentY += frontScaledHeight;

    // 8. Draw Elegant Divider Bar in the Gap
    const dividerY = currentY + Math.round(gap / 2);
    ctx.strokeStyle = dividerColor;
    ctx.lineWidth = 1.5;
    ctx.setLineDash([6, 6]);
    ctx.beginPath();
    ctx.moveTo(padding, dividerY);
    ctx.lineTo(padding + targetWidth, dividerY);
    ctx.stroke();
    ctx.setLineDash([]); // Reset line dash

    currentY += gap;

    // 9. Draw Back Side Label (if enabled)
    if (addLabels) {
      ctx.fillStyle = "#1E293B"; // slate-800
      ctx.font = "bold 13px -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";
      ctx.fillText("BACK SIDE", padding + 4, currentY + 20);

      // Subtext
      ctx.fillStyle = "#64748B"; // slate-500
      ctx.font = "normal 11px -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif";
      ctx.fillText("Secondary / Address Details", padding + 85, currentY + 20);

      currentY += labelHeight;
    }

    // 10. Draw Back Image
    ctx.drawImage(backImg, padding, currentY, targetWidth, backScaledHeight);

    // 11. Serialize Canvas to JPEG Blob & Base64 Data URL
    return new Promise((resolve, reject) => {
      try {
        const base64 = canvas.toDataURL("image/jpeg", quality);

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error("Failed to serialize combined document canvas to image blob."));
              return;
            }

            const blobUrl = URL.createObjectURL(blob);
            resolve({
              blob,
              blobUrl,
              base64,
              width: canvasWidth,
              height: totalHeight,
              sizeFormatted: formatBytes(blob.size),
            });
          },
          "image/jpeg",
          quality
        );
      } catch (err: any) {
        reject(new Error(`Collage serialization failed: ${err?.message || "Unknown canvas error"}`));
      }
    });
  },
};
