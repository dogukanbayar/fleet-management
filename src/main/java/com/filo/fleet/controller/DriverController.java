package com.filo.fleet.controller;

import com.filo.fleet.dto.request.DriverRequest;
import com.filo.fleet.dto.response.DriverResponse;
import com.filo.fleet.dto.response.ErrorResponse;
import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Sürücüler", description = "Sürücü kayıtlarının yönetimi ve atamaya uygun sürücülerin listelenmesi")
@ApiResponse(responseCode = "500", description = "Beklenmeyen sunucu hatası",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @Operation(summary = "Yeni sürücü oluşturur",
            description = "Ehliyet numarası benzersiz olmalıdır. `active` gönderilmezse sürücü aktif kabul edilir.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Sürücü oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Doğrulama hatası",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ehliyet numarası zaten kayıtlı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<DriverResponse> create(@Valid @RequestBody DriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverService.create(request));
    }

    @GetMapping
    @Operation(summary = "Tüm sürücüleri listeler", description = "Soyad ve ada göre sıralı döner.")
    @ApiResponse(responseCode = "200", description = "Sürücü listesi")
    public List<DriverResponse> getAll() {
        return driverService.getAll();
    }

    @GetMapping("/available")
    @Operation(summary = "Atamaya uygun sürücüleri listeler",
            description = "Aktif, ehliyeti geçerli ve halihazırda aktif görevi olmayan sürücüleri döner. "
                    + "`requiredLicenseClass` verilirse yalnızca o sınıfı karşılayan sürücüler listelenir.")
    @ApiResponse(responseCode = "200", description = "Uygun sürücü listesi")
    public List<DriverResponse> getAvailable(
            @Parameter(description = "Aracın gerektirdiği ehliyet sınıfı (opsiyonel)", example = "CE")
            @RequestParam(name = "requiredLicenseClass", required = false) LicenseClass requiredLicenseClass) {
        return driverService.getAvailable(requiredLicenseClass);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Sürücüyü kimliğe göre getirir")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sürücü bulundu"),
            @ApiResponse(responseCode = "404", description = "Sürücü bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public DriverResponse getById(@Parameter(description = "Sürücü kimliği", example = "1")
                                  @PathVariable("id") Long id) {
        return driverService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sürücüyü günceller")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sürücü güncellendi"),
            @ApiResponse(responseCode = "400", description = "Doğrulama hatası",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Sürücü bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Ehliyet numarası başka sürücüde kayıtlı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public DriverResponse update(@Parameter(description = "Sürücü kimliği", example = "1")
                                 @PathVariable("id") Long id,
                                 @Valid @RequestBody DriverRequest request) {
        return driverService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Sürücüyü siler",
            description = "Atama geçmişi bulunan sürücü silinemez; bunun yerine `active=false` yapılmalıdır.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sürücü silindi"),
            @ApiResponse(responseCode = "404", description = "Sürücü bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Sürücünün atama geçmişi var",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Sürücü kimliği", example = "1")
                                       @PathVariable("id") Long id) {
        driverService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
