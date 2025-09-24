package com.wms.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "goods")
public class Goods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "goods_number", nullable = false, unique = true)
    private String goodsNumber; // 货物编号
    
    @Column(name = "goods_name", nullable = false)
    private String goodsName; // 货物名称
    
    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode; // 二维码
    
    @Column(name = "warehouse_code")
    private String warehouseCode; // 所在仓库
    
    @Column(name = "`row_number`")
    private Integer rowNumber; // 行号
    
    @Column(name = "`column_number`")
    private Integer columnNumber; // 列号
    
    @Enumerated(EnumType.STRING)
    @Column(name = "material_status")
    private MaterialStatus materialStatus; // 物料状态
    
    @Column(name = "quantity")
    private Integer quantity = 1; // 数量
    
    public enum MaterialStatus {
        RAW,        // 毛坯
        FINISHED    // 成品
    }
}
