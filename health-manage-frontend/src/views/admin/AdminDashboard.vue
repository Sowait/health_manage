<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card shadow="always">
          <template #header>总览</template>
          <div ref="pie1Ref" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card shadow="always">
          <template #header>今日概览</template>
          <div ref="pie2Ref" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="24">
        <el-card shadow="always">
          <template #header>健康档案状态</template>
          <div ref="pie3Ref" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import api from '../../utils/api'

const pie1Ref = ref<HTMLDivElement | null>(null)
const pie2Ref = ref<HTMLDivElement | null>(null)
const pie3Ref = ref<HTMLDivElement | null>(null)
let c1: echarts.ECharts | null = null
let c2: echarts.ECharts | null = null
let c3: echarts.ECharts | null = null

const render = (stats: any) => {
  const t = stats.totals
  const td = stats.today
  const ha = stats.healthAlert
  if (pie1Ref.value && !c1) c1 = echarts.init(pie1Ref.value)
  if (pie2Ref.value && !c2) c2 = echarts.init(pie2Ref.value)
  if (pie3Ref.value && !c3) c3 = echarts.init(pie3Ref.value)
  c1?.setOption({ tooltip:{}, series:[{ type:'pie', radius:'70%', label:{ formatter:'{b}: {c}' }, data:[
    { name:'总用户数', value:t.users }, { name:'总打卡数', value:t.checkins }, { name:'总饮食记录', value:t.diets }, { name:'总健康档案数', value:t.healthRecords }
  ]}] })
  c2?.setOption({ tooltip:{}, series:[{ type:'pie', radius:'70%', label:{ formatter:'{b}: {c}' }, data:[
    { name:'今日注册数', value:td.registered }, { name:'今日打卡数', value:td.checkins }, { name:'今日饮食记录', value:td.diets }, { name:'今日健康档案数', value:td.healthRecords }
  ]}] })
  c3?.setOption({ tooltip:{}, series:[{ type:'pie', radius:'70%', label:{ formatter:'{b}: {c}' }, data:[
    { name:'总健康档案数', value:ha.total }, { name:'异常档案数', value:ha.abnormal }, { name:'已报警档案数', value:ha.alarmed }
  ]}] })
}

const fetchStats = async () => {
  const res = await api.get('/admin/stats')
  if (res.code === 200) render(res.data)
}

onMounted(() => fetchStats())
onUnmounted(() => { c1?.dispose(); c2?.dispose(); c3?.dispose() })
</script>

<style scoped>
.chart { height: 320px }
</style>
