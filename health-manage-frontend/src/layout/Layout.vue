<template>
  <div class="app-layout">
    <div class="sidebar" :class="{ collapsed: isCollapsed }">
      <div class="logo">
        <el-icon :size="32" color="#42b983"><FirstAidKit /></el-icon>
        <span v-if="!isCollapsed">健康管家</span>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical"
        :collapse="isCollapsed"
        router
        background-color="transparent"
        text-color="#333"
        active-text-color="#42b983"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        
        <el-menu-item index="/health-data">
          <el-icon><Document /></el-icon>
          <template #title>健康档案</template>
        </el-menu-item>
        
        <el-menu-item index="/check-in">
          <el-icon><Calendar /></el-icon>
          <template #title>每日打卡</template>
        </el-menu-item>
        
        <el-menu-item index="/diet">
          <el-icon><Food /></el-icon>
          <template #title>饮食管理</template>
        </el-menu-item>
      </el-menu>

      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon v-if="isCollapsed"><Expand /></el-icon>
        <el-icon v-else><Fold /></el-icon>
      </div>
    </div>

    <div class="main-container">
      <div class="header">
        <div class="header-left">
          <h2>{{ pageTitle }}</h2>
          <p class="subtitle">Keep Healthy, Keep Happy!</p>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="40" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">{{ username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      
      <div class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Odometer,
  Document,
  Calendar,
  Food,
  Expand,
  Fold,
  FirstAidKit
} from '@element-plus/icons-vue'
import api from '../utils/api'

const route = useRoute()
const router = useRouter()
const isCollapsed = ref(false)
const user = JSON.parse(localStorage.getItem('user') || '{}')
const username = user.nickname || user.username || 'User'

const activeMenu = computed(() => route.path)

const pageTitle = computed(() => {
  const map: Record<string, string> = {
    '/dashboard': '仪表盘',
    '/health-data': '健康档案',
    '/check-in': '每日打卡',
    '/diet': '饮食管理'
  }
  return map[route.path] || '健康管理系统'
})

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
}

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await api.post('/user/logout')
    } catch (e) {
      // ignore
    }
    localStorage.removeItem('user')
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.app-layout {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
  
  .sidebar {
    width: 240px;
    background-color: white;
    height: 96vh;
    margin: 2vh 0 2vh 20px;
    border-radius: 16px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    display: flex;
    flex-direction: column;
    transition: width 0.3s ease;
    position: relative;
    z-index: 10;
    
    &.collapsed {
      width: 80px;
      
      .logo span {
        display: none;
      }
    }
    
    .logo {
      height: 80px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      font-size: 20px;
      font-weight: bold;
      color: #333;
      border-bottom: 1px solid #f0f0f0;
    }
    
    .el-menu-vertical {
      border-right: none;
      flex: 1;
      padding: 20px 0;
      
      :deep(.el-menu-item) {
        height: 56px;
        margin: 4px 12px;
        border-radius: 8px;
        
        &.is-active {
          background-color: #e6f7f0 !important;
          font-weight: bold;
          
          .el-icon {
            animation: bounce 0.5s;
          }
        }
        
        &:hover {
          background-color: #f5f7fa;
        }
      }
    }
    
    .collapse-btn {
      height: 50px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: #909399;
      border-top: 1px solid #f0f0f0;
      
      &:hover {
        color: #42b983;
      }
    }
  }
  
  .main-container {
    flex: 1;
    display: flex;
    flex-direction: column;
    padding: 0 20px;
    overflow: hidden;
    
    .header {
      height: 80px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 10px;
      margin-top: 2vh;
      
      .header-left {
        h2 {
          margin: 0;
          font-size: 24px;
          color: #303133;
        }
        
        .subtitle {
          margin: 5px 0 0;
          font-size: 12px;
          color: #909399;
          font-family: 'Comic Sans MS', cursive, sans-serif;
        }
      }
      
      .user-info {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;
        padding: 5px 10px;
        border-radius: 20px;
        transition: background 0.3s;
        
        &:hover {
          background: white;
        }
        
        .username {
          font-weight: 500;
          color: #606266;
        }
      }
    }
    
    .content {
      flex: 1;
      overflow-y: auto;
      padding: 20px 10px;
      
      /* Custom Scrollbar */
      &::-webkit-scrollbar {
        width: 6px;
      }
      &::-webkit-scrollbar-thumb {
        background: #dcdfe6;
        border-radius: 3px;
      }
    }
  }
}

@keyframes bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
