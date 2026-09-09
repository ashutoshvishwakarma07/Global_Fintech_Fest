import type { Metadata, Viewport } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "GFF Visiting Card OCR Portal - Global Fintech Fest",
  description: "Enterprise visiting card image capture, OCR extraction, and verification dashboard",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  viewportFit: "cover",
  themeColor: "#4f46e5",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="antialiased bg-slate-50 text-slate-900 selection:bg-indigo-100 selection:text-indigo-900 min-h-screen">
        {children}
      </body>
    </html>
  );
}
