package com.wms.repository;

import com.wms.entity.Pallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PalletRepository extends JpaRepository<Pallet, Long> {
    
    Optional<Pallet> findByPalletNumber(String palletNumber);
    
    Optional<Pallet> findByQrCode(String qrCode);
    
    List<Pallet> findByWarehouseCodeAndRowNumberAndColumnNumber(
        String warehouseCode, Integer rowNumber, Integer columnNumber);
    
    List<Pallet> findByIsOccupiedTrue();
}
