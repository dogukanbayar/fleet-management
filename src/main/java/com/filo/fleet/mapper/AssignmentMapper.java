package com.filo.fleet.mapper;

import com.filo.fleet.dto.response.AssignmentResponse;
import com.filo.fleet.entity.VehicleAssignment;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssignmentMapper {

    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverFullName", source = "driver.fullName")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehiclePlate", source = "vehicle.plate")
    @Mapping(target = "vehicleMaintenanceDue", expression = "java(assignment.getVehicle().isMaintenanceDue())")
    AssignmentResponse toResponse(VehicleAssignment assignment);

    List<AssignmentResponse> toResponseList(List<VehicleAssignment> assignments);
}
