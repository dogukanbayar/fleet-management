package com.filo.fleet.controller;

import com.filo.fleet.dto.request.MaintenanceCompleteRequest;
import com.filo.fleet.dto.response.ErrorResponse;
import com.filo.fleet.dto.response.MaintenanceLogResponse;
import com.filo.fleet.dto.response.VehicleResponse;
import com.filo.fleet.service.MaintenanceService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicles/{vehicleId}/maintenance")
@Tag(name = "Bakım", description = "Aracı bakıma alma, bakımı tamamlama ve bakım geçmişi")
@ApiResponse(responseCode = "500", description = "Beklenmeyen sunucu hatası",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/start")
    @Operation(summary = "Aracı bakıma alır",
            description = "Araç `IN_MAINTENANCE` durumuna geçer. Görevdeki veya zaten bakımda olan araç bakıma alınamaz.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Araç bakıma alındı"),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Araç görevde veya zaten bakımda",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public VehicleResponse start(@Parameter(description = "Araç kimliği", example = "1")
                                 @PathVariable("vehicleId") Long vehicleId) {
        return maintenanceService.startMaintenance(vehicleId);
    }

    @PostMapping("/complete")
    @Operation(summary = "Bakımı tamamlar",
            description = "Bakım kaydı oluşturulur, son bakım KM/tarihi güncellenir ve araç `IDLE` durumuna geçer.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bakım tamamlandı, kayıt oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Doğrulama hatası",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Araç bakımda değil",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MaintenanceLogResponse> complete(
            @Parameter(description = "Araç kimliği", example = "1") @PathVariable("vehicleId") Long vehicleId,
            @Valid @RequestBody MaintenanceCompleteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maintenanceService.completeMaintenance(vehicleId, request));
    }

    @GetMapping("/logs")
    @Operation(summary = "Aracın bakım geçmişini listeler", description = "En yeni bakım kaydı önce gelir.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bakım kayıtları"),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<MaintenanceLogResponse> getLogs(@Parameter(description = "Araç kimliği", example = "1")
                                                @PathVariable("vehicleId") Long vehicleId) {
        return maintenanceService.getLogs(vehicleId);
    }
}
