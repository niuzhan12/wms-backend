<template>
  <div class="debug">
    <div class="page-header">
      <h2>调试</h2>
    </div>
    
    <div class="debug-content">
      <el-row :gutter="20">
        <!-- 位置选择 -->
        <el-col :span="8">
          <el-card class="debug-card">
            <template #header>
              <div class="card-header">
                <span>位置选择</span>
              </div>
            </template>
            
            <el-form :model="locationForm" label-width="80px">
              <el-form-item label="库">
                <el-select v-model="locationForm.warehouse" placeholder="选择仓库">
                  <el-option label="A库" value="A" />
                  <el-option label="B库" value="B" />
                </el-select>
              </el-form-item>
              <el-form-item label="行">
                <el-input-number v-model="locationForm.row" :min="1" :max="4" />
              </el-form-item>
              <el-form-item label="列">
                <el-input-number v-model="locationForm.column" :min="1" :max="6" />
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>
        
        <!-- 出入库操作 -->
        <el-col :span="8">
          <el-card class="debug-card">
            <template #header>
              <div class="card-header">
                <span>出入库操作</span>
              </div>
            </template>
            
            <div class="operation-buttons">
              <el-button 
                type="primary" 
                size="large" 
                @click="showInboundDialog"
                :disabled="!locationForm.warehouse"
              >
                <el-icon><Upload /></el-icon>
                入库
              </el-button>
              
              <el-button 
                type="success" 
                size="large" 
                @click="showOutboundDialog"
                :disabled="!locationForm.warehouse"
              >
                <el-icon><Download /></el-icon>
                出库
              </el-button>
              
              <el-button 
                type="warning" 
                size="large" 
                @click="resetInboundOutbound"
              >
                <el-icon><RefreshRight /></el-icon>
                出入库复位
              </el-button>
            </div>
          </el-card>
        </el-col>
        
        <!-- 线体控制 -->
        <el-col :span="8">
          <el-card class="debug-card">
            <template #header>
              <div class="card-header">
                <span>线体控制</span>
              </div>
            </template>
            
            <div class="operation-buttons">
              <el-button 
                type="primary" 
                size="large" 
                @click="lineInbound"
                :disabled="!locationForm.warehouse"
              >
                <el-icon><Upload /></el-icon>
                线体入货
              </el-button>
              
              <el-button 
                type="success" 
                size="large" 
                @click="lineOutbound"
                :disabled="!locationForm.warehouse"
              >
                <el-icon><Download /></el-icon>
                线体出货
              </el-button>
              
              <el-button 
                type="warning" 
                size="large" 
                @click="lineReset"
              >
                <el-icon><RefreshRight /></el-icon>
                线体复位
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 其他控制按钮 -->
      <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="24">
          <el-card class="debug-card">
            <template #header>
              <div class="card-header">
                <span>其他控制</span>
              </div>
            </template>
            
            <div class="other-controls">
              <el-button type="info" size="large">
                <el-icon><VideoPlay /></el-icon>
                开伺服
              </el-button>
              
              <el-button type="info" size="large">
                <el-icon><Document /></el-icon>
                启动文件
              </el-button>
              
              <el-button type="info" size="large">
                <el-icon><Position /></el-icon>
                出库AOV到位
              </el-button>
              
              <el-button type="info" size="large">
                <el-icon><Connection /></el-icon>
                AOV接完成
              </el-button>
              
              <el-button type="info" size="large">
                <el-icon><Position /></el-icon>
                入库AOV到位
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 日志显示 -->
      <el-card class="debug-card" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>系统日志</span>
            <el-button size="small" @click="clearLogs">清空日志</el-button>
          </div>
        </template>
        
        <div class="log-container">
          <div 
            v-for="(log, index) in logs" 
            :key="index" 
            class="log-item"
            :class="log.type"
          >
            <span class="log-time">{{ log.time }}</span>
            <span class="log-message">{{ log.message }}</span>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 入库对话框 -->
    <el-dialog
      v-model="inboundDialogVisible"
      title="入库操作"
      width="500px"
    >
      <el-form :model="inboundForm" label-width="100px">
        <el-form-item label="托盘编码">
          <el-input v-model="inboundForm.palletCode" placeholder="请输入托盘编码" />
        </el-form-item>
        <el-form-item label="物料编码">
          <el-input v-model="inboundForm.materialCode" placeholder="请输入物料编码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="inboundDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="executeInbound">确定</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 出库对话框 -->
    <el-dialog
      v-model="outboundDialogVisible"
      title="出库操作"
      width="400px"
    >
      <div class="outbound-confirm">
        <p>确定要从位置 <strong>{{ locationForm.warehouse }}-{{ locationForm.row }}-{{ locationForm.column }}</strong> 出库吗？</p>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="outboundDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="executeOutbound">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { 
  Upload, Download, RefreshRight, VideoPlay, Document, 
  Position, Connection 
} from '@element-plus/icons-vue'

