<template>
  <div class="device-status">
    <div class="page-header">
      <h2>设备状态</h2>
    </div>
    
    <div class="status-content">
      <el-row :gutter="20">
        <!-- 设备状态参数 -->
        <el-col :span="12">
          <el-card class="status-card">
            <template #header>
              <div class="card-header">
                <span>设备状态参数</span>
              </div>
            </template>
            
            <el-descriptions :column="1" border>
              <el-descriptions-item label="设备状态">
                <el-tag :type="getDeviceStatusType(deviceStatus.status)">
                  {{ getDeviceStatusText(deviceStatus.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="操作的列">
                {{ deviceStatus.currentColumn }}
              </el-descriptions-item>
              <el-descriptions-item label="操作的行">
                {{ deviceStatus.currentRow }}
              </el-descriptions-item>
              <el-descriptions-item label="仓库选择">
                <el-select 
                  v-model="deviceStatus.warehouseSelection" 
                  placeholder="选择仓库"
                  style="width: 100%"
                  @change="onWarehouseChange"
                >
                  <el-option label="未选择" value="" />
                  <el-option label="A库" value="A" />
                  <el-option label="B库" value="B" />
                </el-select>
              </el-descriptions-item>
              <el-descriptions-item label="命令">
                <el-input v-model="deviceStatus.command" placeholder="输入命令" />
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
        
        <!-- 控制器状态 -->
        <el-col :span="12">
          <el-card class="status-card">
            <template #header>
              <div class="card-header">
                <span>控制器状态</span>
              </div>
            </template>
            
            <el-descriptions :column="1" border>
              <el-descriptions-item label="模式选择">
                <el-select 
                  v-model="deviceStatus.remoteMode" 
                  placeholder="选择模式"
                  style="width: 100%"
                  @change="onRemoteModeChange"
                >
                  <el-option label="本地模式" :value="false" />
                  <el-option label="远程模式" :value="true" />
                </el-select>
              </el-descriptions-item>
              <el-descriptions-item label="是否工作中">
                <el-tag :type="deviceStatus.remoteWorking ? 'warning' : 'info'">
                  {{ deviceStatus.remoteWorking ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="文件1执行中">
                <el-tag :type="deviceStatus.fileExecuting ? 'warning' : 'info'">
                  {{ deviceStatus.fileExecuting ? '是' : '否' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
    </div>
    
    <!-- 设置忙状态对话框 -->
    <el-dialog
      v-model="setBusyDialogVisible"
      title="设置设备忙状态"
      width="400px"
    >
      <el-form :model="busyForm" label-width="80px">
        <el-form-item label="行">
          <el-input-number v-model="busyForm.row" :min="1" :max="4" />
        </el-form-item>
        <el-form-item label="列">
          <el-input-number v-model="busyForm.column" :min="1" :max="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="setBusyDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="setDeviceBusy">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import deviceModeManager from '../utils/deviceMode.js'

export default {
  name: 'DeviceStatus',
  components: {},
  setup() {
    const deviceStatus = ref({
      status: 'IDLE',
      currentRow: 0,
      currentColumn: 0,
      controlMode: 'LOCAL',
      warehouseSelection: '未选择',
      command: '',
      remoteMode: false,
      remoteWorking: false,
      fileExecuting: false
    })

    // 初始化设备模式
    deviceStatus.value.remoteMode = deviceModeManager.getMode() === 'remote'
    
    const setBusyDialogVisible = ref(false)
    const busyForm = reactive({
      row: 1,
      column: 1
    })
    
    const loadDeviceStatus = async () => {
      try {
        const response = await axios.get('/api/device/status')
        // 保存当前的模式设置
        const currentMode = deviceStatus.value.remoteMode
        deviceStatus.value = response.data
        // 恢复模式设置
        deviceStatus.value.remoteMode = currentMode
        console.log('设备状态已加载:', deviceStatus.value)
      } catch (error) {
        ElMessage.error('加载设备状态失败')
        console.error(error)
      }
    }
    
    // 监听MES-WMS模式变化
    const checkWmsMode = async () => {
      try {
        const response = await axios.get('/api/mes-wms/mode')
        if (response.data.success) {
          const wmsMode = response.data.mode
          const currentMode = deviceStatus.value.remoteMode ? 1 : 0
          if (wmsMode !== currentMode) {
            deviceStatus.value.remoteMode = wmsMode === 1
            deviceModeManager.setMode(wmsMode === 1 ? 'remote' : 'local')
            console.log('WMS模式已同步:', wmsMode === 1 ? '远程' : '本地')
          }
        }
      } catch (error) {
        console.error('检查WMS模式失败:', error)
      }
    }
    
    const getDeviceStatusType = (status) => {
      return status === 'IDLE' ? 'success' : 'warning'
    }
    
    const getDeviceStatusText = (status) => {
      return status === 'IDLE' ? '空闲' : '忙'
    }
    
    
    
    
    const onWarehouseChange = (value) => {
      if (value) {
        ElMessage.success(`已选择${value === 'A' ? 'A库' : 'B库'}`)
        // 这里可以添加选择仓库后的逻辑，比如更新设备状态等
      } else {
        ElMessage.info('已取消仓库选择')
      }
    }
    
    const onRemoteModeChange = async (value) => {
      try {
        // 更新MES-WMS模式
        const mode = value ? 1 : 0
        const response = await axios.post('/api/mes-wms/update-mode', { mode })
        
        if (response.data.success) {
          // 更新全局设备模式状态
          const modeText = value ? 'remote' : 'local'
          deviceModeManager.setMode(modeText)
          
          // 强制更新本地设备状态
          deviceStatus.value.remoteMode = value
          
          console.log(`模式已切换到: ${modeText}, remoteMode: ${deviceStatus.value.remoteMode}`)
          ElMessage.success(`已切换到${value ? '远程模式' : '本地模式'}`)
        } else {
          ElMessage.error(response.data.message || '切换模式失败')
          // 如果失败，恢复原值
          deviceStatus.value.remoteMode = !value
        }
      } catch (error) {
        ElMessage.error('切换模式失败: ' + error.message)
        console.error(error)
        // 如果失败，恢复原值
        deviceStatus.value.remoteMode = !value
        deviceModeManager.setMode(deviceStatus.value.remoteMode ? 'remote' : 'local')
      }
    }
    
    onMounted(() => {
      // 先设置模式，再加载设备状态
      const currentMode = deviceModeManager.getMode()
      deviceStatus.value.remoteMode = currentMode === 'remote'
      console.log('初始化设备模式:', currentMode, 'remoteMode:', deviceStatus.value.remoteMode)
      
      loadDeviceStatus()
      
      // 定期检查WMS模式变化
      setInterval(checkWmsMode, 3000) // 每3秒检查一次
    })
    
    return {
      deviceStatus,
      setBusyDialogVisible,
      busyForm,
      getDeviceStatusType,
      getDeviceStatusText,
      onWarehouseChange,
      onRemoteModeChange
    }
  }
}
</script>

<style scoped>
.device-status {
  padding: 20px;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0;
  color: #2c3e50;
}

.status-content {
  margin-bottom: 20px;
}

.status-card {
  margin-bottom: 20px;
}

.card-header {
  font-weight: bold;
  color: #2c3e50;
}


.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
