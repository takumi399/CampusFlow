<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Calendar,
  CircleCheck,
  Clock,
  Location,
  Refresh,
  Tickets,
  Warning,
} from '@element-plus/icons-vue'
import AppShell from '../components/AppShell.vue'
import CountdownText from '../components/CountdownText.vue'
import StatusTag from '../components/StatusTag.vue'
import { eventApi } from '../api/events'
import {
  formatDateParts,
  formatDateTime,
  formatFullDateTime,
  formatTimeRange,
} from '../utils/format'

const router = useRouter()
const registrations = ref([])
const loading = ref(true)
const activeFilter = ref('ACTIVE')
const busyId = ref(null)

const filters = [
  { key: 'ACTIVE', label: '进行中' },
  { key: 'PROMOTED', label: '待确认' },
  { key: 'WAITLISTED', label: '候补中' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'CHECKED_IN', label: '已签到' },
  { key: 'HISTORY', label: '历史记录' },
  { key: 'ALL', label: '全部' },
]

const activeStatuses = ['PROMOTED', 'WAITLISTED', 'CONFIRMED']
const historyStatuses = ['CANCELED', 'EXPIRED', 'CHECKED_IN']

const counts = computed(() => ({
  active: registrations.value.filter((item) => activeStatuses.includes(item.status)).length,
  promoted: registrations.value.filter((item) => item.status === 'PROMOTED').length,
  waitlisted: registrations.value.filter((item) => item.status === 'WAITLISTED').length,
  confirmed: registrations.value.filter((item) => item.status === 'CONFIRMED').length,
  checkedIn: registrations.value.filter((item) => item.status === 'CHECKED_IN').length,
}))

const filteredRegistrations = computed(() => {
  const filtered = registrations.value.filter((item) => {
    if (activeFilter.value === 'ALL') return true
    if (activeFilter.value === 'ACTIVE') return activeStatuses.includes(item.status)
    if (activeFilter.value === 'HISTORY') return historyStatuses.includes(item.status)
    return item.status === activeFilter.value
  })

  const rank = {
    PROMOTED: 0,
    WAITLISTED: 1,
    CONFIRMED: 2,
    CHECKED_IN: 3,
    CANCELED: 4,
    EXPIRED: 5,
  }

  return [...filtered].sort((left, right) => {
    const statusDiff = (rank[left.status] ?? 9) - (rank[right.status] ?? 9)
    if (statusDiff !== 0) return statusDiff
    return new Date(left.eventStartTime).getTime() - new Date(right.eventStartTime).getTime()
  })
})

