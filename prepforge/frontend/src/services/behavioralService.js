import { apiClient } from "./api";

export const behavioralService = {
  async getAnalytics() {
    const { data } = await apiClient.get("/api/v1/behavioral/analytics/me");
    return data;
  },

  async getQuestions() {
    const { data } = await apiClient.get("/api/v1/behavioral/questions");
    return data;
  },

  async startSession(payload) {
    const { data } = await apiClient.post("/api/v1/behavioral/sessions", payload);
    return data;
  },

  async getSessionDetail(sessionId) {
    const { data } = await apiClient.get(`/api/v1/behavioral/sessions/${sessionId}`);
    return data;
  },

  async getSessionSummary(sessionId) {
    const { data } = await apiClient.get(`/api/v1/behavioral/sessions/${sessionId}/summary`);
    return data;
  },

  async submitResponse(sessionId, payload) {
    const { data } = await apiClient.post("/api/v1/behavioral/submissions", {
      sessionId,
      responseText: payload.responseText,
    });
    return data;
  },

  async improveResponse(payload) {
    const { data } = await apiClient.post("/api/v1/behavioral/improve", payload);
    return data;
  },
};
