package com.filo.fleet.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LicenseClass hiyerarşi kuralları")
class LicenseClassTest {

    @Test
    @DisplayName("Üst sınıf ehliyet, alt sınıfın gerektirdiği aracı karşılar")
    void covers_true_whenHigherClassCoversLowerRequirement() {
        assertThat(LicenseClass.CE.covers(LicenseClass.B)).isTrue();
        assertThat(LicenseClass.D.covers(LicenseClass.CE)).isTrue();
    }

    @Test
    @DisplayName("Alt sınıf ehliyet, üst sınıfın gerektirdiği aracı karşılamaz")
    void covers_false_whenLowerClassCannotCoverHigherRequirement() {
        assertThat(LicenseClass.B.covers(LicenseClass.CE)).isFalse();
    }

    @Test
    @DisplayName("Aynı sınıf kendini karşılar")
    void covers_true_whenSameClass() {
        assertThat(LicenseClass.C.covers(LicenseClass.C)).isTrue();
    }
}
