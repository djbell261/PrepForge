import axios from "axios";

const TOKEN_KEY = "prepforge_token";
const UNAUTHORIZED_EVENT = "prepforge:unauthorized";

// Change VITE_API_BASE_URL in frontend/.env when your backend runs on a different host or port.
const rawBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const baseURL = rawBaseUrl.replace(/\/+$/, "");

export const getStoredToken = () => localStorage.getItem(TOKEN_KEY);

export const setStoredToken = (token) => {
  localStorage.setItem(TOKEN_KEY, token);
};

export const clearStoredToken = () => {
  localStorage.removeItem(TOKEN_KEY);
};

export const extractApiErrorMessage = (error, fallbackMessage) =>
  error?.response?.data?.message || fallbackMessage;

export const onUnauthorized = (handler) => {
  window.addEventListener(UNAUTHORIZED_EVENT, handler);

  return () => window.removeEventListener(UNAUTHORIZED_EVENT, handler);
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

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error?.response?.status === 401) {
      clearStoredToken();
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }

    return Promise.reject(error);
  },
);
