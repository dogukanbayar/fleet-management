package com.filo.fleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.filo.fleet.dto.request.DriverRequest;
import com.filo.fleet.dto.response.DriverResponse;
import com.filo.fleet.entity.Driver;
import com.filo.fleet.entity.LicenseClass;
import com.filo.fleet.exception.DuplicateResourceException;
import com.filo.fleet.exception.InvalidOperationException;
import com.filo.fleet.exception.ResourceNotFoundException;
import com.filo.fleet.mapper.DriverMapper;
import com.filo.fleet.repository.DriverRepository;
import com.filo.fleet.repository.VehicleAssignmentRepository;
import com.filo.fleet.service.impl.DriverServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriverServiceImpl birim testleri")
class DriverServiceImplTest {

    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleAssignmentRepository assignmentRepository;
    @Mock
    private DriverMapper driverMapper;

    @InjectMocks
    private DriverServiceImpl driverService;

    private DriverRequest request;
    private Driver driver;

    @BeforeEach
    void setUp() {
        request = new DriverRequest("Ali", "Yılmaz", "0532", "TR-123", LicenseClass.CE,
                LocalDate.now().plusYears(2), true);
        driver = new Driver();
        driver.setId(1L);
        driver.setFirstName("Ali");
        driver.setLastName("Yılmaz");
        driver.setLicenseNumber("TR-123");
        driver.setLicenseClass(LicenseClass.CE);
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        driver.setActive(true);
    }

    @Test
    @DisplayName("Benzersiz ehliyet numarasıyla sürücü oluşturulur")
    void create_savesDriver_whenLicenseNumberIsUnique() {
        when(driverRepository.existsByLicenseNumber("TR-123")).thenReturn(false);
        when(driverMapper.toEntity(request)).thenReturn(driver);
        when(driverRepository.save(driver)).thenReturn(driver);
        when(driverMapper.toResponse(driver)).thenReturn(sampleResponse());

        DriverResponse result = driverService.create(request);

        assertThat(result.licenseNumber()).isEqualTo("TR-123");
        verify(driverRepository).save(driver);
    }

    @Test
    @DisplayName("Ehliyet numarası zaten kayıtlıysa DuplicateResourceException fırlatılır")
    void create_throws_whenLicenseNumberDuplicated() {
        when(driverRepository.existsByLicenseNumber("TR-123")).thenReturn(true);

        assertThatThrownBy(() -> driverService.create(request))
                .isInstanceOf(DuplicateResourceException.class);
        verify(driverRepository, never()).save(any());
    }

    @Test
    @DisplayName("Var olmayan sürücü sorgulanırsa ResourceNotFoundException fırlatılır")
    void getById_throws_whenDriverMissing() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Atama geçmişi olan sürücü silinemez")
    void delete_throws_whenDriverHasAssignmentHistory() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.existsByDriverId(1L)).thenReturn(true);

        assertThatThrownBy(() -> driverService.delete(1L))
                .isInstanceOf(InvalidOperationException.class);
        verify(driverRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Atama geçmişi olmayan sürücü silinebilir")
    void delete_removesDriver_whenNoAssignmentHistory() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(assignmentRepository.existsByDriverId(1L)).thenReturn(false);

        driverService.delete(1L);

        verify(driverRepository).delete(driver);
    }

    @Test
    @DisplayName("getAvailable, gerekli ehliyet sınıfını karşılamayan sürücüleri eler")
    void getAvailable_filtersByRequiredLicenseClass() {
        Driver bClassDriver = new Driver();
        bClassDriver.setLicenseClass(LicenseClass.B);
        when(driverRepository.findAvailableDrivers(any(LocalDate.class)))
                .thenReturn(List.of(driver, bClassDriver));
        when(driverMapper.toResponseList(List.of(driver))).thenReturn(List.of(sampleResponse()));

        List<DriverResponse> result = driverService.getAvailable(LicenseClass.C);

        assertThat(result).hasSize(1);
        verify(driverMapper).toResponseList(List.of(driver));
    }

    @Test
    @DisplayName("active alanı gönderilmezse (null) sürücü varsayılan olarak aktif oluşturulur")
    void create_defaultsActiveToTrue_whenActiveFieldIsNull() {
        DriverRequest requestWithoutActive = new DriverRequest("Ayşe", "Demir", "0533", "TR-456",
                LicenseClass.C, LocalDate.now().plusYears(1), null);
        Driver newDriver = new Driver();
        newDriver.setActive(false); // mapper "active" alanını atladığı için varsayılan (primitif) değer
        when(driverRepository.existsByLicenseNumber("TR-456")).thenReturn(false);
        when(driverMapper.toEntity(requestWithoutActive)).thenReturn(newDriver);
        when(driverRepository.save(newDriver)).thenReturn(newDriver);
        when(driverMapper.toResponse(newDriver)).thenReturn(sampleResponse());

        driverService.create(requestWithoutActive);

        assertThat(newDriver.isActive()).isTrue();
    }

    @Test
    @DisplayName("Güncellemede farklı sürücüde kayıtlı ehliyet numarası reddedilir")
    void update_throws_whenLicenseNumberBelongsToAnotherDriver() {
        when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
        when(driverRepository.existsByLicenseNumberAndIdNot(eq("TR-123"), anyLong())).thenReturn(true);

        assertThatThrownBy(() -> driverService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    private DriverResponse sampleResponse() {
        return new DriverResponse(1L, "Ali", "Yılmaz", "Ali Yılmaz", "0532", "TR-123",
                LicenseClass.CE, LocalDate.now().plusYears(2), true);
    }
}
