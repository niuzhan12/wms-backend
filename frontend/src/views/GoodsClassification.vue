<template>
  <div class="goods-classification">
    <div class="page-header">
      <h2>货物分类</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加货物
        </el-button>
        <el-button type="success" @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>
    
    <div class="table-container">
      <el-table :data="goods" border stripe style="width: 100%">
             <el-table-column label="序号" width="80">
         <template #default="scope">
           {{ scope.$index + 1 }}
         </template>
       </el-table-column>
      <el-table-column prop="goodsNumber" label="货物编号" width="120" />
      <el-table-column prop="goodsName" label="货物名称" width="200" />
      <el-table-column prop="qrCode" label="二维码" width="150" />
      <el-table-column prop="materialStatus" label="物料状态" width="100">
        <template #default="scope">
          <el-tag :type="getMaterialStatusType(scope.row.materialStatus)">
            {{ getMaterialStatusText(scope.row.materialStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="80" />
      <el-table-column prop="locationDistribution" label="位置分布" width="200">
        <template #default="scope">
          <div class="location-tags">
            <el-tag 
              v-for="location in getLocationDistribution(scope.row.goodsNumber)" 
              :key="location"
              size="small"
              type="success"
              style="margin: 2px"
            >
              {{ location }}
            </el-tag>
            <span v-if="getLocationDistribution(scope.row.goodsNumber).length === 0" class="no-location">
              无位置信息
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="editGoods(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deleteGoods(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>
    
    <!-- 添加/编辑货物对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="goodsForm" :rules="rules" ref="goodsFormRef" label-width="100px">
        <el-form-item label="货物编号" prop="goodsNumber">
          <el-input v-model="goodsForm.goodsNumber" placeholder="请输入货物编号" />
        </el-form-item>
        <el-form-item label="货物名称" prop="goodsName">
          <el-input v-model="goodsForm.goodsName" placeholder="请输入货物名称" />
        </el-form-item>
        <el-form-item label="二维码" prop="qrCode">
          <el-input v-model="goodsForm.qrCode" placeholder="请输入二维码" />
        </el-form-item>


        <el-form-item label="物料状态" prop="materialStatus">
          <el-select v-model="goodsForm.materialStatus" placeholder="选择物料状态">
            <el-option label="毛坯" value="RAW" />
            <el-option label="成品" value="FINISHED" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量" prop="quantity">
          <el-input-number v-model="goodsForm.quantity" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveGoods">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, reactive } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
 import { Plus, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'GoodsClassification',
          components: {
      Plus,
      Refresh
    },
  setup() {
         const goods = ref([])
     
     // 从localStorage恢复货物数据
     const loadGoodsFromStorage = () => {
       try {
         const stored = localStorage.getItem('goodsData')
         if (stored) {
           goods.value = JSON.parse(stored)
         }
       } catch (error) {
         console.error('加载本地货物数据失败:', error)
       }
     }
     
     // 保存货物数据到localStorage
     const saveGoodsToStorage = () => {
       try {
         localStorage.setItem('goodsData', JSON.stringify(goods.value))
       } catch (error) {
         console.error('保存本地货物数据失败:', error)
       }
     }
    const dialogVisible = ref(false)
    const dialogTitle = ref('添加货物')
    const goodsFormRef = ref(null)
    const isEdit = ref(false)
    
    const goodsForm = reactive({
      id: null,
      goodsNumber: '',
      goodsName: '',
      qrCode: '',

      materialStatus: 'RAW',
      quantity: 1
    })
    
    const rules = {
      goodsNumber: [
        { required: true, message: '请输入货物编号', trigger: 'blur' }
      ],
      goodsName: [
        { required: true, message: '请输入货物名称', trigger: 'blur' }
      ],
      qrCode: [
        { required: true, message: '请输入二维码', trigger: 'blur' }
      ],

      materialStatus: [
        { required: true, message: '请选择物料状态', trigger: 'change' }
      ]
    }
    
             // 仓库状态数据（从MySQL获取）
    const warehouseStatus = ref({})
    
    // 全局位置分布状态（与仓储分类页面同步）
    const globalLocationDistribution = ref({})
    
    // 获取货物的位置分布
    const getLocationDistribution = (goodsNumber) => {
      // 只从MySQL数据中计算位置分布，不使用localStorage
      const locations = []
      
      // 遍历仓库状态，找到包含该货物的位置
      if (warehouseStatus.value) {
        Object.keys(warehouseStatus.value).forEach(warehouseCode => {
          const warehouse = warehouseStatus.value[warehouseCode]
          if (warehouse && warehouse.grid) {
            Object.keys(warehouse.grid).forEach(position => {
              const cell = warehouse.grid[position]
              if (cell.materialCode === goodsNumber) {
                const [row, col] = position.split('-')
                locations.push(`${warehouseCode}-${row}-${col}`)
              }
            })
          }
        })
      }
      
      // 直接返回MySQL中的位置信息，不读取localStorage
      return locations
    }
    
         // 更新货物的位置分布（供仓储分类页面调用）
     const updateLocationDistribution = (goodsNumber, location, action) => {
       console.log(`货物分类页面收到更新请求：${goodsNumber} - ${action} - ${location}`)
       
       // 同时更新内存状态和localStorage
       if (!globalLocationDistribution.value[goodsNumber]) {
         globalLocationDistribution.value[goodsNumber] = []
       }
       
       if (action === 'add') {
         // 入库：添加位置
         if (!globalLocationDistribution.value[goodsNumber].includes(location)) {
           globalLocationDistribution.value[goodsNumber].push(location)
           console.log(`已添加位置${location}到货物${goodsNumber}`)
         } else {
           console.log(`位置${location}已存在于货物${goodsNumber}中`)
         }
       } else if (action === 'remove') {
         // 出库：移除位置
         const index = globalLocationDistribution.value[goodsNumber].indexOf(location)
         if (index > -1) {
           globalLocationDistribution.value[goodsNumber].splice(index, 1)
           console.log(`已移除位置${location}从货物${goodsNumber}`)
         } else {
           console.log(`位置${location}不存在于货物${goodsNumber}中`)
         }
       } else if (action === 'clear') {
         // 清空：移除所有位置
         globalLocationDistribution.value[goodsNumber] = []
         console.log(`已清空货物${goodsNumber}的所有位置`)
       }
       
       // 强制更新视图
       globalLocationDistribution.value = { ...globalLocationDistribution.value }
       
       // 同步到localStorage
       try {
         const locationDistributionKey = 'goodsLocationDistribution'
         localStorage.setItem(locationDistributionKey, JSON.stringify(globalLocationDistribution.value))
         console.log('位置分布数据已同步到localStorage')
       } catch (error) {
         console.error('同步到localStorage失败:', error)
       }
       
       console.log(`货物${goodsNumber}的当前位置分布:`, globalLocationDistribution.value[goodsNumber])
     }
    
         // 将方法暴露到全局，供仓储分类页面调用
     window.updateGoodsLocationDistribution = updateLocationDistribution
     console.log('全局方法已暴露:', !!window.updateGoodsLocationDistribution)
    
             // 加载仓库状态数据
    const loadWarehouseStatus = async () => {
      try {
        const [statusA, statusB] = await Promise.all([
          axios.get('/api/warehouse/status/A'),
          axios.get('/api/warehouse/status/B')
        ])
        
        warehouseStatus.value = {
          'A': statusA.data,
          'B': statusB.data
        }
        
        console.log('从MySQL获取到仓库状态:', warehouseStatus.value)
      } catch (error) {
        console.error('加载仓库状态失败:', error)
      }
    }
    
    const loadGoods = async () => {
      try {
        const response = await axios.get('/api/warehouse/goods')
        // 直接使用API数据，覆盖本地存储
        goods.value = response.data
        // 保存到本地存储作为备份
        saveGoodsToStorage()
        
        // 为每个货物初始化位置分布（如果还没有的话）
        goods.value.forEach(good => {
          if (!globalLocationDistribution.value[good.goodsNumber]) {
            globalLocationDistribution.value[good.goodsNumber] = []
          }
        })
        
        console.log('从MySQL获取到最新数据:', goods.value)
      } catch (error) {
        ElMessage.error('加载货物数据失败，使用本地备份数据')
        console.error('API调用失败:', error)
        // 如果API失败，才使用本地存储作为备份
        loadGoodsFromStorage()
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
        default: return '未知'
      }
    }
    
    const showAddDialog = () => {
      isEdit.value = false
      dialogTitle.value = '添加货物'
      resetForm()
      dialogVisible.value = true
    }
    
    const editGoods = (row) => {
      isEdit.value = true
      dialogTitle.value = '编辑货物'
      Object.assign(goodsForm, row)
      dialogVisible.value = true
    }
    
    const deleteGoods = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除这个货物吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
                 // 临时解决方案：从前端数组中移除货物
         const index = goods.value.findIndex(g => g.id === row.id)
         if (index > -1) {
           goods.value.splice(index, 1)
           // 保存删除后的状态到本地存储
           saveGoodsToStorage()
           ElMessage.success('删除成功')
         } else {
           ElMessage.error('货物不存在')
         }
        // 暂时注释掉重新加载，等后端API完善后再启用
        // loadGoods()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }
    
        const saveGoods = async () => {
      try {
        await goodsFormRef.value.validate()
        
        if (isEdit.value) {
          // 调用MySQL API更新货物
          try {
            await axios.put(`/api/warehouse/goods/${goodsForm.id}`, goodsForm)
            
            // 更新成功后，重新从MySQL加载数据
            await loadGoods()
            ElMessage.success('更新成功')
          } catch (apiError) {
            console.error('API更新失败:', apiError)
            ElMessage.error('更新失败：API调用失败')
          }
        } else {
          // 调用MySQL API添加新货物
          try {
            await axios.post('/api/warehouse/goods', goodsForm)
            
            // 添加成功后，重新从MySQL加载数据
            await loadGoods()
            ElMessage.success('添加成功')
          } catch (apiError) {
            console.error('API添加失败:', apiError)
            ElMessage.error('添加失败：API调用失败')
          }
        }
        
        dialogVisible.value = false
      } catch (error) {
        console.error(error)
        ElMessage.error('操作失败')
      }
    }
    
    const resetForm = () => {
      Object.assign(goodsForm, {
        id: null,
        goodsNumber: '',
        goodsName: '',
        qrCode: '',

        materialStatus: 'RAW',
        quantity: 1
      })
    }
    
    const formatLocationDistribution = (locations) => {
      if (!locations) return '无位置信息'
      if (typeof locations === 'string') {
        return locations.split(',').map(loc => loc.trim()).join(', ')
      }
      return locations.join(', ')
    }
    
         const refreshData = () => {
       loadGoods()
     }
     

    
    
    
         // 同步现有仓库状态到位置分布
     const syncExistingWarehouseStatus = () => {
       try {
         const warehouseStatus = localStorage.getItem('warehouseStatus')
         if (warehouseStatus) {
           const status = JSON.parse(warehouseStatus)
           
           // 遍历所有仓库状态，找到有托盘的位置
           for (const warehouseCode in status) {
             if (status[warehouseCode]?.grid) {
               for (const key in status[warehouseCode].grid) {
                 const cell = status[warehouseCode].grid[key]
                 if (cell.hasPallet && cell.materialCode) {
                   // 解析行列信息
                   const [row, col] = key.split('-')
                   const location = `${warehouseCode}-${row}-${col}`
                   
                   // 更新位置分布
                   updateLocationDistribution(cell.materialCode, location, 'add')
                 }
               }
             }
           }
           
           console.log('已同步现有仓库状态到位置分布:', globalLocationDistribution.value)
         }
       } catch (error) {
         console.error('同步仓库状态失败:', error)
       }
     }
     
     onMounted(async () => {
      // 同时加载货物数据和仓库状态
      await Promise.all([
        loadGoods(),
        loadWarehouseStatus()
      ])
      
      // 不再从localStorage加载位置分布数据，只使用MySQL数据
      console.log('位置分布数据完全基于MySQL，不使用localStorage缓存')
      
      // 延迟同步现有仓库状态，确保页面完全加载
      setTimeout(() => {
        syncExistingWarehouseStatus()
      }, 500)
    })
    
        return {
      goods,
      warehouseStatus,
      dialogVisible,
      dialogTitle,
      goodsFormRef,
      goodsForm,
      rules,
      getMaterialStatusType,
      getMaterialStatusText,
      formatLocationDistribution,
      getLocationDistribution,
      updateLocationDistribution,
      showAddDialog,
      editGoods,
      deleteGoods,
      saveGoods,
      refreshData,
      saveGoodsToStorage,
      loadGoodsFromStorage,
      syncExistingWarehouseStatus
    }
  }
}
</script>

<style scoped>
.goods-classification {
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

.header-actions {
  display: flex;
  gap: 10px;
}

.table-container {
  width: 100%;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.location-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.no-location {
  color: #909399;
  font-style: italic;
  font-size: 12px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
}
</style>
