<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import * as echarts from 'echarts'
import { statsOverview, statsDaily, statsHot, statsFeedback } from '@/api/admin'

const lineRef = ref()
const barRef = ref()
const pieRef = ref()
let charts = []

const overview = ref({ documents: 0, categories: 0, questions: 0, goodRate: 0 })
const daily = ref({ dates: [], counts: [] })
const hot = ref([])
const feedback = ref([])

const cards = computed(() => [
  { label: '累计提问量', value: overview.value.questions, color: '#1890ff' },
  { label: '知识库文档', value: overview.value.documents, color: '#52c41a' },
  { label: '问答好评率', value: `${overview.value.goodRate}%`, color: '#faad14' },
  { label: '知识库分类', value: overview.value.categories, color: '#722ed1' },
])

function initCharts() {
  const line = echarts.init(lineRef.value)
  line.setOption({
    title: { text: '每日提问量', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 50, bottom: 30 },
    xAxis: { type: 'category', data: daily.value.dates },
    yAxis: { type: 'value' },
    series: [
      {
        data: daily.value.counts,
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#1890ff' },
      },
    ],
  })

  const bar = echarts.init(barRef.value)
  bar.setOption({
    title: { text: '热门问题排行', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 130, right: 30, top: 50, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: hot.value.map((i) => i.name).reverse() },
    series: [
      {
        data: hot.value.map((i) => i.value).reverse(),
        type: 'bar',
        itemStyle: { color: '#52c41a', borderRadius: [0, 4, 4, 0] },
      },
    ],
  })

  const pie = echarts.init(pieRef.value)
  pie.setOption({
    title: { text: '问答反馈分布', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '52%'],
        data: feedback.value,
        label: { formatter: '{b}: {d}%' },
      },
    ],
  })

  charts = [line, bar, pie]
}

function handleResize() {
  charts.forEach((chart) => chart.resize())
}

async function loadData() {
  const [o, d, h, f] = await Promise.all([
    statsOverview(),
    statsDaily({ days: 7 }),
    statsHot({ limit: 6, days: 30 }),
    statsFeedback(),
  ])
  overview.value = o?.data || overview.value
  daily.value = d?.data || daily.value
  hot.value = h?.data || []
  feedback.value = f?.data || []
}

onMounted(async () => {
  try {
    await loadData()
  } finally {
    initCharts()
    window.addEventListener('resize', handleResize)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((chart) => chart.dispose())
  charts = []
})
</script>

<template>
  <div>
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :md="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="text-muted">{{ card.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts">
      <el-col :md="24">
        <el-card shadow="never"><div ref="lineRef" class="chart" /></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="charts">
      <el-col :md="14">
        <el-card shadow="never"><div ref="barRef" class="chart" /></el-card>
      </el-col>
      <el-col :md="10">
        <el-card shadow="never"><div ref="pieRef" class="chart" /></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.stat-card {
  text-align: center;
  margin-bottom: 16px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
}

.charts {
  margin-bottom: 16px;
}

.chart {
  height: 320px;
}
</style>
