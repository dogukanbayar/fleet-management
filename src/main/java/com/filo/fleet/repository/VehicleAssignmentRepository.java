package com.filo.fleet.repository;

import com.filo.fleet.entity.AssignmentStatus;
import com.filo.fleet.entity.VehicleAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, Long> {

    boolean existsByDriverIdAndStatus(Long driverId, AssignmentStatus status);

    boolean existsByDriverId(Long driverId);

    boolean existsByVehicleId(Long vehicleId);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    Optional<VehicleAssignment> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<VehicleAssignment> findAllByOrderByStartDateDesc();

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<VehicleAssignment> findByStatusOrderByStartDateDesc(AssignmentStatus status);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<VehicleAssignment> findByDriverIdOrderByStartDateDesc(Long driverId);

    @EntityGraph(attributePaths = {"driver", "vehicle"})
    List<VehicleAssignment> findByVehicleIdOrderByStartDateDesc(Long vehicleId);
}
