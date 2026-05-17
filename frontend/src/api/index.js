import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      console.error('API错误:', res.message)
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    console.error('请求异常:', error)
    return Promise.reject(error)
  }
)

export function searchLogs(question, page = 1, size = 20) {
  return api.post('/logs/search', { question, page, size })
}

export function getAnalysisResult(taskId) {
  return api.get(`/logs/analyze/${taskId}`)
}

export function healthCheck() {
  return api.get('/logs/health')
}
