import http from './http'

export const checkInApi = {
  async checkIn(token) {
    const response = await http.post(`/check-in/${encodeURIComponent(token)}`)
    return response.data.data
  },
}
