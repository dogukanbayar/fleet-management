package com.filo.fleet.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Vehicle entity iş kuralı hesaplamaları")
class VehicleTest {

    private Vehicle vehicle(long currentKm, long lastMaintenanceKm, LocalDate nextMaintenanceDate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setCurrentKm(currentKm);
        vehicle.setLastMaintenanceKm(lastMaintenanceKm);
        vehicle.setMaintenanceIntervalKm(10000);
        vehicle.setNextMaintenanceDate(nextMaintenanceDate);
        return vehicle;
    }

    @Test
    @DisplayName("9999 km yapılmışsa bakım KM bazında henüz gerekmez")
    void isMaintenanceDueByKm_false_whenBelowThreshold() {
        Vehicle vehicle = vehicle(9999, 0, LocalDate.now().plusDays(30));
        assertThat(vehicle.isMaintenanceDueByKm()).isFalse();
    }

    @Test
    @DisplayName("Tam 10000 km yapılmışsa bakım KM bazında gereklidir")
    void isMaintenanceDueByKm_true_whenExactlyAtThreshold() {
        Vehicle vehicle = vehicle(10000, 0, LocalDate.now().plusDays(30));
        assertThat(vehicle.isMaintenanceDueByKm()).isTrue();
    }

    @Test
    @DisplayName("Bakım tarihi bugün veya geçmişse gün bazında bakım gereklidir")
    void isMaintenanceDueByDate_true_whenDateReached() {
        Vehicle vehicle = vehicle(100, 0, LocalDate.now());
        assertThat(vehicle.isMaintenanceDueByDate(LocalDate.now())).isTrue();
    }

    @Test
    @DisplayName("applyMaintenance sonrası son bakım bilgileri ve sonraki tarih güncellenir")
    void applyMaintenance_updatesLastAndNextMaintenance() {
        Vehicle vehicle = vehicle(10000, 0, LocalDate.now());
        vehicle.setMaintenanceIntervalDays(180);

        vehicle.applyMaintenance(10000, LocalDate.of(2025, 1, 1));

        assertThat(vehicle.getLastMaintenanceKm()).isEqualTo(10000);
        assertThat(vehicle.getLastMaintenanceDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(vehicle.getNextMaintenanceDate()).isEqualTo(LocalDate.of(2025, 1, 1).plusDays(180));
    }

    @Test
    @DisplayName("getRemainingKm, negatif olmadan kalan KM'yi döner")
    void getRemainingKm_neverNegative() {
        Vehicle vehicle = vehicle(15000, 0, LocalDate.now().plusDays(30));
        assertThat(vehicle.getRemainingKm()).isZero();
    }
}
