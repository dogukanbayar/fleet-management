package com.filo.fleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.filo.fleet.dto.request.VehicleRequest;
import com.filo.fleet.dto.response.VehicleResponse;
import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleStatus;
import com.filo.fleet.exception.DuplicateResourceException;
import com.filo.fleet.exception.InvalidKilometerUpdateException;
import com.filo.fleet.exception.ResourceNotFoundException;
import com.filo.fleet.mapper.VehicleMapper;
import com.filo.fleet.repository.VehicleAssignmentRepository;
import com.filo.fleet.repository.VehicleRepository;
import com.filo.fleet.service.impl.VehicleServiceImpl;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("VehicleServiceImpl birim testleri")
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private VehicleAssignmentRepository assignmentRepository;
    @Mock
    private VehicleMapper vehicleMapper;

    private VehicleServiceImpl vehicleService;

    @BeforeEach
    void setUp() {
        vehicleService = new VehicleServiceImpl(vehicleRepository, assignmentRepository, vehicleMapper, 10000, 180);
    }

    private Vehicle sampleVehicle(long currentKm) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setPlate("34 ABC 123");
        vehicle.setRequiredLicenseClass(LicenseClass.CE);
        vehicle.setCurrentKm(currentKm);
        vehicle.setLastMaintenanceKm(0);
        vehicle.setLastMaintenanceDate(LocalDate.now());
        vehicle.setNextMaintenanceDate(LocalDate.now().plusDays(180));
        vehicle.setMaintenanceIntervalKm(10000);
        vehicle.setMaintenanceIntervalDays(180);
        vehicle.setStatus(VehicleStatus.IDLE);
        return vehicle;
    }

    @Test
    @DisplayName("Benzersiz plaka ile araç oluşturulur ve varsayılan bakım aralıkları atanır")
    void create_savesVehicle_withDefaultMaintenanceIntervals() {
        VehicleRequest request = new VehicleRequest("34 abc 123", "Mercedes", "Actros", 2022,
                LicenseClass.CE, 500L, null, null, null, null);
        Vehicle mapped = new Vehicle();
        when(vehicleRepository.existsByPlate("34 ABC 123")).thenReturn(false);
        when(vehicleMapper.toEntity(request)).thenReturn(mapped);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleMapper.toResponse(any(Vehicle.class))).thenReturn(sampleResponse());

        vehicleService.create(request);

        assertThat(mapped.getPlate()).isEqualTo("34 ABC 123");
        assertThat(mapped.getMaintenanceIntervalKm()).isEqualTo(10000);
        assertThat(mapped.getMaintenanceIntervalDays()).isEqualTo(180);
        assertThat(mapped.getStatus()).isEqualTo(VehicleStatus.IDLE);
    }

    @Test
    @DisplayName("Plaka zaten kayıtlıysa DuplicateResourceException fırlatılır")
    void create_throws_whenPlateDuplicated() {
        VehicleRequest request = new VehicleRequest("34 ABC 123", "Mercedes", "Actros", 2022,
                LicenseClass.CE, 500L, null, null, null, null);
        when(vehicleRepository.existsByPlate("34 ABC 123")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.create(request)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("Yeni KM, mevcut KM'den düşükse InvalidKilometerUpdateException fırlatılır")
    void updateKm_throws_whenNewKmLowerThanCurrent() {
        Vehicle vehicle = sampleVehicle(5000);
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.updateKm(1L, 4000L))
                .isInstanceOf(InvalidKilometerUpdateException.class);
    }

    @Test
    @DisplayName("Yeni KM mevcut KM'ye eşit veya büyükse güncellenir")
    void updateKm_updates_whenNewKmValid() {
        Vehicle vehicle = sampleVehicle(5000);
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleMapper.toResponse(vehicle)).thenReturn(sampleResponse());

        vehicleService.updateKm(1L, 5500L);

        assertThat(vehicle.getCurrentKm()).isEqualTo(5500L);
    }

    @Test
    @DisplayName("Var olmayan araç için KM güncellemesi ResourceNotFoundException fırlatır")
    void updateKm_throws_whenVehicleMissing() {
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.updateKm(1L, 100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private VehicleResponse sampleResponse() {
        return new VehicleResponse(1L, "34 ABC 123", "Mercedes", "Actros", 2022, LicenseClass.CE, 500L,
                VehicleStatus.IDLE, 500L, LocalDate.now(), LocalDate.now().plusDays(180), 10000, 180, 0L, 10000L, false);
    }
}
