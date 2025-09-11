<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>仓储软件系统</h1>
        <p>请登录以访问系统</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="formData"
        :rules="rules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="请输入用户名"
            size="large"
            clearable
            @input="updateUsername"
          >
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @input="updatePassword"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-button"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

export default {
  name: 'LoginFixed',
  components: {
    User,
    Lock
  },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const loginFormRef = ref(null)
    
    // 使用reactive创建响应式数据
    const formData = reactive({
      username: '',
      password: ''
    })
    
    const rules = reactive({
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
      ]
    })
    
    // 手动更新函数
    const updateUsername = (value) => {
      formData.username = value
    }
    
    const updatePassword = (value) => {
      formData.password = value
    }
    
    const handleLogin = async () => {
      // 验证表单
      if (!loginFormRef.value) return
      
      try {
        await loginFormRef.value.validate()
      } catch (error) {
        console.log('表单验证失败:', error)
        return
      }
      
      if (!formData.username || !formData.password) {
        ElMessage.warning('请输入用户名和密码')
        return
      }
      
      loading.value = true
      
      try {
        const response = await fetch('http://localhost:8080/api/auth/login', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            username: formData.username,
            password: formData.password
          })
        })
        
        const result = await response.json()
        
        if (result.success) {
          // 保存用户信息和token到localStorage
          localStorage.setItem('token', result.token)
          localStorage.setItem('username', result.username)
          localStorage.setItem('role', result.role)
          localStorage.setItem('roleDescription', result.roleDescription)
          
          console.log('=== 登录成功 ===')
          console.log('用户名:', result.username)
          console.log('角色:', result.role)
          console.log('角色描述:', result.roleDescription)
          console.log('localStorage已保存')
          console.log('================')
          
          ElMessage.success('登录成功！')
          
          // 直接刷新页面以确保权限正确应用
          setTimeout(() => {
            window.location.href = '/warehouse-status'
          }, 500)
        } else {
          ElMessage.error(result.message || '登录失败')
        }
      } catch (error) {
        console.error('登录错误:', error)
        ElMessage.error('网络错误，请检查后端服务是否启动')
      } finally {
        loading.value = false
      }
    }
    
    return {
      loginFormRef,
      formData,
      rules,
      loading,
      updateUsername,
      updatePassword,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-box {
  background: white;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  padding: 40px;
  width: 100%;
  max-width: 400px;
  text-align: center;
}

.login-header {
  margin-bottom: 30px;
}

.login-header h1 {
  color: #2c3e50;
  font-size: 24px;
  margin: 0 0 10px 0;
  font-weight: bold;
}

.login-header p {
  color: #7f8c8d;
  margin: 0;
  font-size: 14px;
}

.login-form {
  margin-bottom: 20px;
}

.login-form .el-form-item {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  height: 45px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.login-button:hover {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
}

</style>
