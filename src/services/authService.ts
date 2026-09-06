import { User, UserRole } from "@/types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api/v1";
const TOKEN_KEY = "gff_auth_token";

export interface LoginResult {
  success: boolean;
  user?: User;
  token?: string;
  error?: string;
}

export function normalizeUserRole(backendRole?: string): UserRole {
  if (!backendRole) return "Field User";
  const upper = backendRole.toUpperCase();
  if (upper === "ADMIN") return "Admin";
  if (upper === "SUPERVISOR") return "Supervisor";
  return "Field User";
}

export const authService = {
  getToken(): string | null {
    if (typeof window === "undefined") return null;
    return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY);
  },

  setToken(token: string) {
    if (typeof window === "undefined") return;
    sessionStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(TOKEN_KEY, token);
  },

  clearToken() {
    if (typeof window === "undefined") return;
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_KEY);
  },

  /**
   * Authenticate strictly against the backend database using /api/v1/auth/login.
   */
  async login(email: string, password: string): Promise<LoginResult> {
    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          email: email.trim(),
          password,
        }),
      });

      const json = await response.json().catch(() => ({}));

      if (!response.ok || !json.success) {
        return {
          success: false,
          error: json.message || "Invalid username/email or password.",
        };
      }

      const data = json.data;
      if (!data || !data.token) {
        return {
          success: false,
          error: "Authentication service returned an invalid response.",
        };
      }

      this.setToken(data.token);

      const user: User = {
        id: String(data.id),
        email: data.email,
        name: data.name,
        role: normalizeUserRole(data.role),
        mobile: data.mobile || undefined,
        avatar: data.avatar || undefined,
      };

      return {
        success: true,
        user,
        token: data.token,
      };
    } catch (err: any) {
      return {
        success: false,
        error: "Unable to connect to the authentication server. Please check your network or try again later.",
      };
    }
  },

  /**
   * Validate existing session token against /api/v1/auth/me.
   */
  async getMe(): Promise<User | null> {
    const token = this.getToken();
    if (!token) return null;

    try {
      const response = await fetch(`${API_BASE_URL}/auth/me`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        credentials: "include",
      });

      if (!response.ok) {
        this.clearToken();
        return null;
      }

      const json = await response.json();
      if (!json.success || !json.data) {
        this.clearToken();
        return null;
      }

      const data = json.data;
      return {
        id: String(data.id),
        email: data.email,
        name: data.name,
        role: normalizeUserRole(data.role),
        mobile: data.mobile || undefined,
        avatar: data.avatar || undefined,
      };
    } catch {
      return null;
    }
  },

  /**
   * Terminate user session.
   */
  async logout(): Promise<void> {
    const token = this.getToken();
    try {
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        credentials: "include",
      });
    } catch {
      // Ignore network errors on logout
    } finally {
      this.clearToken();
    }
  },
};
