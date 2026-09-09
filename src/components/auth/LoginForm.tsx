"use client";

import React, { useState } from "react";
import { User } from "@/types";
import { authService } from "@/services/authService";
import {
  LogIn,
  Lock,
  Mail,
  AlertCircle,
  Eye,
  EyeOff,
} from "lucide-react";

interface LoginFormProps {
  onLoginSuccess: (user: User) => void;
}

export const LoginForm: React.FC<LoginFormProps> = ({ onLoginSuccess }) => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [focusedField, setFocusedField] = useState<"email" | "password" | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    const cleanEmail = email.trim();
    const cleanPassword = password.trim();
    if (!cleanEmail) {
      setErrorMessage("Please enter your email or username.");
      return;
    }
    if (!cleanPassword) {
      setErrorMessage("Please enter your password.");
      return;
    }

    setLoading(true);
    try {
      const result = await authService.login(cleanEmail, cleanPassword);
      if (result.success && result.user) {
        onLoginSuccess(result.user);
      } else {
        setErrorMessage(result.error || "Authentication failed. Please check your credentials.");
      }
    } catch {
      setErrorMessage("An unexpected error occurred. Please check your connection and try again.");
    } finally {
      setLoading(false);
    }
  };


  return (
    <div className="relative w-full min-h-[100dvh] overflow-y-auto flex flex-col items-center justify-center bg-[#021329] p-4 sm:p-6 select-none">
      {/* Background Animated Gradient Blobs & Cosmic Glows (Sampled from Qualtech GFF 2026 Theme) */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0">
        {/* Deep ocean teal radial ambient aurora */}
        <div className="absolute -top-32 -left-32 w-96 sm:w-[520px] h-96 sm:h-[520px] bg-gradient-to-br from-[#04848c]/25 via-[#03435c]/20 to-transparent rounded-full blur-3xl animate-blob-1" />
        <div className="absolute -bottom-32 -right-32 w-96 sm:w-[520px] h-96 sm:h-[520px] bg-gradient-to-tl from-[#025265]/35 via-[#032f54]/25 to-transparent rounded-full blur-3xl animate-blob-2" />
        <div className="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-80 sm:w-[450px] h-80 sm:h-[450px] bg-[#00e5ff]/10 rounded-full blur-3xl animate-pulse-glow" />
        
        {/* Subtle Starlight / Tech Grid Overlay */}
        <div className="absolute inset-0 tech-grid-pattern opacity-30 [mask-image:radial-gradient(ellipse_at_center,black_55%,transparent_80%)]" />
      </div>

      {/* Unified Centered Container for Logo Header + Login Card */}
      <div className="relative z-10 w-full max-w-sm sm:max-w-[420px] mx-auto my-auto flex flex-col items-center py-2 sm:py-4">
        {/* Brand Header */}
        <header className="w-full flex flex-col items-center text-center mb-4 sm:mb-5 shrink-0">
          {/* Brand Logos Arranged Vertically (Qualtech on top & bigger, AI Foundry below) */}
          <div className="flex flex-col items-center gap-2.5 mb-3.5">
            {/* Qualtech Logo (Top - Animated & Glowing) */}
            <div className="relative group">
              {/* Pulsing Aurora Ambient Glow behind Card */}
              <div className="absolute -inset-1 bg-gradient-to-r from-teal-500/30 via-cyan-400/40 to-blue-500/30 rounded-2xl blur-md opacity-70 group-hover:opacity-100 transition duration-500 animate-pulse-glow" />
              
              <div className="relative overflow-hidden px-5 sm:px-6 py-2.5 sm:py-3 rounded-2xl bg-[#031d38]/85 backdrop-blur-md border border-cyan-500/35 shadow-lg shadow-cyan-950/70 hover:border-cyan-400/60 transition-all flex items-center justify-center">
                {/* Shimmer sweep effect */}
                <div className="absolute inset-0 w-1/2 h-full bg-white/10 skew-x-12 -translate-x-full group-hover:animate-shimmer pointer-events-none" />

                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src="/qualtech-logo.png"
                  alt="Qualtech Logo"
                  className="h-9 sm:h-11 w-auto max-h-[44px] object-contain brightness-110 animate-logo-breathe hover:scale-105 transition-transform duration-300"
                  style={{ maxHeight: "44px", width: "auto" }}
                />
              </div>
            </div>

            {/* AI Foundry Logo (Below - Clean, no box/border) */}
            <div className="flex items-center justify-center py-0.5">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src="/ai-foundry-logo.png"
                alt="AI Foundry Logo"
                className="h-6 sm:h-7 w-auto max-h-[28px] object-contain drop-shadow-[0_2px_10px_rgba(56,189,248,0.45)] hover:scale-105 transition-transform duration-300"
                style={{ maxHeight: "28px", width: "auto" }}
              />
            </div>
          </div>

          <h1 className="text-xl sm:text-2xl font-extrabold tracking-tight text-white leading-tight">
            GFF Visiting Card <span className="text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 via-teal-300 to-sky-400">OCR Portal</span>
          </h1>
        </header>

        {/* Main Login Card */}
        <main className="w-full">
          <div className="relative rounded-3xl bg-[#031d38]/85 backdrop-blur-2xl border border-cyan-500/30 p-5 sm:p-7 shadow-[0_25px_60px_-15px_rgba(0,0,0,0.65),0_0_35px_rgba(4,132,140,0.18)] transition-all">
            {/* Top Gradient Accent Bar */}
            <div className="absolute top-0 left-6 right-6 h-[3px] bg-gradient-to-r from-teal-400 via-cyan-400 to-sky-400 rounded-full shadow-[0_0_12px_rgba(34,211,238,0.6)]" />

            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="text-lg font-bold text-white tracking-tight">Sign In</h2>
                <p className="text-xs text-cyan-100/70 mt-0.5">
                  Enter your credentials to access your account
                </p>
              </div>
              <div className="w-8 h-8 rounded-xl bg-cyan-950/60 border border-cyan-500/30 flex items-center justify-center text-cyan-400 shadow-inner">
                <Lock className="w-4 h-4 text-cyan-400" />
              </div>
            </div>

            {errorMessage && (
              <div className="mb-4 p-3 rounded-xl bg-rose-950/60 border border-rose-800/80 flex items-start gap-2.5 text-rose-200 text-xs sm:text-sm animate-in fade-in duration-200">
                <AlertCircle className="w-4 h-4 mt-0.5 shrink-0 text-rose-400" />
                <div className="font-medium">{errorMessage}</div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-3.5">
              <div>
                <label
                  htmlFor="email"
                  className="block text-[11px] font-bold text-cyan-200/80 uppercase tracking-wider mb-1"
                >
                  Email / Username
                </label>
                <div
                  className={`relative rounded-xl transition-all duration-200 ${
                    focusedField === "email"
                      ? "ring-2 ring-cyan-400/30 border-cyan-400 shadow-sm shadow-cyan-500/20"
                      : "border-slate-700/80 hover:border-cyan-700"
                  }`}
                >
                  <div
                    className={`absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none transition-colors duration-200 ${
                      focusedField === "email" ? "text-cyan-400" : "text-slate-400"
                    }`}
                  >
                    <Mail className="w-4.5 h-4.5" />
                  </div>
                  <input
                    id="email"
                    type="text"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    onFocus={() => setFocusedField("email")}
                    onBlur={() => setFocusedField(null)}
                    placeholder="name@company.com"
                    disabled={loading}
                    className="w-full pl-10 pr-3.5 py-2.5 sm:py-3 bg-[#02142b]/90 hover:bg-[#021733] focus:bg-[#021733] border border-cyan-900/50 rounded-xl text-sm text-white placeholder-slate-400 focus:outline-none focus:border-cyan-400 transition-all disabled:opacity-60"
                    autoComplete="username email"
                    autoFocus
                  />
                </div>
              </div>

              <div>
                <label
                  htmlFor="password"
                  className="block text-[11px] font-bold text-cyan-200/80 uppercase tracking-wider mb-1"
                >
                  Password
                </label>
                <div
                  className={`relative rounded-xl transition-all duration-200 ${
                    focusedField === "password"
                      ? "ring-2 ring-cyan-400/30 border-cyan-400 shadow-sm shadow-cyan-500/20"
                      : "border-slate-700/80 hover:border-cyan-700"
                  }`}
                >
                  <div
                    className={`absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none transition-colors duration-200 ${
                      focusedField === "password" ? "text-cyan-400" : "text-slate-400"
                    }`}
                  >
                    <Lock className="w-4.5 h-4.5" />
                  </div>
                  <input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onFocus={() => setFocusedField("password")}
                    onBlur={() => setFocusedField(null)}
                    placeholder="••••••••"
                    disabled={loading}
                    className="w-full pl-10 pr-10 py-2.5 sm:py-3 bg-[#02142b]/90 hover:bg-[#021733] focus:bg-[#021733] border border-cyan-900/50 rounded-xl text-sm text-white placeholder-slate-400 focus:outline-none focus:border-cyan-400 transition-all disabled:opacity-60"
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-cyan-400/70 hover:text-cyan-300 transition-colors"
                    aria-label={showPassword ? "Hide password" : "Show password"}
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Submit Button */}
              <button
                type="submit"
                disabled={loading}
                className="w-full mt-2 py-3 px-4 bg-gradient-to-r from-cyan-500 via-teal-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 active:scale-[0.99] text-white font-bold text-sm rounded-xl shadow-lg shadow-cyan-500/25 hover:shadow-cyan-500/40 transition-all flex items-center justify-center gap-2 group relative overflow-hidden disabled:opacity-60 cursor-pointer"
              >
                {/* Shimmer sweep effect */}
                <div className="absolute inset-0 w-1/2 h-full bg-white/20 skew-x-12 -translate-x-full group-hover:animate-shimmer pointer-events-none" />

                {loading ? (
                  <>
                    <div className="w-4.5 h-4.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span>Verifying Credentials...</span>
                  </>
                ) : (
                  <>
                    <span>Sign In</span>
                    <LogIn className="w-4 h-4 transition-transform duration-200 group-hover:translate-x-1" />
                  </>
                )}
              </button>
            </form>
          </div>
        </main>
      </div>
    </div>
  );
};

