import { apiClient } from "./api";

export const codingService = {
  async getAnalytics() {
    const { data } = await apiClient.get("/api/v1/coding/analytics");
    return data;
  },

  async getSessions() {
    const { data } = await apiClient.get("/api/v1/coding/sessions");
    return data;
  },

  async getSessionDetail(sessionId) {
    const { data } = await apiClient.get(`/api/v1/coding/sessions/${sessionId}`);
    return data;
  },
};
