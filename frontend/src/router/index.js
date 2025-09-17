import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authService } from '../services/auth'
import Login from '../views/LoginFixed.vue'
import WarehouseStatus from '../views/WarehouseStatus.vue'
import PalletClassification from '../views/PalletClassification.vue'
import GoodsClassification from '../views/GoodsClassification.vue'
import DeviceStatus from '../views/DeviceStatus.vue'
import Debug from '../views/Debug.vue'
import ModbusTest from '../views/ModbusTest.vue'
import MesWmsMonitor from '../views/MesWmsMonitor.vue'
import StackerMonitor from '../views/StackerMonitor.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/warehouse-status',
    name: 'WarehouseStatus',
    component: WarehouseStatus,
    meta: { requiresAuth: true }
  },
  {
    path: '/pallet-classification',
    name: 'PalletClassification',
    component: PalletClassification,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/goods-classification',
    name: 'GoodsClassification',
    component: GoodsClassification,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/device-status',
    name: 'DeviceStatus',
    component: DeviceStatus,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/debug',
    name: 'Debug',
    component: Debug,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/modbus-test',
    name: 'ModbusTest',
    component: ModbusTest,
    meta: { requiresAuth: true }
  },
  {
    path: '/mes-wms-monitor',
    name: 'MesWmsMonitor',
    component: MesWmsMonitor,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/stacker-monitor',
    name: 'StackerMonitor',
    component: StackerMonitor,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  // 如果路由不需要认证，直接通过
  if (!to.meta.requiresAuth) {
    next()
    return
  }
  
  // 检查是否已登录
  if (!authService.isLoggedIn()) {
    ElMessage.warning('请先登录')
    next('/login')
    return
  }
  
  // 验证token有效性
  const isValid = await authService.validateToken()
  if (!isValid) {
    ElMessage.error('登录已过期，请重新登录')
    next('/login')
    return
  }
  
  // 检查管理员权限
  if (to.meta.requiresAdmin && !authService.isAdmin()) {
    ElMessage.error('您没有权限访问此页面')
    next('/warehouse-status')
    return
  }
  
  next()
})

export default router
