import request from "./request";

export const api = {
  login: (data, config = {}) => request.post("/auth/login", data, config),
  register: (data, config = {}) => request.post("/auth/register", data, config),
  me: (config = {}) => request.get("/auth/me", config),
  get: (url, params, config = {}) => request.get(url, { params, ...config }),
  post: (url, data, config = {}) => request.post(url, data, config),
  put: (url, data, config = {}) => request.put(url, data, config),
  del: (url, config = {}) => request.delete(url, config),
  upload: (url, data, config = {}) => request.post(url, data, config)
};
