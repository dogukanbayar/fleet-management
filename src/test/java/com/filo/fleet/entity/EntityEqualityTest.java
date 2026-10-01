package com.filo.fleet.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JPA entity'lerinde id bazlı equals/hashCode davranışı")
class EntityEqualityTest {

    @Test
    @DisplayName("Aynı id'ye sahip iki Driver örneği eşittir")
    void driver_equal_whenSameId() {
        Driver first = new Driver();
        first.setId(1L);
        Driver second = new Driver();
        second.setId(1L);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("Farklı id'ye sahip iki Driver örneği eşit değildir")
    void driver_notEqual_whenDifferentId() {
        Driver first = new Driver();
        first.setId(1L);
        Driver second = new Driver();
        second.setId(2L);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Henüz kalıcı olmayan (id=null) iki Driver örneği yalnızca referans eşitse eşittir")
    void driver_notEqual_whenBothTransientAndDifferentReferences() {
        Driver first = new Driver();
        Driver second = new Driver();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).isEqualTo(first);
    }

    @Test
    @DisplayName("Aynı id'ye sahip iki Vehicle örneği eşittir")
    void vehicle_equal_whenSameId() {
        Vehicle first = new Vehicle();
        first.setId(10L);
        Vehicle second = new Vehicle();
        second.setId(10L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Aynı id'ye sahip iki VehicleAssignment örneği eşittir")
    void assignment_equal_whenSameId() {
        VehicleAssignment first = new VehicleAssignment();
        first.setId(5L);
        VehicleAssignment second = new VehicleAssignment();
        second.setId(5L);

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Aynı id'ye sahip iki MaintenanceLog örneği eşittir")
    void maintenanceLog_equal_whenSameId() {
        MaintenanceLog first = new MaintenanceLog();
        first.setId(7L);
        first.setMaintenanceDate(LocalDate.now());
        MaintenanceLog second = new MaintenanceLog();
        second.setId(7L);

        assertThat(first).isEqualTo(second);
    }
}
