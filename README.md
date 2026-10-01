# Filo Yönetim Sistemi (Fleet Management System)

Bir lojistik şirketinin araç filosunu, sürücü atamalarını ve araçların periyodik (KM veya gün bazlı)
bakımlarını takip eden REST API. Araçlar **görevde (IN_SERVICE)**, **bakımda (IN_MAINTENANCE)** veya
**boşta (IDLE)** durumlarında olabilir.

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Dil / Framework | Java 17, Spring Boot 3.3.4 |
| Veritabanı | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate (sorgular yalnızca JPQL / Derived Query Method; **native SQL yok**) |
| Mapping | MapStruct |
| Dokümantasyon | springdoc-openapi (Swagger UI) |
| Test | JUnit 5, Mockito, AssertJ, H2 (integration testler için) |
| Kapsama | JaCoCo (minimum %70, `mvn verify` sırasında zorunlu) |
| Konteyner | Docker, docker-compose |

## Mimari

Katmanlı mimari uygulanmıştır: **Controller → Service → Repository**. Entity'ler API dışına asla
doğrudan açılmaz; her uçta DTO (`request` / `response`) kullanılır ve dönüşümler MapStruct mapper'ları
ile yapılır.

```
com.filo.fleet
├── config            OpenAPI (Swagger) yapılandırması
├── controller         REST uçları
├── service /impl       İş kuralları
├── repository          Spring Data JPA arayüzleri (JPQL / derived query)
├── entity               JPA entity'leri ve enum'lar
├── dto/request, dto/response
├── mapper               MapStruct arayüzleri
└── exception            Custom exception'lar + GlobalExceptionHandler
```

## Veri Modeli (ER)

- **Driver** (Sürücü) `1 — N` → **VehicleAssignment** (Görev/Atama)
- **Vehicle** (Araç) `1 — N` → **VehicleAssignment**
- **Vehicle** `1 — N` → **MaintenanceLog** (Bakım Kaydı, `cascade = ALL, orphanRemoval = true`)

Görsel şema kök dizindeki **`db-model.drawio`** dosyasındadır (draw.io / diagrams.net ile açılabilir).

Tüm ilişkiler `FetchType.LAZY`; N+1 sorgu sorununu önlemek için atama sorgularında `@EntityGraph`,
sürücü müsaitlik sorgusunda JPQL `NOT EXISTS` alt sorgusu kullanılmıştır.

## İş Kuralları ve Custom Exception'lar

| Exception | HTTP | Kural |
|---|---|---|
| `DriverNotAvailableException` | 422 | Ehliyet sınıfı yetersiz (B < C < CE < D hiyerarşisi), ehliyeti süresi dolmuş, pasif veya halihazırda aktif görevde olan sürücüye araç atanamaz. |
| `MaintenanceRequiredException` | 422 | Son bakımından bu yana **KM aralığı** (varsayılan 10.000 km) veya **gün aralığı** (varsayılan 180 gün) dolmuş araç yeni göreve atanamaz. |
| `InvalidKilometerUpdateException` | 400 | Araca girilen yeni KM değeri mevcut KM değerinden düşük olamaz (hem `PATCH /vehicles/{id}/km` hem görev bitiminde kontrol edilir). |
| `ResourceNotFoundException` | 404 | Sürücü / araç / atama bulunamadı. |
| `DuplicateResourceException` | 409 | Ehliyet numarası veya plaka zaten kayıtlı. |
| `InvalidOperationException` | 409 | Örn. görevdeki araç bakıma alınamaz, atama geçmişi olan kayıt silinemez, atama zaten tamamlanmış. |

Tüm hatalar `@RestControllerAdvice` (`GlobalExceptionHandler`) üzerinden standart `ErrorResponse`
formatında döner:

```json
{
  "timestamp": "2026-09-29T10:15:30",
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "MAINTENANCE_REQUIRED",
  "message": "34 ABC 123 plakalı araç son bakımından bu yana 10500 km yapmış (sınır: 10000 km). Göreve atanamaz, bakıma alınmalıdır.",
  "path": "/api/v1/assignments",
  "validationErrors": []
}
```

