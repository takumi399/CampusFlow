<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Calendar,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const navItems = [
  { to: '/admin/events', label: '活动管理', meta: 'Events', icon: Calendar },
]

const currentLabel = computed(() =>
  route.name === 'admin-event-detail' ? '活动详情' : '活动管理',
)

const accountInitial = computed(
  () => auth.user?.displayName?.slice(0, 1) || auth.user?.username?.slice(0, 1) || 'A',
)

const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'short',
}).format(new Date())

function isActive(path) {
  return path === '/admin/events'
    ? route.path === path || route.path.startsWith(`${path}/`)
    : route.path === path
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => document.body.classList.add('admin-route'))
onBeforeUnmount(() => document.body.classList.remove('admin-route'))
</script>

<template>
  <div class="admin-shell">
    <a class="admin-skip-link" href="#admin-main">跳到主要内容</a>

    <aside class="admin-sidebar">
      <router-link to="/admin/events" class="admin-brand" aria-label="CampusFlow 活动运营中心">
        <span class="admin-brand-mark">C</span>
        <span class="admin-brand-copy">
          <strong>CampusFlow</strong>
          <small>活动运营中心</small>
        </span>
      </router-link>

      <div class="admin-workspace-label">
        <span>WORKSPACE</span>
        <i aria-hidden="true"></i>
      </div>

      <nav class="admin-nav" aria-label="管理端导航">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="admin-nav-link"
          :class="{ active: isActive(item.to) }"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span class="admin-nav-copy">
            <strong>{{ item.label }}</strong>
            <small>{{ item.meta }}</small>
          </span>
        </router-link>
      </nav>

      <div class="admin-sidebar-note">
        <span>运营提醒</span>
        <p>活动发布后核心信息将锁定，报名与签到数据会持续同步。</p>
      </div>

      <div class="admin-account">
        <span class="admin-avatar">{{ accountInitial }}</span>
        <span class="admin-account-copy">
          <strong>{{ auth.user?.displayName || '管理员' }}</strong>
          <small>活动运营 · 管理员</small>
        </span>
        <button type="button" aria-label="退出登录" @click="logout">
          <el-icon><SwitchButton /></el-icon>
        </button>
      </div>
    </aside>

    <div class="admin-workspace">
      <header class="admin-topbar">
        <div class="admin-breadcrumb" aria-label="面包屑">
          <span>CampusFlow</span>
          <i aria-hidden="true">/</i>
          <router-link to="/admin/events">运营中心</router-link>
          <i aria-hidden="true">/</i>
          <strong>{{ currentLabel }}</strong>
        </div>

        <div class="admin-topbar-meta">
          <span class="admin-environment">
            <i aria-hidden="true"></i>
            运营端
          </span>
          <span class="admin-date">{{ todayLabel }}</span>
        </div>
      </header>

      <main id="admin-main" class="admin-main">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  --admin-page: #eef1f5;
  --admin-panel: #fbfcfe;
  --admin-panel-strong: #ffffff;
  --admin-sidebar: #111c2f;
  --admin-sidebar-soft: #17253c;
  --admin-sidebar-border: rgba(255, 255, 255, 0.09);
  --admin-rail-text: #f4f7fb;
  --admin-rail-muted: #94a2b8;
  --admin-ink: #162033;
  --admin-ink-2: #46556c;
  --admin-ink-3: #6f7d92;
  --admin-border: rgba(23, 38, 64, 0.1);
  --admin-border-strong: rgba(23, 38, 64, 0.17);
  --admin-accent: #2d5bd8;
  --admin-accent-deep: #2146ad;
  --admin-accent-soft: rgba(45, 91, 216, 0.09);
  --admin-amber: #a86414;
  --admin-amber-soft: rgba(183, 122, 22, 0.1);
  --admin-up: #16744a;
  --admin-up-soft: rgba(22, 116, 74, 0.1);
  --admin-down: #b93e37;
  --admin-down-soft: rgba(185, 62, 55, 0.09);
  --admin-shadow: 0 1px 2px rgba(17, 28, 47, 0.04), 0 8px 24px rgba(17, 28, 47, 0.035);
  --admin-radius: 14px;
  display: grid;
  min-height: 100dvh;
  grid-template-columns: 244px minmax(0, 1fr);
  color: var(--admin-ink);
  background: var(--admin-page);
  font-family: var(--sans);
}

