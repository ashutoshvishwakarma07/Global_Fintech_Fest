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

export const ALLOWED_FILE_EXTENSIONS = [".png", ".jpg", ".jpeg", ".doc", ".docx", ".pdf"];
export const ALLOWED_MIME_TYPES = [
  "image/png",
  "image/jpeg",
  "image/jpg",
  "image/pjpeg",
  "application/pdf",
  "application/msword",
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
];

export const ACCEPTED_FILE_INPUT_TYPES = ".png,.jpg,.jpeg,.doc,.docx,.pdf,image/png,image/jpeg,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document";

export const imageProcessing = {
  formatFileSize(bytes: number): string {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
  },

  validateFile(file: File): { valid: boolean; error?: string } {
    const fileName = (file.name || "").toLowerCase();
    const extMatch = ALLOWED_FILE_EXTENSIONS.some((ext) => fileName.endsWith(ext));
    const mimeMatch = ALLOWED_MIME_TYPES.includes(file.type.toLowerCase());

    if (!extMatch && !mimeMatch) {
      return {
        valid: false,
        error: "Unsupported file format. Only PNG, JPG/JPEG, Word (.doc, .docx), and PDF files are allowed.",
      };
    }

    const maxSize = 25 * 1024 * 1024; // 25MB
    if (file.size > maxSize) {
      return { valid: false, error: "File size exceeds 25MB limit. Please select a smaller file." };
    }

    return { valid: true };
  },

  validateImage(file: File): { valid: boolean; error?: string } {
    return this.validateFile(file);
  },

  isDocumentFile(src: string): boolean {
    const lower = src.toLowerCase();
    return (
      lower.startsWith("data:application/pdf") ||
      lower.startsWith("data:application/msword") ||
      lower.startsWith("data:application/vnd") ||
      lower.endsWith(".pdf") ||
      lower.endsWith(".doc") ||
      lower.endsWith(".docx")
    );
  },

  /**
   * Compresses an image source (File, Blob, or Data URL) into a high-quality JPEG Blob
   * constrained to maxDimension (default 1200px) and quality (default 0.8).
   * If source is PDF/DOC, returns the blob directly without canvas conversion.
   */
  async compressToBlob(
    source: File | Blob | string,
    maxDimension = 1200,
    quality = 0.8
  ): Promise<ProcessedImage> {
    // If source is a document (PDF / Word), handle as raw Blob
    if (typeof source === "string" && this.isDocumentFile(source)) {
      const res = await fetch(source);
      const blob = await res.blob();
      const blobUrl = URL.createObjectURL(blob);
      return {
        blob,
        blobUrl,
        width: 0,
        height: 0,
        sizeFormatted: this.formatFileSize(blob.size),
      };
    }

    if (source instanceof Blob && !source.type.startsWith("image/")) {
      const blobUrl = URL.createObjectURL(source);
      return {
        blob: source,
        blobUrl,
        width: 0,
        height: 0,
        sizeFormatted: this.formatFileSize(source.size),
      };
    }

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
