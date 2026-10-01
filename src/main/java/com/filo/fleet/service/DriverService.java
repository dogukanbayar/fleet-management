package com.filo.fleet.service;

import com.filo.fleet.dto.request.DriverRequest;
import com.filo.fleet.dto.response.DriverResponse;
import com.filo.fleet.entity.LicenseClass;
import java.util.List;

public interface DriverService {

    DriverResponse create(DriverRequest request);

    DriverResponse getById(Long id);

    List<DriverResponse> getAll();

    /**
     * Aktif, ehliyeti geçerli ve aktif görevi olmayan sürücüler.
     *
     * @param requiredLicenseClass verilirse yalnızca bu sınıfı karşılayan sürücüler döner (opsiyonel)
     */
    List<DriverResponse> getAvailable(LicenseClass requiredLicenseClass);

    DriverResponse update(Long id, DriverRequest request);

    void delete(Long id);
}
