<script setup>
import { computed } from 'vue'
import { statusLabel } from '../utils/format'

const props = defineProps({
  status: { type: String, required: true },
  size: { type: String, default: 'default' },
  mode: { type: String, default: 'campus' },
})

const label = computed(() => statusLabel(props.status))
const variant = computed(() => `status-${(props.status || 'unknown').toLowerCase()}`)
</script>

<template>
  <span class="status-tag" :class="[variant, `status-${size}`, `mode-${mode}`]">
    <span class="status-dot" aria-hidden="true"></span>
    {{ label }}
  </span>
</template>

<style scoped>
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  width: max-content;
  min-height: 25px;
  border: 1.5px solid currentColor;
  border-radius: 5px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.15;
  white-space: nowrap;
  transform: rotate(-1.5deg);
}

.status-small {
  min-height: 22px;
  padding: 2px 7px;
  font-size: 10px;
}

.status-dot {
  width: 6px;
  height: 6px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: currentColor;
}

.status-published,
.status-confirmed {
  color: var(--mint-deep);
  background: var(--mint);
}

.status-promoted {
  color: var(--tomato);
  background: var(--tomato-soft);
}

.status-waitlisted {
  color: var(--ink);
  background: var(--butter);
}

.status-checked_in {
  color: var(--cobalt-deep);
  background: var(--cobalt-soft);
}

.status-draft,
.status-finished,
.status-canceled,
.status-expired,
.status-unknown {
  color: var(--ink-2);
  background: var(--surface-2);
}

.status-tag.mode-admin {
  gap: 6px;
  min-height: 24px;
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 10px;
  font-weight: 650;
  transform: none;
}

.status-tag.mode-admin.status-small {
  min-height: 22px;
  padding: 2px 8px;
  font-size: 10px;
}

.status-tag.mode-admin .status-dot {
  width: 5px;
  height: 5px;
}

.status-tag.mode-admin.status-published,
.status-tag.mode-admin.status-confirmed {
  color: #16744a;
  background: rgba(22, 116, 74, 0.1);
}

.status-tag.mode-admin.status-promoted {
  color: #a35d0b;
  background: rgba(183, 122, 22, 0.12);
}

.status-tag.mode-admin.status-waitlisted {
  color: #8a5a12;
  background: rgba(246, 207, 59, 0.16);
}

.status-tag.mode-admin.status-checked_in {
  color: #2449ad;
  background: rgba(45, 91, 216, 0.09);
}

.status-tag.mode-admin.status-draft,
.status-tag.mode-admin.status-finished,
.status-tag.mode-admin.status-canceled,
.status-tag.mode-admin.status-expired,
.status-tag.mode-admin.status-unknown {
  color: #5f6d81;
  background: #f0f3f7;
}
</style>
