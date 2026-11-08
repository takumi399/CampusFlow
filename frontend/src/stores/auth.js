import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '../api/auth'

const TOKEN_KEY = 'campusflow_token'
const USER_KEY = 'campusflow_user'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(readUser())

  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isLoggedIn = computed(() => Boolean(token.value))

  function persist() {
    if (token.value) {
      localStorage.setItem(TOKEN_KEY, token.value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
    if (user.value) {
      localStorage.setItem(USER_KEY, JSON.stringify(user.value))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }

  async function login(credentials) {
    const payload = await authApi.login(credentials)
    token.value = payload.token
    user.value = payload.user
    persist()
    return payload
  }

  async function fetchMe() {
    user.value = await authApi.me()
    persist()
    return user.value
  }

  function logout() {
    token.value = ''
    user.value = null
    persist()
  }

  return {
    token,
    user,
    isAdmin,
    isLoggedIn,
    login,
    fetchMe,
    logout,
  }
})
