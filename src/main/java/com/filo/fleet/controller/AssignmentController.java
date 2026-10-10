package com.filo.fleet.controller;


import lombok.extern.slf4j.Slf4j;
import com.filo.fleet.dto.request.AssignmentCompleteRequest;
import com.filo.fleet.dto.request.AssignmentRequest;
import com.filo.fleet.dto.response.AssignmentResponse;
import com.filo.fleet.dto.response.ErrorResponse;
import com.filo.fleet.entity.AssignmentStatus;
import com.filo.fleet.service.AssignmentService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/assignments")
@Tag(name = "Atamalar", description = "Sürücü - araç atama (görev) kayıtları")
@ApiResponse(responseCode = "500", description = "Beklenmeyen sunucu hatası",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping
    @Operation(summary = "Sürücüye araç atar",
            description = """
                    Atama kuralları:
                    - Sürücü aktif olmalı ve ehliyeti geçerli olmalıdır.
                    - Sürücünün ehliyet sınıfı aracın gerektirdiği sınıfı karşılamalıdır (B < C < CE < D).
                    - Sürücünün başka bir aktif görevi olmamalıdır.
                    - Araç boşta (`IDLE`) olmalıdır.
                    - Son bakımdan bu yana 10.000 km (veya gün aralığı) dolmuş araç atanamaz.

                    Başarılı atamada araç `IN_SERVICE` durumuna geçer.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Atama oluşturuldu"),
            @ApiResponse(responseCode = "404", description = "Sürücü veya araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Araç zaten görevde veya bakımda",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422",
                    description = "DRIVER_NOT_AVAILABLE: ehliyet yetersiz/süresi dolmuş veya sürücü zaten görevde. "
                            + "MAINTENANCE_REQUIRED: araç bakıma alınmalı.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssignmentResponse> assign(@Valid @RequestBody AssignmentRequest request) {
        log.info("AssignmentController endpoint invoked");
        return ResponseEntity.status(HttpStatus.CREATED).body(assignmentService.assign(request));
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Görevi sonlandırır",
            description = "Bitiş KM'si aracın mevcut KM değerinden düşük olamaz. Araç KM'si güncellenir ve araç `IDLE` olur. "
                    + "Yanıttaki `vehicleMaintenanceDue` true ise araç bakıma alınmalıdır.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Görev sonlandırıldı"),
            @ApiResponse(responseCode = "400", description = "Bitiş KM geçersiz",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Atama bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Atama zaten sonlandırılmış",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AssignmentResponse complete(@Parameter(description = "Atama kimliği", example = "1")
                                       @PathVariable("id") Long id,
                                       @Valid @RequestBody AssignmentCompleteRequest request) {
        return assignmentService.complete(id, request);
    }

    @GetMapping
    @Operation(summary = "Atamaları listeler",
            description = "`status` verilirse yalnızca o durumdaki atamalar (ACTIVE / COMPLETED) döner. En yeni önce.")
    @ApiResponse(responseCode = "200", description = "Atama listesi")
    public List<AssignmentResponse> getAll(
            @Parameter(description = "Atama durumu filtresi (opsiyonel)", example = "ACTIVE")
            @RequestParam(name = "status", required = false) AssignmentStatus status) {
        return assignmentService.getAll(status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Atamayı kimliğe göre getirir")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Atama bulundu"),
            @ApiResponse(responseCode = "404", description = "Atama bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AssignmentResponse getById(@Parameter(description = "Atama kimliği", example = "1")
                                      @PathVariable("id") Long id) {
        return assignmentService.getById(id);
    }

    @GetMapping("/drivers/{driverId}")
    @Operation(summary = "Sürücünün atama geçmişini getirir")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sürücünün atamaları"),
            @ApiResponse(responseCode = "404", description = "Sürücü bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<AssignmentResponse> getByDriver(@Parameter(description = "Sürücü kimliği", example = "1")
                                                @PathVariable("driverId") Long driverId) {
        return assignmentService.getByDriver(driverId);
    }

    @GetMapping("/vehicles/{vehicleId}")
    @Operation(summary = "Aracın atama geçmişini getirir")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aracın atamaları"),
            @ApiResponse(responseCode = "404", description = "Araç bulunamadı",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<AssignmentResponse> getByVehicle(@Parameter(description = "Araç kimliği", example = "1")
                                                 @PathVariable("vehicleId") Long vehicleId) {
        return assignmentService.getByVehicle(vehicleId);
    }
}
