package com.vn.backend.exception;

import com.vn.backend.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.file.AccessDeniedException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .statusCode(status.value())
                .message(message)
                .path(request.getRequestURI())
                .timestamp(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")))
                .build();

        if (status.is5xxServerError()) {
            log.error("HTTP {} at {} -> {}", status.value(), request.getRequestURI(), message);
        } else {
            log.warn("HTTP {} at {} -> {}", status.value(), request.getRequestURI(), message);
        }
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
        if (status.is5xxServerError()) {
            log.error("AppException({}): {}", status.value(), message, ex);
        } else {
            log.warn("AppException({}): {}", status.value(), message);
        }
        return build(status, message, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a, LinkedHashMap::new));
        String details = fieldErrors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
        String message = "Xác thực không thành công" + (details.isEmpty() ? "" : " - " + details);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a, LinkedHashMap::new));
        String details = fieldErrors.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("; "));
        String message = "Xác thực không thành công" + (details.isEmpty() ? "" : " - " + details);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                   HttpServletRequest request) {
        String details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        String message = "Vi phạm ràng buộc" + (details.isEmpty() ? "" : " - " + details);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /* -------- Sai kiểu tham số (ví dụ id=abc nhưng cần số) -------- */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Lỗi không khớp kiểu tham số - %s yêu cầu %s, giá trị là '%s'",
                ex.getName(), required, String.valueOf(ex.getValue()));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /* -------- Body không parse được / JSON sai định dạng -------- */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Yêu cầu JSON không đúng định dạng";
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /* -------- Thiếu tham số bắt buộc -------- */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        String message = "Thiếu tham số bắt buộc - " + ex.getParameterName();
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /* -------- Method không hỗ trợ -------- */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                  HttpServletRequest request) {
        String message = "Phương pháp không được phép - " + ex.getMethod();
        return build(HttpStatus.METHOD_NOT_ALLOWED, message, request);
    }

    /* -------- Media type không hỗ trợ -------- */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpServletRequest request) {
        String message = "Loại phương tiện không được hỗ trợ" + (ex.getContentType() != null ? " - " + ex.getContentType() : "");
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, request);
    }

    /* -------- Không đủ quyền (Spring Security) -------- */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    /* -------- User không tồn tại (Spring Security) -------- */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Không tìm thấy người dùng";
        return build(HttpStatus.UNAUTHORIZED, message, request);
    }

    /* -------- Sai thông tin đăng nhập (Spring Security) -------- */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Email hoặc mật khẩu không hợp lệ";
        return build(HttpStatus.UNAUTHORIZED, message, request);
    }

    /* -------- Lỗi xác thực chung (Spring Security) -------- */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Xác thực không thành công";
        return build(HttpStatus.UNAUTHORIZED, message, request);
    }

    /* -------------------- Fallback -------------------- */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Lỗi ngoại lệ chưa được xử lý tại {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi máy chủ nội bộ", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        // Log ở mức WARN nếu cần
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .message("Không tìm thấy")
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
