<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Bell,
  Calendar,
  CircleCheckFilled,
  Clock,
  Location,
  Tickets,
  User,
} from '@element-plus/icons-vue'
import AppShell from '../components/AppShell.vue'
import CountdownText from '../components/CountdownText.vue'
import EventPoster from '../components/EventPoster.vue'
import StatusTag from '../components/StatusTag.vue'
import { eventApi } from '../api/events'
import { useAuthStore } from '../stores/auth'
import {
  formatDateTime,
  formatFullDateTime,
  formatTimeRange,
  isRegistrationOpen,
} from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const event = ref(null)
const loading = ref(true)
const actionLoading = ref(false)

const registration = computed(() => event.value?.registration || null)
const registrationOpen = computed(() => isRegistrationOpen(event.value))
const isRejoinable = computed(
  () => !registration.value || ['CANCELED', 'EXPIRED'].includes(registration.value.status),
)
const capacityPercent = computed(() => {
  if (!event.value?.capacity) return 0
  return Math.min(100, Math.round((event.value.occupiedCount / event.value.capacity) * 100))
})

async function load() {
  loading.value = true
  try {
    event.value = await eventApi.get(route.params.id)
  } catch (error) {
    ElMessage.error(error.message)
    event.value = null
  } finally {
    loading.value = false
  }
}