.admin-skip-link {
  position: fixed;
  z-index: 999;
  top: 10px;
  left: 10px;
  border-radius: 7px;
  padding: 10px 14px;
  color: var(--admin-rail-text);
  background: var(--admin-sidebar);
  font-size: 13px;
  font-weight: 650;
  transform: translateY(-160%);
}

.admin-skip-link:focus {
  transform: translateY(0);
}

.admin-sidebar {
  position: sticky;
  top: 0;
  display: flex;
  overflow: hidden;
  height: 100dvh;
  flex-direction: column;
  padding: 22px 16px 18px;
  color: var(--admin-rail-text);
  background:
    linear-gradient(180deg, rgba(45, 91, 216, 0.16), transparent 28%),
    var(--admin-sidebar);
  isolation: isolate;
}

.admin-sidebar::before {
  position: absolute;
  z-index: -1;
  top: -140px;
  left: 65px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  background: rgba(45, 91, 216, 0.22);
  filter: blur(70px);
  content: "";
}

.admin-sidebar::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 1px;
  background: var(--admin-sidebar-border);
  content: "";
}

.admin-brand {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  border-radius: 10px;
  padding: 4px 5px;
}

.admin-brand-mark {
  display: grid;
  width: 36px;
  height: 36px;
  flex: none;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 9px;
  color: #fff;
  background: linear-gradient(145deg, #4f78e8, #244bbf);
  box-shadow: 0 7px 20px rgba(10, 26, 64, 0.34);
  font-family: var(--display);
  font-size: 20px;
  font-weight: 900;
}

.admin-brand-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  line-height: 1.1;
}

.admin-brand-copy strong {
  color: #fff;
  font-size: 16px;
  font-weight: 720;
  letter-spacing: -0.025em;
}

.admin-brand-copy small {
  margin-top: 4px;
  color: var(--admin-rail-muted);
  font-size: 10px;
  font-weight: 500;
}

.admin-workspace-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 37px 10px 12px;
  color: #72839b;
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 650;
  letter-spacing: 0.12em;
}

.admin-workspace-label i {
  height: 1px;
  flex: 1;
  background: var(--admin-sidebar-border);
}

.admin-nav {
  display: grid;
  gap: 5px;
}

.admin-nav-link {
  position: relative;
  display: flex;
  min-height: 54px;
  align-items: center;
  gap: 11px;
  border: 1px solid transparent;
  border-radius: 9px;
  padding: 9px 10px;
  color: #b7c2d1;
  transition:
    color 160ms ease,
    background 160ms ease,
    border-color 160ms ease;
}

.admin-nav-link:hover {
  border-color: var(--admin-sidebar-border);
  color: #fff;
  background: rgba(255, 255, 255, 0.045);
}

.admin-nav-link.active {
  border-color: rgba(122, 153, 235, 0.26);
  color: #fff;
  background: linear-gradient(90deg, rgba(45, 91, 216, 0.4), rgba(45, 91, 216, 0.13));
}

.admin-nav-link.active::before {
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: -17px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: #7d9dfa;
  content: "";
}

.admin-nav-link > .el-icon {
  width: 30px;
  height: 30px;
  flex: none;
  border-radius: 7px;
  color: #aab8ca;
  background: rgba(255, 255, 255, 0.055);
  font-size: 16px;
}

.admin-nav-link.active > .el-icon {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.admin-nav-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.admin-nav-copy strong {
  font-size: 13px;
  font-weight: 620;
}

.admin-nav-copy small {
  color: #7f8fa6;
  font-size: 10px;
  font-weight: 450;
}

.admin-nav-link.active .admin-nav-copy small {
  color: #adc0ec;
}

.admin-sidebar-note {
  margin-top: auto;
  border: 1px solid var(--admin-sidebar-border);
  border-radius: 9px;
  padding: 13px;
  background: rgba(255, 255, 255, 0.035);
}

.admin-sidebar-note span {
  color: #dbe4ef;
  font-size: 11px;
  font-weight: 650;
}

.admin-sidebar-note p {
  margin: 6px 0 0;
  color: #8291a6;
  font-size: 10px;
  line-height: 1.65;
}

.admin-account {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 12px;
  border-top: 1px solid var(--admin-sidebar-border);
  padding: 15px 4px 0;
}

.admin-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  flex: none;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 50%;
  color: #dfe7f4;
  background: var(--admin-sidebar-soft);
  font-size: 13px;
  font-weight: 650;
}

