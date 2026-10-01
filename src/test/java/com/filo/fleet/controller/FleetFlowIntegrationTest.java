package com.filo.fleet.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filo.fleet.dto.request.AssignmentCompleteRequest;
import com.filo.fleet.dto.request.AssignmentRequest;
import com.filo.fleet.dto.request.DriverRequest;
import com.filo.fleet.dto.request.KmUpdateRequest;
import com.filo.fleet.dto.request.MaintenanceCompleteRequest;
import com.filo.fleet.dto.request.VehicleRequest;
import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.entity.MaintenanceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Denetim -&gt; Servis -&gt; Repository katmanlarını gerçek Spring bağlamı ve H2 veritabanı ile uçtan uca
 * doğrulayan entegrasyon testi. Şartnamedeki üç özel iş kuralını da kapsar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Filo yönetimi uçtan uca akış testleri")
class FleetFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Sürücü + araç oluşturma, atama, görev bitirme ve bakım akışı baştan sona başarılı çalışır")
    void fullLifecycle_createAssignCompleteAndMaintain() throws Exception {
        Long driverId = createDriver("LIC-FLOW-1", LicenseClass.CE);
        Long vehicleId = createVehicle("34 FLW 001", LicenseClass.CE, 1000L);

        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignmentRequest(driverId, vehicleId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_SERVICE")));

        Long assignmentId = getActiveAssignmentId(driverId);

        mockMvc.perform(patch("/api/v1/assignments/{id}/complete", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignmentCompleteRequest(1500L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.endKm", is(1500)));

        mockMvc.perform(post("/api/v1/vehicles/{id}/maintenance/start", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_MAINTENANCE")));

        mockMvc.perform(post("/api/v1/vehicles/{id}/maintenance/complete", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new MaintenanceCompleteRequest(MaintenanceType.PERIODIC,
                                BigDecimal.valueOf(1250.50), "Genel bakım", "Yetkili Servis"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kmAtMaintenance", is(1500)));

        mockMvc.perform(get("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IDLE")));
    }

    @Test
    @DisplayName("DriverNotAvailableException: ehliyet sınıfı yetersiz sürücüye 422 döner")
    void assign_returns422_whenLicenseClassInsufficient() throws Exception {
        Long driverId = createDriver("LIC-FLOW-2", LicenseClass.B);
        Long vehicleId = createVehicle("34 FLW 002", LicenseClass.CE, 1000L);

        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignmentRequest(driverId, vehicleId))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("DRIVER_NOT_AVAILABLE")));
    }

    @Test
    @DisplayName("MaintenanceRequiredException: 10.000 km'yi aşan araç göreve atanamaz, 422 döner")
    void assign_returns422_whenVehicleMaintenanceOverdue() throws Exception {
        Long driverId = createDriver("LIC-FLOW-3", LicenseClass.CE);
        Long vehicleId = createVehicle("34 FLW 003", LicenseClass.CE, 100L);

        mockMvc.perform(patch("/api/v1/vehicles/{id}/km", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new KmUpdateRequest(10500L))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignmentRequest(driverId, vehicleId))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_REQUIRED")));
    }

    @Test
    @DisplayName("InvalidKilometerUpdateException: geriye doğru KM güncellemesi 400 döner")
    void updateKm_returns400_whenNewKmLowerThanCurrent() throws Exception {
        Long vehicleId = createVehicle("34 FLW 004", LicenseClass.CE, 5000L);

        mockMvc.perform(patch("/api/v1/vehicles/{id}/km", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new KmUpdateRequest(4000L))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INVALID_KILOMETER_UPDATE")));
    }

    @Test
    @DisplayName("Doğrulama hatası standart ErrorResponse formatında ve alan detaylarıyla döner")
    void create_returns400_withFieldValidationErrors_whenRequestInvalid() throws Exception {
        DriverRequest invalid = new DriverRequest("", "", null, "", null, null, null);

        mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.validationErrors").isArray());
    }

    @Test
    @DisplayName("Var olmayan sürücü sorgulanırsa 404 ve standart hata formatı döner")
    void getById_returns404_whenDriverNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/drivers/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    private Long createDriver(String licenseNumber, LicenseClass licenseClass) throws Exception {
        DriverRequest request = new DriverRequest("Test", licenseNumber, "0500", licenseNumber, licenseClass,
                LocalDate.now().plusYears(2), true);
        String response = mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createVehicle(String plate, LicenseClass requiredClass, long currentKm) throws Exception {
        VehicleRequest request = new VehicleRequest(plate, "Mercedes", "Actros", 2022, requiredClass,
                currentKm, null, null, null, null);
        String response = mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long getActiveAssignmentId(Long driverId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/assignments/drivers/{driverId}", driverId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get(0).get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
