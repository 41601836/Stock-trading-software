package com.stock.analysis.module.risk;

import com.stock.analysis.module.largeorder.LargeOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RiskControlService {

    // 模拟配置：当前持仓方向 (多头)
    private static final String CURRENT_POSITION_DIRECTION = "BUY";
    // 模拟配置：关键支撑位 (单位: 分)
    private static final int SUPPORT_PRICE = 1000;

    /**
     * 监听大单异动事件
     */
    @Async // 异步处理，不阻塞交易流
    @EventListener
    public void handleLargeOrderEvent(LargeOrderEvent event) {
        log.info("Risk Agent 收到大单数据: code={}, amount={}万, price={}, dir={}", 
                event.getStockCode(), event.getAmount() / 1000000.0, event.getPrice(), event.getDirection());

        // 1. 判断方向是否相反 (持仓买入，大单卖出则为风险)
        boolean isOppositeDirection = !CURRENT_POSITION_DIRECTION.equalsIgnoreCase(event.getDirection());

        // 2. 判断是否跌破支撑位
        boolean isBelowSupport = event.getPrice() < SUPPORT_PRICE;

        if (isOppositeDirection && isBelowSupport) {
            triggerRiskAlert(event);
        }
    }

    private void triggerRiskAlert(LargeOrderEvent event) {
        log.error("【风控预警】检测到危险大单！股票: {}, 价格: {} (跌破支撑位 {}), 方向: {}, 金额: {}万",
                event.getStockCode(),
                event.getPrice(),
                SUPPORT_PRICE,
                event.getDirection(),
                event.getAmount() / 1000000.0);
        
        // TODO: 这里可以进一步调用交易接口进行减仓或止损
    }
}
