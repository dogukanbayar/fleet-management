package com.filo.fleet.service;

import com.filo.fleet.dto.request.VehicleRequest;
import com.filo.fleet.dto.response.VehicleResponse;
import com.filo.fleet.entity.VehicleStatus;
import java.util.List;

public interface VehicleService {

    VehicleResponse create(VehicleRequest request);

    VehicleResponse getById(Long id);

    /** @param status null ise tüm araçlar döner */
    List<VehicleResponse> getAll(VehicleStatus status);

    VehicleResponse update(Long id, VehicleRequest request);

    void delete(Long id);

    /** Araç kilometresini günceller; mevcut değerden düşük değer kabul edilmez. */
    VehicleResponse updateKm(Long id, Long newKm);

    /** KM veya gün bazlı bakım zamanı gelen araçlar. */
    List<VehicleResponse> getMaintenanceDue();
}
