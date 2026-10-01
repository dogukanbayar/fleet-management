package com.filo.fleet.dto.response;

import com.filo.fleet.entity.MaintenanceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Bakım kaydı")
public record MaintenanceLogResponse(
        @Schema(description = "Bakım kaydı kimliği", example = "1") Long id,
        @Schema(description = "Araç kimliği", example = "1") Long vehicleId,
        @Schema(description = "Araç plakası", example = "34 ABC 123") String vehiclePlate,
        @Schema(description = "Bakım türü", example = "PERIODIC") MaintenanceType maintenanceType,
        @Schema(description = "Bakım tarihi", example = "2025-06-01") LocalDate maintenanceDate,
        @Schema(description = "Bakım anındaki KM", example = "20100") long kmAtMaintenance,
        @Schema(description = "Maliyet (TL)", example = "4500.50") BigDecimal cost,
        @Schema(description = "Açıklama") String description,
        @Schema(description = "Bakımı yapan", example = "Yetkili Servis A.Ş.") String performedBy) {
}
