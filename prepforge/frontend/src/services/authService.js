import { apiClient } from "./api";

export const authService = {
  async login(payload) {
    const { data } = await apiClient.post("/api/v1/auth/login", payload);
    return data;
  },

  async register(payload) {
    const { data } = await apiClient.post("/api/v1/auth/register", payload);
    return data;
  },

  async getCurrentUser() {
    const { data } = await apiClient.get("/api/v1/users/me");
    return data;
  },
};
