<template>
  <div class="auth-page" :style="pageStyle">
    <div class="auth-overlay"></div>

    <section class="auth-shell">
      <aside class="auth-copy">
        <div class="brand-lockup">
          <span class="brand-en">YUTU TRAVEL</span>
          <h1>豫途旅游服务平台</h1>
        </div>

        <div
          class="hero-visual"
          :style="heroMotion"
          aria-hidden="true"
          @pointerenter="activateHero"
          @pointermove="updateHeroMotion"
          @pointerleave="resetHeroMotion"
        >
          <div class="hero-visual-glow"></div>
          <div class="hero-visual-surface">
            <img :src="loginTravelHero" alt="" class="hero-image" />
          </div>
        </div>
      </aside>

      <section class="auth-panel">
        <div class="panel-top">
          <p class="panel-title">欢迎注册</p>
          <div class="panel-badge">豫</div>
        </div>

        <el-form :model="form" class="auth-form" label-position="top" @submit.prevent>
          <el-form-item label="用户名">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" size="large" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" size="large" maxlength="11" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" size="large" show-password placeholder="请输入密码" />
          </el-form-item>

          <el-button class="submit-btn" type="primary" size="large" @click="submit">立即注册</el-button>
          <el-button class="switch-btn" text @click="$router.push('/login')">已有账号？返回登录</el-button>
        </el-form>
      </section>
    </section>
  </div>
</template>

<script setup>
import { reactive } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { api } from "../../api";
import loginTravelBg from "../../assets/login-travel-bg.svg";
import loginTravelHero from "../../assets/login-travel-hero.svg";
import { isValidPhone } from "../../utils/phone";

const pageStyle = {
  backgroundImage: `linear-gradient(116deg, rgba(6, 30, 42, 0.82), rgba(8, 53, 68, 0.42) 52%, rgba(232, 206, 160, 0.1)), url(${loginTravelBg})`
};

function createHeroMotion() {
  return {
    "--spot-x": "52%",
    "--spot-y": "42%",
    "--tilt-x": "0deg",
    "--tilt-y": "0deg",
    "--float-y": "0px",
    "--float-x": "0px",
    "--scale": "1",
    "--glow-opacity": "0.2",
    "--image-brightness": "1"
  };
}

const heroMotion = reactive(createHeroMotion());

const router = useRouter();
const form = reactive({
  username: "",
  nickname: "",
  phone: "",
  password: ""
});

function activateHero() {
  heroMotion["--scale"] = "1.015";
  heroMotion["--glow-opacity"] = "0.28";
  heroMotion["--image-brightness"] = "1.03";
}

function updateHeroMotion(event) {
  const currentTarget = event.currentTarget;
  if (!(currentTarget instanceof HTMLElement)) {
    return;
  }
  const rect = currentTarget.getBoundingClientRect();
  const px = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1);
  const py = Math.min(Math.max((event.clientY - rect.top) / rect.height, 0), 1);
  heroMotion["--spot-x"] = `${(px * 100).toFixed(2)}%`;
  heroMotion["--spot-y"] = `${(py * 100).toFixed(2)}%`;
  heroMotion["--tilt-x"] = `${((0.5 - py) * 5.2).toFixed(2)}deg`;
  heroMotion["--tilt-y"] = `${((px - 0.5) * 7.2).toFixed(2)}deg`;
  heroMotion["--float-x"] = `${((px - 0.5) * 10).toFixed(2)}px`;
  heroMotion["--float-y"] = `${(-8 - py * 4).toFixed(2)}px`;
  heroMotion["--scale"] = "1.035";
  heroMotion["--glow-opacity"] = "0.42";
  heroMotion["--image-brightness"] = "1.08";
}

function resetHeroMotion() {
  Object.assign(heroMotion, createHeroMotion());
}

async function submit() {
  if (!isValidPhone(form.phone)) {
    ElMessage.warning("手机号必须为 11 位数字");
    return;
  }
  await api.register(form);
  ElMessage.success("注册成功，请登录");
  router.push("/login");
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 28px 0;
  overflow: hidden;
  background-color: #d9dedf;
  background-position: center;
  background-repeat: no-repeat;
  background-size: cover;
}

.auth-overlay {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 14%, rgba(255, 233, 190, 0.16), transparent 22%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), rgba(255, 255, 255, 0));
}

.auth-shell {
  position: relative;
  z-index: 1;
  width: calc(100vw - 56px);
  max-width: 1280px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 430px;
  gap: 48px;
  align-items: center;
}

