package com.filo.fleet.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.filo.fleet.exception.InvalidKilometerUpdateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KilometerValidator birim testleri")
class KilometerValidatorTest {

    @Test
    @DisplayName("Yeni KM, mevcut KM'den düşükse InvalidKilometerUpdateException fırlatılır")
    void validateNotDecreasing_throws_whenCandidateLowerThanCurrent() {
        assertThatThrownBy(() -> KilometerValidator.validateNotDecreasing("34 ABC 123", 5000, 4999))
                .isInstanceOf(InvalidKilometerUpdateException.class)
                .hasMessageContaining("34 ABC 123");
    }

    @Test
    @DisplayName("Yeni KM mevcut KM'ye eşitse hata fırlatılmaz")
    void validateNotDecreasing_doesNotThrow_whenCandidateEqualsCurrent() {
        assertThatCode(() -> KilometerValidator.validateNotDecreasing("34 ABC 123", 5000, 5000))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Yeni KM mevcut KM'den büyükse hata fırlatılmaz")
    void validateNotDecreasing_doesNotThrow_whenCandidateGreaterThanCurrent() {
        assertThatCode(() -> KilometerValidator.validateNotDecreasing("34 ABC 123", 5000, 5001))
                .doesNotThrowAnyException();
    }
}
