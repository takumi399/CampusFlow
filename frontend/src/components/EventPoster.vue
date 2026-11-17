<script setup>
import { computed } from 'vue'

const props = defineProps({
  category: { type: String, default: '校园活动' },
  title: { type: String, default: '' },
  eventId: { type: [Number, String], default: '' },
  compact: { type: Boolean, default: false },
})

const themes = {
  文艺: { className: 'poster-live', word: 'LIVE', mark: '声' },
  技术: { className: 'poster-make', word: 'MAKE', mark: '码' },
  体育: { className: 'poster-play', word: 'PLAY', mark: '动' },
  兴趣: { className: 'poster-meet', word: 'MEET', mark: '趣' },
}

const theme = computed(() => {
  const normalized = props.category?.trim()
  return (
    themes[normalized] || {
      className: 'poster-campus',
      word: normalized ? normalized.slice(0, 5).toUpperCase() : 'CAMPUS',
      mark: normalized?.slice(0, 1) || '校',
    }
  )
})

const serial = computed(() => String(props.eventId || '').padStart(2, '0'))
</script>

<template>
  <div class="event-poster" :class="[theme.className, { compact }]">
    <span class="poster-serial">{{ serial ? `NO.${serial}` : 'CAMPUS' }}</span>
    <span class="poster-word">{{ theme.word }}</span>
    <span class="poster-category">{{ category }}</span>
    <span class="poster-mark" aria-hidden="true">{{ theme.mark }}</span>
    <span class="poster-shape poster-shape-one" aria-hidden="true"></span>
    <span class="poster-shape poster-shape-two" aria-hidden="true"></span>
    <span class="poster-stripe" aria-hidden="true"></span>
    <span v-if="title" class="poster-title">{{ title }}</span>
  </div>
</template>

<style scoped>
.event-poster {
  --poster-bg: var(--cobalt);
  --poster-ink: var(--surface);
  --poster-accent: var(--butter);
  position: relative;
  overflow: hidden;
  min-width: 0;
  aspect-ratio: 1.32;
  border-bottom: 1.5px solid var(--ink);
  color: var(--poster-ink);
  background: var(--poster-bg);
  isolation: isolate;
}

.poster-live {
  --poster-bg: var(--cobalt);
  --poster-ink: var(--surface);
  --poster-accent: var(--butter);
}

.poster-make {
  --poster-bg: var(--butter);
  --poster-ink: var(--ink);
  --poster-accent: var(--cobalt);
}

.poster-play {
  --poster-bg: var(--tomato);
  --poster-ink: var(--surface);
  --poster-accent: var(--mint);
}

.poster-meet {
  --poster-bg: var(--mint);
  --poster-ink: var(--ink);
  --poster-accent: var(--tomato);
}

.poster-campus {
  --poster-bg: var(--lilac);
  --poster-ink: var(--ink);
  --poster-accent: var(--cobalt);
}

.event-poster::after {
  position: absolute;
  z-index: -1;
  inset: 0;
  opacity: 0.18;
  background: radial-gradient(circle, currentColor 1.2px, transparent 1.2px) 0 0 / 9px 9px;
  content: "";
}

.poster-serial,
.poster-category {
  position: absolute;
  z-index: 2;
  font-family: var(--mono);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.poster-serial {
  top: 14px;
  left: 16px;
}

.poster-category {
  top: 12px;
  right: 14px;
  border: 1.5px solid currentColor;
  border-radius: 999px;
  padding: 3px 9px;
}

.poster-word {
  position: absolute;
  z-index: 2;
  right: 14px;
  bottom: 12px;
  font-family: var(--display);
  font-size: clamp(30px, 5vw, 58px);
  font-style: italic;
  font-weight: 900;
  letter-spacing: -0.08em;
  line-height: 0.9;
  transform: rotate(-3deg);
}

.poster-mark {
  position: absolute;
  z-index: 1;
  top: 50%;
  left: 50%;
  color: transparent;
  -webkit-text-stroke: 2px currentColor;
  font-family: var(--display);
  font-size: clamp(104px, 17vw, 190px);
  font-weight: 900;
  line-height: 0.8;
  opacity: 0.82;
  transform: translate(-50%, -48%) rotate(-7deg);
}

.event-poster.compact .poster-mark {
  font-size: clamp(72px, 12vw, 126px);
}

.poster-shape {
  position: absolute;
  z-index: 0;
  display: block;
  background: var(--poster-accent);
}

.poster-shape-one {
  top: 24%;
  right: -10%;
  width: 42%;
  aspect-ratio: 1;
  border: 2px solid var(--ink);
  border-radius: 50%;
}

.poster-shape-two {
  bottom: 16%;
  left: -7%;
  width: 28%;
  aspect-ratio: 1;
  border: 2px solid var(--ink);
  transform: rotate(22deg);
}

.poster-stripe {
  position: absolute;
  z-index: 0;
  top: -12%;
  left: 42%;
  width: 11%;
  height: 134%;
  border-right: 2px solid var(--ink);
  border-left: 2px solid var(--ink);
  background: var(--poster-accent);
  transform: rotate(17deg);
}

.poster-title {
  position: absolute;
  z-index: 2;
  bottom: 15px;
  left: 16px;
  width: 40%;
  font-size: 13px;
  font-weight: 850;
  line-height: 1.3;
}

@media (max-width: 560px) {
  .poster-serial,
  .poster-category {
    font-size: 9px;
  }

  .poster-serial {
    top: 11px;
    left: 12px;
  }

  .poster-category {
    top: 9px;
    right: 10px;
  }

  .poster-word {
    right: 11px;
    bottom: 9px;
  }
}
</style>
