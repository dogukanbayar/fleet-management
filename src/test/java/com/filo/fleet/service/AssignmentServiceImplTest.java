package com.filo.fleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.filo.fleet.dto.request.AssignmentCompleteRequest;
import com.filo.fleet.dto.request.AssignmentRequest;
import com.filo.fleet.dto.response.AssignmentResponse;
import com.filo.fleet.entity.AssignmentStatus;
import com.filo.fleet.entity.Driver;
import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleAssignment;
import com.filo.fleet.entity.VehicleStatus;
import com.filo.fleet.exception.DriverNotAvailableException;
import com.filo.fleet.exception.InvalidOperationException;
import com.filo.fleet.exception.MaintenanceRequiredException;
import com.filo.fleet.mapper.AssignmentMapper;
import com.filo.fleet.repository.DriverRepository;
import com.filo.fleet.repository.VehicleAssignmentRepository;
import com.filo.fleet.repository.VehicleRepository;
import com.filo.fleet.service.impl.AssignmentServiceImpl;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentServiceImpl birim testleri (iş kuralları)")
class AssignmentServiceImplTest {

    @Mock
    private VehicleAssignmentRepository assignmentRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private AssignmentMapper assignmentMapper;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    private Driver activeDriver;
    private Vehicle idleVehicle;

    @BeforeEach
    void setUp() {
        activeDriver = new Driver();
        activeDriver.setId(1L);
        activeDriver.setFirstName("Ali");
        activeDriver.setLastName("Yılmaz");
        activeDriver.setActive(true);
        activeDriver.setLicenseClass(LicenseClass.CE);
        activeDriver.setLicenseExpiryDate(LocalDate.now().plusYears(1));

        idleVehicle = new Vehicle();
        idleVehicle.setId(1L);
        idleVehicle.setPlate("34 ABC 123");
        idleVehicle.setRequiredLicenseClass(LicenseClass.CE);
        idleVehicle.setStatus(VehicleStatus.IDLE);
        idleVehicle.setCurrentKm(1000);
        idleVehicle.setLastMaintenanceKm(0);
        idleVehicle.setLastMaintenanceDate(LocalDate.now());
        idleVehicle.setNextMaintenanceDate(LocalDate.now().plusDays(180));
        idleVehicle.setMaintenanceIntervalKm(10000);
        idleVehicle.setMaintenanceIntervalDays(180);
    }

    private AssignmentRequest request() {
        return new AssignmentRequest(1L, 1L);
    }

    @Test
    @DisplayName("Uygun sürücü ve araçla atama başarıyla oluşturulur, araç IN_SERVICE olur")
    void assign_succeeds_whenDriverAndVehicleEligible() {
        when(driverRepository.findWithLockById(1L)).thenReturn(Optional.of(activeDriver));
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(idleVehicle));
        when(assignmentRepository.existsByDriverIdAndStatus(1L, AssignmentStatus.ACTIVE)).thenReturn(false);
        when(assignmentRepository.save(any(VehicleAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentMapper.toResponse(any(VehicleAssignment.class))).thenReturn(sampleResponse());

        AssignmentResponse response = assignmentService.assign(request());

        assertThat(response).isNotNull();
        assertThat(idleVehicle.getStatus()).isEqualTo(VehicleStatus.IN_SERVICE);
    }

    @Test
    @DisplayName("Ehliyet sınıfı yetersiz sürücüye araç atanamaz")
    void assign_throws_whenLicenseClassInsufficient() {
        activeDriver.setLicenseClass(LicenseClass.B);
        when(driverRepository.findWithLockById(1L)).thenReturn(Optional.of(activeDriver));
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(idleVehicle));

        assertThatThrownBy(() -> assignmentService.assign(request()))
                .isInstanceOf(DriverNotAvailableException.class);
    }

    @Test
    @DisplayName("Ehliyet süresi dolmuş sürücüye araç atanamaz")
    void assign_throws_whenLicenseExpired() {
        activeDriver.setLicenseExpiryDate(LocalDate.now().minusDays(1));
        when(driverRepository.findWithLockById(1L)).thenReturn(Optional.of(activeDriver));
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(idleVehicle));

        assertThatThrownBy(() -> assignmentService.assign(request()))
                .isInstanceOf(DriverNotAvailableException.class);
    }

    @Test
    @DisplayName("Zaten aktif görevi olan sürücüye yeni araç atanamaz")
    void assign_throws_whenDriverAlreadyOnActiveAssignment() {
        when(driverRepository.findWithLockById(1L)).thenReturn(Optional.of(activeDriver));
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(idleVehicle));
        when(assignmentRepository.existsByDriverIdAndStatus(1L, AssignmentStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> assignmentService.assign(request()))
                .isInstanceOf(DriverNotAvailableException.class);
    }

    @Test
    @DisplayName("Bakım KM sınırını aşan araç göreve atanamaz")
    void assign_throws_whenVehicleMaintenanceDueByKm() {
        idleVehicle.setCurrentKm(10500);
        idleVehicle.setLastMaintenanceKm(0);
        when(driverRepository.findWithLockById(1L)).thenReturn(Optional.of(activeDriver));
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(idleVehicle));

        assertThatThrownBy(() -> assignmentService.assign(request()))
                .isInstanceOf(MaintenanceRequiredException.class);
    }

    @Test
    @DisplayName("Bakım tarihi geçmiş araç göreve atanamaz")
    void assign_throws_whenVehicleMaintenanceDueByDate() {
        idleVehicle.setNextMaintenanceDate(LocalDate.now().minusDays(1));
        when(driverRepository.findWithLockById(1L)).thenReturn(Optional.of(activeDriver));
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(idleVehicle));

        assertThatThrownBy(() -> assignmentService.assign(request()))
                .isInstanceOf(MaintenanceRequiredException.class);
    }

    @Test
    @DisplayName("Görevde olan araç tekrar atanamaz")
    void assign_throws_whenVehicleAlreadyInService() {
        idleVehicle.setStatus(VehicleStatus.IN_SERVICE);
        when(driverRepository.findWithLockById(1L)).thenReturn(Optional.of(activeDriver));
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(idleVehicle));

        assertThatThrownBy(() -> assignmentService.assign(request()))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Aktif olmayan atama tamamlanmaya çalışılırsa InvalidOperationException fırlatılır")
    void complete_throws_whenAssignmentAlreadyCompleted() {
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setId(1L);
        assignment.setDriver(activeDriver);
        assignment.setVehicle(idleVehicle);
        assignment.setStatus(AssignmentStatus.COMPLETED);
        when(assignmentRepository.findWithDetailsById(1L)).thenReturn(Optional.of(assignment));

        assertThatThrownBy(() -> assignmentService.complete(1L, new AssignmentCompleteRequest(2000L)))
                .isInstanceOf(InvalidOperationException.class);
    }

    private AssignmentResponse sampleResponse() {
        return new AssignmentResponse(1L, 1L, "Ali Yılmaz", 1L, "34 ABC 123",
                null, null, 1000L, null, AssignmentStatus.ACTIVE, false);
    }
}
