package com.filo.fleet.dto.response;

import com.filo.fleet.entity.AssignmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Araç atama (görev) kaydı")
public record AssignmentResponse(
        @Schema(description = "Atama kimliği", example = "1") Long id,
        @Schema(description = "Sürücü kimliği", example = "1") Long driverId,
        @Schema(description = "Sürücü ad soyad", example = "Ali Yılmaz") String driverFullName,
        @Schema(description = "Araç kimliği", example = "1") Long vehicleId,
        @Schema(description = "Araç plakası", example = "34 ABC 123") String vehiclePlate,
        @Schema(description = "Görev başlangıç zamanı") LocalDateTime startDate,
        @Schema(description = "Görev bitiş zamanı; aktif görevde boştur") LocalDateTime endDate,
        @Schema(description = "Görev başlangıç KM", example = "12500") long startKm,
        @Schema(description = "Görev bitiş KM; aktif görevde boştur", example = "13800") Long endKm,
        @Schema(description = "Atama durumu", example = "ACTIVE") AssignmentStatus status,
        @Schema(description = "Aracın bakım zamanı geldi mi? Görev bitiminde bakım uyarısı olarak kullanılır",
                example = "false") boolean vehicleMaintenanceDue) {
}
