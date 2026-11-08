import http from './http'

export const authApi = {
  async login(payload) {
    const response = await http.post('/auth/login', payload)
    return response.data.data
  },

  async me() {
    const response = await http.get('/auth/me')
    return response.data.data
  },
}