export default {
  name: 'Debug',
  components: {
    Upload,
    Download,
    RefreshRight,
    VideoPlay,
    Document,
    Position,
    Connection
  },
  setup() {
    const locationForm = reactive({
      warehouse: '',
      row: 1,
      column: 1
    })
    
    const inboundDialogVisible = ref(false)
    const outboundDialogVisible = ref(false)
    const inboundForm = reactive({
      palletCode: '',
      materialCode: ''
    })
    
    const logs = ref([])
    
    const addLog = (message, type = 'info') => {
      const now = new Date()
      const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
      logs.value.unshift({ time, message, type })
      
      if (logs.value.length > 100) {
        logs.value = logs.value.slice(0, 100)
      }
    }
    
    const showInboundDialog = () => {
      if (!locationForm.warehouse) {
        ElMessage.warning('请先选择仓库位置')
        return
      }
      inboundDialogVisible.value = true
    }
    
    const showOutboundDialog = () => {
      if (!locationForm.warehouse) {
        ElMessage.warning('请先选择仓库位置')
        return
      }
      outboundDialogVisible.value = true
    }
    
    const executeInbound = async () => {
      try {
        const response = await axios.post('/api/warehouse/inbound', {
          warehouseCode: locationForm.warehouse,
          row: locationForm.row,
          column: locationForm.column,
          palletCode: inboundForm.palletCode,
          materialCode: inboundForm.materialCode
        })
        
        if (response.data.success) {
          ElMessage.success('入库成功')
          addLog(`入库成功: ${locationForm.warehouse}-${locationForm.row}-${locationForm.column}`, 'success')
        } else {
          ElMessage.error(response.data.message || '入库失败')
          addLog(`入库失败: ${response.data.message}`, 'error')
        }
        
        inboundDialogVisible.value = false
        inboundForm.palletCode = ''
        inboundForm.materialCode = ''
      } catch (error) {
        ElMessage.error('入库操作失败')
        addLog(`入库操作失败: ${error.message}`, 'error')
        console.error(error)
      }
    }
    
    const executeOutbound = async () => {
      try {
        const response = await axios.post('/api/warehouse/outbound', {
          warehouseCode: locationForm.warehouse,
          row: locationForm.row,
          column: locationForm.column
        })
        
        if (response.data.success) {
          ElMessage.success('出库成功')
          addLog(`出库成功: ${locationForm.warehouse}-${locationForm.row}-${locationForm.column}`, 'success')
        } else {
          ElMessage.error(response.data.message || '出库失败')
          addLog(`出库失败: ${response.data.message}`, 'error')
        }
        
        outboundDialogVisible.value = false
      } catch (error) {
        ElMessage.error('出库操作失败')
        addLog(`出库操作失败: ${error.message}`, 'error')
        console.error(error)
      }
    }
    
    const resetInboundOutbound = async () => {
      try {
        await axios.post('/api/device/reset')
        ElMessage.success('出入库复位成功')
        addLog('出入库复位成功', 'success')
      } catch (error) {
        ElMessage.error('出入库复位失败')
        addLog(`出入库复位失败: ${error.message}`, 'error')
        console.error(error)
      }
    }
    
    const lineInbound = async () => {
      try {
        const response = await axios.post('/api/warehouse/line-control', {
          operation: 'inbound',
          row: locationForm.row,
          column: locationForm.column
        })
        
        if (response.data.success) {
          ElMessage.success('线体入货成功')
          addLog(`线体入货成功: ${locationForm.warehouse}-${locationForm.row}-${locationForm.column}`, 'success')
        } else {
          ElMessage.error(response.data.message || '线体入货失败')
          addLog(`线体入货失败: ${response.data.message}`, 'error')
        }
      } catch (error) {
        ElMessage.error('线体入货操作失败')
        addLog(`线体入货操作失败: ${error.message}`, 'error')
        console.error(error)
      }
    }
    
    const lineOutbound = async () => {
      try {
        const response = await axios.post('/api/warehouse/line-control', {
          operation: 'outbound',
          row: locationForm.row,
          column: locationForm.column
        })
        
        if (response.data.success) {
          ElMessage.success('线体出货成功')
          addLog(`线体出货成功: ${locationForm.warehouse}-${locationForm.row}-${locationForm.column}`, 'success')
        } else {
          ElMessage.error(response.data.message || '线体出货失败')
          addLog(`线体出货失败: ${response.data.message}`, 'error')
        }
      } catch (error) {
        ElMessage.error('线体出货操作失败')
        addLog(`线体出货操作失败: ${error.message}`, 'error')
        console.error(error)
      }
    }
    
    const lineReset = async () => {
      try {
        const response = await axios.post('/api/warehouse/line-control', {
          operation: 'reset',
          row: 0,
          column: 0
        })
        
        if (response.data.success) {
          ElMessage.success('线体复位成功')
          addLog('线体复位成功', 'success')
        } else {
          ElMessage.error(response.data.message || '线体复位失败')
          addLog(`线体复位失败: ${response.data.message}`, 'error')
        }
      } catch (error) {
        ElMessage.error('线体复位操作失败')
        addLog(`线体复位操作失败: ${error.message}`, 'error')
        console.error(error)
      }
    }
    
    const clearLogs = () => {
      logs.value = []
      ElMessage.success('日志已清空')
    }
    
    return {
      locationForm,
      inboundDialogVisible,
      outboundDialogVisible,
      inboundForm,
      logs,
      showInboundDialog,
      showOutboundDialog,
      executeInbound,
      executeOutbound,
      resetInboundOutbound,
      lineInbound,
      lineOutbound,
      lineReset,
      clearLogs
    }
  }
}
</script>

<style scoped>
.debug {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0;
  color: #2c3e50;
}

.debug-content {
  margin-bottom: 20px;
}

.debug-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  color: #2c3e50;
}

.operation-buttons {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.other-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.log-container {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  background-color: #f5f7fa;
}

.log-item {
  padding: 5px 0;
  border-bottom: 1px solid #e4e7ed;
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #909399;
  margin-right: 10px;
  font-weight: bold;
}

.log-message {
  color: #606266;
}

.log-item.error .log-message {
  color: #f56c6c;
}

.log-item.success .log-message {
  color: #67c23a;
}

.log-item.warning .log-message {
  color: #e6a23c;
}

.outbound-confirm {
  text-align: center;
  padding: 20px 0;
}

.outbound-confirm p {
  margin: 0;
  font-size: 16px;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
