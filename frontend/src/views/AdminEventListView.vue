<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DataAnalysis,
  Edit,
  Plus,
  Promotion,
  Refresh,
  Search,
  Tickets,
  VideoPlay,
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import AdminShell from '../components/AdminShell.vue'
import StatusTag from '../components/StatusTag.vue'
import { adminApi } from '../api/admin'
import { formatDateTime } from '../utils/format'

const route = useRoute()
const router = useRouter()

const events = ref([])
const loading = ref(true)
const search = ref('')
const activeStatus = ref('ALL')
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const dialogMode = ref('create')
const editingId = ref(null)
const saving = ref(false)
const statusBusyId = ref(null)
const formRef = ref()

function initialForm() {
  const base = dayjs().second(0)
  return {
    title: '',
    description: '',
    location: '',
    category: '',
    organizer: '',
    registrationStart: base.format('YYYY-MM-DDTHH:mm:ss'),
    registrationEnd: base.add(5, 'day').hour(23).minute(59).format('YYYY-MM-DDTHH:mm:ss'),
    startTime: base.add(7, 'day').hour(19).minute(0).format('YYYY-MM-DDTHH:mm:ss'),
    endTime: base.add(7, 'day').hour(21).minute(0).format('YYYY-MM-DDTHH:mm:ss'),
    capacity: 50,
  }
}

const form = reactive(initialForm())

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  category: [{ required: true, message: '请输入活动分类', trigger: 'blur' }],
  organizer: [{ required: true, message: '请输入主办方', trigger: 'blur' }],
  registrationStart: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEnd: [{ required: true, message: '请选择报名结束时间', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择活动开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择活动结束时间', trigger: 'change' }],
  capacity: [{ required: true, message: '请设置活动名额', trigger: 'change' }],
}

const statusFilters = [
  { key: 'ALL', label: '全部' },
  { key: 'DRAFT', label: '草稿' },
  { key: 'PUBLISHED', label: '报名中' },
  { key: 'FINISHED', label: '已结束' },
  { key: 'CANCELED', label: '已取消' },
]

const stats = computed(() => ({
  total: events.value.length,
  published: events.value.filter((item) => item.status === 'PUBLISHED').length,
  drafts: events.value.filter((item) => item.status === 'DRAFT').length,
  registrations: events.value.reduce(
    (sum, item) =>
      sum + item.confirmedCount + item.checkedInCount + item.promotedCount + item.waitlistedCount,
    0,
  ),
}))

const filteredEvents = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return events.value.filter((event) => {
    const matchesStatus = activeStatus.value === 'ALL' || event.status === activeStatus.value
    const matchesKeyword =
      !keyword || [event.title, `#${event.id}`].join(' ').toLowerCase().includes(keyword)
    return matchesStatus && matchesKeyword
  })
})

async function load() {
  loading.value = true
  try {
    events.value = await adminApi.events()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, initialForm())
}

function clearValidationSoon() {
  nextTick(() => formRef.value?.clearValidate())
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  resetForm()
  dialogVisible.value = true
  clearValidationSoon()
}

async function openEdit(row) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  resetForm()
  dialogVisible.value = true
  dialogLoading.value = true

  try {
    const detail = await adminApi.detail(row.id)
    Object.assign(form, {
      title: detail.title,
      description: detail.description || '',
      location: detail.location,
      category: detail.category,
      organizer: detail.organizer,
      registrationStart: detail.registrationStart,
      registrationEnd: detail.registrationEnd,
      startTime: detail.startTime,
      endTime: detail.endTime,
      capacity: detail.capacity,
    })
    clearValidationSoon()
  } catch (error) {
    dialogVisible.value = false
    ElMessage.error(error.message)
  } finally {
    dialogLoading.value = false
  }
}

function validateTimeline() {
  const registrationStart = dayjs(form.registrationStart)
  const registrationEnd = dayjs(form.registrationEnd)
  const startTime = dayjs(form.startTime)
  const endTime = dayjs(form.endTime)

  if (!registrationStart.isBefore(registrationEnd)) {
    ElMessage.warning('报名开始时间必须早于报名结束时间')
    return false
  }
  if (registrationEnd.isAfter(startTime)) {
    ElMessage.warning('报名结束时间不能晚于活动开始时间')
    return false
  }
  if (!startTime.isBefore(endTime)) {
    ElMessage.warning('活动开始时间必须早于结束时间')
    return false
  }
  return true
}

