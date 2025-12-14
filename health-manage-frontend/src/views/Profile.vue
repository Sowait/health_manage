<template>
  <div class="profile-container">
    <el-card shadow="always" class="card">
      <h2>个人中心</h2>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ user.role || 'USER' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
const user = JSON.parse(localStorage.getItem('user') || '{}')
</script>

<style scoped lang="scss">
.profile-container {
  .card {
    border-radius: 12px;
  }
}
</style>
