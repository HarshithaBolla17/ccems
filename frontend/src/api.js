import axios from 'axios'

// In development Vite proxies /api to Spring Boot (see vite.config.js).
// For a deployed build, set VITE_API_URL to the backend's public URL + /api.
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' })

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ccems_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// An expired or invalid token anywhere except the auth screens signs the user out.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const url = err.config?.url || ''
    if (err.response?.status === 401 && !url.includes('/auth/')) {
      window.dispatchEvent(new Event('ccems:logout'))
    }
    return Promise.reject(err)
  },
)

export function errMsg(e) {
  if (e?.response?.data?.message) return e.response.data.message
  if (e?.request && !e.response) return 'Cannot reach the server. Check that the backend is running on port 8080.'
  return e?.message || 'Something went wrong.'
}

export default api
