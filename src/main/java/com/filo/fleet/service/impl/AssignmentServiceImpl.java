package com.filo.fleet.service.impl;

import com.filo.fleet.dto.request.AssignmentCompleteRequest;
import com.filo.fleet.dto.request.AssignmentRequest;
import com.filo.fleet.dto.response.AssignmentResponse;
import com.filo.fleet.entity.AssignmentStatus;
import com.filo.fleet.entity.Driver;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleAssignment;
import com.filo.fleet.entity.VehicleStatus;
import com.filo.fleet.exception.DriverNotAvailableException;
import com.filo.fleet.exception.InvalidOperationException;
import com.filo.fleet.exception.MaintenanceRequiredException;
import com.filo.fleet.exception.ResourceNotFoundException;
import com.filo.fleet.mapper.AssignmentMapper;
import com.filo.fleet.repository.DriverRepository;
import com.filo.fleet.repository.VehicleAssignmentRepository;
import com.filo.fleet.repository.VehicleRepository;
import com.filo.fleet.service.AssignmentService;
import com.filo.fleet.util.KilometerValidator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private static final String DRIVER = "Sürücü";
    private static final String VEHICLE = "Araç";
    private static final String ASSIGNMENT = "Atama";

    private final VehicleAssignmentRepository assignmentRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final AssignmentMapper assignmentMapper;

    public AssignmentServiceImpl(VehicleAssignmentRepository assignmentRepository,
                                 DriverRepository driverRepository,
                                 VehicleRepository vehicleRepository,
                                 AssignmentMapper assignmentMapper) {
        this.assignmentRepository = assignmentRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.assignmentMapper = assignmentMapper;
    }

    @Override
    @Transactional
    public AssignmentResponse assign(AssignmentRequest request) {
        // Kilit sırası her zaman sürücü -> araç (deadlock önleme).
        Driver driver = driverRepository.findWithLockById(request.driverId())
                .orElseThrow(() -> new ResourceNotFoundException(DRIVER, request.driverId()));
        Vehicle vehicle = vehicleRepository.findWithLockById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException(VEHICLE, request.vehicleId()));

        LocalDate today = LocalDate.now();
        validateDriver(driver, vehicle, today);
        validateVehicle(vehicle, today);

        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setStartDate(LocalDateTime.now());
        assignment.setStartKm(vehicle.getCurrentKm());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        vehicle.setStatus(VehicleStatus.IN_SERVICE);

        return assignmentMapper.toResponse(assignmentRepository.save(assignment));
    }

    @Override
    @Transactional
    public AssignmentResponse complete(Long assignmentId, AssignmentCompleteRequest request) {
        VehicleAssignment assignment = assignmentRepository.findWithDetailsById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT, assignmentId));
        if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
            throw new InvalidOperationException("Bu atama zaten sonlandırılmış.");
        }
        Long vehicleId = assignment.getVehicle().getId();
        Vehicle vehicle = vehicleRepository.findWithLockById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(VEHICLE, vehicleId));

        long endKm = request.endKm();
        KilometerValidator.validateNotDecreasing(vehicle.getPlate(), vehicle.getCurrentKm(), endKm);
        vehicle.setCurrentKm(endKm);
        vehicle.setStatus(VehicleStatus.IDLE);
        assignment.setEndKm(endKm);
        assignment.setEndDate(LocalDateTime.now());
        assignment.setStatus(AssignmentStatus.COMPLETED);

        return assignmentMapper.toResponse(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponse getById(Long id) {
        return assignmentRepository.findWithDetailsById(id)
                .map(assignmentMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(ASSIGNMENT, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAll(AssignmentStatus status) {
        List<VehicleAssignment> assignments = status == null
                ? assignmentRepository.findAllByOrderByStartDateDesc()
                : assignmentRepository.findByStatusOrderByStartDateDesc(status);
        return assignmentMapper.toResponseList(assignments);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getByDriver(Long driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw new ResourceNotFoundException(DRIVER, driverId);
        }
        return assignmentMapper.toResponseList(assignmentRepository.findByDriverIdOrderByStartDateDesc(driverId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponse> getByVehicle(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException(VEHICLE, vehicleId);
        }
        return assignmentMapper.toResponseList(assignmentRepository.findByVehicleIdOrderByStartDateDesc(vehicleId));
    }

    private void validateDriver(Driver driver, Vehicle vehicle, LocalDate today) {
        if (!driver.isActive()) {
            throw new DriverNotAvailableException(driver.getFullName() + " pasif durumda olduğu için araç atanamaz.");
        }
        if (driver.getLicenseExpiryDate().isBefore(today)) {
            throw new DriverNotAvailableException(
                    driver.getFullName() + " adlı sürücünün ehliyet süresi dolmuş (" + driver.getLicenseExpiryDate() + ").");
        }
        if (!driver.getLicenseClass().covers(vehicle.getRequiredLicenseClass())) {
            throw new DriverNotAvailableException(
                    driver.getFullName() + " adlı sürücünün ehliyet sınıfı (" + driver.getLicenseClass()
                            + ") yetersiz. Bu araç için en az " + vehicle.getRequiredLicenseClass() + " gerekir.");
        }
        if (assignmentRepository.existsByDriverIdAndStatus(driver.getId(), AssignmentStatus.ACTIVE)) {
            throw new DriverNotAvailableException(
                    driver.getFullName() + " adlı sürücü halihazırda başka bir araçta aktif görevde.");
        }
    }

    private void validateVehicle(Vehicle vehicle, LocalDate today) {
        if (vehicle.getStatus() == VehicleStatus.IN_SERVICE) {
            throw new InvalidOperationException(vehicle.getPlate() + " plakalı araç zaten görevde.");
        }
        if (vehicle.getStatus() == VehicleStatus.IN_MAINTENANCE) {
            throw new InvalidOperationException(vehicle.getPlate() + " plakalı araç şu anda bakımda.");
        }
        if (vehicle.isMaintenanceDueByKm()) {
            throw new MaintenanceRequiredException(vehicle.getPlate() + " plakalı araç son bakımından bu yana "
                    + vehicle.getKmSinceLastMaintenance() + " km yapmış (sınır: " + vehicle.getMaintenanceIntervalKm()
                    + " km). Göreve atanamaz, bakıma alınmalıdır.");
        }
        if (vehicle.isMaintenanceDueByDate(today)) {
            throw new MaintenanceRequiredException(vehicle.getPlate() + " plakalı aracın bakım tarihi ("
                    + vehicle.getNextMaintenanceDate() + ") geçmiş. Göreve atanamaz, bakıma alınmalıdır.");
        }
    }
}
