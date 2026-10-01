package com.filo.fleet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Araç kilometre güncelleme isteği")
public record KmUpdateRequest(
        @Schema(description = "Yeni kilometre değeri. Mevcut KM değerinden düşük olamaz", example = "15200")
        @NotNull(message = "Yeni KM zorunludur")
        @PositiveOrZero(message = "KM negatif olamaz")
        Long newKm) {
}