async function load() {
  loading.value = true
  try {
    registrations.value = await eventApi.mine()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function cancelRegistration(registration) {
  const isPromotion = registration.status === 'PROMOTED'
  try {
    await ElMessageBox.confirm(
      isPromotion
        ? '放弃递补后，这个名额会立即释放给下一位候补者。确定放弃吗？'
        : '取消后，已占用的名额会按候补顺序自动递补。确定取消吗？',
      isPromotion ? '放弃递补' : '取消报名',
      {
        type: 'warning',
        confirmButtonText: isPromotion ? '确定放弃' : '确定取消',
        cancelButtonText: '再想想',
      },
    )
  } catch {
    return
  }

  busyId.value = registration.id
  try {
    await eventApi.cancel(registration.id)
    ElMessage.success(isPromotion ? '已放弃递补资格' : '报名已取消')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    busyId.value = null
  }
}

async function confirmPromotion(registration) {
  busyId.value = registration.id
  try {
    await eventApi.confirm(registration.id)
    ElMessage.success('候补资格已确认，到时见')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    busyId.value = null
  }
}

function openEvent(registration) {
  router.push(`/events/${registration.eventId}`)
}

function statusCopy(registration) {
  if (registration.eventStatus === 'CANCELED') {
    return '活动已被管理员取消，当前报名记录仅作历史保留。'
  }
  if (registration.eventStatus === 'FINISHED') {
    return ['CONFIRMED', 'CHECKED_IN'].includes(registration.status)
      ? '活动已经结束，报名与签到记录已归档。'
      : '活动已经结束，候补流程不再继续。'
  }

  const copies = {
    PROMOTED: '你已递补成功，请在倒计时结束前确认参加。',
    WAITLISTED: `当前排在第 ${registration.waitlistPosition || '—'} 位，名额释放后会依次通知。`,
    CONFIRMED: '名额已保留，现场可使用管理员展示的二维码签到。',
    CHECKED_IN: '签到已经完成，活动记录会保留在这里。',
    CANCELED: '这次报名已取消，活动仍开放时可以重新报名。',
    EXPIRED: '递补确认已超时，名额已经释放给下一位候补者。',
  }
  return copies[registration.status] || '报名状态已更新。'
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="page-shell registrations-page">
      <header class="registrations-header">
        <div>
          <p class="eyebrow">MY TICKET POCKET</p>
          <h1 class="page-title">我的票夹</h1>
          <p class="page-subtitle">
            已确认、候补和签到记录都收在这里。轮到递补时会亮起 10 分钟确认倒计时。
          </p>
        </div>
        <el-button class="refresh-button" :icon="Refresh" :loading="loading" @click="load">
          刷新票夹
        </el-button>
      </header>

      <section class="ticket-summary" aria-label="报名概览">
        <article class="stat-card stat-cobalt">
          <span>手上还有安排</span>
          <strong>{{ counts.active }}</strong>
          <small>含待确认与候补</small>
        </article>
        <article class="stat-card stat-paper">
          <span>已经落袋</span>
          <strong>{{ counts.confirmed }}</strong>
          <small>现场扫码即可签到</small>
        </article>
        <article class="stat-card stat-butter">
          <span>等一个空位</span>
          <strong>{{ counts.waitlisted + counts.promoted }}</strong>
          <small>{{ counts.promoted }} 个递补正在等你</small>
        </article>
        <article class="stat-card stat-mint">
          <span>到场盖章</span>
          <strong>{{ counts.checkedIn }}</strong>
          <small>你的校园足迹</small>
        </article>
      </section>

      <section class="records-section">
        <div class="records-toolbar">
          <div>
            <p class="eyebrow">YOUR MOVES</p>
            <h2>报名记录</h2>
          </div>
          <div class="filter-pills">
            <button
              v-for="filter in filters"
              :key="filter.key"
              type="button"
              class="filter-pill"
              :class="{ active: activeFilter === filter.key }"
              @click="activeFilter = filter.key"
            >
              {{ filter.label }}
            </button>
          </div>
        </div>

        <div v-if="loading" class="loading-panel">
          <el-skeleton :rows="6" animated />
        </div>

        <div v-else-if="filteredRegistrations.length" class="registration-list">
          <article
            v-for="registration in filteredRegistrations"
            :key="registration.id"
            class="ticket-card"
            :class="{
              promoted: registration.status === 'PROMOTED',
              muted: ['CANCELED', 'EXPIRED'].includes(registration.status),
            }"
          >
            <div class="ticket-stub">
              <span>{{ formatDateParts(registration.eventStartTime).month }}</span>
              <strong>{{ formatDateParts(registration.eventStartTime).day }}</strong>
              <small>{{ formatDateParts(registration.eventStartTime).weekday }}</small>
              <i aria-hidden="true">CAMPUS</i>
            </div>

            <div class="registration-content">
              <div class="registration-topline">
                <StatusTag :status="registration.status" size="small" />
                <span v-if="registration.status === 'PROMOTED'" class="urgent-label">
                  <el-icon><Warning /></el-icon>
                  快确认，别让票飞走
                </span>
                <span v-else-if="registration.waitlistPosition" class="position-label">
                  候补第 {{ registration.waitlistPosition }} 位
                </span>
                <span class="ticket-id">TICKET {{ String(registration.id).padStart(3, '0') }}</span>
              </div>

              <h3>{{ registration.eventTitle }}</h3>
              <p class="status-copy">{{ statusCopy(registration) }}</p>

              <div class="registration-meta">
                <span>
                  <el-icon><Calendar /></el-icon>
                  {{ formatTimeRange(registration.eventStartTime, registration.eventEndTime) }}
                </span>
                <span>
                  <el-icon><Location /></el-icon>
                  {{ registration.eventLocation }}
                </span>
                <span>
                  <el-icon><Clock /></el-icon>
                  报名于 {{ formatDateTime(registration.createdAt) }}
                </span>
              </div>

              <div v-if="registration.status === 'PROMOTED'" class="deadline-panel">
                <div>
                  <span>确认窗口</span>
                  <CountdownText :deadline="registration.confirmDeadline" prefix="剩" />
                </div>
                <small>截止：{{ formatFullDateTime(registration.confirmDeadline) }}</small>
              </div>

              <div v-if="registration.status === 'CHECKED_IN'" class="checked-panel">
                <el-icon><CircleCheck /></el-icon>
                已在 {{ formatDateTime(registration.checkedInAt) }} 完成签到
              </div>

              <div class="registration-actions">
                <el-button :icon="Tickets" @click="openEvent(registration)">查看活动</el-button>
                <el-button
                  v-if="registration.canConfirm"
                  type="warning"
                  :loading="busyId === registration.id"
                  @click="confirmPromotion(registration)"
                >
                  确认参加
                </el-button>
                <el-button
                  v-if="registration.canCancel"
                  type="danger"
                  plain
                  :loading="busyId === registration.id"
                  @click="cancelRegistration(registration)"
                >
                  {{ registration.status === 'PROMOTED' ? '放弃递补' : '取消报名' }}
                </el-button>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="empty-panel">
          <div class="empty-ticket" aria-hidden="true">0</div>
          <h3>票夹这一格还是空的</h3>
          <p>去公告栏挑一张，回来它就有东西了。</p>
          <el-button type="primary" @click="router.push('/events')">去逛逛活动</el-button>
        </div>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.registrations-page {
  padding-top: 34px;
}

.registrations-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}

.refresh-button {
  flex: 0 0 auto;
  white-space: nowrap;
}

.ticket-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 15px;
  margin-bottom: 46px;
}