.admin-account-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.admin-account-copy strong {
  overflow: hidden;
  color: #f0f4f9;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.admin-account-copy small {
  color: #7f8ea4;
  font-size: 9px;
}

.admin-account button {
  display: grid;
  width: 30px;
  height: 30px;
  flex: none;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 7px;
  color: #8695aa;
  background: transparent;
  cursor: pointer;
}

.admin-account button:hover {
  border-color: var(--admin-sidebar-border);
  color: #fff;
  background: rgba(255, 255, 255, 0.055);
}

.admin-workspace {
  min-width: 0;
}

.admin-topbar {
  position: sticky;
  z-index: 50;
  top: 0;
  display: flex;
  min-height: 64px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid var(--admin-border);
  padding: 0 clamp(22px, 3vw, 38px);
  background: rgba(251, 252, 254, 0.9);
  -webkit-backdrop-filter: blur(16px);
  backdrop-filter: blur(16px);
}

.admin-breadcrumb {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
  color: var(--admin-ink-3);
  font-size: 11px;
  white-space: nowrap;
}

.admin-breadcrumb span {
  color: var(--admin-ink-2);
  font-weight: 600;
}

.admin-breadcrumb i {
  color: #a7b1c0;
  font-style: normal;
}

.admin-breadcrumb a {
  color: var(--admin-ink-3);
}

.admin-breadcrumb a:hover {
  color: var(--admin-accent);
}

.admin-breadcrumb strong {
  overflow: hidden;
  color: var(--admin-ink);
  font-weight: 650;
  text-overflow: ellipsis;
}

.admin-topbar-meta {
  display: flex;
  flex: none;
  align-items: center;
  gap: 14px;
  color: var(--admin-ink-3);
  font-size: 10px;
}

.admin-environment {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: 1px solid var(--admin-border);
  border-radius: 999px;
  padding: 4px 8px;
  color: var(--admin-ink-2);
  background: var(--admin-panel-strong);
  font-size: 9px;
  font-weight: 600;
}

.admin-environment i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2e9e72;
  box-shadow: 0 0 0 3px rgba(46, 158, 114, 0.11);
}

.admin-date {
  color: var(--admin-ink-3);
  font-variant-numeric: tabular-nums;
}

.admin-main {
  min-width: 0;
}

/* Shared enterprise overrides for pages rendered inside this shell. */
.admin-shell :deep(.page-shell) {
  width: min(1360px, calc(100% - 56px));
  padding: 30px 0 64px;
}

.admin-shell :deep(.eyebrow) {
  gap: 8px;
  margin-bottom: 8px;
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 0.09em;
}

.admin-shell :deep(.eyebrow::before) {
  width: 18px;
  height: 2px;
  background: var(--admin-accent);
}

.admin-shell :deep(.surface-card) {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
}

.admin-shell :deep(.page-title) {
  margin-bottom: 8px;
  font-family: var(--sans);
  font-size: 30px;
  font-weight: 720;
  letter-spacing: -0.035em;
  line-height: 1.15;
}

