/**
 * Centralized API Base URL configuration.
 * Dynamically resolves NEXT_PUBLIC_API_URL for Local development vs AWS EC2 production.
 * Automatically handles whether the context-path '/api/v1' is present in the env variable.
 */
function resolveApiBaseUrl(): string {
  // Read from environment; default safely to localhost for local development
  const envUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
  const trimmed = envUrl.trim().replace(/\/+$/, "");

  // If already ends with /api/v1, return as is. Otherwise append /api/v1
  return trimmed.endsWith("/api/v1") ? trimmed : `${trimmed}/api/v1`;
}

export const API_BASE_URL = resolveApiBaseUrl();
