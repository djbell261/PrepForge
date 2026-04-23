import { apiClient } from "./api";

export const dashboardService = {
  async getCoachingSummary() {
    const { data } = await apiClient.get("/api/v1/dashboard/coaching-summary");
    return data;
  },
};
