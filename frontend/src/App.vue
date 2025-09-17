<template>
  <div id="app">
    <!-- 登录页面 -->
    <router-view v-if="$route.name === 'Login'" />
    
    <!-- 主系统界面 -->
    <el-container v-else>
      <!-- 顶部菜单栏 -->
      <el-header height="60px" class="main-header">
        <div class="header-content">
          <h1 class="system-title">仓储软件系统 - 在线模式</h1>
          <div class="header-right">
            <div class="user-info">
              <span class="user-role">{{ isAdmin ? '管理员' : '普通用户' }}</span>
            </div>
            <div class="main-menu">
              <el-button type="text" @click="showSystemMenu">系统菜单</el-button>
              <el-button type="text" @click="showNumberSettings">编号设置</el-button>
              <el-button type="text" @click="showCommunicationSettings">通讯设置</el-button>
              <el-button type="text" @click="showResetRun">复位运行</el-button>
              <el-button type="text" @click="showHelp">帮助</el-button>
              <el-button type="text" @click="handleLogout" class="logout-btn">退出登录</el-button>
            </div>
          </div>
        </div>
      </el-header>
      
      <el-container>
        <!-- 左侧导航栏 -->
        <el-aside width="250px" class="main-aside">
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            @select="handleMenuSelect"
            background-color="#2c3e50"
            text-color="#fff"
            active-text-color="#409EFF">
            
            <el-menu-item index="warehouse-status">
              <el-icon><Grid /></el-icon>
              <span>仓储分类</span>
            </el-menu-item>
            
            <!-- 管理员专用菜单 -->
            <template v-if="isAdmin">
              <el-menu-item index="pallet-classification">
                <el-icon><Box /></el-icon>
                <span>托盘分类</span>
              </el-menu-item>
              
              <el-menu-item index="goods-classification">
                <el-icon><Goods /></el-icon>
                <span>货物分类</span>
              </el-menu-item>
              
              <el-menu-item index="device-status">
                <el-icon><Monitor /></el-icon>
                <span>设备状态</span>
              </el-menu-item>
              
              <el-menu-item index="debug">
                <el-icon><Tools /></el-icon>
                <span>调试</span>
              </el-menu-item>
              
              <el-menu-item index="modbus-test">
                <el-icon><Connection /></el-icon>
                <span>Modbus测试</span>
              </el-menu-item>
              
              <el-menu-item index="mes-wms-monitor">
                <el-icon><Monitor /></el-icon>
                <span>MES-WMS监控</span>
              </el-menu-item>
              
              <el-menu-item index="stacker-monitor">
                <el-icon><Van /></el-icon>
                <span>堆垛机监控</span>
              </el-menu-item>
              
            </template>
          </el-menu>
        </el-aside>
        
        <!-- 主内容区域 -->
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Grid, Box, Goods, Monitor, Tools, Van } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authService } from './services/auth'

export default {
  name: 'App',
  components: {
    Grid,
    Box,
    Goods,
    Monitor,
    Tools
  },
  setup() {
    const router = useRouter()
    const activeMenu = ref('warehouse-status')
    const userInfo = ref({})
    
    // 计算属性：是否为管理员
    const isAdmin = computed(() => {
      const role = localStorage.getItem('role')
      const username = localStorage.getItem('username')
      console.log('=== 权限检查 ===')
      console.log('当前用户:', username)
      console.log('角色:', role)
      console.log('角色类型:', typeof role)
      console.log('角色长度:', role ? role.length : 'null')
      console.log('角色 === "ADMIN":', role === 'ADMIN')
      console.log('角色 === "USER":', role === 'USER')
      
      // 严格检查：只有ADMIN才是管理员
      const admin = role === 'ADMIN'
      console.log('是否为管理员:', admin)
      console.log('================')
      
      return admin
    })
    
    // 初始化用户信息
    const initUserInfo = () => {
      userInfo.value = authService.getUserInfo()
      console.log('用户信息已初始化:', userInfo.value)
    }
    
    // 处理菜单选择
    const handleMenuSelect = (index) => {
      // 检查权限
      if (index !== 'warehouse-status' && !isAdmin.value) {
        ElMessage.error('您没有权限访问此功能')
        return
      }
      
      activeMenu.value = index
      router.push(`/${index}`)
    }
    
    // 处理登出
    const handleLogout = async () => {
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        authService.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch {
        // 用户取消
      }
    }
    
    const showSystemMenu = () => {
      ElMessage.info('系统菜单功能')
    }
    
    const showNumberSettings = () => {
      ElMessage.info('编号设置功能')
    }
    
    const showCommunicationSettings = () => {
      ElMessage.info('通讯设置功能')
    }
    
    const showResetRun = () => {
      ElMessage.info('复位运行功能')
    }
    
    const showHelp = () => {
      ElMessage.info('帮助功能')
    }
    
    onMounted(() => {
      initUserInfo()
      console.log('组件已挂载，当前isAdmin状态:', isAdmin.value)
    })
    
    return {
      activeMenu,
      userInfo,
      isAdmin,
      handleMenuSelect,
      handleLogout,
      showSystemMenu,
      showNumberSettings,
      showCommunicationSettings,
      showResetRun,
      showHelp
    }
  }
}
</script>

