<template>
  <div class="modbus-test">
    <div class="page-header">
      <h2>Modbus连接测试</h2>
    </div>
    
    <div class="content-container">
      <el-row :gutter="20">
        <!-- 连接配置 -->
        <el-col :span="12">
          <el-card class="config-card">
            <template #header>
              <div class="card-header">
                <span>连接配置</span>
              </div>
            </template>
            
            <el-form :model="connectionConfig" label-width="80px">
              <el-form-item label="IP地址">
                <el-input v-model="connectionConfig.host" placeholder="127.0.0.1" />
              </el-form-item>
              <el-form-item label="端口">
                <el-input-number v-model="connectionConfig.port" :min="1" :max="65535" />
              </el-form-item>
              <el-form-item label="从站ID">
                <el-input-number v-model="connectionConfig.slaveId" :min="1" :max="255" />
              </el-form-item>
              <el-form-item>
                <el-button 
                  :type="connectionStatus.connected ? 'danger' : 'success'" 
                  @click="toggleConnection"
                  :loading="connecting"
                >
                  <el-icon><Connection /></el-icon>
                  {{ connectionStatus.connected ? '断开连接' : '连接' }}
                </el-button>
                <el-button type="primary" @click="checkConnection" style="margin-left: 10px;">
                  <el-icon><Refresh /></el-icon>
                  检查状态
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
        
        <!-- 连接状态 -->
        <el-col :span="12">
          <el-card class="status-card">
            <template #header>
              <div class="card-header">
                <span>连接状态</span>
              </div>
            </template>
            
            <div class="status-content">
              <el-tag :type="connectionStatus.connected ? 'success' : 'danger'" size="large">
                {{ connectionStatus.connected ? '已连接' : '未连接' }}
              </el-tag>
              <p class="status-message">{{ connectionStatus.message }}</p>
              <div v-if="connectionStatus.connected" class="connection-info">
                <p><strong>连接信息：</strong></p>
                <p>IP: {{ connectionConfig.host }}</p>
                <p>端口: {{ connectionConfig.port }}</p>
                <p>从站ID: {{ connectionConfig.slaveId }}</p>
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
    const connectionStatus = ref({
      connected: false,
      message: '未检查'
    })
    
    const connectionConfig = reactive({
      host: '127.0.0.1',
      port: 502,
      slaveId: 1
    })
    
    const connecting = ref(false)
    
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
    
    // 检查连接状态
    const checkConnection = async () => {
      try {
        const response = await axios.get('/api/modbus/status')
        connectionStatus.value = response.data
        addLog('INFO', response.data.message)
      } catch (error) {
        ElMessage.error('检查连接失败')
        addLog('ERROR', '检查连接失败: ' + error.message)
      }
    }
    
    // 切换连接状态
    const toggleConnection = async () => {
      if (connectionStatus.value.connected) {
        // 断开连接
        try {
          const response = await axios.post('/api/modbus/disconnect')
          connectionStatus.value.connected = false
          connectionStatus.value.message = '已断开连接'
          addLog('INFO', 'Modbus连接已断开')
          ElMessage.success('连接已断开')
        } catch (error) {
          ElMessage.error('断开连接失败')
          addLog('ERROR', '断开连接失败: ' + error.message)
        }
      } else {
        // 建立连接
        connecting.value = true
        try {
          const response = await axios.post('/api/modbus/connect', {
            host: connectionConfig.host,
            port: connectionConfig.port,
            slaveId: connectionConfig.slaveId
          })
          connectionStatus.value.connected = response.data.success
          connectionStatus.value.message = response.data.message
          addLog('INFO', response.data.message)
          ElMessage.success(response.data.message)
        } catch (error) {
          ElMessage.error('连接失败')
          addLog('ERROR', '连接失败: ' + error.message)
        } finally {
          connecting.value = false
        }
      }
    }
    
    // 重新连接
    const reconnect = async () => {
      try {
        const response = await axios.post('/api/modbus/reconnect')
        connectionStatus.value.connected = response.data.success
        connectionStatus.value.message = response.data.message
        addLog('INFO', response.data.message)
        ElMessage.success(response.data.message)
      } catch (error) {
        ElMessage.error('重连失败')
        addLog('ERROR', '重连失败: ' + error.message)
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
    
    onMounted(() => {
      checkConnection()
    })
    
    return {
      connectionStatus,
      connectionConfig,
      connecting,
      readForm,
      writeForm,
      readResult,
      reading,
      writing,
      logs,
      checkConnection,
      toggleConnection,
      reconnect,
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
