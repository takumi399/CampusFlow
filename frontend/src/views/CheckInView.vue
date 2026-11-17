<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  CircleCheck,
  Loading,
  Refresh,
  SwitchButton,
  Warning,
} from '@element-plus/icons-vue'
import { checkInApi } from '../api/checkin'
import { useAuthStore } from '../stores/auth'
import { formatFullDateTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const state = ref('loading')
const result = ref(null)
const errorMessage = ref('')

const token = computed(() => {
  const value = route.query.token
  return typeof value === 'string' ? value : ''
})

async function checkIn() {
  if (!token.value) {
    state.value = 'error'
    errorMessage.value = '签到链接缺少 token，请重新扫描管理员展示的二维码。'
    return
  }

  state.value = 'loading'
  errorMessage.value = ''
  result.value = null
  try {
    result.value = await checkInApi.checkIn(token.value)
    state.value = 'success'
  } catch (error) {
    state.value = 'error'
    errorMessage.value = error.message
  }
}

function switchAccount() {
  auth.logout()
  router.replace({
    path: '/login',
    query: { redirect: route.fullPath },
  })
}

onMounted(checkIn)
</script>

<template>
  <div class="checkin-page">
    <header class="checkin-topbar">
      <router-link to="/events" class="brand" aria-label="返回活动广场">
        <span class="brand-mark">C</span>
        <span class="brand-copy">
          <strong>CampusFlow</strong>
          <small>现场入口 / CHECK-IN</small>
        </span>
      </router-link>

      <div class="account-area">
        <span class="account-name">{{ auth.user?.displayName }}</span>
        <button type="button" class="switch-button" @click="switchAccount">
          <el-icon><SwitchButton /></el-icon>
          <span>换账号</span>
        </button>
      </div>
    </header>

    <main class="checkin-main">
      <div class="gate-copy">
        <span>ONE TAP · ONE ENTRY</span>
        <p>把票交给现场，剩下的事系统来核对。</p>
      </div>

      <section class="checkin-card" :class="`state-${state}`">
        <span class="ticket-notch notch-left" aria-hidden="true"></span>
        <span class="ticket-notch notch-right" aria-hidden="true"></span>

        <div class="ticket-head">
          <span>ADMIT ONE / CAMPUSFLOW</span>
          <span>{{ state === 'success' ? 'VALID' : state === 'error' ? 'VOID' : 'CHECKING' }}</span>
        </div>

        <template v-if="state === 'loading'">
          <div class="scan-box">
            <div class="scan-line" aria-hidden="true"></div>
            <el-icon class="is-loading"><Loading /></el-icon>
          </div>
          <p class="eyebrow">CHECKING THE GATE</p>
          <h1>正在核对<br />这张入场票</h1>
          <p class="state-copy">
            请稍候，系统正在校验二维码签名、有效期和你的报名状态。
          </p>
          <div class="loading-track" aria-hidden="true"><span></span></div>
        </template>

        <template v-else-if="state === 'success'">
          <div class="success-stamp">
            <el-icon><CircleCheck /></el-icon>
            <span>已签到</span>
          </div>
          <p class="eyebrow">WELCOME IN</p>
          <h1>入场成功，<br />今天没白来。</h1>
          <p class="event-title">{{ result?.eventTitle }}</p>

          <div class="result-panel">
            <div>
              <span>签到状态</span>
              <strong>{{ result?.message }}</strong>
            </div>
            <div>
              <span>盖章时间</span>
              <strong>{{ formatFullDateTime(result?.checkedInAt) }}</strong>
            </div>
            <div>
              <span>入场账号</span>
              <strong>{{ auth.user?.displayName }} · {{ auth.user?.username }}</strong>
            </div>
          </div>

          <div class="action-row">
            <el-button
              v-if="result?.eventId"
              type="primary"
              size="large"
              @click="router.push(`/events/${result.eventId}`)"
            >
              查看活动
              <el-icon class="button-icon"><ArrowRight /></el-icon>
            </el-button>
            <el-button size="large" @click="router.push('/me/registrations')">
              查看票夹
            </el-button>
          </div>

          <p class="footnote">重复扫描同一活动二维码，不会重复写入签到记录。</p>
        </template>

        <template v-else>
          <div class="error-stamp" aria-hidden="true">!</div>
          <p class="eyebrow">NOT THIS TIME</p>
          <h1>这张票，<br />暂时进不来。</h1>
          <p class="error-message">{{ errorMessage }}</p>

          <div class="error-tips">
            <span>检查一下这几种情况：</span>
            <ul>
              <li>二维码是否已经超过 5 分钟有效期</li>
              <li>当前账号是否已确认报名该活动</li>
              <li>管理员是否已经结束或取消活动</li>
            </ul>
          </div>

          <div class="action-row">
            <el-button v-if="token" type="primary" size="large" :icon="Refresh" @click="checkIn">
              再试一次
            </el-button>
            <el-button size="large" @click="router.push('/events')">回到活动广场</el-button>
          </div>
        </template>

        <div class="ticket-barcode" aria-hidden="true"></div>
        <p class="ticket-footnote">CAMPUSFLOW · 每一次到场都值得被记录</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.checkin-page {
  position: relative;
  display: flex;
  overflow: hidden;
  min-height: 100dvh;
  flex-direction: column;
  color: var(--surface);
  background: var(--cobalt);
  isolation: isolate;
}

.checkin-page::before {
  position: fixed;
  z-index: -2;
  inset: 0;
  opacity: 0.2;
  background: radial-gradient(circle, var(--surface) 1.2px, transparent 1.2px) 0 0 / 10px 10px;
  content: "";
}

.checkin-page::after {
  position: fixed;
  z-index: -1;
  right: -12vw;
  bottom: -24vw;
  width: min(64vw, 720px);
  aspect-ratio: 1;
  border: 2px solid var(--ink);
  border-radius: 50%;
  background: var(--butter);
  content: "";
}

.checkin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  width: min(1040px, calc(100% - 40px));
  margin: 0 auto;
  border-bottom: 1.5px solid color-mix(in srgb, var(--surface) 48%, transparent);
  padding: 22px 0 18px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.brand-mark {
  display: grid;
  width: 39px;
  height: 39px;
  place-items: center;
  border: 2px solid var(--ink);
  border-radius: 10px 10px 10px 3px;
  color: var(--ink);
  background: var(--butter);
  box-shadow: 2px 2px 0 var(--ink);
  font-family: var(--display);
  font-size: 21px;
  font-weight: 900;
  transform: rotate(-3deg);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-copy strong {
  color: var(--surface);
  font-family: var(--display);
  font-size: 17px;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.brand-copy small {
  margin-top: 4px;
  color: color-mix(in srgb, var(--surface) 66%, transparent);
  font-family: var(--mono);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.account-area {
  display: flex;
  align-items: center;
  gap: 11px;
}

.account-name {
  color: color-mix(in srgb, var(--surface) 82%, transparent);
  font-size: 12px;
  font-weight: 750;
}

.switch-button {
  display: inline-flex;
  min-height: 40px;
  align-items: center;
  gap: 5px;
  border: 1.5px solid var(--surface);
  border-radius: 999px;
  padding: 7px 11px;
  color: var(--surface);
  background: transparent;
  font-size: 11px;
  font-weight: 750;
  cursor: pointer;
}

.switch-button:hover {
  color: var(--ink);
  background: var(--surface);
}

.checkin-main {
  display: flex;
  width: min(660px, calc(100% - 28px));
  flex: 1;
  flex-direction: column;
  align-items: center;
  margin: 0 auto;
  padding: 34px 0 44px;
}

.gate-copy {
  width: 100%;
  margin-bottom: 16px;
  color: color-mix(in srgb, var(--surface) 82%, transparent);
  text-align: center;
}

.gate-copy span {
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.gate-copy p {
  margin: 6px 0 0;
  font-size: 11px;
}

.checkin-card {
  position: relative;
  width: min(500px, 100%);
  border: 2px solid var(--ink);
  border-radius: 17px;
  padding: 25px 27px 21px;
  color: var(--ink);
  background: var(--surface);
  box-shadow: 8px 8px 0 var(--ink);
  text-align: center;
  animation: paper-in 420ms ease both;
}

.ticket-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 24px;
  border-bottom: 1.5px dashed var(--line);
  padding-bottom: 13px;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.state-success .ticket-head span:last-child {
  color: var(--mint-deep);
}

.state-error .ticket-head span:last-child {
  color: var(--tomato);
}

.ticket-notch {
  position: absolute;
  top: 54%;
  width: 20px;
  height: 20px;
  border: 2px solid var(--ink);
  border-radius: 50%;
  background: var(--cobalt);
}

.notch-left {
  left: -12px;
}

.notch-right {
  right: -12px;
}

.scan-box {
  position: relative;
  display: grid;
  width: 92px;
  height: 92px;
  margin: 26px auto 29px;
  place-items: center;
  overflow: hidden;
  border: 2px solid var(--ink);
  border-radius: 12px;
  color: var(--cobalt);
  background: var(--cobalt-soft);
  font-size: 35px;
}

.scan-box::before,
.scan-box::after {
  position: absolute;
  width: 19px;
  height: 19px;
  border-color: var(--tomato);
  border-style: solid;
  content: "";
}

.scan-box::before {
  top: 7px;
  left: 7px;
  border-width: 3px 0 0 3px;
}

.scan-box::after {
  right: 7px;
  bottom: 7px;
  border-width: 0 3px 3px 0;
}

.scan-line {
  position: absolute;
  z-index: 1;
  top: 10%;
  right: 10px;
  left: 10px;
  height: 2px;
  background: var(--tomato);
  box-shadow: 0 0 8px var(--tomato);
  animation: scan 1.7s ease-in-out infinite alternate;
}

@keyframes scan {
  to {
    transform: translateY(66px);
  }
}

.checkin-card h1 {
  margin-bottom: 14px;
  font-family: var(--display);
  font-size: clamp(39px, 8vw, 54px);
  font-weight: 900;
  letter-spacing: -0.07em;
  line-height: 0.98;
}

.state-copy,
.error-message {
  max-width: 365px;
  margin: 0 auto 18px;
  color: var(--ink-2);
  font-size: 12px;
  line-height: 1.7;
}

.loading-track {
  overflow: hidden;
  height: 9px;
  margin: 26px 0 8px;
  border: 1.5px solid var(--ink);
  border-radius: 999px;
  background: var(--paper-deep);
}

.loading-track span {
  display: block;
  width: 42%;
  height: 100%;
  background: var(--cobalt);
  animation: loading-run 1.25s ease-in-out infinite alternate;
}

@keyframes loading-run {
  to {
    transform: translateX(138%);
  }
}

.success-stamp {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 2px auto 27px;
  border: 4px double var(--tomato);
  border-radius: 9px;
  padding: 10px 16px;
  color: var(--tomato);
  font-family: var(--display);
  font-size: 29px;
  font-weight: 900;
  letter-spacing: -0.04em;
  transform: rotate(-5deg);
  animation: stamp-in 440ms ease both;
}

.success-stamp .el-icon {
  font-size: 22px;
}

.event-title {
  margin: 0 auto 18px;
  border-bottom: 1.5px dashed var(--line);
  padding-bottom: 17px;
  color: var(--cobalt-deep);
  font-size: 14px;
  font-weight: 850;
}

.result-panel {
  display: grid;
  gap: 0;
  margin-bottom: 19px;
  border: 1.5px solid var(--ink);
  border-radius: 8px;
  text-align: left;
}

.result-panel > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 15px;
  padding: 10px 12px;
}

.result-panel > div:not(:last-child) {
  border-bottom: 1.5px dashed var(--line);
}

.result-panel span {
  flex: 0 0 auto;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 8px;
  font-weight: 800;
}

.result-panel strong {
  font-size: 10px;
  font-weight: 800;
  text-align: right;
}

.action-row {
  display: flex;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.button-icon {
  margin-left: 5px;
}

.footnote {
  margin: 16px 0 0;
  color: var(--ink-3);
  font-size: 9px;
  line-height: 1.5;
}

.error-stamp {
  display: grid;
  width: 78px;
  height: 78px;
  margin: 20px auto 28px;
  place-items: center;
  border: 3px solid var(--tomato);
  border-radius: 50%;
  color: var(--tomato);
  font-family: var(--display);
  font-size: 43px;
  font-weight: 900;
  transform: rotate(-7deg);
}

.error-tips {
  margin: 20px 0 22px;
  border: 1.5px solid var(--ink);
  border-radius: 9px;
  padding: 13px 15px;
  background: var(--tomato-soft);
  text-align: left;
}

.error-tips > span {
  font-size: 11px;
  font-weight: 850;
}

.error-tips ul {
  display: grid;
  gap: 5px;
  margin: 9px 0 0;
  padding-left: 17px;
  color: var(--ink-2);
  font-size: 10px;
  line-height: 1.55;
}

.ticket-barcode {
  width: 74%;
  height: 30px;
  margin: 24px auto 8px;
  opacity: 0.68;
  background: repeating-linear-gradient(
    90deg,
    var(--ink) 0 2px,
    transparent 2px 5px,
    var(--ink) 5px 8px,
    transparent 8px 11px
  );
}

.ticket-footnote {
  margin: 0;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 7px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

@media (max-width: 560px) {
  .checkin-topbar {
    width: calc(100% - 28px);
    padding-top: 16px;
  }

  .account-name {
    display: none;
  }

  .switch-button span {
    display: none;
  }

  .switch-button {
    width: 42px;
    height: 42px;
    justify-content: center;
    padding: 0;
  }

  .checkin-main {
    padding-top: 23px;
  }

  .gate-copy p {
    display: none;
  }

  .checkin-card {
    padding: 21px 18px 18px;
    box-shadow: 5px 5px 0 var(--ink);
  }

  .ticket-head {
    margin-bottom: 20px;
  }

  .checkin-card h1 {
    font-size: 41px;
  }

  .action-row {
    align-items: stretch;
    flex-direction: column;
  }

  .action-row :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
