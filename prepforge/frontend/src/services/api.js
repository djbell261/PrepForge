import axios from "axios";

const TOKEN_KEY = "prepforge_token";

// Change VITE_API_BASE_URL in frontend/.env when your backend runs on a different host or port.
const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY);

export const setStoredToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearStoredToken = () => {
  localStorage.removeItem(TOKEN_KEY);
};

export const apiClient = axios.create({
  baseURL,
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const token = getStoredToken();

  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  return config;
});
