package com.stock.analysis.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    SUCCESS(200, "操作成功"),
    
    // 4xx Client Errors
    PARAM_ERROR(400, "参数无效/缺失"),
    UNAUTHORIZED(401, "未授权/Token过期"),
    FORBIDDEN(403, "权限不足"),
    NOT_FOUND(404, "资源不存在"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    
    // 5xx Server Errors
    INTERNAL_SERVER_ERROR(500, "服务端内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    
    // 6xx Business Errors
    STOCK_CODE_ERROR(60001, "股票代码格式错误"),
    ACCOUNT_NOT_FOUND(60002, "账户不存在"),
    NO_HEAT_DATA(60003, "游资数据暂无记录"),
    LARGE_ORDER_THRESHOLD_ERROR(60004, "大单阈值设置不合理");

    private final int code;
    private final String msg;
}
