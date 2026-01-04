package com.stock.analysis.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
public class RequestIdInterceptor implements HandlerInterceptor {

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String START_TIME_KEY = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = request.getHeader(REQUEST_ID_KEY);
        if (requestId == null || requestId.isEmpty()) {
            requestId = "req-" + UUID.randomUUID().toString().replace("-", "");
        }
        
        MDC.put(REQUEST_ID_KEY, requestId);
        request.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        
        // 将 requestId 放入响应头，方便客户端追踪
        response.setHeader(REQUEST_ID_KEY, requestId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_KEY);
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String uri = request.getRequestURI();
            log.info("Request Finished: {} {} | Duration: {}ms", request.getMethod(), uri, duration);
            
            if (duration > 300) {
                log.warn("Slow Request Detected! Duration > 300ms: {}", uri);
            }
        }
        MDC.clear();
    }
}
