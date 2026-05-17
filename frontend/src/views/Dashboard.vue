<template>
  <div class="dashboard">
    <header class="header">
      <h1>LogAnalytics</h1>
      <p class="subtitle">电商用户行为日志分析平台</p>
    </header>

    <main class="main-content">
      <el-card class="search-card" shadow="never">
        <div class="search-section">
          <el-input
            v-model="searchQuery"
            placeholder="请输入您的问题，例如：最近一小时支付失败最多的错误类型"
            size="large"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button type="primary" @click="handleSearch" :loading="loading">
                查询
              </el-button>
            </template>
          </el-input>
        </div>

        <div class="preset-questions">
          <span class="preset-label">快速示例：</span>
          <el-tag
            v-for="(q, index) in presetQuestions"
            :key="index"
            class="preset-tag"
            :type="index % 2 === 0 ? 'primary' : 'success'"
            effect="plain"
            @click="selectPreset(q)"
          >
            {{ q }}
          </el-tag>
        </div>
      </el-card>

      <div v-if="loading" class="loading-section">
        <div class="search-steps">
          <div class="step" :class="{ active: searchStep >= 1, done: searchStep > 1 }">
            <span class="step-icon">
              <template v-if="searchStep > 1">✅</template>
              <template v-else>🤖</template>
            </span>
            <div class="step-content">
              <span class="step-title">AI 正在理解问题</span>
              <span class="step-desc">分析语义，生成查询语句</span>
            </div>
          </div>
          <div class="step" :class="{ active: searchStep >= 2, done: searchStep > 2 }">
            <span class="step-icon">
              <template v-if="searchStep > 2">✅</template>
              <template v-else>🔍</template>
            </span>
            <div class="step-content">
              <span class="step-title">正在查询日志数据</span>
              <span class="step-desc">检索 Elasticsearch 索引</span>
            </div>
          </div>
          <div class="step" :class="{ active: searchStep >= 3, done: searchStep >= 3 }">
            <span class="step-icon">
              <template v-if="searchStep >= 3">✅</template>
              <template v-else>📊</template>
            </span>
            <div class="step-content">
              <span class="step-title">正在渲染结果</span>
              <span class="step-desc">生成图表和数据展示</span>
            </div>
          </div>
        </div>
      </div>

      <template v-if="!loading && hasResult">
        <el-card class="analysis-card" shadow="never">
          <template #header>
            <span class="analysis-title">
              <el-icon><DataAnalysis /></el-icon>
              AI智能分析
            </span>
          </template>

          <div v-if="analysisResult" class="analysis-result">
            <el-alert
              :title="analysisResult"
              type="success"
              :closable="false"
              show-icon
            />
          </div>

          <div v-else-if="analysisError" class="analysis-result">
            <el-alert
              :title="analysisError"
              type="error"
              :closable="false"
              show-icon
            />
          </div>

          <div v-else-if="analyzing" class="analysis-pending">
            <el-button type="primary" loading>
              AI分析中，请稍候...
            </el-button>
            <span class="analysis-hint">AI正在分析聚合数据，生成洞察报告</span>
          </div>

          <div v-else class="analysis-idle">
            <el-button type="primary" @click="handleAnalyze" :icon="Aim">
              开始AI分析
            </el-button>
            <span class="analysis-hint">点击后AI将自动分析查询结果，生成数据洞察与业务建议</span>
          </div>
        </el-card>

        <el-card v-if="hasChart" class="chart-card" shadow="never">
          <template #header>
            <span class="chart-title">
              <el-icon><DataAnalysis /></el-icon>
              数据可视化
            </span>
          </template>
          <div ref="chartRef" class="chart-container"></div>
        </el-card>

        <el-card class="logs-card" shadow="never">
          <template #header>
            <span class="logs-title">
              <el-icon><Document /></el-icon>
              日志明细
              <span class="total-info">共 {{ pagination.total }} 条</span>
            </span>
          </template>

          <el-table
            :data="logs"
            stripe
            border
            style="width: 100%"
            v-if="logs.length > 0"
          >
            <el-table-column prop="timestamp" label="时间" width="180" />
            <el-table-column prop="user_id" label="用户ID" width="100" />
            <el-table-column prop="page" label="页面" min-width="150" />
            <el-table-column prop="event" label="事件" width="100">
              <template #default="{ row }">
                <el-tag :type="getEventType(row.event)" size="small">
                  {{ row.event }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="时长(秒)" width="90" />
            <el-table-column prop="device" label="设备" width="80" />
            <el-table-column prop="error_msg" label="错误信息" min-width="200" />
          </el-table>

          <el-empty v-else description="暂无日志数据" />

          <div class="pagination-wrapper" v-if="pagination.total > 0">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="pagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="handleSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </el-card>
      </template>

      <el-empty
        v-if="!loading && !hasResult && hasSearched"
        description="未找到相关数据，请尝试其他查询"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Search, Aim, DataAnalysis, Document } from '@element-plus/icons-vue'