async function submitForm() {
  if (dialogLoading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || !validateTimeline()) return

  saving.value = true
  try {
    const payload = {
      ...form,
      capacity: Number(form.capacity),
    }
    if (dialogMode.value === 'create') {
      await adminApi.createEvent(payload)
      ElMessage.success('活动草稿已创建')
    } else {
      await adminApi.updateEvent(editingId.value, payload)
      ElMessage.success('活动草稿已更新')
    }
    dialogVisible.value = false
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function publishEvent(row) {
  try {
    await ElMessageBox.confirm(
      `发布「${row.title}」后，学生将可以浏览并报名。确定发布吗？`,
      '发布活动',
      { type: 'info', confirmButtonText: '确定发布', cancelButtonText: '再检查一下' },
    )
  } catch {
    return
  }

  statusBusyId.value = row.id
  try {
    await adminApi.publish(row.id)
    ElMessage.success('活动已发布')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    statusBusyId.value = null
  }
}

async function finishEvent(row) {
  try {
    await ElMessageBox.confirm(
      `结束后「${row.title}」将停止签到，活动记录会保留。确定结束吗？`,
      '结束活动',
      { type: 'warning', confirmButtonText: '确定结束', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  statusBusyId.value = row.id
  try {
    await adminApi.finish(row.id)
    ElMessage.success('活动已结束')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    statusBusyId.value = null
  }
}

async function cancelEvent(row) {
  try {
    await ElMessageBox.confirm(
      `取消「${row.title}」后，所有有效报名会同步取消，且无法恢复。确定取消吗？`,
      '取消活动',
      {
        type: 'error',
        confirmButtonText: '确定取消活动',
        cancelButtonText: '返回',
      },
    )
  } catch {
    return
  }

  statusBusyId.value = row.id
  try {
    await adminApi.cancel(row.id)
    ElMessage.success('活动已取消')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    statusBusyId.value = null
  }
}

function openDetail(row) {
  router.push(`/admin/events/${row.id}`)
}

onMounted(async () => {
  await load()
  const editId = Number(Array.isArray(route.query.edit) ? route.query.edit[0] : route.query.edit)
  if (editId) {
    const target = events.value.find((item) => item.id === editId)
    if (target?.status === 'DRAFT') await openEdit(target)
  }
})
</script>

<template>
  <AdminShell>
    <div class="page-shell admin-list-page">
      <header class="admin-header">
        <div>
          <p class="eyebrow">EVENT OPERATIONS</p>
          <h1 class="page-title">活动管理</h1>
          <p class="page-subtitle">
            统一管理活动草稿、发布状态、报名名额与现场签到。活动发布后核心信息将锁定，确保学生看到的信息始终一致。
          </p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建活动</el-button>
      </header>

      <section class="admin-stats">
        <article>
          <el-icon><DataAnalysis /></el-icon>
          <div>
            <span>活动总数</span>
            <strong>{{ stats.total }}</strong>
          </div>
        </article>
        <article>
          <el-icon><VideoPlay /></el-icon>
          <div>
            <span>报名中</span>
            <strong>{{ stats.published }}</strong>
          </div>
        </article>
        <article>
          <el-icon><Edit /></el-icon>
          <div>
            <span>待完善草稿</span>
            <strong>{{ stats.drafts }}</strong>
          </div>
        </article>
        <article class="highlight-stat">
          <el-icon><Tickets /></el-icon>
          <div>
            <span>累计报名</span>
            <strong>{{ stats.registrations }}</strong>
          </div>
        </article>
      </section>

      <section class="event-table-section surface-card">
        <div class="table-toolbar">
          <div>
            <p class="eyebrow">EVENT DIRECTORY</p>
            <h2>活动列表</h2>
          </div>
          <div class="toolbar-actions">
            <el-input
              v-model="search"
              :prefix-icon="Search"
              placeholder="搜索活动名称或编号"
              clearable
            />
            <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
          </div>
        </div>

        <div class="filter-pills table-filters">
          <button
            v-for="filter in statusFilters"
            :key="filter.key"
            type="button"
            class="filter-pill"
            :class="{ active: activeStatus === filter.key }"
            @click="activeStatus = filter.key"
          >
            {{ filter.label }}
          </button>
        </div>

        <el-table
          v-loading="loading"
          :data="filteredEvents"
          row-key="id"
          empty-text="暂无符合条件的活动"
          class="event-table"
        >
          <el-table-column label="活动" min-width="235">
            <template #default="{ row }">
              <button type="button" class="event-title-cell" @click="openDetail(row)">
                <strong>{{ row.title }}</strong>
                <small>活动 #{{ row.id }}</small>
              </button>
            </template>
          </el-table-column>

          <el-table-column label="状态" width="105">
            <template #default="{ row }">
              <StatusTag :status="row.status" size="small" mode="admin" />
            </template>
          </el-table-column>

          <el-table-column label="活动时间" min-width="190">
            <template #default="{ row }">
              <span class="table-text">{{ formatDateTime(row.startTime) }}</span>
              <small class="table-subtext">报名截止 {{ formatDateTime(row.registrationEnd) }}</small>
            </template>
          </el-table-column>

          <el-table-column label="名额" width="155">
            <template #default="{ row }">
              <div class="capacity-cell">
                <span>{{ row.occupiedCount }} / {{ row.capacity }}</span>
                <el-progress
                  :percentage="Math.min(100, Math.round((row.occupiedCount / row.capacity) * 100))"
                  :show-text="false"
                  :stroke-width="5"
                  :color="row.remainingSeats > 0 ? 'var(--cobalt)' : 'var(--tomato)'"
                />
              </div>
            </template>
          </el-table-column>

          <el-table-column label="报名 / 候补" width="130">
            <template #default="{ row }">
              <div class="count-cell">
                <strong>{{ row.confirmedCount + row.checkedInCount + row.promotedCount }}</strong>
                <span>+ {{ row.waitlistedCount }} 候补</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="245">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="primary"
                  :icon="Edit"
                  @click="openEdit(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="row.status === 'DRAFT'"
                  link
                  type="success"
                  :icon="Promotion"
                  :loading="statusBusyId === row.id"
                  @click="publishEvent(row)"
                >
                  发布
                </el-button>
                <el-button link :icon="DataAnalysis" @click="openDetail(row)">查看</el-button>
                <el-button
                  v-if="row.status === 'PUBLISHED'"
                  link
                  type="warning"
                  :loading="statusBusyId === row.id"
                  @click="finishEvent(row)"
                >
                  结束
                </el-button>
                <el-button
                  v-if="row.status === 'PUBLISHED'"
                  link
                  type="danger"
                  :loading="statusBusyId === row.id"
                  @click="cancelEvent(row)"
                >
                  取消
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="mobile-event-list">
          <article v-for="row in filteredEvents" :key="`mobile-${row.id}`">
            <div class="mobile-event-head">
              <StatusTag :status="row.status" size="small" mode="admin" />
              <span>活动 #{{ row.id }}</span>
            </div>
            <button type="button" class="mobile-event-title" @click="openDetail(row)">
              {{ row.title }}
            </button>
            <div class="mobile-event-meta">
              <span>{{ formatDateTime(row.startTime) }}</span>
              <span>报名截止 {{ formatDateTime(row.registrationEnd) }}</span>
            </div>
            <div class="mobile-capacity">
              <div>
                <span>名额</span>
                <strong>{{ row.occupiedCount }} / {{ row.capacity }}</strong>
              </div>
              <div class="mobile-capacity-track">
                <span
                  :style="{
                    width: `${Math.min(100, Math.round((row.occupiedCount / row.capacity) * 100))}%`,
                  }"
                ></span>
              </div>
              <small>{{ row.confirmedCount + row.checkedInCount + row.promotedCount }} 人确认 · {{ row.waitlistedCount }} 人候补</small>
            </div>
            <div class="mobile-event-actions">
              <el-button
                v-if="row.status === 'DRAFT'"
                type="primary"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="row.status === 'DRAFT'"
                type="success"
                :icon="Promotion"
                :loading="statusBusyId === row.id"
                @click="publishEvent(row)"
              >
                发布
              </el-button>
              <el-button :icon="DataAnalysis" @click="openDetail(row)">查看</el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                type="warning"
                :loading="statusBusyId === row.id"
                @click="finishEvent(row)"
              >
                结束
              </el-button>
              <el-button
                v-if="row.status === 'PUBLISHED'"
                type="danger"
                plain
                :loading="statusBusyId === row.id"
                @click="cancelEvent(row)"
              >
                取消
              </el-button>
            </div>
          </article>
          <div v-if="!filteredEvents.length" class="mobile-empty">暂无符合条件的活动</div>
        </div>

        <div class="table-note">
          发布前请检查报名窗口、活动时间与名额设置。已发布活动可结束或取消，避免已报名学生看到的信息被改写。
        </div>
      </section>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建活动草稿' : '编辑活动草稿'"
      width="min(760px, 94vw)"
      destroy-on-close
      class="event-dialog admin-dialog"
    >
      <div v-loading="dialogLoading">
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <div class="form-grid">
            <el-form-item label="活动标题" prop="title" class="span-2">
              <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：秋日校园音乐节" />
            </el-form-item>

            <el-form-item label="活动分类" prop="category">
              <el-input v-model="form.category" maxlength="30" placeholder="文艺 / 技术 / 体育" />
            </el-form-item>

            <el-form-item label="主办方" prop="organizer">
              <el-input v-model="form.organizer" maxlength="100" placeholder="校学生会" />
            </el-form-item>

            <el-form-item label="活动地点" prop="location" class="span-2">
              <el-input v-model="form.location" maxlength="100" placeholder="东区草坪" />
            </el-form-item>

            <el-form-item label="报名开始" prop="registrationStart">
              <el-date-picker
                v-model="form.registrationStart"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择报名开始时间"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="报名截止" prop="registrationEnd">
              <el-date-picker
                v-model="form.registrationEnd"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择报名截止时间"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="活动开始" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择活动开始时间"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="活动结束" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择活动结束时间"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="活动名额" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :max="10000" controls-position="right" style="width: 100%" />
            </el-form-item>

            <el-form-item label="活动描述" prop="description" class="span-2">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                maxlength="2000"
                show-word-limit
                placeholder="说明活动内容、参与方式和注意事项"
              />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="dialogLoading" @click="submitForm">
          {{ dialogMode === 'create' ? '保存草稿' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </AdminShell>
</template>

<style scoped>
.admin-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 26px;
}

.admin-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 28px;
}

.admin-stats article {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 112px;
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 18px;
  background: rgba(255, 253, 248, 0.78);
  box-shadow: var(--shadow-sm);
}

.admin-stats article > .el-icon {
  display: grid;
  width: 42px;
  height: 42px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 13px;
  color: var(--green-deep);
  background: var(--green-soft);
  font-size: 19px;
}

.admin-stats article div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.admin-stats span {
  color: var(--ink-faint);
  font-size: 11px;
}

.admin-stats strong {
  font-family: var(--serif);
  font-size: 31px;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.admin-stats .highlight-stat {
  border-color: transparent;
  color: #fff;
  background:
    radial-gradient(circle at 90% 0%, rgba(217, 154, 50, 0.35), transparent 8rem),
    var(--green-deep);
}

.admin-stats .highlight-stat span {
  color: rgba(255, 255, 255, 0.58);
}

.admin-stats .highlight-stat > .el-icon {
  color: #e5bc6b;
  background: rgba(255, 255, 255, 0.1);
}

.event-table-section {
  overflow: hidden;
  border-radius: 24px;
}

.table-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 24px 14px;
}

.table-toolbar h2 {
  margin-bottom: 0;
  font-family: var(--serif);
  font-size: 30px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.toolbar-actions .el-input {
  width: 240px;
}

.table-filters {
  padding: 0 24px 16px;
}

.event-table {
  width: 100%;
  --el-table-border-color: var(--line);
  --el-table-header-bg-color: var(--surface-soft);
  --el-table-row-hover-bg-color: rgba(220, 234, 223, 0.35);
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
}

.event-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 100%;
  border: 0;
  padding: 0;
  text-align: left;
  color: var(--ink);
  background: transparent;
  cursor: pointer;
}

.event-title-cell strong {
  overflow: hidden;
  font-family: var(--serif);
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-title-cell small,
.table-subtext {
  display: block;
  margin-top: 4px;
  color: var(--ink-faint);
  font-size: 10px;
}

.event-title-cell:hover strong {
  color: var(--green);
}

.table-text {
  font-size: 12px;
}

.capacity-cell {
  display: grid;
  gap: 6px;
  width: 105px;
}

.capacity-cell span {
  color: var(--ink-soft);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.count-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.count-cell strong {
  font-size: 15px;
}

.count-cell span {
  color: var(--ink-faint);
  font-size: 10px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 5px;
}

.table-note {
  padding: 14px 24px 18px;
  color: var(--ink-faint);
  font-size: 11px;
  line-height: 1.6;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.span-2 {
  grid-column: 1 / -1;
}

@media (max-width: 920px) {
  .admin-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions .el-input {
    width: min(100%, 320px);
  }
}

@media (max-width: 640px) {
  .admin-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-stats {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: auto;
  }
}
</style>

<style scoped>
/* Final page-scoped overrides: keep the admin visual isolated from the campus theme. */
.admin-list-page .admin-header {
  border-bottom: 1px solid var(--admin-border);
  padding-bottom: 22px;
}

.admin-list-page .admin-stats article {
  min-height: 96px;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  padding: 16px;
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
  transform: none;
}

.admin-list-page .admin-stats article:nth-child(2),
.admin-list-page .admin-stats article:nth-child(3) {
  background: var(--admin-panel);
  transform: none;
}

.admin-list-page .admin-stats article > .el-icon {
  border: 0;
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
}

.admin-list-page .admin-stats span {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 550;
  letter-spacing: 0;
}

.admin-list-page .admin-stats strong {
  font-family: var(--sans);
  font-size: 29px;
  font-weight: 720;
  letter-spacing: -0.035em;
}

.admin-list-page .admin-stats .highlight-stat {
  border-color: #20304a;
  color: #fff;
  background:
    linear-gradient(120deg, rgba(45, 91, 216, 0.3), transparent 58%),
    #152238;
  box-shadow: 0 10px 26px rgba(17, 28, 47, 0.12);
  transform: none;
}

.admin-list-page .admin-stats .highlight-stat span {
  color: #aebbd0;
}

.admin-list-page .admin-stats .highlight-stat > .el-icon {
  color: #d7e1ff;
  background: rgba(255, 255, 255, 0.1);
}

.admin-list-page .event-table-section {
  border: 1px solid var(--admin-border);
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
}

.admin-list-page .table-toolbar h2 {
  font-family: var(--sans);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.015em;
}

.admin-list-page .table-toolbar {
  align-items: center;
  border-bottom: 0;
}

.admin-list-page .table-toolbar .eyebrow {
  color: var(--admin-ink-3);
}

.admin-list-page .event-title-cell strong {
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 12px;
  font-weight: 650;
}

.admin-list-page .event-title-cell:hover strong {
  color: var(--admin-accent);
}

.admin-list-page .event-title-cell small,
.admin-list-page .table-subtext {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
}

.admin-list-page .table-text {
  color: var(--admin-ink-2);
  font-size: 11px;
  font-weight: 500;
}

.admin-list-page .capacity-cell span,
.admin-list-page .count-cell span {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
}

.admin-list-page .count-cell strong {
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 13px;
  font-weight: 650;
}

.admin-list-page .table-note {
  border-top: 1px solid var(--admin-border);
  color: var(--admin-ink-3);
  background: #f8fafc;
  font-size: 10px;
}

@media (max-width: 820px) {
  .admin-list-page .mobile-event-list {
    display: grid;
  }

  .admin-list-page .mobile-event-list > article {
    border: 1px solid var(--admin-border);
    border-radius: 10px;
    background: var(--admin-panel-strong);
    box-shadow: none;
  }

  .admin-list-page .mobile-event-title {
    font-family: var(--sans);
    font-size: 15px;
    font-weight: 650;
    letter-spacing: -0.01em;
  }

  .admin-list-page .mobile-capacity-track {
    border: 0;
    background: #e8edf4;
  }

  .admin-list-page .mobile-capacity-track span {
    background: var(--admin-accent);
  }

  .admin-list-page .mobile-capacity {
    border-top: 1px solid var(--admin-border);
  }

  .admin-list-page .mobile-event-meta span:last-child,
  .admin-list-page .mobile-capacity strong,
  .admin-list-page .mobile-event-head > span {
    font-family: var(--sans);
  }

  .admin-list-page .table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-list-page .toolbar-actions {
    width: 100%;
  }
}

@media (max-width: 640px) {
  .admin-list-page .admin-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-list-page .admin-stats article {
    min-height: 82px;
    padding: 12px;
  }

  .admin-list-page .admin-stats article > .el-icon {
    display: none;
  }

  .admin-list-page .admin-stats strong {
    font-size: 25px;
  }

  .admin-list-page .table-filters {
    flex-wrap: nowrap;
  }
}
</style>

<style scoped>
.admin-header {
  align-items: center;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--admin-border);
  padding: 2px 0 22px;
}

.admin-header > div {
  min-width: 0;
}

.admin-header .page-title {
  color: var(--admin-ink);
}

.admin-header .page-subtitle {
  max-width: 760px;
  margin-bottom: 0;
}

.admin-header > .el-button {
  min-width: 104px;
  flex: none;
}

.admin-stats {
  gap: 12px;
  margin-bottom: 18px;
}

.admin-stats article {
  position: relative;
  min-height: 96px;
  align-items: flex-start;
  gap: 12px;
  overflow: hidden;
  border: 1px solid var(--admin-border);
  border-radius: 12px;
  padding: 16px;
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
  transform: none;
}

.admin-stats article:nth-child(2),
.admin-stats article:nth-child(3) {
  background: var(--admin-panel);
  transform: none;
}

.admin-stats article::after {
  position: absolute;
  top: 0;
  right: 0;
  width: 72px;
  height: 72px;
  border-radius: 0 0 0 72px;
  background: linear-gradient(135deg, transparent, rgba(45, 91, 216, 0.035));
  content: "";
}

.admin-stats article > .el-icon {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 9px;
  color: var(--admin-accent);
  background: var(--admin-accent-soft);
  font-size: 16px;
}

.admin-stats article div {
  gap: 8px;
}

.admin-stats span {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
  font-weight: 550;
  letter-spacing: 0;
}

.admin-stats strong {
  font-family: var(--sans);
  font-size: 29px;
  font-weight: 720;
  letter-spacing: -0.035em;
  line-height: 1;
}

.admin-stats .highlight-stat {
  border-color: #20304a;
  color: #fff;
  background:
    linear-gradient(120deg, rgba(45, 91, 216, 0.3), transparent 58%),
    #152238;
  box-shadow: 0 10px 26px rgba(17, 28, 47, 0.12);
  transform: none;
}

.admin-stats .highlight-stat::after {
  background: linear-gradient(135deg, transparent, rgba(255, 255, 255, 0.08));
}

.admin-stats .highlight-stat span {
  color: #aebbd0;
}

.admin-stats .highlight-stat > .el-icon {
  color: #d7e1ff;
  background: rgba(255, 255, 255, 0.1);
}

.event-table-section {
  overflow: hidden;
  border: 1px solid var(--admin-border);
  border-radius: var(--admin-radius);
  background: var(--admin-panel);
  box-shadow: var(--admin-shadow);
}

.table-toolbar {
  align-items: center;
  gap: 16px;
  padding: 19px 20px 13px;
}

.table-toolbar .eyebrow {
  margin-bottom: 5px;
}

.table-toolbar h2 {
  font-family: var(--sans);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.015em;
}

.toolbar-actions {
  gap: 8px;
}

.toolbar-actions .el-input {
  width: 250px;
}

.table-filters {
  width: max-content;
  margin: 0 20px 14px;
  padding: 0;
}

.table-filters.filter-pills {
  border: 1px solid var(--admin-border);
  border-radius: 8px;
  padding: 3px;
}

.event-table {
  --el-table-border-color: var(--admin-border);
  --el-table-header-bg-color: #f5f7fa;
  --el-table-row-hover-bg-color: #f7f9fc;
}

.event-title-cell {
  gap: 3px;
}

.event-title-cell strong {
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 12px;
  font-weight: 650;
}

.event-title-cell small,
.table-subtext {
  margin-top: 2px;
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
}

.event-title-cell:hover strong {
  color: var(--admin-accent);
}

.table-text {
  color: var(--admin-ink-2);
  font-size: 11px;
  font-weight: 500;
}

.capacity-cell {
  width: 104px;
  gap: 5px;
}

.capacity-cell span {
  color: var(--admin-ink-2);
  font-size: 10px;
  font-weight: 600;
}

.count-cell {
  gap: 3px;
}

.count-cell strong {
  color: var(--admin-ink);
  font-family: var(--sans);
  font-size: 13px;
  font-weight: 650;
}

.count-cell span {
  color: var(--admin-ink-3);
  font-family: var(--sans);
  font-size: 10px;
}

.row-actions {
  gap: 0 3px;
}

.row-actions .el-button {
  padding-right: 3px;
  padding-left: 3px;
  font-size: 11px;
}

.table-note {
  border-top: 1px solid var(--admin-border);
  padding: 11px 20px 12px;
  color: var(--admin-ink-3);
  background: #f8fafc;
  font-size: 10px;
  line-height: 1.6;
}

.mobile-event-list {
  display: none;
}

@media (max-width: 920px) {
  .admin-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .table-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions .el-input {
    width: min(100%, 340px);
  }
}

@media (max-width: 820px) {
  .event-table {
    display: none;
  }

  .table-note {
    display: none;
  }

  .mobile-event-list {
    display: grid;
    gap: 10px;
    padding: 0 14px 16px;
  }

  .mobile-event-list > article {
    border: 1px solid var(--admin-border);
    border-radius: 10px;
    padding: 14px;
    background: var(--admin-panel-strong);
    box-shadow: none;
  }

  .mobile-event-head {
    margin-bottom: 10px;
  }

  .mobile-event-head > span {
    color: var(--admin-ink-3);
    font-family: var(--sans);
    font-size: 9px;
    font-weight: 500;
  }

  .mobile-event-title {
    margin-bottom: 8px;
    color: var(--admin-ink);
    font-family: var(--sans);
    font-size: 15px;
    font-weight: 650;
    letter-spacing: -0.01em;
    line-height: 1.4;
  }

  .mobile-event-meta {
    gap: 3px;
    margin-bottom: 12px;
    color: var(--admin-ink-2);
    font-size: 10px;
  }

  .mobile-event-meta span:last-child {
    color: var(--admin-ink-3);
    font-family: var(--sans);
    font-size: 9px;
  }

  .mobile-capacity {
    margin-bottom: 12px;
    border-top: 1px solid var(--admin-border);
    padding-top: 10px;
  }

  .mobile-capacity > div:first-child {
    color: var(--admin-ink-3);
    font-size: 9px;
  }

  .mobile-capacity strong {
    color: var(--admin-ink);
    font-family: var(--sans);
    font-size: 10px;
    font-weight: 650;
  }

  .mobile-capacity-track {
    height: 6px;
    border: 0;
    border-radius: 999px;
    background: #e8edf4;
  }

  .mobile-capacity-track span {
    background: var(--admin-accent);
  }

  .mobile-capacity small {
    margin-top: 5px;
    color: var(--admin-ink-3);
    font-size: 9px;
  }

  .mobile-event-actions {
    gap: 6px;
  }

  .mobile-event-actions .el-button {
    min-height: 32px;
    border-radius: 6px;
    font-size: 11px;
  }

  .mobile-empty {
    border: 1px dashed var(--admin-border-strong);
    border-radius: 9px;
    padding: 30px 12px;
    color: var(--admin-ink-3);
    background: #f8fafc;
    font-size: 11px;
  }
}

@media (max-width: 640px) {
  .admin-header {
    align-items: flex-start;
    gap: 15px;
    padding-bottom: 18px;
  }

  .admin-header > .el-button {
    width: 100%;
  }

  .admin-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
  }

  .admin-stats article {
    min-height: 82px;
    padding: 12px;
  }

  .admin-stats article > .el-icon {
    display: none;
  }

  .admin-stats span {
    font-size: 9px;
  }

  .admin-stats strong {
    font-size: 25px;
  }

  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions .el-input {
    width: 100%;
  }

  .table-toolbar {
    padding: 16px 14px 11px;
  }

  .table-filters {
    overflow-x: auto;
    max-width: calc(100% - 28px);
    margin: 0 14px 12px;
    scrollbar-width: none;
  }

  .table-filters::-webkit-scrollbar {
    display: none;
  }
}
</style>

<style scoped>
.admin-header {
  align-items: flex-end;
  margin-bottom: 24px;
  border-bottom: 2px solid var(--ink);
  padding-bottom: 18px;
}

.admin-stats {
  gap: 12px;
  margin-bottom: 24px;
}

.admin-stats article {
  min-height: 104px;
  border: 1.5px solid var(--ink);
  border-radius: 11px;
  padding: 15px;
  background: var(--surface);
  box-shadow: 3px 3px 0 var(--ink);
}

.admin-stats article:nth-child(2) {
  background: var(--cobalt-soft);
  transform: rotate(0.7deg);
}

.admin-stats article:nth-child(3) {
  background: var(--butter-soft);
  transform: rotate(-0.8deg);
}

.admin-stats article > .el-icon {
  width: 38px;
  height: 38px;
  border: 1.5px solid var(--ink);
  border-radius: 9px;
  color: var(--ink);
  background: var(--surface);
  font-size: 17px;
}

.admin-stats span {
  color: var(--ink-2);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.admin-stats strong {
  font-family: var(--display);
  font-size: 38px;
  font-weight: 900;
  letter-spacing: -0.07em;
}

.admin-stats .highlight-stat {
  border-color: var(--ink);
  color: var(--surface);
  background: var(--cobalt);
  box-shadow: 3px 3px 0 var(--ink);
  transform: rotate(1deg);
}

.admin-stats .highlight-stat span {
  color: color-mix(in srgb, var(--surface) 72%, transparent);
}

.admin-stats .highlight-stat > .el-icon {
  color: var(--ink);
  background: var(--butter);
}

.event-table-section {
  border: 1.5px solid var(--ink);
  border-radius: 14px;
  background: var(--surface);
  box-shadow: 4px 4px 0 var(--ink);
}

.table-toolbar {
  align-items: center;
  border-bottom: 1.5px dashed var(--line);
  padding: 20px 20px 14px;
}

.table-toolbar h2 {
  font-family: var(--display);
  font-size: 32px;
  font-weight: 900;
  letter-spacing: -0.045em;
}

.table-toolbar .eyebrow {
  margin-bottom: 7px;
  color: var(--tomato);
}

.table-filters {
  padding: 13px 20px 15px;
}

.event-table {
  --el-table-header-bg-color: var(--surface-2);
  --el-table-row-hover-bg-color: var(--butter-soft);
}

.event-title-cell strong {
  font-family: var(--sans);
  font-size: 14px;
  font-weight: 850;
}

.event-title-cell:hover strong {
  color: var(--cobalt);
}

.event-title-cell small,
.table-subtext {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 8px;
}

.table-text {
  font-size: 11px;
  font-weight: 650;
}

.capacity-cell span,
.count-cell span {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
}

.count-cell strong {
  font-family: var(--display);
  font-size: 18px;
  font-weight: 900;
}

.row-actions {
  gap: 2px 3px;
}

.table-note {
  border-top: 1.5px dashed var(--line);
  padding: 13px 20px 16px;
  color: var(--ink-3);
  background: var(--surface-2);
  font-size: 10px;
  line-height: 1.65;
}

.form-grid {
  gap: 0 17px;
}

.mobile-event-list {
  display: none;
}

@media (max-width: 920px) {
  .admin-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-stats article:nth-child(n) {
    transform: none;
  }
}

@media (max-width: 820px) {
  .event-table {
    display: none;
  }

  .table-note {
    display: none;
  }

  .mobile-event-list {
    display: grid;
    gap: 10px;
    padding: 0 14px 16px;
  }

  .mobile-event-list > article {
    border: 1.5px solid var(--ink);
    border-radius: 10px;
    padding: 13px;
    background: var(--surface);
    box-shadow: 2px 2px 0 var(--ink);
  }

  .mobile-event-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 9px;
  }

  .mobile-event-head > span {
    color: var(--ink-3);
    font-family: var(--mono);
    font-size: 8px;
    font-weight: 800;
  }

  .mobile-event-title {
    display: block;
    width: 100%;
    margin-bottom: 9px;
    border: 0;
    padding: 0;
    text-align: left;
    background: transparent;
    font-family: var(--display);
    font-size: 20px;
    font-weight: 900;
    letter-spacing: -0.035em;
    line-height: 1.15;
    cursor: pointer;
  }

  .mobile-event-meta {
    display: grid;
    gap: 4px;
    margin-bottom: 12px;
    color: var(--ink-2);
    font-size: 10px;
  }

  .mobile-event-meta span:last-child {
    color: var(--ink-3);
    font-family: var(--mono);
    font-size: 8px;
  }

  .mobile-capacity {
    margin-bottom: 13px;
    border-top: 1.5px dashed var(--line);
    padding-top: 10px;
  }

  .mobile-capacity > div:first-child {
    display: flex;
    justify-content: space-between;
    margin-bottom: 6px;
    color: var(--ink-3);
    font-size: 9px;
  }

  .mobile-capacity strong {
    color: var(--ink);
    font-family: var(--mono);
    font-size: 10px;
  }

  .mobile-capacity-track {
    overflow: hidden;
    height: 7px;
    border: 1px solid var(--ink);
    border-radius: 999px;
    background: var(--paper-deep);
  }

  .mobile-capacity-track span {
    display: block;
    height: 100%;
    background: var(--cobalt);
  }

  .mobile-capacity small {
    display: block;
    margin-top: 6px;
    color: var(--ink-3);
    font-size: 8px;
  }

  .mobile-event-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 7px;
  }

  .mobile-event-actions :deep(.el-button) {
    margin-left: 0;
  }

  .mobile-empty {
    padding: 34px 12px;
    color: var(--ink-3);
    text-align: center;
    font-size: 12px;
  }
}

@media (max-width: 640px) {
  .admin-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .admin-stats {
    grid-template-columns: 1fr 1fr;
    gap: 9px;
  }

  .admin-stats article {
    min-height: 92px;
    padding: 12px;
  }

  .admin-stats article > .el-icon {
    display: none;
  }

  .admin-stats strong {
    font-size: 33px;
  }

  .toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .toolbar-actions .el-input {
    width: 100%;
  }
}
</style>
