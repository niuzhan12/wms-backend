package com.wms.service;

import com.wms.entity.WarehouseLocation;
import com.wms.entity.Pallet;
import com.wms.entity.Goods;
import com.wms.repository.WarehouseLocationRepository;
import com.wms.repository.PalletRepository;
import com.wms.repository.GoodsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class WarehouseService {
    
    @Autowired
    private WarehouseLocationRepository locationRepository;
    
    @Autowired
    private PalletRepository palletRepository;
    
    @Autowired
    private GoodsRepository goodsRepository;
    
    // 获取仓库状态显示
    public Map<String, Object> getWarehouseStatus(String warehouseCode) {
        Map<String, Object> result = new HashMap<>();
        
        List<WarehouseLocation> locations = locationRepository
            .findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc(warehouseCode);
        
        // 构建网格数据
        Map<String, Object> grid = new HashMap<>();
        for (WarehouseLocation location : locations) {
            String key = location.getRowNumber() + "-" + location.getColumnNumber();
            Map<String, Object> cell = new HashMap<>();
            cell.put("hasPallet", location.getHasPallet());
            cell.put("palletCode", location.getPalletCode());
            cell.put("materialCode", location.getMaterialCode());
            cell.put("materialStatus", location.getMaterialStatus());
            cell.put("remarks", location.getRemarks());
            grid.put(key, cell);
        }
        
        result.put("warehouseCode", warehouseCode);
        result.put("grid", grid);
        result.put("totalRows", 4); // 固定4行
        result.put("totalColumns", 6); // 固定6列
        
        return result;
    }
    
    // 获取托盘分类
    public List<Map<String, Object>> getPalletClassification() {
        List<Pallet> pallets = palletRepository.findAll();
        return pallets.stream().map(pallet -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", pallet.getId());
            map.put("palletNumber", pallet.getPalletNumber());
            map.put("palletName", pallet.getPalletName());
            map.put("qrCode", pallet.getQrCode());
            map.put("row", pallet.getRowNumber());
            map.put("column", pallet.getColumnNumber());
            return map;
        }).collect(Collectors.toList());
    }
    
    // 获取货物分类
    public List<Map<String, Object>> getGoodsClassification() {
        List<Goods> goods = goodsRepository.findAll();
        return goods.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", item.getId());
            map.put("goodsNumber", item.getGoodsNumber());
            map.put("goodsName", item.getGoodsName());
            map.put("qrCode", item.getQrCode());
            map.put("row", item.getRowNumber());
            map.put("column", item.getColumnNumber());
            map.put("materialStatus", item.getMaterialStatus());
            map.put("quantity", item.getQuantity());
            return map;
        }).collect(Collectors.toList());
    }
    
    // 入库操作
    public boolean inbound(String warehouseCode, Integer row, Integer column, String palletCode, String materialCode) {
        WarehouseLocation location = locationRepository
            .findByWarehouseCodeAndRowNumberAndColumnNumber(warehouseCode, row, column)
            .orElse(null);
            
        if (location != null && !location.getHasPallet()) {
            location.setHasPallet(true);
            location.setPalletCode(palletCode);
            location.setMaterialCode(materialCode);
            location.setMaterialStatus(WarehouseLocation.MaterialStatus.RAW);
            locationRepository.save(location);
            return true;
        }
        return false;
    }
    
    // 出库操作
    public boolean outbound(String warehouseCode, Integer row, Integer column) {
        WarehouseLocation location = locationRepository
            .findByWarehouseCodeAndRowNumberAndColumnNumber(warehouseCode, row, column)
            .orElse(null);
            
        if (location != null && location.getHasPallet()) {
            location.setHasPallet(false);
            location.setPalletCode(null);
            location.setMaterialCode(null);
            location.setMaterialStatus(WarehouseLocation.MaterialStatus.EMPTY);
            locationRepository.save(location);
            return true;
        }
        return false;
    }
    
    // 自动入库操作 - 按顺序找第一个空闲位置
    public Map<String, Object> autoInbound(String warehouseCode, String palletCode, String materialCode) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        // 按行优先顺序查找空闲位置 (1-4行, 1-6列)
        for (int row = 1; row <= 4; row++) {
            for (int column = 1; column <= 6; column++) {
                WarehouseLocation location = locationRepository
                    .findByWarehouseCodeAndRowNumberAndColumnNumber(warehouseCode, row, column)
                    .orElse(null);
                    
                if (location != null && !location.getHasPallet()) {
                    // 找到空闲位置，执行入库
                    location.setHasPallet(true);
                    location.setPalletCode(palletCode);
                    location.setMaterialCode(materialCode);
                    location.setMaterialStatus(WarehouseLocation.MaterialStatus.RAW);
                    locationRepository.save(location);
                    
                    result.put("success", true);
                    result.put("message", "自动入库成功");
                    result.put("row", row);
                    result.put("column", column);
                    return result;
                }
            }
        }
        
        // 没有找到空闲位置
        result.put("success", false);
        result.put("message", "自动入库失败：没有空闲位置");
        return result;
    }
    
    // 自动出库操作 - 按顺序找第一个有料位置
    public Map<String, Object> autoOutbound(String warehouseCode) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        // 按行优先顺序查找有料位置 (1-4行, 1-6列)
        for (int row = 1; row <= 4; row++) {
            for (int column = 1; column <= 6; column++) {
                WarehouseLocation location = locationRepository
                    .findByWarehouseCodeAndRowNumberAndColumnNumber(warehouseCode, row, column)
                    .orElse(null);
                    
                if (location != null && location.getHasPallet()) {
                    // 找到有料位置，执行出库
                    String palletCode = location.getPalletCode();
                    String materialCode = location.getMaterialCode();
                    
                    location.setHasPallet(false);
                    location.setPalletCode(null);
                    location.setMaterialCode(null);
                    location.setMaterialStatus(WarehouseLocation.MaterialStatus.EMPTY);
                    locationRepository.save(location);
                    
                    result.put("success", true);
                    result.put("message", "自动出库成功");
                    result.put("row", row);
                    result.put("column", column);
                    result.put("palletCode", palletCode);
                    result.put("materialCode", materialCode);
                    return result;
                }
            }
        }
        
        // 没有找到有料位置
        result.put("success", false);
        result.put("message", "自动出库失败：没有有料位置");
        return result;
    }
    
    // 添加托盘
    public boolean addPallet(Map<String, Object> request) {
        try {
            Pallet pallet = new Pallet();
            pallet.setPalletNumber((String) request.get("palletNumber"));
            pallet.setPalletName((String) request.get("palletName"));
            pallet.setQrCode((String) request.get("qrCode"));
            pallet.setWarehouseCode((String) request.get("warehouseCode"));
            pallet.setRowNumber((Integer) request.get("row"));
            pallet.setColumnNumber((Integer) request.get("column"));
            pallet.setIsOccupied(false);
            
            palletRepository.save(pallet);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 更新托盘
    public boolean updatePallet(Long id, Map<String, Object> request) {
        try {
            Pallet pallet = palletRepository.findById(id).orElse(null);
            if (pallet != null) {
                pallet.setPalletNumber((String) request.get("palletNumber"));
                pallet.setPalletName((String) request.get("palletName"));
                pallet.setQrCode((String) request.get("qrCode"));
                pallet.setWarehouseCode((String) request.get("warehouseCode"));
                pallet.setRowNumber((Integer) request.get("row"));
                pallet.setColumnNumber((Integer) request.get("column"));
                
                palletRepository.save(pallet);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 删除托盘
    public boolean deletePallet(Long id) {
        try {
            palletRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 添加货物
    public boolean addGoods(Map<String, Object> request) {
        try {
            Goods goods = new Goods();
            goods.setGoodsNumber((String) request.get("goodsNumber"));
            goods.setGoodsName((String) request.get("goodsName"));
            goods.setQrCode((String) request.get("qrCode"));
            goods.setWarehouseCode((String) request.get("warehouseCode"));
            goods.setRowNumber((Integer) request.get("row"));
            goods.setColumnNumber((Integer) request.get("column"));
            goods.setMaterialStatus(Goods.MaterialStatus.valueOf((String) request.get("materialStatus")));
            goods.setQuantity((Integer) request.get("quantity"));
            
            goodsRepository.save(goods);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 更新货物
    public boolean updateGoods(Long id, Map<String, Object> request) {
        try {
            Goods goods = goodsRepository.findById(id).orElse(null);
            if (goods != null) {
                goods.setGoodsNumber((String) request.get("goodsNumber"));
                goods.setGoodsName((String) request.get("goodsName"));
                goods.setQrCode((String) request.get("qrCode"));
                goods.setWarehouseCode((String) request.get("warehouseCode"));
                goods.setRowNumber((Integer) request.get("row"));
                goods.setColumnNumber((Integer) request.get("column"));
                goods.setMaterialStatus(Goods.MaterialStatus.valueOf((String) request.get("materialStatus")));
                goods.setQuantity((Integer) request.get("quantity"));
                
                goodsRepository.save(goods);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // 删除货物
    public boolean deleteGoods(Long id) {
        try {
            goodsRepository.deleteById(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
