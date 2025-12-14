<template>
  <div class="dashboard-container">
    <div class="welcome-banner">
      <h3>早上好，用户！今天也是充满活力的一天 🌞</h3>
    </div>

    <el-row :gutter="20" class="kpi-cards">
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card heart-rate">
          <div class="card-header">
            <span>心率</span>
            <el-icon><Timer /></el-icon>
          </div>
          <div class="card-value">
            {{ kpiData.heartRate || '-' }} <span class="unit">bpm</span>
          </div>
          <div class="card-footer">
            <span class="status normal">正常</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card steps">
          <div class="card-header">
            <span>今日步数</span>
            <el-icon><Bicycle /></el-icon>
          </div>
          <div class="card-value">
            {{ kpiData.steps || '-' }} <span class="unit">步</span>
          </div>
          <div class="card-footer">
            <el-progress :percentage="Math.min(((kpiData.steps || 0) / 10000) * 100, 100)" :status="'success'" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card weight">
          <div class="card-header">
            <span>体重</span>
            <el-icon><User /></el-icon>
          </div>
          <div class="card-value">
            {{ kpiData.weight || '-' }} <span class="unit">kg</span>
          </div>
          <div class="card-footer">
            <span>最近更新</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="kpi-card bp">
          <div class="card-header">
            <span>血压</span>
            <el-icon><Odometer /></el-icon>
          </div>
          <div class="card-value">
            {{ kpiData.systolic }}/{{ kpiData.diastolic }} <span class="unit">mmHg</span>
          </div>
          <div class="card-footer">
            <span class="status normal">理想</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card shadow="always">
          <template #header>
            <div class="chart-header">
              <div class="title-with-hint">
                <span>健康趋势</span>
                <el-tooltip placement="bottom">
                  <template #content>
                    红色节点表示管理员提醒健康异常的数据
                  </template>
                  <el-icon class="hint-icon"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <el-radio-group v-model="chartPeriod" size="small">
                <el-radio-button label="7days">近7天</el-radio-button>
                <el-radio-button label="30days">近30天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </el-card>

        <el-card shadow="always" style="margin-top: 20px">
          <template #header>
            <span>近7天摄入卡路里</span>
          </template>
          <div ref="calChartRef" class="chart-container" style="height:240px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="always" class="shortcuts-card">
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="shortcuts">
            <div class="shortcut-btn" @click="$router.push('/health-data')">
              <div class="icon-bg blue"><el-icon><Edit /></el-icon></div>
              <span>记一笔</span>
            </div>
            <div class="shortcut-btn" @click="$router.push('/check-in')">
              <div class="icon-bg green"><el-icon><CircleCheck /></el-icon></div>
              <span>去打卡</span>
            </div>
          </div>
        </el-card>
        
        <el-card shadow="always" class="todo-card" style="margin-top: 20px">
          <template #header>
            <span>今日打卡任务</span>
          </template>
          <div v-if="todoList.length === 0" class="empty-todo">暂无任务</div>
          <div v-for="task in todoList" :key="task.id" class="todo-item">
            <el-checkbox v-model="task.completed" @change="toggleTask(task)">{{ task.name }}</el-checkbox>
          </div>
        </el-card>
        
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import api from '../utils/api'
import { Timer, Bicycle, User, Odometer, Edit, CircleCheck, QuestionFilled } from '@element-plus/icons-vue'

const chartPeriod = ref('7days')
const trendData = ref<any[]>([])
const trendChartRef = ref(null)
const calChartRef = ref(null)
const todoList = ref<any[]>([])
const userId = JSON.parse(localStorage.getItem('user') || '{}').id
let myChart: echarts.ECharts | null = null
let calChart: echarts.ECharts | null = null

const kpiData = ref({
  heartRate: 0,
  steps: 0,
  weight: 0,
  systolic: 0,
  diastolic: 0
})

const fetchData = async () => {
  try {
    const res = await api.get('/health/list', { params: { userId, page: 1, pageSize: 30 } })
    if (res.code === 200 && res.data.length > 0) {
      const latest = res.data[0]
      kpiData.value = latest
      trendData.value = res.data
      const count = chartPeriod.value === '30days' ? 30 : 7
      const built = buildTrend(count)
      updateChart(built)
    }
  } catch (e) {
    // error
  }
}

const fetchCalories = async () => {
  try {
    const end = new Date()
    const start = new Date(); start.setDate(end.getDate()-6)
    const toStr = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    const res = await api.get('/diet/history', { params: { userId, page: 1, pageSize: 100, startDate: toStr(start), endDate: toStr(end) } })
    if (res.code === 200) {
      const byDay: Record<string, number> = {}
      const buildDates: string[] = []
      for (let i=0;i<7;i++){ const d=new Date(start); d.setDate(start.getDate()+i); const k=toStr(d); buildDates.push(k); byDay[k]=0 }
      res.data.forEach((r: any)=>{ const k=r.recordDate; byDay[k]=(byDay[k]||0)+(r.calories||0) })
      const values = buildDates.map(d=>byDay[d]||0)
      if (calChart) calChart.setOption({ xAxis: { type:'category', data: buildDates }, yAxis:{ type:'value' }, series:[{ type:'bar', data: values, itemStyle:{ color:'#E6A23C' } }] })
    }
  } catch(e) {}
}

const fetchTodoList = async () => {
  try {
    const res = await api.get('/checkin/tasks', { params: { userId } })
    if (res.code === 200) {
      todoList.value = res.data
    }
  } catch (e) {
    // error
  }
}

