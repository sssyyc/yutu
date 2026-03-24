<template>
  <el-container class="user-layout">
    <el-header class="user-header">
      <div class="header-card">
        <div class="header-inner">
          <div class="header-left">
            <div class="brand-block">
              <div class="brand-mark">YT</div>
              <div class="brand-copy">
                <div class="brand-title">豫途 · 用户端</div>
                <div class="brand-subtitle">Travel Center</div>
              </div>
            </div>

            <div class="city-card">
              <div class="city-label">当前城市</div>
              <button class="city-trigger" type="button" :disabled="locatingCity" @click="locateCurrentCity">
                <el-icon><LocationFilled /></el-icon>
                <span>{{ currentCity }}</span>
              </button>
            </div>
          </div>

          <div class="search-panel">
            <div class="search-shell">
              <el-icon class="search-leading"><Search /></el-icon>
              <input
                v-model="searchKeyword"
                class="search-input"
                type="text"
                placeholder="搜索路线、景点、目的地"
                @keyup.enter="submitSearch"
              />
              <button class="search-btn" type="button" @click="submitSearch">搜索</button>
            </div>
            <div class="hot-searches">
              <span class="hot-label">热门搜索</span>
              <button
                v-for="item in hotKeywords"
                :key="item"
                type="button"
                class="hot-tag"
                @click="quickSearch(item)"
              >
                {{ item }}
              </button>
            </div>
          </div>

          <div class="header-actions">
            <el-button
              v-if="auth.user?.roleType === 2"
              plain
              class="switch-btn"
              @click="router.push('/merchant')"
            >
              商户端
            </el-button>
            <el-button
              v-if="auth.user?.roleType === 3"
              plain
              class="switch-btn"
              @click="router.push('/admin')"
            >
              管理端
            </el-button>

            <el-dropdown trigger="click">
              <div class="profile-trigger">
                <el-avatar :size="42" :src="auth.user?.avatar">
                  {{ avatarFallback }}
                </el-avatar>
                <div class="profile-meta">
                  <div class="profile-name">{{ auth.user?.nickname || auth.user?.username || "用户" }}</div>
                  <div class="profile-role">普通用户</div>
                </div>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="router.push('/user/profile')">个人中心</el-dropdown-item>
                  <el-dropdown-item @click="router.push('/user/travelers')">出行人管理</el-dropdown-item>
                  <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </div>

      <div class="nav-card">
        <div class="menu-scroll">
          <el-menu :default-active="currentRoute.path" class="user-menu" mode="horizontal" router :ellipsis="false">
            <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
              <span class="menu-icon-badge">
                <el-icon><component :is="item.icon" /></el-icon>
              </span>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-menu>
        </div>
      </div>
    </el-header>

    <el-main class="user-main">
      <div class="main-shell">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from "vue"
import {
  House,
  LocationFilled,
  MapLocation,
  Notebook,
  Search,
  Star,
  Tickets,
  UserFilled,
  Warning
} from "@element-plus/icons-vue"
import { useRoute, useRouter } from "vue-router"
import { useAuthStore } from "../stores/auth"

const auth = useAuthStore()
const router = useRouter()
const currentRoute = useRoute()

const hotKeywords = ["郑州", "洛阳", "少林寺", "龙门石窟"]
const searchKeyword = ref("")
const currentCity = ref(localStorage.getItem("yutu-current-city") || "定位中")
const cityTip = ref("正在获取位置")
const locatingCity = ref(false)

const menuItems = [
  { path: "/", label: "首页", icon: House },
  { path: "/route/list", label: "路线浏览", icon: MapLocation },
  { path: "/favorite", label: "我的收藏", icon: Star },
  { path: "/order/list", label: "我的订单", icon: Tickets },
  { path: "/contract/list", label: "我的合同", icon: Notebook },
  { path: "/complaint/list", label: "我的投诉", icon: Warning },
  { path: "/user/travelers", label: "出行人管理", icon: UserFilled }
]

const avatarFallback = computed(() => {
  const text = auth.user?.nickname || auth.user?.username || "用"
  return text.slice(0, 1).toUpperCase()
})

function syncSearchFromRoute() {
  if (currentRoute.path === "/route/list") {
    searchKeyword.value = typeof currentRoute.query.keyword === "string" ? currentRoute.query.keyword : ""
  }
}

async function submitSearch() {
  const keyword = searchKeyword.value.trim()
  await router.push({
    path: "/route/list",
    query: keyword ? { keyword } : {}
  })
}

async function quickSearch(keyword) {
  searchKeyword.value = keyword
  await submitSearch()
}

function logout() {
  auth.logout()
  location.href = "/login"
}

