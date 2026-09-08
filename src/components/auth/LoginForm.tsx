"use client";

import React, { useState } from "react";
import { User } from "@/types";
import { authService } from "@/services/authService";
import {
  LogIn,
  Lock,
  Mail,
  Shield,
  AlertCircle,
  Eye,
  EyeOff,
  Sparkles,
  ShieldCheck,
  Zap,
  CheckCircle2,
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
    <div className="relative w-full h-[100dvh] max-h-[100dvh] overflow-y-auto md:overflow-hidden flex flex-col justify-between bg-gradient-to-b from-slate-50 via-indigo-50/25 to-slate-100/50 p-3 sm:p-5 md:p-6 select-none">
      {/* Background Animated Gradient Blobs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none z-0">
        <div className="absolute -top-24 -left-24 w-80 sm:w-96 h-80 sm:h-96 bg-gradient-to-br from-indigo-400/20 to-purple-400/15 rounded-full blur-3xl animate-blob-1" />
        <div className="absolute -bottom-24 -right-24 w-80 sm:w-96 h-80 sm:h-96 bg-gradient-to-tl from-violet-400/20 to-indigo-300/15 rounded-full blur-3xl animate-blob-2" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-72 sm:w-80 h-72 sm:h-80 bg-cyan-300/10 rounded-full blur-3xl animate-pulse-glow" />
        {/* Subtle Tech Grid Overlay */}
        <div className="absolute inset-0 tech-grid-pattern opacity-60 [mask-image:radial-gradient(ellipse_at_center,black_45%,transparent_75%)]" />
      </div>

      {/* Floating Desktop Feature Badges */}
      <div className="hidden xl:flex absolute top-1/2 left-8 lg:left-12 -translate-y-1/2 flex-col gap-4 pointer-events-none z-10 animate-float-slow">
        <div className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-white/70 backdrop-blur-md border border-white/80 shadow-lg shadow-indigo-500/5">
          <div className="w-9 h-9 rounded-xl bg-indigo-50 border border-indigo-100/80 flex items-center justify-center text-indigo-600">
            <Zap className="w-5 h-5 animate-pulse" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
              <span>Neural OCR Engine</span>
              <span className="inline-flex items-center px-1.5 py-0.5 rounded-full text-[9px] font-semibold bg-emerald-100 text-emerald-700">
                v2.4
              </span>
            </div>
            <div className="text-[11px] text-slate-500">99.8% Field Extraction</div>
          </div>
        </div>

        <div className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-white/70 backdrop-blur-md border border-white/80 shadow-lg shadow-indigo-500/5">
          <div className="w-9 h-9 rounded-xl bg-emerald-50 border border-emerald-100/80 flex items-center justify-center text-emerald-600">
            <CheckCircle2 className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">Two-Sided Stitching</div>
            <div className="text-[11px] text-slate-500">Auto Front & Back Merge</div>
          </div>
        </div>
      </div>

      <div className="hidden xl:flex absolute top-1/2 right-8 lg:right-12 -translate-y-1/2 flex-col gap-4 pointer-events-none z-10 animate-float-reverse">
        <div className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-white/70 backdrop-blur-md border border-white/80 shadow-lg shadow-indigo-500/5">
          <div className="w-9 h-9 rounded-xl bg-violet-50 border border-violet-100/80 flex items-center justify-center text-violet-600">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">256-bit SSL Vault</div>
            <div className="text-[11px] text-slate-500">Bank-grade Data Security</div>
          </div>
        </div>

        <div className="flex items-center gap-3 px-4 py-3 rounded-2xl bg-white/70 backdrop-blur-md border border-white/80 shadow-lg shadow-indigo-500/5">
          <div className="w-9 h-9 rounded-xl bg-blue-50 border border-blue-100/80 flex items-center justify-center text-blue-600">
            <Sparkles className="w-5 h-5" />
          </div>
          <div>
            <div className="text-xs font-bold text-slate-800">GFF 2026 Ready</div>
            <div className="text-[11px] text-slate-500">High-concurrency Ready</div>
          </div>
        </div>
      </div>

      {/* Top Brand Header */}
      <header className="relative z-10 w-full max-w-sm sm:max-w-md mx-auto pt-2 sm:pt-4 text-center shrink-0">
        <div className="inline-flex relative group mb-2.5">
          <div className="absolute -inset-1.5 bg-gradient-to-r from-indigo-500 to-violet-600 rounded-2xl blur opacity-35 group-hover:opacity-60 transition duration-300 animate-pulse-glow" />
          <div className="relative inline-flex items-center justify-center w-12 h-12 sm:w-13 sm:h-13 rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-700 to-violet-700 text-white shadow-md shadow-indigo-500/25 ring-2 ring-white/80 animate-float-slow">
            <Shield className="w-6 h-6 sm:w-6.5 sm:h-6.5" />
          </div>
        </div>

        <h1 className="text-xl sm:text-2xl font-extrabold tracking-tight text-slate-900 leading-tight">
          FieldCapture <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 to-violet-600">Portal</span>
        </h1>
        <div className="inline-flex items-center gap-2 mt-1 px-3 py-1 rounded-full bg-indigo-50/80 border border-indigo-100/80 text-[11px] font-medium text-indigo-700">
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
          <span>Online Document Verification & OCR Extraction</span>
        </div>
      </header>

      {/* Main Login Card */}
      <main className="relative z-10 w-full max-w-sm sm:max-w-[420px] mx-auto my-auto py-2 sm:py-3 shrink-0">
        <div className="relative rounded-3xl bg-white/90 backdrop-blur-xl border border-white/90 p-5 sm:p-7 shadow-[0_20px_50px_-15px_rgba(79,70,229,0.12),0_10px_25px_-10px_rgba(15,23,42,0.06)] transition-all">
          {/* Top Gradient Accent Bar */}
          <div className="absolute top-0 left-6 right-6 h-[3px] bg-gradient-to-r from-indigo-500 via-purple-500 to-cyan-400 rounded-full" />

          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-lg font-bold text-slate-900 tracking-tight">Sign In</h2>
              <p className="text-xs text-slate-500 mt-0.5">
                Enter your credentials to access your account
              </p>
            </div>
            <div className="w-8 h-8 rounded-xl bg-slate-50 border border-slate-100 flex items-center justify-center text-slate-400">
              <Lock className="w-4 h-4 text-indigo-600" />
            </div>
          </div>

          {errorMessage && (
            <div className="mb-4 p-3 rounded-xl bg-rose-50 border border-rose-200/80 flex items-start gap-2.5 text-rose-800 text-xs sm:text-sm animate-in fade-in duration-200">
              <AlertCircle className="w-4 h-4 mt-0.5 shrink-0 text-rose-600" />
              <div className="font-medium">{errorMessage}</div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-3.5">
            <div>
              <label
                htmlFor="email"
                className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1"
              >
                Email / Username
              </label>
              <div
                className={`relative rounded-xl transition-all duration-200 ${
                  focusedField === "email"
                    ? "ring-2 ring-indigo-500/20 border-indigo-500 shadow-sm"
                    : "border-slate-200 hover:border-slate-300"
                }`}
              >
                <div
                  className={`absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none transition-colors duration-200 ${
                    focusedField === "email" ? "text-indigo-600" : "text-slate-400"
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
                  className="w-full pl-10 pr-3.5 py-2.5 sm:py-3 bg-slate-50/70 hover:bg-slate-50 focus:bg-white border border-slate-200 rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-indigo-500 transition-all disabled:opacity-60"
                  autoComplete="username email"
                  autoFocus
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="password"
                className="block text-[11px] font-bold text-slate-600 uppercase tracking-wider mb-1"
              >
                Password
              </label>
              <div
                className={`relative rounded-xl transition-all duration-200 ${
                  focusedField === "password"
                    ? "ring-2 ring-indigo-500/20 border-indigo-500 shadow-sm"
                    : "border-slate-200 hover:border-slate-300"
                }`}
              >
                <div
                  className={`absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none transition-colors duration-200 ${
                    focusedField === "password" ? "text-indigo-600" : "text-slate-400"
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
                  className="w-full pl-10 pr-10 py-2.5 sm:py-3 bg-slate-50/70 hover:bg-slate-50 focus:bg-white border border-slate-200 rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:outline-none focus:border-indigo-500 transition-all disabled:opacity-60"
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-600 transition-colors"
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
              className="w-full mt-2 py-3 px-4 bg-gradient-to-r from-indigo-600 via-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 active:scale-[0.99] text-white font-bold text-sm rounded-xl shadow-md shadow-indigo-500/25 hover:shadow-lg hover:shadow-indigo-500/30 transition-all flex items-center justify-center gap-2 group relative overflow-hidden disabled:opacity-60 cursor-pointer"
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

      {/* Clean Minimal Footer */}
      <footer className="relative z-10 text-center py-2 sm:py-3 text-[11px] text-slate-400 shrink-0">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-100/70 border border-slate-200/60">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500" />
          </span>
          <span className="font-medium text-slate-600">OCR Engine v2.4 Active</span>
          <span className="text-slate-300">•</span>
          <span className="text-slate-500">Enterprise Field Operations</span>
        </div>
      </footer>
    </div>
  );
};

