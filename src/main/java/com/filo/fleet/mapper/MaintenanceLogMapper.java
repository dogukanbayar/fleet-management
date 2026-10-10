package com.filo.fleet.mapper;

import com.filo.fleet.dto.request.MaintenanceCompleteRequest;
import com.filo.fleet.dto.response.MaintenanceLogResponse;
import com.filo.fleet.entity.MaintenanceLog;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MaintenanceLogMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    @Mapping(target = "maintenanceDate", ignore = true)
    @Mapping(target = "kmAtMaintenance", ignore = true)
    MaintenanceLog toEntity(MaintenanceCompleteRequest request);

    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehiclePlate", source = "vehicle.plate")
    MaintenanceLogResponse toResponse(MaintenanceLog log);

    List<MaintenanceLogResponse> toResponseList(List<MaintenanceLog> logs);
}
