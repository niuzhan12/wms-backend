<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>仓储AS/AR软件系统</h1>
        <p>请登录以访问系统</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
            clearable
            @input="handleUsernameInput"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @input="handlePasswordInput"
            @keyup.enter="handleLogin"
          />
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
      
      <div class="login-tips">
        <p><strong>默认账户：</strong></p>
        <p>管理员：admin / admin123</p>
        <p>普通用户：user / user123</p>
      </div>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

export default {
  name: 'Login',
  components: {
    User,
    Lock
  },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const loginFormRef = ref(null)
    
    const loginForm = ref({
      username: '',
      password: ''
    })
    
    const loginRules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
      ]
    }
    
    // 处理用户名输入
    const handleUsernameInput = (value) => {
      loginForm.value.username = value
    }
    
    // 处理密码输入
    const handlePasswordInput = (value) => {
      loginForm.value.password = value
    }
    
    const handleLogin = async () => {
      if (!loginForm.value.username || !loginForm.value.password) {
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
            username: loginForm.value.username,
            password: loginForm.value.password
          })
        })
        
        const result = await response.json()
        
        if (result.success) {
          // 保存用户信息和token到localStorage
          localStorage.setItem('token', result.token)
          localStorage.setItem('username', result.username)
          localStorage.setItem('role', result.role)
          localStorage.setItem('roleDescription', result.roleDescription)
          
          ElMessage.success('登录成功！')
          
          // 根据用户角色跳转到不同页面
          if (result.role === 'ADMIN') {
            router.push('/warehouse-status')
          } else {
            router.push('/warehouse-status')
          }
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
      loginForm,
      loginRules,
      loading,
      handleUsernameInput,
      handlePasswordInput,
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

.login-tips {
  background: #f8f9fa;
  border-radius: 6px;
  padding: 15px;
  text-align: left;
  font-size: 12px;
  color: #6c757d;
  line-height: 1.5;
}

.login-tips p {
  margin: 5px 0;
}

.login-tips strong {
  color: #495057;
}
</style>