import { searchLogs, getAnalysisResult } from '../api/index.js'
import * as echarts from 'echarts'

const searchQuery = ref('')
const loading = ref(false)
const searchStep = ref(0)
const hasSearched = ref(false)
const logs = ref([])
const pagination = ref({ page: 1, size: 20, total: 0 })
const aggregations = ref(null)
const currentPage = ref(1)
const pageSize = ref(20)
const chartRef = ref(null)
let chartInstance = null

const analyzing = ref(false)
const analysisTaskId = ref('')
const analysisResult = ref('')
const analysisError = ref('')
let pollTimer = null

const presetQuestions = [
  '最近一小时各事件类型分布',
  '过去24小时访问量最高的页面',
  '最近一小时支付失败最多的错误类型',
  '今天每小时的页面访问趋势',
  '最近24小时各设备类型分布'
]

const hasResult = ref(false)

const hasChart = ref(false)

const getEventType = (event) => {
  const map = {
    'page_view': '',
    'click': 'success',
    'payment': 'warning',
    'error': 'danger',
    'login': 'info'
  }
  return map[event] || 'info'
}

const selectPreset = async (question) => {
  searchQuery.value = question
  await handleSearch()
}

const cleanupAnalysis = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  analyzing.value = false
  analysisTaskId.value = ''
  analysisResult.value = ''
  analysisError.value = ''
}

const handleSearch = async () => {
  if (!searchQuery.value.trim()) return

  cleanupAnalysis()
  loading.value = true
  searchStep.value = 1
  hasSearched.value = true
  hasResult.value = false
  hasChart.value = false

  const stepTimer1 = setTimeout(() => {
    searchStep.value = 2
  }, 800)
  const stepTimer2 = setTimeout(() => {
    searchStep.value = 3
  }, 1600)

  try {
    const data = await searchLogs(searchQuery.value, currentPage.value, pageSize.value)

    clearTimeout(stepTimer1)
    clearTimeout(stepTimer2)
    searchStep.value = 3

    if (data) {
      logs.value = data.logs || []
      pagination.value = data.pagination || { page: 1, size: 20, total: 0 }
      hasResult.value = true

      if (data.aggregations) {
        aggregations.value = data.aggregations
        hasChart.value = true
      } else {
        aggregations.value = null
        hasChart.value = false
      }

      if (data.analysisTaskId) {
        analysisTaskId.value = data.analysisTaskId
      }
    }
  } catch (error) {
    console.error('查询失败:', error)
  } finally {
    loading.value = false
    searchStep.value = 0
  }
}

const handleAnalyze = async () => {
  if (analyzing.value) return
  if (!analysisTaskId.value) return

  analyzing.value = true
  analysisResult.value = ''
  analysisError.value = ''
  startPolling()
}

const startPolling = () => {
  if (pollTimer) clearInterval(pollTimer)
  pollTimer = setInterval(async () => {
    if (!analysisTaskId.value) {
      clearInterval(pollTimer)
      pollTimer = null
      return
    }

    try {
      const data = await getAnalysisResult(analysisTaskId.value)
      if (data && data.analysisTask) {
        const task = data.analysisTask
        if (task.status === 'SUCCESS') {
          analysisResult.value = task.result || '分析完成'
          analyzing.value = false
          clearInterval(pollTimer)
          pollTimer = null
        } else if (task.status === 'FAILED') {
          analysisError.value = task.error || 'AI分析失败'
          analyzing.value = false
          clearInterval(pollTimer)
          pollTimer = null
        }
      }
    } catch (error) {
      console.error('轮询分析结果异常:', error)
    }
  }, 2000)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  handleSearch()
}

