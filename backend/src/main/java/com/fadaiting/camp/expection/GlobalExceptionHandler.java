package com.fadaiting.camp.expection;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import com.fadaiting.camp.pojo.Result;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.ObjectError;


import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(LineIdentityException.class)
    public org.springframework.http.ResponseEntity<Result> handleLineIdentity(LineIdentityException exception) {
        return org.springframework.http.ResponseEntity.status(exception.isUnavailable() ? 503 : 401).body(Result.error(exception.getMessage()));
    }
    @ExceptionHandler(BookingConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result handleBookingConflict(BookingConflictException exception) {
        return Result.error(exception.getMessage());
    }
    @ExceptionHandler(AccountConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result handleAccountConflict(AccountConflictException exception) {
        return Result.error(exception.getMessage());
    }
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result handleAccessDenied(AccessDeniedException exception) {
        return Result.error(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e){
        e.printStackTrace();
        return Result.error(StringUtils.hasLength(e.getMessage())?e.getMessage():"操作失敗");
    }
    // 处理验证异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationException(MethodArgumentNotValidException e) {
        BindingResult result = e.getBindingResult();
        String errorMessage = result.getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(errorMessage);
    }
}