function getBrowserPosition() {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error("geolocation unavailable"))
      return
    }
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 8000,
      maximumAge: 600000
    })
  })
}

async function reverseGeocodeCity(latitude, longitude) {
  const response = await fetch(
    `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}&accept-language=zh-CN`,
    {
      headers: {
        Accept: "application/json"
      }
    }
  )
  if (!response.ok) {
    throw new Error("reverse geocode failed")
  }
  const payload = await response.json()
  const address = payload?.address || {}
  return (
    address.city ||
    address.town ||
    address.county ||
    address.state ||
    address.region ||
    "郑州"
  )
}

async function locateByIp() {
  const response = await fetch("https://ipapi.co/json/")
  if (!response.ok) {
    throw new Error("ip locate failed")
  }
  const payload = await response.json()
  return payload?.city || payload?.region || "郑州"
}

async function locateCurrentCity() {
  locatingCity.value = true
  cityTip.value = "定位中，请稍候"

  try {
    const position = await getBrowserPosition()
    const city = await reverseGeocodeCity(position.coords.latitude, position.coords.longitude)
    currentCity.value = city
    cityTip.value = "已根据当前位置自动识别"
  } catch (error) {
    try {
      const city = await locateByIp()
      currentCity.value = city
      cityTip.value = "已切换为网络定位结果"
    } catch (fallbackError) {
      currentCity.value = currentCity.value === "定位中" ? "郑州" : currentCity.value
      cityTip.value = "定位失败，已为你保留当前城市"
    }
  } finally {
    localStorage.setItem("yutu-current-city", currentCity.value)
    locatingCity.value = false
  }
}

watch(
  () => currentRoute.fullPath,
  () => {
    syncSearchFromRoute()
  },
  { immediate: true }
)

onMounted(() => {
  if (!localStorage.getItem("yutu-current-city")) {
    locateCurrentCity()
  } else {
    cityTip.value = "点击城市可重新定位"
  }
})
</script>

<style scoped>
.user-layout {
  --user-primary: #4b9d90;
  --user-primary-strong: #2f7f74;
  --user-primary-soft: #edf8f5;
  --user-primary-surface: #f8fcfb;
  --user-primary-line: #d9ebe6;
  --user-accent: #e7b35a;
  --user-accent-soft: #fff6df;
  --user-text: #142235;
  --user-muted: #6d7b88;
  --user-card-border: rgba(20, 64, 89, 0.08);
  --user-card-shadow: 0 14px 34px rgba(20, 43, 60, 0.06);
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(75, 157, 144, 0.12), transparent 28%),
    radial-gradient(circle at top right, rgba(231, 179, 90, 0.1), transparent 24%),
    #f2f6f5;
}

.user-header {
  height: auto;
  padding: 18px 24px 16px;
  background: rgba(247, 249, 248, 0.9);
  backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 20;
}

.header-card,
.nav-card {
  max-width: 1440px;
  margin: 0 auto;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(247, 251, 250, 0.94));
  border: 1px solid var(--user-card-border);
  box-shadow: var(--user-card-shadow);
}

.header-card {
  padding: 18px 22px;
  border-radius: 28px;
}

.nav-card {
  margin-top: 14px;
  padding: 8px 14px;
  border-radius: 24px;
}

.header-inner {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(360px, 560px) auto;
  gap: 22px;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  flex: 1 1 auto;
}

.brand-mark {
  width: 50px;
  height: 50px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f7ca6b, var(--user-accent));
  color: #163347;
  font-weight: 900;
  font-size: 20px;
  box-shadow: 0 14px 28px rgba(231, 179, 90, 0.26);
}

.brand-title {
  font-size: 26px;
  font-weight: 900;
  color: var(--user-text);
  line-height: 1.1;
}

.brand-subtitle {
  margin-top: 4px;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: var(--user-muted);
  text-transform: uppercase;
}

.city-card {
  min-width: 160px;
  max-width: 180px;
}

.city-label {
  font-size: 12px;
  color: var(--user-muted);
}

