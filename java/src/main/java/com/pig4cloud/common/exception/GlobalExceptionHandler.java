package com.pig4cloud.common.exception;

import com.pig4cloud.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;

/**
 * 全局异常处理：业务异常转R.fail，HTTP状态保持200，由响应体code区分结果
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 方法级权限(@PreAuthorize)抛出的异常原样上抛，交给Security的AccessDeniedHandler处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDenied(AccessDeniedException e) throws AccessDeniedException {
        throw e;
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public R<Void> handleBadCredentials(Exception e) {
        return R.fail("用户名或密码不正确");
    }

    @ExceptionHandler(DisabledException.class)
    public R<Void> handleDisabled(DisabledException e) {
        return R.fail(e.getMessage());
    }

    /**
     * UserDetailsService里抛出的业务类认证异常(如所属租户停用/过期)会被DaoAuthenticationProvider
     * 包装成InternalAuthenticationServiceException，取原始message透出给前端
     */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public R<Void> handleInternalAuthenticationService(InternalAuthenticationServiceException e) {
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("请求参数不合法");
        return R.fail(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return R.fail("请求体格式错误");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class, MultipartException.class})
    public R<Void> handleParams(Exception e) {
        return R.fail("请求参数错误");
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        log.error("未处理异常", e);
        return R.fail("系统异常，请稍后重试");
    }
}
