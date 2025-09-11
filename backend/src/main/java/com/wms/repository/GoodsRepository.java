package com.wms.repository;

import com.wms.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsRepository extends JpaRepository<Goods, Long> {
    
    Optional<Goods> findByGoodsNumber(String goodsNumber);
    
    Optional<Goods> findByQrCode(String qrCode);
    
    List<Goods> findByWarehouseCodeAndRowNumberAndColumnNumber(
        String warehouseCode, Integer rowNumber, Integer columnNumber);
    
    List<Goods> findByMaterialStatus(Goods.MaterialStatus materialStatus);
    
    List<Goods> findByGoodsNameContaining(String goodsName);
}
