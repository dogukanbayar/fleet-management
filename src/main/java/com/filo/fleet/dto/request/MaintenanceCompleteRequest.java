package com.filo.fleet.dto.request;

import com.filo.fleet.entity.MaintenanceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Bakımı tamamlama isteği; bakım kaydı oluşturur")
public record MaintenanceCompleteRequest(
        @Schema(description = "Bakım türü", example = "PERIODIC")
        @NotNull(message = "Bakım türü zorunludur")
        MaintenanceType maintenanceType,

        @Schema(description = "Bakım maliyeti (TL)", example = "4500.50")
        @PositiveOrZero(message = "Maliyet negatif olamaz")
        BigDecimal cost,

        @Schema(description = "Yapılan işlemlerin açıklaması", example = "Yağ ve filtre değişimi, fren kontrolü")
        @Size(max = 500, message = "Açıklama en fazla 500 karakter olabilir")
        String description,

        @Schema(description = "Bakımı yapan servis / kişi", example = "Yetkili Servis A.Ş.")
        @Size(max = 100, message = "Servis adı en fazla 100 karakter olabilir")
        String performedBy) {
}
