<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import dayjs from 'dayjs'

const props = defineProps({
  deadline: { type: String, default: '' },
  prefix: { type: String, default: '剩余' },
})

const now = ref(Date.now())
let timer = null

const remainingMs = computed(() => {
  if (!props.deadline) return 0
  return Math.max(0, dayjs(props.deadline).valueOf() - now.value)
})

const text = computed(() => {
  const totalSeconds = Math.floor(remainingMs.value / 1000)
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})

watch(
  () => props.deadline,
  () => {
    now.value = Date.now()
  },
)

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})
</script>

<template>
  <span v-if="!deadline">—</span>
  <span
    v-else
    class="countdown"
    :class="{ expired: remainingMs <= 0 }"
    role="timer"
    :aria-label="remainingMs > 0 ? `${prefix} ${text}` : '已过期'"
  >
    {{ remainingMs > 0 ? `${prefix} ${text}` : '已过期' }}
  </span>
</template>
