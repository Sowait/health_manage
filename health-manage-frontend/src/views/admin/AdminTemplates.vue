<template>
  <div class="admin-templates">
    <el-card shadow="always" class="card">
      <template #header>任务模板</template>
      <div class="toolbar">
        <el-button type="primary" @click="addTaskTemplate">新增模板</el-button>
        <el-button @click="applyAll">应用到用户</el-button>
      </div>
      <el-table :data="taskTemplates" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskName" label="任务名称" />
        <el-table-column prop="targetDesc" label="目标" />
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="editTask(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTask(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const taskTemplates = ref<any[]>([])

const fetchAll = async () => {
  const t = await api.get('/admin/task-templates')
  if (t.code === 200) taskTemplates.value = t.data
}

const addTaskTemplate = async () => {
  ElMessageBox.prompt('模板任务名称', '新增模板').then(async ({ value }) => {
    if (value) {
      const res = await api.post('/admin/task-templates', { taskName: value, icon: 'Bicycle', color: '#409EFF', targetDesc: '坚持打卡' })
      if (res.code === 200) { ElMessage.success('已添加'); fetchAll() }
    }
  }).catch(() => {})
}
const editTask = async (row: any) => {
  ElMessageBox.prompt('编辑模板任务名称', '编辑模板', { inputValue: row.taskName }).then(async ({ value }) => {
    if (value) {
      const res = await api.put(`/admin/task-templates/${row.id}`, { ...row, taskName: value })
      if (res.code === 200) { ElMessage.success('已保存'); fetchAll() }
    }
  }).catch(() => {})
}
const deleteTask = async (row: any) => {
  const res = await api.delete(`/admin/task-templates/${row.id}`)
  if (res.code === 200) { ElMessage.success('已删除'); fetchAll() }
}
const applyAll = async () => {
  const res = await api.post('/admin/task-templates/apply')
  if (res.code === 200) ElMessage.success('已应用到所有用户')
}


onMounted(() => fetchAll())
</script>

<style scoped>
.card { border-radius: 12px; }
.toolbar { display:flex; gap:10px; margin-bottom:10px; }
</style>
