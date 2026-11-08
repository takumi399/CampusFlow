<script setup>
import { useRoute, useRouter } from 'vue-router'
import {
  Calendar,
  SwitchButton,
  Tickets,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { to: '/events', label: '活动广场', icon: Calendar },
  { to: '/me/registrations', label: '我的票夹', icon: Tickets },
]

function isActive(path) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-frame">
    <a class="skip-link" href="#main">跳到主要内容</a>

    <header class="topbar">
      <div class="topbar-inner">
        <router-link to="/events" class="brand" aria-label="CampusFlow 活动广场">
          <span class="brand-mark">
            C
            <span>C</span>
          </span>
          <span class="brand-copy">
            <strong>CampusFlow</strong>
            <small>青禾活动 · 今天碰个面</small>
          </span>
        </router-link>

        <nav class="nav-links" aria-label="主要导航">
          <router-link
            v-for="item in navItems"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ active: isActive(item.to) }"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </router-link>
        </nav>

        <div class="user-area">
          <span class="role-sticker">STUDENT</span>
          <span class="user-name">{{ auth.user?.displayName }}</span>
          <button
            class="logout-button"
            type="button"
            aria-label="退出登录"
            @click="logout"
          >
            <el-icon><SwitchButton /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main id="main" class="page-main">
      <slot />
    </main>

    <footer class="site-foot">
      <span>CAMPUSFLOW / CAMPUS EDITION 2026</span>
      <span>让好活动找到真正想去的人</span>
    </footer>
  </div>
</template>

<style scoped>
.app-frame {
  min-height: 100dvh;
}

.skip-link {
  position: fixed;
  z-index: 999;
  top: 8px;
  left: 8px;
  border: 2px solid var(--ink);
  border-radius: 8px;
  padding: 10px 14px;
  background: var(--butter);
  font-weight: 800;
  transform: translateY(-160%);
}

.skip-link:focus {
  transform: translateY(0);
}

.topbar {
  position: sticky;
  z-index: var(--z-nav);
  top: 0;
  border-bottom: 2px solid var(--ink);
  background: color-mix(in srgb, var(--surface) 94%, transparent);
  -webkit-backdrop-filter: blur(14px);
  backdrop-filter: blur(14px);
}

.topbar-inner {
  display: flex;
  width: min(1240px, calc(100% - 48px));
  min-height: var(--nav-h);
  align-items: center;
  gap: 28px;
  margin: 0 auto;
}

.brand {
  display: inline-flex;
  min-width: 0;
  flex: none;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  position: relative;
  display: grid;
  width: 44px;
  height: 44px;
  flex: none;
  place-items: center;
  border: 2px solid var(--ink);
  border-radius: 12px 12px 12px 4px;
  color: var(--surface);
  background: var(--cobalt);
  box-shadow: 3px 3px 0 var(--ink);
  font-family: var(--display);
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
  transform: rotate(-3deg);
}

.brand-mark span {
  position: absolute;
  top: -8px;
  right: -7px;
  display: grid;
  width: 17px;
  height: 17px;
  place-items: center;
  border: 1.5px solid var(--ink);
  border-radius: 50%;
  color: var(--ink);
  background: var(--butter);
  font-family: var(--mono);
  font-size: 8px;
  font-weight: 900;
}

.brand-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.05;
}

.brand-copy strong {
  font-family: var(--display);
  font-size: 18px;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.brand-copy small {
  overflow: hidden;
  margin-top: 5px;
  color: var(--ink-3);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-links {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 5px;
}

.nav-link {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  gap: 6px;
  border: 1.5px solid transparent;
  border-radius: 999px;
  padding: 8px 13px;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 800;
  line-height: 1;
  white-space: nowrap;
  transition:
    transform 150ms,
    background 150ms,
    color 150ms,
    box-shadow 150ms;
}

.nav-link:hover {
  border-color: var(--ink);
  color: var(--ink);
  background: var(--butter-soft);
  transform: rotate(-1deg);
}

.nav-link.active {
  border-color: var(--ink);
  color: var(--surface);
  background: var(--cobalt);
  box-shadow: 2px 2px 0 var(--ink);
}

.user-area {
  display: flex;
  flex: none;
  align-items: center;
  gap: 9px;
}

.role-sticker {
  border: 1.5px solid var(--ink);
  border-radius: 5px;
  padding: 4px 7px;
  background: var(--mint);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.06em;
  transform: rotate(2deg);
}

.user-name {
  color: var(--ink);
  font-size: 13px;
  font-weight: 750;
}

.logout-button {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1.5px solid var(--ink);
  border-radius: 50%;
  background: var(--surface);
  cursor: pointer;
  transition:
    transform 150ms,
    background 150ms;
}

.logout-button:hover {
  background: var(--tomato-soft);
  transform: rotate(-8deg);
}

.page-main {
  min-height: calc(100dvh - var(--nav-h) - 100px);
}

.site-foot {
  display: flex;
  width: min(1240px, calc(100% - 48px));
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin: 0 auto;
  border-top: 1.5px solid var(--ink);
  padding: 22px 0 28px;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.site-foot span:nth-child(2) {
  font-family: var(--sans);
  letter-spacing: 0;
}

@media (max-width: 900px) {
  .topbar-inner {
    flex-wrap: wrap;
    gap: 10px 18px;
    padding: 12px 0 0;
  }

  .nav-links {
    width: 100%;
    flex: 0 0 100%;
    order: 3;
    overflow-x: auto;
    padding-bottom: 10px;
    scrollbar-width: none;
  }

  .nav-links::-webkit-scrollbar {
    display: none;
  }

  .user-area {
    margin-left: auto;
  }
}

@media (max-width: 560px) {
  .topbar-inner,
  .site-foot {
    width: min(100% - 28px, 1240px);
  }

  .brand-mark {
    width: 40px;
    height: 40px;
  }

  .brand-copy strong {
    font-size: 16px;
  }

  .brand-copy small,
  .user-name,
  .role-sticker {
    display: none;
  }

  .nav-link {
    min-height: 44px;
  }

  .site-foot {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
}
</style>
