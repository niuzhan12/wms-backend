<template>
  <div class="warehouse-status">
    <div class="page-header">
      <h2>仓库状态显示</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showInboundDialog">
          <el-icon><Plus /></el-icon>
          入库操作
        </el-button>
        <el-button type="success" @click="showOutboundDialog">
          <el-icon><Minus /></el-icon>
          出库操作
        </el-button>
      </div>
    </div>
    
    <div class="content-container">
      <div class="warehouse-grid">
      <div class="warehouse-section" v-for="warehouse in ['A', 'B']" :key="warehouse">
        <h3>{{ warehouse }}库</h3>
        <div class="grid-container">
          <div 
            v-for="row in 4" 
            :key="row" 
            class="grid-row"
          >
            <div 
              v-for="col in 6" 
              :key="col" 
              class="grid-cell"
              :class="getCellClass(warehouse, row, col)"
              @click="showCellDetails(warehouse, row, col)"
            >
              <div class="cell-coordinate">{{ warehouse }}{{ row }}-{{ col }}</div>
              <div class="cell-indicator">
                <div class="indicator-box" v-if="!getCellStatus(warehouse, row, col).hasPallet"></div>
                <div class="indicator-check" v-else>✓</div>
              </div>
            </div>
          </div>
        </div>
      </div>
      </div>
    </div>
    
    <!-- 单元格详情对话框 -->
    <el-dialog
      v-model="cellDetailsVisible"
      title="位置详情"
      width="500px"
    >
      <div class="cell-details" v-if="selectedCell">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="位置">{{ selectedCell.position }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ selectedCell.warehouse }}库</el-descriptions-item>
          <el-descriptions-item label="行">{{ selectedCell.row }}</el-descriptions-item>
          <el-descriptions-item label="列">{{ selectedCell.column }}</el-descriptions-item>
          <el-descriptions-item label="托盘状态">
            <el-tag :type="selectedCell.hasPallet ? 'success' : 'info'">
              {{ selectedCell.hasPallet ? '有托盘' : '无托盘' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="托盘编码" v-if="selectedCell.hasPallet">
            {{ selectedCell.palletCode || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="物料编码" v-if="selectedCell.hasPallet">
            {{ selectedCell.materialCode || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="物料状态" v-if="selectedCell.hasPallet">
            <el-tag :type="getMaterialStatusType(selectedCell.materialStatus)">
              {{ getMaterialStatusText(selectedCell.materialStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注">
            {{ selectedCell.remarks || '无' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
    
    <!-- 入库操作对话框 -->
    <el-dialog
      v-model="inboundDialogVisible"
      title="入库操作"
      width="500px"
    >
      <el-form :model="inboundForm" :rules="inboundRules" ref="inboundFormRef" label-width="100px">
        <el-form-item label="仓库" prop="warehouseCode">
          <el-select v-model="inboundForm.warehouseCode" placeholder="选择仓库">
            <el-option label="A库" value="A" />
            <el-option label="B库" value="B" />
          </el-select>
        </el-form-item>
        <el-form-item label="行" prop="row" v-if="!isRemoteMode">
          <el-input-number v-model="inboundForm.row" :min="1" :max="4" />
        </el-form-item>
        <el-form-item label="列" prop="column" v-if="!isRemoteMode">
          <el-input-number v-model="inboundForm.column" :min="1" :max="6" />
        </el-form-item>
        <el-form-item v-if="isRemoteMode">
          <el-alert
            title="远程模式：系统将自动选择第一个空闲位置进行入库"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
                 <el-form-item label="托盘编码" prop="palletCode">
           <el-select v-model="inboundForm.palletCode" placeholder="选择托盘" filterable>
             <el-option 
               v-for="pallet in availablePallets" 
               :key="pallet.id" 
               :label="`${pallet.palletNumber} - ${pallet.palletName}`" 
               :value="pallet.palletNumber"
             />
           </el-select>
         </el-form-item>
         <el-form-item label="物料编码" prop="materialCode">
           <el-select v-model="inboundForm.materialCode" placeholder="选择货物" filterable>
             <el-option 
               v-for="good in availableGoods" 
               :key="good.id" 
               :label="`${good.goodsNumber} - ${good.goodsName}`" 
               :value="good.goodsNumber"
             />
           </el-select>
         </el-form-item>
        <el-form-item label="物料状态" prop="materialStatus">
          <el-select v-model="inboundForm.materialStatus" placeholder="选择物料状态">
            <el-option label="毛坯" value="RAW" />
            <el-option label="成品" value="FINISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundForm.remarks" type="textarea" :rows="2" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="inboundDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="executeInbound">确认入库</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 出库操作对话框 -->
    <el-dialog
      v-model="outboundDialogVisible"
      title="出库操作"
      width="500px"
    >
      <el-form :model="outboundForm" :rules="outboundRules" ref="outboundFormRef" label-width="100px">
        <el-form-item label="仓库" prop="warehouseCode">
          <el-select v-model="outboundForm.warehouseCode" placeholder="选择仓库">
            <el-option label="A库" value="A" />
            <el-option label="B库" value="B" />
          </el-select>
        </el-form-item>
        <el-form-item label="行" prop="row" v-if="!isRemoteMode">
          <el-input-number v-model="outboundForm.row" :min="1" :max="4" />
        </el-form-item>
        <el-form-item label="列" prop="column" v-if="!isRemoteMode">
          <el-input-number v-model="outboundForm.column" :min="1" :max="6" />
        </el-form-item>
        <el-form-item v-if="isRemoteMode">
          <el-alert
            title="远程模式：系统将自动选择第一个有托盘的位置进行出库"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>
        <el-form-item label="出库原因" prop="reason">
          <el-select v-model="outboundForm.reason" placeholder="选择出库原因">
            <el-option label="生产需要" value="PRODUCTION" />
            <el-option label="质量检验" value="QUALITY_CHECK" />
            <el-option label="库存调整" value="INVENTORY_ADJUST" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="outboundForm.remarks" type="textarea" :rows="2" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="outboundDialogVisible = false">取消</el-button>
          <el-button type="success" @click="executeOutbound">确认出库</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 系统日志 -->
    <el-card class="system-log-card">
      <template #header>
        <div class="card-header">
          <span>系统日志</span>
          <el-button size="small" @click="clearSystemLog">清空日志</el-button>
        </div>
      </template>
      <div class="log-content">
        <div 
          v-for="(log, index) in systemLogs" 
          :key="index" 
          class="log-item"
          :class="getLogLevelClass(log.level)"
        >
          <span class="log-time">{{ log.timestamp }}</span>
          <span class="log-level">{{ log.level }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
        <div v-if="systemLogs.length === 0" class="no-logs">
          暂无系统日志
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted, reactive, computed } from 'vue'
import axios from 'axios'
 import { ElMessage, ElMessageBox } from 'element-plus'
 import { Plus, Minus } from '@element-plus/icons-vue'
import deviceModeManager from '../utils/deviceMode.js'

export default {
  name: 'WarehouseStatus',
  setup() {

     const warehouseStatus = ref({})
     const cellDetailsVisible = ref(false)
     const selectedCell = ref(null)
     
     // 从localStorage恢复仓库状态
     const loadWarehouseStatusFromStorage = () => {
       try {
         const stored = localStorage.getItem('warehouseStatus')
         if (stored) {
           warehouseStatus.value = JSON.parse(stored)
         }
       } catch (error) {
         console.error('加载本地仓库状态失败:', error)
       }
     }
     
     // 保存仓库状态到localStorage
     const saveWarehouseStatusToStorage = () => {
       try {
         localStorage.setItem('warehouseStatus', JSON.stringify(warehouseStatus.value))
       } catch (error) {
         console.error('保存本地仓库状态失败:', error)
       }
     }
     

     

    
    // 出入库对话框状态
    const inboundDialogVisible = ref(false)
    const outboundDialogVisible = ref(false)
    
    // 入库表单
    const inboundForm = reactive({
      warehouseCode: 'A',
      row: 1,
      column: 1,
      palletCode: '',
      materialCode: '',
      materialStatus: 'RAW',
      remarks: ''
    })
    
         // 出库表单
     const outboundForm = reactive({
       warehouseCode: 'A',
       row: 1,
       column: 1,
       reason: '',
       remarks: ''
     })
     
     // 可用的托盘和货物数据
     const availablePallets = ref([])
     const availableGoods = ref([])
     
     // 加载可用的托盘数据
     const loadAvailablePallets = async () => {
       try {
         // 优先从托盘分类页面的localStorage获取数据
         const palletData = localStorage.getItem('palletData')
         if (palletData) {
           availablePallets.value = JSON.parse(palletData)
           console.log('从托盘分类页面获取托盘数据:', availablePallets.value)
           return
         }
         
         // 如果localStorage没有数据，尝试从API获取
         const response = await axios.get('/api/warehouse/pallets')
         availablePallets.value = response.data
       } catch (error) {
         console.error('加载托盘数据失败:', error)
         // 如果API失败，使用模拟数据，使用托盘分类页面的实际数据结构
         availablePallets.value = [
           { id: 1, palletNumber: 'P001', palletName: '标准托盘1' },
           { id: 2, palletNumber: 'P002', palletName: '标准托盘2' },
           { id: 3, palletNumber: 'P003', palletName: '标准托盘3' },
           { id: 4, palletNumber: 'P004', palletName: '标准托盘4' },
           { id: 5, palletNumber: 'P005', palletName: '标准托盘5' },
           { id: 6, palletNumber: 'P006', palletName: '标准托盘6' },
           { id: 7, palletNumber: 'P007', palletName: '标准托盘7' },
           { id: 8, palletNumber: 'P008', palletName: '标准托盘8' }
         ]
       }
       console.log('托盘数据已加载:', availablePallets.value)
     }
     
     // 加载可用的货物数据
     const loadAvailableGoods = async () => {
       try {
         // 优先从货物分类页面的localStorage获取数据
         const goodsData = localStorage.getItem('goodsData')
         if (goodsData) {
           availableGoods.value = JSON.parse(goodsData)
           console.log('从货物分类页面获取货物数据:', availableGoods.value)
           return
         }
         
         // 如果localStorage没有数据，尝试从API获取
         const response = await axios.get('/api/warehouse/goods')
         availableGoods.value = response.data
       } catch (error) {
         console.error('加载货物数据失败:', error)
         // 如果API失败，使用模拟数据
         availableGoods.value = [
           { id: 1, goodsNumber: '1000', goodsName: '原材料A' },
           { id: 2, goodsNumber: '1001', goodsName: '原材料B' },
           { id: 3, goodsNumber: '2000', goodsName: '成品A' },
           { id: 4, goodsNumber: '2002', goodsName: '成品B' }
         ]
       }
       console.log('货物数据已加载:', availableGoods.value)
     }
     
    // 检查是否为远程模式
    const isRemoteMode = computed(() => {
      return deviceModeManager.isRemoteMode()
    })

     // 表单验证规则
    const inboundRules = computed(() => {
      const baseRules = {
        warehouseCode: [{ required: true, message: '请选择仓库', trigger: 'change' }],
        palletCode: [{ required: true, message: '请选择托盘', trigger: 'change' }],
        materialCode: [{ required: true, message: '请选择货物', trigger: 'change' }],
        materialStatus: [{ required: true, message: '请选择物料状态', trigger: 'change' }]
      }
      
      // 本地模式才需要验证行列
      if (!isRemoteMode.value) {
        baseRules.row = [{ required: true, message: '请选择行', trigger: 'change' }]
        baseRules.column = [{ required: true, message: '请选择列', trigger: 'change' }]
      }
      
      return baseRules
    })
    
    const outboundRules = computed(() => {
      const baseRules = {
        warehouseCode: [{ required: true, message: '请选择仓库', trigger: 'change' }],
        reason: [{ required: true, message: '请选择出库原因', trigger: 'change' }]
      }
      
      // 本地模式才需要验证行列
      if (!isRemoteMode.value) {
        baseRules.row = [{ required: true, message: '请选择行', trigger: 'change' }]
        baseRules.column = [{ required: true, message: '请选择列', trigger: 'change' }]
      }
      
      return baseRules
    })
    
         // 系统日志
     const systemLogs = ref([])
     
     // 从localStorage恢复系统日志
     const loadSystemLogsFromStorage = () => {
       try {
         const stored = localStorage.getItem('systemLogs')
         if (stored) {
           systemLogs.value = JSON.parse(stored)
         }
       } catch (error) {
         console.error('加载本地系统日志失败:', error)
       }
     }
     
     // 保存系统日志到localStorage
     const saveSystemLogsToStorage = () => {
       try {
         localStorage.setItem('systemLogs', JSON.stringify(systemLogs.value))
       } catch (error) {
         console.error('保存本地系统日志失败:', error)
       }
     }
    

    
         const loadWarehouseStatus = async (warehouseCode, forceLoad = false) => {
       try {
         const response = await axios.get(`/api/warehouse/status/${warehouseCode}`)
         // 只有在强制加载或本地没有该仓库数据时才更新
         if (forceLoad || !warehouseStatus.value[warehouseCode] || Object.keys(warehouseStatus.value[warehouseCode]).length === 0) {
           warehouseStatus.value[warehouseCode] = response.data
         }
       } catch (error) {
         ElMessage.error('加载仓库状态失败')
         console.error(error)
       }
     }
    
    const getCellStatus = (warehouse, row, col) => {
      const key = `${row}-${col}`
      const status = warehouseStatus.value[warehouse]?.grid?.[key]
      return status || {
        hasPallet: false,
        palletCode: null,
        materialCode: null,
        materialStatus: 'EMPTY',
        remarks: ''
      }
    }
    
    // 根据托盘状态设置单元格样式
    const getCellClass = (warehouse, row, col) => {
      const status = getCellStatus(warehouse, row, col)
      return {
        'has-pallet': status.hasPallet,
        'no-pallet': !status.hasPallet
      }
    }
    
    const getMaterialStatusType = (status) => {
      switch (status) {
        case 'RAW': return 'warning'
        case 'FINISHED': return 'success'
        default: return 'info'
      }
    }
    
    const getMaterialStatusText = (status) => {
      switch (status) {
        case 'RAW': return '毛坯'
        case 'FINISHED': return '成品'
        default: return '无料'
      }
    }
    
    const showCellDetails = (warehouse, row, col) => {
      const status = getCellStatus(warehouse, row, col)
      selectedCell.value = {
        warehouse,
        row,
        column: col,
        position: `${warehouse}${row}-${col}`,
        ...status
      }
      cellDetailsVisible.value = true
    }
    
         // 显示入库对话框
     const showInboundDialog = () => {
       // 检查数据是否已加载
       if (availablePallets.value.length === 0 || availableGoods.value.length === 0) {
         ElMessage.warning('托盘或货物数据正在加载中，请稍后再试')
         return
       }
       
       inboundForm.warehouseCode = 'A'
       inboundDialogVisible.value = true
       console.log('显示入库对话框，托盘数据:', availablePallets.value)
       console.log('显示入库对话框，货物数据:', availableGoods.value)
     }
    
    // 显示出库对话框
    const showOutboundDialog = () => {
      outboundForm.warehouseCode = 'A'
      outboundDialogVisible.value = true
    }
    
        // 执行入库操作
    const executeInbound = async () => {
      try {
        const currentMode = deviceModeManager.getMode()
        console.log('开始执行入库操作，当前模式:', currentMode)
        console.log('入库表单数据:', inboundForm)
        
        let response
        
        if (currentMode === 'remote') {
          // 远程模式：检查MES订单或执行自动入库
          console.log('远程模式：检查MES订单')
          
          // 先检查是否有MES入库订单
          const orderResponse = await axios.get('/api/mes-wms/check-orders')
          if (orderResponse.data.success && orderResponse.data.hasInboundOrder) {
            // 有MES入库订单，执行MES入库
            console.log('执行MES入库订单')
            response = await axios.post('/api/mes-wms/execute-inbound')
          } else {
            // 没有MES订单，执行自动入库
            console.log('执行自动入库')
            response = await axios.post('/api/warehouse/auto-inbound', {
              warehouseCode: inboundForm.warehouseCode,
              palletCode: inboundForm.palletCode,
              materialCode: inboundForm.materialCode
            })
          }
          
          // 入库成功后，更新表单中的位置信息
          if (response.data.success) {
            inboundForm.row = response.data.row
            inboundForm.column = response.data.column
            console.log('入库找到位置:', response.data.row, response.data.column)
          }
        } else {
          // 本地模式：手动入库
          console.log('本地模式：执行手动入库')
          response = await axios.post('/api/warehouse/inbound', {
            warehouseCode: inboundForm.warehouseCode,
            row: inboundForm.row,
            column: inboundForm.column,
            palletCode: inboundForm.palletCode,
            materialCode: inboundForm.materialCode
          })
        }
        
        if (response.data.success) {
          ElMessage.success(response.data.message)
          addSystemLog('INFO', response.data.message)
          
          // 入库成功后，重新从MySQL加载仓库状态
          await Promise.all([
            loadWarehouseStatus('A', true),
            loadWarehouseStatus('B', true)
          ])
          
          // 更新货物分类中的位置分布
          updateGoodsLocationDistribution(
            inboundForm.materialCode,
            `${inboundForm.warehouseCode}-${inboundForm.row}-${inboundForm.column}`,
            'add'
          )
          
          // 保存状态到本地存储（作为备份）
          saveWarehouseStatusToStorage()
          
          inboundDialogVisible.value = false
        } else {
          ElMessage.error(response.data.message || '入库失败')
          addSystemLog('ERROR', response.data.message || '入库失败')
        }
        
      } catch (error) {
        console.error('入库操作失败:', error)
        ElMessage.error('入库操作失败: ' + error.message)
        addSystemLog('ERROR', '入库操作失败: ' + error.message)
      }
    }
    
        // 执行出库操作
    const executeOutbound = async () => {
      try {
        const currentMode = deviceModeManager.getMode()
        console.log('开始执行出库操作，当前模式:', currentMode)
        console.log('出库表单数据:', outboundForm)
        
        let response
        
        if (currentMode === 'remote') {
          // 远程模式：检查MES订单或执行自动出库
          console.log('远程模式：检查MES订单')
          
          // 先检查是否有MES出库订单
          const orderResponse = await axios.get('/api/mes-wms/check-orders')
          if (orderResponse.data.success && orderResponse.data.hasOutboundOrder) {
            // 有MES出库订单，执行MES出库
            console.log('执行MES出库订单')
            response = await axios.post('/api/mes-wms/execute-outbound')
          } else {
            // 没有MES订单，执行自动出库
            console.log('执行自动出库')
            response = await axios.post('/api/warehouse/auto-outbound', {
              warehouseCode: outboundForm.warehouseCode
            })
          }
          
          // 出库成功后，更新表单中的位置信息
          if (response.data.success) {
            outboundForm.row = response.data.row
            outboundForm.column = response.data.column
            console.log('出库找到位置:', response.data.row, response.data.column)
          }
        } else {
          // 本地模式：手动出库
          console.log('本地模式：执行手动出库')
          response = await axios.post('/api/warehouse/outbound', {
            warehouseCode: outboundForm.warehouseCode,
            row: outboundForm.row,
            column: outboundForm.column
          })
        }
        
        if (response.data.success) {
          ElMessage.success(response.data.message)
          addSystemLog('INFO', response.data.message)
          
          // 出库成功后，重新从MySQL加载仓库状态
          await Promise.all([
            loadWarehouseStatus('A', true),
            loadWarehouseStatus('B', true)
          ])
          
          // 保存状态到本地存储（作为备份）
          saveWarehouseStatusToStorage()
          
          outboundDialogVisible.value = false
        } else {
          ElMessage.error(response.data.message || '出库失败')
          addSystemLog('ERROR', response.data.message || '出库失败')
        }
        
      } catch (error) {
        console.error('出库操作失败:', error)
        ElMessage.error('出库操作失败: ' + error.message)
        addSystemLog('ERROR', '出库操作失败: ' + error.message)
      }
    }
    
    // 添加系统日志
    const addSystemLog = (level, message) => {
      const now = new Date()
      const timestamp = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
      
      systemLogs.value.unshift({
        timestamp,
        level,
        message
      })
      
      // 限制日志数量，最多保留100条
      if (systemLogs.value.length > 100) {
        systemLogs.value = systemLogs.value.slice(0, 100)
      }
      
      // 保存到localStorage
      saveSystemLogsToStorage()
    }
    
    // 清空系统日志
    const clearSystemLog = () => {
      systemLogs.value = []
      // 保存清空后的状态到localStorage
      saveSystemLogsToStorage()
      ElMessage.success('系统日志已清空')
    }
    
    // 更新单元格状态
    const updateCellStatus = (warehouseCode, row, column, hasPallet) => {
      const key = `${row}-${column}`
      if (warehouseStatus.value[warehouseCode]?.grid?.[key]) {
        warehouseStatus.value[warehouseCode].grid[key].hasPallet = hasPallet
        if (!hasPallet) {
          warehouseStatus.value[warehouseCode].grid[key].palletCode = null
          warehouseStatus.value[warehouseCode].grid[key].materialCode = null
          warehouseStatus.value[warehouseCode].grid[key].materialStatus = 'EMPTY'
        }
        console.log(`更新${warehouseCode}库${row}行${column}列状态: ${hasPallet ? '有托盘' : '无托盘'}`)
      }
    }

    // 获取出库原因文本
    const getOutboundReasonText = (reason) => {
      const reasonMap = {
        'PRODUCTION': '生产需要',
        'QUALITY_CHECK': '质量检验',
        'INVENTORY_ADJUST': '库存调整',
        'OTHER': '其他'
      }
      return reasonMap[reason] || '未知'
    }

    // 自动查找空位（远程模式使用）
    const findEmptyPosition = (warehouseCode) => {
      console.log(`在${warehouseCode}库中查找空位...`)
      
      // 从第一个位置开始检测
      for (let row = 1; row <= 4; row++) {
        for (let col = 1; col <= 6; col++) {
          const key = `${row}-${col}`
          const status = warehouseStatus.value[warehouseCode]?.grid?.[key]
          
          // 如果该位置没有托盘，则返回这个位置
          if (!status || !status.hasPallet) {
            console.log(`找到空位：${warehouseCode}库${row}行${col}列`)
            return { row, col }
          }
        }
      }
      
      console.log(`${warehouseCode}库已满，没有空位`)
      return null
    }

    // 自动查找有托盘的位置（远程模式出库使用）
    const findOccupiedPosition = (warehouseCode) => {
      console.log(`在${warehouseCode}库中查找有托盘的位置...`)
      
      // 从第一个位置开始检测
      for (let row = 1; row <= 4; row++) {
        for (let col = 1; col <= 6; col++) {
          const key = `${row}-${col}`
          const status = warehouseStatus.value[warehouseCode]?.grid?.[key]
          
          // 如果该位置有托盘，则返回这个位置
          if (status && status.hasPallet) {
            console.log(`找到有托盘的位置：${warehouseCode}库${row}行${col}列`)
            return { row, col }
          }
        }
      }
      
      console.log(`${warehouseCode}库为空，没有托盘`)
      return null
    }
    
    // 获取日志级别样式类
    const getLogLevelClass = (level) => {
      return `log-${level.toLowerCase()}`
    }
    
    
             // 更新货物分类中的位置分布
    const updateGoodsLocationDistribution = (materialCode, location, action) => {
      console.log(`尝试更新货物${materialCode}位置分布：${action === 'add' ? '新增' : '移除'}${location}`)
      
      // 直接更新本地存储中的位置分布，不依赖货物分类页面
      try {
        const locationDistributionKey = 'goodsLocationDistribution'
        let locationDistribution = JSON.parse(localStorage.getItem(locationDistributionKey) || '{}')
        
        if (!locationDistribution[materialCode]) {
          locationDistribution[materialCode] = []
        }
        
        if (action === 'add') {
          // 入库：添加位置
          if (!locationDistribution[materialCode].includes(location)) {
            locationDistribution[materialCode].push(location)
            console.log(`已添加位置${location}到货物${materialCode}`)
          }
        } else if (action === 'remove') {
          // 出库：移除位置
          const index = locationDistribution[materialCode].indexOf(location)
          if (index > -1) {
            locationDistribution[materialCode].splice(index, 1)
            console.log(`已移除位置${location}从货物${materialCode}`)
          }
        } else if (action === 'clear') {
          // 清空：移除所有位置
          locationDistribution[materialCode] = []
        }
        
        // 保存到localStorage
        localStorage.setItem(locationDistributionKey, JSON.stringify(locationDistribution))
        
        // 尝试调用货物分类页面的全局方法（如果存在）
        if (window.updateGoodsLocationDistribution) {
          window.updateGoodsLocationDistribution(materialCode, location, action)
          console.log('货物分类页面同步成功')
        }
        
        addSystemLog('INFO', `货物${materialCode}位置分布已更新：${action === 'add' ? '新增' : '移除'}${location}`)
        console.log('位置分布更新成功')
        
      } catch (error) {
        console.error('更新位置分布失败:', error)
        addSystemLog('ERROR', `更新位置分布失败：${materialCode} - ${error.message}`)
      }
    }
    
             onMounted(async () => {
      // 先加载可用的托盘和货物数据
      await Promise.all([
        loadAvailablePallets(),
        loadAvailableGoods()
      ])
      
      // 恢复系统日志
      loadSystemLogsFromStorage()
      
      // 添加设备模式变化监听器
      const modeChangeHandler = (mode) => {
        console.log('仓储分类页面收到模式变化通知:', mode)
        addSystemLog('INFO', `设备模式已切换到: ${mode === 'remote' ? '远程模式' : '本地模式'}`)
      }
      deviceModeManager.addListener(modeChangeHandler)
      
      // 每次都从MySQL获取最新数据
      Promise.all([
        loadWarehouseStatus('A', true).catch(() => {}),
        loadWarehouseStatus('B', true).catch(() => {})
      ]).then(() => {
        saveWarehouseStatusToStorage();
      }).catch(() => {
        // 如果API失败，确保localStorage仍然保存为空
        saveWarehouseStatusToStorage();
      });
    })
    
    return {
      warehouseStatus,
      cellDetailsVisible,
      selectedCell,
      inboundDialogVisible,
      outboundDialogVisible,
      inboundForm,
      outboundForm,
      inboundRules,
      outboundRules,
      systemLogs,
      availablePallets,
      availableGoods,
      isRemoteMode,
      loadWarehouseStatus,
      getCellStatus,
      getCellClass,
      getMaterialStatusType,
      getMaterialStatusText,
      showCellDetails,
      showInboundDialog,
      showOutboundDialog,
      executeInbound,
      executeOutbound,
      clearSystemLog,
      getLogLevelClass,
      updateGoodsLocationDistribution,
      saveWarehouseStatusToStorage,
      saveSystemLogsToStorage,
      loadSystemLogsFromStorage,
      findEmptyPosition,
      findOccupiedPosition,
      updateCellStatus
    }
  }
}
</script>

<style scoped>
.warehouse-status {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 20px 0;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 24px;
  font-weight: bold;
}

.content-container {
  width: 100%;
}

.warehouse-grid {
  display: flex;
  gap: 40px;
}

.warehouse-section {
  flex: 1;
}

.warehouse-section h3 {
  text-align: center;
  margin-bottom: 20px;
  color: #34495e;
  font-size: 18px;
}

.grid-container {
  border: 2px solid #bdc3c7;
  border-radius: 8px;
  overflow: hidden;
}

.grid-row {
  display: flex;
}

.grid-cell {
  flex: 1;
  height: 65px;
  border: 1px solid #e9ecef;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  background-color: #ffffff;
}

.grid-cell:hover {
  background-color: #f8f9fa;
  transform: scale(1.02);
  border-color: #007bff;
  box-shadow: 0 2px 8px rgba(0, 123, 255, 0.15);
}

.grid-cell.has-pallet {
  background-color: #e8f5e8;
  border-color: #27ae60;
}

.grid-cell.has-pallet:hover {
  background-color: #d4edda;
  border-color: #28a745;
}

.grid-cell.no-pallet {
  background-color: #ffffff;
  border-color: #e9ecef;
}

.cell-coordinate {
  font-size: 12px;
  color: #495057;
  font-weight: 600;
  margin-bottom: 8px;
  text-align: center;
}

.cell-indicator {
  display: flex;
  justify-content: center;
  align-items: center;
}

.indicator-box {
  width: 18px;
  height: 18px;
  border: 2px solid #dee2e6;
  border-radius: 4px;
  background-color: #f8f9fa;
  transition: all 0.2s ease;
}

.grid-cell:hover .indicator-box {
  border-color: #007bff;
  background-color: #e3f2fd;
  transform: scale(1.1);
}

.indicator-check {
  width: 18px;
  height: 18px;
  border: 2px solid #27ae60;
  border-radius: 4px;
  background-color: #27ae60;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  transition: all 0.2s ease;
}

.grid-cell.has-pallet:hover .indicator-check {
  background-color: #28a745;
  border-color: #28a745;
  transform: scale(1.1);
}

.cell-details {
  padding: 20px 0;
}

.header-actions {
  display: flex;
  gap: 15px;
  align-items: center;
}



.system-log-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
