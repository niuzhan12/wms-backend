<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>仓储AS/AR软件系统</h1>
        <p>请登录以访问系统</p>
      </div>
      
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="username"
            type="text"
            placeholder="请输入用户名"
            class="form-input"
            required
          />
        </div>
        
        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            placeholder="请输入密码"
            class="form-input"
            required
          />
        </div>
        
        <button
          type="submit"
          :disabled="loading"
          class="login-button"
        >
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      
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

export default {
  name: 'LoginSimple',
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const username = ref('')
    const password = ref('')
    
    const handleLogin = async () => {
      if (!username.value || !password.value) {
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
            username: username.value,
            password: password.value
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
          
          // 跳转到主页面
          router.push('/warehouse-status')
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
      loading,
      username,
      password,
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

.form-group {
  margin-bottom: 20px;
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #2c3e50;
  font-weight: 500;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e1e8ed;
  border-radius: 8px;
  font-size: 16px;
  transition: border-color 0.3s ease;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #409EFF;
  box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.1);
}

.form-input:disabled {
  background-color: #f5f7fa;
  cursor: not-allowed;
}

.login-button {
  width: 100%;
  height: 45px;
  font-size: 16px;
  font-weight: bold;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.login-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
  transform: translateY(-1px);
}

.login-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
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
