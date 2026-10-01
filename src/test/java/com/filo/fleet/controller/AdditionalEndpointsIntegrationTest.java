package com.filo.fleet.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filo.fleet.dto.request.AssignmentCompleteRequest;
import com.filo.fleet.dto.request.AssignmentRequest;
import com.filo.fleet.dto.request.DriverRequest;
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
 * {@link FleetFlowIntegrationTest} yalnızca ana yaşam döngüsünü ve üç özel iş kuralı hatasını
 * kapsar. Bu sınıf, şartnamede tanımlı ama ana akışta üretime katılmayan diğer uçları
 * (listeleme/filtreleme, güncelleme, silme, bakım geçmişi, bakımı gelen araçlar) doğrular.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Ek uç nokta entegrasyon testleri (CRUD, listeleme, filtreleme)")
class AdditionalEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Sürücü listelenebilir, güncellenebilir ve atama geçmişi yoksa silinebilir")
    void driver_list_update_delete_lifecycle() throws Exception {
        Long driverId = createDriver("LIC-ADD-1");

        mockMvc.perform(get("/api/v1/drivers"))
                .andExpect(status().isOk());

        DriverRequest updateRequest = new DriverRequest("Ali", "Güncellendi", "0555", "LIC-ADD-1",
                LicenseClass.CE, LocalDate.now().plusYears(3), true);
        mockMvc.perform(put("/api/v1/drivers/{id}", driverId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName", is("Güncellendi")));

        mockMvc.perform(delete("/api/v1/drivers/{id}", driverId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/drivers/{id}", driverId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Atama geçmişi olan sürücü silinemez (409 INVALID_OPERATION)")
    void driver_delete_returns409_whenAssignmentHistoryExists() throws Exception {
        Long driverId = createDriver("LIC-ADD-2");
        Long vehicleId = createVehicle("34 ADD 002", 100L);
        mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignmentRequest(driverId, vehicleId))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/drivers/{id}", driverId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_OPERATION")));
    }

    @Test
    @DisplayName("Araç durum filtresiyle listelenebilir, güncellenebilir ve atama geçmişi yoksa silinebilir")
    void vehicle_filter_update_delete_lifecycle() throws Exception {
        Long vehicleId = createVehicle("34 ADD 003", 500L);

        mockMvc.perform(get("/api/v1/vehicles").param("status", "IDLE"))
                .andExpect(status().isOk());

        VehicleRequest updateRequest = new VehicleRequest("34 ADD 003", "Volvo", "FH16", 2023,
                LicenseClass.CE, 600L, null, null, null, null);
        mockMvc.perform(put("/api/v1/vehicles/{id}", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand", is("Volvo")));

        mockMvc.perform(delete("/api/v1/vehicles/{id}", vehicleId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Bakımı gelen araçlar listesi ve bakım geçmişi doğru döner")
    void vehicle_maintenanceDueList_andMaintenanceLogs() throws Exception {
        Long vehicleId = createVehicle("34 ADD 004", 100L);

        mockMvc.perform(patch("/api/v1/vehicles/{id}/km", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newKm\": 10200}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vehicles/maintenance-due"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.plate == '34 ADD 004')]").exists());

        mockMvc.perform(post("/api/v1/vehicles/{id}/maintenance/start", vehicleId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/vehicles/{id}/maintenance/complete", vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new MaintenanceCompleteRequest(MaintenanceType.PERIODIC,
                                BigDecimal.valueOf(999.99), "Periyodik bakım", "Servis X"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/vehicles/{id}/maintenance/logs", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].maintenanceType", is("PERIODIC")));
    }

    @Test
    @DisplayName("Atamalar durum filtresiyle ve araca göre listelenebilir")
    void assignment_filterByStatus_andByVehicle() throws Exception {
        Long driverId = createDriver("LIC-ADD-5");
        Long vehicleId = createVehicle("34 ADD 005", 100L);
        String createResponse = mockMvc.perform(post("/api/v1/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignmentRequest(driverId, vehicleId))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long assignmentId = objectMapper.readTree(createResponse).get("id").asLong();

        mockMvc.perform(get("/api/v1/assignments").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + assignmentId + ")]").exists());

        mockMvc.perform(patch("/api/v1/assignments/{id}/complete", assignmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new AssignmentCompleteRequest(300L))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/assignments/vehicles/{vehicleId}", vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("COMPLETED")));
    }

    @Test
    @DisplayName("Var olmayan araç için bakım/atama geçmişi sorgusu 404 döner")
    void getByVehicle_returns404_whenVehicleMissing() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/vehicles/{vehicleId}", 999_999))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/vehicles/{id}/maintenance/logs", 999_999))
                .andExpect(status().isNotFound());
    }

    private Long createDriver(String licenseNumber) throws Exception {
        DriverRequest request = new DriverRequest("Ali", "Yılmaz", "0500", licenseNumber, LicenseClass.CE,
                LocalDate.now().plusYears(2), true);
        String response = mockMvc.perform(post("/api/v1/drivers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private Long createVehicle(String plate, long currentKm) throws Exception {
        VehicleRequest request = new VehicleRequest(plate, "Mercedes", "Actros", 2022, LicenseClass.CE,
                currentKm, null, null, null, null);
        String response = mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
