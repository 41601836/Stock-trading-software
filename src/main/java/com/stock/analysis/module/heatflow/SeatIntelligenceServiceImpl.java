package com.stock.analysis.module.heatflow;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 席位情报库服务实现
 */
@Service
@RequiredArgsConstructor
public class SeatIntelligenceServiceImpl implements SeatIntelligenceService {

    private final AkShareService akShareService;
    
    // 顶级游资席位名称列表
    private static final List<String> TOP_HOT_MONEY_SEATS = Arrays.asList(
            "朱雀大街", "南京唯宁路", "章盟主", "赵老哥", "孙哥", "养家", "小鳄鱼", "方新侠",
            "作手新一", "宁波解放南路", "上海溧阳路", "成都系", "佛山系", "温州帮", "山东帮"
    );
    
    // 机构专用席位标识
    private static final String INSTITUTION_SEAT_FLAG = "机构专用";
    
    @Override
    public SeatInfo identifySeatType(String seatName) {
        SeatInfo seatInfo = new SeatInfo();
        seatInfo.setSeatName(seatName);
        
        // 识别机构席位
        if (seatName.contains(INSTITUTION_SEAT_FLAG)) {
            seatInfo.setSeatType(2);
            seatInfo.setSeatTypeName("机构专用");
        } 
        // 识别顶级游资
        else if (isTopHotMoneySeat(seatName)) {
            seatInfo.setSeatType(1);
            seatInfo.setSeatTypeName("顶级游资");
            seatInfo.setHotMoneyLevel(1);
        } 
        // 普通席位
        else {
            seatInfo.setSeatType(3);
            seatInfo.setSeatTypeName("普通席位");
        }
        
        return seatInfo;
    }
    
    @Override
    public SeatPremiumRate calculateSeatPremiumRate(String seatCode, int days) {
        // 这里简化实现，实际应该查询历史交易数据计算
        // 模拟计算结果
        SeatPremiumRate premiumRate = new SeatPremiumRate();
        premiumRate.setSeatCode(seatCode);
        premiumRate.setStatDays(days);
        
        // 模拟数据：根据席位类型设置不同的溢价率
        // 实际应该从数据库或其他数据源获取历史数据进行计算
        Random random = new Random();
        if (seatCode.startsWith("JG")) {
            // 机构席位
            premiumRate.setAvgNextDayIncrease(random.nextInt(50, 150));
            premiumRate.setAvgFiveDayIncrease(random.nextInt(100, 300));
            premiumRate.setWinRate(random.nextInt(60, 80));
        } else if (seatCode.startsWith("YZ")) {
            // 游资席位
            premiumRate.setAvgNextDayIncrease(random.nextInt(30, 200));
            premiumRate.setAvgFiveDayIncrease(random.nextInt(50, 400));
            premiumRate.setWinRate(random.nextInt(50, 75));
        } else {
            // 普通席位
            premiumRate.setAvgNextDayIncrease(random.nextInt(-20, 80));
            premiumRate.setAvgFiveDayIncrease(random.nextInt(-50, 150));
            premiumRate.setWinRate(random.nextInt(40, 60));
        }
        
        return premiumRate;
    }
    
    @Override
    public List<LhbDetail> getStockLhbSeats(String stockCode, String date) {
        // 调用AkShare获取龙虎榜数据
        List<JsonNode> lhbData = akShareService.getLhbDetail(stockCode, date);
        
        // 转换为LhbDetail对象并识别席位类型
        List<LhbDetail> result = new ArrayList<>();
        for (JsonNode data : lhbData) {
            LhbDetail detail = convertToLhbDetail(data, stockCode, date);
            // 识别席位类型
            SeatInfo seatInfo = identifySeatType(detail.getDepartmentName());
            detail.setSeatType(seatInfo.getSeatType());
            result.add(detail);
        }
        
        return result;
    }
    
    @Override
    public List<SeatInfo> getActiveHotMoneySeats(int limit) {
        // 这里简化实现，实际应该查询活跃的顶级游资
        List<SeatInfo> result = new ArrayList<>();
        
        // 模拟数据
        for (int i = 0; i < Math.min(limit, TOP_HOT_MONEY_SEATS.size()); i++) {
            SeatInfo seatInfo = new SeatInfo();
            seatInfo.setSeatName(TOP_HOT_MONEY_SEATS.get(i));
            seatInfo.setSeatType(1);
            seatInfo.setSeatTypeName("顶级游资");
            seatInfo.setHotMoneyLevel(1);
            seatInfo.setRecentAppearCount(15 + new Random().nextInt(20));
            seatInfo.setActiveStatus(1);
            result.add(seatInfo);
        }
        
        return result;
    }
    
    @Override
    public List<LhbDetail> getInstitutionSeatOperations(String stockCode, int days) {
        // 这里简化实现，实际应该查询最近几天的机构席位操作记录
        List<LhbDetail> result = new ArrayList<>();
        
        // 模拟数据
        for (int i = 0; i < days; i++) {
            LhbDetail detail = new LhbDetail();
            detail.setStockCode(stockCode);
            detail.setTradeDate("2024-01-" + String.format("%02d", i + 1));
            detail.setDepartmentName(INSTITUTION_SEAT_FLAG);
            detail.setSeatCode("JG" + String.format("%06d", i + 1));
            detail.setBuyAmount((long) (100000000 + new Random().nextInt(900000000)));
            detail.setSellAmount((long) (new Random().nextInt(500000000)));
            detail.setNetBuyAmount(detail.getBuyAmount() - detail.getSellAmount());
            detail.setBuyRatio(100 + new Random().nextInt(200));
            detail.setSellRatio(new Random().nextInt(100));
            detail.setSeatType(2);
            result.add(detail);
        }
        
        return result;
    }
    
    /**
     * 判断是否为顶级游资席位
     */
    private boolean isTopHotMoneySeat(String seatName) {
        for (String hotMoneySeat : TOP_HOT_MONEY_SEATS) {
            if (seatName.contains(hotMoneySeat)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 将JsonNode转换为LhbDetail对象
     */
    private LhbDetail convertToLhbDetail(JsonNode data, String stockCode, String date) {
        LhbDetail detail = new LhbDetail();
        detail.setStockCode(stockCode);
        detail.setTradeDate(date);
        
        // 根据实际返回的JSON结构进行字段映射
        // 这里需要根据AkShare返回的实际字段名进行调整
        if (data.has("营业部名称")) {
            detail.setDepartmentName(data.get("营业部名称").asText());
        }
        if (data.has("买入额")) {
            detail.setBuyAmount((long) (data.get("买入额").asDouble() * 100)); // 转换为分
        }
        if (data.has("卖出额")) {
            detail.setSellAmount((long) (data.get("卖出额").asDouble() * 100)); // 转换为分
        }
        if (data.has("净额")) {
            detail.setNetBuyAmount((long) (data.get("净额").asDouble() * 100)); // 转换为分
        }
        
        // 计算净买入金额
        if (detail.getBuyAmount() != null && detail.getSellAmount() != null) {
            detail.setNetBuyAmount(detail.getBuyAmount() - detail.getSellAmount());
        }
        
        return detail;
    }
}