## Eşzamanlılık

Atama ve KM güncelleme işlemlerinde `PESSIMISTIC_WRITE` kilidi kullanılır (`findWithLockById`); iki
isteğin aynı anda aynı sürücü/araca çakışan işlem yapması engellenir. Sürücü → araç sırasıyla kilitlenerek
deadlock riski azaltılır.

## Çalıştırma

### 1. Veritabanını ayağa kaldırın

```bash
docker compose up -d postgres
```

### 2. Uygulamayı çalıştırın

```bash
mvn spring-boot:run
```

veya uygulamayı da konteynerde çalıştırmak isterseniz:

```bash
docker compose --profile app up --build
```

Uygulama varsayılan olarak `http://localhost:8080` üzerinde ayağa kalkar.

### 3. Swagger / OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Sorun Giderme

**PostgreSQL konteyneri başlamıyor veya uygulama veritabanına bağlanamıyor (port 5432 meşgul)**

macOS'ta Homebrew ile kurulu yerel bir PostgreSQL servisi genelde 5432 portunu zaten dinliyor olabilir;
bu durumda `docker compose up -d postgres` konteyneri ayağa kalksa bile port çakışması nedeniyle
uygulama bağlanamaz. Kontrol etmek için:

```bash
lsof -i :5432
```

Bir sonuç dönerse port doludur. İki seçeneğiniz var:

1. **Yerel PostgreSQL'i durdurun** (varsa): `brew services stop postgresql@<sürüm>`
2. **Uygulamayı farklı bir porta yönlendirin** — `docker-compose.yml`'i değiştirmeden, konteyneri
   geçici olarak başka bir host portuna eşleyip uygulamayı o porta işaret edin:
   ```bash
   docker run -d --name fleet-postgres-local -p 5435:5432 \
     -e POSTGRES_DB=fleetdb -e POSTGRES_USER=fleet -e POSTGRES_PASSWORD=fleet123 \
     postgres:16-alpine
   DB_URL=jdbc:postgresql://localhost:5435/fleetdb mvn spring-boot:run
   ```

`docker-compose.yml`'deki port eşlemesini kalıcı olarak değiştirip **repoya commit etmeyin** — bu
sizin makinenizdeki çakışmaya özel bir geçici çözüm; başka bir geliştiricide 5432 boş olabilir ve
onun ortamında anlamsız bir fark yaratır. Kalıcı bir yerel tercih istiyorsanız `.gitignore`'a
eklenmiş, sizin repo dışı bir `docker-compose.override.yml` dosyası kullanın.

**Eski veri/şema kalıntısı nedeniyle tablo hataları**

```bash
docker compose down -v   # -v: volume'u da siler, veritabanını sıfırlar
docker compose up -d postgres
```

## Testler ve Kod Kapsama

```bash
mvn clean verify
```

- Unit testler (`service` paketi): Mockito ile bağımlılıklar izole edilerek iş kuralları test edilir
  (ehliyet sınıfı/geçerliliği, aktif görev çakışması, bakım eşiği, KM geri gitme, vb.).
- Entegrasyon testleri: `@DataJpaTest` ile repository sorguları (H2, PostgreSQL modu), `@SpringBootTest`
  + `MockMvc` ile Controller → Service → Repository uçtan uca akış ve hata senaryoları.
