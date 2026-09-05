import { User } from "@/types";

export interface DemoUser extends User {
  password: string;
}

export const DEMO_USERS: DemoUser[] = [
  {
    id: "usr_1",
    email: "user1@demo.com",
    password: "Demo@123",
    name: "Rahul Sharma",
    role: "Field User",
    mobile: "9876543210",
    avatar: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80",
  },
  {
    id: "usr_2",
    email: "user2@demo.com",
    password: "Demo@123",
    name: "Priya Verma",
    role: "Supervisor",
    mobile: "9812345678",
    avatar: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150&auto=format&fit=crop&q=80",
  },
  {
    id: "usr_admin",
    email: "admin@demo.com",
    password: "Admin@123",
    name: "Admin User",
    role: "Admin",
    mobile: "9900112233",
    avatar: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&auto=format&fit=crop&q=80",
  },
];

const AUTH_STORAGE_KEY = "gff_current_user";

export const mockAuthService = {
  getDemoUsers(): DemoUser[] {
    return DEMO_USERS;
  },

  getCurrentUser(): User | null {
    if (typeof window === "undefined") return null;
    try {
      const data = localStorage.getItem(AUTH_STORAGE_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  async login(emailOrUsername: string, password: string): Promise<{ success: boolean; user?: User; error?: string }> {
    // Artificial small delay for realistic UX feedback
    await new Promise((resolve) => setTimeout(resolve, 300));

    const normalizedEmail = emailOrUsername.trim().toLowerCase();
    const user = DEMO_USERS.find(
      (u) => (u.email.toLowerCase() === normalizedEmail || u.id === normalizedEmail) && u.password === password
    );

    if (!user) {
      return {
        success: false,
        error: "Invalid credentials. Please select from the Demo accounts below.",
      };
    }

    const sessionUser: User = {
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
      mobile: user.mobile,
      avatar: user.avatar,
    };

    if (typeof window !== "undefined") {
      localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(sessionUser));
    }

    return { success: true, user: sessionUser };
  },

  logout(): void {
    if (typeof window !== "undefined") {
      localStorage.removeItem(AUTH_STORAGE_KEY);
    }
  },
};
