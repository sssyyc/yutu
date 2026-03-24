import { defineStore } from "pinia";
import { api } from "../api";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("yutu_token") || "",
    user: null,
    perms: []
  }),
  actions: {
    async login(form) {
      const data = await api.login(form);
      this.token = data.token;
      localStorage.setItem("yutu_token", data.token);
      await this.fetchMe();
      return data;
    },
    async fetchMe() {
      if (!this.token) return null;
      const data = await api.me();
      this.user = data;
      this.perms = data.perms || [];
      return data;
    },
    hasPerm(perm) {
      return this.perms.includes(perm);
    },
    logout() {
      this.token = "";
      this.user = null;
      this.perms = [];
      localStorage.removeItem("yutu_token");
    }
  }
});
