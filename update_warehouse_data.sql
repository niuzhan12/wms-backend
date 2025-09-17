USE wms_db;

-- 更新一些库位为有料状态
UPDATE warehouse_location 
SET has_pallet=1, pallet_code='TEST001', material_code='MAT001', material_status='RAW' 
WHERE warehouse_code='A' AND row_number=1 AND column_number=1;

UPDATE warehouse_location 
SET has_pallet=1, pallet_code='TEST002', material_code='MAT002', material_status='RAW' 
WHERE warehouse_code='A' AND row_number=1 AND column_number=2;

UPDATE warehouse_location 
SET has_pallet=1, pallet_code='TEST003', material_code='MAT003', material_status='FINISHED' 
WHERE warehouse_code='A' AND row_number=2 AND column_number=1;

UPDATE warehouse_location 
SET has_pallet=1, pallet_code='TEST004', material_code='MAT004', material_status='FINISHED' 
WHERE warehouse_code='A' AND row_number=2 AND column_number=2;

UPDATE warehouse_location 
SET has_pallet=1, pallet_code='TEST005', material_code='MAT005', material_status='RAW' 
WHERE warehouse_code='A' AND row_number=3 AND column_number=1;

-- 查看更新结果
SELECT warehouse_code, row_number, column_number, has_pallet, pallet_code, material_code, material_status 
FROM warehouse_location 
WHERE warehouse_code='A' 
ORDER BY row_number, column_number;
