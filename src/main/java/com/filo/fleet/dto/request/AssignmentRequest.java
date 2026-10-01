package com.filo.fleet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Sürücüye araç atama isteği")
public record AssignmentRequest(
        @Schema(description = "Sürücü kimliği", example = "1")
        @NotNull(message = "Sürücü kimliği zorunludur")
        Long driverId,

        @Schema(description = "Araç kimliği", example = "1")
        @NotNull(message = "Araç kimliği zorunludur")
        Long vehicleId) {
}
