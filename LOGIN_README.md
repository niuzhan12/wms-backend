# 仓储管理系统登录功能说明

## 功能概述

本系统已成功集成了用户登录和权限控制功能，支持管理员和普通用户两种角色，实现了基于JWT的身份认证和权限管理。

## 系统架构

### 后端功能
- **用户实体 (User)**: 包含用户名、密码、角色等基本信息
- **JWT认证**: 使用JWT进行无状态身份认证
- **Spring Security**: 提供安全框架支持
- **权限控制**: 基于角色的访问控制(RBAC)

### 前端功能
- **登录页面**: 美观的登录界面，支持用户名密码登录
- **路由守卫**: 自动检查用户登录状态和权限
- **权限控制**: 根据用户角色显示不同的菜单选项
- **状态管理**: 使用localStorage持久化用户登录状态

## 默认账户

系统初始化时会自动创建以下默认账户：

| 用户名 | 密码 | 角色 | 权限说明 |
|--------|------|------|----------|
| admin | admin123 | 管理员 | 可以访问所有功能模块 |
| user | user123 | 普通用户 | 只能访问仓储分类模块 |

## 权限说明

### 管理员权限
- ✅ 仓储分类 (WarehouseStatus)
- ✅ 托盘分类 (PalletClassification)
- ✅ 货物分类 (GoodsClassification)
- ✅ 设备状态 (DeviceStatus)
- ✅ 调试 (Debug)
- ✅ Modbus测试 (ModbusTest)

### 普通用户权限
- ✅ 仓储分类 (WarehouseStatus)
- ❌ 托盘分类 (PalletClassification)
- ❌ 货物分类 (GoodsClassification)
- ❌ 设备状态 (DeviceStatus)
- ❌ 调试 (Debug)
- ❌ Modbus测试 (ModbusTest)

## 启动方法

### 方法1: 使用启动脚本
```bash
# Windows
start_with_login.bat

# Linux/Mac
chmod +x start_with_login.sh
./start_with_login.sh
```

### 方法2: 手动启动
```bash
# 启动后端
cd backend
mvn spring-boot:run

# 启动前端
cd frontend
npm run dev
```

## 访问地址

- **前端应用**: http://localhost:5173
- **后端API**: http://localhost:8080
- **登录测试页面**: 打开 `test_login.html` 文件

## 技术实现

### 后端技术栈
- Spring Boot 2.7.0
- Spring Security
- Spring Data JPA
- JWT (io.jsonwebtoken)
- MySQL数据库

### 前端技术栈
- Vue 3
- Element Plus
- Vue Router
- Axios (用于API调用)

### 安全特性
- 密码加密存储 (BCrypt)
- JWT Token认证
- 跨域请求支持
- 路由权限控制
- 自动Token验证

## API接口

### 登录接口
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

### Token验证接口
```
POST /api/auth/validate
Content-Type: application/json

{
  "token": "your-jwt-token"
}
```

## 使用流程

1. **访问系统**: 打开浏览器访问 http://localhost:5173
2. **自动跳转**: 系统会自动跳转到登录页面
3. **输入凭据**: 使用默认账户登录
4. **权限验证**: 系统根据用户角色显示相应功能
5. **安全退出**: 点击右上角"退出登录"按钮

## 测试功能

打开 `test_login.html` 文件可以进行以下测试：
- 管理员登录测试
- 普通用户登录测试
- 错误密码测试
- Token验证测试
- 权限控制测试

## 注意事项

1. **首次启动**: 系统会自动创建数据库表和默认用户
2. **Token过期**: JWT Token默认24小时过期，过期后需要重新登录
3. **权限控制**: 普通用户尝试访问受限页面会被自动重定向
4. **数据安全**: 所有密码都经过BCrypt加密存储
5. **跨域支持**: 已配置CORS支持前后端分离部署

## 故障排除

### 常见问题

1. **登录失败**: 检查后端服务是否启动，数据库连接是否正常
2. **权限错误**: 确认用户角色是否正确，Token是否有效
3. **页面空白**: 检查前端服务是否启动，控制台是否有错误
4. **API调用失败**: 检查网络连接和后端API地址

### 调试方法

1. 打开浏览器开发者工具查看控制台错误
2. 检查网络请求是否成功
3. 查看后端日志输出
4. 使用测试页面验证API功能

## 扩展功能

系统支持以下扩展：
- 添加更多用户角色
- 实现用户注册功能
- 添加密码重置功能
- 实现记住登录状态
- 添加操作日志记录
- 实现多租户支持
