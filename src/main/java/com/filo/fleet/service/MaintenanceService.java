package com.filo.fleet.service;

import com.filo.fleet.dto.request.MaintenanceCompleteRequest;
import com.filo.fleet.dto.response.MaintenanceLogResponse;
import com.filo.fleet.dto.response.VehicleResponse;
import java.util.List;

public interface MaintenanceService {

    /** Aracı bakıma alır (IN_MAINTENANCE). Görevdeki araç bakıma alınamaz. */
    VehicleResponse startMaintenance(Long vehicleId);

    /** Bakımı tamamlar, bakım kaydı oluşturur ve aracı boşa (IDLE) çıkarır. */
    MaintenanceLogResponse completeMaintenance(Long vehicleId, MaintenanceCompleteRequest request);

    List<MaintenanceLogResponse> getLogs(Long vehicleId);
}
