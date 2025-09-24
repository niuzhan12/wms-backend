package com.wms.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "pallets")
public class Pallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "pallet_number", nullable = false, unique = true)
    private String palletNumber; // 托盘编号
    
    @Column(name = "pallet_name", nullable = false)
    private String palletName; // 托盘名称
    
    @Column(name = "qr_code", nullable = false, unique = true)
    private String qrCode; // 二维码
    
    @Column(name = "warehouse_code")
    private String warehouseCode; // 所在仓库
    
    @Column(name = "`row_number`")
    private Integer rowNumber; // 行号
    
    @Column(name = "`column_number`")
    private Integer columnNumber; // 列号
    
    @Column(name = "is_occupied")
    private Boolean isOccupied = false; // 是否被占用
}
