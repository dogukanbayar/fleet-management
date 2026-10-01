package com.filo.fleet.dto.request;

import com.filo.fleet.entity.LicenseClass;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Schema(description = "Sürücü oluşturma / güncelleme isteği")
public record DriverRequest(
        @Schema(description = "Sürücünün adı", example = "Ali")
        @NotBlank(message = "Ad boş olamaz")
        @Size(max = 50, message = "Ad en fazla 50 karakter olabilir")
        String firstName,

        @Schema(description = "Sürücünün soyadı", example = "Yılmaz")
        @NotBlank(message = "Soyad boş olamaz")
        @Size(max = 50, message = "Soyad en fazla 50 karakter olabilir")
        String lastName,

        @Schema(description = "Telefon numarası", example = "05321234567")
        @Size(max = 20, message = "Telefon en fazla 20 karakter olabilir")
        String phone,

        @Schema(description = "Benzersiz ehliyet numarası", example = "TR-C-123456")
        @NotBlank(message = "Ehliyet numarası boş olamaz")
        @Size(max = 30, message = "Ehliyet numarası en fazla 30 karakter olabilir")
        String licenseNumber,

        @Schema(description = "Ehliyet sınıfı (B < C < CE < D)", example = "CE")
        @NotNull(message = "Ehliyet sınıfı zorunludur")
        LicenseClass licenseClass,

        @Schema(description = "Ehliyetin son geçerlilik tarihi", example = "2030-12-31")
        @NotNull(message = "Ehliyet bitiş tarihi zorunludur")
        LocalDate licenseExpiryDate,

        @Schema(description = "Sürücü aktif mi? Gönderilmezse true kabul edilir", example = "true")
        Boolean active) {
}