<style>
#app {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  height: 100vh;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

body {
  margin: 0;
  padding: 0;
  overflow: hidden;
}

/* 全局样式重置 */
* {
  box-sizing: border-box;
}

/* 确保Element Plus组件不会影响布局 */
.el-container {
  height: calc(100vh - 60px);
  display: flex;
}

.el-aside {
  overflow: visible !important;
}

.el-menu {
  overflow: visible !important;
}

.main-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  height: 60px;
  line-height: 60px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
  padding: 0 20px;
  min-width: 0;
  overflow: hidden;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
  flex-wrap: nowrap;
  min-width: 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
  font-size: 14px;
  min-width: 100px;
  padding: 0 15px;
  flex-shrink: 0;
  white-space: nowrap;
}

.user-role {
  color: white;
  font-weight: bold;
  font-size: 15px;
}

.system-title {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
  line-height: 60px;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  color: white;
}

.main-menu {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
  min-width: 0;
  flex-shrink: 1;
}

.main-menu .el-button {
  color: white;
  margin: 0;
  padding: 6px 10px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 1;
}

.main-menu .el-button:hover {
  color: #409EFF;
}

.logout-btn:hover {
  color: #ff6b6b !important;
}

.main-aside {
  background-color: #2c3e50;
  border-right: 1px solid #34495e;
  height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden;
  width: 250px;
  flex-shrink: 0;
}

/* 隐藏滚动条但保持滚动功能 */
.main-aside::-webkit-scrollbar {
  width: 0px;
  background: transparent;
}

.main-aside::-webkit-scrollbar-thumb {
  background: transparent;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
  width: 100%;
  overflow: visible;
}

/* 隐藏Element Plus菜单的滚动指示器 */
.sidebar-menu .el-menu {
  overflow: visible !important;
}

.sidebar-menu .el-menu::-webkit-scrollbar {
  display: none;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  height: calc(100vh - 60px);
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  box-sizing: border-box;
}

.sidebar-menu .el-menu-item {
  height: 60px;
  line-height: 60px;
  margin: 5px 10px;
  border-radius: 8px;
  transition: all 0.3s ease;
  padding: 0 20px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-menu .el-menu-item:hover {
  background-color: #34495e !important;
  transform: translateX(5px);
}

.sidebar-menu .el-menu-item.is-active {
  background-color: #409EFF !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
  color: white !important;
}

.sidebar-menu .el-menu-item.is-active .el-icon {
  color: white !important;
}

.sidebar-menu .el-menu-item.is-active span {
  color: white !important;
}

.sidebar-menu .el-menu-item {
  color: white !important;
}

.sidebar-menu .el-menu-item .el-icon {
  margin-right: 12px;
  font-size: 18px;
  flex-shrink: 0;
  color: white !important;
}

.sidebar-menu .el-menu-item span {
  font-size: 16px;
  font-weight: 500;
  flex: 1;
  min-width: 0;
  color: white !important;
}
</style>
