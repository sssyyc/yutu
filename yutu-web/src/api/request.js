import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 15000
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("yutu_token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (resp) => {
    const res = resp.data;
    if (res.code !== 0) {
      if (!resp.config?.silent) {
        ElMessage.error(res.message || "请求失败");
      }
      return Promise.reject(new Error(res.message || "Error"));
    }
    return res.data;
  },
  (error) => {
    const status = error?.response?.status;
    const serverMessage = error?.response?.data?.message;
    if (status === 401) {
      localStorage.removeItem("yutu_token");
      if (location.pathname !== "/login") {
        location.href = "/login";
      }
    } else if (!error?.config?.silent) {
      ElMessage.error(
        serverMessage ||
          (status === 500 ? "后端服务异常，请检查 yutu-admin 控制台日志" : error?.message || "请求失败")
      );
    }
    return Promise.reject(error);
  }
);

export default request;
