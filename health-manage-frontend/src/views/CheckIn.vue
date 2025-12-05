<template>
  <div class="check-in-container">
    <div class="header">
      <h2>每日打卡</h2>
      <el-button type="primary" circle @click="addTask">
        <el-icon><Plus /></el-icon>
      </el-button>
    </div>

    <div class="tasks-grid">
      <div v-if="tasks.length === 0" class="empty-tip">
        暂无打卡任务，点击右上角添加
      </div>
      <div
        v-for="task in tasks"
        :key="task.id"
        class="task-card"
        :class="{ completed: task.completed }"
        @click="toggleTask(task)"
      >
        <div class="icon-wrapper" :style="{ backgroundColor: task.color || '#409EFF' }">
          <el-icon size="24" color="white">
            <component :is="task.icon || 'Bicycle'" />
          </el-icon>
        </div>
        <div class="task-info">
          <h3>{{ task.name }}</h3>
          <p>{{ task.desc }}</p>
        </div>
        <div class="status-icon">
          <el-icon v-if="task.completed" color="#67C23A" size="24"><CircleCheckFilled /></el-icon>
          <el-icon v-else color="#E4E7ED" size="24"><CircleCheck /></el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../utils/api'
import {
  Sunrise,
  GobletFull,
  Moon,
  Bicycle,
  Apple,
  Plus,
  CircleCheck,
  CircleCheckFilled
} from '@element-plus/icons-vue'

const tasks = ref<any[]>([])
const userId = JSON.parse(localStorage.getItem('user') || '{}').id

const fetchTasks = async () => {
  try {
    const res = await api.get('/checkin/tasks', { params: { userId } })
    if (res.code === 200) {
      tasks.value = res.data
    }
  } catch (e) {
    // error
  }
}

onMounted(() => {
  if (userId) {
    fetchTasks()
  }
})

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
  }
}

const addTask = () => {
  ElMessageBox.prompt('请输入任务名称', '新增打卡任务', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  }).then(async ({ value }) => {
    if (value) {
      await api.post('/checkin/task/add', {
        userId,
        taskName: value,
        icon: 'Bicycle', // 默认图标
        color: '#409EFF', // 默认颜色
        targetDesc: '坚持打卡'
      })
      ElMessage.success('添加成功')
      fetchTasks()
    }
  }).catch(() => {})
}
</script>

<style scoped lang="scss">
.check-in-container {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    
    h2 {
      color: #333;
      margin: 0;
    }
  }

  .tasks-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 20px;
    
    .empty-tip {
      color: #909399;
      text-align: center;
      grid-column: 1 / -1;
      padding: 40px 0;
    }
  }

  .task-card {
    background: white;
    border-radius: 12px;
    padding: 20px;
    display: flex;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s ease;
    border: 1px solid #EBEEF5;
    position: relative;
    overflow: hidden;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 8px 16px rgba(0,0,0,0.1);
    }

    &.completed {
      background-color: #f0f9eb;
      border-color: #e1f3d8;
      
      .task-info h3 {
        color: #67C23A;
      }
    }

    .icon-wrapper {
      width: 50px;
      height: 50px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 15px;
    }

    .task-info {
      flex: 1;
      
      h3 {
        margin: 0 0 5px 0;
        font-size: 16px;
        color: #303133;
      }
      
      p {
        margin: 0;
        font-size: 13px;
        color: #909399;
      }
    }
  }
}
</style>