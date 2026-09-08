/**
 * Centralized API Base URL configuration.
 * Dynamically resolves API URL for both Local development (localhost) and Production (18.60.179.46).
 */
export function getApiBaseUrl(): string {
  if (typeof window !== "undefined") {
    const host = window.location.hostname;
    // If accessed via production IP, route calls to production backend
    if (host === "18.60.179.46") {
      return "http://18.60.179.46:8080/api/v1";
    }
    // If accessed via another remote domain or IP, dynamically target port 8080
    if (host && host !== "localhost" && host !== "127.0.0.1") {
      return `${window.location.protocol}//${host}:8080/api/v1`;
    }
  }
  const envUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
  const trimmed = envUrl.trim().replace(/\/+$/, "");
  return trimmed.endsWith("/api/v1") ? trimmed : `${trimmed}/api/v1`;
}

// Proxy/String wrapper to ensure template strings `${API_BASE_URL}/path`
// always evaluate getApiBaseUrl() dynamically in real time
export const API_BASE_URL = {
  toString: () => getApiBaseUrl(),
  valueOf: () => getApiBaseUrl(),
  concat: (...args: string[]) => getApiBaseUrl().concat(...args),
  replace: (pattern: any, replacement: any) => getApiBaseUrl().replace(pattern, replacement),
  endsWith: (searchString: string) => getApiBaseUrl().endsWith(searchString),
  startsWith: (searchString: string) => getApiBaseUrl().startsWith(searchString),
  includes: (searchString: string) => getApiBaseUrl().includes(searchString),
  indexOf: (searchString: string) => getApiBaseUrl().indexOf(searchString),
  [Symbol.toPrimitive]: () => getApiBaseUrl(),
} as unknown as string;

