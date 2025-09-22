<template>
  <div class="stacker-monitor">
    <el-card class="monitor-card">
      <template #header>
        <div class="card-header">
          <span>WMS-堆垛机监控</span>
          <div class="header-actions">
            <el-button type="primary" @click="initializeFlow" :loading="initializing">
              初始化流程
            </el-button>
            <el-button type="warning" @click="forceResetAll" :loading="resetting">
              强制重置
            </el-button>
            <el-button type="info" @click="checkCompletion">
              检查完成状态
            </el-button>
            <el-button type="danger" @click="clearAllStatus" :loading="clearing">
              清理所有状态
            </el-button>
          </div>
        </div>
      </template>

      <!-- 连接状态 -->
      <div class="status-section">
        <h3>连接状态</h3>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">Modbus连接:</span>
                <el-tag v-if="loading" type="info">检查中...</el-tag>
                <el-tag v-else :type="status.connected ? 'success' : 'danger'">
                  {{ status.connected ? '已连接' : '未连接' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">系统状态:</span>
                <el-tag :type="status.systemBusy ? 'warning' : 'success'">
                  {{ status.systemBusy ? '忙碌' : '空闲' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">订单状态:</span>
                <el-tag :type="status.hasOrder ? 'primary' : 'info'">
                  {{ status.hasOrder ? '有订单' : '无订单' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- MES-WMS状态 -->
      <div class="status-section">
        <h3>MES-WMS状态</h3>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">WMS状态:</span>
                <el-tag :type="status.wmsBusy ? 'warning' : 'success'">
                  {{ status.wmsBusy ? '忙碌' : '空闲' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">出库进度:</span>
                <el-tag :type="status.wmsOutboundProgress ? 'primary' : 'info'">
                  {{ status.wmsOutboundProgress ? '进行中' : '无' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">入库进度:</span>
                <el-tag :type="status.wmsInboundProgress ? 'primary' : 'info'">
                  {{ status.wmsInboundProgress ? '进行中' : '无' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">当前位置:</span>
                <span class="status-value">{{ status.wmsCurrentRow || 0 }}行{{ status.wmsCurrentColumn || 0 }}列</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 堆垛机状态 -->
      <div class="status-section">
        <h3>堆垛机状态</h3>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">堆垛机状态:</span>
                <el-tag :type="getStackerStatusType(status.stackerStatus)">
                  {{ getStackerStatusText(status.stackerStatus) }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">操作类型:</span>
                <el-tag :type="getOperationType(status.stackerOperation)">
                  {{ getOperationText(status.stackerOperation) }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">当前位置:</span>
                <span class="status-value">{{ status.stackerCurrentRow || 0 }}行{{ status.stackerCurrentColumn || 0 }}列</span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- MES订单状态 -->
      <div class="status-section">
        <h3>MES订单状态</h3>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">出库订单:</span>
                <el-tag :type="status.mesOutboundOrder ? 'primary' : 'info'">
                  {{ status.mesOutboundOrder ? '有订单' : '无订单' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="status-card">
              <div class="status-item">
                <span class="status-label">入库订单:</span>
                <el-tag :type="status.mesInboundOrder ? 'primary' : 'info'">
                  {{ status.mesInboundOrder ? '有订单' : '无订单' }}
                </el-tag>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>


      <!-- 操作日志 -->
      <div class="status-section">
        <h3>操作日志</h3>
        <el-card class="log-card">
          <div class="log-content">
            <div v-for="(log, index) in logs" :key="index" class="log-item">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

export default {
  name: 'StackerMonitor',
  setup() {
    const status = reactive({
      connected: false,
      systemBusy: false,
      hasOrder: false,
      wmsBusy: 0,
      wmsOutboundProgress: 0,
      wmsInboundProgress: 0,
      wmsCurrentRow: 0,
      wmsCurrentColumn: 0,
      stackerStatus: 0,
      stackerOperation: 0,
      stackerCurrentRow: 0,
      stackerCurrentColumn: 0,
      stackerProgress: 0,
      stackerComplete: 0,
      mesOutboundOrder: 0,
      mesInboundOrder: 0
    })

    const loading = ref(false)

    const logs = ref([])
    const initializing = ref(false)
    const resetting = ref(false)
    const clearing = ref(false)
    let monitoringInterval = null

    // 添加日志
    const addLog = (message) => {
      const now = new Date()
      const time = now.toLocaleTimeString()
      logs.value.unshift({ time, message })
      if (logs.value.length > 50) {
        logs.value.pop()
      }
    }

    // 从后端获取操作日志
    const loadOperationLogs = async () => {
      try {
        const response = await fetch('/api/logs/mes-wms?page=0&size=20')
        const data = await response.json()
        
        if (data.content) {
          logs.value = data.content.map(log => ({
            time: new Date(log.timestamp).toLocaleTimeString(),
            message: `[${log.level}] ${log.message}`,
            level: log.level
          }))
        }
      } catch (error) {
        console.error('获取操作日志失败:', error)
      }
    }

    // 获取状态
    const getStatus = async (showLoading = false) => {
      try {
        if (showLoading) {
          loading.value = true
        }
        const response = await fetch('/api/stacker-monitor/status')
        const data = await response.json()
        
        // 检查连接状态，如果后端显示未连接但localStorage显示已连接，保持localStorage状态
        if (!data.connected) {
          const savedStatus = localStorage.getItem('wmsStackerConnectionStatus')
          if (savedStatus) {
            try {
              const parsed = JSON.parse(savedStatus)
              if (parsed.connected) {
                console.log('检测到localStorage中的连接状态，保持localStorage状态')
                data.connected = true
                data.message = '连接状态已从缓存恢复'
                addLog('WARNING', '后端显示未连接，但localStorage显示已连接，保持localStorage状态')
              }
            } catch (e) {
              console.error('解析localStorage状态失败:', e)
            }
          }
        } else {
          // 如果连接正常，保存到localStorage
          localStorage.setItem('wmsStackerConnectionStatus', JSON.stringify({
            connected: data.connected,
            message: data.message || 'WMS-堆垛机连接正常'
          }))
        }
        
        Object.assign(status, data)
        
        if (data.error) {
          addLog(`错误: ${data.error}`)
        }
        
        // 同时加载操作日志
        await loadOperationLogs()
      } catch (error) {
        console.error('获取状态失败:', error)
        addLog(`获取状态失败: ${error.message}`)
        
        // 如果请求失败，尝试从localStorage恢复状态
        const savedStatus = localStorage.getItem('wmsStackerConnectionStatus')
        if (savedStatus) {
          try {
            const parsed = JSON.parse(savedStatus)
            if (parsed.connected) {
              console.log('从localStorage恢复WMS-堆垛机连接状态')
              status.connected = true
              status.message = parsed.message || '连接状态已恢复'
              addLog('WARNING', '连接状态检查失败，保持localStorage状态')
            }
          } catch (e) {
            console.error('恢复连接状态失败:', e)
          }
        }
      } finally {
        if (showLoading) {
          loading.value = false
        }
      }
    }

    // 初始化流程
    const initializeFlow = async () => {
      initializing.value = true
      try {
        const response = await fetch('/api/stacker-monitor/initialize', {
          method: 'POST'
        })
        const data = await response.json()
        
        if (data.success) {
          ElMessage.success('初始化成功')
          addLog('WMS-堆垛机状态初始化成功')
        } else {
          ElMessage.error('初始化失败: ' + data.error)
          addLog('初始化失败: ' + data.error)
        }
      } catch (error) {
        console.error('初始化失败:', error)
        ElMessage.error('初始化失败: ' + error.message)
        addLog('初始化失败: ' + error.message)
      } finally {
        initializing.value = false
      }
    }

    // 强制重置
    const forceResetAll = async () => {
      resetting.value = true
      try {
        const response = await fetch('/api/stacker-monitor/reset-all', {
          method: 'POST'
        })
        const data = await response.json()
        
        if (data.success) {
          ElMessage.success('重置成功')
          addLog('所有状态已强制重置')
        } else {
          ElMessage.error('重置失败: ' + data.error)
          addLog('重置失败: ' + data.error)
        }
      } catch (error) {
        console.error('重置失败:', error)
        ElMessage.error('重置失败: ' + error.message)
        addLog('重置失败: ' + error.message)
      } finally {
        resetting.value = false
      }
    }

    // 检查完成状态
    const checkCompletion = async () => {
      try {
        const response = await fetch('/api/stacker-monitor/check-completion', {
          method: 'POST'
        })
        const data = await response.json()
        
        if (data.success) {
          addLog('完成状态检查完成')
        } else {
          addLog('检查完成状态失败: ' + data.error)
        }
      } catch (error) {
        console.error('检查完成状态失败:', error)
        addLog('检查完成状态失败: ' + error.message)
      }
    }

    // 清理所有状态
    const clearAllStatus = async () => {
      clearing.value = true
      try {
        const response = await fetch('/api/stacker-monitor/clear-all-status', {
          method: 'POST'
        })
        const data = await response.json()
        
        if (data.success) {
          ElMessage.success('所有状态已清理完成')
          addLog('所有状态已清理完成')
          // 刷新状态
          await getStatus()
        } else {
          ElMessage.error('清理状态失败: ' + data.error)
          addLog('清理状态失败: ' + data.error)
        }
      } catch (error) {
        console.error('清理状态失败:', error)
        ElMessage.error('清理状态失败: ' + error.message)
        addLog('清理状态失败: ' + error.message)
      } finally {
        clearing.value = false
      }
    }

    // 获取堆垛机状态类型
    const getStackerStatusType = (status) => {
      switch (status) {
        case 0: return 'success'
        case 1: return 'warning'
        case 2: return 'danger'
        default: return 'info'
      }
    }

    // 获取堆垛机状态文本
    const getStackerStatusText = (status) => {
      switch (status) {
        case 0: return '空闲'
        case 1: return '忙碌'
        case 2: return '错误'
        default: return '未知'
      }
    }

    // 获取操作类型
    const getOperationType = (operation) => {
      switch (operation) {
        case 0: return 'info'
        case 1: return 'primary'
        case 2: return 'warning'
        default: return 'info'
      }
    }

    // 获取操作文本
    const getOperationText = (operation) => {
      switch (operation) {
        case 0: return '无操作'
        case 1: return '入库'
        case 2: return '出库'
        default: return '未知'
      }
    }

    // 开始监控
    const startMonitoring = () => {
      monitoringInterval = setInterval(() => getStatus(false), 2000)
      addLog('开始监控WMS-堆垛机状态')
    }

    // 停止监控
    const stopMonitoring = () => {
      if (monitoringInterval) {
        clearInterval(monitoringInterval)
        monitoringInterval = null
        addLog('停止监控WMS-堆垛机状态')
      }
    }

    // 从localStorage恢复连接状态
    const restoreConnectionState = () => {
      try {
        const savedStatus = localStorage.getItem('wmsStackerConnectionStatus')
        if (savedStatus) {
          const parsed = JSON.parse(savedStatus)
          if (parsed.connected) {
            console.log('从localStorage恢复WMS-堆垛机连接状态')
            status.connected = true
            status.message = parsed.message || '连接状态已恢复'
            addLog('INFO', 'WMS-堆垛机连接状态已从缓存恢复')
          }
        }
      } catch (error) {
        console.error('恢复连接状态失败:', error)
      }
    }

    onMounted(async () => {
      // 先尝试从localStorage恢复连接状态
      restoreConnectionState()
      
      // 然后获取实际状态
      await getStatus(true)
      startMonitoring()
    })

    onUnmounted(() => {
      stopMonitoring()
    })

    return {
      status,
      logs,
      loading,
      initializing,
      resetting,
      clearing,
      initializeFlow,
      forceResetAll,
      checkCompletion,
      clearAllStatus,
      getStackerStatusType,
      getStackerStatusText,
      getOperationType,
      getOperationText
    }
  }
}
</script>

<style scoped>
.stacker-monitor {
  padding: 20px;
}

.monitor-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.status-section {
  margin-bottom: 30px;
}

.status-section h3 {
  margin-bottom: 15px;
  color: #303133;
  border-bottom: 2px solid #409EFF;
  padding-bottom: 5px;
}

.status-card {
  margin-bottom: 10px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
}

.status-label {
  font-weight: bold;
  color: #606266;
}

.status-value {
  color: #409EFF;
  font-weight: bold;
}


.log-card {
  max-height: 300px;
  overflow-y: auto;
}

.log-content {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.log-item {
  display: flex;
  gap: 10px;
  padding: 2px 0;
  border-bottom: 1px solid #F0F0F0;
}

.log-time {
  color: #909399;
  min-width: 80px;
}

.log-message {
  color: #303133;
  flex: 1;
}

.el-progress {
  width: 100%;
}
</style>
