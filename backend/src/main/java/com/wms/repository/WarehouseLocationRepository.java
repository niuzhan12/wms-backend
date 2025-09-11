package com.wms.repository;

import com.wms.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {
    
    List<WarehouseLocation> findByWarehouseCodeOrderByRowNumberAscColumnNumberAsc(String warehouseCode);
    
    Optional<WarehouseLocation> findByWarehouseCodeAndRowNumberAndColumnNumber(
        String warehouseCode, Integer rowNumber, Integer columnNumber);
    
    List<WarehouseLocation> findByHasPalletTrue();
    
    List<WarehouseLocation> findByMaterialStatus(WarehouseLocation.MaterialStatus materialStatus);
}
