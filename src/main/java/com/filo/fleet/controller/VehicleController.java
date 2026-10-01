package com.filo.fleet.controller;

import com.filo.fleet.dto.request.KmUpdateRequest;
import com.filo.fleet.dto.request.VehicleRequest;
import com.filo.fleet.dto.response.ErrorResponse;
import com.filo.fleet.dto.response.VehicleResponse;
import com.filo.fleet.entity.VehicleStatus;
import com.filo.fleet.service.VehicleService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Araçlar", description = "Araç filosu, kilometre takibi ve bakım zamanı gelen araçlar")
@ApiResponse(responseCode = "500", description = "Beklenmeyen sunucu hatası",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @Operation(summary = "Yeni araç oluşturur",
            description = "Plaka benzersiz olmalıdır. Araç `IDLE` (boşta) durumunda oluşur. Bakım aralıkları "
                    + "gönderilmezse 10.000 km ve 180 gün varsayılır.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Araç oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Doğrulama hatası veya tutarsız KM değerleri",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Plaka zaten kayıtlı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody VehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vehicleService.create(request));
    }

    @GetMapping
    @Operation(summary = "Araçları listeler",
            description = "`status` verilirse yalnızca o durumdaki araçlar (IN_SERVICE / IN_MAINTENANCE / IDLE) döner.")
    @ApiResponse(responseCode = "200", description = "Araç listesi")
    public List<VehicleResponse> getAll(
            @Parameter(description = "Araç durumu filtresi (opsiyonel)", example = "IDLE")
            @RequestParam(name = "status", required = false) VehicleStatus status) {
        return vehicleService.getAll(status);
    }

    @GetMapping("/maintenance-due")
    @Operation(summary = "Bakım zamanı gelen araçları listeler",
            description = "Son bakımdan bu yana KM aralığı dolan veya bakım tarihi gelen (bakımda olmayan) araçlar.")
    @ApiResponse(responseCode = "200", description = "Bakımı gelen araçlar")
    public List<VehicleResponse> getMaintenanceDue() {
        return vehicleService.getMaintenanceDue();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Aracı kimliğe göre getirir")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Araç bulundu"),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public VehicleResponse getById(@Parameter(description = "Araç kimliği", example = "1")
                                   @PathVariable("id") Long id) {
        return vehicleService.getById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aracı günceller",
            description = "Gönderilen `currentKm`, mevcut KM değerinden düşük olamaz. Araç durumu bu uçtan değiştirilemez.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Araç güncellendi"),
            @ApiResponse(responseCode = "400", description = "Doğrulama hatası veya geçersiz KM",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Plaka başka araçta kayıtlı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public VehicleResponse update(@Parameter(description = "Araç kimliği", example = "1")
                                  @PathVariable("id") Long id,
                                  @Valid @RequestBody VehicleRequest request) {
        return vehicleService.update(id, request);
    }

    @PatchMapping("/{id}/km")
    @Operation(summary = "Araç kilometresini günceller",
            description = "Yeni KM değeri mevcut değerden düşük olamaz; aksi halde `INVALID_KILOMETER_UPDATE` döner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KM güncellendi"),
            @ApiResponse(responseCode = "400", description = "Yeni KM mevcut KM'den düşük veya geçersiz",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public VehicleResponse updateKm(@Parameter(description = "Araç kimliği", example = "1")
                                    @PathVariable("id") Long id,
                                    @Valid @RequestBody KmUpdateRequest request) {
        return vehicleService.updateKm(id, request.newKm());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Aracı siler",
            description = "Atama geçmişi bulunan araç silinemez. Aracın bakım kayıtları birlikte silinir (cascade).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Araç silindi"),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Aracın atama geçmişi var",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@Parameter(description = "Araç kimliği", example = "1")
                                       @PathVariable("id") Long id) {
        vehicleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
