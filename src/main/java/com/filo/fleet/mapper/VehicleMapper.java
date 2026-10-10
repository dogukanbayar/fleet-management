package com.filo.fleet.mapper;

import com.filo.fleet.dto.request.VehicleRequest;
import com.filo.fleet.dto.response.VehicleResponse;
import com.filo.fleet.entity.Vehicle;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * KM ve bakım alanları iş kuralları gerektirdiği için (doğrulama, varsayılan değerler) mapper tarafından
 * doldurulmaz; servis katmanı yönetir.
 */
@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentKm", ignore = true)
    @Mapping(target = "lastMaintenanceKm", ignore = true)
    @Mapping(target = "lastMaintenanceDate", ignore = true)
    @Mapping(target = "nextMaintenanceDate", ignore = true)
    @Mapping(target = "maintenanceIntervalKm", ignore = true)
    @Mapping(target = "maintenanceIntervalDays", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "maintenanceLogs", ignore = true)
    Vehicle toEntity(VehicleRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentKm", ignore = true)
    @Mapping(target = "lastMaintenanceKm", ignore = true)
    @Mapping(target = "lastMaintenanceDate", ignore = true)
    @Mapping(target = "nextMaintenanceDate", ignore = true)
    @Mapping(target = "maintenanceIntervalKm", ignore = true)
    @Mapping(target = "maintenanceIntervalDays", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "maintenanceLogs", ignore = true)
    void updateEntity(VehicleRequest request, @MappingTarget Vehicle vehicle);

    VehicleResponse toResponse(Vehicle vehicle);

    List<VehicleResponse> toResponseList(List<Vehicle> vehicles);
}
