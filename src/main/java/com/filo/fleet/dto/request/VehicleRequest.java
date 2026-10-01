package com.filo.fleet.dto.request;

import com.filo.fleet.entity.LicenseClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Araç oluşturma / güncelleme isteği")
public record VehicleRequest(
        @Schema(description = "Benzersiz plaka", example = "34 ABC 123")
        @NotBlank(message = "Plaka boş olamaz")
        @Size(max = 20, message = "Plaka en fazla 20 karakter olabilir")
        String plate,

        @Schema(description = "Marka", example = "Mercedes-Benz")
        @NotBlank(message = "Marka boş olamaz")
        @Size(max = 50, message = "Marka en fazla 50 karakter olabilir")
        String brand,

        @Schema(description = "Model", example = "Actros 1848")
        @NotBlank(message = "Model boş olamaz")
        @Size(max = 50, message = "Model en fazla 50 karakter olabilir")
        String model,

        @Schema(description = "Üretim yılı", example = "2022")
        @NotNull(message = "Üretim yılı zorunludur")
        @Min(value = 1980, message = "Üretim yılı 1980'den küçük olamaz")
        @Max(value = 2100, message = "Üretim yılı 2100'den büyük olamaz")
        Integer manufactureYear,

        @Schema(description = "Aracı kullanmak için gereken en düşük ehliyet sınıfı", example = "CE")
        @NotNull(message = "Gerekli ehliyet sınıfı zorunludur")
        LicenseClass requiredLicenseClass,

        @Schema(description = "Aracın güncel kilometresi. Güncellemede mevcut değerden düşük olamaz", example = "12500")
        @NotNull(message = "Güncel KM zorunludur")
        @PositiveOrZero(message = "KM negatif olamaz")
        Long currentKm,

        @Schema(description = "Son bakımın yapıldığı KM. Gönderilmezse güncel KM kabul edilir", example = "10000")
        @PositiveOrZero(message = "Son bakım KM değeri negatif olamaz")
        Long lastMaintenanceKm,

        @Schema(description = "Son bakım tarihi. Gönderilmezse bugün kabul edilir", example = "2025-01-15")
        @PastOrPresent(message = "Son bakım tarihi gelecekte olamaz")
        LocalDate lastMaintenanceDate,

        @Schema(description = "KM bazlı bakım aralığı. Gönderilmezse 10.000 kabul edilir", example = "10000")
        @Min(value = 1, message = "Bakım KM aralığı en az 1 olmalıdır")
        Integer maintenanceIntervalKm,

        @Schema(description = "Gün bazlı bakım aralığı. Gönderilmezse 180 kabul edilir", example = "180")
        @Min(value = 1, message = "Bakım gün aralığı en az 1 olmalıdır")
        Integer maintenanceIntervalDays) {
}
