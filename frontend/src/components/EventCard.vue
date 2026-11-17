<script setup>
import { computed } from 'vue'
import { Clock, Location } from '@element-plus/icons-vue'
import EventPoster from './EventPoster.vue'
import { formatDateParts, formatTimeRange } from '../utils/format'

const props = defineProps({
  event: { type: Object, required: true },
})

const date = computed(() => formatDateParts(props.event.startTime))
const percentage = computed(() => {
  if (!props.event.capacity) return 0
  return Math.min(100, Math.round((props.event.occupiedCount / props.event.capacity) * 100))
})
const isFull = computed(() => props.event.remainingSeats <= 0)
</script>

<template>
  <router-link :to="`/events/${event.id}`" class="event-card">
    <EventPoster :category="event.category" :event-id="event.id" />

    <div class="date-sticker" aria-label="活动日期">
      <span>{{ date.month }}</span>
      <strong>{{ date.day }}</strong>
      <small>{{ date.weekday }}</small>
    </div>

    <div class="event-card-body">
      <div class="event-card-kicker">
        <span class="organizer">{{ event.organizer }}</span>
        <span class="event-number">EVENT {{ String(event.id).padStart(2, '0') }}</span>
      </div>

      <h3>{{ event.title }}</h3>

      <div class="event-meta-list">
        <span><el-icon><Location /></el-icon>{{ event.location }}</span>
        <span><el-icon><Clock /></el-icon>{{ formatTimeRange(event.startTime, event.endTime) }}</span>
      </div>

      <div class="event-card-footer">
        <div class="capacity-block">
          <div class="capacity-label">
            <strong>{{ isFull ? '已满，可候补' : `还剩 ${event.remainingSeats} 席` }}</strong>
            <small>{{ event.occupiedCount }}/{{ event.capacity }}</small>
          </div>
          <div
            class="seat-meter"
            :class="{ full: isFull }"
            role="progressbar"
            :aria-valuenow="percentage"
            aria-valuemin="0"
            aria-valuemax="100"
            :aria-label="`报名进度 ${percentage}%`"
          >
            <span :style="{ width: `${percentage}%` }"></span>
          </div>
        </div>
        <span class="detail-link">拿张票 <span aria-hidden="true">↗</span></span>
      </div>
    </div>
  </router-link>
</template>

<style scoped>
.event-card {
  position: relative;
  display: flex;
  min-width: 0;
  flex-direction: column;
  border: 1.5px solid var(--ink);
  border-radius: 19px;
  background: var(--surface);
  box-shadow: var(--shadow-hard-sm);
  transition:
    transform 180ms ease,
    box-shadow 180ms ease;
}

.event-card:nth-child(even) {
  rotate: 0.45deg;
}

.event-card:hover {
  transform: translateY(-5px) rotate(-0.4deg);
  box-shadow: 7px 8px 0 var(--ink);
}

.event-card :deep(.event-poster) {
  border-radius: 17px 17px 0 0;
}

.date-sticker {
  position: absolute;
  z-index: 3;
  top: 15px;
  left: 18px;
  display: grid;
  width: 68px;
  min-height: 79px;
  place-items: center;
  border: 1.5px solid var(--ink);
  border-radius: 8px;
  padding: 8px 5px 7px;
  color: var(--ink);
  background: var(--surface);
  box-shadow: 3px 3px 0 var(--ink);
  transform: rotate(-4deg);
}

.date-sticker span,
.date-sticker small {
  color: var(--ink-2);
  font-family: var(--mono);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.date-sticker strong {
  font-family: var(--display);
  font-size: 29px;
  font-weight: 900;
  letter-spacing: -0.08em;
  line-height: 0.95;
}

.event-card-body {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  padding: 18px 20px 20px;
}

.event-card-kicker {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 9px;
  font-family: var(--mono);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.organizer {
  overflow: hidden;
  color: var(--cobalt);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-number {
  flex: 0 0 auto;
  color: var(--ink-3);
}

h3 {
  margin-bottom: 14px;
  font-family: var(--display);
  font-size: clamp(22px, 2.4vw, 31px);
  font-weight: 900;
  letter-spacing: -0.045em;
  line-height: 1.15;
  text-wrap: balance;
}

.event-meta-list {
  display: grid;
  gap: 7px;
  margin-bottom: 18px;
  color: var(--ink-2);
  font-size: 12px;
}

.event-meta-list span {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
}

.event-meta-list .el-icon {
  flex: 0 0 auto;
  color: var(--tomato);
}

.event-card-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-top: auto;
  border-top: 1.5px dashed var(--line);
  padding-top: 14px;
}

.capacity-block {
  min-width: 0;
  flex: 1;
  max-width: 240px;
}

.capacity-label {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 7px;
}

.capacity-label strong {
  font-size: 12px;
  font-weight: 800;
}

.capacity-label small {
  color: var(--ink-3);
  font-family: var(--mono);
  font-size: 9px;
  font-variant-numeric: tabular-nums;
}

.seat-meter {
  overflow: hidden;
  height: 9px;
  border: 1.5px solid var(--ink);
  border-radius: 999px;
  background: var(--paper-deep);
}

.seat-meter span {
  display: block;
  height: 100%;
  background: var(--cobalt);
  transition: width 300ms ease;
}

.seat-meter.full span {
  background: var(--tomato);
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--cobalt-deep);
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.detail-link span {
  font-size: 17px;
  line-height: 0.8;
}

@media (max-width: 560px) {
  .event-card:nth-child(even) {
    rotate: 0deg;
  }

  .date-sticker {
    top: 12px;
    left: 13px;
    width: 61px;
    min-height: 70px;
  }

  .date-sticker strong {
    font-size: 25px;
  }

  .event-card-body {
    padding: 16px 16px 18px;
  }

  h3 {
    font-size: 23px;
  }
}
</style>
