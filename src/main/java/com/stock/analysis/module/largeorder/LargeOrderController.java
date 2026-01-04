package com.stock.analysis.module.largeorder;

import com.stock.analysis.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/largeorder")
@RequiredArgsConstructor
public class LargeOrderController {

    private final LargeOrderMonitorService monitorService;

    /**
     * 模拟触发 Level-2 数据流 (测试用)
     * 每次调用会随机生成一笔交易，如果金额 > 50万则触发大单逻辑
     */
    @PostMapping("/simulate")
    public Result<String> simulateTrade(@RequestParam String stockCode) {
        monitorService.processRealTimeStream(stockCode);
        return Result.success("模拟数据已发送，请查看控制台日志");
    }
}
