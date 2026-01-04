package com.stock.analysis.module.opinion;

import java.util.List;

public interface OpinionAnalysisService {

    /**
     * 处理单条舆情 (过滤 -> 评分 -> 热点检测)
     * @param opinion 原始舆情对象
     * @return 处理后的舆情对象 (若被过滤则返回 null)
     */
    PublicOpinion processOpinion(PublicOpinion opinion);

    /**
     * 获取舆情聚合摘要
     * @param stockCode 股票代码
     * @param days 统计天数
     * @return 聚合结果
     */
    OpinionSummary getOpinionSummary(String stockCode, int days);
    
    /**
     * 模拟批量导入舆情 (用于测试聚合)
     */
    void mockImportOpinions(String stockCode, int count);
}
