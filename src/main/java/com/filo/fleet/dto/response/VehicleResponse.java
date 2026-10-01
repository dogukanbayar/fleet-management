package com.filo.fleet.dto.response;

import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.entity.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Araç bilgileri ve bakım durumu")
public record VehicleResponse(
        @Schema(description = "Araç kimliği", example = "1") Long id,
        @Schema(description = "Plaka", example = "34 ABC 123") String plate,
        @Schema(description = "Marka", example = "Mercedes-Benz") String brand,
        @Schema(description = "Model", example = "Actros 1848") String model,
        @Schema(description = "Üretim yılı", example = "2022") int manufactureYear,
        @Schema(description = "Gerekli ehliyet sınıfı", example = "CE") LicenseClass requiredLicenseClass,
        @Schema(description = "Güncel kilometre", example = "12500") long currentKm,
        @Schema(description = "Araç durumu: IN_SERVICE (görevde), IN_MAINTENANCE (bakımda), IDLE (boşta)",
                example = "IDLE") VehicleStatus status,
        @Schema(description = "Son bakımın yapıldığı KM", example = "10000") long lastMaintenanceKm,
        @Schema(description = "Son bakım tarihi", example = "2025-01-15") LocalDate lastMaintenanceDate,
        @Schema(description = "Bir sonraki bakımın gün bazlı son tarihi", example = "2025-07-14") LocalDate nextMaintenanceDate,
        @Schema(description = "KM bazlı bakım aralığı", example = "10000") int maintenanceIntervalKm,
        @Schema(description = "Gün bazlı bakım aralığı", example = "180") int maintenanceIntervalDays,
        @Schema(description = "Son bakımdan bu yana yapılan KM", example = "2500") long kmSinceLastMaintenance,
        @Schema(description = "Bakıma kalan KM (0 ise bakım zamanı gelmiştir)", example = "7500") long remainingKm,
        @Schema(description = "KM veya gün bazlı bakım zamanı geldi mi?", example = "false") boolean maintenanceDue) {
}
