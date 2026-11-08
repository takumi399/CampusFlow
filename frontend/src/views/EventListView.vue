<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import AppShell from '../components/AppShell.vue'
import EventCard from '../components/EventCard.vue'
import EventPoster from '../components/EventPoster.vue'
import { eventApi } from '../api/events'
import { formatDateParts, formatRelativeDay } from '../utils/format'

const events = ref([])
const loading = ref(true)
const search = ref('')
const activeCategory = ref('全部')
const onlyAvailable = ref(false)

const categories = computed(() => {
  const values = [...new Set(events.value.map((event) => event.category))]
  return ['全部', ...values]
})

const filteredEvents = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return events.value.filter((event) => {
    const matchesCategory =
      activeCategory.value === '全部' || event.category === activeCategory.value
    const matchesSearch =
      !keyword ||
      [event.title, event.organizer, event.location, event.category]
        .join(' ')
        .toLowerCase()
        .includes(keyword)
    const matchesAvailability = !onlyAvailable.value || event.remainingSeats > 0
    return matchesCategory && matchesSearch && matchesAvailability
  })
})

const totalRemaining = computed(() =>
  events.value.reduce((sum, event) => sum + event.remainingSeats, 0),
)
const categoryCount = computed(() => new Set(events.value.map((event) => event.category)).size)
const nextEvent = computed(() => events.value[0] || null)
const nextDate = computed(() => formatDateParts(nextEvent.value?.startTime))

async function load() {
  loading.value = true
  try {
    events.value = await eventApi.list()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <AppShell>
    <div class="page-shell events-page">
      <section class="event-hero">
        <div class="hero-copy">
          <p class="eyebrow">CAMPUSFLOW / 今日份新鲜事</p>
          <h1>今天，<br /><span class="headline-highlight">去碰点</span><br />新鲜的。</h1>
          <p class="hero-description">
            校园里正在发生的事都贴在这里。报名、候补、现场签到，
            一次走完，不用在群聊和表格之间来回找。
          </p>

          <div class="hero-metrics" aria-label="活动概览">
            <div>
              <strong>{{ events.length }}</strong>
              <span>场正在发生</span>
            </div>
            <div>
              <strong>{{ totalRemaining }}</strong>
              <span>个剩余座位</span>
            </div>
            <div>
              <strong>{{ categoryCount }}</strong>
              <span>种校园兴趣</span>
            </div>
          </div>
        </div>

        <aside v-if="nextEvent" class="hero-feature">
          <span class="feature-tape">本周先冲</span>
          <EventPoster
            class="feature-poster"
            :category="nextEvent.category"
            :event-id="nextEvent.id"
          />
          <div class="feature-copy">
            <div class="feature-date">
              <span>{{ formatRelativeDay(nextEvent.startTime) }}</span>
              <strong>{{ nextDate.day }}</strong>
              <small>{{ nextDate.month }} / {{ nextDate.weekday }}</small>
            </div>
            <div class="feature-title">
              <h3>{{ nextEvent.title }}</h3>
              <p>{{ nextEvent.location }} · {{ nextEvent.organizer }}</p>
            </div>
            <router-link :to="`/events/${nextEvent.id}`" class="feature-link">
              去占个位 <span aria-hidden="true">↗</span>
            </router-link>
          </div>
        </aside>
      </section>

      <section class="events-section">
        <div class="section-heading board-heading">
          <div>
            <p class="eyebrow">THE NOTICE BOARD</p>
            <h2>校园公告栏</h2>
            <p>按兴趣挑一张，或者直接搜你关心的关键词。</p>
          </div>
          <span class="result-count"><b>{{ filteredEvents.length }}</b> 张海报</span>
        </div>

        <div class="event-toolbar">
          <el-input
            v-model="search"
            class="event-search"
            :prefix-icon="Search"
            placeholder="搜活动、主办方或地点"
            clearable
            size="large"
          />
          <div class="filter-pills">
            <button
              v-for="category in categories"
              :key="category"
              type="button"
              class="filter-pill"
              :class="{ active: activeCategory === category }"
              @click="activeCategory = category"
            >
              {{ category }}
            </button>
          </div>
          <label class="availability-toggle">
            <el-switch v-model="onlyAvailable" />
            <span>只看有余位</span>
          </label>
        </div>

        <div v-if="loading" class="skeleton-wrap">
          <el-skeleton :rows="7" animated />
        </div>

        <div v-else-if="filteredEvents.length" class="event-grid">
          <EventCard v-for="event in filteredEvents" :key="event.id" :event="event" />
        </div>

        <div v-else class="empty-panel">
          <div class="empty-stamp" aria-hidden="true">空</div>
          <h3>这栏暂时没贴东西</h3>
          <p>换个关键词，或者把筛选撕掉再看看。</p>
          <el-button
            type="primary"
            @click="search = ''; activeCategory = '全部'; onlyAvailable = false"
          >
            重置筛选
          </el-button>
        </div>
      </section>
    </div>
  </AppShell>
</template>

<style scoped>
.events-page {
  padding-top: 34px;
}

.event-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(330px, 0.75fr);
  gap: clamp(28px, 5vw, 72px);
  align-items: center;
  margin-bottom: clamp(54px, 8vw, 94px);
}

.hero-copy {
  padding: 14px 0;
  animation: paper-in 420ms ease both;
}

.hero-copy h1 {
  margin-bottom: 24px;
  font-family: var(--display);
  font-size: clamp(58px, 8.4vw, 112px);
  font-weight: 900;
  letter-spacing: -0.085em;
  line-height: 0.91;
  text-wrap: balance;
}

.headline-highlight {
  position: relative;
  display: inline-block;
  z-index: 0;
  color: var(--ink);
}

.headline-highlight::before {
  position: absolute;
  z-index: -1;
  right: -0.05em;
  bottom: 0.02em;
  left: -0.06em;
  height: 0.38em;
  background: var(--butter);
  content: "";
  transform: rotate(-1.4deg);
}

.hero-description {
  max-width: 610px;
  margin-bottom: 30px;
  color: var(--ink-2);
  font-size: 15px;
  line-height: 1.85;
}

.hero-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 26px;
}

