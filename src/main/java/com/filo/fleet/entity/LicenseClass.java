package com.filo.fleet.entity;

/**
 * Ehliyet sınıfları. Sınıflar hiyerarşiktir: üst sınıf, alt sınıfların gerektirdiği araçları da kullanabilir
 * (B &lt; C &lt; CE &lt; D).
 */
public enum LicenseClass {
    B(1),
    C(2),
    CE(3),
    D(4);

    private final int rank;

    LicenseClass(int rank) {
        this.rank = rank;
    }

    /**
     * @param required aracın gerektirdiği ehliyet sınıfı
     * @return bu ehliyet, gerekli sınıfı karşılıyorsa true
     */
    public boolean covers(LicenseClass required) {
        return required != null && this.rank >= required.rank;
    }
}
