package com.filo.fleet.service.impl;

import com.filo.fleet.dto.request.VehicleRequest;
import com.filo.fleet.dto.response.VehicleResponse;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleStatus;
import com.filo.fleet.exception.DuplicateResourceException;
import com.filo.fleet.exception.InvalidKilometerUpdateException;
import com.filo.fleet.exception.InvalidOperationException;
import com.filo.fleet.exception.ResourceNotFoundException;
import com.filo.fleet.mapper.VehicleMapper;
import com.filo.fleet.repository.VehicleAssignmentRepository;
import com.filo.fleet.repository.VehicleRepository;
import com.filo.fleet.service.VehicleService;
import com.filo.fleet.util.KilometerValidator;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleServiceImpl implements VehicleService {

    private static final String VEHICLE = "Araç";

    private final VehicleRepository vehicleRepository;
    private final VehicleAssignmentRepository assignmentRepository;
    private final VehicleMapper vehicleMapper;
    private final int defaultIntervalKm;
    private final int defaultIntervalDays;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              VehicleAssignmentRepository assignmentRepository,
                              VehicleMapper vehicleMapper,
                              @Value("${fleet.maintenance.default-interval-km:10000}") int defaultIntervalKm,
                              @Value("${fleet.maintenance.default-interval-days:180}") int defaultIntervalDays) {
        this.vehicleRepository = vehicleRepository;
        this.assignmentRepository = assignmentRepository;
        this.vehicleMapper = vehicleMapper;
        this.defaultIntervalKm = defaultIntervalKm;
        this.defaultIntervalDays = defaultIntervalDays;
    }

    @Override
    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        String plate = normalizePlate(request.plate());
        if (vehicleRepository.existsByPlate(plate)) {
            throw duplicatePlate(plate);
        }
        Vehicle vehicle = vehicleMapper.toEntity(request);
        vehicle.setPlate(plate);
        vehicle.setStatus(VehicleStatus.IDLE);
        vehicle.setCurrentKm(request.currentKm());
        vehicle.setMaintenanceIntervalKm(defaultIntervalKm);
        vehicle.setMaintenanceIntervalDays(defaultIntervalDays);
        vehicle.applyMaintenance(request.currentKm(), LocalDate.now());
        applyMaintenanceSettings(vehicle, request);
        return vehicleMapper.toResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getById(Long id) {
        return vehicleMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAll(VehicleStatus status) {
        List<Vehicle> vehicles = status == null
                ? vehicleRepository.findAllByOrderByPlateAsc()
                : vehicleRepository.findByStatusOrderByPlateAsc(status);
        return vehicleMapper.toResponseList(vehicles);
    }

    @Override
    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle vehicle = findOrThrow(id);
        String plate = normalizePlate(request.plate());
        if (vehicleRepository.existsByPlateAndIdNot(plate, id)) {
            throw duplicatePlate(plate);
        }
        validateNewKm(vehicle, request.currentKm());
        vehicleMapper.updateEntity(request, vehicle);
        vehicle.setPlate(plate);
        vehicle.setCurrentKm(request.currentKm());
        applyMaintenanceSettings(vehicle, request);
        return vehicleMapper.toResponse(vehicle);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = findOrThrow(id);
        if (assignmentRepository.existsByVehicleId(id)) {
            throw new InvalidOperationException("Atama geçmişi bulunan araç silinemez.");
        }
        vehicleRepository.delete(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponse updateKm(Long id, Long newKm) {
        Vehicle vehicle = vehicleRepository.findWithLockById(id)
                .orElseThrow(() -> new ResourceNotFoundException(VEHICLE, id));
        validateNewKm(vehicle, newKm);
        vehicle.setCurrentKm(newKm);
        return vehicleMapper.toResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMaintenanceDue() {
        return vehicleMapper.toResponseList(vehicleRepository.findMaintenanceDue(LocalDate.now()));
    }

    private void applyMaintenanceSettings(Vehicle vehicle, VehicleRequest request) {
        if (request.maintenanceIntervalKm() != null) {
            vehicle.setMaintenanceIntervalKm(request.maintenanceIntervalKm());
        }
        if (request.maintenanceIntervalDays() != null) {
            vehicle.setMaintenanceIntervalDays(request.maintenanceIntervalDays());
        }
        long lastKm = request.lastMaintenanceKm() != null ? request.lastMaintenanceKm() : vehicle.getLastMaintenanceKm();
        LocalDate lastDate = request.lastMaintenanceDate() != null
                ? request.lastMaintenanceDate() : vehicle.getLastMaintenanceDate();
        if (lastKm > vehicle.getCurrentKm()) {
            throw new InvalidKilometerUpdateException(
                    "Son bakım KM değeri (" + lastKm + ") aracın güncel KM değerinden ("
                            + vehicle.getCurrentKm() + ") büyük olamaz.");
        }
        vehicle.applyMaintenance(lastKm, lastDate);
    }

    private void validateNewKm(Vehicle vehicle, long newKm) {
        KilometerValidator.validateNotDecreasing(vehicle.getPlate(), vehicle.getCurrentKm(), newKm);
    }

    private String normalizePlate(String plate) {
        return plate.trim().toUpperCase(Locale.ROOT);
    }

    private Vehicle findOrThrow(Long id) {
        return vehicleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(VEHICLE, id));
    }

    private DuplicateResourceException duplicatePlate(String plate) {
        return new DuplicateResourceException("Bu plaka ile kayıtlı bir araç zaten var: " + plate);
    }
}
