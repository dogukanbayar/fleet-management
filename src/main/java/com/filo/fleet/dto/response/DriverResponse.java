package com.filo.fleet.dto.response;

import com.filo.fleet.entity.LicenseClass;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "Sürücü bilgileri")
public record DriverResponse(
        @Schema(description = "Sürücü kimliği", example = "1") Long id,
        @Schema(description = "Ad", example = "Ali") String firstName,
        @Schema(description = "Soyad", example = "Yılmaz") String lastName,
        @Schema(description = "Ad soyad", example = "Ali Yılmaz") String fullName,
        @Schema(description = "Telefon", example = "05321234567") String phone,
        @Schema(description = "Ehliyet numarası", example = "TR-C-123456") String licenseNumber,
        @Schema(description = "Ehliyet sınıfı", example = "CE") LicenseClass licenseClass,
        @Schema(description = "Ehliyet son geçerlilik tarihi", example = "2030-12-31") LocalDate licenseExpiryDate,
        @Schema(description = "Sürücü aktif mi?", example = "true") boolean active) {
}