- `mvn clean verify` komutu JaCoCo raporunu `target/site/jacoco/index.html` altında üretir ve satır
  kapsamı **%70'in altındaysa build'i başarısız kılar** (`jacoco-maven-plugin` `check` execution'ı).

## Örnek Uç Noktalar

| Metot | Yol | Açıklama |
|---|---|---|
| `POST` | `/api/v1/drivers` | Sürücü oluşturur |
| `GET` | `/api/v1/drivers/available?requiredLicenseClass=CE` | Atamaya uygun sürücüleri listeler |
| `POST` | `/api/v1/vehicles` | Araç oluşturur |
| `PATCH` | `/api/v1/vehicles/{id}/km` | Kilometreyi günceller |
| `GET` | `/api/v1/vehicles/maintenance-due` | Bakım zamanı gelen araçları listeler |
| `POST` | `/api/v1/assignments` | Sürücüye araç atar |
| `PATCH` | `/api/v1/assignments/{id}/complete` | Görevi sonlandırır |
| `POST` | `/api/v1/vehicles/{id}/maintenance/start` | Aracı bakıma alır |
| `POST` | `/api/v1/vehicles/{id}/maintenance/complete` | Bakımı tamamlar, bakım kaydı oluşturur |
| `GET` | `/api/v1/vehicles/{id}/maintenance/logs` | Aracın bakım geçmişi |

Tüm uçların tam parametre/şema açıklamaları Swagger UI üzerinden görüntülenebilir.

## Örnek İstek / Yanıtlar

**Sürücü oluşturma** — `POST /api/v1/drivers`

```json
{
  "firstName": "Ali",
  "lastName": "Yılmaz",
  "phone": "05321234567",
  "licenseNumber": "TR-C-123456",
  "licenseClass": "CE",
  "licenseExpiryDate": "2030-12-31",
  "active": true
}
```

Yanıt (`201 Created`):

```json
{
  "id": 1,
  "firstName": "Ali",
  "lastName": "Yılmaz",
  "fullName": "Ali Yılmaz",
  "phone": "05321234567",
  "licenseNumber": "TR-C-123456",
  "licenseClass": "CE",
  "licenseExpiryDate": "2030-12-31",
  "active": true
}
```

**Sürücüye araç atama** — `POST /api/v1/assignments`

```json
{
  "driverId": 1,
  "vehicleId": 1
}
```

Yanıt (`201 Created`):

```json
{
  "id": 1,
  "driverId": 1,
  "driverFullName": "Ali Yılmaz",
  "vehicleId": 1,
  "vehiclePlate": "34 ABC 123",
  "startDate": "2026-09-30T10:15:30",
  "endDate": null,
  "startKm": 12500,
  "endKm": null,
  "status": "ACTIVE",
  "vehicleMaintenanceDue": false
}
```

Aynı isteği bakımı gelen bir araçla tekrarlarsanız (`422 Unprocessable Entity`):

```json
{
  "timestamp": "2026-09-30T10:16:02",
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "MAINTENANCE_REQUIRED",
  "message": "34 ABC 123 plakalı araç son bakımından bu yana 10500 km yapmış (sınır: 10000 km). Göreve atanamaz, bakıma alınmalıdır.",
  "path": "/api/v1/assignments",
  "validationErrors": []
}
```

## Kod Kalitesi Notları (Madde 8)

- **KM doğrulama tekrarı giderildi**: "Yeni KM, mevcut KM'den düşük olamaz" kuralı hem `VehicleService`
  hem `AssignmentService` içinde ayrı ayrı yazılmak yerine tek bir yardımcı sınıfta
  (`com.filo.fleet.util.KilometerValidator`) toplanmıştır.
- **JPA entity'lerinde güvenli `equals`/`hashCode`**: `Driver`, `Vehicle`, `VehicleAssignment`,
  `MaintenanceLog` entity'leri, Hibernate için önerilen "id atanana kadar yalnızca referans eşitliği,
  id atandıktan sonra id bazlı eşitlik, sabit `hashCode`" desenini kullanır. Bu, entity'ler
  koleksiyonlarda (`Set`, `List` içinde arama vb.) kullanıldığında tutarsız davranışı önler.



## Notlar

- Şema yönetimi `spring.jpa.hibernate.ddl-auto=update` ile yapılır; uygulama ilk açılışta gerekli
  tabloları PostgreSQL üzerinde otomatik oluşturur.
- `fleet.maintenance.default-interval-km` ve `fleet.maintenance.default-interval-days` (varsayılan
  10.000 km / 180 gün) `application.yml` üzerinden veya ortam değişkeniyle değiştirilebilir.
