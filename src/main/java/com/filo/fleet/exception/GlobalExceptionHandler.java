package com.filo.fleet.exception;


import lombok.extern.slf4j.Slf4j;
import com.filo.fleet.dto.response.ErrorResponse;
import com.filo.fleet.dto.response.ErrorResponse.FieldValidationError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tüm hataları {@link ErrorResponse} formatında döndüren merkezi hata yönetimi
 * ({@code @ControllerAdvice} + {@code @ExceptionHandler}).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    static final String INVALID_PARAMETER = "INVALID_PARAMETER";
    static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    static final String ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";
    static final String DATA_CONFLICT = "DATA_CONFLICT";
    static final String CONCURRENT_UPDATE = "CONCURRENT_UPDATE";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        return build(ex.getStatus(), ex.getCode(), ex.getMessage(), request, Collections.emptyList());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        List<FieldValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldValidationError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, VALIDATION_ERROR,
                "İstekteki bazı alanlar geçersiz. Lütfen alanları kontrol edin.", request, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST,
                "İstek gövdesi okunamadı. JSON biçimini ve alan değerlerini (ör. enum değerleri) kontrol edin.",
                request, Collections.emptyList());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        String message = "'" + ex.getName() + "' parametresi için geçersiz değer: " + ex.getValue();
        return build(HttpStatus.BAD_REQUEST, INVALID_PARAMETER, message, request, Collections.emptyList());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        return build(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED,
                "Bu uç nokta " + ex.getMethod() + " metodunu desteklemiyor.", request, Collections.emptyList());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
                                                          HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ENDPOINT_NOT_FOUND, "İstenen uç nokta bulunamadı.", request,
                Collections.emptyList());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        LOGGER.warn("Veri bütünlüğü ihlali: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, DATA_CONFLICT,
                "İşlem, veri bütünlüğü kurallarıyla çakıştı (ör. benzersiz alan tekrarı).", request,
                Collections.emptyList());
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrency(ConcurrencyFailureException ex,
                                                           HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        LOGGER.warn("Eşzamanlılık hatası: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, CONCURRENT_UPDATE,
                "Kayıt başka bir işlem tarafından kullanılıyor. Lütfen tekrar deneyin.", request,
                Collections.emptyList());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.warn("Handled business/validation exception: {}", ex.getMessage());
        LOGGER.error("Beklenmeyen hata: {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR,
                "Beklenmeyen bir hata oluştu. Lütfen daha sonra tekrar deneyin.", request,
                Collections.emptyList());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest request,
                                                List<FieldValidationError> validationErrors) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                code, message, request.getRequestURI(), validationErrors);
        return ResponseEntity.status(status).body(body);
    }
}