.stat-card {
  display: flex;
  min-height: 145px;
  flex-direction: column;
  justify-content: space-between;
  border: 1.5px solid var(--ink);
  border-radius: 14px;
  padding: 17px;
  box-shadow: 3px 3px 0 var(--ink);
}

.stat-card:nth-child(1) {
  transform: rotate(-1.4deg);
}

.stat-card:nth-child(2) {
  transform: rotate(0.8deg) translateY(5px);
}

.stat-card:nth-child(3) {
  transform: rotate(-0.6deg);
}

.stat-card:nth-child(4) {
  transform: rotate(1.2deg) translateY(4px);
}

.stat-cobalt {
  color: var(--surface);
  background: var(--cobalt);
}

.stat-paper {
  background: var(--surface);
}

.stat-butter {
  background: var(--butter);
}

.stat-mint {
  background: var(--mint);
}

.stat-card span {
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.stat-card strong {
  margin: 7px 0;
  font-family: var(--display);
  font-size: 53px;
  font-weight: 900;
  letter-spacing: -0.08em;
  line-height: 0.85;
}

.stat-card small {
  opacity: 0.76;
  font-size: 10px;
  font-weight: 700;
  line-height: 1.3;
}

.records-section {
  position: relative;
}

.records-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 22px;
  margin-bottom: 22px;
  border-bottom: 2px solid var(--ink);
  padding-bottom: 15px;
}

.records-toolbar h2 {
  margin: 0;
  font-family: var(--display);
  font-size: clamp(30px, 4vw, 46px);
  font-weight: 900;
  letter-spacing: -0.055em;
  line-height: 1;
}

.loading-panel {
  border: 1.5px dashed var(--ink);
  border-radius: 18px;
  padding: 32px;
  background: var(--surface);
}

.registration-list {
  display: grid;
  gap: 18px;
}

.ticket-card {
  position: relative;
  display: grid;
  grid-template-columns: 116px minmax(0, 1fr);
  overflow: hidden;
  border: 1.5px solid var(--ink);
  border-radius: 17px;
  background: var(--surface);
  box-shadow: 4px 4px 0 var(--ink);
  animation: paper-in 380ms ease both;
}

.ticket-card::before,
.ticket-card::after {
  position: absolute;
  z-index: 2;
  left: 107px;
  width: 17px;
  height: 17px;
  border: 1.5px solid var(--ink);
  border-radius: 50%;
  background: var(--paper);
  content: "";
}

