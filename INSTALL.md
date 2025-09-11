# 安装和配置说明

## 系统要求

- Java 11 或更高版本
- Maven 3.6 或更高版本
- Node.js 16 或更高版本
- npm 8 或更高版本

## 快速安装

### 1. 检查环境

```bash
# 检查Java版本
java -version

# 检查Maven版本
mvn -version

# 检查Node.js版本
node -v

# 检查npm版本
npm -v
```

### 2. 启动系统

#### Windows用户
双击运行 `start.bat` 文件

#### Linux/Mac用户
```bash
chmod +x start.sh
./start.sh
```

### 3. 手动启动（可选）

#### 启动后端
```bash
cd backend
mvn spring-boot:run
```

#### 启动前端
```bash
cd frontend
npm install
npm run dev
```

## 访问地址

- **前端界面**: http://localhost:3000
- **后端API**: http://localhost:8080
- **H2数据库控制台**: http://localhost:8080/h2-console
  - 用户名: sa
  - 密码: (留空)

## 功能说明

### 1. 仓库状态显示
- 显示A库和B库的4行6列网格布局
- 实时显示托盘状态、物料状态、二维码信息
- 支持点击查看详细位置信息

### 2. 托盘分类
- 管理托盘信息（编号、名称、二维码、位置）
- 支持添加、编辑、删除托盘
- 显示托盘在仓库中的分布

### 3. 货物分类
- 管理货物信息（编号、名称、二维码、位置、状态）
- 区分毛坯和成品物料
- 支持数量管理

### 4. 设备状态
- 实时显示设备状态（空闲/忙）
- 显示当前运行的行列位置
- 支持本地/远程控制模式切换
- 提供设备状态控制功能

### 5. 调试功能
- 出入库操作（选择位置、输入托盘和物料编码）
- 线体控制（入货、出货、复位）
- 系统日志显示
- 其他控制功能

### 6. 主菜单
- 系统菜单、编号设置、通讯设置
- 复位运行、帮助等系统功能

## 数据库

系统使用H2内存数据库，启动时会自动创建表结构和示例数据：

- 仓库位置表 (warehouse_locations)
- 托盘表 (pallets)
- 货物表 (goods)
- 设备状态表 (device_status)

## 故障排除

### 常见问题

1. **端口被占用**
   - 后端端口8080被占用：修改 `backend/src/main/resources/application.yml` 中的 `server.port`
   - 前端端口3000被占用：修改 `frontend/vite.config.js` 中的 `server.port`

2. **依赖下载失败**
   - 检查网络连接
   - 配置Maven镜像源
   - 配置npm镜像源

3. **前端无法连接后端**
   - 检查后端服务是否启动
   - 检查 `frontend/vite.config.js` 中的代理配置

### 日志查看

- 后端日志：查看控制台输出
- 前端日志：浏览器开发者工具Console
- 系统日志：调试页面的日志显示区域

## 开发说明

### 后端技术栈
- Spring Boot 2.7.0
- Spring Data JPA
- H2 Database
- Maven

### 前端技术栈
- Vue.js 3
- Element Plus UI
- Axios
- Vue Router
- Vite

### 项目结构
```
springboot-vue-wms3/
├── backend/                 # Spring Boot后端
│   ├── src/main/java/      # Java源代码
│   ├── src/main/resources/ # 配置文件
│   └── pom.xml            # Maven配置
├── frontend/               # Vue.js前端
│   ├── src/               # 源代码
│   ├── public/            # 静态资源
│   └── package.json       # npm配置
├── start.bat              # Windows启动脚本
├── start.sh               # Linux/Mac启动脚本
└── README.md              # 项目说明
```

## 联系支持

如有问题，请查看：
1. 系统日志信息
2. 浏览器控制台错误
3. 后端服务控制台输出
