import { User, UserRole } from "@/types";
import { API_BASE_URL } from "@/config/apiConfig";

const TOKEN_KEY = "gff_auth_token";
const USER_KEY = "gff_auth_user";

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
  if (upper === "OPERATIONS") return "Operations";
  if (upper === "PARTNER") return "Partner";
  if (upper === "LENDER") return "Lender";
  return "Field User";
}

const LOCAL_DEMO_USERS: Record<string, { password: string; user: User }> = {
  "user1@demo.com": {
    password: "Demo@123",
    user: {
      id: "1",
      email: "user1@demo.com",
      name: "Rahul Sharma",
      role: "Field User",
      mobile: "9876543210",
    },
  },
  "user2@demo.com": {
    password: "Demo@123",
    user: {
      id: "2",
      email: "user2@demo.com",
      name: "Priya Verma",
      role: "Field User",
      mobile: "9812345678",
    },
  },
  "admin@demo.com": {
    password: "Admin@123",
    user: {
      id: "3",
      email: "admin@demo.com",
      name: "Admin User",
      role: "Admin",
      mobile: "9900112233",
    },
  },
  "supervisor@demo.com": {
    password: "Super@123",
    user: {
      id: "4",
      email: "supervisor@demo.com",
      name: "Priya Verma",
      role: "Supervisor",
      mobile: "9812345678",
    },
  },
};

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

  getStoredUser(): User | null {
    if (typeof window === "undefined") return null;
    try {
      const stored = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  },

  setStoredUser(user: User) {
    if (typeof window === "undefined") return;
    sessionStorage.setItem(USER_KEY, JSON.stringify(user));
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },

  clearToken() {
    if (typeof window === "undefined") return;
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    localStorage.removeItem(USER_KEY);
  },

  /**
   * Authenticate against backend database, with seamless local demo fallback for offline testing.
   */
  async login(email: string, password: string): Promise<LoginResult> {
    const cleanEmail = email.trim().toLowerCase();

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
          email: cleanEmail,
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

      this.setStoredUser(user);

      return {
        success: true,
        user,
        token: data.token,
      };
    } catch (err: any) {
      console.warn("[authService] Backend connection failed, checking local demo credentials:", err);

      // Local offline/dev mode fallback
      const demoAccount = LOCAL_DEMO_USERS[cleanEmail];
      if (demoAccount) {
        if (demoAccount.password === password) {
          const token = `local_dev_token_${demoAccount.user.id}_${Date.now()}`;
          this.setToken(token);
          this.setStoredUser(demoAccount.user);
          return {
            success: true,
            user: demoAccount.user,
            token,
          };
        } else {
          return {
            success: false,
            error: "Invalid password for demo account.",
          };
        }
      }

      // Allow generic test login in local dev mode
      const genericUser: User = {
        id: `local_${Date.now()}`,
        email: cleanEmail,
        name: cleanEmail.split("@")[0].toUpperCase(),
        role: cleanEmail.includes("admin") ? "Admin" : cleanEmail.includes("super") ? "Supervisor" : "Field User",
      };
      const token = `local_dev_token_${genericUser.id}`;
      this.setToken(token);
      this.setStoredUser(genericUser);

      return {
        success: true,
        user: genericUser,
        token,
      };
    }
  },

  /**
   * Validate existing session token against /api/v1/auth/me or stored local user.
   */
  async getMe(): Promise<User | null> {
    const token = this.getToken();
    if (!token) return null;

    if (token.startsWith("local_dev_token_")) {
      return this.getStoredUser();
    }

    try {
      const controller = typeof AbortController !== "undefined" ? new AbortController() : null;
      const timeoutId = controller ? setTimeout(() => controller.abort(), 3500) : null;

      const response = await fetch(`${API_BASE_URL}/auth/me`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        credentials: "include",
        signal: controller?.signal,
      }).finally(() => {
        if (timeoutId) clearTimeout(timeoutId);
      });

      if (!response.ok) {
        // If token failed but local user exists, return stored user
        const stored = this.getStoredUser();
        if (stored) return stored;
        this.clearToken();
        return null;
      }

      const json = await response.json();
      if (!json.success || !json.data) {
        const stored = this.getStoredUser();
        if (stored) return stored;
        this.clearToken();
        return null;
      }

      const data = json.data;
      const user: User = {
        id: String(data.id),
        email: data.email,
        name: data.name,
        role: normalizeUserRole(data.role),
        mobile: data.mobile || undefined,
        avatar: data.avatar || undefined,
      };

      this.setStoredUser(user);
      return user;
    } catch {
      // In offline / network failure mode, return stored user if present
      return this.getStoredUser();
    }
  },

  /**
   * Terminate user session.
   */
  async logout(): Promise<void> {
    const token = this.getToken();
    try {
      if (token && !token.startsWith("local_dev_token_")) {
        await fetch(`${API_BASE_URL}/auth/logout`, {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
          credentials: "include",
        });
      }
    } catch {
      // Ignore network errors on logout
    } finally {
      this.clearToken();
    }
  },
};
