package com.yutu.common.exception;

import com.yutu.common.result.Result;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<?> handleBiz(BizException ex) {
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(400, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBind(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(400, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return Result.fail(400, "上传文件过大，请控制在 10MB 内");
    }

    @ExceptionHandler({MultipartException.class, MissingServletRequestParameterException.class})
    public Result<?> handleMultipart(Exception ex) {
        return Result.fail(400, "上传请求格式错误，请重新选择图片上传");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        return Result.fail(500, ex.getMessage());
    }
}