.hero-metrics > div {
  display: flex;
  min-width: 120px;
  align-items: baseline;
  gap: 7px;
  border-bottom: 2px solid var(--ink);
  padding: 0 2px 8px;
}

.hero-metrics strong {
  font-family: var(--display);
  font-size: 39px;
  font-weight: 900;
  letter-spacing: -0.07em;
  line-height: 1;
}

.hero-metrics span {
  color: var(--ink-2);
  font-size: 11px;
  font-weight: 700;
}

.hero-feature {
  position: relative;
  border: 2px solid var(--ink);
  border-radius: 18px;
  background: var(--surface);
  box-shadow: 8px 8px 0 var(--ink);
  transform: rotate(1.5deg);
  animation: paper-in 500ms 70ms ease both;
}

.feature-tape {
  position: absolute;
  z-index: 4;
  top: -16px;
  left: 50%;
  width: max-content;
  border: 1.5px solid var(--ink);
  border-radius: 4px;
  padding: 6px 16px;
  background: var(--butter);
  box-shadow: 2px 2px 0 var(--ink);
  font-size: 12px;
  font-weight: 900;
  transform: translateX(-50%) rotate(-2deg);
}

.feature-poster {
  border-radius: 15px 15px 0 0;
}

.feature-copy {
  padding: 17px 19px 19px;
}

.feature-date {
  display: flex;
  align-items: baseline;
  gap: 8px;
  width: max-content;
  margin-bottom: 13px;
  border-bottom: 1.5px dashed var(--line);
  padding-bottom: 10px;
}

.feature-date span,
.feature-date small {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
}

.feature-date strong {
  color: var(--cobalt);
  font-family: var(--display);
  font-size: 40px;
  font-weight: 900;
  letter-spacing: -0.08em;
  line-height: 0.9;
}

.feature-title {
  margin-bottom: 15px;
}

