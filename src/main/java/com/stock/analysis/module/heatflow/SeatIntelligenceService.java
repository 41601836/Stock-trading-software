package com.stock.analysis.module.heatflow;

import java.util.List;
import java.util.Map;

/**
 * 席位情报库服务接口
 */
public interface SeatIntelligenceService {
    
    /**
     * 识别席位类型
     * @param seatName 席位名称
     * @return 席位信息，包含识别后的类型
     */
    SeatInfo identifySeatType(String seatName);
    
    /**
     * 计算席位溢价率
     * @param seatCode 席位代码
     * @param days 统计天数
     * @return 席位溢价率信息
     */
    SeatPremiumRate calculateSeatPremiumRate(String seatCode, int days);
    
    /**
     * 获取股票的龙虎榜席位信息
     * @param stockCode 股票代码
     * @param date 日期
     * @return 龙虎榜席位列表
     */
    List<LhbDetail> getStockLhbSeats(String stockCode, String date);
    
    /**
     * 获取活跃的顶级游资席位列表
     * @param limit 限制数量
     * @return 活跃顶级游资列表
     */
    List<SeatInfo> getActiveHotMoneySeats(int limit);
    
    /**
     * 获取机构席位操作记录
     * @param stockCode 股票代码
     * @param days 近几天
     * @return 机构席位操作记录
     */
    List<LhbDetail> getInstitutionSeatOperations(String stockCode, int days);
}