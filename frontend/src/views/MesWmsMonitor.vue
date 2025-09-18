<template>
  <div class="mes-wms-monitor">
    <div class="page-header">
      <h2>MES-WMS交互监控</h2>
      <div class="header-actions">
        <el-button type="primary" @click="initializeFlow">1. 初始化流程</el-button>
        <el-button type="success" @click="startMonitoring" :disabled="isMonitoring">2. 开始监控</el-button>
        <el-button type="warning" @click="stopMonitoring" :disabled="!isMonitoring">停止监控</el-button>
        <el-button type="info" @click="updateLocationStatus">更新库位状态</el-button>
        <el-button type="warning" @click="refreshStatus">刷新状态</el-button>
        <el-button type="danger" @click="forceResetAll">强制重置</el-button>
      </div>
    </div>
    
    <div class="monitor-content">
      <el-row :gutter="20">
        <!-- MES-WMS状态 -->
        <el-col :span="12">
          <el-card class="status-card">
            <template #header>
              <div class="card-header">
                <span>MES-WMS交互状态</span>
                <el-tag :type="status.connected ? 'success' : 'danger'">
                  {{ status.connected ? '已连接' : '未连接' }}
                </el-tag>
              </div>
            </template>
            
            <el-descriptions :column="1" border>
              <el-descriptions-item label="WMS模式">
                <el-tag :type="status.wmsMode === 1 ? 'warning' : 'info'">
                  {{ status.wmsMode === 1 ? '远程模式' : '本地模式' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="WMS是否在忙">
                <el-tag :type="status.wmsBusy === 1 ? 'warning' : 'success'">
                  {{ status.wmsBusy === 1 ? '忙' : '空闲' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="WMS出库中">
                <el-tag :type="status.wmsOutboundProgress === 1 ? 'warning' : 'info'">
                  {{ status.wmsOutboundProgress === 1 ? '出库中' : '空闲' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="WMS入库中">
                <el-tag :type="status.wmsInboundProgress === 1 ? 'warning' : 'info'">
                  {{ status.wmsInboundProgress === 1 ? '入库中' : '空闲' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="WMS出库完成">
                <el-tag :type="status.wmsOutboundComplete === 1 ? 'success' : 'info'">
                  {{ status.wmsOutboundComplete === 1 ? '完成' : '空闲' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="WMS入库完成">
                <el-tag :type="status.wmsInboundComplete === 1 ? 'success' : 'info'">
                  {{ status.wmsInboundComplete === 1 ? '完成' : '空闲' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        
        <!-- MES订单状态 -->
        <el-col :span="12">
          <el-card class="status-card">
            <template #header>
              <div class="card-header">
                <span>MES订单状态</span>
              </div>
            </template>
            
            <el-descriptions :column="1" border>
              <el-descriptions-item label="MES出库订单">
                <el-tag :type="status.mesOutboundOrder === 1 ? 'danger' : 'info'">
                  {{ status.mesOutboundOrder === 1 ? '有订单' : '无订单' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="MES入库订单">
                <el-tag :type="status.mesInboundOrder === 1 ? 'danger' : 'info'">
                  {{ status.mesInboundOrder === 1 ? '有订单' : '无订单' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
            
            <!-- 当前执行状态 -->
            <div v-if="status.wmsCurrentRow > 0 && status.wmsCurrentColumn > 0" class="execution-status">
              <el-alert
                :title="`正在执行操作 - 位置: ${status.wmsCurrentRow}行${status.wmsCurrentColumn}列`"
                type="warning"
                :closable="false"
                show-icon
              />
            </div>
            
            
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 库位状态 -->
      <el-card class="location-card" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>库位状态 (A库 4行6列)</span>
          </div>
        </template>
        
        <div class="location-grid">
          <div 
            v-for="row in 4" 
            :key="row" 
            class="location-row"
          >
            <div 
              v-for="col in 6" 
              :key="col" 
              class="location-cell"
              :class="getLocationClass(row, col)"
            >
              <div class="location-label">{{ row }}-{{ col }}</div>
              <div class="location-status">
                {{ getLocationStatus(row, col) }}
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 操作日志 -->
      <el-card class="log-card" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>操作日志</span>
            <div class="log-actions">
              <el-button size="small" @click="loadPersistentLogs">加载历史日志</el-button>
              <el-button size="small" @click="clearLogs">清空当前日志</el-button>
              <el-button size="small" @click="exportLogs">导出日志</el-button>
            </div>
          </div>
        </template>
        
        <div class="log-content">
          <div 
            v-for="(log, index) in operationLogs" 
            :key="index" 
            class="log-item"
            :class="getLogLevelClass(log.level)"
          >
            <span class="log-time">{{ formatLogTime(log.timestamp) }}</span>
            <span class="log-level">{{ log.level }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
          <div v-if="operationLogs.length === 0" class="no-logs">
            暂无操作日志
          </div>
        </div>
        
        <!-- 分页 -->
        <div class="log-pagination" v-if="totalLogs > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="totalLogs"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

export default {
  name: 'MesWmsMonitor',
  setup() {
    const status = ref({
      connected: false,
      wmsMode: 0,
      wmsBusy: 0,
      wmsOutboundProgress: 0,
      wmsInboundProgress: 0,
      wmsOutboundComplete: 0,
      wmsInboundComplete: 0,
      mesOutboundOrder: 0,
      mesInboundOrder: 0,
      wmsCurrentRow: 0,
      wmsCurrentColumn: 0,
      locationStatus: {}
    })
    
    const loading = ref(false)
    const isMonitoring = ref(false)
    const monitoringInterval = ref(null)
    const operationLogs = ref([])
    const currentPage = ref(1)
    const pageSize = ref(20)
    const totalLogs = ref(0)
    const persistentLogs = ref([])
    
    // 初始化MES-WMS交互
    const initializeMesWms = async () => {
      loading.value = true
      try {
        const response = await axios.post('/api/mes-wms/initialize')
        if (response.data.success) {
          ElMessage.success('MES-WMS交互初始化成功')
          await refreshStatus()
        } else {
          ElMessage.error(response.data.message || '初始化失败')
        }
      } catch (error) {
        ElMessage.error('初始化失败: ' + error.message)
        console.error(error)
      } finally {
        loading.value = false
      }
    }
    
    // 更新库位状态
    const updateLocationStatus = async () => {
      loading.value = true
      try {
        const response = await axios.post('/api/mes-wms/update-location-status')
        if (response.data.success) {
          ElMessage.success('库位状态更新成功')
          await refreshStatus()
        } else {
          ElMessage.error(response.data.message || '更新失败')
        }
      } catch (error) {
        ElMessage.error('更新库位状态失败: ' + error.message)
        console.error(error)
      } finally {
        loading.value = false
      }
    }
    
    // 检查订单
    const checkOrders = async () => {
      loading.value = true
      try {
        const response = await axios.get('/api/mes-wms/check-orders')
        if (response.data.success) {
          ElMessage.success(`检查完成 - 出库订单: ${response.data.hasOutboundOrder ? '有' : '无'}, 入库订单: ${response.data.hasInboundOrder ? '有' : '无'}`)
          await refreshStatus()
        } else {
          ElMessage.error(response.data.message || '检查订单失败')
        }
      } catch (error) {
        ElMessage.error('检查订单失败: ' + error.message)
        console.error(error)
      } finally {
        loading.value = false
      }
    }
    
    
    // 刷新状态
    const refreshStatus = async () => {
      try {
        const response = await axios.get('/api/mes-wms/status')
        status.value = response.data
        
        // 如果MES-WMS显示未连接，但localStorage中有连接状态，尝试恢复
        if (!status.value.connected) {
          const savedStatus = localStorage.getItem('mesWmsConnectionStatus')
          if (savedStatus) {
            try {
              const parsed = JSON.parse(savedStatus)
              if (parsed.connected) {
                console.log('检测到localStorage中的连接状态，尝试恢复显示')
                status.value.connected = true
                status.value.message = '连接状态已从缓存恢复'
              }
            } catch (e) {
              console.error('解析localStorage状态失败:', e)
            }
          }
        }
        
        console.log('MES-WMS状态已刷新:', status.value)
      } catch (error) {
        // 如果请求失败，尝试从localStorage恢复状态
        const savedStatus = localStorage.getItem('mesWmsConnectionStatus')
        if (savedStatus) {
          try {
            const parsed = JSON.parse(savedStatus)
            if (parsed.connected) {
              console.log('网络请求失败，从localStorage恢复连接状态')
              status.value.connected = true
              status.value.message = '连接状态已从缓存恢复（网络异常）'
              return
            }
          } catch (e) {
            console.error('解析localStorage状态失败:', e)
          }
        }
        
        ElMessage.error('刷新状态失败: ' + error.message)
        console.error(error)
      }
    }
    
    // 强制重置所有状态
    const forceResetAll = async () => {
      loading.value = true
      try {
        addLog('INFO', '开始强制重置所有状态...')
        const response = await axios.post('/api/mes-wms/reset-all')
        
        if (response.data.success) {
          addLog('SUCCESS', response.data.message)
          ElMessage.success('强制重置成功')
          await refreshStatus()
        } else {
          addLog('ERROR', response.data.message)
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        addLog('ERROR', '强制重置失败: ' + error.message)
        ElMessage.error('强制重置失败: ' + error.message)
      } finally {
        loading.value = false
      }
    }
    
    // 获取库位样式类
    const getLocationClass = (row, col) => {
      const locationKey = `location${(row - 1) * 6 + col}`
      const hasPallet = status.value.locationStatus[locationKey] === 1
      const isExecuting = status.value.wmsCurrentRow === row && status.value.wmsCurrentColumn === col
      
      return {
        'has-pallet': hasPallet,
        'empty': !hasPallet,
        'executing': isExecuting
      }
    }
    
    // 获取库位状态文本
    const getLocationStatus = (row, col) => {
      const locationKey = `location${(row - 1) * 6 + col}`
      const hasPallet = status.value.locationStatus[locationKey] === 1
      const isExecuting = status.value.wmsCurrentRow === row && status.value.wmsCurrentColumn === col
      
      if (isExecuting) {
        return '执行中'
      }
      return hasPallet ? '有料' : '空'
    }
    
    // 添加操作日志
    const addLog = (level, message) => {
      const timestamp = new Date().toLocaleTimeString()
      operationLogs.value.unshift({
        timestamp,
        level,
        message
      })
      
      // 限制日志数量
      if (operationLogs.value.length > 100) {
        operationLogs.value = operationLogs.value.slice(0, 100)
      }
    }
    
    // 清空日志
    const clearLogs = () => {
      operationLogs.value = []
    }
    
    // 获取日志级别样式
    const getLogLevelClass = (level) => {
      return {
        'log-info': level === 'INFO',
        'log-success': level === 'SUCCESS',
        'log-warning': level === 'WARNING',
        'log-error': level === 'ERROR'
      }
    }
    
    // 初始化MES-WMS流程
    const initializeFlow = async () => {
      loading.value = true
      try {
        addLog('INFO', '开始初始化MES-WMS流程...')
        const response = await axios.post('/api/mes-wms-flow/initialize')
        
        if (response.data.success) {
          addLog('SUCCESS', response.data.message)
          ElMessage.success('MES-WMS流程初始化成功')
          await refreshStatus()
        } else {
          addLog('ERROR', response.data.message)
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        addLog('ERROR', '初始化失败: ' + error.message)
        ElMessage.error('初始化失败: ' + error.message)
      } finally {
        loading.value = false
      }
    }
    
    // 开始监控MES订单
    const startMonitoring = () => {
      if (isMonitoring.value) {
        ElMessage.warning('监控已在运行中')
        return
      }
      
      addLog('INFO', '开始监控MES订单...')
      ElMessage.success('开始监控MES订单')
      
      isMonitoring.value = true
      monitoringInterval.value = setInterval(async () => {
        try {
          const response = await axios.post('/api/mes-wms-flow/check-and-execute')
          
          if (response.data.success) {
            if (response.data.hasOrder !== false) {
              addLog('SUCCESS', response.data.message)
              if (response.data.operation) {
                addLog('INFO', `执行${response.data.operation === 'inbound' ? '入库' : '出库'}操作 - 位置: ${response.data.row}行${response.data.column}列`)
              }
            }
          } else {
            addLog('ERROR', response.data.message)
          }
          
          await refreshStatus()
        } catch (error) {
          addLog('ERROR', '监控出错: ' + error.message)
        }
      }, 5000) // 每5秒检查一次，避免重复处理
    }
    
    // 停止监控
    const stopMonitoring = () => {
      if (monitoringInterval.value) {
        clearInterval(monitoringInterval.value)
        monitoringInterval.value = null
        isMonitoring.value = false
        addLog('INFO', '停止监控MES订单')
        ElMessage.info('已停止监控')
      }
    }
    
    // 格式化日志时间
    const formatLogTime = (timestamp) => {
      if (typeof timestamp === 'string') {
        return timestamp
      }
      if (timestamp && timestamp.timestamp) {
        return timestamp.timestamp
      }
      return new Date().toLocaleTimeString()
    }
    
    // 加载持久化日志
    const loadPersistentLogs = async () => {
      try {
        const response = await axios.get('/api/logs/mes-wms', {
          params: {
            page: currentPage.value - 1,
            size: pageSize.value
          }
        })
        
        if (response.data && response.data.content) {
          persistentLogs.value = response.data.content
          totalLogs.value = response.data.totalElements
          
          // 合并到当前日志显示
          operationLogs.value = [...persistentLogs.value, ...operationLogs.value]
          
          ElMessage.success('历史日志加载成功')
        }
      } catch (error) {
        ElMessage.error('加载历史日志失败: ' + error.message)
      }
    }
    
    
    // 导出日志
    const exportLogs = () => {
      try {
        const logs = operationLogs.value.map(log => ({
          时间: formatLogTime(log.timestamp),
          级别: log.level,
          消息: log.message,
          模块: log.module || 'MES-WMS',
          操作: log.operation || ''
        }))
        
        const csvContent = [
          '时间,级别,消息,模块,操作',
          ...logs.map(log => `"${log.时间}","${log.级别}","${log.消息}","${log.模块}","${log.操作}"`)
        ].join('\n')
        
        const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' })
        const link = document.createElement('a')
        const url = URL.createObjectURL(blob)
        link.setAttribute('href', url)
        link.setAttribute('download', `MES-WMS日志_${new Date().toISOString().slice(0, 10)}.csv`)
        link.style.visibility = 'hidden'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        
        ElMessage.success('日志导出成功')
      } catch (error) {
        ElMessage.error('导出日志失败: ' + error.message)
      }
    }
    
    // 分页处理
    const handleSizeChange = (val) => {
      pageSize.value = val
      currentPage.value = 1
      loadPersistentLogs()
    }
    
    const handleCurrentChange = (val) => {
      currentPage.value = val
      loadPersistentLogs()
    }
    
    
    // 从localStorage恢复连接状态
    const restoreConnectionState = () => {
      try {
        const savedStatus = localStorage.getItem('mesWmsConnectionStatus')
        if (savedStatus) {
          const parsed = JSON.parse(savedStatus)
          if (parsed.connected) {
            console.log('从localStorage恢复MES-WMS连接状态')
            status.value.connected = true
            status.value.message = parsed.message || '连接状态已恢复'
          }
        }
      } catch (error) {
        console.error('恢复连接状态失败:', error)
      }
    }

    onMounted(() => {
      // 先尝试从localStorage恢复连接状态
      restoreConnectionState()
      
      // 然后刷新实际状态
      refreshStatus()
      
      // 每5秒自动刷新状态
      setInterval(refreshStatus, 5000)
    })
    
    // 组件卸载时清理定时器
    const onUnmounted = () => {
      if (monitoringInterval.value) {
        clearInterval(monitoringInterval.value)
      }
    }
    
    return {
      status,
      loading,
      isMonitoring,
      operationLogs,
      currentPage,
      pageSize,
      totalLogs,
      initializeMesWms,
      initializeFlow,
      startMonitoring,
      stopMonitoring,
      updateLocationStatus,
      checkOrders,
      refreshStatus,
      forceResetAll,
      getLocationClass,
      getLocationStatus,
      addLog,
      clearLogs,
      getLogLevelClass,
      formatLogTime,
      loadPersistentLogs,
      exportLogs,
      handleSizeChange,
      handleCurrentChange,
      onUnmounted
    }
  }
}
</script>

<style scoped>
.mes-wms-monitor {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0;
  color: #2c3e50;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.status-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #2c3e50;
}

.log-actions {
  display: flex;
  gap: 8px;
}

.location-card {
  margin-top: 20px;
}

.location-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.location-row {
  display: flex;
  gap: 10px;
}

.location-cell {
  width: 80px;
  height: 60px;
  border: 2px solid #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.location-cell.has-pallet {
  background-color: #f0f9ff;
  border-color: #0ea5e9;
  color: #0ea5e9;
}

.location-cell.empty {
  background-color: #f8fafc;
  border-color: #cbd5e1;
  color: #64748b;
}

.location-cell.executing {
  background-color: #fef3c7;
  border-color: #f59e0b;
  color: #92400e;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.location-label {
  font-weight: bold;
  font-size: 12px;
}

.location-status {
  font-size: 10px;
  margin-top: 2px;
}

.execution-status {
  margin: 15px 0;
}



.log-card {
  margin-top: 20px;
}

.log-content {
  max-height: 300px;
  overflow-y: auto;
}

.log-item {
  display: flex;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.log-time {
  width: 80px;
  color: #999;
  margin-right: 10px;
}

.log-level {
  width: 60px;
  margin-right: 10px;
  font-weight: bold;
}

.log-info .log-level {
  color: #409eff;
}

.log-success .log-level {
  color: #67c23a;
}

.log-warning .log-level {
  color: #e6a23c;
}

.log-error .log-level {
  color: #f56c6c;
}

.log-message {
  flex: 1;
}

.no-logs {
  text-align: center;
  color: #999;
  padding: 20px;
}


.log-pagination {
  margin-top: 15px;
  display: flex;
  justify-content: center;
}
</style>
