package com.example.bilibili.service.handler;

import com.example.bilibili.domain.JsonResponse;
import com.example.bilibili.domain.exception.ConditionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonGlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    @ResponseBody // return type
    public JsonResponse<String> commonExceptionHandler(HttpServletRequest request, Exception e){
        String errorMsg = e.getMessage();
        if(e instanceof ConditionException){
            // show user condition exception instead of runtime exception for easier debug
            String errorCode = ((ConditionException)e).getCode();
            return new JsonResponse<>(errorCode, errorMsg);
        }else{
            return new JsonResponse<>("500", errorMsg);
        }
    }
}