.feature-title h3 {
  margin-bottom: 5px;
  font-family: var(--display);
  font-size: 27px;
  font-weight: 900;
  letter-spacing: -0.045em;
  line-height: 1.1;
}

.feature-title p {
  margin-bottom: 0;
  color: var(--ink-3);
  font-size: 11px;
}

.feature-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--cobalt-deep);
  font-size: 13px;
  font-weight: 900;
}

.feature-link span {
  font-size: 18px;
  line-height: 0.8;
}

.events-section {
  position: relative;
}

.board-heading {
  align-items: flex-end;
  border-bottom: 2px solid var(--ink);
  padding-bottom: 16px;
}

.result-count {
  border: 1.5px solid var(--ink);
  border-radius: 999px;
  padding: 7px 13px;
  background: var(--surface);
  box-shadow: 2px 2px 0 var(--ink);
  font-size: 11px;
  font-weight: 750;
  white-space: nowrap;
  transform: rotate(1.5deg);
}

.result-count b {
  color: var(--cobalt);
  font-family: var(--display);
  font-size: 16px;
  font-weight: 900;
}

.event-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 0.78fr) minmax(0, 1.55fr) auto;
  gap: 16px;
  align-items: center;
  margin-bottom: 27px;
  border: 1.5px solid var(--ink);
  border-radius: 13px;
  padding: 11px;
  background: var(--surface);
  box-shadow: 3px 3px 0 var(--ink);
}

.availability-toggle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--ink-2);
  font-size: 11px;
  font-weight: 750;
  white-space: nowrap;
}

.event-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 24px;
}

.event-grid :deep(.event-card):nth-child(4n + 1),
.event-grid :deep(.event-card):nth-child(4n + 4) {
  grid-column: span 7;
}

.event-grid :deep(.event-card):nth-child(4n + 2),
.event-grid :deep(.event-card):nth-child(4n + 3) {
  grid-column: span 5;
}

.skeleton-wrap {
  border: 1.5px dashed var(--ink);
  border-radius: 18px;
  padding: 34px;
  background: var(--surface);
}

.empty-panel {
  display: grid;
  place-items: center;
  border: 1.5px dashed var(--ink);
  border-radius: 20px;
  padding: 54px 24px;
  background: var(--surface);
  text-align: center;
}

.empty-stamp {
  display: grid;
  width: 72px;
  height: 72px;
  margin-bottom: 15px;
  place-items: center;
  border: 2px dashed var(--tomato);
  border-radius: 50%;
  color: var(--tomato);
  font-family: var(--display);
  font-size: 30px;
  font-weight: 900;
  transform: rotate(-8deg);
}

.empty-panel h3 {
  margin-bottom: 7px;
  font-family: var(--display);
  font-size: 26px;
  font-weight: 900;
  letter-spacing: -0.04em;
}

.empty-panel p {
  margin-bottom: 18px;
  color: var(--ink-2);
  font-size: 13px;
}

@media (max-width: 1040px) {
  .event-hero {
    grid-template-columns: minmax(0, 1fr) minmax(300px, 0.72fr);
    gap: 30px;
  }

  .event-toolbar {
    grid-template-columns: 1fr;
  }

  .availability-toggle {
    padding: 4px 2px;
  }
}

@media (max-width: 860px) {
  .event-hero {
    grid-template-columns: 1fr;
  }

  .hero-feature {
    width: min(520px, 100%);
    justify-self: center;
  }

  .event-grid :deep(.event-card) {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .events-page {
    padding-top: 22px;
  }

  .hero-copy h1 {
    font-size: 57px;
  }

  .hero-metrics {
    gap: 12px;
  }

  .hero-metrics > div {
    min-width: 0;
    flex: 1;
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .hero-metrics strong {
    font-size: 31px;
  }

  .hero-metrics span {
    font-size: 9px;
    white-space: nowrap;
  }

  .hero-feature {
    transform: none;
  }

  .board-heading {
    align-items: flex-start;
  }

  .event-toolbar {
    gap: 12px;
  }

  .event-grid {
    gap: 20px;
  }
}
</style>
