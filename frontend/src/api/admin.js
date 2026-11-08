import http from './http'

export const adminApi = {
  async events() {
    const response = await http.get('/admin/events')
    return response.data.data
  },

  async event(id) {
    const response = await http.get(`/admin/events/${id}`)
    return response.data.data
  },

  async detail(id) {
    const response = await http.get(`/admin/events/${id}/detail`)
    return response.data.data
  },

  async createEvent(payload) {
    const response = await http.post('/admin/events', payload)
    return response.data.data
  },

  async updateEvent(id, payload) {
    const response = await http.put(`/admin/events/${id}`, payload)
    return response.data.data
  },

  async publish(id) {
    const response = await http.post(`/admin/events/${id}/publish`)
    return response.data.data
  },

  async cancel(id) {
    const response = await http.post(`/admin/events/${id}/cancel`)
    return response.data.data
  },

  async finish(id) {
    const response = await http.post(`/admin/events/${id}/finish`)
    return response.data.data
  },

  async registrations(id) {
    const response = await http.get(`/admin/events/${id}/registrations`)
    return response.data.data
  },

  async checkIn(registrationId) {
    const response = await http.post(`/admin/registrations/${registrationId}/check-in`)
    return response.data.data
  },

  async export(id) {
    return http.get(`/admin/events/${id}/export`, { responseType: 'blob' })
  },

  async generateCheckInToken(id) {
    const response = await http.post(`/admin/events/${id}/check-in-token`)
    return response.data.data
  },
}
