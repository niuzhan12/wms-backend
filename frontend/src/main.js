import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { authService } from './services/auth'
import axios from 'axios'

// 配置axios baseURL
axios.defaults.baseURL = 'http://localhost:8080'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 应用启动时检查登录状态
const initApp = async () => {
  // 如果当前不在登录页面且没有有效token，跳转到登录页
  if (window.location.pathname !== '/login') {
    const isValid = await authService.validateToken()
    if (!isValid) {
      router.push('/login')
    }
  }
}

app.use(ElementPlus)
app.use(router)

// 初始化应用
initApp().then(() => {
  app.mount('#app')
})
