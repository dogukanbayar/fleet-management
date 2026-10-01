package com.filo.fleet.service.impl;

import com.filo.fleet.dto.request.MaintenanceCompleteRequest;
import com.filo.fleet.dto.response.MaintenanceLogResponse;
import com.filo.fleet.dto.response.VehicleResponse;
import com.filo.fleet.entity.MaintenanceLog;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleStatus;
import com.filo.fleet.exception.InvalidOperationException;
import com.filo.fleet.exception.ResourceNotFoundException;
import com.filo.fleet.mapper.MaintenanceLogMapper;
import com.filo.fleet.mapper.VehicleMapper;
import com.filo.fleet.repository.MaintenanceLogRepository;
import com.filo.fleet.repository.VehicleRepository;
import com.filo.fleet.service.MaintenanceService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceServiceImpl implements MaintenanceService {

    private static final String VEHICLE = "Araç";

    private final VehicleRepository vehicleRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;
    private final VehicleMapper vehicleMapper;
    private final MaintenanceLogMapper maintenanceLogMapper;

    public MaintenanceServiceImpl(VehicleRepository vehicleRepository,
                                  MaintenanceLogRepository maintenanceLogRepository,
                                  VehicleMapper vehicleMapper,
                                  MaintenanceLogMapper maintenanceLogMapper) {
        this.vehicleRepository = vehicleRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
        this.vehicleMapper = vehicleMapper;
        this.maintenanceLogMapper = maintenanceLogMapper;
    }

    @Override
    @Transactional
    public VehicleResponse startMaintenance(Long vehicleId) {
        Vehicle vehicle = findLockedOrThrow(vehicleId);
        if (vehicle.getStatus() == VehicleStatus.IN_SERVICE) {
            throw new InvalidOperationException(
                    vehicle.getPlate() + " plakalı araç görevde olduğu için bakıma alınamaz. Önce görevi sonlandırın.");
        }
        if (vehicle.getStatus() == VehicleStatus.IN_MAINTENANCE) {
            throw new InvalidOperationException(vehicle.getPlate() + " plakalı araç zaten bakımda.");
        }
        vehicle.setStatus(VehicleStatus.IN_MAINTENANCE);
        return vehicleMapper.toResponse(vehicle);
    }

    @Override
    @Transactional
    public MaintenanceLogResponse completeMaintenance(Long vehicleId, MaintenanceCompleteRequest request) {
        Vehicle vehicle = findLockedOrThrow(vehicleId);
        if (vehicle.getStatus() != VehicleStatus.IN_MAINTENANCE) {
            throw new InvalidOperationException(
                    vehicle.getPlate() + " plakalı araç bakımda değil; tamamlanacak bir bakım yok.");
        }
        LocalDate today = LocalDate.now();

        MaintenanceLog log = maintenanceLogMapper.toEntity(request);
        log.setVehicle(vehicle);
        log.setMaintenanceDate(today);
        log.setKmAtMaintenance(vehicle.getCurrentKm());
        MaintenanceLog saved = maintenanceLogRepository.save(log);

        vehicle.applyMaintenance(vehicle.getCurrentKm(), today);
        vehicle.setStatus(VehicleStatus.IDLE);

        return maintenanceLogMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceLogResponse> getLogs(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException(VEHICLE, vehicleId);
        }
        return maintenanceLogMapper.toResponseList(
                maintenanceLogRepository.findByVehicleIdOrderByMaintenanceDateDescIdDesc(vehicleId));
    }

    private Vehicle findLockedOrThrow(Long vehicleId) {
        return vehicleRepository.findWithLockById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(VEHICLE, vehicleId));
    }
}
