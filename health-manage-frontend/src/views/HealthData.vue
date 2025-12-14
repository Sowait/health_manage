<template>
  <div class="health-data-container">
    <div class="toolbar">
      <el-button type="primary" size="large" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon> 新增记录
      </el-button>
      <div class="filter">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          size="large"
          @change="handleDateChange"
        />
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" style="width: 100%" stripe size="large" :row-class-name="rowClass">
        <el-table-column prop="recordDate" label="记录日期" width="180" sortable />
        <el-table-column prop="weight" label="体重 (kg)" width="120" />
        <el-table-column prop="heartRate" label="心率 (bpm)" width="120" />
        <el-table-column label="血压 (mmHg)" width="150">
          <template #default="scope">
            {{ scope.row.systolic }}/{{ scope.row.diastolic }}
          </template>
        </el-table-column>
        <el-table-column prop="steps" label="步数" width="120" />
        <el-table-column prop="bloodSugar" label="血糖 (mmol/L)" width="150" />
        <el-table-column prop="alarmStatus" label="报警状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.alarmStatus === 1 ? 'danger' : 'info'">{{ scope.row.alarmStatus === 1 ? '已报警' : '未报警' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="新增健康记录"
      width="500px"
      destroy-on-close
      class="record-dialog"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="记录日期">
          <el-date-picker v-model="form.date" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="体重 (kg)">
          <el-input-number v-model="form.weight" :precision="1" :step="0.1" />
        </el-form-item>
        <el-form-item label="心率 (bpm)">
          <el-input-number v-model="form.heartRate" :step="1" />
        </el-form-item>
        <el-form-item label="收缩压">
          <el-input-number v-model="form.systolic" :step="1" />
        </el-form-item>
        <el-form-item label="舒张压">
          <el-input-number v-model="form.diastolic" :step="1" />
        </el-form-item>
        <el-form-item label="今日步数">
          <el-input-number v-model="form.steps" :step="100" />
        </el-form-item>
        <el-form-item label="血糖">
          <el-input-number v-model="form.bloodSugar" :precision="1" :step="0.1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave">确认保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'

const dateRange = ref<[Date, Date] | null>(null)
const dialogVisible = ref(false)
const userId = JSON.parse(localStorage.getItem('user') || '{}').id

const form = reactive({
  id: undefined,
  date: new Date(),
  weight: 60.0,
  heartRate: 75,
  systolic: 120,
  diastolic: 80,
  steps: 5000,
  bloodSugar: 5.0
})

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchData = async () => {
  try {
    const params: any = { userId, page: currentPage.value, pageSize: pageSize.value }
    if (dateRange?.value && dateRange.value.length === 2) {
      const [start, end] = dateRange.value
      const toStr = (d: Date) => {
        const y = d.getFullYear()
        const m = String(d.getMonth() + 1).padStart(2, '0')
        const da = String(d.getDate()).padStart(2, '0')
        return `${y}-${m}-${da}`
      }
      params.startDate = toStr(start)
      params.endDate = toStr(end)
    }
    const res = await api.get('/health/list', { params })
    if (res.code === 200) {
      tableData.value = res.data
      total.value = res.total || 0
    }
  } catch (e) {
    // error
  }
}

onMounted(() => {
  if (userId) fetchData()
})

const handleDateChange = () => {
  currentPage.value = 1
  fetchData()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchData()
}

const handleSave = async () => {
  try {
    const date = new Date(form.date)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const dateStr = `${year}-${month}-${day}`

    const res = await api.post('/health/save', {
      id: form.id,
      userId,
      recordDate: dateStr,
      weight: form.weight,
      heartRate: form.heartRate,
      systolic: form.systolic,
      diastolic: form.diastolic,
      steps: form.steps,
      bloodSugar: form.bloodSugar
    })
    if (res.code === 200) {
      ElMessage.success('记录保存成功')
      dialogVisible.value = false
      fetchData()
    }
  } catch (e) {
    // error
  }
}

const handleEdit = (row: any) => {
  form.id = row.id
  form.date = new Date(row.recordDate)
  form.weight = row.weight
  form.heartRate = row.heartRate
  form.systolic = row.systolic
  form.diastolic = row.diastolic
  form.steps = row.steps
  form.bloodSugar = row.bloodSugar
  dialogVisible.value = true
}

const handleDelete = (row: any) => {
  ElMessageBox.confirm(
    '确定要删除这条记录吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {
      await api.delete(`/health/delete/${row.id}`)
      ElMessage.success('删除成功')
      fetchData()
    })
    .catch(() => {})
}
</script>

<style scoped lang="scss">
.health-data-container {
  .toolbar {
    display: flex;
    justify-content: space-between;
    margin-bottom: 20px;
  }
  
.table-card {
  border-radius: 8px;
}
  
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
const rowClass = ({ row }: any) => {
  return row.alarmStatus === 1 ? 'alarm-row' : ''
}
.alarm-row { background: #fde2e2 }
