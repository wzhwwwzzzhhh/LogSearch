const BASE_URL = 'http://localhost:8080/api/logs/search'

const QUERIES = [
  { id: 1,  category: '事件分布',  query: '最近一小时各事件类型分布' },
  { id: 2,  category: '事件分布',  query: '过去24小时不同事件类型的数量统计' },
  { id: 3,  category: '事件分布',  query: '最近7天的事件类型占比情况' },
  { id: 4,  category: '事件分布',  query: '今天各事件的发生次数' },
  { id: 5,  category: '事件分布',  query: '最近30天的事件分布' },
  { id: 11, category: '错误分析',  query: '最近一小时错误最多的类型' },
  { id: 12, category: '错误分析',  query: '过去24小时支付失败最多的错误信息' },
  { id: 13, category: '错误分析',  query: '今天发生的所有错误日志' },
  { id: 14, category: '错误分析',  query: '最近7天系统错误的统计' },
  { id: 15, category: '错误分析',  query: '过去1小时数据库连接超时出现了几次' },
  { id: 21, category: '页面访问',  query: '最近一小时访问量最高的页面' },
  { id: 22, category: '页面访问',  query: '过去24小时热门页面排行' },
  { id: 31, category: '设备分析',  query: '最近24小时各设备类型分布' },
  { id: 32, category: '设备分析',  query: '今天移动端和PC端的访问比例' },
  { id: 41, category: '趋势分析',  query: '今天每小时的页面访问趋势' },
  { id: 42, category: '趋势分析',  query: '最近7天的日访问量走势' },
  { id: 51, category: '用户行为',  query: '过去24小时访问最多的用户' },
  { id: 52, category: '用户行为',  query: '最近7天活跃用户排行' },
  { id: 61, category: '时间范围',  query: '最近5分钟的错误日志' },
  { id: 62, category: '时间范围',  query: '过去30分钟的页面访问' },
  { id: 71, category: '组合查询',  query: '过去24小时PC端访问量最高的页面' },
  { id: 72, category: '组合查询',  query: '今天移动端错误最多的类型' },
  { id: 81, category: '异常检测',  query: '今天访问量异常高的页面' },
  { id: 91, category: '业务场景',  query: '最近24小时电商平台整体的运营概况' },
  { id: 100,category: '业务场景',  query: '今天最需要关注的系统问题' },
]

const fs = require('fs')
const path = require('path')
const reportFile = path.join(__dirname, 'batch-test-report.json')
const logFile = path.join(__dirname, 'batch-test-log.txt')

const logStream = fs.createWriteStream(logFile, { flags: 'w' })
function log(msg) {
  logStream.write(msg + '\n')
  process.stdout.write(msg + '\n')
}

async function testOne(item) {
  const start = Date.now()
  try {
    const res = await fetch(BASE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ question: item.query, page: 1, size: 20 })
    })
    const elapsed = Date.now() - start
    const body = await res.json()

    if (body.code === 200 && body.data) {
      const data = body.data
      const hasAggs = data.aggregations && Object.keys(data.aggregations).length > 0
      const hasLogs = data.logs && data.logs.length > 0
      const total = data.pagination ? data.pagination.total : 0
      const hasTaskId = !!data.analysisTaskId
      return {
        id: item.id, category: item.category, query: item.query,
        status: 'PASS', code: body.code, ms: elapsed,
        detail: `aggregations:${hasAggs} logs:${hasLogs ? data.logs.length + '条' : '0'} total:${total} taskId:${hasTaskId}`
      }
    } else {
      return {
        id: item.id, category: item.category, query: item.query,
        status: 'FAIL', code: body.code, ms: elapsed,
        detail: `error: ${body.message || 'unknown'}`
      }
    }
  } catch (err) {
    return {
      id: item.id, category: item.category, query: item.query,
      status: 'ERROR', code: -1, ms: Date.now() - start,
      detail: `exception: ${err.message}`
    }
  }
}

async function runAll() {
  log('\n======================================================================')
  log('  LogAnalytics Batch Test Report (25 queries)')
  log(`  Server: ${BASE_URL}`)
  log(`  Total:  ${QUERIES.length} queries`)
  log(`  Time:   ${new Date().toLocaleString()}`)
  log('======================================================================\n')

  const allResults = []
  const catStats = {}

  for (let i = 0; i < QUERIES.length; i++) {
    const item = QUERIES[i]
    process.stdout.write(`  #${String(item.id).padStart(3)} [${item.category}] ${item.query.substring(0, 28).padEnd(28)} ... `)

    const result = await testOne(item)
    allResults.push(result)

    if (!catStats[result.category]) catStats[result.category] = { total: 0, pass: 0, fail: 0 }
    catStats[result.category].total++
    if (result.status === 'PASS') catStats[result.category].pass++
    else catStats[result.category].fail++

    const icon = result.status === 'PASS' ? '✅' : result.status === 'FAIL' ? '❌' : '💥'
    log(`${icon} ${result.status} (${result.ms}ms)  ${result.detail}`)
  }

  log('\n======================================================================')
  log('  CATEGORY SUMMARY')
  log('======================================================================')
  log(`  ${'Category'.padEnd(12)} ${'Total'.padStart(5)} ${'Pass'.padStart(5)} ${'Fail'.padStart(5)} ${'Rate'.padStart(7)}`)
  log('  ' + '-'.repeat(40))
  let totalPass = 0, totalFail = 0
  for (const [cat, st] of Object.entries(catStats)) {
    const rate = st.total > 0 ? (st.pass / st.total * 100).toFixed(1) + '%' : '-'
    log(`  ${cat.padEnd(12)} ${String(st.total).padStart(5)} ${String(st.pass).padStart(5)} ${String(st.fail).padStart(5)} ${rate.padStart(7)}`)
    totalPass += st.pass; totalFail += st.fail
  }
  log('  ' + '-'.repeat(40))
  const totalRate = (totalPass / QUERIES.length * 100).toFixed(1)
  log(`  ${'TOTAL'.padEnd(12)} ${String(QUERIES.length).padStart(5)} ${String(totalPass).padStart(5)} ${String(totalFail).padStart(5)} ${totalRate.padStart(7)}`)

  const failed = allResults.filter(r => r.status !== 'PASS')
  if (failed.length > 0) {
    log('\n======================================================================')
    log('  FAILED DETAILS')
    log('======================================================================')
    for (const f of failed) {
      log(`  #${f.id} [${f.category}] ${f.query}`)
      log(`       ${f.detail}`)
    }
  }

  const ms = allResults.map(r => r.ms).sort((a, b) => a - b)
  const avg = (ms.reduce((a, b) => a + b, 0) / ms.length).toFixed(0)
  log('\n======================================================================')
  log(`  PERF: avg=${avg}ms min=${ms[0]}ms max=${ms[ms.length-1]}ms P50=${ms[Math.floor(ms.length*0.5)]}ms P95=${ms[Math.floor(ms.length*0.95)]}ms`)
  log('======================================================================')

  fs.writeFileSync(reportFile, JSON.stringify({
    time: new Date().toISOString(), total: QUERIES.length,
    pass: totalPass, fail: totalFail, rate: totalRate + '%',
    avgMs: avg, results: allResults
  }, null, 2))
  log(`\nReport saved to: ${reportFile}`)
  log(`Log saved to:    ${logFile}`)
  logStream.end()
}

runAll().catch(err => {
  log(`\nFATAL: ${err.message}`)
  logStream.end()
  process.exit(1)
})
