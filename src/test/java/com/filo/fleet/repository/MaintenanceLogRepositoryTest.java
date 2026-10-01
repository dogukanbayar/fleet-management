package com.filo.fleet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.entity.MaintenanceLog;
import com.filo.fleet.entity.MaintenanceType;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("MaintenanceLogRepository entegrasyon testleri (cascade / orphanRemoval)")
class MaintenanceLogRepositoryTest {

    @Autowired
    private MaintenanceLogRepository maintenanceLogRepository;
    @Autowired
    private VehicleRepository vehicleRepository;

    private Vehicle saveVehicle(String plate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate(plate);
        vehicle.setBrand("Ford");
        vehicle.setModel("Cargo");
        vehicle.setManufactureYear(2021);
        vehicle.setRequiredLicenseClass(LicenseClass.CE);
        vehicle.setCurrentKm(1000);
        vehicle.setLastMaintenanceKm(0);
        vehicle.setLastMaintenanceDate(LocalDate.now());
        vehicle.setNextMaintenanceDate(LocalDate.now().plusDays(180));
        vehicle.setMaintenanceIntervalKm(10000);
        vehicle.setMaintenanceIntervalDays(180);
        vehicle.setStatus(VehicleStatus.IDLE);
        return vehicleRepository.save(vehicle);
    }

    private MaintenanceLog buildLog(Vehicle vehicle, LocalDate date, long km) {
        MaintenanceLog log = new MaintenanceLog();
        log.setVehicle(vehicle);
        log.setMaintenanceType(MaintenanceType.PERIODIC);
        log.setMaintenanceDate(date);
        log.setKmAtMaintenance(km);
        log.setCost(BigDecimal.valueOf(500));
        log.setDescription("Rutin bakım");
        log.setPerformedBy("Servis");
        return log;
    }

    @Test
    @DisplayName("findByVehicleIdOrderByMaintenanceDateDescIdDesc, en yeni bakım kaydını önce döner")
    void findByVehicleId_returnsLogsNewestFirst() {
        Vehicle vehicle = saveVehicle("34 REPO 010");
        maintenanceLogRepository.save(buildLog(vehicle, LocalDate.now().minusMonths(6), 1000));
        maintenanceLogRepository.save(buildLog(vehicle, LocalDate.now(), 11000));

        List<MaintenanceLog> result = maintenanceLogRepository.findByVehicleIdOrderByMaintenanceDateDescIdDesc(vehicle.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getKmAtMaintenance()).isEqualTo(11000L);
        assertThat(result.get(1).getKmAtMaintenance()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Araç silinince bakım kayıtları da cascade ile silinir (orphanRemoval)")
    void deletingVehicle_cascadesToMaintenanceLogs() {
        Vehicle vehicle = saveVehicle("34 REPO 011");
        vehicle.getMaintenanceLogs().add(buildLog(vehicle, LocalDate.now(), 5000));
        vehicle = vehicleRepository.save(vehicle);
        Long vehicleId = vehicle.getId();

        assertThat(maintenanceLogRepository.findByVehicleIdOrderByMaintenanceDateDescIdDesc(vehicleId)).hasSize(1);

        vehicleRepository.delete(vehicle);
        vehicleRepository.flush();

        assertThat(maintenanceLogRepository.findByVehicleIdOrderByMaintenanceDateDescIdDesc(vehicleId)).isEmpty();
    }
}
