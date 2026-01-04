package com.stock.analysis.module.opinion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpinionAnalysisServiceImpl implements OpinionAnalysisService {

    // 模拟数据库存储
    private final Map<String, List<PublicOpinion>> opinionStore = new ConcurrentHashMap<>();
    
    // 广告关键词黑名单
    private static final Set<String> SPAM_KEYWORDS = Set.of("加群", "免费领取", "推荐牛股", "内部消息", "点击链接");

    @Override
    public PublicOpinion processOpinion(PublicOpinion opinion) {
        // Layer 1: 关键词规则过滤 (FastText 降噪思想)
        if (isSpam(opinion.getContent()) || isSpam(opinion.getTitle())) {
            log.warn("舆情被过滤(广告): {}", opinion.getTitle());
            return null;
        }

        // Layer 2: 深度学习模型情感评分 (RoBERTa Hook)
        int score = callRobertaModel(opinion.getTitle() + " " + opinion.getContent());
        opinion.setSentimentScore(score);
        
        // 补充时间
        if (opinion.getPublishTime() == null) {
            opinion.setPublishTime(LocalDateTime.now());
        }

        // 存入模拟库
        opinionStore.computeIfAbsent(opinion.getStockCode(), k -> new ArrayList<>()).add(opinion);

        // 触发热点检测
        checkHotEvent(opinion.getStockCode());

        return opinion;
    }

    @Override
    public OpinionSummary getOpinionSummary(String stockCode, int days) {
        List<PublicOpinion> opinions = opinionStore.getOrDefault(stockCode, Collections.emptyList());
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);

        // 筛选时间范围内的舆情
        List<PublicOpinion> validOpinions = opinions.stream()
                .filter(o -> o.getPublishTime().isAfter(startTime))
                .toList();

        int total = validOpinions.size();
        if (total == 0) {
            return OpinionSummary.builder()
                    .stockCode(stockCode)
                    .totalCount(0)
                    .positiveRatio(BigDecimal.ZERO)
                    .negativeRatio(BigDecimal.ZERO)
                    .neutralRatio(BigDecimal.ZERO)
                    .avgSentimentScore(50)
                    .build();
        }

        // 统计分布
        long positiveCount = validOpinions.stream().filter(o -> o.getSentimentScore() > 60).count();
        long negativeCount = validOpinions.stream().filter(o -> o.getSentimentScore() < 40).count();
        long neutralCount = total - positiveCount - negativeCount;
        
        double avgScore = validOpinions.stream().mapToInt(PublicOpinion::getSentimentScore).average().orElse(50.0);

        // 检查当前是否处于热点状态
        boolean isHot = checkHotEvent(stockCode);

        return OpinionSummary.builder()
                .stockCode(stockCode)
                .totalCount(total)
                .positiveRatio(calculateRatio(positiveCount, total))
                .negativeRatio(calculateRatio(negativeCount, total))
                .neutralRatio(calculateRatio(neutralCount, total))
                .avgSentimentScore((int) avgScore)
                .isHotEvent(isHot)
                .hotEventMsg(isHot ? "当前舆情热度激增，请注意风险！" : null)
                .build();
    }

    @Override
    public void mockImportOpinions(String stockCode, int count) {
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            PublicOpinion op = PublicOpinion.builder()
                    .stockCode(stockCode)
                    .title("模拟新闻标题 " + i)
                    .content("模拟新闻内容 " + i)
                    .source("MockSource")
                    .publishTime(LocalDateTime.now().minusHours(random.nextInt(48))) // 过去48小时内
                    .build();
            processOpinion(op);
        }
    }

    // ----------------- 内部核心逻辑 -----------------

    /**
     * Layer 1: 广告过滤
     */
    private boolean isSpam(String text) {
        if (text == null) return false;
        for (String keyword : SPAM_KEYWORDS) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    /**
     * Layer 2: RoBERTa 模型调用 Hook
     * 这里使用简单的关键词匹配模拟深度学习模型的输出
     */
    private int callRobertaModel(String text) {
        // 模拟 API 调用延迟
        // Thread.sleep(10); 
        
        int baseScore = 50;
        if (text.contains("利好") || text.contains("大涨") || text.contains("突破") || text.contains("买入")) {
            baseScore += 30;
        } else if (text.contains("利空") || text.contains("大跌") || text.contains("破位") || text.contains("减持")) {
            baseScore -= 30;
        }
        
        // 增加随机扰动 (0-10)
        int noise = new Random().nextInt(11) - 5;
        int finalScore = baseScore + noise;
        
        return Math.max(0, Math.min(100, finalScore)); // 限制在 0-100
    }

    /**
     * 热点事件自发现逻辑
     * 规则：1小时内舆情条数激增 300% (对比上一个小时)
     */
    private boolean checkHotEvent(String stockCode) {
        List<PublicOpinion> opinions = opinionStore.getOrDefault(stockCode, Collections.emptyList());
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);

        long currentHourCount = opinions.stream()
                .filter(o -> o.getPublishTime().isAfter(oneHourAgo) && o.getPublishTime().isBefore(now))
                .count();

        long prevHourCount = opinions.stream()
                .filter(o -> o.getPublishTime().isAfter(twoHoursAgo) && o.getPublishTime().isBefore(oneHourAgo))
                .count();

        // 避免分母为0，且设定最小触发阈值 (例如当前小时至少要有 5 条才算热点)
        if (prevHourCount == 0) {
            return currentHourCount >= 10; // 如果之前没数据，突然出现10条，也算激增
        }
        
        if (currentHourCount < 5) {
            return false; // 绝对数量太少，忽略
        }

        double growthRate = (double) (currentHourCount - prevHourCount) / prevHourCount;
        
        if (growthRate >= 3.0) { // 300%
            log.warn("【热点预警】股票 {} 舆情激增！当前1h: {}, 上个1h: {}, 增长率: {}%", 
                    stockCode, currentHourCount, prevHourCount, (int)(growthRate * 100));
            return true;
        }
        
        return false;
    }

    private BigDecimal calculateRatio(long count, int total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf((double) count / total).setScale(4, RoundingMode.HALF_UP);
    }
}
