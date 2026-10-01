package com.filo.fleet.repository;

import com.filo.fleet.entity.Driver;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

    List<Driver> findAllByOrderByLastNameAscFirstNameAsc();

    /** Eşzamanlı atamaları engellemek için kayıt kilitlenerek okunur. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Driver> findWithLockById(Long id);

    /** Aktif, ehliyeti geçerli ve aktif görevi olmayan sürücüler. */
    @Query("""
            select d from Driver d
            where d.active = true
              and d.licenseExpiryDate >= :today
              and not exists (
                    select a.id from VehicleAssignment a
                    where a.driver = d
                      and a.status = com.filo.fleet.entity.AssignmentStatus.ACTIVE)
            order by d.lastName, d.firstName
            """)
    List<Driver> findAvailableDrivers(@Param("today") LocalDate today);
}
