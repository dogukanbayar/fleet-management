package com.filo.fleet.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Tüm hatalar için standart yanıt formatı")
public record ErrorResponse(
        @Schema(description = "Hatanın oluştuğu zaman") LocalDateTime timestamp,
        @Schema(description = "HTTP durum kodu", example = "422") int status,
        @Schema(description = "HTTP durum açıklaması", example = "Unprocessable Entity") String error,
        @Schema(description = "Makine tarafından okunabilir hata kodu", example = "MAINTENANCE_REQUIRED") String code,
        @Schema(description = "Kullanıcıya gösterilebilir hata mesajı") String message,
        @Schema(description = "İsteğin yapıldığı yol", example = "/api/v1/assignments") String path,
        @Schema(description = "Alan bazlı doğrulama hataları; yalnızca doğrulama hatalarında dolu gelir")
        List<FieldValidationError> validationErrors) {

    @Schema(description = "Tek bir alanın doğrulama hatası")
    public record FieldValidationError(
            @Schema(description = "Hatalı alan adı", example = "licenseNumber") String field,
            @Schema(description = "Hata mesajı", example = "Ehliyet numarası boş olamaz") String message) {
    }
}