.ticket-card::before {
  top: -10px;
}

.ticket-card::after {
  bottom: -10px;
}

.ticket-card.promoted {
  background: var(--butter-soft);
  box-shadow: 5px 5px 0 var(--tomato);
}

.ticket-card.muted {
  opacity: 0.68;
  filter: saturate(0.65);
}

.ticket-stub {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-right: 1.5px dashed var(--ink);
  padding: 22px 10px;
  background: var(--surface-2);
  text-align: center;
}

.ticket-stub span,
.ticket-stub small {
  color: var(--ink-2);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
}

.ticket-stub strong {
  margin: 5px 0;
  font-family: var(--display);
  font-size: 47px;
  font-weight: 900;
  letter-spacing: -0.08em;
  line-height: 0.9;
}

.ticket-stub i {
  margin-top: 14px;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 8px;
  font-style: normal;
  font-weight: 800;
  letter-spacing: 0.08em;
  writing-mode: vertical-rl;
}

.registration-content {
  min-width: 0;
  padding: 20px 22px;
}

.registration-topline {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.urgent-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--tomato);
  font-size: 10px;
  font-weight: 900;
}

.position-label {
  color: var(--cobalt-deep);
  font-size: 10px;
  font-weight: 800;
}

.ticket-id {
  margin-left: auto;
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.registration-content h3 {
  margin-bottom: 7px;
  font-family: var(--display);
  font-size: clamp(23px, 3vw, 32px);
  font-weight: 900;
  letter-spacing: -0.045em;
  line-height: 1.08;
}

.status-copy {
  max-width: 760px;
  margin-bottom: 14px;
  color: var(--ink-2);
  font-size: 12px;
  line-height: 1.7;
}

.registration-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 17px;
  margin-bottom: 17px;
  color: var(--ink-3);
  font-size: 10px;
}

.registration-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.registration-meta .el-icon {
  color: var(--tomato);
}

.deadline-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 14px;
  border: 1.5px solid var(--ink);
  border-radius: 8px;
  padding: 9px 11px;
  background: var(--surface);
}

.deadline-panel > div {
  display: flex;
  align-items: center;
  gap: 9px;
}

.deadline-panel span {
  color: var(--ink-2);
  font-size: 10px;
  font-weight: 750;
}

.deadline-panel small {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 8px;
}

.checked-panel {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  border: 1.5px solid var(--mint-deep);
  border-radius: 7px;
  padding: 6px 9px;
  color: var(--mint-deep);
  background: var(--mint);
  font-size: 10px;
  font-weight: 800;
}

.registration-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.empty-panel {
  display: grid;
  place-items: center;
  border: 1.5px dashed var(--ink);
  border-radius: 18px;
  padding: 56px 24px;
  background: var(--surface);
  text-align: center;
}

.empty-ticket {
  display: grid;
  width: 74px;
  height: 54px;
  margin-bottom: 16px;
  place-items: center;
  border: 2px dashed var(--cobalt);
  border-radius: 8px;
  color: var(--cobalt);
  font-family: var(--display);
  font-size: 30px;
  font-weight: 900;
  transform: rotate(-6deg);
}

.empty-panel h3 {
  margin-bottom: 7px;
  font-family: var(--display);
  font-size: 27px;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.empty-panel p {
  margin-bottom: 18px;
  color: var(--ink-2);
  font-size: 13px;
}

@media (max-width: 920px) {
  .ticket-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-card:nth-child(n) {
    transform: none;
  }

  .records-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 620px) {
  .registrations-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .ticket-summary {
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .stat-card {
    min-height: 124px;
    padding: 14px;
  }

  .stat-card strong {
    font-size: 45px;
  }

  .ticket-card {
    grid-template-columns: 82px minmax(0, 1fr);
  }

  .ticket-card::before,
  .ticket-card::after {
    left: 73px;
  }

  .ticket-stub strong {
    font-size: 36px;
  }

  .ticket-stub i {
    display: none;
  }

  .registration-content {
    padding: 16px;
  }

  .ticket-id {
    display: none;
  }

  .deadline-panel {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }
}

</style>
