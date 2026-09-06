/**
 * Deprecated: All authentication is now handled exclusively by backend PostgreSQL and Spring Boot.
 * Refer to authService.ts.
 */
import { authService } from "./authService";

export const mockAuthService = {
  getCurrentUser() {
    return null;
  },
  logout() {
    return authService.logout();
  },
};
