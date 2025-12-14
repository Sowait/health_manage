<template>
  <div class="diet-container">
    <div class="summary-panel">
      <div class="date-picker">
        <el-date-picker
          v-model="currentDate"
          type="date"
          placeholder="选择日期"
          size="large"
          :clearable="false"
        />
      </div>
      <div class="calorie-stats">
        <el-progress 
          type="dashboard" 
          :percentage="percentage" 
          :color="progressColor"
        >
          <template #default>
            <div class="progress-content">
              <span class="label">今日摄入</span>
              <span class="value">{{ totalCalories }}</span>
              <span class="unit">kcal</span>
            </div>
          </template>
        </el-progress>
        <div class="target-info">
          <div class="item">
            <span>目标</span>
            <strong>{{ targetCalories }}</strong>
          </div>
          <div class="item">
            <span>剩余</span>
            <strong :class="{ warning: remainingCalories < 0 }">{{ remainingCalories }}</strong>
          </div>
        </div>
      </div>
    </div>

    <div class="meals-grid">
      <el-card 
        v-for="(meal, type) in meals" 
        :key="type" 
        class="meal-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <div class="header-left">
              <div class="icon-wrapper" :class="type">
                <el-icon size="20" color="white">
                  <component :is="getMealIcon(type)" />
                </el-icon>
              </div>
              <span>{{ getMealName(type) }}</span>
            </div>
            <div class="header-right">
              <span class="meal-calories">{{ getMealCalories(meal) }} kcal</span>
              <el-button circle size="small" type="primary" plain @click="openAddFood(type)">
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </div>
        </template>
        
        <div v-if="meal.length === 0" class="empty-state">
          暂无记录
        </div>
        <div v-else class="food-list">
          <div v-for="(item, index) in meal" :key="index" class="food-item">
            <div class="food-info">
              <span class="name">{{ item.foodName }}</span>
              <span class="amount">{{ item.amount }}</span>
            </div>
            <div class="food-cal">
              {{ item.calories }}
              <el-icon class="delete-btn" @click="removeFood(type, index)"><Delete /></el-icon>
            </div>
          </div>
        </div>
      </el-card>
    </div>

  <el-drawer
      v-model="drawerVisible"
      :title="'添加' + getMealName(currentMealType)"
      direction="rtl"
      size="400px"
    >
      <div class="food-selector">
        <el-input
          v-model="searchQuery"
          placeholder="搜索食物..."
          prefix-icon="Search"
          clearable
          class="search-input"
        />
        
        <el-tabs v-model="activeCategory" class="food-tabs">
          <el-tab-pane label="常见" name="common">
            <div class="food-options">
              <div 
                v-for="food in filteredFoods" 
                :key="food.id" 
                class="food-option"
                @click="addFood(food)"
              >
                <div class="option-info">
                  <span class="name">{{ food.name }}</span>
                  <span class="unit">{{ food.unit }}</span>
                </div>
                <span class="cal">{{ food.calories }} kcal</span>
                <el-icon color="#42b983"><CirclePlus /></el-icon>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
  </el-drawer>

  <el-card shadow="never" class="history-card" style="margin-top: 20px">
    <template #header>
      <div class="card-header">
        <span>历史饮食记录</span>
        <div style="display:flex;gap:10px;align-items:center">
          <el-date-picker v-model="historyRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" size="small" />
          <el-button size="small" @click="fetchHistory">查询</el-button>
        </div>
      </div>
    </template>
    <el-table :data="historyData" stripe size="large">
      <el-table-column prop="recordDate" label="日期" width="160" />
      <el-table-column prop="mealType" label="餐次" width="120" />
      <el-table-column prop="foodName" label="食物" />
      <el-table-column prop="calories" label="卡路里" width="120" />
    </el-table>
    <div class="pagination">
      <el-pagination background layout="prev, pager, next" :current-page="historyPage" :page-size="historyPageSize" :total="historyTotal" @current-change="handleHistoryPage" />
    </div>
  </el-card>
