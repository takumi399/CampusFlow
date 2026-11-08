import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale('zh-cn')

export function formatDateTime(value) {
  return value ? dayjs(value).format('MM月DD日 HH:mm') : '—'
}

export function formatFullDateTime(value) {
  return value ? dayjs(value).format('YYYY年MM月DD日 HH:mm') : '—'
}

export function formatTime(value) {
  return value ? dayjs(value).format('HH:mm') : '—'
}

export function formatTimeRange(start, end) {
  if (!start || !end) return '—'
  const startDate = dayjs(start)
  const endDate = dayjs(end)
  if (startDate.isSame(endDate, 'day')) {
    return `${startDate.format('MM月DD日 HH:mm')} - ${endDate.format('HH:mm')}`
  }
  return `${startDate.format('MM月DD日 HH:mm')} - ${endDate.format('MM月DD日 HH:mm')}`
}

export function formatDateParts(value) {
  if (!value) {
    return { month: '--', day: '--', weekday: '' }
  }
  const date = dayjs(value)
  return {
    month: date.format('MM月'),
    day: date.format('DD'),
    weekday: date.format('ddd'),
  }
}

export function formatRelativeDay(value) {
  if (!value) return '—'
  const date = dayjs(value)
  const today = dayjs().startOf('day')
  const target = date.startOf('day')
  const diff = target.diff(today, 'day')
  if (diff === 0) return '今天'
  if (diff === 1) return '明天'
  if (diff === 2) return '后天'
  return date.format('MM月DD日')
}

export function statusLabel(status) {
  const map = {
    DRAFT: '草稿',
    PUBLISHED: '报名中',
    CANCELED: '已取消',
    FINISHED: '已结束',
    CONFIRMED: '已确认',
    WAITLISTED: '候补中',
    PROMOTED: '待确认',
    CHECKED_IN: '已签到',
    EXPIRED: '已过期',
  }
  return map[status] || status || '未知'
}

export function statusType(status) {
  const map = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    CANCELED: 'info',
    FINISHED: 'info',
    CONFIRMED: 'success',
    WAITLISTED: 'warning',
    PROMOTED: 'danger',
    CHECKED_IN: 'primary',
    EXPIRED: 'info',
  }
  return map[status] || 'info'
}

export function roleLabel(role) {
  return role === 'ADMIN' ? '管理员' : '学生'
}

export function isRegistrationOpen(event) {
  if (!event || event.status !== 'PUBLISHED') return false
  const now = dayjs()
  return (
    now.isAfter(dayjs(event.registrationStart)) &&
    now.isBefore(dayjs(event.registrationEnd))
  )
}

export function isPast(value) {
  return value ? dayjs(value).isBefore(dayjs()) : false
}
