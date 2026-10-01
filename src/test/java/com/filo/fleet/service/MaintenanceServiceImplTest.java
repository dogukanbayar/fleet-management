package com.filo.fleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.filo.fleet.dto.request.MaintenanceCompleteRequest;
import com.filo.fleet.dto.response.MaintenanceLogResponse;
import com.filo.fleet.entity.MaintenanceLog;
import com.filo.fleet.entity.MaintenanceType;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleStatus;
import com.filo.fleet.exception.InvalidOperationException;
import com.filo.fleet.mapper.MaintenanceLogMapper;
import com.filo.fleet.mapper.VehicleMapper;
import com.filo.fleet.repository.MaintenanceLogRepository;
import com.filo.fleet.repository.VehicleRepository;
import com.filo.fleet.service.impl.MaintenanceServiceImpl;
import java.math.BigDecimal;
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
@DisplayName("MaintenanceServiceImpl birim testleri")
class MaintenanceServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private MaintenanceLogRepository maintenanceLogRepository;
    @Mock
    private VehicleMapper vehicleMapper;
    @Mock
    private MaintenanceLogMapper maintenanceLogMapper;

    @InjectMocks
    private MaintenanceServiceImpl maintenanceService;

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle();
        vehicle.setId(1L);
        vehicle.setPlate("34 ABC 123");
        vehicle.setCurrentKm(12000);
        vehicle.setMaintenanceIntervalKm(10000);
        vehicle.setMaintenanceIntervalDays(180);
    }

    @Test
    @DisplayName("Boştaki araç bakıma alınabilir")
    void startMaintenance_succeeds_whenVehicleIdle() {
        vehicle.setStatus(VehicleStatus.IDLE);
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleMapper.toResponse(vehicle)).thenReturn(null);

        maintenanceService.startMaintenance(1L);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_MAINTENANCE);
    }

    @Test
    @DisplayName("Görevdeki araç bakıma alınamaz")
    void startMaintenance_throws_whenVehicleInService() {
        vehicle.setStatus(VehicleStatus.IN_SERVICE);
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> maintenanceService.startMaintenance(1L))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Bakımda olmayan araç için bakım tamamlanamaz")
    void completeMaintenance_throws_whenVehicleNotInMaintenance() {
        vehicle.setStatus(VehicleStatus.IDLE);
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(vehicle));
        MaintenanceCompleteRequest request = new MaintenanceCompleteRequest(MaintenanceType.PERIODIC,
                BigDecimal.TEN, "Bakım", "Servis");

        assertThatThrownBy(() -> maintenanceService.completeMaintenance(1L, request))
                .isInstanceOf(InvalidOperationException.class);
    }

    @Test
    @DisplayName("Bakım tamamlanınca kayıt oluşur, araç IDLE olur ve son bakım bilgisi güncellenir")
    void completeMaintenance_savesLogAndResetsVehicle() {
        vehicle.setStatus(VehicleStatus.IN_MAINTENANCE);
        vehicle.setLastMaintenanceKm(0);
        vehicle.setLastMaintenanceDate(LocalDate.now().minusMonths(6));
        MaintenanceCompleteRequest request = new MaintenanceCompleteRequest(MaintenanceType.PERIODIC,
                BigDecimal.TEN, "Yağ değişimi", "Servis A.Ş.");
        MaintenanceLog mappedLog = new MaintenanceLog();
        when(vehicleRepository.findWithLockById(1L)).thenReturn(Optional.of(vehicle));
        when(maintenanceLogMapper.toEntity(request)).thenReturn(mappedLog);
        when(maintenanceLogRepository.save(mappedLog)).thenReturn(mappedLog);
        when(maintenanceLogMapper.toResponse(mappedLog)).thenReturn(sampleLogResponse());

        MaintenanceLogResponse response = maintenanceService.completeMaintenance(1L, request);

        assertThat(response).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IDLE);
        assertThat(vehicle.getLastMaintenanceKm()).isEqualTo(12000L);
        assertThat(mappedLog.getKmAtMaintenance()).isEqualTo(12000L);
    }

    private MaintenanceLogResponse sampleLogResponse() {
        return new MaintenanceLogResponse(1L, 1L, "34 ABC 123", MaintenanceType.PERIODIC,
                LocalDate.now(), 12000L, BigDecimal.TEN, "Yağ değişimi", "Servis A.Ş.");
    }
}