const toggleTask = async (task: any) => {
  try {
    const res = await api.post('/checkin/toggle', {
      userId,
      taskId: task.id
    })
    if (res.code === 200) {
      task.completed = res.status
      if (task.completed) {
        ElMessage.success(`完成任务：${task.name}`)
      }
    }
  } catch (e) {
    // error
    task.completed = !task.completed // revert on error
  }
}

const updateChart = (data: any[]) => {
  if (!myChart) return
  const dates = data.map(item => item.recordDate)
  const weights = data.map(item => item.weight)
  const bps = data.map(item => item.systolic)
  const heartRates = data.map(item => item.heartRate)
  const bloodSugars = data.map(item => item.bloodSugar)
  const alarms = data.map(item => item.alarmStatus)
  
  const mark = (val: any, alarm: any) => alarm === 1 && val != null ? { value: val, symbolSize: 10, itemStyle: { color: '#F56C6C' } } : val
  myChart.setOption({
    xAxis: { data: dates },
    series: [
      { data: weights.map((v, i) => mark(v, alarms[i])) },
      { data: bps.map((v, i) => mark(v, alarms[i])) },
      { data: heartRates.map((v, i) => mark(v, alarms[i])) },
      { data: bloodSugars.map((v, i) => mark(v, alarms[i])) }
    ]
  })
}

const toStr = (d: Date) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const da = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${da}`
}

const buildTrend = (n: number) => {
  const today = new Date()
  const arr: any[] = []
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(today.getDate() - i)
    const s = toStr(d)
    const rec = trendData.value.find(r => r.recordDate === s)
    arr.push({
      recordDate: s,
      weight: rec ? rec.weight : null,
      systolic: rec ? rec.systolic : null,
      heartRate: rec ? rec.heartRate : null,
      bloodSugar: rec ? rec.bloodSugar : null,
      alarmStatus: rec ? rec.alarmStatus : 0
    })
  }
  return arr
}

const initChart = () => {
  if (trendChartRef.value) {
    myChart = echarts.init(trendChartRef.value)
    const option = {
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['体重', '收缩压', '心率', '血糖']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: []
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '体重',
          type: 'line',
          smooth: true,
          data: [],
          itemStyle: { color: '#42b983' }
        },
        {
          name: '收缩压',
          type: 'line',
          smooth: true,
          data: [],
          itemStyle: { color: '#35495e' }
        },
        {
          name: '心率',
          type: 'line',
          smooth: true,
          data: [],
          itemStyle: { color: '#E6A23C' }
        },
        {
          name: '血糖',
          type: 'line',
          smooth: true,
          data: [],
          itemStyle: { color: '#9B59B6' }
        }
      ]
    }
    myChart.setOption(option)
  }
  if (calChartRef.value) {
    calChart = echarts.init(calChartRef.value)
    calChart.setOption({ xAxis:{ type:'category', data:[] }, yAxis:{ type:'value' }, series:[{ type:'bar', data:[] }] })
  }
}

onMounted(() => {
  initChart()
  if (userId) {
    fetchData()
    fetchTodoList()
    fetchCalories()
  }
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  if (myChart) myChart.dispose()
  if (calChart) calChart.dispose()
})

const resizeChart = () => {
  if (myChart) myChart.resize()
  if (calChart) calChart.resize()
}

watch(chartPeriod, () => {
  const count = chartPeriod.value === '30days' ? 30 : 7
  const built = buildTrend(count)
  updateChart(built)
})
</script>

<style scoped lang="scss">
.dashboard-container {
  .welcome-banner {
    margin-bottom: 20px;
    h3 {
      color: #35495e;
      font-weight: 500;
    }
  }

  .kpi-cards {
    margin-bottom: 20px;
    
    .kpi-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        color: #888;
        font-size: 14px;
      }
      
      .card-value {
        font-size: 24px;
        font-weight: bold;
        margin: 10px 0;
        color: #333;
        
        .unit {
          font-size: 12px;
          color: #999;
          font-weight: normal;
        }
      }
      
      .card-footer {
        font-size: 12px;
        color: #666;
        
        .down {
          color: #42b983;
        }
        
        .status.normal {
          color: #42b983;
          background: rgba(66, 185, 131, 0.1);
          padding: 2px 6px;
          border-radius: 4px;
        }
      }
    }
  }

.charts-row {
  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .title-with-hint { display:flex; align-items:center; gap:6px }
  .hint-icon { cursor:pointer; color:#909399 }
  .hint-icon:hover { color:#606266 }
    
    .chart-container {
      height: 300px;
    }
    
    .shortcuts {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 15px;
      
      .shortcut-btn {
        display: flex;
        flex-direction: column;
        align-items: center;
        cursor: pointer;
        padding: 10px;
        border-radius: 8px;
        transition: background 0.3s;
        
        &:hover {
          background: #f5f7fa;
        }
        
        .icon-bg {
          width: 40px;
          height: 40px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
          margin-bottom: 8px;
          
          &.blue { background: #409EFF; }
          &.green { background: #67C23A; }
          &.orange { background: #E6A23C; }
          &.purple { background: #909399; }
        }
        
        span {
          font-size: 13px;
          color: #606266;
        }
      }
    }
    
    .todo-card {
      .empty-todo {
        color: #999;
        text-align: center;
        padding: 20px;
        font-size: 13px;
      }
      .todo-item {
        margin-bottom: 10px;
      }
    }
  }
}
</style>
