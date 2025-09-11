# 解决托盘数据显示问题的方案

## 问题描述
即使没有手动添加托盘数据，界面仍然显示托盘信息，这是因为：

1. **后端自动初始化**：`DataInitializer.java` 在应用启动时自动创建了15个示例托盘
2. **前端数据缓存**：浏览器localStorage中保存了之前的托盘和仓库状态数据
3. **模拟数据备用**：前端有硬编码的模拟数据作为备用

## 解决方案

### 1. 禁用后端自动初始化（已完成）
- 已修改 `backend/src/main/java/com/wms/config/DataInitializer.java`
- 注释掉了 `initializePallets()` 方法的调用
- 重启后端应用后，将不再自动创建托盘数据

### 2. 清除数据库中的现有数据
执行 `clear_pallet_data.sql` 脚本：
```sql
-- 清除托盘表数据
DELETE FROM pallet;

-- 清除货物表数据
DELETE FROM goods;

-- 重置仓库位置状态
UPDATE warehouse_location SET has_pallet = false, material_status = 'EMPTY', remarks = '';
```

### 3. 清除前端本地存储数据
使用 `frontend/clear_local_storage.html` 页面：
- 在浏览器中打开该页面
- 点击"清除所有本地数据"按钮
- 或者分别清除托盘数据、仓库状态等

### 4. 重启应用
1. 停止后端服务
2. 清除数据库数据（执行SQL脚本）
3. 重启后端服务
4. 刷新前端页面

## 验证结果
清除完成后，仓库状态页面应该显示：
- 所有位置都是空的（白色背景）
- 没有托盘数据
- 没有货物信息

## 注意事项
- 清除操作不可逆，请谨慎操作
- 如果仍有数据显示，检查浏览器开发者工具的Application > Local Storage
- 确保数据库连接正常，API调用成功

## 预防措施
- 避免在开发环境中使用自动数据初始化
- 定期清理测试数据
- 使用环境变量控制数据初始化行为