.city-trigger {
  margin-top: 6px;
  width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid var(--user-primary-line);
  border-radius: 16px;
  background: linear-gradient(135deg, var(--user-primary-soft), #ffffff);
  color: var(--user-primary-strong);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.city-trigger:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 22px rgba(75, 157, 144, 0.12);
}

.city-trigger:disabled {
  cursor: default;
  opacity: 0.75;
}

.search-panel {
  min-width: 0;
  align-self: stretch;
  display: flex;
  flex-direction: column;
  justify-content: center;
  justify-self: center;
  width: min(100%, 560px);
}

.search-shell {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 10px;
  padding: 6px 6px 6px 12px;
  border-radius: 18px;
  border: 2px solid var(--user-primary);
  background: #ffffff;
  box-shadow: 0 12px 28px rgba(75, 157, 144, 0.08);
}

.search-leading {
  color: var(--user-primary);
  font-size: 18px;
}

.search-input {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
  color: var(--user-text);
  font-size: 14px;
}

.search-input::placeholder {
  color: #95a3af;
}

.search-btn {
  min-width: 96px;
  height: 38px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--user-primary), var(--user-primary-strong));
  color: #ffffff;
  font-size: 16px;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 10px 20px rgba(75, 157, 144, 0.16);
}

.hot-searches {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  align-items: center;
}

.hot-label {
  color: var(--user-muted);
  font-size: 13px;
}

.hot-tag {
  border: none;
  border-radius: 999px;
  padding: 6px 10px;
  background: var(--user-primary-soft);
  color: var(--user-primary-strong);
  font-size: 12px;
  cursor: pointer;
}

.hot-tag:hover {
  background: rgba(75, 157, 144, 0.18);
  color: var(--user-primary-strong);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}

.switch-btn {
  border-color: rgba(20, 64, 89, 0.12);
  color: var(--user-primary-strong);
  background: rgba(255, 255, 255, 0.78);
  border-radius: 999px;
  padding-inline: 18px;
}

.switch-btn:hover {
  border-color: var(--user-primary-line);
  color: var(--user-primary-strong);
  background: var(--user-primary-soft);
}

.profile-trigger {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 18px;
  cursor: pointer;
  transition: background 0.2s ease, transform 0.2s ease;
}

.profile-trigger:hover {
  background: var(--user-primary-soft);
  transform: translateY(-1px);
}

.profile-name {
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--user-text);
  font-size: 18px;
  font-weight: 700;
}

.profile-role {
  margin-top: 3px;
  color: var(--user-muted);
  font-size: 12px;
}

.menu-scroll {
  margin: 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 4px;
}

.menu-scroll::-webkit-scrollbar {
  height: 6px;
}

.menu-scroll::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.5);
  border-radius: 999px;
}

.user-menu {
  width: max-content;
  min-width: 100%;
  border: none;
  background: transparent;
  display: flex;
  gap: 10px;
}

.user-menu :deep(.el-menu-item) {
  height: 50px;
  border-bottom: none !important;
  border-radius: 16px;
  color: #334155;
  padding: 0 18px;
  font-size: 15px;
  font-weight: 700;
}

.user-menu :deep(.el-menu-item:hover) {
  color: var(--user-text);
  background: var(--user-primary-surface);
}

.user-menu :deep(.el-menu-item.is-active) {
  color: #ffffff;
  background: linear-gradient(135deg, var(--user-primary-strong), var(--user-primary));
  box-shadow: 0 12px 24px rgba(75, 157, 144, 0.2);
}

.user-menu :deep(.el-menu-item.is-active),
.user-menu :deep(.el-menu-item.is-active > span),
.user-menu :deep(.el-menu-item.is-active .el-icon) {
  color: #ffffff !important;
}

.menu-icon-badge {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  border-radius: 10px;
  background: var(--user-primary-soft);
  color: var(--user-primary);
  font-size: 15px;
}

.user-menu :deep(.el-menu-item.is-active) .menu-icon-badge {
  background: rgba(255, 255, 255, 0.16);
  color: #ffffff;
}

.user-main {
  padding: 24px;
}

.main-shell {
  max-width: 1440px;
  margin: 0 auto;
}

@media (max-width: 1320px) {
  .header-inner {
    grid-template-columns: 1fr;
    align-items: flex-start;
  }

  .header-left {
    width: 100%;
    justify-content: space-between;
  }

  .header-actions {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .user-header {
    padding: 16px 16px 12px;
  }

  .header-card,
  .nav-card {
    border-radius: 22px;
  }

  .header-card {
    padding: 16px;
  }

  .nav-card {
    padding: 8px 10px;
  }

  .brand-title {
    font-size: 22px;
  }

  .header-left {
    flex-direction: column;
    align-items: flex-start;
  }

  .city-card {
    max-width: none;
    width: 100%;
  }

  .search-shell {
    grid-template-columns: auto 1fr;
  }

  .search-btn {
    grid-column: 1 / -1;
    width: 100%;
  }

  .profile-name {
    max-width: 120px;
    font-size: 16px;
  }

  .user-menu :deep(.el-menu-item) {
    height: 44px;
    padding: 0 14px;
    font-size: 14px;
  }

  .user-main {
    padding: 16px;
  }
}
</style>
