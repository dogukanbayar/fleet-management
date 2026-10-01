package com.filo.fleet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drivers")
@Getter
@Setter
@NoArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(length = 20)
    private String phone;

    @Column(name = "license_number", nullable = false, unique = true, length = 30)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "license_class", nullable = false, length = 10)
    private LicenseClass licenseClass;

    @Column(name = "license_expiry_date", nullable = false)
    private LocalDate licenseExpiryDate;

    @Column(nullable = false)
    private boolean active = true;

    /** Sürücünün atama geçmişi. Geçmiş kayıtlar korunmalı olduğu için cascade tanımlanmamıştır. */
    @OneToMany(mappedBy = "driver", fetch = FetchType.LAZY)
    private List<VehicleAssignment> assignments = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * JPA entity'leri için önerilen güvenli desen: eşitlik yalnızca kalıcı (id atanmış) kayıtlar
     * arasında ve id üzerinden kurulur; henüz kalıcı olmayan (id null) örnekler yalnızca referans
     * eşitliğiyle eşit sayılır. {@code hashCode} sabit tutularak nesne bir koleksiyona eklendikten
     * sonra id atanması durumunda "kaybolma" riski önlenir.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Driver other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
