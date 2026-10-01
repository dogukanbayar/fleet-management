package com.filo.fleet.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String plate;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(name = "manufacture_year", nullable = false)
    private int manufactureYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_license_class", nullable = false, length = 10)
    private LicenseClass requiredLicenseClass;

    @Column(name = "current_km", nullable = false)
    private long currentKm;

    @Column(name = "last_maintenance_km", nullable = false)
    private long lastMaintenanceKm;

    @Column(name = "last_maintenance_date", nullable = false)
    private LocalDate lastMaintenanceDate;

    @Column(name = "next_maintenance_date", nullable = false)
    private LocalDate nextMaintenanceDate;

    @Column(name = "maintenance_interval_km", nullable = false)
    private int maintenanceIntervalKm;

    @Column(name = "maintenance_interval_days", nullable = false)
    private int maintenanceIntervalDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus status = VehicleStatus.IDLE;

    /** Atama geçmişi; geçmiş silinmemesi için cascade yoktur. */
    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY)
    private List<VehicleAssignment> assignments = new ArrayList<>();

    /** Bakım kayıtları araca aittir; araç silinirse kayıtları da silinir. */
    @OneToMany(mappedBy = "vehicle", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaintenanceLog> maintenanceLogs = new ArrayList<>();

    public long getKmSinceLastMaintenance() {
        return currentKm - lastMaintenanceKm;
    }

    public long getRemainingKm() {
        return Math.max(0, maintenanceIntervalKm - getKmSinceLastMaintenance());
    }

    public boolean isMaintenanceDueByKm() {
        return getKmSinceLastMaintenance() >= maintenanceIntervalKm;
    }

    public boolean isMaintenanceDueByDate(LocalDate today) {
        return !today.isBefore(nextMaintenanceDate);
    }

    public boolean isMaintenanceDue(LocalDate today) {
        return isMaintenanceDueByKm() || isMaintenanceDueByDate(today);
    }

    public boolean isMaintenanceDue() {
        return isMaintenanceDue(LocalDate.now());
    }

    /** Bakım yapıldığında son bakım bilgilerini ve bir sonraki bakım tarihini günceller. */
    public void applyMaintenance(long km, LocalDate date) {
        this.lastMaintenanceKm = km;
        this.lastMaintenanceDate = date;
        this.nextMaintenanceDate = date.plusDays(maintenanceIntervalDays);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
