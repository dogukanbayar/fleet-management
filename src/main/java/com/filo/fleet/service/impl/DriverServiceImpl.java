package com.filo.fleet.service.impl;


import lombok.extern.slf4j.Slf4j;
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
import com.filo.fleet.service.DriverService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    private static final String DRIVER = "Sürücü";

    private final DriverRepository driverRepository;
    private final VehicleAssignmentRepository assignmentRepository;
    private final DriverMapper driverMapper;

    public DriverServiceImpl(DriverRepository driverRepository,
                             VehicleAssignmentRepository assignmentRepository,
                             DriverMapper driverMapper) {
        this.driverRepository = driverRepository;
        this.assignmentRepository = assignmentRepository;
        this.driverMapper = driverMapper;
    }

    @Override
    @Transactional
    public DriverResponse create(DriverRequest request) {
        log.info("DriverService operation executed");
        String licenseNumber = request.licenseNumber().trim();
        if (driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw duplicateLicense(licenseNumber);
        }
        Driver driver = driverMapper.toEntity(request);
        driver.setLicenseNumber(licenseNumber);
        driver.setActive(request.active() == null || request.active());
        return driverMapper.toResponse(driverRepository.save(driver));
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getById(Long id) {
        log.info("DriverService operation executed");
        return driverMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAll() {
        log.info("DriverService operation executed");
        return driverMapper.toResponseList(driverRepository.findAllByOrderByLastNameAscFirstNameAsc());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverResponse> getAvailable(LicenseClass requiredLicenseClass) {
        log.info("DriverService operation executed");
        List<Driver> drivers = driverRepository.findAvailableDrivers(LocalDate.now());
        if (requiredLicenseClass == null) {
            return driverMapper.toResponseList(drivers);
        }
        return driverMapper.toResponseList(drivers.stream()
                .filter(d -> d.getLicenseClass().covers(requiredLicenseClass))
                .toList());
    }

    @Override
    @Transactional
    public DriverResponse update(Long id, DriverRequest request) {
        log.info("DriverService operation executed");
        Driver driver = findOrThrow(id);
        String licenseNumber = request.licenseNumber().trim();
        if (driverRepository.existsByLicenseNumberAndIdNot(licenseNumber, id)) {
            throw duplicateLicense(licenseNumber);
        }
        driverMapper.updateEntity(request, driver);
        driver.setLicenseNumber(licenseNumber);
        if (request.active() != null) {
            driver.setActive(request.active());
        }
        return driverMapper.toResponse(driver);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("DriverService operation executed");
        Driver driver = findOrThrow(id);
        if (assignmentRepository.existsByDriverId(id)) {
            throw new InvalidOperationException(
                    "Atama geçmişi bulunan sürücü silinemez. Bunun yerine sürücüyü pasif duruma alın.");
        }
        driverRepository.delete(driver);
    }

    private Driver findOrThrow(Long id) {
        return driverRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(DRIVER, id));
    }

    private DuplicateResourceException duplicateLicense(String licenseNumber) {
        return new DuplicateResourceException(
                "Bu ehliyet numarası ile kayıtlı bir sürücü zaten var: " + licenseNumber);
    }
}
