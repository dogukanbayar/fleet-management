package com.filo.fleet.repository;

import com.filo.fleet.entity.Vehicle;
import com.filo.fleet.entity.VehicleStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlate(String plate);

    boolean existsByPlateAndIdNot(String plate, Long id);

    List<Vehicle> findAllByOrderByPlateAsc();

    List<Vehicle> findByStatusOrderByPlateAsc(VehicleStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Vehicle> findWithLockById(Long id);

    /** KM veya gün bazlı bakım süresi dolan (halihazırda bakımda olmayan) araçlar. */
    @Query("""
            select v from Vehicle v
            where v.status <> com.filo.fleet.entity.VehicleStatus.IN_MAINTENANCE
              and ((v.currentKm - v.lastMaintenanceKm) >= v.maintenanceIntervalKm
                   or v.nextMaintenanceDate <= :today)
            order by v.plate
            """)
    List<Vehicle> findMaintenanceDue(@Param("today") LocalDate today);
}
