package com.wms.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "warehouse_location")
public class WarehouseLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "warehouse_code", nullable = false)
    private String warehouseCode; // 仓库代码 (A, B)
    
    @Column(name = "`row_number`", nullable = false)
    private Integer rowNumber; // 行号
    
    @Column(name = "`column_number`", nullable = false)
    private Integer columnNumber; // 列号
    
    @Column(name = "has_pallet")
    private Boolean hasPallet = false; // 是否有托盘
    
    @Column(name = "pallet_code")
    private String palletCode; // 托盘编码
    
    @Column(name = "material_code")
    private String materialCode; // 物料编码
    
    @Enumerated(EnumType.STRING)
    @Column(name = "material_status")
    private MaterialStatus materialStatus; // 物料状态
    
    @Column(name = "remarks")
    private String remarks; // 备注信息
    
    public enum MaterialStatus {
        EMPTY,      // 无料
        RAW,        // 毛坯
        FINISHED    // 成品
    }
}
