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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("VehicleAssignmentRepository entegrasyon testleri (@EntityGraph, cascade dışı ilişki)")
class VehicleAssignmentRepositoryTest {

    @Autowired
    private VehicleAssignmentRepository assignmentRepository;
    @Autowired
    private DriverRepository driverRepository;
    @Autowired
    private VehicleRepository vehicleRepository;

    private Driver saveDriver(String licenseNumber) {
        Driver driver = new Driver();
        driver.setFirstName("Test");
        driver.setLastName(licenseNumber);
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseClass(LicenseClass.CE);
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        driver.setActive(true);
        return driverRepository.save(driver);
    }

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
        vehicle.setStatus(VehicleStatus.IN_SERVICE);
        return vehicleRepository.save(vehicle);
    }

    @Test
    @DisplayName("existsByDriverIdAndStatus, yalnızca belirtilen durumdaki atamayı doğru tespit eder")
    void existsByDriverIdAndStatus_detectsOnlyMatchingStatus() {
        Driver driver = saveDriver("REPO-ASG-1");
        Vehicle vehicle = saveVehicle("34 REPO 001");
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setStartDate(LocalDateTime.now());
        assignment.setStartKm(1000);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignmentRepository.save(assignment);

        assertThat(assignmentRepository.existsByDriverIdAndStatus(driver.getId(), AssignmentStatus.ACTIVE)).isTrue();
        assertThat(assignmentRepository.existsByDriverIdAndStatus(driver.getId(), AssignmentStatus.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("findWithDetailsById, sürücü ve aracı @EntityGraph ile birlikte getirir")
    void findWithDetailsById_fetchesDriverAndVehicleEagerly() {
        Driver driver = saveDriver("REPO-ASG-2");
        Vehicle vehicle = saveVehicle("34 REPO 002");
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setDriver(driver);
        assignment.setVehicle(vehicle);
        assignment.setStartDate(LocalDateTime.now());
        assignment.setStartKm(1000);
        assignment.setStatus(AssignmentStatus.ACTIVE);
        VehicleAssignment saved = assignmentRepository.save(assignment);

        Optional<VehicleAssignment> found = assignmentRepository.findWithDetailsById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getDriver().getLicenseNumber()).isEqualTo("REPO-ASG-2");
        assertThat(found.get().getVehicle().getPlate()).isEqualTo("34 REPO 002");
    }

    @Test
    @DisplayName("findByVehicleIdOrderByStartDateDesc, o araca ait atamaları en yeniden eskiye sıralı döner")
    void findByVehicleId_returnsAssignmentsNewestFirst() {
        Driver driver = saveDriver("REPO-ASG-3");
        Vehicle vehicle = saveVehicle("34 REPO 003");

        VehicleAssignment older = new VehicleAssignment();
        older.setDriver(driver);
        older.setVehicle(vehicle);
        older.setStartDate(LocalDateTime.now().minusDays(2));
        older.setStartKm(500);
        older.setEndKm(600L);
        older.setEndDate(LocalDateTime.now().minusDays(1));
        older.setStatus(AssignmentStatus.COMPLETED);
        assignmentRepository.save(older);

        VehicleAssignment newer = new VehicleAssignment();
        newer.setDriver(driver);
        newer.setVehicle(vehicle);
        newer.setStartDate(LocalDateTime.now());
        newer.setStartKm(600);
        newer.setStatus(AssignmentStatus.ACTIVE);
        assignmentRepository.save(newer);

        List<VehicleAssignment> result = assignmentRepository.findByVehicleIdOrderByStartDateDesc(vehicle.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(AssignmentStatus.ACTIVE);
        assertThat(result.get(1).getStatus()).isEqualTo(AssignmentStatus.COMPLETED);
    }
}