.auth-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 10px 0 12px;
}

.brand-lockup {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
}

.brand-en {
  color: rgba(245, 241, 231, 0.78);
  font-size: 12px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
}

.brand-lockup h1 {
  margin: 0;
  color: #fbf7ee;
  font-size: clamp(32px, 4vw, 58px);
  line-height: 1.14;
  font-family: "STZhongsong", "Songti SC", "Noto Serif SC", "Source Han Serif SC", serif;
  font-weight: 700;
  letter-spacing: -0.02em;
  text-wrap: balance;
  text-shadow: 0 8px 24px rgba(7, 24, 33, 0.16);
}

.hero-visual {
  position: relative;
  width: min(100%, 860px);
  margin-left: -28px;
  padding-top: 4px;
  cursor: pointer;
  --spot-x: 52%;
  --spot-y: 42%;
  --tilt-x: 0deg;
  --tilt-y: 0deg;
  --float-y: 0px;
  --float-x: 0px;
  --scale: 1;
  --glow-opacity: 0.2;
  --image-brightness: 1;
}

.hero-visual-glow {
  position: absolute;
  inset: 0 8% 8% 2%;
  background:
    radial-gradient(circle at var(--spot-x) var(--spot-y), rgba(255, 231, 181, 0.34), transparent 16%),
    radial-gradient(circle at var(--spot-x) var(--spot-y), rgba(255, 248, 230, 0.14), transparent 34%);
  opacity: var(--glow-opacity);
  transition: opacity 180ms ease;
  pointer-events: none;
}

.hero-visual-surface {
  position: relative;
  transform:
    translateX(calc(-8px + var(--float-x)))
    translateY(var(--float-y))
    perspective(1400px)
    rotateX(var(--tilt-x))
    rotateY(var(--tilt-y))
    scale(var(--scale));
  transform-origin: center center;
  transition: transform 180ms ease, filter 180ms ease;
  will-change: transform, filter;
}

.hero-image {
  display: block;
  width: 100%;
  height: auto;
  filter:
    brightness(var(--image-brightness))
    saturate(1.03)
    drop-shadow(0 24px 40px rgba(7, 25, 34, 0.14));
  transition: filter 180ms ease;
  pointer-events: none;
  user-select: none;
}

.auth-panel {
  width: 430px;
  max-width: 100%;
  margin-left: auto;
  padding: 30px 28px 24px;
  border-radius: 30px;
  background: rgba(248, 243, 234, 0.64);
  box-shadow: 0 24px 64px rgba(6, 22, 31, 0.18);
  backdrop-filter: blur(22px);
}

.panel-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-title {
  margin: 0;
  color: #18303b;
  font-size: 32px;
  font-weight: 900;
}

.panel-badge {
  width: 62px;
  height: 62px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0f5265;
  font-size: 24px;
  font-weight: 900;
  background: linear-gradient(135deg, #f7d597, #f3e7cd);
}

.auth-form :deep(.el-form-item) {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}

.auth-form :deep(.el-form-item__label) {
  display: flex;
  align-items: center;
  min-height: 50px;
  padding: 0;
  color: #314750;
  font-weight: 700;
}

.auth-form :deep(.el-form-item__label-wrap) {
  width: 72px;
  margin: 0;
  flex: 0 0 72px;
}

.auth-form :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}

:deep(.el-input__wrapper) {
  min-height: 50px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 0 0 1px rgba(24, 48, 59, 0.08) inset;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(14, 93, 118, 0.55) inset;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, #0f6077, #1f89a0);
}

.switch-btn {
  width: 100%;
  margin-top: 12px;
  color: #405962;
}

@media (max-width: 960px) {
  .auth-shell {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .hero-visual {
    width: min(100%, 760px);
    margin-left: -12px;
  }

  .auth-panel {
    margin: 0 auto;
  }
}

@media (max-width: 720px) {
  .auth-page {
    padding: 20px 0;
  }

  .auth-shell {
    width: min(100vw - 24px, 100%);
  }

  .auth-copy {
    display: none;
  }

  .auth-panel {
    padding: 24px 18px 18px;
  }

  .panel-title {
    font-size: 28px;
  }

  .auth-form :deep(.el-form-item) {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .auth-form :deep(.el-form-item__label-wrap) {
    width: 100%;
    flex-basis: auto;
  }

  .auth-form :deep(.el-form-item__label) {
    min-height: auto;
    padding-bottom: 8px;
  }
}
</style>
