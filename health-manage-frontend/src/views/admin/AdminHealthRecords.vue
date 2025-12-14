<template>
  <div class="admin-health-records">
    <el-card shadow="always">
      <template #header>
        <div class="toolbar">
          <div class="title-with-hint">
            <span>健康档案管理</span>
            <el-tooltip placement="bottom">
              <template #content>
                <div>异常规则</div>
                <div>心率：>100 或 <50</div>
                <div>收缩压：>140 或 <90</div>
                <div>舒张压：>90 或 <60</div>
                <div>血糖：>7.8 或 <3.9</div>
              </template>
              <el-icon class="hint-icon"><QuestionFilled /></el-icon>
            </el-tooltip>
          </div>
          <div class="filters">
            <el-input v-model="userIdFilter" placeholder="按用户ID筛选" style="width:200px" />
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
            <el-button type="primary" @click="fetchRecords">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="records" stripe size="large" :row-class-name="rowClass">
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="recordDate" label="记录日期" width="140" />
        <el-table-column prop="weight" label="体重" width="100" />
        <el-table-column prop="heartRate" label="心率" width="100" />
        <el-table-column label="血压" width="140">
          <template #default="{ row }">{{ row.systolic }}/{{ row.diastolic }}</template>
        </el-table-column>
        <el-table-column prop="steps" label="步数" width="100" />
        <el-table-column prop="bloodSugar" label="血糖" width="120" />
        <el-table-column prop="alarmStatus" label="报警状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.alarmStatus === 1 ? 'danger' : 'info'">{{ row.alarmStatus === 1 ? '已报警' : '未报警' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <template v-if="isAbnormal(row)">
              <el-button size="small" type="danger" :disabled="row.alarmStatus === 1" @click="markAlarm(row)">{{ row.alarmStatus === 1 ? '已报警' : '报警' }}</el-button>
            </template>
            <template v-else>
              <el-button size="small" type="success" disabled>正常</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination background layout="prev, pager, next" :current-page="page" :page-size="pageSize" :total="total" @current-change="handlePage" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import api from '../../utils/api'

const records = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const userIdFilter = ref<string>('')
const dateRange = ref<[Date, Date] | null>(null)

const toStr = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`

const fetchRecords = async () => {
  const params: any = { page: page.value, pageSize: pageSize.value }
  if (userIdFilter.value) params.userId = Number(userIdFilter.value)
  if (dateRange?.value && dateRange.value.length === 2) {
    params.startDate = toStr(dateRange.value[0])
    params.endDate = toStr(dateRange.value[1])
  }
  const res = await api.get('/admin/health-records', { params })
  if (res.code === 200) { records.value = res.data; total.value = res.total || 0 }
}

const isAbnormal = (r: any) => {
  if (r.heartRate != null && (r.heartRate > 100 || r.heartRate < 50)) return true
  if (r.systolic != null && (r.systolic > 140 || r.systolic < 90)) return true
  if (r.diastolic != null && (r.diastolic > 90 || r.diastolic < 60)) return true
  if (r.bloodSugar != null && (r.bloodSugar > 7.8 || r.bloodSugar < 3.9)) return true
  return false
}

const rowClass = ({ row }: any) => {
  return row.alarmStatus === 1 ? 'alarm-row' : ''
}

const markAlarm = async (row: any) => {
  const res = await api.post(`/admin/health-records/${row.id}/alarm`)
  if (res.code === 200) { row.alarmStatus = 1; ElMessage.success('已报警') }
}

fetchRecords()

const handlePage = (p: number) => {
  page.value = p
  fetchRecords()
}
</script>

<style scoped>
.toolbar { display:flex; justify-content:space-between; align-items:center }
.filters { display:flex; gap:10px; align-items:center }
.pagination { margin-top: 16px; display:flex; justify-content:flex-end }
.alarm-row { background: #fde2e2 }
.hint-icon { cursor:pointer; color:#909399 }
.hint-icon:hover { color:#606266 }
.title-with-hint { display:flex; align-items:center; gap:6px }
</style>
