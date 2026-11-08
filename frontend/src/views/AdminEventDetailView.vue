<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Calendar,
  CircleCheck,
  Clock,
  DataAnalysis,
  Download,
  Edit,
  Location,
  Refresh,
  Search,
  Tickets,
  User,
  Warning,
} from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import AdminShell from '../components/AdminShell.vue'
import CountdownText from '../components/CountdownText.vue'
import StatusTag from '../components/StatusTag.vue'
import { adminApi } from '../api/admin'
import {
  formatDateTime,
  formatFullDateTime,
  formatTimeRange,
} from '../utils/format'

const route = useRoute()
const router = useRouter()

const eventId = computed(() => Number(route.params.id))
const detail = ref(null)
const registrations = ref([])
const loading = ref(true)
const activeTab = ref('registrations')
const search = ref('')
const statusFilter = ref('ALL')
const checkInBusyId = ref(null)
const statusBusy = ref(false)
const exporting = ref(false)
const generatingToken = ref(false)
const tokenData = ref(null)
const qrDataUrl = ref('')

const registrationStatuses = [
  { value: 'ALL', label: '全部状态' },
  { value: 'CONFIRMED', label: '已确认' },
  { value: 'PROMOTED', label: '待确认' },
  { value: 'WAITLISTED', label: '候补中' },
  { value: 'CHECKED_IN', label: '已签到' },
  { value: 'CANCELED', label: '已取消' },
  { value: 'EXPIRED', label: '已过期' },
]

const filteredRegistrations = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return registrations.value.filter((item) => {
    const matchesStatus = statusFilter.value === 'ALL' || item.status === statusFilter.value
    const matchesKeyword =
      !keyword ||
      [item.username, item.displayName].join(' ').toLowerCase().includes(keyword)
    return matchesStatus && matchesKeyword
  })
})

const activeRegistrationCount = computed(() =>
  registrations.value.filter((item) =>
    ['CONFIRMED', 'PROMOTED', 'WAITLISTED', 'CHECKED_IN'].includes(item.status),
  ).length,
)

const checkInUrl = computed(() => {
  if (!tokenData.value?.token) return ''
  return `${window.location.origin}/check-in?token=${encodeURIComponent(tokenData.value.token)}`
})

const isLoopbackAddress = computed(() =>
  ['localhost', '127.0.0.1', '::1'].includes(window.location.hostname),
)