async function register() {
  if (!registrationOpen.value) return
  actionLoading.value = true
  try {
    await eventApi.register(event.value.id)
    ElMessage.success(event.value.remainingSeats > 0 ? '报名成功' : '已加入候补队列')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

async function cancel() {
  try {
    await ElMessageBox.confirm(
      '取消后名额会释放给候补队列，确定要取消吗？',
      '取消报名',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' },
    )
  } catch {
    return
  }

  actionLoading.value = true
  try {
    await eventApi.cancel(registration.value.id)
    ElMessage.success('报名已取消')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

async function confirmPromotion() {
  actionLoading.value = true
  try {
    await eventApi.confirm(registration.value.id)
    ElMessage.success('候补资格已确认，现场见')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div v-if="loading" class="page-shell detail-loading">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="event" class="page-shell detail-shell">
      <button type="button" class="back-link" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回活动广场
      </button>

      <header class="detail-hero">
        <div class="detail-main">
          <EventPoster
            class="detail-poster"
            compact
            :category="event.category"
            :event-id="event.id"
          />

          <div class="detail-copy">
            <div class="detail-kicker">
              <span class="category-pill">{{ event.category }}</span>
              <StatusTag :status="event.status" />
            </div>

            <h1>{{ event.title }}</h1>
            <p class="detail-description">
              {{ event.description || '这场活动还没有补充描述，但报名信息都在这里。' }}
            </p>

            <div class="detail-meta-grid">
              <div class="detail-meta-card">
                <el-icon><Calendar /></el-icon>
                <span>活动时间</span>
                <strong>{{ formatTimeRange(event.startTime, event.endTime) }}</strong>
              </div>
              <div class="detail-meta-card">
                <el-icon><Location /></el-icon>
                <span>碰头地点</span>
                <strong>{{ event.location }}</strong>
              </div>
              <div class="detail-meta-card">
                <el-icon><User /></el-icon>
                <span>谁在张罗</span>
                <strong>{{ event.organizer }}</strong>
              </div>
              <div class="detail-meta-card">
                <el-icon><Tickets /></el-icon>
                <span>拿票截止</span>
                <strong>{{ formatFullDateTime(event.registrationEnd) }}</strong>
              </div>
            </div>
          </div>
        </div>

        <aside class="registration-panel">
          <div class="ticket-head">
            <p class="eyebrow">YOUR SPOT</p>
            <span>NO.{{ String(event.id).padStart(2, '0') }}</span>
          </div>

          <template v-if="isRejoinable">
            <div class="seat-number">
              <strong>{{ event.remainingSeats > 0 ? event.remainingSeats : 'FULL' }}</strong>
              <span>{{ event.remainingSeats > 0 ? '个位子还空着' : '现在只能排队等位' }}</span>
            </div>
            <h2>
              {{ event.remainingSeats > 0 ? '这张票，留给你。' : '满了也别急着走。' }}
            </h2>
            <p class="panel-copy">
              {{
                event.remainingSeats > 0
                  ? '报名成功后名额会保留到活动开始，现场扫码就能完成签到。'
                  : '加入候补队列后，名额一释放就按顺序通知你，确认后票才是你的。'
              }}
            </p>
            <el-button
              class="panel-action"
              type="primary"
              size="large"
              :loading="actionLoading"
              :disabled="!registrationOpen"
              @click="register"
            >
              {{ event.remainingSeats > 0 ? '拿一张票' : '加入候补' }}
            </el-button>
            <p v-if="!registrationOpen" class="closed-note">当前不在报名时间内</p>
            <div v-if="registration" class="previous-status">
              <span>上次状态</span>
              <StatusTag :status="registration.status" size="small" />
            </div>
          </template>

          <template v-else-if="registration.status === 'PROMOTED'">
            <div class="status-hero promoted">
              <el-icon><Bell /></el-icon>
              <span>轮到你递补了</span>
            </div>
            <h2>先确认，<br />别让票飞走。</h2>
            <p class="panel-copy">
              你获得了候补递补资格，名额会在
              <CountdownText :deadline="registration.confirmDeadline" />
              后自动释放给下一位。
            </p>
            <el-button
              class="panel-action"
              type="warning"
              size="large"
              :loading="actionLoading"
              @click="confirmPromotion"
            >
              确认参加
            </el-button>
            <el-button text type="danger" @click="cancel">放弃这次递补</el-button>
          </template>

          <template v-else-if="registration.status === 'WAITLISTED'">
            <div class="status-hero waiting">
              <el-icon><Clock /></el-icon>
              <span>正在候补</span>
            </div>
            <h2>候补第 {{ registration.waitlistPosition }} 位</h2>
            <p class="panel-copy">
              当前票已经发完。有人取消时，系统会按候补顺序递补，
              并在「我的票夹」里显示确认倒计时。
            </p>
            <el-button text type="danger" @click="cancel">取消候补</el-button>
          </template>

          <template v-else-if="registration.status === 'CONFIRMED'">
            <div class="status-hero confirmed">
              <el-icon><CircleCheckFilled /></el-icon>
              <span>票已盖章</span>
            </div>
            <h2>到时见，{{ auth.user?.displayName }}。</h2>
            <p class="panel-copy">
              现场签到请扫描管理员展示的二维码。取消报名会把名额释放给候补队列。
            </p>
            <el-button text type="danger" @click="cancel">取消报名</el-button>
          </template>

          <template v-else-if="registration.status === 'CHECKED_IN'">
            <div class="status-hero checked">
              <el-icon><CircleCheckFilled /></el-icon>
              <span>已完成签到</span>
            </div>
            <h2>这次活动，你已经在场。</h2>
            <p class="panel-copy">签到时间：{{ formatDateTime(registration.checkedInAt) }}</p>
          </template>

          <div class="capacity-summary">
            <div>
              <span>报名进度</span>
              <strong>{{ event.occupiedCount }} / {{ event.capacity }}</strong>
            </div>
            <el-progress
              :percentage="capacityPercent"
              :show-text="false"
              :stroke-width="7"
              :color="event.remainingSeats > 0 ? 'var(--cobalt)' : 'var(--tomato)'"
            />
            <small>{{ event.remainingSeats > 0 ? `还剩 ${event.remainingSeats} 席` : '名额已满' }}</small>
          </div>
        </aside>
      </header>

      <section class="detail-body">
        <article class="story-card">
          <p class="eyebrow">ABOUT THE EVENT</p>
          <h2>这场活动，<br />到底玩什么？</h2>
          <p>
            {{ event.description || '主办方暂时没有留下更多描述。你可以根据时间、地点和报名窗口安排自己的行程。' }}
          </p>
          <div class="story-note">
            <span>组织者</span>
            <strong>{{ event.organizer }}</strong>
            <span>分类</span>
            <strong>{{ event.category }}</strong>
          </div>
        </article>

        <article class="timeline-card">
          <p class="eyebrow">THE TIMELINE</p>
          <h3>这张票的时间线</h3>
          <div class="timeline-list">
            <div class="timeline-item done">
              <span class="dot">1</span>
              <div>
                <strong>报名开闸</strong>
                <small>{{ formatFullDateTime(event.registrationStart) }}</small>
              </div>
            </div>
            <div class="timeline-item" :class="{ done: !isRegistrationOpen(event) }">
              <span class="dot">2</span>
              <div>
                <strong>停止发车</strong>
                <small>{{ formatFullDateTime(event.registrationEnd) }}</small>
              </div>
            </div>
            <div class="timeline-item">
              <span class="dot">3</span>
              <div>
                <strong>现场碰头</strong>
                <small>{{ formatFullDateTime(event.startTime) }}</small>
              </div>
            </div>
          </div>
        </article>
      </section>
    </div>

    <div v-else class="page-shell">
      <div class="empty-panel">
        <div class="missing-stamp" aria-hidden="true">404</div>
        <h2>这张海报被撕掉了</h2>
        <p>活动不存在或已经被移除，回公告栏看看别的。</p>
        <el-button type="primary" @click="router.push('/events')">回到活动广场</el-button>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.detail-shell {
  padding-top: 24px;
}

.detail-loading {
  min-height: 70dvh;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 22px;
  border: 0;
  border-bottom: 1.5px solid currentColor;
  padding: 2px 0 4px;
  color: var(--ink-2);
  background: transparent;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.back-link:hover {
  color: var(--cobalt);
}

.detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 372px;
  gap: 28px;
  align-items: start;
  margin-bottom: 42px;
}

.detail-main {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: clamp(24px, 4vw, 44px);
  align-items: start;
  animation: paper-in 420ms ease both;
}

.detail-poster {
  aspect-ratio: 0.82;
  border: 2px solid var(--ink);
  border-radius: 17px;
  box-shadow: 6px 6px 0 var(--ink);
  transform: rotate(-1.4deg);
}

.detail-copy {
  padding-top: 4px;
}

.detail-kicker {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
  margin-bottom: 20px;
}

.category-pill {
  border: 1.5px solid var(--ink);
  border-radius: 999px;
  padding: 5px 10px;
  background: var(--butter);
  font-size: 11px;
  font-weight: 900;
  transform: rotate(-2deg);
}

.detail-copy h1 {
  margin-bottom: 18px;
  font-family: var(--display);
  font-size: clamp(42px, 5.7vw, 76px);
  font-weight: 900;
  letter-spacing: -0.07em;
  line-height: 0.98;
  text-wrap: balance;
}

.detail-description {
  max-width: 680px;
  margin-bottom: 28px;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.85;
}

.detail-meta-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  border-top: 1.5px solid var(--ink);
  border-left: 1.5px solid var(--ink);
}

.detail-meta-card {
  display: grid;
  grid-template-columns: 28px 1fr;
  gap: 2px 9px;
  min-width: 0;
  border-right: 1.5px solid var(--ink);
  border-bottom: 1.5px solid var(--ink);
  padding: 13px 14px;
  background: color-mix(in srgb, var(--surface) 88%, transparent);
}

.detail-meta-card .el-icon {
  grid-row: 1 / span 2;
  align-self: center;
  color: var(--tomato);
  font-size: 18px;
}

.detail-meta-card span {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
}

.detail-meta-card strong {
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  line-height: 1.45;
  text-overflow: ellipsis;
}

.registration-panel {
  position: sticky;
  z-index: var(--z-sticky);
  top: calc(var(--nav-h) + 20px);
  border: 2px solid var(--ink);
  border-radius: 18px;
  padding: 22px;
  background: var(--surface);
  box-shadow: 7px 7px 0 var(--ink);
  animation: paper-in 480ms 80ms ease both;
}

.registration-panel::before,
.registration-panel::after {
  position: absolute;
  top: 88px;
  width: 17px;
  height: 17px;
  border: 2px solid var(--ink);
  border-radius: 50%;
  background: var(--paper);
  content: "";
}

.registration-panel::before {
  left: -10px;
}

.registration-panel::after {
  right: -10px;
}

.ticket-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1.5px dashed var(--line);
  padding-bottom: 13px;
}

.ticket-head .eyebrow {
  margin-bottom: 0;
}

.ticket-head > span {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
}

.seat-number {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin: 20px 0 12px;
}

.seat-number strong {
  color: var(--cobalt);
  font-family: var(--display);
  font-size: 57px;
  font-weight: 900;
  letter-spacing: -0.08em;
  line-height: 0.82;
}

.seat-number span {
  max-width: 80px;
  padding-bottom: 4px;
  color: var(--ink-2);
  font-size: 10px;
  font-weight: 750;
  line-height: 1.35;
}

.registration-panel h2 {
  margin-bottom: 9px;
  font-family: var(--display);
  font-size: 29px;
  font-weight: 900;
  letter-spacing: -0.05em;
  line-height: 1.08;
}

.panel-copy {
  margin-bottom: 17px;
  color: var(--ink-2);
  font-size: 12px;
  line-height: 1.75;
}

.panel-action {
  width: 100%;
}

.closed-note {
  margin: 9px 0 0;
  color: var(--tomato);
  font-size: 11px;
  font-weight: 750;
  text-align: center;
}

.previous-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 15px;
  border-top: 1.5px dashed var(--line);
  padding-top: 12px;
  color: var(--ink-3);
  font-size: 10px;
}

.status-hero {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin: 20px 0 14px;
  border: 1.5px solid currentColor;
  border-radius: 6px;
  padding: 7px 10px;
  font-size: 12px;
  font-weight: 900;
  transform: rotate(-2deg);
}

.status-hero.promoted {
  color: var(--tomato);
  background: var(--tomato-soft);
}

.status-hero.waiting {
  color: var(--ink);
  background: var(--butter);
}

.status-hero.confirmed,
.status-hero.checked {
  color: var(--mint-deep);
  background: var(--mint);
}

.capacity-summary {
  margin-top: 20px;
  border-top: 1.5px dashed var(--line);
  padding-top: 14px;
}

.capacity-summary > div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.capacity-summary span {
  color: var(--ink-3);
  font-size: 10px;
  font-weight: 750;
}

.capacity-summary strong {
  font-family: var(--mono);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.capacity-summary small {
  display: block;
  margin-top: 8px;
  color: var(--ink-3);
  font-size: 10px;
}

.detail-body {
  display: grid;
  grid-template-columns: minmax(0, 1.28fr) minmax(300px, 0.72fr);
  gap: 24px;
}

.story-card,
.timeline-card {
  border: 1.5px solid var(--ink);
  border-radius: 20px;
  background: var(--surface);
  box-shadow: 4px 4px 0 var(--ink);
}

.story-card {
  padding: 28px 30px;
  background-image: repeating-linear-gradient(
    to bottom,
    transparent 0 31px,
    color-mix(in srgb, var(--ink) 7%, transparent) 31px 32px
  );
}

.story-card h2 {
  margin-bottom: 20px;
  font-family: var(--display);
  font-size: clamp(30px, 3.8vw, 48px);
  font-weight: 900;
  letter-spacing: -0.055em;
  line-height: 1.02;
}

.story-card > p:not(.eyebrow) {
  max-width: 720px;
  margin-bottom: 24px;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 2.05;
}

.story-note {
  display: grid;
  grid-template-columns: max-content minmax(0, 1fr);
  gap: 9px 18px;
  width: min(440px, 100%);
  border-top: 1.5px dashed var(--line);
  padding-top: 16px;
}

.story-note span {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
}

.story-note strong {
  font-size: 12px;
}

.timeline-card {
  padding: 24px;
  background: var(--butter-soft);
  transform: rotate(0.8deg);
}

.timeline-card h3 {
  margin-bottom: 20px;
  font-family: var(--display);
  font-size: 27px;
  font-weight: 900;
  letter-spacing: -0.045em;
  line-height: 1.05;
}

.timeline-list {
  display: grid;
  gap: 0;
}

.timeline-item {
  position: relative;
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 11px;
  min-height: 67px;
}

.timeline-item:not(:last-child)::before {
  position: absolute;
  top: 28px;
  bottom: 0;
  left: 14px;
  width: 1.5px;
  background: var(--ink);
  content: "";
}

.dot {
  position: relative;
  z-index: 1;
  display: grid;
  width: 29px;
  height: 29px;
  place-items: center;
  border: 1.5px solid var(--ink);
  border-radius: 50%;
  background: var(--surface);
  font-family: var(--mono);
  font-size: 10px;
  font-weight: 900;
}

.timeline-item.done .dot {
  color: var(--surface);
  background: var(--cobalt);
}

.timeline-item div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 2px;
}

.timeline-item strong {
  font-size: 12px;
  font-weight: 850;
}

.timeline-item small {
  color: var(--ink-2);
  font-family: var(--mono);
  font-size: 9px;
  line-height: 1.45;
}

.empty-panel {
  display: grid;
  place-items: center;
  margin-top: 56px;
  border: 2px dashed var(--ink);
  border-radius: 22px;
  padding: 70px 24px;
  background: var(--surface);
  text-align: center;
}

.missing-stamp {
  margin-bottom: 14px;
  border: 3px solid var(--tomato);
  border-radius: 10px;
  padding: 9px 15px;
  color: var(--tomato);
  font-family: var(--display);
  font-size: 36px;
  font-weight: 900;
  transform: rotate(-7deg);
}

.empty-panel h2 {
  margin-bottom: 8px;
  font-family: var(--display);
  font-size: 34px;
  font-weight: 900;
}

.empty-panel p {
  margin-bottom: 18px;
  color: var(--ink-2);
  font-size: 13px;
}

@media (max-width: 1140px) {
  .detail-hero {
    grid-template-columns: minmax(0, 1fr) 338px;
  }

  .detail-main {
    grid-template-columns: 210px minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }

  .registration-panel {
    position: static;
    width: min(560px, 100%);
  }

  .detail-body {
    grid-template-columns: 1fr;
  }

  .timeline-card {
    transform: none;
  }
}

@media (max-width: 700px) {
  .detail-main {
    grid-template-columns: 1fr;
  }

  .detail-poster {
    width: min(330px, 100%);
    aspect-ratio: 1.32;
    transform: rotate(-1deg);
  }

  .detail-kicker {
    align-items: flex-start;
  }

  .detail-copy h1 {
    font-size: 47px;
  }

  .detail-meta-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .detail-shell {
    padding-top: 18px;
  }

  .detail-hero {
    gap: 24px;
  }

  .detail-copy h1 {
    font-size: 40px;
  }

  .registration-panel {
    padding: 19px;
  }

  .story-card {
    padding: 23px 20px;
  }

  .story-card h2 {
    font-size: 34px;
  }
}
</style>