const handlePageChange = (page) => {
  currentPage.value = page
  handleSearch()
}

const renderChart = () => {
  if (!chartRef.value) return

  if (chartInstance) {
    chartInstance.dispose()
  }

  chartInstance = echarts.init(chartRef.value)

  const aggData = aggregations.value
  if (!aggData) return

  const aggKeys = Object.keys(aggData)
  if (aggKeys.length === 0) return

  const firstAgg = aggData[aggKeys[0]]
  if (!Array.isArray(firstAgg) || firstAgg.length === 0) return

  const isTimeSeries = firstAgg[0].key && /\d{4}-\d{2}-\d{2}/.test(firstAgg[0].key)

  let keys
  if (isTimeSeries) {
    keys = firstAgg.map(item => {
      const k = item.key
      if (k.length >= 16) {
        return k.substring(5, 16).replace('T', ' ')
      }
      if (k.length >= 10) {
        return k.substring(5, 10)
      }
      return k
    })
  } else {
    keys = firstAgg.map(item => item.key.length > 12 ? item.key.substring(0, 12) + '...' : item.key)
  }
  const values = firstAgg.map(item => item.count)

  let option

  if (isTimeSeries) {
    option = {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        data: keys,
        axisLabel: { rotate: 45, fontSize: 11 }
      },
      yAxis: { type: 'value' },
      series: [{
        data: values,
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.3)' },
            { offset: 1, color: 'rgba(64,158,255,0.05)' }
          ])
        },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' }
      }]
    }
  } else if (firstAgg.length <= 5) {
    option = {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['30%', '60%'],
        center: ['50%', '55%'],
        data: firstAgg.map(item => ({ name: item.key, value: item.count })),
        label: {
          formatter: '{b}: {d}%'
        }
      }]
    }
  } else {
    option = {
      tooltip: { trigger: 'axis' },
      grid: { left: '3%', right: '4%', bottom: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        data: keys,
        axisLabel: { rotate: 45, fontSize: 10 }
      },
      yAxis: { type: 'value' },
      series: [{
        data: values,
        type: 'bar',
        barWidth: '50%',
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#79bbff' }
          ])
        }
      }]
    }
  }

  chartInstance.setOption(option)
}

const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

watch(aggregations, (newVal) => {
  if (newVal && chartRef.value) {
    renderChart()
  }
}, { flush: 'post' })

onMounted(() => {
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: white;
  padding: 32px 40px;
  text-align: center;
}

.header h1 {
  font-size: 28px;
  font-weight: 600;
  letter-spacing: 2px;
}

.header .subtitle {
  margin-top: 8px;
  font-size: 14px;
  opacity: 0.7;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.search-section {
  margin-bottom: 12px;
}

.preset-questions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.preset-label {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.preset-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.preset-tag:hover {
  transform: translateY(-1px);
}

.loading-section {
  padding: 32px 40px;
  background: white;
  border-radius: 8px;
}

.search-steps {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.step {
  display: flex;
  align-items: center;
  gap: 16px;
  opacity: 0.3;
  transition: all 0.5s ease;
}

.step.active {
  opacity: 1;
}

.step.done {
  opacity: 0.6;
}

.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}

.step.active .step-icon {
  background: #ecf5ff;
  box-shadow: 0 0 0 2px #409eff;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 2px rgba(64,158,255,0.4); }
  50% { box-shadow: 0 0 0 6px rgba(64,158,255,0.1); }
}

.step-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.step-desc {
  font-size: 13px;
  color: #909399;
}

.analysis-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.analysis-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.analysis-idle {
  display: flex;
  align-items: center;
  gap: 16px;
}

.analysis-pending {
  display: flex;
  align-items: center;
  gap: 16px;
}

.analysis-result {
  line-height: 1.6;
}

.analysis-hint {
  font-size: 13px;
  color: #909399;
}

.chart-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.chart-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.chart-container {
  width: 100%;
  height: 350px;
}

.logs-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.logs-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.total-info {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
  margin-left: 8px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
