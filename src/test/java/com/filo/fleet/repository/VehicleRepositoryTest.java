package com.filo.fleet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("VehicleRepository entegrasyon testleri (derived query / JPQL)")
class VehicleRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    private Vehicle buildVehicle(String plate, long currentKm, long lastMaintenanceKm, LocalDate nextMaintenanceDate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setPlate(plate);
        vehicle.setBrand("Mercedes");
        vehicle.setModel("Actros");
        vehicle.setManufactureYear(2022);
        vehicle.setRequiredLicenseClass(LicenseClass.CE);
        vehicle.setCurrentKm(currentKm);
        vehicle.setLastMaintenanceKm(lastMaintenanceKm);
        vehicle.setLastMaintenanceDate(LocalDate.now().minusMonths(1));
        vehicle.setNextMaintenanceDate(nextMaintenanceDate);
        vehicle.setMaintenanceIntervalKm(10000);
        vehicle.setMaintenanceIntervalDays(180);
        vehicle.setStatus(VehicleStatus.IDLE);
        return vehicle;
    }

    @Test
    @DisplayName("findMaintenanceDue, KM veya tarih sınırını aşan araçları döner; bakımdakileri hariç tutar")
    void findMaintenanceDue_returnsOnlyEligibleVehicles() {
        Vehicle dueByKm = buildVehicle("34 AAA 001", 11000, 0, LocalDate.now().plusDays(30));
        Vehicle dueByDate = buildVehicle("34 AAA 002", 500, 0, LocalDate.now().minusDays(1));
        Vehicle notDue = buildVehicle("34 AAA 003", 200, 0, LocalDate.now().plusDays(30));
        Vehicle inMaintenance = buildVehicle("34 AAA 004", 20000, 0, LocalDate.now().minusDays(5));
        inMaintenance.setStatus(VehicleStatus.IN_MAINTENANCE);
        vehicleRepository.saveAll(List.of(dueByKm, dueByDate, notDue, inMaintenance));

        List<Vehicle> result = vehicleRepository.findMaintenanceDue(LocalDate.now());

        assertThat(result).extracting(Vehicle::getPlate)
                .containsExactlyInAnyOrder("34 AAA 001", "34 AAA 002");
    }

    @Test
    @DisplayName("existsByPlate, kaydedilmiş plakayı doğru tespit eder")
    void existsByPlate_detectsSavedPlate() {
        vehicleRepository.save(buildVehicle("34 XYZ 999", 100, 0, LocalDate.now().plusDays(180)));

        assertThat(vehicleRepository.existsByPlate("34 XYZ 999")).isTrue();
        assertThat(vehicleRepository.existsByPlate("06 YOK 000")).isFalse();
    }

    @Test
    @DisplayName("findByStatusOrderByPlateAsc, plakaya göre sıralı ve doğru durumdaki araçları döner")
    void findByStatus_returnsSortedVehiclesForGivenStatus() {
        vehicleRepository.saveAll(List.of(
                buildVehicle("34 B 002", 100, 0, LocalDate.now().plusDays(180)),
                buildVehicle("34 A 001", 100, 0, LocalDate.now().plusDays(180))));

        List<Vehicle> result = vehicleRepository.findByStatusOrderByPlateAsc(VehicleStatus.IDLE);

        assertThat(result).extracting(Vehicle::getPlate).containsExactly("34 A 001", "34 B 002");
    }
}
