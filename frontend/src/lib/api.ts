import axios from 'axios'

// Configure the backend base URL. Can be overridden via Vite env variable.
// Usage: create a .env file (e.g., .env, .env.development, .env.production)
// and set VITE_API_BASE to your backend address, for example:
// VITE_API_BASE=http://localhost:8080/

const baseURL = (import.meta as any)?.env?.VITE_API_BASE || 'http://localhost:8080/'

export const api = axios.create({
  baseURL,
  // Send cookies if backend uses session-based auth; harmless otherwise
  withCredentials: true,
})

export default api
