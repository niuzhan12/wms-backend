// 认证服务
export const authService = {
  // 获取token
  getToken() {
    return localStorage.getItem('token')
  },
  
  // 获取用户信息
  getUserInfo() {
    return {
      username: localStorage.getItem('username'),
      role: localStorage.getItem('role'),
      roleDescription: localStorage.getItem('roleDescription')
    }
  },
  
  // 检查是否已登录
  isLoggedIn() {
    const token = this.getToken()
    return !!token
  },
  
  // 检查是否为管理员
  isAdmin() {
    const role = localStorage.getItem('role')
    return role === 'ADMIN'
  },
  
  // 检查是否为普通用户
  isUser() {
    const role = localStorage.getItem('role')
    return role === 'USER'
  },
  
  // 验证token有效性
  async validateToken() {
    const token = this.getToken()
    if (!token) {
      return false
    }
    
    try {
      const response = await fetch('http://localhost:8080/api/auth/validate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ token })
      })
      
      const result = await response.json()
      
      if (result.success) {
        // 更新用户信息
        localStorage.setItem('username', result.username)
        localStorage.setItem('role', result.role)
        return true
      } else {
        this.logout()
        return false
      }
    } catch (error) {
      console.error('Token验证失败:', error)
      this.logout()
      return false
    }
  },
  
  // 登出
  logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('roleDescription')
  },
  
  // 检查用户是否有权限访问某个路由
  hasPermission(routeName) {
    const role = localStorage.getItem('role')
    
    // 管理员可以访问所有页面
    if (role === 'ADMIN') {
      return true
    }
    
    // 普通用户只能访问仓储分类页面
    if (role === 'USER') {
      return routeName === 'WarehouseStatus'
    }
    
    return false
  }
}