.admin-shell :deep(.page-subtitle) {
  color: var(--admin-ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.admin-shell :deep(.el-button) {
  min-height: 34px;
  border-radius: 7px;
  font-weight: 600;
  box-shadow: none;
  transform: none;
}

.admin-shell :deep(.el-button:not([class*="el-button--"]):not(.is-text):not(.is-link)) {
  border: 1px solid var(--admin-border-strong);
  color: var(--admin-ink-2);
  background: var(--admin-panel-strong);
}

.admin-shell :deep(.el-button:not([class*="el-button--"]):not(.is-text):not(.is-link):hover) {
  border-color: rgba(45, 91, 216, 0.28);
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
}

.admin-shell :deep(.el-button--default:not(.is-text):not(.is-link)) {
  border: 1px solid var(--admin-border-strong);
  color: var(--admin-ink-2);
  background: var(--admin-panel-strong);
}

.admin-shell :deep(.el-button--default:not(.is-text):not(.is-link):hover) {
  border-color: rgba(45, 91, 216, 0.28);
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
}

.admin-shell :deep(.el-button--primary:not(.is-text):not(.is-link)) {
  border: 1px solid var(--admin-accent);
  color: #fff;
  background: var(--admin-accent);
  box-shadow: 0 4px 12px rgba(45, 91, 216, 0.18);
}

.admin-shell :deep(.el-button--primary:not(.is-text):not(.is-link):hover) {
  border-color: var(--admin-accent-deep);
  background: var(--admin-accent-deep);
  transform: none;
  box-shadow: 0 6px 16px rgba(45, 91, 216, 0.22);
}

.admin-shell :deep(.el-button--success:not(.is-text):not(.is-link)) {
  border: 1px solid #1d7954;
  color: #fff;
  background: #1d7954;
  box-shadow: none;
}

.admin-shell :deep(.el-button--success:not(.is-text):not(.is-link):hover) {
  border-color: #155f42;
  background: #155f42;
  transform: none;
}

.admin-shell :deep(.el-button--warning:not(.is-text):not(.is-link)) {
  border: 1px solid #c68a30;
  color: #4a3210;
  background: #f7e7c7;
  box-shadow: none;
}

.admin-shell :deep(.el-button--danger:not(.is-text):not(.is-link)) {
  border: 1px solid rgba(185, 62, 55, 0.35);
  box-shadow: none;
}

.admin-shell :deep(.el-button.is-link),
.admin-shell :deep(.el-button.is-text) {
  min-height: 30px;
  font-weight: 600;
}

.admin-shell :deep(.el-input__wrapper),
.admin-shell :deep(.el-textarea__inner),
.admin-shell :deep(.el-select__wrapper),
.admin-shell :deep(.el-date-editor.el-input__wrapper) {
  border-radius: 7px;
  background: var(--admin-panel-strong);
  box-shadow: 0 0 0 1px var(--admin-border-strong) inset;
}

.admin-shell :deep(.el-input__wrapper),
.admin-shell :deep(.el-select__wrapper),
.admin-shell :deep(.el-date-editor.el-input__wrapper) {
  min-height: 36px;
}

.admin-shell :deep(.el-input__wrapper.is-focus),
.admin-shell :deep(.el-select__wrapper.is-focused),
.admin-shell :deep(.el-date-editor.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px var(--admin-accent) inset,
    0 0 0 3px var(--admin-accent-soft);
}

.admin-shell :deep(.el-input__inner),
.admin-shell :deep(.el-textarea__inner) {
  color: var(--admin-ink);
  font-size: 12px;
}

.admin-shell :deep(.el-form-item__label) {
  color: var(--admin-ink-2);
  font-size: 12px;
  font-weight: 600;
}

.admin-shell :deep(.filter-pills) {
  gap: 4px;
  border: 1px solid var(--admin-border);
  border-radius: 9px;
  padding: 3px;
  background: var(--admin-panel-strong);
}

.admin-shell :deep(.filter-pill) {
  min-height: 30px;
  border: 0;
  border-radius: 6px;
  padding: 5px 11px;
  color: var(--admin-ink-3);
  background: transparent;
  font-size: 11px;
  font-weight: 600;
  box-shadow: none;
  transform: none;
}

.admin-shell :deep(.filter-pill:hover) {
  color: var(--admin-ink);
  background: #f3f5f8;
  box-shadow: none;
  transform: none;
}

.admin-shell :deep(.filter-pill.active) {
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
  box-shadow: none;
}

.admin-shell :deep(.el-table) {
  --el-table-border-color: var(--admin-border);
  --el-table-header-bg-color: #f5f7fa;
  --el-table-row-hover-bg-color: #f7f9fc;
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-text-color: var(--admin-ink-3);
  color: var(--admin-ink-2);
  font-size: 12px;
}

.admin-shell :deep(.el-table th.el-table__cell) {
  height: 42px;
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 650;
  letter-spacing: 0.03em;
}

.admin-shell :deep(.el-table td.el-table__cell) {
  min-height: 52px;
  padding-top: 10px;
  padding-bottom: 10px;
}

.admin-shell :deep(.el-progress-bar__outer) {
  border: 0;
  background: #e8edf4;
}

.admin-shell :deep(.countdown) {
  gap: 4px;
  border: 1px solid rgba(185, 62, 55, 0.2);
  border-radius: 999px;
  padding: 2px 7px;
  color: var(--admin-down);
  background: var(--admin-down-soft);
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0;
  transform: none;
}

.admin-shell :deep(.countdown.expired) {
  border-color: var(--admin-border);
  color: var(--admin-ink-3);
  background: #f3f5f8;
}

.admin-shell :deep(.el-alert) {
  border-radius: 8px;
}

.admin-shell :deep(.el-alert--warning.is-light) {
  border: 1px solid rgba(183, 122, 22, 0.18);
  color: #6f470e;
  background: #fffbeb;
}

.admin-shell :deep(.el-alert__title) {
  font-size: 11px;
  font-weight: 600;
}

.admin-shell :deep(.el-alert__description) {
  margin-top: 4px;
  font-size: 10px;
  line-height: 1.6;
}

.admin-shell :deep(.el-tabs__item) {
  color: var(--admin-ink-3);
  font-size: 12px;
  font-weight: 600;
}

.admin-shell :deep(.el-tabs__item.is-active) {
  color: var(--admin-accent);
}

.admin-shell :deep(.el-tabs__active-bar) {
  height: 2px;
  background: var(--admin-accent);
}

.admin-shell :deep(.el-skeleton__item) {
  background: #e8edf4;
}

.admin-shell :deep(.el-empty__description p) {
  color: var(--admin-ink-3);
  font-size: 12px;
  font-weight: 500;
}

@media (max-width: 1040px) {
  .admin-shell {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .admin-sidebar {
    padding-right: 12px;
    padding-left: 12px;
  }
}

@media (max-width: 860px) {
  .admin-shell {
    display: block;
  }

  .admin-sidebar {
    position: relative;
    height: auto;
    padding: 14px 16px 12px;
  }

  .admin-workspace-label,
  .admin-sidebar-note {
    display: none;
  }

  .admin-nav {
    display: flex;
    overflow-x: auto;
    gap: 6px;
    margin-top: 12px;
    padding-bottom: 1px;
    scrollbar-width: none;
  }

  .admin-nav::-webkit-scrollbar {
    display: none;
  }

  .admin-nav-link {
    min-height: 42px;
    flex: none;
    padding: 7px 10px;
  }

  .admin-nav-link.active::before,
  .admin-nav-copy small {
    display: none;
  }

  .admin-nav-link > .el-icon {
    width: 26px;
    height: 26px;
  }

  .admin-account {
    position: absolute;
    top: 13px;
    right: 16px;
    margin: 0;
    border: 0;
    padding: 0;
  }

  .admin-account-copy {
    display: none;
  }

  .admin-topbar {
    position: relative;
    min-height: 52px;
    padding: 0 18px;
  }

  .admin-date,
  .admin-environment {
    display: none;
  }

  .admin-shell :deep(.page-shell) {
    width: min(100% - 32px, 1360px);
    padding-top: 22px;
  }
}

@media (max-width: 560px) {
  .admin-brand-mark {
    width: 32px;
    height: 32px;
  }

  .admin-brand-copy strong {
    font-size: 15px;
  }

  .admin-brand-copy small,
  .admin-breadcrumb > span,
  .admin-breadcrumb > i,
  .admin-breadcrumb > a {
    display: none;
  }

  .admin-topbar {
    padding: 0 14px;
  }

  .admin-shell :deep(.page-shell) {
    width: min(100% - 28px, 1360px);
  }

  .admin-shell :deep(.page-title) {
    font-size: 27px;
  }
}
</style>
