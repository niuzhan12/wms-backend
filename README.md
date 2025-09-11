# 仓储AS/AR软件系统

一个完整的仓库管理系统，包含前端Vue.js界面和后端Spring Boot API。

## 主要功能

### 1. 仓库状态显示
- 行列：整体显示几行几列排布
- 托盘状态：是否有托盘
- 物料状态：是否有料，毛坯还是成品
- 二维码：托盘编码，物料编码
- 备注：备注信息

### 2. 托盘分类
- 序号、托盘编号、托盘名称、二维码、行、列

### 3. 货物分类
- 序号、货物编号、货物名称、二维码、行、列

### 4. 设备状态
- 设备状态：空闲、忙
- 当前运行的行、列
- 控制模式：本地、远程

### 5. 调试
- 出库还是入库
- 行、列
- 出入库复位
- 线体控制
- 线体进料
- 线体出料
- 线体复位

### 6. 主菜单
- 系统菜单
- 编号设置
- 通讯设置
- 复位运行
- 帮助

## 技术栈

### 后端
- Spring Boot 2.7.0
- Spring Data JPA
- H2 Database
- Maven

### 前端
- Vue.js 3
- Element Plus UI
- Axios
- Vue Router

## 快速开始

### 后端启动
```bash
cd backend
mvn spring-boot:run
```

### 前端启动
```bash
cd frontend
npm install
npm run dev
```

## 项目结构
```
springboot-vue-wms3/
├── backend/                 # Spring Boot后端
├── frontend/               # Vue.js前端
└── README.md              # 项目说明
```

