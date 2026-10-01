package com.filo.fleet.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.filo.fleet.dto.response.ErrorResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * {@link GlobalExceptionHandler}'ın her dalını doğrudan çağırarak test eder; bu sayede
 * entegrasyon testleriyle tetiklenmesi zor olan (ör. veri bütünlüğü ihlali, eşzamanlılık hatası,
 * desteklenmeyen HTTP metodu) senaryolar da güvenceye alınır.
 */
@DisplayName("GlobalExceptionHandler birim testleri")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

    // MethodArgumentNotValidException / MethodArgumentTypeMismatchException için MethodParameter üretmede kullanılır.
    @SuppressWarnings("unused")
    private void dummyTarget(Object arg) {
        // sadece reflection için var; hiçbir zaman çağrılmaz.
    }

    private MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", Object.class);
        return new MethodParameter(method, 0);
    }

    @Test
    @DisplayName("ApiException alt sınıfları kendi HTTP durumu ve kodlarıyla ErrorResponse döner")
    void handleApiException_returnsExceptionStatusAndCode() {
        ResponseEntity<ErrorResponse> response = handler.handleApiException(
                new ResourceNotFoundException("Araç", 1L), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(ResourceNotFoundException.CODE);
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("Doğrulama hataları alan adı ve mesajıyla birlikte 400 olarak döner")
    void handleValidation_returnsFieldErrors() throws NoSuchMethodException {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "driverRequest");
        bindingResult.addError(new FieldError("driverRequest", "licenseNumber", "Ehliyet numarası boş olamaz"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.VALIDATION_ERROR);
        assertThat(response.getBody().validationErrors()).hasSize(1);
        assertThat(response.getBody().validationErrors().get(0).field()).isEqualTo("licenseNumber");
    }

    @Test
    @DisplayName("Okunamayan istek gövdesi 400 ve MALFORMED_REQUEST kodu döner")
    void handleUnreadable_returns400() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("Bozuk JSON", (org.springframework.http.HttpInputMessage) null);

        ResponseEntity<ErrorResponse> response = handler.handleUnreadable(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.MALFORMED_REQUEST);
    }

    @Test
    @DisplayName("Yol parametresi tipi uyuşmazlığında 400 ve INVALID_PARAMETER kodu döner")
    void handleTypeMismatch_returns400() throws NoSuchMethodException {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", dummyMethodParameter(), new NumberFormatException("abc"));

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.INVALID_PARAMETER);
        assertThat(response.getBody().message()).contains("id");
    }

    @Test
    @DisplayName("Desteklenmeyen HTTP metodu 405 ve METHOD_NOT_ALLOWED kodu döner")
    void handleMethodNotSupported_returns405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.METHOD_NOT_ALLOWED);
    }

    @Test
    @DisplayName("Bilinmeyen uç nokta 404 ve ENDPOINT_NOT_FOUND kodu döner")
    void handleNoResource_returns404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/bilinmeyen");

        ResponseEntity<ErrorResponse> response = handler.handleNoResource(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.ENDPOINT_NOT_FOUND);
    }

    @Test
    @DisplayName("Veri bütünlüğü ihlali 409 ve DATA_CONFLICT kodu döner")
    void handleDataIntegrity_returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "unique constraint", new RuntimeException("kök neden"));

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrity(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.DATA_CONFLICT);
    }

    @Test
    @DisplayName("Eşzamanlılık hatası 409 ve CONCURRENT_UPDATE kodu döner")
    void handleConcurrency_returns409() {
        ConcurrencyFailureException ex = new ConcurrencyFailureException("kayıt kilitli");

        ResponseEntity<ErrorResponse> response = handler.handleConcurrency(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.CONCURRENT_UPDATE);
    }

    @Test
    @DisplayName("Beklenmeyen genel hata 500 ve INTERNAL_ERROR kodu döner, detay sızdırılmaz")
    void handleGeneric_returns500_withoutLeakingInternalDetails() {
        RuntimeException ex = new RuntimeException("veritabanı şifresi: gizli-1234");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo(GlobalExceptionHandler.INTERNAL_ERROR);
        assertThat(response.getBody().message()).doesNotContain("gizli-1234");
    }
}
