package com.filo.fleet.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.filo.fleet.entity.AssignmentStatus;
import com.filo.fleet.entity.Driver;
import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleAssignment;
import com.filo.fleet.entity.VehicleStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DriverRepository entegrasyon testleri (JPQL findAvailableDrivers)")
class DriverRepositoryTest {

    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private VehicleAssignmentRepository assignmentRepository;

    private Driver buildDriver(String licenseNumber, boolean active, LocalDate expiry) {
        Driver driver = new Driver();
        driver.setFirstName("Test");
        driver.setLastName(licenseNumber);
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseClass(LicenseClass.CE);
        driver.setLicenseExpiryDate(expiry);
        driver.setActive(active);
        return driver;
    }

    @Test
    @DisplayName("findAvailableDrivers; pasif, ehliyeti dolmuş ve aktif görevdeki sürücüleri hariç tutar")
    void findAvailableDrivers_excludesIneligibleDrivers() {
        Driver eligible = driverRepository.save(buildDriver("LIC-1", true, LocalDate.now().plusYears(1)));
        driverRepository.save(buildDriver("LIC-2", false, LocalDate.now().plusYears(1)));
        driverRepository.save(buildDriver("LIC-3", true, LocalDate.now().minusDays(1)));
        Driver busyDriver = driverRepository.save(buildDriver("LIC-4", true, LocalDate.now().plusYears(1)));

        Vehicle vehicle = new Vehicle();
        vehicle.setPlate("34 ZZZ 111");
        vehicle.setBrand("Ford");
        vehicle.setModel("Cargo");
        vehicle.setManufactureYear(2021);
        vehicle.setRequiredLicenseClass(LicenseClass.CE);
        vehicle.setCurrentKm(100);
        vehicle.setLastMaintenanceKm(0);
        vehicle.setLastMaintenanceDate(LocalDate.now());
        vehicle.setNextMaintenanceDate(LocalDate.now().plusDays(180));
        vehicle.setMaintenanceIntervalKm(10000);
        vehicle.setMaintenanceIntervalDays(180);
        vehicle.setStatus(VehicleStatus.IN_SERVICE);
        vehicleRepository.save(vehicle);

        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setDriver(busyDriver);
        assignment.setVehicle(vehicle);
        assignment.setStartDate(LocalDateTime.now());
        assignment.setStartKm(100);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignmentRepository.save(assignment);

        List<Driver> result = driverRepository.findAvailableDrivers(LocalDate.now());

        assertThat(result).extracting(Driver::getLicenseNumber).containsExactly(eligible.getLicenseNumber());
    }
}
