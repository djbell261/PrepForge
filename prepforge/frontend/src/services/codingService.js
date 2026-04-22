import { apiClient } from "./api";

export const codingService = {
  async getAnalytics() {
    const { data } = await apiClient.get("/api/v1/coding/analytics");
    return data;
  },

  async getQuestions() {
    const { data } = await apiClient.get("/api/v1/coding/questions");
    return data;
  },

  async getSessions() {
    const { data } = await apiClient.get("/api/v1/coding/sessions");
    return data;
  },

  async startSession(payload) {
    const { data } = await apiClient.post("/api/v1/coding/sessions", payload);
    return data;
  },

  async getSessionDetail(sessionId) {
    const { data } = await apiClient.get(`/api/v1/coding/sessions/${sessionId}`);
    return data;
  },

  async saveStrategy(sessionId, payload) {
    const { data } = await apiClient.put(`/api/v1/coding/sessions/${sessionId}/strategy`, payload);
    return data;
  },

  async evaluateStrategy(sessionId, payload) {
    const { data } = await apiClient.post(`/api/v1/coding/sessions/${sessionId}/strategy/evaluate`, payload);
    return data;
  },

  async compareApproach(sessionId) {
    const { data } = await apiClient.post(`/api/v1/coding/sessions/${sessionId}/compare`);
    return data;
  },

  async submitSolution(sessionId, payload) {
    const { data } = await apiClient.post(`/api/v1/coding/sessions/${sessionId}/submissions`, payload);
    return data;
  },

  async runCode(sessionId, payload) {
    const { data } = await apiClient.post(`/api/v1/coding/sessions/${sessionId}/run`, payload);
    return data;
  },

  async improveSubmission(submissionId) {
    const { data } = await apiClient.post(`/api/v1/coding/submissions/${submissionId}/improve`);
    return data;
  },
};