</div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../utils/api'
import { Sunrise, Sunny, Moon, Grape, Search, CirclePlus, Delete, Plus } from '@element-plus/icons-vue'

const currentDate = ref(new Date())
const targetCalories = 2000
const drawerVisible = ref(false)
const currentMealType = ref('breakfast')
const searchQuery = ref('')
const activeCategory = ref('common')
const userId = JSON.parse(localStorage.getItem('user') || '{}').id
const historyRange = ref<[Date, Date] | null>(null)
const historyData = ref<any[]>([])
const historyPage = ref(1)
const historyPageSize = ref(10)
const historyTotal = ref(0)

// Mock Data for Selector
const foodDatabase = [
  { id: 1, name: '米饭', unit: '1碗 (150g)', calories: 174, category: 'common' },
  { id: 2, name: '煮鸡蛋', unit: '1个', calories: 70, category: 'common' },
  { id: 3, name: '牛奶', unit: '1盒 (250ml)', calories: 135, category: 'common' },
  { id: 4, name: '苹果', unit: '1个', calories: 95, category: 'common' },
  { id: 5, name: '鸡胸肉', unit: '100g', calories: 165, category: 'common' },
  { id: 6, name: '全麦面包', unit: '1片', calories: 75, category: 'common' },
  { id: 7, name: '香蕉', unit: '1根', calories: 105, category: 'common' }
]

const meals = ref<any>({
  breakfast: [],
  lunch: [],
  dinner: [],
  snack: []
})

