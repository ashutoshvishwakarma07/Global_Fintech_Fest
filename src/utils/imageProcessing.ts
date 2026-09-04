/**
 * Image processing utilities for client-side compression and Blob generation
 * without bloating memory with gigantic base64 strings.
 */

export interface ProcessedImage {
  blob: Blob;
  blobUrl: string;
  width: number;
  height: number;
  sizeFormatted: string;
}

export const imageProcessing = {
  formatFileSize(bytes: number): string {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
  },

  validateImage(file: File): { valid: boolean; error?: string } {
    const validTypes = ["image/jpeg", "image/png", "image/webp", "image/heic", "image/jpg"];
    if (!validTypes.includes(file.type.toLowerCase()) && !file.type.startsWith("image/")) {
      return { valid: false, error: "Invalid file format. Please choose a JPG, PNG, or WEBP image." };
    }

    const maxSize = 25 * 1024 * 1024; // 25MB
    if (file.size > maxSize) {
      return { valid: false, error: "Image file exceeds 25MB limit. Please select a smaller photo." };
    }

    return { valid: true };
  },

  /**
   * Compresses an image source (File, Blob, or Data URL) into a high-quality JPEG Blob
   * constrained to maxDimension (default 1200px) and quality (default 0.8)
   */
  async compressToBlob(
    source: File | Blob | string,
    maxDimension = 1200,
    quality = 0.8
  ): Promise<ProcessedImage> {
    return new Promise((resolve, reject) => {
      let objectUrlToRevoke: string | null = null;
      let src = "";

      if (typeof source === "string") {
        src = source;
      } else {
        src = URL.createObjectURL(source);
        objectUrlToRevoke = src;
      }

      const img = new Image();
      img.onload = () => {
        if (objectUrlToRevoke) {
          URL.revokeObjectURL(objectUrlToRevoke);
        }

        let { width, height } = img;
        if (width > maxDimension || height > maxDimension) {
          if (width > height) {
            height = Math.round((height * maxDimension) / width);
            width = maxDimension;
          } else {
            width = Math.round((width * maxDimension) / height);
            height = maxDimension;
          }
        }

        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext("2d");

        if (!ctx) {
          reject(new Error("Unable to create canvas 2D rendering context"));
          return;
        }

        // Draw and compress to blob
        ctx.drawImage(img, 0, 0, width, height);

        canvas.toBlob(
          (blob) => {
            if (!blob) {
              reject(new Error("Canvas toBlob serialization failed"));
              return;
            }

            const blobUrl = URL.createObjectURL(blob);
            resolve({
              blob,
              blobUrl,
              width,
              height,
              sizeFormatted: imageProcessing.formatFileSize(blob.size),
            });
          },
          "image/jpeg",
          quality
        );
      };

      img.onerror = () => {
        if (objectUrlToRevoke) URL.revokeObjectURL(objectUrlToRevoke);
        reject(new Error("Failed to load image for processing"));
      };

      img.src = src;
    });
  },
};
