package com.filo.fleet.repository;

import com.filo.fleet.entity.MaintenanceLog;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {

    @EntityGraph(attributePaths = "vehicle")
    List<MaintenanceLog> findByVehicleIdOrderByMaintenanceDateDescIdDesc(Long vehicleId);
}