const formatDate = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`

const fetchDiet = async () => {
  try {
    const dateStr = formatDate(currentDate.value)
    const res = await api.get('/diet/list', {
      params: { userId, date: dateStr }
    })
    if (res.code === 200) {
      meals.value = res.data
    }
  } catch (e) {
    // error
  }
}

watch(currentDate, () => {
  if (userId) fetchDiet()
})

onMounted(() => {
  if (userId) fetchDiet()
})

// Computed
const totalCalories = computed(() => {
  let total = 0
  Object.values(meals.value).forEach((meal: any) => {
    meal.forEach((item: any) => total += item.calories)
  })
  return total
})

const remainingCalories = computed(() => targetCalories - totalCalories.value)
const percentage = computed(() => Math.min((totalCalories.value / targetCalories) * 100, 100))

const progressColor = computed(() => {
  if (percentage.value < 50) return '#67C23A'
  if (percentage.value < 80) return '#E6A23C'
  return '#F56C6C'
})

const filteredFoods = computed(() => {
  return foodDatabase.filter(food => 
    food.name.includes(searchQuery.value) && 
    (activeCategory.value === 'common' ? true : food.category === activeCategory.value)
  )
})

// Methods
const getMealName = (type: string) => {
  const map: any = { breakfast: '早餐', lunch: '午餐', dinner: '晚餐', snack: '加餐' }
  return map[type]
}

const getMealIcon = (type: string) => {
  const map: any = { breakfast: Sunrise, lunch: Sunny, dinner: Moon, snack: Grape }
  return map[type]
}

const getMealCalories = (meal: any[]) => {
  return meal.reduce((sum, item) => sum + item.calories, 0)
}

const openAddFood = (type: string) => {
  currentMealType.value = type
  drawerVisible.value = true
}

const addFood = async (food: any) => {
  try {
    const dateStr = formatDate(currentDate.value)
    const res = await api.post('/diet/add', {
      userId,
      mealType: currentMealType.value,
      foodName: food.name,
      calories: food.calories,
      amount: food.unit,
      recordDate: dateStr
    })
    if (res.code === 200) {
      ElMessage.success(`已添加 ${food.name}`)
      // Add to local list directly to avoid refresh flicker
      meals.value[currentMealType.value].push(res.data)
    }
  } catch (e) {
    // error
  }
}

const removeFood = async (type: string, index: number) => {
  const item = meals.value[type][index]
  try {
    await api.delete(`/diet/delete/${item.id}`)
    meals.value[type].splice(index, 1)
    ElMessage.success('删除成功')
  } catch (e) {
    // error
  }
}

const fetchHistory = async () => {
  try {
    const params: any = { userId, page: historyPage.value, pageSize: historyPageSize.value }
    if (historyRange?.value && historyRange.value.length === 2) {
      const toStr = (d: Date) => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
      params.startDate = toStr(historyRange.value[0])
      params.endDate = toStr(historyRange.value[1])
    }
    const res = await api.get('/diet/history', { params })
    if (res.code === 200) {
      historyData.value = res.data
      historyTotal.value = res.total || 0
    }
  } catch (e) {}
}

const handleHistoryPage = (p: number) => {
  historyPage.value = p
  fetchHistory()
}

onMounted(() => {
  if (userId) fetchHistory()
})
</script>

<style scoped lang="scss">
.diet-container {
  .summary-panel {
    background: white;
    padding: 20px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: space-around;
    margin-bottom: 20px;
    box-shadow: 0 2px 12px rgba(0,0,0,0.05);
    
    .calorie-stats {
      display: flex;
      align-items: center;
      gap: 40px;
      
      .progress-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        
        .label { font-size: 12px; color: #909399; }
        .value { font-size: 24px; font-weight: bold; color: #303133; }
        .unit { font-size: 12px; color: #909399; }
      }
      
      .target-info {
        display: flex;
        flex-direction: column;
        gap: 15px;
        
        .item {
          display: flex;
          flex-direction: column;
          span { font-size: 12px; color: #909399; }
          strong { font-size: 18px; color: #303133; }
          
          .warning { color: #F56C6C; }
        }
      }
    }
  }

  .meals-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
    
    .meal-card {
      border-radius: 8px;
      
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .header-left {
          display: flex;
          align-items: center;
          gap: 10px;
          
          .icon-wrapper {
            width: 32px;
            height: 32px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            
            &.breakfast { background: #E6A23C; }
            &.lunch { background: #F56C6C; }
            &.dinner { background: #409EFF; }
            &.snack { background: #67C23A; }
          }
          
          span { font-weight: bold; color: #303133; }
        }
        
        .header-right {
          display: flex;
          align-items: center;
          gap: 10px;
          
          .meal-calories { font-size: 13px; color: #909399; }
        }
      }
      
      .empty-state {
        text-align: center;
        color: #C0C4CC;
        padding: 20px 0;
        font-size: 13px;
      }
      
      .food-list {
        .food-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 10px 0;
          border-bottom: 1px solid #f0f0f0;
          
          &:last-child { border-bottom: none; }
          
          .food-info {
            display: flex;
            flex-direction: column;
            .name { font-size: 14px; color: #606266; }
            .amount { font-size: 12px; color: #909399; }
          }
          
          .food-cal {
            display: flex;
            align-items: center;
            gap: 10px;
            font-size: 14px;
            color: #303133;
            
            .delete-btn {
              cursor: pointer;
              color: #C0C4CC;
              &:hover { color: #F56C6C; }
            }
          }
        }
      }
    }
  }
  
.food-selector {
    .search-input { margin-bottom: 15px; }
    
    .food-options {
      display: flex;
      flex-direction: column;
      gap: 10px;
      
      .food-option {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px;
        border: 1px solid #EBEEF5;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.2s;
        
        &:hover {
          background-color: #f0f9eb;
          border-color: #c2e7b0;
        }
        
        .option-info {
          display: flex;
          flex-direction: column;
          .name { font-size: 14px; font-weight: 500; }
          .unit { font-size: 12px; color: #909399; }
        }
        
        .cal { font-size: 13px; color: #606266; }
      }
    }
  }
}
.history-card { border-radius: 12px; }
.pagination { display:flex; justify-content:flex-end; margin-top:10px; }
</style>
