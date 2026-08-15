import axios from "axios";

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || "http://localhost:8080"
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("sf_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const register = (payload) => api.post("/auth/register", payload);
export const login = (payload) => api.post("/auth/login", payload);
export const generateRoadmap = (payload) => api.post("/ai/generate-roadmap", payload);
export const getRoadmaps = () => api.get("/ai/roadmaps");
export const getModules = (roadmapId) => api.get(`/modules/${roadmapId}`);
export const completeModule = (moduleId) => api.put(`/modules/${moduleId}/complete`);
export const getProgress = (roadmapId) => api.get(`/progress/${roadmapId}`);

export default api;
