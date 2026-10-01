package com.filo.fleet.service;

import com.filo.fleet.dto.request.AssignmentCompleteRequest;
import com.filo.fleet.dto.request.AssignmentRequest;
import com.filo.fleet.dto.response.AssignmentResponse;
import com.filo.fleet.entity.AssignmentStatus;
import java.util.List;

public interface AssignmentService {

    AssignmentResponse assign(AssignmentRequest request);

    AssignmentResponse complete(Long assignmentId, AssignmentCompleteRequest request);

    AssignmentResponse getById(Long id);

    /** @param status null ise tüm atamalar döner */
    List<AssignmentResponse> getAll(AssignmentStatus status);

    List<AssignmentResponse> getByDriver(Long driverId);

    List<AssignmentResponse> getByVehicle(Long vehicleId);
}
