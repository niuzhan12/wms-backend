-- 清除托盘数据的SQL脚本
-- 请在数据库中执行以下SQL语句来清除所有托盘数据

-- 清除托盘表数据
DELETE FROM pallet;

-- 清除货物表数据（如果货物与托盘关联）
DELETE FROM goods;

-- 重置仓库位置状态
UPDATE warehouse_location SET has_pallet = false, material_status = 'EMPTY', remarks = '';

-- 重置自增ID（如果使用自增主键）
-- ALTER TABLE pallet AUTO_INCREMENT = 1;
-- ALTER TABLE goods AUTO_INCREMENT = 1;

-- 验证清除结果
SELECT '托盘表记录数:' as table_name, COUNT(*) as record_count FROM pallet
UNION ALL
SELECT '货物表记录数:' as table_name, COUNT(*) as record_count FROM goods
UNION ALL
SELECT '仓库位置表记录数:' as table_name, COUNT(*) as record_count FROM warehouse_location;



