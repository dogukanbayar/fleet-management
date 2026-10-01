package com.filo.fleet.util;

import com.filo.fleet.exception.InvalidKilometerUpdateException;

/**
 * Araç kilometresi ile ilgili "yeni değer, mevcut değerden düşük olamaz" kuralını tek bir yerde
 * uygular. Hem {@code VehicleService} (KM güncelleme / araç güncelleme) hem de
 * {@code AssignmentService} (görev bitirme) bu kontrolü kullanır; kural iki serviste ayrı ayrı
 * tekrarlanmaz.
 */
public final class KilometerValidator {

    private KilometerValidator() {
    }

    /**
     * @param context     hata mesajında gösterilecek bağlam (ör. araç plakası)
     * @param currentKm   aracın mevcut kilometre değeri
     * @param candidateKm önerilen yeni kilometre değeri
     * @throws InvalidKilometerUpdateException candidateKm, currentKm'den düşükse
     */
    public static void validateNotDecreasing(String context, long currentKm, long candidateKm) {
        if (candidateKm < currentKm) {
            throw new InvalidKilometerUpdateException(
                    context + ": yeni KM değeri (" + candidateKm + "), mevcut KM değerinden (" + currentKm
                            + ") düşük olamaz.");
        }
    }
}
