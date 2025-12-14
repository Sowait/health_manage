<template>
  <div class="admin-users">
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="关键字" style="width:240px" />
      <el-button type="primary" @click="fetchUsers">搜索</el-button>
    </div>
    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination">
      <el-pagination background layout="prev, pager, next" :current-page="page" :page-size="pageSize" :total="total" @current-change="handlePage" />
    </div>

    <el-dialog v-model="editVisible" title="编辑用户" width="400px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="editForm.password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import api from '../../utils/api'
import { ElMessage } from 'element-plus'

const keyword = ref('')
const users = ref<any[]>([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const editVisible = ref(false)
const editForm = ref<any>({})

const fetchUsers = async () => {
  const res = await api.get('/admin/users', { params: { keyword: keyword.value, page: page.value, pageSize: pageSize.value } })
  if (res.code === 200) {
    users.value = res.data
    total.value = res.total || 0
  }
}
const handlePage = (p: number) => { page.value = p; fetchUsers() }
const openEdit = (row: any) => { editForm.value = { id: row.id, nickname: row.nickname, password: row.password }; editVisible.value = true }
const saveEdit = async () => {
  const res = await api.put(`/admin/users/${editForm.value.id}`, editForm.value)
  if (res.code === 200) { ElMessage.success('已保存'); editVisible.value=false; fetchUsers() }
}
const remove = async (row: any) => {
  const res = await api.delete(`/admin/users/${row.id}`)
  if (res.code === 200) { ElMessage.success('已删除'); fetchUsers() }
}
onMounted(() => fetchUsers())
</script>

<style scoped>
.toolbar { display:flex; gap:10px; margin-bottom:10px; }
.pagination { display:flex; justify-content:flex-end; margin-top:10px; }
</style>
