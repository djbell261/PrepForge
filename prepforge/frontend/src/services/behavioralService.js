import { apiClient } from "./api";

export const behavioralService = {
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

  async submitResponse(sessionId, payload) {
    const { data } = await apiClient.post(`/api/v1/behavioral/sessions/${sessionId}/submissions`, payload);
    return data;
  },
};
