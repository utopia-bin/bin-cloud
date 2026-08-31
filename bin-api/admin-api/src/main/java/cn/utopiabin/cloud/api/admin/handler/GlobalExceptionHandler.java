package cn.utopiabin.cloud.api.admin.handler;

import cn.utopiabin.cloud.common.exception.BizException;
import cn.utopiabin.cloud.common.rest.RestResult;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 管理端 REST 接口统一异常处理器。
 *
 * @author Bin
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<RestResult<Void>> handleBizException(BizException exception) {
        HttpStatus status = resolveHttpStatus(exception.getCode());
        return ResponseEntity.status(status)
                .body(RestResult.fail(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResult<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数错误" : error.getDefaultMessage())
                .orElse("请求参数错误");
        return badRequest(message);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<RestResult<Void>> handleBindException(BindException exception) {
        String message = exception.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "请求参数错误" : error.getDefaultMessage())
                .orElse("请求参数错误");
        return badRequest(message);
    }

    @ExceptionHandler({ConstraintViolationException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<RestResult<Void>> handleRequestException(Exception exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler({org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<RestResult<Void>> handleMalformedRequest(Exception exception) {
        return badRequest("请求参数格式错误，请检查 ID、日期及必填字段");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestResult<Void>> handleUnexpectedException(Exception exception) {
        var seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable, Boolean>());
        for (Throwable cause = exception; cause != null && seen.add(cause); cause = cause.getCause()) {
            if (cause instanceof BizException biz) return handleBizException(biz);
        }
        LOG.error("管理端接口发生未处理异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RestResult.fail(500, "系统繁忙，请稍后重试"));
    }

    private ResponseEntity<RestResult<Void>> badRequest(String message) {
        return ResponseEntity.badRequest().body(RestResult.fail(400, message));
    }

    private HttpStatus resolveHttpStatus(int code) {
        return switch (code) {
            case 400 -> HttpStatus.BAD_REQUEST;
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            default -> code >= 500 && code < 600
                    ? HttpStatus.INTERNAL_SERVER_ERROR
                    : HttpStatus.BAD_REQUEST;
        };
    }
}
