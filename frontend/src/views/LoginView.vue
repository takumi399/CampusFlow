<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight, Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const demos = [
  { label: '学生视角', username: 'student1', password: 'student123' },
  { label: '管理平台', username: 'admin', password: 'admin123' },
]

function fillDemo(demo) {
  form.username = demo.username
  form.password = demo.password
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login({ ...form })
    const requested =
      typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
        ? route.query.redirect
        : ''
    const isAdminDestination = requested === '/admin' || requested.startsWith('/admin/')
    const destinationAllowed = auth.isAdmin ? isAdminDestination : !isAdminDestination
    const fallback = auth.isAdmin ? '/admin/events' : '/events'
    await router.replace((destinationAllowed && requested) || fallback)
    ElMessage.success(`欢迎回来，${auth.user?.displayName}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-visual">
      <span class="visual-shape shape-sun" aria-hidden="true"></span>
      <span class="visual-shape shape-star" aria-hidden="true"></span>
      <span class="visual-shape shape-stripe" aria-hidden="true"></span>

      <div class="visual-top">
        <div class="brand-lockup">
          <span class="brand-mark">C</span>
          <span>
            <strong>CampusFlow</strong>
            <small>青禾活动</small>
          </span>
        </div>
        <span class="edition">CAMPUS EDITION / 2026</span>
      </div>

      <div class="visual-copy">
        <span class="visual-sticker">今天碰个面</span>
        <h1>别只在群里<br />看别人<br /><em>玩得热闹。</em></h1>
        <p class="visual-description">
          校园里正在发生的事，报名、候补、签到，一张票把流程走完。
          名额会流动，但每一次机会都按顺序交给下一位。
        </p>
      </div>

      <div class="visual-footer">
        <div class="process-strip" aria-label="使用流程">
          <span>拿票</span>
          <i aria-hidden="true">→</i>
          <span>等位</span>
          <i aria-hidden="true">→</i>
          <span>到场</span>
          <i aria-hidden="true">→</i>
          <span>盖章</span>
        </div>
        <p>MAKE ROOM FOR CAMPUS.</p>
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <div class="card-topline">
          <span>MEMBERS ONLY</span>
          <span>NO. 2026</span>
        </div>

        <h2>先登记，<br />再出发。</h2>
        <p class="login-hint">用你的校园账号进场，活动安排和候补状态都在里面。</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" :prefix-icon="User" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              :prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              size="large"
            />
          </el-form-item>

          <el-button class="login-button" type="primary" size="large" :loading="loading" @click="submit">
            盖章入场
            <el-icon class="button-icon"><ArrowRight /></el-icon>
          </el-button>
        </el-form>

        <div class="demo-block">
          <span class="demo-title">借别人的票试试看</span>
          <div class="demo-list">
            <button v-for="demo in demos" :key="demo.username" type="button" @click="fillDemo(demo)">
              <strong>{{ demo.label }}</strong>
              <span>{{ demo.username }}</span>
            </button>
          </div>
        </div>

        <p class="login-note">演示账号仅用于本地预览，不包含真实学校数据。</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(430px, 0.92fr);
  min-height: 100dvh;
  background: var(--paper);
}

.login-visual {
  position: relative;
  display: flex;
  overflow: hidden;
  flex-direction: column;
  justify-content: space-between;
  border-right: 2px solid var(--ink);
  padding: 38px clamp(28px, 5vw, 70px) 34px;
  color: var(--surface);
  background: var(--cobalt);
  isolation: isolate;
}

.login-visual::after {
  position: absolute;
  z-index: -1;
  inset: 0;
  opacity: 0.19;
  background: radial-gradient(circle, var(--surface) 1.2px, transparent 1.2px) 0 0 / 11px 11px;
  content: "";
}

.visual-shape {
  position: absolute;
  z-index: -1;
  display: block;
  border: 2px solid var(--ink);
}

.shape-sun {
  top: 17%;
  right: -7%;
  width: clamp(190px, 28vw, 390px);
  aspect-ratio: 1;
  border-radius: 50%;
  background: var(--butter);
}

.shape-star {
  right: 16%;
  bottom: 13%;
  width: 104px;
  aspect-ratio: 1;
  background: var(--tomato);
  transform: rotate(20deg);
}

.shape-stripe {
  top: -18%;
  left: 84%;
  width: 46px;
  height: 135%;
  background: var(--butter);
  transform: rotate(18deg);
}

.visual-top,
.visual-copy,
.visual-footer {
  position: relative;
  z-index: 2;
}

.visual-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.brand-lockup {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  display: grid;
  width: 45px;
  height: 45px;
  place-items: center;
  border: 2px solid var(--ink);
  border-radius: 12px 12px 12px 4px;
  color: var(--ink);
  background: var(--butter);
  box-shadow: 3px 3px 0 var(--ink);
  font-family: var(--display);
  font-size: 25px;
  font-weight: 900;
  transform: rotate(-4deg);
}

.brand-lockup > span:last-child {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-lockup strong {
  font-family: var(--display);
  font-size: 18px;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.brand-lockup small {
  margin-top: 5px;
  color: color-mix(in srgb, var(--surface) 70%, transparent);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.edition {
  border: 1.5px solid var(--ink);
  border-radius: 999px;
  padding: 6px 10px;
  color: var(--ink);
  background: var(--surface);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.03em;
  transform: rotate(2deg);
}

.visual-copy {
  max-width: 720px;
  padding: 76px 0 62px;
}

.visual-sticker {
  display: inline-block;
  margin-bottom: 24px;
  border: 2px solid var(--ink);
  border-radius: 6px;
  padding: 7px 12px;
  color: var(--ink);
  background: var(--butter);
  box-shadow: 3px 3px 0 var(--ink);
  font-size: 14px;
  font-weight: 900;
  transform: rotate(-3deg);
}

.visual-copy h1 {
  margin-bottom: 28px;
  font-family: var(--display);
  font-size: clamp(54px, 7.2vw, 106px);
  font-weight: 900;
  letter-spacing: -0.075em;
  line-height: 0.96;
  text-wrap: balance;
}

.visual-copy em {
  color: var(--butter);
  font-style: normal;
  text-decoration: underline;
  text-decoration-color: var(--tomato);
  text-decoration-thickness: 7px;
  text-underline-offset: 8px;
}

.visual-description {
  max-width: 540px;
  margin-bottom: 0;
  color: color-mix(in srgb, var(--surface) 78%, transparent);
  font-size: 15px;
  line-height: 1.9;
}

.visual-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  border-top: 1.5px solid color-mix(in srgb, var(--surface) 45%, transparent);
  padding-top: 20px;
}

.process-strip {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  font-family: var(--mono);
  font-size: 10px;
  font-weight: 800;
}

.process-strip span {
  border: 1px solid color-mix(in srgb, var(--surface) 50%, transparent);
  border-radius: 999px;
  padding: 5px 9px;
}

.process-strip i {
  color: var(--butter);
  font-style: normal;
}

.visual-footer p {
  margin-bottom: 2px;
  color: color-mix(in srgb, var(--surface) 60%, transparent);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  white-space: nowrap;
}

.login-panel {
  display: grid;
  place-items: center;
  padding: 42px;
}

.login-card {
  position: relative;
  width: min(440px, 100%);
  border: 2px solid var(--ink);
  border-radius: 22px;
  padding: 30px 32px 26px;
  background: var(--surface);
  box-shadow: 7px 7px 0 var(--ink);
}

.login-card::before {
  position: absolute;
  top: -12px;
  left: 50%;
  width: 96px;
  height: 24px;
  background: color-mix(in srgb, var(--butter) 80%, transparent);
  content: "";
  transform: translateX(-50%) rotate(-2deg);
}

.card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 25px;
  border-bottom: 1.5px dashed var(--line);
  padding-bottom: 12px;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.login-card h2 {
  margin-bottom: 10px;
  font-family: var(--display);
  font-size: clamp(36px, 4vw, 48px);
  font-weight: 900;
  letter-spacing: -0.06em;
  line-height: 0.98;
}

.login-hint {
  margin-bottom: 25px;
  color: var(--ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.login-button {
  width: 100%;
  margin-top: 3px;
}

.button-icon {
  margin-left: 7px;
}

.demo-block {
  margin-top: 24px;
  border-top: 1.5px dashed var(--line);
  padding-top: 17px;
}

.demo-title {
  display: block;
  margin-bottom: 9px;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.demo-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.demo-list button {
  display: flex;
  min-height: 52px;
  flex-direction: column;
  justify-content: center;
  border: 1.5px solid var(--ink);
  border-radius: 9px;
  padding: 8px 10px;
  text-align: left;
  background: var(--surface-2);
  cursor: pointer;
  transition:
    transform 150ms ease,
    background 150ms ease;
}

.demo-list button:hover {
  background: var(--butter-soft);
  transform: translateY(-2px) rotate(-1deg);
}

.demo-list strong {
  font-size: 12px;
  font-weight: 850;
}

.demo-list span {
  margin-top: 3px;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
}

.login-note {
  margin: 16px 0 0;
  color: var(--ink-3);
  font-size: 10px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-visual {
    min-height: 520px;
    border-right: 0;
    border-bottom: 2px solid var(--ink);
  }

  .visual-copy {
    padding: 62px 0 48px;
  }

  .login-panel {
    padding: 48px 20px 58px;
  }
}

@media (max-width: 560px) {
  .login-visual {
    min-height: 570px;
    padding: 24px 20px 24px;
  }

  .edition {
    display: none;
  }

  .visual-copy h1 {
    font-size: 53px;
  }

  .visual-description {
    font-size: 13px;
  }

  .visual-footer {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .visual-footer p {
    display: none;
  }

  .shape-star {
    right: 3%;
    bottom: 7%;
    width: 70px;
  }

  .shape-stripe {
    left: 91%;
    width: 30px;
  }

  .login-panel {
    padding: 38px 14px 48px;
  }

  .login-card {
    padding: 26px 20px 22px;
  }

  .login-card h2 {
    font-size: 39px;
  }

  .demo-list {
    grid-template-columns: 1fr;
  }
}
</style>
