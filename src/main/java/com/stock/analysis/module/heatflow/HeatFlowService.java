package com.stock.analysis.module.heatflow;

import com.stock.analysis.common.model.StockBase;
import com.stock.analysis.common.model.StockPrice;

import java.util.List;
import java.util.Map;

/**
 * 资金流向分析服务
 */
public interface HeatFlowService {
    
    /**
     * 获取股票资金流向数据
     * @param stockCode 股票代码
     * @return 资金流向分析结果
     */
    HeatFlowData getHeatFlowData(String stockCode);
    
    /**
     * 获取行业资金流向排行榜
     * @param industry 行业名称
     * @return 行业资金流向排行榜
     */
    List<IndustryHeatFlow> getIndustryHeatFlowRanking(String industry);
    
    /**
     * 获取概念资金流向排行榜
     * @param concept 概念名称
     * @return 概念资金流向排行榜
     */
    List<ConceptHeatFlow> getConceptHeatFlowRanking(String concept);
    
    /**
     * 获取热门资金流入股票列表
     * @param limit 限制数量
     * @return 热门资金流入股票列表
     */
    List<StockBase> getHotInflowStocks(int limit);
    
    /**
     * 获取热门资金流出股票列表
     * @param limit 限制数量
     * @return 热门资金流出股票列表
     */
    List<StockBase> getHotOutflowStocks(int limit);
}