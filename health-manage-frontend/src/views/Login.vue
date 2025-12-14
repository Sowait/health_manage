<template>
  <div class="login-container">
    <div class="left-panel">
      <div class="content">
        <el-icon class="logo-icon" :size="60"><FirstAidKit /></el-icon>
        <h1>管理健康，掌控人生</h1>
        <p>您的个人智能健康管家</p>
        <img src="https://img.freepik.com/free-vector/healthy-lifestyle-concept-illustration_114360-5233.jpg" alt="Health Illustration" class="illustration" />
      </div>
    </div>
    <div class="right-panel">
      <div class="form-box">
        <transition name="fade" mode="out-in">
          <!-- 登录表单 -->
          <div v-if="!isRegister" key="login">
            <h2>欢迎回来</h2>
            <p class="subtitle">请登录您的账号</p>
            
            <el-form :model="loginForm" class="login-form" label-position="top">
              <el-form-item label="用户名">
                <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large">
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item label="密码">
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password size="large">
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <div class="actions">
                <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
              </div>
              
              <el-button type="primary" class="submit-btn" size="large" @click="handleLogin">登录</el-button>
              
              <div class="switch-mode">
                还没有账号？ <el-link type="primary" @click="toggleMode">立即注册</el-link>
              </div>
            </el-form>
          </div>

          <!-- 注册表单 -->
          <div v-else key="register">
            <h2>创建账号</h2>
            <p class="subtitle">开启您的健康之旅</p>
            
            <el-form :model="registerForm" class="login-form" label-position="top">
              <el-form-item label="用户名">
                <el-input v-model="registerForm.username" placeholder="设置用户名" size="large">
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="昵称">
                <el-input v-model="registerForm.nickname" placeholder="您的称呼" size="large">
                  <template #prefix>
                    <el-icon><Edit /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-form-item label="密码">
                <el-input v-model="registerForm.password" type="password" placeholder="设置密码" show-password size="large">
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>

              <el-form-item label="确认密码">
                <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" show-password size="large">
                  <template #prefix>
                    <el-icon><Lock /></el-icon>
                  </template>
                </el-input>
              </el-form-item>
              
              <el-button type="primary" class="submit-btn" size="large" @click="handleRegister" style="margin-top: 20px">注册</el-button>
              
              <div class="switch-mode">
                已有账号？ <el-link type="primary" @click="toggleMode">返回登录</el-link>
              </div>
            </el-form>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Edit } from '@element-plus/icons-vue'
import api from '../utils/api'

const router = useRouter()
const isRegister = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const toggleMode = () => {
  isRegister.value = !isRegister.value
}

const handleLogin = async () => {
  if (loginForm.username && loginForm.password) {
    try {
      const res = await api.post('/user/login', {
        username: loginForm.username,
        password: loginForm.password
      })
      if (res.code === 200) {
        ElMessage.success('登录成功')
        localStorage.setItem('user', JSON.stringify(res.data))
        router.push('/dashboard')
      }
    } catch (e) {
      // error handled by interceptor
    }
  } else {
    ElMessage.warning('请输入用户名和密码')
  }
}

const handleRegister = async () => {
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  
  try {
    const res = await api.post('/user/register', {
      username: registerForm.username,
      password: registerForm.password,
      nickname: registerForm.nickname
    })
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      isRegister.value = false
    }
  } catch (e) {
    // error
  }
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.left-panel {
  flex: 1.5;
  background: linear-gradient(135deg, #42b983 0%, #35495e 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  position: relative;
  
  .content {
    text-align: center;
    z-index: 2;
    
    h1 {
      font-size: 2.5rem;
      margin-bottom: 10px;
    }
    
    p {
      font-size: 1.2rem;
      opacity: 0.9;
      margin-bottom: 40px;
    }
    
    .logo-icon {
      margin-bottom: 20px;
    }
    
    .illustration {
      max-width: 80%;
      border-radius: 20px;
      box-shadow: 0 20px 40px rgba(0,0,0,0.2);
    }
  }
}

.right-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: white;
  
  .form-box {
    width: 80%;
    max-width: 400px;
    
    h2 {
      font-size: 2rem;
      color: #333;
      margin-bottom: 10px;
    }
    
    .subtitle {
      color: #888;
      margin-bottom: 40px;
    }
    
    .actions {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 30px;
    }
    
    .submit-btn {
      width: 100%;
      font-weight: bold;
      margin-bottom: 20px;
      background: linear-gradient(90deg, #42b983, #35495e);
      border: none;
      
      &:hover {
        opacity: 0.9;
      }
    }
    
    .switch-mode {
      text-align: center;
      font-size: 14px;
      color: #666;
    }
  }
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
