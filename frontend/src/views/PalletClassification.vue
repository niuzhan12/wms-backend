<template>
  <div class="pallet-classification">
    <div class="page-header">
      <h2>托盘分类</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showAddDialog">
          <el-icon><Plus /></el-icon>
          添加托盘
        </el-button>
      </div>
    </div>
    
    <div class="table-container">
      <el-table :data="pallets" border stripe style="width: 100%">
      <el-table-column label="序号" width="80">
        <template #default="scope">
          {{ scope.$index + 1 }}
        </template>
      </el-table-column>
      <el-table-column prop="palletNumber" label="托盘编号" width="120" />
      <el-table-column prop="palletName" label="托盘名称" width="150" />
      <el-table-column prop="qrCode" label="二维码" width="150" />
      <el-table-column prop="row" label="行" width="80" />
      <el-table-column prop="column" label="列" width="80" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-button size="small" @click="editPallet(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="deletePallet(scope.row)">删除</el-button>
        </template>
      </el-table-column>
      </el-table>
    </div>
    
    <!-- 添加/编辑托盘对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
    >
      <el-form :model="palletForm" :rules="rules" ref="palletFormRef" label-width="100px">
        <el-form-item label="托盘编号" prop="palletNumber">
          <el-input v-model="palletForm.palletNumber" placeholder="请输入托盘编号" />
        </el-form-item>
        <el-form-item label="托盘名称" prop="palletName">
          <el-input v-model="palletForm.palletName" placeholder="请输入托盘名称" />
        </el-form-item>
        <el-form-item label="二维码" prop="qrCode">
          <el-input v-model="palletForm.qrCode" placeholder="请输入二维码" />
        </el-form-item>
        <el-form-item label="仓库" prop="warehouseCode">
          <el-select v-model="palletForm.warehouseCode" placeholder="选择仓库">
            <el-option label="A库" value="A" />
            <el-option label="B库" value="B" />
          </el-select>
        </el-form-item>
        <el-form-item label="行" prop="row">
          <el-input-number v-model="palletForm.row" :min="1" :max="4" />
        </el-form-item>
        <el-form-item label="列" prop="column">
          <el-input-number v-model="palletForm.column" :min="1" :max="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePallet">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, reactive } from 'vue'
import axios from 'axios'
 import { ElMessage, ElMessageBox } from 'element-plus'
 import { Plus } from '@element-plus/icons-vue'


 export default {
   name: 'PalletClassification',
   components: {
     Plus
   },
   setup() {
    const pallets = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('添加托盘')
    const palletFormRef = ref(null)
    const isEdit = ref(false)
    
    const palletForm = reactive({
      id: null,
      palletNumber: '',
      palletName: '',
      qrCode: '',
      warehouseCode: '',
      row: 1,
      column: 1
    })
    
    const rules = {
      palletNumber: [
        { required: true, message: '请输入托盘编号', trigger: 'blur' }
      ],
      palletName: [
        { required: true, message: '请输入托盘名称', trigger: 'blur' }
      ],
      qrCode: [
        { required: true, message: '请输入二维码', trigger: 'blur' }
      ],
      warehouseCode: [
        { required: true, message: '请选择仓库', trigger: 'change' }
      ]
    }
    
    // 从localStorage恢复托盘数据（仅作为备用）
    const loadPalletsFromStorage = () => {
      try {
        const stored = localStorage.getItem('palletData')
        if (stored) {
          pallets.value = JSON.parse(stored)
        }
      } catch (error) {
        console.error('加载本地托盘数据失败:', error)
      }
    }
    
    // 保存托盘数据到localStorage（仅作为备份）
    const savePalletsToStorage = () => {
      try {
        localStorage.setItem('palletData', JSON.stringify(pallets.value))
      } catch (error) {
        console.error('保存本地托盘数据失败:', error)
      }
    }
    
             const loadPallets = async () => {
      try {
        const response = await axios.get('/api/warehouse/pallets')
        // 直接使用API数据，覆盖本地存储
        pallets.value = response.data
        // 保存到本地存储作为备份
        savePalletsToStorage()
        
        console.log('从MySQL获取到最新托盘数据:', pallets.value)
      } catch (error) {
        ElMessage.error('加载托盘数据失败，使用本地备份数据')
        console.error('API调用失败:', error)
        // 如果API失败，才使用本地存储作为备份
        loadPalletsFromStorage()
      }
    }
     
     const showAddDialog = () => {
       isEdit.value = false
       dialogTitle.value = '添加托盘'
       resetForm()
       dialogVisible.value = true
     }
     
     const editPallet = (row) => {
      isEdit.value = true
      dialogTitle.value = '编辑托盘'
      Object.assign(palletForm, row)
      dialogVisible.value = true
    }
    
    const deletePallet = async (row) => {
      try {
        await ElMessageBox.confirm('确定要删除这个托盘吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 调用MySQL API删除托盘
        try {
          await axios.delete(`/api/warehouse/pallets/${row.id}`)
          
          // 删除成功后，重新从MySQL加载数据
          await loadPallets()
          ElMessage.success('删除成功')
        } catch (apiError) {
          console.error('API删除失败:', apiError)
          ElMessage.error('删除失败：API调用失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }
    
    const savePallet = async () => {
      try {
        await palletFormRef.value.validate()
        
        if (isEdit.value) {
          // 调用MySQL API更新托盘
          try {
            await axios.put(`/api/warehouse/pallets/${palletForm.id}`, palletForm)
            
            // 更新成功后，重新从MySQL加载数据
            await loadPallets()
            ElMessage.success('更新成功')
          } catch (apiError) {
            console.error('API更新失败:', apiError)
            ElMessage.error('更新失败：API调用失败')
          }
        } else {
          // 调用MySQL API添加新托盘
          try {
            await axios.post('/api/warehouse/pallets', palletForm)
            
            // 添加成功后，重新从MySQL加载数据
            await loadPallets()
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
      Object.assign(palletForm, {
        id: null,
        palletNumber: '',
        palletName: '',
        qrCode: '',
        warehouseCode: '',
        row: 1,
        column: 1
      })
    }
    
    
    
             onMounted(() => {
      // 每次都从MySQL获取最新数据
      loadPallets()
    })
    
         return {
       pallets,
       dialogVisible,
       dialogTitle,
       palletFormRef,
       palletForm,
       rules,
       showAddDialog,
       editPallet,
       deletePallet,
       savePallet,
       savePalletsToStorage
     }
  }
}
</script>

<style scoped>
.pallet-classification {
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
</style>