async function load() {
  loading.value = true
  try {
    const [eventDetail, list] = await Promise.all([
      adminApi.detail(eventId.value),
      adminApi.registrations(eventId.value),
    ])
    detail.value = eventDetail
    registrations.value = list
  } catch (error) {
    ElMessage.error(error.message)
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function refreshRegistrations() {
  try {
    const [eventDetail, list] = await Promise.all([
      adminApi.detail(eventId.value),
      adminApi.registrations(eventId.value),
    ])
    detail.value = eventDetail
    registrations.value = list
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function manualCheckIn(row) {
  try {
    await ElMessageBox.confirm(
      `确认为「${row.displayName}（${row.username}）」手动签到吗？`,
      '手动签到',
      { type: 'info', confirmButtonText: '确认签到', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  checkInBusyId.value = row.id
  try {
    await adminApi.checkIn(row.id)
    ElMessage.success('签到成功')
    await refreshRegistrations()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    checkInBusyId.value = null
  }
}

async function publishEvent() {
  try {
    await ElMessageBox.confirm(
      '发布后，活动会出现在学生活动广场，且核心信息不能再修改。确定发布吗？',
      '发布活动',
      { type: 'info', confirmButtonText: '确定发布', cancelButtonText: '再检查一下' },
    )
  } catch {
    return
  }

  statusBusy.value = true
  try {
    await adminApi.publish(eventId.value)
    ElMessage.success('活动已发布')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    statusBusy.value = false
  }
}

async function finishEvent() {
  try {
    await ElMessageBox.confirm(
      '结束后将不能再进行二维码或手动签到，历史名单会保留。确定结束吗？',
      '结束活动',
      { type: 'warning', confirmButtonText: '确定结束', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  statusBusy.value = true
  try {
    await adminApi.finish(eventId.value)
    tokenData.value = null
    qrDataUrl.value = ''
    ElMessage.success('活动已结束')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    statusBusy.value = false
  }
}

async function cancelEvent() {
  try {
    await ElMessageBox.confirm(
      '取消活动会同步取消所有有效报名，并清空当前占用名额，此操作不可恢复。确定取消吗？',
      '取消活动',
      { type: 'error', confirmButtonText: '确定取消活动', cancelButtonText: '返回' },
    )
  } catch {
    return
  }

  statusBusy.value = true
  try {
    await adminApi.cancel(eventId.value)
    tokenData.value = null
    qrDataUrl.value = ''
    ElMessage.success('活动已取消')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    statusBusy.value = false
  }
}

async function exportRegistrations() {
  exporting.value = true
  try {
    const response = await adminApi.export(eventId.value)
    const blob =
      response.data instanceof Blob
        ? response.data
        : new Blob([response.data], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `活动报名名单-${eventId.value}.xlsx`
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('Excel 已开始下载')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    exporting.value = false
  }
}

async function generateCheckInToken() {
  if (!detail.value || detail.value.status !== 'PUBLISHED') {
    ElMessage.warning('只有已发布活动可以生成签到二维码')
    return
  }

  generatingToken.value = true
  try {
    tokenData.value = await adminApi.generateCheckInToken(eventId.value)
    const url = `${window.location.origin}/check-in?token=${encodeURIComponent(
      tokenData.value.token,
    )}`
    qrDataUrl.value = await QRCode.toDataURL(url, {
      width: 380,
      margin: 2,
      errorCorrectionLevel: 'M',
      color: {
        dark: '#1f3f9e',
        light: '#ffffff',
      },
    })
    ElMessage.success('签到二维码已生成，有效期 5 分钟')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    generatingToken.value = false
  }
}

async function copyCheckInLink() {
  if (!checkInUrl.value) return
  try {
    await navigator.clipboard.writeText(checkInUrl.value)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = checkInUrl.value
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    textarea.remove()
  }
  ElMessage.success('签到链接已复制')
}

function backToList() {
  router.push('/admin/events')
}

onMounted(load)
</script>

<template>
  <AdminShell>
    <div v-if="loading" class="page-shell detail-loading">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="detail" class="page-shell admin-detail-page">
      <button type="button" class="back-link" @click="backToList">
        <el-icon><ArrowLeft /></el-icon>
        返回活动管理
      </button>

      <header class="admin-detail-hero">
        <div class="hero-copy">
          <p class="eyebrow">EVENT OVERVIEW</p>
          <div class="hero-kicker">
            <StatusTag :status="detail.status" mode="admin" />
            <span>活动 #{{ detail.id }}</span>
            <span>{{ detail.category }}</span>
          </div>
          <h1>{{ detail.title }}</h1>
          <p>{{ detail.description || '这场活动暂时没有补充描述。' }}</p>

          <div class="hero-meta">
            <span>
              <el-icon><Calendar /></el-icon>
              {{ formatTimeRange(detail.startTime, detail.endTime) }}
            </span>
            <span>
              <el-icon><Location /></el-icon>
              {{ detail.location }}
            </span>
            <span>
              <el-icon><User /></el-icon>
              {{ detail.organizer }}
            </span>
            <span>
              <el-icon><Clock /></el-icon>
              报名至 {{ formatFullDateTime(detail.registrationEnd) }}
            </span>
          </div>
        </div>

        <aside class="operation-panel surface-card">
          <p class="eyebrow">EVENT ACTIONS</p>
          <h2>活动操作</h2>
          <p class="operation-copy">
            名单、签到与导出都基于当前活动。二维码只对已确认报名且已登录的学生有效。
          </p>
          <div class="operation-actions">
            <el-button :icon="Refresh" @click="load">刷新数据</el-button>
            <el-button :icon="Download" :loading="exporting" @click="exportRegistrations">
              导出名单
            </el-button>
            <el-button
              v-if="detail.status === 'DRAFT'"
              type="primary"
              :icon="Edit"
              @click="router.push({ path: '/admin/events', query: { edit: detail.id } })"
            >
              编辑活动
            </el-button>
            <el-button
              v-if="detail.status === 'DRAFT'"
              type="success"
              :loading="statusBusy"
              @click="publishEvent"
            >
              发布活动
            </el-button>
            <el-button
              v-if="detail.status === 'PUBLISHED'"
              type="warning"
              :loading="statusBusy"
              @click="finishEvent"
            >
              结束活动
            </el-button>
            <el-button
              v-if="detail.status === 'PUBLISHED'"
              type="danger"
              plain
              :loading="statusBusy"
              @click="cancelEvent"
            >
              取消活动
            </el-button>
          </div>
        </aside>
      </header>

      <section class="detail-stats">
        <article>
          <span>名额占用</span>
          <strong>{{ detail.occupiedCount }}<small>/{{ detail.capacity }}</small></strong>
          <el-progress
            :percentage="Math.min(100, Math.round((detail.occupiedCount / detail.capacity) * 100))"
            :show-text="false"
            :stroke-width="6"
          :color="detail.remainingSeats > 0 ? 'var(--cobalt)' : 'var(--tomato)'"
          />
        </article>
        <article>
          <span>待核验 / 已签到</span>
          <strong>{{ detail.confirmedCount }}<small> / {{ detail.checkedInCount }}</small></strong>
          <p>活动现场核验进度</p>
        </article>
        <article class="amber-stat">
          <span>待确认递补</span>
          <strong>{{ detail.promotedCount }}</strong>
          <p>超时后会自动释放给下一位</p>
        </article>
        <article>
          <span>候补队列</span>
          <strong>{{ detail.waitlistedCount }}</strong>
          <p>共 {{ activeRegistrationCount }} 条有效报名记录</p>
        </article>
      </section>

      <el-tabs v-model="activeTab" class="detail-tabs surface-card">
        <el-tab-pane name="registrations">
          <template #label>
            <span class="tab-label">
              <el-icon><DataAnalysis /></el-icon>
              报名记录
              <b>{{ registrations.length }}</b>
            </span>
          </template>

          <div class="tab-toolbar">
            <div>
              <h2>报名与签到记录</h2>
              <p>候补顺序按进入队列时间与记录编号排序，保证同一时刻进入队列时仍有稳定顺序。</p>
            </div>
            <div class="tab-filters">
              <el-input
                v-model="search"
                :prefix-icon="Search"
                placeholder="搜索姓名或用户名"
                clearable
              />
              <el-select v-model="statusFilter" style="width: 140px">
                <el-option
                  v-for="option in registrationStatuses"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
          </div>

          <el-table
            :data="filteredRegistrations"
            row-key="id"
            empty-text="没有匹配的报名记录"
            class="registration-table"
          >
            <el-table-column label="学生" min-width="180">
              <template #default="{ row }">
                <div class="student-cell">
                  <span class="student-avatar">{{ row.displayName?.slice(0, 1) || '?' }}</span>
                  <div>
                    <strong>{{ row.displayName }}</strong>
                    <small>@{{ row.username }}</small>
                  </div>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="状态" width="105">
              <template #default="{ row }">
                <StatusTag :status="row.status" size="small" mode="admin" />
              </template>
            </el-table-column>

            <el-table-column label="候补顺位" width="100">
              <template #default="{ row }">
                <span class="table-muted">
                  {{ row.waitlistPosition ? `第 ${row.waitlistPosition} 位` : '—' }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="报名时间" min-width="135">
              <template #default="{ row }">
                <span class="table-muted">{{ formatDateTime(row.createdAt) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="递补截止" min-width="135">
              <template #default="{ row }">
                <CountdownText
                  v-if="row.status === 'PROMOTED'"
                  :deadline="row.confirmDeadline"
                  prefix="剩余"
                />
                <span v-else class="table-muted">—</span>
              </template>
            </el-table-column>

            <el-table-column label="签到时间" min-width="135">
              <template #default="{ row }">
                <span class="table-muted">
                  {{ row.checkedInAt ? formatDateTime(row.checkedInAt) : '—' }}
                </span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button
                  v-if="row.canCheckIn"
                  link
                  type="primary"
                  :icon="CircleCheck"
                  :loading="checkInBusyId === row.id"
                  @click="manualCheckIn(row)"
                >
                  手动签到
                </el-button>
                <span v-else-if="row.status === 'CHECKED_IN'" class="checked-text">已签到</span>
                <span v-else class="table-muted">—</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="mobile-registration-list">
            <article v-for="row in filteredRegistrations" :key="`mobile-${row.id}`">
              <div class="mobile-student-head">
                <div class="student-cell">
                  <span class="student-avatar">{{ row.displayName?.slice(0, 1) || '?' }}</span>
                  <div>
                    <strong>{{ row.displayName }}</strong>
                    <small>@{{ row.username }}</small>
                  </div>
                </div>
                <StatusTag :status="row.status" size="small" mode="admin" />
              </div>

              <div class="mobile-registration-meta">
                <span>
                  候补顺位
                  <strong>{{ row.waitlistPosition ? `第 ${row.waitlistPosition} 位` : '—' }}</strong>
                </span>
                <span>
                  报名时间
                  <strong>{{ formatDateTime(row.createdAt) }}</strong>
                </span>
                <span>
                  签到时间
                  <strong>{{ row.checkedInAt ? formatDateTime(row.checkedInAt) : '—' }}</strong>
                </span>
              </div>

              <div v-if="row.status === 'PROMOTED'" class="mobile-promotion">
                <span>递补确认窗口</span>
                <CountdownText :deadline="row.confirmDeadline" prefix="剩余" />
              </div>

              <el-button
                v-if="row.canCheckIn"
                type="primary"
                :icon="CircleCheck"
                :loading="checkInBusyId === row.id"
                @click="manualCheckIn(row)"
              >
                手动签到
              </el-button>
              <span v-else-if="row.status === 'CHECKED_IN'" class="checked-text">已签到</span>
            </article>
            <div v-if="!filteredRegistrations.length" class="mobile-empty">
              没有匹配的报名记录
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="check-in">
          <template #label>
            <span class="tab-label">
              <el-icon><Tickets /></el-icon>
              现场签到
            </span>
          </template>

          <div v-if="detail.status === 'PUBLISHED'" class="check-in-workspace">
            <section class="check-in-copy">
              <p class="eyebrow">ON-SITE CHECK-IN</p>
              <h2>生成短时签到二维码</h2>
              <p>
                管理员生成活动级短时 token，学生扫码后仍需登录。后端会校验 token
                签名、过期时间、报名状态和用户身份，重复请求也只返回同一次签到结果。
              </p>

              <div class="logic-list">
                <div>
                  <strong>01</strong>
                  <span>二维码默认 5 分钟过期，降低转发风险</span>
                </div>
                <div>
                  <strong>02</strong>
                  <span>仅“已确认”报名可以签到</span>
                </div>
                <div>
                  <strong>03</strong>
                  <span>签到成功后更新为“已签到”，重复扫码不会重复写入</span>
                </div>
              </div>

              <el-button
                type="primary"
                size="large"
                :icon="Refresh"
                :loading="generatingToken"
                @click="generateCheckInToken"
              >
                {{ qrDataUrl ? '重新生成签到码' : '生成签到二维码' }}
              </el-button>
            </section>

            <section class="qr-panel">
              <div class="qr-frame" :class="{ ready: qrDataUrl }">
                <img v-if="qrDataUrl" :src="qrDataUrl" alt="活动签到二维码" />
                <div v-else class="qr-placeholder">
                  <el-icon><Tickets /></el-icon>
                  <strong>等待生成</strong>
                  <span>点击左侧按钮创建签到码</span>
                </div>
              </div>

              <template v-if="tokenData">
                <div class="qr-status">
                  <span>签到码有效期</span>
                  <CountdownText :deadline="tokenData.expiresAt" prefix="剩余" />
                </div>
                <p class="qr-expiry">过期时间：{{ formatFullDateTime(tokenData.expiresAt) }}</p>
                <el-button :icon="Download" @click="copyCheckInLink">复制签到链接</el-button>
              </template>
              <p v-else class="qr-hint">生成后请把屏幕展示给学生扫描，不要截图长期转发。</p>
            </section>
          </div>

          <div v-else class="check-in-closed">
            <el-icon><Warning /></el-icon>
            <h3>当前活动不能生成签到码</h3>
            <p>只有状态为「报名中」的活动可以生成现场签到二维码。</p>
          </div>

          <el-alert
            v-if="detail.status === 'PUBLISHED' && isLoopbackAddress"
            title="你正在通过 localhost 访问，手机无法打开二维码里的地址。"
            description="请让前端以局域网 IP 访问，例如 http://192.168.x.x:5173，再从该地址生成二维码。"
            type="warning"
            :closable="false"
            show-icon
            class="lan-alert"
          />
        </el-tab-pane>
      </el-tabs>
    </div>

    <div v-else class="page-shell admin-detail-page">
      <div class="surface-card empty-panel">
        <el-empty description="活动不存在或已经被移除">
          <el-button type="primary" @click="backToList">返回运营台</el-button>
        </el-empty>
      </div>
    </div>
  </AdminShell>
</template>

<style scoped>
.detail-loading {
  padding-top: 64px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  border: 0;
  padding: 0;
  color: var(--ink-soft);
  background: transparent;
  font-size: 13px;
  cursor: pointer;
}

.back-link:hover {
  color: var(--green);
}

.admin-detail-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(310px, 0.65fr);
  gap: 24px;
  align-items: start;
  margin-bottom: 20px;
}

.hero-copy {
  padding: 20px 0 8px;
}

.hero-kicker {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  margin-bottom: 18px;
  color: var(--ink-faint);
  font-size: 11px;
}

.hero-copy h1 {
  margin-bottom: 14px;
  font-family: var(--serif);
  font-size: clamp(38px, 4.8vw, 64px);
  line-height: 1.08;
  letter-spacing: -0.05em;
}

.hero-copy > p {
  max-width: 690px;
  margin-bottom: 22px;
  color: var(--ink-soft);
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-line;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 9px 18px;
  color: var(--ink-soft);
  font-size: 12px;
}

.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.hero-meta .el-icon {
  color: var(--green);
}

.operation-panel {
  padding: 24px;
}

.operation-panel h2 {
  margin-bottom: 8px;
  font-family: var(--serif);
  font-size: 27px;
}

.operation-copy {
  margin-bottom: 16px;
  color: var(--ink-soft);
  font-size: 12px;
  line-height: 1.7;
}

.operation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.operation-actions .el-button {
  margin-left: 0;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 13px;
  margin-bottom: 20px;
}

.detail-stats article {
  display: flex;
  flex-direction: column;
  min-height: 145px;
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 18px;
  background: rgba(255, 253, 248, 0.8);
  box-shadow: var(--shadow-sm);
}

.detail-stats article > span {
  color: var(--ink-faint);
  font-size: 11px;
}

.detail-stats strong {
  margin: auto 0 10px;
  font-family: var(--serif);
  font-size: 39px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.detail-stats strong small {
  color: var(--ink-faint);
  font-family: var(--sans);
  font-size: 14px;
}

.detail-stats p {
  margin: 0;
  color: var(--ink-faint);
  font-size: 10px;
}

.detail-stats .amber-stat {
  background: linear-gradient(145deg, rgba(247, 232, 200, 0.8), rgba(255, 253, 248, 0.9));
}

.detail-stats .amber-stat strong {
  color: #9a6415;
}

.detail-tabs {
  overflow: hidden;
  padding: 0 24px 24px;
  border-radius: 24px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.detail-tabs :deep(.el-tabs__item) {
  height: 60px;
  font-weight: 600;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-label b {
  border-radius: 999px;
  padding: 1px 7px;
  color: var(--green-deep);
  background: var(--green-soft);
  font-size: 10px;
}

.tab-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
}

.tab-toolbar h2 {
  margin-bottom: 4px;
  font-family: var(--serif);
  font-size: 26px;
}

.tab-toolbar p {
  margin-bottom: 0;
  color: var(--ink-faint);
  font-size: 11px;
}

.tab-filters {
  display: flex;
  gap: 8px;
}

.tab-filters .el-input {
  width: 220px;
}

.registration-table {
  --el-table-border-color: var(--line);
  --el-table-header-bg-color: var(--surface-soft);
  --el-table-row-hover-bg-color: rgba(220, 234, 223, 0.32);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.student-avatar {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 11px;
  color: var(--green-deep);
  background: var(--green-soft);
  font-family: var(--serif);
  font-size: 16px;
  font-weight: 700;
}

.student-cell div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.student-cell strong {
  font-size: 13px;
}

.student-cell small,
.table-muted {
  color: var(--ink-faint);
  font-size: 11px;
}

.checked-text {
  color: var(--green);
  font-size: 11px;
  font-weight: 600;
}

.check-in-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 24px;
  padding: 8px 0 6px;
}

.check-in-copy {
  padding: 18px 12px 8px 4px;
}

.check-in-copy h2 {
  margin-bottom: 16px;
  font-family: var(--serif);
  font-size: clamp(32px, 4vw, 48px);
  line-height: 1.15;
  letter-spacing: -0.04em;
}

.check-in-copy > p:not(.eyebrow) {
  max-width: 640px;
  margin-bottom: 22px;
  color: var(--ink-soft);
  font-size: 13px;
  line-height: 1.85;
}

.logic-list {
  display: grid;
  gap: 9px;
  margin-bottom: 24px;
}

.logic-list div {
  display: flex;
  align-items: center;
  gap: 11px;
  border: 1.5px dashed var(--line);
  padding: 7px 0 7px 12px;
}

.logic-list strong {
  color: var(--amber);
  font-family: var(--serif);
  font-size: 14px;
}

.logic-list span {
  color: var(--ink-soft);
  font-size: 12px;
}

.qr-panel {
  display: flex;
  align-items: center;
  flex-direction: column;
  border: 1px solid var(--line);
  border-radius: 24px;
  padding: 24px;
  background:
    radial-gradient(circle at 50% 0%, rgba(217, 154, 50, 0.11), transparent 15rem),
    var(--surface-soft);
}

.qr-frame {
  display: grid;
  width: min(100%, 300px);
  aspect-ratio: 1;
  place-items: center;
  border: 1px dashed var(--line-strong);
  border-radius: 22px;
  padding: 12px;
  background: var(--surface);
}

.qr-frame.ready {
  border-style: solid;
  border-color: rgba(47, 107, 79, 0.24);
}

.qr-frame img {
  width: 100%;
  height: 100%;
  border-radius: 13px;
}

.qr-placeholder {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 7px;
  color: var(--ink-faint);
}

.qr-placeholder .el-icon {
  margin-bottom: 6px;
  color: rgba(47, 107, 79, 0.36);
  font-size: 54px;
}

.qr-placeholder strong {
  color: var(--ink-soft);
  font-family: var(--serif);
  font-size: 19px;
}

.qr-placeholder span {
  font-size: 11px;
}

.qr-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: min(100%, 300px);
  margin-top: 16px;
  border-radius: 13px;
  padding: 10px 12px;
  color: var(--ink-soft);
  background: rgba(255, 253, 248, 0.85);
  font-size: 11px;
}

.qr-expiry,
.qr-hint {
  margin: 9px 0 14px;
  color: var(--ink-faint);
  font-size: 10px;
  text-align: center;
}

.check-in-closed {
  display: flex;
  align-items: center;
  flex-direction: column;
  justify-content: center;
  min-height: 340px;
  text-align: center;
}

.check-in-closed .el-icon {
  margin-bottom: 12px;
  color: var(--amber);
  font-size: 42px;
}

.check-in-closed h3 {
  margin-bottom: 6px;
  font-family: var(--serif);
  font-size: 25px;
}

.check-in-closed p {
  color: var(--ink-soft);
  font-size: 12px;
}

.lan-alert {
  margin-top: 18px;
}

@media (max-width: 980px) {
  .admin-detail-hero,
  .check-in-workspace {
    grid-template-columns: 1fr;
  }

  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .check-in-copy {
    padding-right: 4px;
  }
}

@media (max-width: 720px) {
  .tab-toolbar,
  .tab-filters {
    align-items: stretch;
    flex-direction: column;
  }

  .tab-filters {
    width: 100%;
  }

  .tab-filters .el-input,
  .tab-filters .el-select {
    width: 100% !important;
  }

  .detail-tabs {
    padding: 0 14px 18px;
  }
}

@media (max-width: 520px) {
  .detail-stats {
    grid-template-columns: 1fr;
  }

  .operation-actions {
    flex-direction: column;
  }

  .operation-actions .el-button {
    width: 100%;
  }
}
</style>

<style scoped>
.detail-loading {
  padding-top: 48px;
}

.back-link {
  border-bottom: 1.5px solid currentColor;
  padding-bottom: 3px;
  color: var(--ink-2);
  font-size: 11px;
  font-weight: 800;
}

.back-link:hover {
  color: var(--cobalt);
}

.admin-detail-hero {
  gap: 26px;
  margin-bottom: 22px;
}

.hero-copy {
  padding-top: 8px;
}

.hero-kicker {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
}

.hero-copy h1 {
  font-family: var(--display);
  font-size: clamp(40px, 5.1vw, 68px);
  font-weight: 900;
  letter-spacing: -0.065em;
  line-height: 0.99;
}

.hero-copy > p:not(.eyebrow) {
  color: var(--ink-2);
  font-size: 13px;
}

.hero-meta {
  border-top: 1.5px dashed var(--line);
  padding-top: 13px;
  color: var(--ink-2);
  font-size: 10px;
}

.hero-meta .el-icon {
  color: var(--tomato);
}

.operation-panel {
  border: 1.5px solid var(--ink);
  border-radius: 14px;
  padding: 21px;
  background: var(--surface);
  box-shadow: 4px 4px 0 var(--ink);
}

.operation-panel .eyebrow {
  margin-bottom: 9px;
  color: var(--tomato);
}

.operation-panel h2 {
  font-family: var(--display);
  font-size: 27px;
  font-weight: 900;
  letter-spacing: -0.045em;
}

.operation-copy {
  color: var(--ink-2);
  font-size: 11px;
}

.operation-actions .el-button {
  margin-left: 0;
}

.detail-stats {
  gap: 11px;
  margin-bottom: 22px;
}

.detail-stats article {
  min-height: 126px;
  border: 1.5px solid var(--ink);
  border-radius: 10px;
  padding: 15px;
  background: var(--surface);
  box-shadow: 2px 2px 0 var(--ink);
}

.detail-stats article:nth-child(2) {
  background: var(--cobalt-soft);
}

.detail-stats article:nth-child(4) {
  background: var(--mint);
}

.detail-stats article > span {
  color: var(--ink-2);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
}

.detail-stats strong {
  font-family: var(--display);
  font-size: 38px;
  font-weight: 900;
  letter-spacing: -0.06em;
}

.detail-stats strong small {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 11px;
}

.detail-stats p {
  color: var(--ink-3);
  font-size: 9px;
}

.detail-stats .amber-stat {
  background: var(--butter-soft);
}

.detail-stats .amber-stat strong {
  color: var(--tomato);
}

.detail-tabs {
  border: 1.5px solid var(--ink);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 4px 4px 0 var(--ink);
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.detail-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--line);
}

.detail-tabs :deep(.el-tabs__item) {
  height: 52px;
  font-size: 13px;
  font-weight: 800;
}

.tab-label b {
  border: 1px solid var(--ink);
  color: var(--ink);
  background: var(--butter);
  font-family: var(--mono);
}

.tab-toolbar {
  align-items: center;
  border-bottom: 1.5px dashed var(--line);
  padding-bottom: 13px;
}

.tab-toolbar h2 {
  font-family: var(--display);
  font-size: 28px;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.tab-toolbar p {
  color: var(--ink-3);
  font-size: 9px;
}

.registration-table {
  --el-table-header-bg-color: var(--surface-2);
  --el-table-row-hover-bg-color: var(--butter-soft);
}

.student-avatar {
  border: 1.5px solid var(--ink);
  border-radius: 8px;
  color: var(--cobalt-deep);
  background: var(--cobalt-soft);
  font-family: var(--display);
  font-size: 15px;
  font-weight: 900;
}

.student-cell strong {
  font-size: 12px;
  font-weight: 800;
}

.student-cell small,
.table-muted {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
}

.checked-text {
  color: var(--mint-deep);
  font-size: 10px;
  font-weight: 800;
}

.check-in-workspace {
  gap: 25px;
}

.check-in-copy h2 {
  font-family: var(--display);
  font-size: clamp(33px, 4.2vw, 52px);
  font-weight: 900;
  letter-spacing: -0.06em;
  line-height: 1.02;
}

.check-in-copy > p:not(.eyebrow) {
  color: var(--ink-2);
  font-size: 12px;
}

.logic-list div {
  border: 1.5px dashed var(--line);
  border-radius: 0 7px 7px 0;
  padding: 8px 11px;
  background: var(--surface-2);
}

.logic-list strong {
  color: var(--tomato);
  font-family: var(--mono);
  font-size: 11px;
  font-weight: 900;
}

.logic-list span {
  color: var(--ink-2);
  font-size: 11px;
}

.qr-panel {
  border: 1.5px solid var(--ink);
  border-radius: 14px;
  padding: 20px;
  background: var(--butter-soft);
  box-shadow: 4px 4px 0 var(--ink);
  transform: rotate(0.6deg);
}

.qr-frame {
  border: 2px dashed var(--ink);
  border-radius: 11px;
  padding: 10px;
  background: var(--surface);
}

.qr-frame.ready {
  border-style: solid;
  border-color: var(--ink);
}

.qr-frame img {
  border-radius: 4px;
}

.qr-placeholder .el-icon {
  color: var(--cobalt);
}

.qr-placeholder strong {
  color: var(--ink);
  font-family: var(--display);
  font-weight: 900;
}

.qr-placeholder span {
  color: var(--ink-2);
}

.qr-status {
  border: 1.5px solid var(--ink);
  border-radius: 8px;
  color: var(--ink-2);
  background: var(--surface);
}

.qr-expiry,
.qr-hint {
  color: var(--ink-2);
}

.check-in-closed .el-icon {
  color: var(--tomato);
}

.check-in-closed h3 {
  font-family: var(--display);
  font-weight: 900;
}

.check-in-closed p {
  color: var(--ink-2);
}

.mobile-registration-list {
  display: none;
}

@media (max-width: 980px) {
  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .qr-panel {
    width: min(420px, 100%);
    transform: none;
  }
}

@media (max-width: 720px) {
  .registration-table {
    display: none;
  }

  .mobile-registration-list {
    display: grid;
    gap: 10px;
  }

  .mobile-registration-list > article {
    border: 1.5px solid var(--ink);
    border-radius: 10px;
    padding: 13px;
    background: var(--surface);
    box-shadow: 2px 2px 0 var(--ink);
  }

  .mobile-student-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 12px;
  }

  .mobile-registration-meta {
    display: grid;
    gap: 7px;
    margin-bottom: 12px;
    border-top: 1.5px dashed var(--line);
    padding-top: 10px;
  }

  .mobile-registration-meta span {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 12px;
    color: var(--ink-3);
    font-family: var(--mono);
    font-size: 8px;
  }

  .mobile-registration-meta strong {
    color: var(--ink);
    font-family: var(--sans);
    font-size: 10px;
  }

  .mobile-promotion {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 11px;
    border: 1.5px solid var(--ink);
    border-radius: 7px;
    padding: 8px 9px;
    background: var(--butter-soft);
    font-size: 9px;
    font-weight: 750;
  }

  .mobile-registration-list :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .mobile-empty {
    padding: 34px 12px;
    color: var(--ink-3);
    text-align: center;
    font-size: 12px;
  }
}

@media (max-width: 520px) {
  .detail-stats {
    grid-template-columns: 1fr 1fr;
    gap: 9px;
  }

  .detail-stats article {
    min-height: 112px;
    padding: 12px;
  }

  .detail-stats strong {
    font-size: 31px;
  }
}
</style>

<style scoped>
.detail-loading {
  padding-top: 42px;
}

.detail-loading :deep(.el-skeleton) {
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
  padding: 22px;
  background: var(--admin-panel);
}

.back-link {
  gap: 5px;
  margin-bottom: 15px;
  border: 0;
  padding: 0;
  color: var(--admin-ink-3);
  font-size: 11px;
  font-weight: 550;
}

.back-link:hover {
  color: var(--admin-accent);
}

.admin-detail-hero {
  grid-template-columns: minmax(0, 1.45fr) minmax(290px, 0.55fr);
  gap: 14px;
  align-items: stretch;
  margin-bottom: 14px;
}

.hero-copy {
  min-width: 0;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
  padding: 24px;
  background:
    linear-gradient(125deg, rgba(45, 91, 216, 0.035), transparent 42%),
    var(--admin-panel);
  box-shadow: var(--admin-shadow);
}

.hero-kicker {
  gap: 8px;
  margin-bottom: 13px;
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 9px;
  font-weight: 550;
}

.hero-kicker > span:not(.status-tag) {
  display: inline-flex;
  min-height: 22px;
  align-items: center;
  border: 1px solid var(--admin-border);
  border-radius: 999px;
  padding: 2px 8px;
  background: var(--admin-panel-strong);
}

.hero-copy h1 {
  max-width: 900px;
  margin-bottom: 10px;
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 31px;
  font-weight: 720;
  letter-spacing: -0.035em;
  line-height: 1.25;
}

.hero-copy > p {
  max-width: 760px;
  margin-bottom: 18px;
  color: var(--admin-ink-2);
  font-size: 12px;
  line-height: 1.75;
}

.hero-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0;
  margin-top: 18px;
  border-top: 1px solid var(--admin-border);
  padding-top: 0;
  color: var(--admin-ink-2);
  font-size: 10px;
}

.hero-meta span {
  min-width: 0;
  align-items: flex-start;
  gap: 7px;
  border-bottom: 1px solid var(--admin-border);
  padding: 11px 14px 11px 0;
  line-height: 1.55;
}

.hero-meta span:nth-child(even) {
  border-left: 1px solid var(--admin-border);
  padding-left: 14px;
}

.hero-meta span:nth-last-child(-n + 2) {
  border-bottom: 0;
  padding-bottom: 0;
}

.hero-meta .el-icon {
  flex: 0 0 auto;
  margin-top: 1px;
  color: var(--admin-accent);
  font-size: 13px;
}

.operation-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
  padding: 20px;
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
}

.operation-panel .eyebrow {
  margin-bottom: 6px;
}

.operation-panel h2 {
  margin-bottom: 7px;
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.015em;
}

.operation-copy {
  margin-bottom: 15px;
  color: var(--admin-ink-3);
  font-size: 10px;
  line-height: 1.7;
}

.operation-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  margin-top: auto;
}

.operation-actions .el-button {
  width: 100%;
  min-height: 34px;
  margin-left: 0;
  padding-right: 8px;
  padding-left: 8px;
  font-size: 11px;
}

.operation-actions .el-button--danger {
  color: var(--admin-down);
  background: var(--admin-panel-strong);
}

.detail-stats {
  gap: 10px;
  margin-bottom: 14px;
}

.detail-stats article {
  position: relative;
  min-height: 112px;
  overflow: hidden;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  padding: 15px;
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
}

.detail-stats article:nth-child(2),
.detail-stats article:nth-child(4) {
  background: var(--admin-panel);
}

.detail-stats article > span {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 550;
  letter-spacing: 0;
}

.detail-stats strong {
  margin: auto 0 8px;
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 29px;
  font-weight: 720;
  letter-spacing: -0.035em;
  line-height: 1;
}

.detail-stats strong small {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 11px;
  font-weight: 500;
}

.detail-stats p {
  color: var(--admin-ink-3);
  font-size: 9px;
}

.detail-stats .amber-stat {
  border-color: rgba(183, 122, 22, 0.17);
  background: linear-gradient(145deg, rgba(183, 122, 22, 0.08), var(--admin-panel) 68%);
}

.detail-stats .amber-stat strong {
  color: var(--admin-amber);
}

.detail-tabs {
  overflow: hidden;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
  padding: 0 20px 20px;
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
}

.detail-tabs :deep(.el-tabs__header) {
  margin: 0 0 18px;
}

.detail-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0;
}

.detail-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: var(--admin-border);
}

.detail-tabs :deep(.el-tabs__item) {
  height: 52px;
  padding-right: 18px;
  padding-left: 18px;
  font-size: 12px;
  font-weight: 600;
}

.detail-tabs :deep(.el-tabs__item:first-child) {
  padding-left: 0;
}

.detail-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.tab-label {
  gap: 6px;
}

.tab-label b {
  border: 0;
  border-radius: 999px;
  padding: 1px 6px;
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
  font-family: var(--sans);
  font-size: 9px;
  font-weight: 650;
}

.tab-toolbar {
  align-items: center;
  gap: 16px;
  margin-bottom: 13px;
  border-bottom: 0;
  padding-bottom: 0;
}

.tab-toolbar h2 {
  margin-bottom: 3px;
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.tab-toolbar p {
  color: var(--admin-ink-3);
  font-size: 9px;
  line-height: 1.6;
}

.tab-filters {
  flex: none;
  gap: 7px;
}

.tab-filters .el-input {
  width: 220px;
}

.registration-table {
  --el-table-border-color: var(--admin-border);
  --el-table-header-bg-color: #f5f7fa;
  --el-table-row-hover-bg-color: #f7f9fc;
}

.student-cell {
  gap: 9px;
}

.student-avatar {
  width: 31px;
  height: 31px;
  border: 0;
  border-radius: 50%;
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
  font-family: var(--sans);
  font-size: 11px;
  font-weight: 650;
}

.student-cell div {
  gap: 2px;
}

.student-cell strong {
  color: var(--admin-ink);
  font-size: 11px;
  font-weight: 600;
}

.student-cell small,
.table-muted {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 9px;
}

.checked-text {
  color: var(--admin-up);
  font-size: 10px;
  font-weight: 600;
}

.check-in-workspace {
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.75fr);
  gap: 14px;
  padding: 2px 0 0;
}

.check-in-copy {
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  padding: 24px;
  background: var(--admin-panel-strong);
}

.check-in-copy h2 {
  margin-bottom: 11px;
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 24px;
  font-weight: 720;
  letter-spacing: -0.025em;
  line-height: 1.3;
}

.check-in-copy > p:not(.eyebrow) {
  max-width: 650px;
  margin-bottom: 18px;
  color: var(--admin-ink-2);
  font-size: 11px;
  line-height: 1.75;
}

.logic-list {
  gap: 7px;
  margin-bottom: 18px;
}

.logic-list div {
  align-items: flex-start;
  gap: 10px;
  border: 0;
  border-radius: 0;
  padding: 8px 10px;
  background: #f6f8fb;
}

.logic-list strong {
  display: grid;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 6px;
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
  font-family: var(--sans);
  font-size: 9px;
  font-weight: 650;
}

.logic-list span {
  padding-top: 3px;
  color: var(--admin-ink-2);
  font-size: 10px;
  line-height: 1.6;
}

.qr-panel {
  align-items: stretch;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  padding: 20px;
  background:
    radial-gradient(circle at 50% 0, rgba(45, 91, 216, 0.08), transparent 14rem),
    #f7f9fc;
  box-shadow: none;
  transform: none;
}

.qr-frame {
  width: min(100%, 266px);
  align-self: center;
  border: 1px dashed var(--admin-border-strong);
  border-radius: 10px;
  padding: 10px;
  background: #fff;
}

.qr-frame.ready {
  border-style: solid;
  border-color: var(--admin-border);
}

.qr-frame img {
  border-radius: 5px;
}

.qr-placeholder {
  gap: 5px;
}

.qr-placeholder .el-icon {
  color: #9baccc;
  font-size: 42px;
}

.qr-placeholder strong {
  color: var(--admin-ink-2);
  font-family: var(--sans);
  font-size: 13px;
  font-weight: 650;
}

.qr-placeholder span {
  color: var(--admin-ink-3);
  font-size: 9px;
}

.qr-status {
  width: min(100%, 266px);
  align-self: center;
  margin-top: 12px;
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  color: var(--admin-ink-2);
  background: #fff;
  font-size: 10px;
}

.qr-expiry,
.qr-hint {
  margin: 8px auto 11px;
  color: var(--admin-ink-3);
  font-size: 9px;
  line-height: 1.55;
}

.qr-panel > .el-button {
  align-self: center;
  min-width: 150px;
}

.check-in-closed {
  min-height: 300px;
  border: 1px dashed var(--admin-border-strong);
  border-radius: 12px;
  background: #f8fafc;
}

.check-in-closed .el-icon {
  color: var(--admin-amber);
  font-size: 34px;
}

.check-in-closed h3 {
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 17px;
  font-weight: 650;
}

.check-in-closed p {
  color: var(--admin-ink-3);
  font-size: 10px;
}

.lan-alert {
  margin-top: 14px;
  border-radius: 9px;
}

.lang-alert :deep(.el-alert) {
  border-radius: 9px;
}

.mobile-registration-list {
  display: none;
}

@media (max-width: 980px) {
  .admin-detail-hero,
  .check-in-workspace {
    grid-template-columns: 1fr;
  }

  .operation-actions {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .qr-panel {
    width: 100%;
  }
}

@media (max-width: 720px) {
  .tab-toolbar,
  .tab-filters {
    align-items: stretch;
    flex-direction: column;
  }

  .tab-filters {
    width: 100%;
  }

  .tab-filters .el-input,
  .tab-filters .el-select {
    width: 100% !important;
  }

  .detail-tabs {
    padding: 0 14px 16px;
  }

  .detail-tabs :deep(.el-tabs__item:first-child) {
    padding-left: 14px;
  }

  .registration-table {
    display: none;
  }

  .mobile-registration-list {
    display: grid;
    gap: 9px;
  }

  .mobile-registration-list > article {
    border: 1px solid var(--admin-border);
    border-radius: 10px;
    padding: 13px;
    background: var(--admin-panel-strong);
    box-shadow: none;
  }

  .mobile-student-head {
    margin-bottom: 10px;
  }

  .mobile-registration-meta {
    gap: 6px;
    margin-bottom: 10px;
    border-top: 1px solid var(--admin-border);
    padding-top: 9px;
  }

  .mobile-registration-meta span {
    color: var(--admin-ink-3);
    font-family: var(--sans);
    font-size: 9px;
  }

  .mobile-registration-meta strong {
    color: var(--admin-ink-2);
    font-family: var(--sans);
    font-size: 9px;
    font-weight: 600;
  }

  .mobile-promotion {
    border: 1px solid rgba(183, 122, 22, 0.2);
    border-radius: 7px;
    background: var(--admin-amber-soft);
    font-size: 9px;
    font-weight: 600;
  }

  .mobile-empty {
    border: 1px dashed var(--admin-border-strong);
    border-radius: 9px;
    padding: 30px 12px;
    color: var(--admin-ink-3);
    background: #f8fafc;
    font-size: 11px;
  }

  .operation-actions {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .hero-copy {
    padding: 18px;
  }

  .hero-copy h1 {
    font-size: 26px;
  }

  .hero-meta {
    grid-template-columns: 1fr;
  }

  .hero-meta span,
  .hero-meta span:nth-child(even) {
    border-bottom: 1px solid var(--admin-border);
    border-left: 0;
    padding: 9px 0;
  }

  .hero-meta span:last-child {
    border-bottom: 0;
    padding-bottom: 0;
  }

  .operation-panel {
    padding: 17px;
  }

  .detail-stats {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
  }

  .detail-stats article {
    min-height: 100px;
    padding: 12px;
  }

  .detail-stats strong {
    font-size: 25px;
  }

  .check-in-copy {
    padding: 18px;
  }

  .check-in-copy h2 {
    font-size: 21px;
  }
}
</style>
