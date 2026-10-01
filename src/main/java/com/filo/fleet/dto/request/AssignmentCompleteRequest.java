package com.filo.fleet.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Görevi (atamayı) sonlandırma isteği")
public record AssignmentCompleteRequest(
        @Schema(description = "Görev bitişindeki kilometre. Aracın mevcut KM değerinden düşük olamaz", example = "13800")
        @NotNull(message = "Bitiş KM zorunludur")
        @PositiveOrZero(message = "KM negatif olamaz")
        Long endKm) {
}
