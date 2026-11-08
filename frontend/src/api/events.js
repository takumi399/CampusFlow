import http from './http'

export const eventApi = {
  async list() {
    const response = await http.get('/events')
    return response.data.data
  },

  async get(id) {
    const response = await http.get(`/events/${id}`)
    return response.data.data
  },

  async register(id) {
    const response = await http.post(`/events/${id}/registrations`)
    return response.data.data
  },

  async cancel(registrationId) {
    const response = await http.delete(`/registrations/${registrationId}`)
    return response.data.data
  },

  async confirm(registrationId) {
    const response = await http.post(`/registrations/${registrationId}/confirm`)
    return response.data.data
  },

  async mine() {
    const response = await http.get('/me/registrations')
    return response.data.data
  },
}
