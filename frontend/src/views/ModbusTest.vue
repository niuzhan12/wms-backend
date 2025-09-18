<template>
  <div class="modbus-test">
    <div class="page-header">
      <h2>Modbus连接测试</h2>
    </div>
    
    <div class="content-container">
      <el-row :gutter="20">
        <!-- MES-WMS连接配置 -->
        <el-col :span="12">
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <span>MES-WMS连接配置 (端口502)</span>
              </div>
            </template>
            
            <el-form :model="mesWmsConfig" label-width="80px">
              <el-form-item label="IP地址">
                <el-input v-model="mesWmsConfig.host" placeholder="127.0.0.1" />
              </el-form-item>
              <el-form-item label="端口">
                <el-input-number v-model="mesWmsConfig.port" :min="1" :max="65535" />
              </el-form-item>
              <el-form-item label="从站ID">
                <el-input-number v-model="mesWmsConfig.slaveId" :min="1" :max="255" />
              </el-form-item>
              <el-form-item>
                <el-button 
                  :type="mesWmsStatus.connected ? 'danger' : 'success'" 
                  @click="toggleMesWmsConnection"
                  :loading="mesWmsConnecting"
                >
                  <el-icon><Connection /></el-icon>
                  {{ mesWmsStatus.connected ? '断开连接' : '连接' }}
                </el-button>
                <el-button type="primary" @click="checkMesWmsConnection" style="margin-left: 10px;">
                  <el-icon><Refresh /></el-icon>
                  检查状态
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
        
        <!-- WMS-堆垛机连接配置 -->
        <el-col :span="12">
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <span>WMS-堆垛机连接配置 (端口503)</span>
              </div>
            </template>
            
            <el-form :model="wmsStackerConfig" label-width="80px">
              <el-form-item label="IP地址">
                <el-input v-model="wmsStackerConfig.host" placeholder="127.0.0.1" />
              </el-form-item>
              <el-form-item label="端口">
                <el-input-number v-model="wmsStackerConfig.port" :min="1" :max="65535" />
              </el-form-item>
              <el-form-item label="从站ID">
                <el-input-number v-model="wmsStackerConfig.slaveId" :min="1" :max="255" />
              </el-form-item>
              <el-form-item>
                <el-button 
                  :type="wmsStackerStatus.connected ? 'danger' : 'success'" 
                  @click="toggleWmsStackerConnection"
                  :loading="wmsStackerConnecting"
                >
                  <el-icon><Connection /></el-icon>
                  {{ wmsStackerStatus.connected ? '断开连接' : '连接' }}
                </el-button>
                <el-button type="primary" @click="checkWmsStackerConnection" style="margin-left: 10px;">
                  <el-icon><Refresh /></el-icon>
                  检查状态
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="20" style="margin-top: 20px;">
        <!-- MES-WMS连接状态 -->
        <el-col :span="12">
          <el-card class="status-card">
            <template #header>
              <div class="card-header">
                <span>MES-WMS连接状态</span>
              </div>
            </template>
            
            <div class="status-content">
              <el-tag :type="mesWmsStatus.connected ? 'success' : 'danger'" size="large">
                {{ mesWmsStatus.connected ? '已连接' : '未连接' }}
              </el-tag>
              <p class="status-message">{{ mesWmsStatus.message }}</p>
              <div v-if="mesWmsStatus.connected" class="connection-info">
                <p><strong>连接信息：</strong></p>
                <p>IP: {{ mesWmsConfig.host }}</p>
                <p>端口: {{ mesWmsConfig.port }}</p>
                <p>从站ID: {{ mesWmsConfig.slaveId }}</p>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <!-- WMS-堆垛机连接状态 -->
        <el-col :span="12">
          <el-card class="status-card">
            <template #header>
              <div class="card-header">
                <span>WMS-堆垛机连接状态</span>
              </div>
            </template>
            
            <div class="status-content">
              <el-tag :type="wmsStackerStatus.connected ? 'success' : 'danger'" size="large">
                {{ wmsStackerStatus.connected ? '已连接' : '未连接' }}
              </el-tag>
              <p class="status-message">{{ wmsStackerStatus.message }}</p>
              <div v-if="wmsStackerStatus.connected" class="connection-info">
                <p><strong>连接信息：</strong></p>
                <p>IP: {{ wmsStackerConfig.host }}</p>
                <p>端口: {{ wmsStackerConfig.port }}</p>
                <p>从站ID: {{ wmsStackerConfig.slaveId }}</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="20" style="margin-top: 20px;">
        
        <!-- 读取寄存器 -->
        <el-col :span="12">
          <el-card class="operation-card">
            <template #header>
              <div class="card-header">
                <span>读取寄存器</span>
              </div>
            </template>
            
            <el-form :model="readForm" label-width="100px">
              <el-form-item label="起始地址">
                <el-input-number v-model="readForm.startAddress" :min="0" :max="65535" />
              </el-form-item>
              <el-form-item label="数量">
                <el-input-number v-model="readForm.quantity" :min="1" :max="125" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="readRegisters" :loading="reading">
                  读取寄存器
                </el-button>
              </el-form-item>
            </el-form>
            
            <div v-if="readResult.length > 0" class="result-section">
              <h4>读取结果：</h4>
              <div class="register-values">
                <el-tag 
                  v-for="(value, index) in readResult" 
                  :key="index"
                  class="register-tag"
                >
                  {{ readForm.startAddress + index }}: {{ value }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="20" style="margin-top: 20px;">
        <!-- 写入寄存器 -->
        <el-col :span="12">
          <el-card class="operation-card">
            <template #header>
              <div class="card-header">
                <span>写入寄存器</span>
              </div>
            </template>
            
            <el-form :model="writeForm" label-width="100px">
              <el-form-item label="地址">
                <el-input-number v-model="writeForm.address" :min="0" :max="65535" />
              </el-form-item>
              <el-form-item label="值">
                <el-input-number v-model="writeForm.value" :min="0" :max="65535" />
              </el-form-item>
              <el-form-item>
                <el-button type="success" @click="writeRegister" :loading="writing">
                  写入寄存器
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
        
        <!-- 操作日志 -->
        <el-col :span="12">
          <el-card class="log-card">
            <template #header>
              <div class="card-header">
                <span>操作日志</span>
                <el-button size="small" @click="clearLogs">清空日志</el-button>
              </div>
            </template>
            
            <div class="log-content">
              <div 
                v-for="(log, index) in logs" 
                :key="index"
                class="log-item"
                :class="getLogLevelClass(log.level)"
              >
                <span class="log-time">{{ log.timestamp }}</span>
                <span class="log-level">{{ log.level }}</span>
                <span class="log-message">{{ log.message }}</span>
              </div>
              <div v-if="logs.length === 0" class="no-logs">
                暂无操作日志
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Connection, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'ModbusTest',
  components: {
    Connection,
    Refresh
  },
  setup() {
    // MES-WMS连接状态和配置
    const mesWmsStatus = ref({
      connected: false,
      message: '未检查'
    })
    
    const mesWmsConfig = reactive({
      host: '127.0.0.1',
      port: 502,
      slaveId: 1
    })
    
    const mesWmsConnecting = ref(false)
    
    // WMS-堆垛机连接状态和配置
    const wmsStackerStatus = ref({
      connected: false,
      message: '未检查'
    })
    
    const wmsStackerConfig = reactive({
      host: '127.0.0.1',
      port: 503,
      slaveId: 1
    })
    
    const wmsStackerConnecting = ref(false)
    
    const readForm = reactive({
      startAddress: 4001,
      quantity: 10
    })
    
    const writeForm = reactive({
      address: 4001,
      value: 0
    })
    
    const readResult = ref([])
    const reading = ref(false)
    const writing = ref(false)
    const logs = ref([])
    
    // 检查MES-WMS连接状态
    const checkMesWmsConnection = async () => {
      try {
        const response = await axios.get('/api/mes-wms/status')
        const isConnected = response.data.connected || false
        
        // 如果localStorage中有连接状态，优先使用localStorage的状态
        const savedStatus = localStorage.getItem('mesWmsConnectionStatus')
        if (savedStatus) {
          const parsed = JSON.parse(savedStatus)
          if (parsed.connected && !isConnected) {
            addLog('WARNING', '后端显示未连接，但localStorage显示已连接，保持localStorage状态')
            return
          }
        }
        
        mesWmsStatus.value = {
          connected: isConnected,
          message: response.data.message || (isConnected ? 'MES-WMS连接正常' : 'MES-WMS连接已断开')
        }
        addLog('INFO', 'MES-WMS: ' + mesWmsStatus.value.message)
      } catch (error) {
        // 如果请求失败，但之前是连接状态，保持状态
        if (mesWmsStatus.value.connected) {
          addLog('WARNING', 'MES-WMS连接状态检查失败，保持当前连接状态')
          return
        }
        ElMessage.error('检查MES-WMS连接失败')
        addLog('ERROR', 'MES-WMS连接检查失败: ' + error.message)
      }
    }
    
    // 检查WMS-堆垛机连接状态
    const checkWmsStackerConnection = async () => {
      try {
        const response = await axios.get('/api/stacker-monitor/status')
        const isConnected = response.data.connected || false
        
        // 如果localStorage中有连接状态，优先使用localStorage的状态
        const savedStatus = localStorage.getItem('wmsStackerConnectionStatus')
        if (savedStatus) {
          const parsed = JSON.parse(savedStatus)
          if (parsed.connected && !isConnected) {
            addLog('WARNING', '后端显示未连接，但localStorage显示已连接，保持localStorage状态')
            return
          }
        }
        
        wmsStackerStatus.value = {
          connected: isConnected,
          message: response.data.message || (isConnected ? 'WMS-堆垛机连接正常' : 'WMS-堆垛机连接已断开')
        }
        addLog('INFO', 'WMS-堆垛机: ' + wmsStackerStatus.value.message)
      } catch (error) {
        // 如果请求失败，但之前是连接状态，保持状态
        if (wmsStackerStatus.value.connected) {
          addLog('WARNING', '连接状态检查失败，保持当前连接状态')
          return
        }
        ElMessage.error('检查WMS-堆垛机连接失败')
        addLog('ERROR', 'WMS-堆垛机连接检查失败: ' + error.message)
      }
    }
    
    // 切换MES-WMS连接状态
    const toggleMesWmsConnection = async () => {
      if (mesWmsStatus.value.connected) {
        // 断开连接
        try {
          const response = await axios.post('/api/mes-wms/disconnect')
          mesWmsStatus.value.connected = false
          mesWmsStatus.value.message = 'MES-WMS连接已断开'
          addLog('INFO', 'MES-WMS连接已断开')
          ElMessage.success('MES-WMS连接已断开')
          saveConnectionStates() // 保存状态
        } catch (error) {
          ElMessage.error('断开MES-WMS连接失败')
          addLog('ERROR', '断开MES-WMS连接失败: ' + error.message)
        }
      } else {
        // 建立连接
        mesWmsConnecting.value = true
        try {
          const response = await axios.post('/api/mes-wms/connect', {
            host: mesWmsConfig.host,
            port: mesWmsConfig.port,
            slaveId: mesWmsConfig.slaveId
          })
          mesWmsStatus.value.connected = response.data.success
          mesWmsStatus.value.message = response.data.message
          addLog('INFO', 'MES-WMS: ' + response.data.message)
          ElMessage.success('MES-WMS: ' + response.data.message)
          saveConnectionStates() // 保存状态
        } catch (error) {
          ElMessage.error('MES-WMS连接失败')
          addLog('ERROR', 'MES-WMS连接失败: ' + error.message)
        } finally {
          mesWmsConnecting.value = false
        }
      }
    }
    
    // 切换WMS-堆垛机连接状态
    const toggleWmsStackerConnection = async () => {
      if (wmsStackerStatus.value.connected) {
        // 断开连接
        try {
          const response = await axios.post('/api/stacker-monitor/disconnect')
          wmsStackerStatus.value.connected = false
          wmsStackerStatus.value.message = 'WMS-堆垛机连接已断开'
          addLog('INFO', 'WMS-堆垛机连接已断开')
          ElMessage.success('WMS-堆垛机连接已断开')
          saveConnectionStates() // 保存状态
        } catch (error) {
          ElMessage.error('断开WMS-堆垛机连接失败')
          addLog('ERROR', '断开WMS-堆垛机连接失败: ' + error.message)
        }
      } else {
        // 建立连接
        wmsStackerConnecting.value = true
        try {
          const response = await axios.post('/api/stacker-monitor/connect', {
            host: wmsStackerConfig.host,
            port: wmsStackerConfig.port,
            slaveId: wmsStackerConfig.slaveId
          })
          wmsStackerStatus.value.connected = response.data.success
          wmsStackerStatus.value.message = response.data.message
          addLog('INFO', 'WMS-堆垛机: ' + response.data.message)
          ElMessage.success('WMS-堆垛机: ' + response.data.message)
          saveConnectionStates() // 保存状态
        } catch (error) {
          ElMessage.error('WMS-堆垛机连接失败')
          addLog('ERROR', 'WMS-堆垛机连接失败: ' + error.message)
        } finally {
          wmsStackerConnecting.value = false
        }
      }
    }
    
    // 读取寄存器
    const readRegisters = async () => {
      reading.value = true
      try {
        const response = await axios.get(`/api/modbus/read/${readForm.startAddress}/${readForm.quantity}`)
        
        if (response.data.success) {
          readResult.value = response.data.data
          addLog('INFO', `读取寄存器成功: ${readForm.startAddress}-${readForm.startAddress + readForm.quantity - 1}`)
          ElMessage.success('读取成功')
        } else {
          addLog('ERROR', response.data.message)
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        ElMessage.error('读取失败')
        addLog('ERROR', '读取失败: ' + error.message)
      } finally {
        reading.value = false
      }
    }
    
    // 写入寄存器
    const writeRegister = async () => {
      writing.value = true
      try {
        const response = await axios.post(`/api/modbus/write/${writeForm.address}`, {
          value: writeForm.value
        })
        
        if (response.data.success) {
          addLog('INFO', `写入寄存器成功: 地址${writeForm.address} = ${writeForm.value}`)
          ElMessage.success('写入成功')
        } else {
          addLog('ERROR', response.data.message)
          ElMessage.error(response.data.message)
        }
      } catch (error) {
        ElMessage.error('写入失败')
        addLog('ERROR', '写入失败: ' + error.message)
      } finally {
        writing.value = false
      }
    }
    
    // 添加日志
    const addLog = (level, message) => {
      const now = new Date()
      const timestamp = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
      
      logs.value.unshift({
        timestamp,
        level,
        message
      })
      
      // 限制日志数量
      if (logs.value.length > 100) {
        logs.value = logs.value.slice(0, 100)
      }
    }
    
    // 清空日志
    const clearLogs = () => {
      logs.value = []
    }
    
    // 获取日志级别样式类
    const getLogLevelClass = (level) => {
      return `log-${level.toLowerCase()}`
    }
    
    // 从localStorage恢复连接状态
    const restoreConnectionStates = () => {
      try {
        const savedWmsStackerStatus = localStorage.getItem('wmsStackerConnectionStatus')
        if (savedWmsStackerStatus) {
          const parsed = JSON.parse(savedWmsStackerStatus)
          if (parsed.connected) {
            wmsStackerStatus.value = {
              connected: true,
              message: parsed.message || '连接状态已恢复'
            }
            addLog('INFO', 'WMS-堆垛机连接状态已从缓存恢复')
          }
        }
        
        const savedMesWmsStatus = localStorage.getItem('mesWmsConnectionStatus')
        if (savedMesWmsStatus) {
          const parsed = JSON.parse(savedMesWmsStatus)
          if (parsed.connected) {
            mesWmsStatus.value = {
              connected: true,
              message: parsed.message || '连接状态已恢复'
            }
            addLog('INFO', 'MES-WMS连接状态已从缓存恢复')
          }
        }
      } catch (error) {
        addLog('ERROR', '恢复连接状态失败: ' + error.message)
      }
    }
    
    // 保存连接状态到localStorage
    const saveConnectionStates = () => {
      try {
        localStorage.setItem('wmsStackerConnectionStatus', JSON.stringify({
          connected: wmsStackerStatus.value.connected,
          message: wmsStackerStatus.value.message
        }))
        
        localStorage.setItem('mesWmsConnectionStatus', JSON.stringify({
          connected: mesWmsStatus.value.connected,
          message: mesWmsStatus.value.message
        }))
      } catch (error) {
        addLog('ERROR', '保存连接状态失败: ' + error.message)
      }
    }
    
    onMounted(() => {
      // 先恢复缓存的状态
      restoreConnectionStates()
      
      // 延迟检查实际连接状态，确保状态恢复完成
      setTimeout(() => {
        checkMesWmsConnection()
        checkWmsStackerConnection()
      }, 100)
      
      // 定期保存连接状态
      setInterval(saveConnectionStates, 5000)
    })
    
    return {
      mesWmsStatus,
      mesWmsConfig,
      mesWmsConnecting,
      wmsStackerStatus,
      wmsStackerConfig,
      wmsStackerConnecting,
      readForm,
      writeForm,
      readResult,
      reading,
      writing,
      logs,
      checkMesWmsConnection,
      checkWmsStackerConnection,
      toggleMesWmsConnection,
      toggleWmsStackerConnection,
      readRegisters,
      writeRegister,
      clearLogs,
      getLogLevelClass
    }
  }
}
</script>

<style scoped>
.modbus-test {
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

.status-card, .operation-card, .log-card, .config-card {
  height: 100%;
}

.status-content {
  text-align: center;
  padding: 20px 0;
}

.status-message {
  margin-top: 10px;
  color: #666;
}

.result-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.register-values {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.register-tag {
  margin: 2px;
}

.log-content {
  max-height: 300px;
  overflow-y: auto;
}

.log-item {
  padding: 8px 12px;
  margin-bottom: 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  display: flex;
  gap: 15px;
  align-items: center;
}

.log-time {
  color: #909399;
  min-width: 60px;
}

.log-level {
  min-width: 50px;
  text-align: center;
  border-radius: 2px;
  padding: 2px 6px;
  font-weight: bold;
}

.log-message {
  flex: 1;
}

.log-info {
  background-color: #f0f9ff;
  border-left: 3px solid #409eff;
}

.log-error {
  background-color: #fef0f0;
  border-left: 3px solid #f56c6c;
}

.log-warning {
  background-color: #fdf6ec;
  border-left: 3px solid #e6a23c;
}

.no-logs {
  text-align: center;
  color: #909399;
  padding: 20px;
}

.connection-info {
  margin-top: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  text-align: left;
}

.connection-info p {
  margin: 5px 0;
  font-size: 12px;
  color: #666;
}
</style>
