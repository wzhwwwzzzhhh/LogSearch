const BASE_URL = 'http://localhost:8080/api/logs/search'

const QUERIES = [
  { id: 1,  category: '事件分布',  query: '最近一小时各事件类型分布' },
  { id: 2,  category: '事件分布',  query: '过去24小时不同事件类型的数量统计' },
  { id: 3,  category: '事件分布',  query: '最近7天的事件类型占比情况' },
  { id: 4,  category: '事件分布',  query: '今天各事件的发生次数' },
  { id: 5,  category: '事件分布',  query: '最近30天的事件分布' },
  { id: 6,  category: '事件分布',  query: '过去1小时所有事件类型的数量' },
  { id: 7,  category: '事件分布',  query: '最近24小时page_view事件有多少' },
  { id: 8,  category: '事件分布',  query: '今天click事件的数量' },
  { id: 9,  category: '事件分布',  query: '最近7天payment事件的分布情况' },
  { id: 10, category: '事件分布',  query: '过去24小时login事件的统计' },
  { id: 11, category: '错误分析',  query: '最近一小时错误最多的类型' },
  { id: 12, category: '错误分析',  query: '过去24小时支付失败最多的错误信息' },
  { id: 13, category: '错误分析',  query: '今天发生的所有错误日志' },
  { id: 14, category: '错误分析',  query: '最近7天系统错误的统计' },
  { id: 15, category: '错误分析',  query: '过去1小时数据库连接超时出现了几次' },
  { id: 16, category: '错误分析',  query: '最近24小时错误率最高的页面' },
  { id: 17, category: '错误分析',  query: '今天支付网关响应超时的情况' },
  { id: 18, category: '错误分析',  query: '最近7天用户权限不足的错误次数' },
  { id: 19, category: '错误分析',  query: '过去24小时空指针异常发生了多少次' },
  { id: 20, category: '错误分析',  query: '最近一小时库存不足的报警统计' },
  { id: 21, category: '页面访问',  query: '最近一小时访问量最高的页面' },
  { id: 22, category: '页面访问',  query: '过去24小时热门页面排行' },
  { id: 23, category: '页面访问',  query: '今天首页的访问次数' },
  { id: 24, category: '页面访问',  query: '最近7天商品详情页的访问趋势' },
  { id: 25, category: '页面访问',  query: '过去1小时购物车页面的访问量' },
  { id: 26, category: '页面访问',  query: '最近24小时结算页面的访问统计' },
  { id: 27, category: '页面访问',  query: '今天搜索页面的使用情况' },
  { id: 28, category: '页面访问',  query: '最近7天支付成功页面的访问次数' },
  { id: 29, category: '页面访问',  query: '过去24小时用户个人中心的访问量' },
  { id: 30, category: '页面访问',  query: '最近一小时秒杀页面的访问情况' },
  { id: 31, category: '设备分析',  query: '最近24小时各设备类型分布' },
  { id: 32, category: '设备分析',  query: '今天移动端和PC端的访问比例' },
  { id: 33, category: '设备分析',  query: '最近7天不同设备的用户行为' },
  { id: 34, category: '设备分析',  query: '过去1小时Mobile设备的流量' },
  { id: 35, category: '设备分析',  query: '最近24小时Tablet设备的访问情况' },
  { id: 36, category: '设备分析',  query: '今天PC端的事件类型分布' },
  { id: 37, category: '设备分析',  query: '最近7天移动端错误统计' },
  { id: 38, category: '设备分析',  query: '过去24小时各设备的事件分布对比' },
  { id: 39, category: '设备分析',  query: '今天手机端访问量最高的页面' },
  { id: 40, category: '设备分析',  query: '最近一小时各设备活跃度' },
  { id: 41, category: '趋势分析',  query: '今天每小时的页面访问趋势' },
  { id: 42, category: '趋势分析',  query: '最近7天的日访问量走势' },
  { id: 43, category: '趋势分析',  query: '过去24小时事件发生的时间趋势' },
  { id: 44, category: '趋势分析',  query: '今天每小时的错误趋势' },
  { id: 45, category: '趋势分析',  query: '最近30天的用户访问趋势' },
  { id: 46, category: '趋势分析',  query: '过去24小时每10分钟的访问量变化' },
  { id: 47, category: '趋势分析',  query: '今天各小时的支付成功趋势' },
  { id: 48, category: '趋势分析',  query: '最近7天每小时的平均访问时长' },
  { id: 49, category: '趋势分析',  query: '过去24小时登录事件的时间分布' },
  { id: 50, category: '趋势分析',  query: '今天每小时的点击事件趋势' },
  { id: 51, category: '用户行为',  query: '过去24小时访问最多的用户' },
  { id: 52, category: '用户行为',  query: '最近7天活跃用户排行' },
  { id: 53, category: '用户行为',  query: '今天用户停留时长最长的页面' },
  { id: 54, category: '用户行为',  query: '最近一小时高频访问的用户' },
  { id: 55, category: '用户行为',  query: '过去24小时用户平均停留时长' },
  { id: 56, category: '用户行为',  query: '今天哪些用户发生了错误' },
  { id: 57, category: '用户行为',  query: '最近7天用户购物车操作统计' },
  { id: 58, category: '用户行为',  query: '过去24小时用户登录频率' },
  { id: 59, category: '用户行为',  query: '今天用户下单转化情况' },
  { id: 60, category: '用户行为',  query: '最近一小时用户行为路径' },
  { id: 61, category: '时间范围',  query: '最近5分钟的错误日志' },
  { id: 62, category: '时间范围',  query: '过去30分钟的页面访问' },
  { id: 63, category: '时间范围',  query: '最近3小时的各事件统计' },
  { id: 64, category: '时间范围',  query: '过去6小时的数据分布' },
  { id: 65, category: '时间范围',  query: '今天中午12点到下午2点的访问量' },
  { id: 66, category: '时间范围',  query: '最近12小时的错误趋势' },
  { id: 67, category: '时间范围',  query: '过去2天的数据对比' },
  { id: 68, category: '时间范围',  query: '最近一周的支付统计' },
  { id: 69, category: '时间范围',  query: '过去15分钟的异常事件' },
  { id: 70, category: '时间范围',  query: '最近45分钟的用户活跃度' },
  { id: 71, category: '组合查询',  query: '过去24小时PC端访问量最高的页面' },
  { id: 72, category: '组合查询',  query: '今天移动端错误最多的类型' },
  { id: 73, category: '组合查询',  query: '最近7天支付页面在手机上的访问趋势' },
  { id: 74, category: '组合查询',  query: '过去1小时购物车页面的设备分布' },
  { id: 75, category: '组合查询',  query: '今天用户登录后在PC端的行为统计' },
  { id: 76, category: '组合查询',  query: '最近24小时商品详情页在移动端的停留时长' },
  { id: 77, category: '组合查询',  query: '过去7天首页在平板上的访问量' },
  { id: 78, category: '组合查询',  query: '今天支付失败在移动端的分布' },
  { id: 79, category: '组合查询',  query: '最近一小时用户从搜索到支付的转化' },
  { id: 80, category: '组合查询',  query: '过去24小时错误日志中各页面的占比' },
  { id: 81, category: '异常检测',  query: '今天访问量异常高的页面' },
  { id: 82, category: '异常检测',  query: '最近7天持续时间最长的错误' },
  { id: 83, category: '异常检测',  query: '过去24小时响应超时的记录' },
  { id: 84, category: '异常检测',  query: '今天出现多次错误的用户' },
  { id: 85, category: '异常检测',  query: '最近一小时访问量突增的页面' },
  { id: 86, category: '异常检测',  query: '过去24小时异常的登录行为' },
  { id: 87, category: '异常检测',  query: '今天重复支付失败的订单' },
  { id: 88, category: '异常检测',  query: '最近7天访问频率异常的用户' },
  { id: 89, category: '异常检测',  query: '过去24小时页面加载失败的情况' },
  { id: 90, category: '异常检测',  query: '今天系统内部错误的具体信息' },
  { id: 91, category: '业务场景',  query: '最近24小时电商平台整体的运营概况' },
  { id: 92, category: '业务场景',  query: '今天用户从浏览到支付的漏斗分析' },
  { id: 93, category: '业务场景',  query: '过去7天每天的活跃用户数' },
  { id: 94, category: '业务场景',  query: '今天访问深度最高的用户' },
  { id: 95, category: '业务场景',  query: '最近24小时各页面的跳出率情况' },
  { id: 96, category: '业务场景',  query: '过去1小时促销活动的用户反馈' },
  { id: 97, category: '业务场景',  query: '今天搜索关键词的热度排行' },
  { id: 98, category: '业务场景',  query: '最近7天周末和工作日的访问对比' },
  { id: 99, category: '业务场景',  query: '过去24小时新用户和老用户的行为差异' },
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
  log('  LogAnalytics Batch Test Report')
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

  // Summary
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

  // Failed details
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

  // Performance
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
