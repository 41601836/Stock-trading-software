package com.stock.analysis.module.opinion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpinionAnalysisServiceImpl implements OpinionAnalysisService {

    // 热点阈值：单股票单日舆情数量超过该值则触发热点
    private static final int HOT_EVENT_THRESHOLD = 10;
    // 积极情绪阈值
    private static final int POSITIVE_THRESHOLD = 60;
    // 消极情绪阈值
    private static final int NEGATIVE_THRESHOLD = 40;

    // 模拟舆情数据存储
    private final List<PublicOpinion> mockOpinions = new ArrayList<>();

    @Override
    public PublicOpinion processOpinion(PublicOpinion opinion) {
        if (opinion == null) {
            return null;
        }

        // 1. 过滤无效舆情
        if (!isValidOpinion(opinion)) {
            log.debug("Filtered invalid opinion: {}", opinion.getTitle());
            return null;
        }

        // 2. 情感评分（模拟实现，实际应调用NLP服务）
        if (opinion.getSentimentScore() == null) {
            opinion.setSentimentScore(calculateSentimentScore(opinion));
        }

        // 3. 热点检测（简化实现，实际应基于更复杂的算法）
        // 4. 标签生成（简化实现）
        opinion.setTags(generateTags(opinion));

        // 5. 存储舆情数据（实际应存储到数据库）
        mockOpinions.add(opinion);
        log.debug("Processed opinion: {} with sentiment score: {}", opinion.getTitle(), opinion.getSentimentScore());

        return opinion;
    }

    @Override
    public OpinionSummary getOpinionSummary(String stockCode, int days) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock code cannot be empty");
        }

        // 1. 过滤指定股票和时间范围内的舆情
        LocalDate startDate = LocalDate.now().minusDays(days);
        List<PublicOpinion> filteredOpinions = mockOpinions.stream()
                .filter(opinion -> stockCode.equals(opinion.getStockCode()))
                .filter(opinion -> opinion.getPublishTime().toLocalDate().isAfter(startDate))
                .collect(Collectors.toList());

        int totalCount = filteredOpinions.size();
        if (totalCount == 0) {
            return OpinionSummary.builder()
                    .stockCode(stockCode)
                    .totalCount(0)
                    .positiveRatio(BigDecimal.ZERO)
                    .negativeRatio(BigDecimal.ZERO)
                    .neutralRatio(BigDecimal.ONE)
                    .avgSentimentScore(50)
                    .isHotEvent(false)
                    .hotEventMsg("暂无舆情数据")
                    .build();
        }

        // 2. 计算情感分布
        long positiveCount = filteredOpinions.stream()
                .filter(opinion -> opinion.getSentimentScore() > POSITIVE_THRESHOLD)
                .count();

        long negativeCount = filteredOpinions.stream()
                .filter(opinion -> opinion.getSentimentScore() < NEGATIVE_THRESHOLD)
                .count();

        long neutralCount = totalCount - positiveCount - negativeCount;

        // 3. 计算比例
        BigDecimal total = BigDecimal.valueOf(totalCount);
        BigDecimal positiveRatio = BigDecimal.valueOf(positiveCount).divide(total, 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal negativeRatio = BigDecimal.valueOf(negativeCount).divide(total, 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal neutralRatio = BigDecimal.valueOf(neutralCount).divide(total, 2, BigDecimal.ROUND_HALF_UP);

        // 4. 计算平均情感得分
        int avgSentimentScore = (int) filteredOpinions.stream()
                .mapToInt(PublicOpinion::getSentimentScore)
                .average()
                .orElse(50);

        // 5. 热点检测
        boolean isHotEvent = totalCount >= HOT_EVENT_THRESHOLD;
        String hotEventMsg = isHotEvent ? "近期舆情关注度较高，请注意风险" : "舆情关注度正常";

        return OpinionSummary.builder()
                .stockCode(stockCode)
                .totalCount(totalCount)
                .positiveRatio(positiveRatio)
                .negativeRatio(negativeRatio)
                .neutralRatio(neutralRatio)
                .avgSentimentScore(avgSentimentScore)
                .isHotEvent(isHotEvent)
                .hotEventMsg(hotEventMsg)
                .build();
    }

    @Override
    public void mockImportOpinions(String stockCode, int count) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Stock code cannot be empty");
        }

        if (count <= 0) {
            throw new IllegalArgumentException("Count must be positive");
        }

        Random random = new Random();
        String[] sources = {"财联社", "雪球", "东方财富", "同花顺", "新浪财经"};
        String[] titles = {
                "{}业绩超预期，净利润同比增长50%",
                "{}宣布重大资产重组，股价涨停",
                "机构调研：看好{}未来发展前景",
                "{}董事长发表重要讲话，强调创新发展",
                "{}产品获得国家认证，市场份额有望提升",
                "{}遭遇利空消息，股价下跌",
                "分析师下调{}评级，目标价下调",
                "{}发布新产品，市场反响热烈",
                "{}与大型企业达成合作协议",
                "{}公布分红方案，每10股派5元"
        };

        for (int i = 0; i < count; i++) {
            // 随机生成舆情数据
            String title = String.format(titles[random.nextInt(titles.length)], stockCode);
            PublicOpinion opinion = PublicOpinion.builder()
                    .stockCode(stockCode)
                    .publishTime(LocalDateTime.now().minusDays(random.nextInt(30)).minusHours(random.nextInt(24)))
                    .title(title)
                    .content("这是一条关于" + stockCode + "的舆情内容，用于模拟测试。")
                    .source(sources[random.nextInt(sources.length)])
                    .sentimentScore(30 + random.nextInt(50)) // 30-80分
                    .url("https://example.com/opinion/" + i)
                    .createTime(LocalDateTime.now())
                    .build();

            // 处理并存储舆情
            processOpinion(opinion);
        }

        log.info("Mock imported {} opinions for stock: {}", count, stockCode);
    }

    // ----------------- 辅助方法 -----------------

    /**
     * 验证舆情是否有效
     */
    private boolean isValidOpinion(PublicOpinion opinion) {
        if (opinion == null) {
            return false;
        }

        // 验证标题和来源不能为空
        if (opinion.getTitle() == null || opinion.getTitle().trim().isEmpty()) {
            return false;
        }

        if (opinion.getSource() == null || opinion.getSource().trim().isEmpty()) {
            return false;
        }

        // 验证发布时间不能为空
        return opinion.getPublishTime() != null;
    }

    /**
     * 计算情感评分（模拟实现）
     */
    private int calculateSentimentScore(PublicOpinion opinion) {
        if (opinion == null || opinion.getTitle() == null) {
            return 50;
        }

        // 简单的关键词匹配（实际应使用更复杂的NLP算法）
        String text = opinion.getTitle() + " " + (opinion.getContent() != null ? opinion.getContent() : "");
        int score = 50;

        // 积极关键词
        String[] positiveWords = {"增长", "涨停", "超预期", "看好", "合作", "创新", "分红", "利好", "突破"};
        // 消极关键词
        String[] negativeWords = {"下跌", "跌停", "利空", "下调", "亏损", "风险", "警告", "负面"};

        for (String word : positiveWords) {
            if (text.contains(word)) {
                score += 5;
            }
        }

        for (String word : negativeWords) {
            if (text.contains(word)) {
                score -= 5;
            }
        }

        // 确保分数在0-100之间
        return Math.max(0, Math.min(100, score));
    }

    /**
     * 生成标签（模拟实现）
     */
    private String generateTags(PublicOpinion opinion) {
        if (opinion == null || opinion.getTitle() == null) {
            return "";
        }

        List<String> tags = new ArrayList<>();
        String text = opinion.getTitle() + " " + (opinion.getContent() != null ? opinion.getContent() : "");

        // 根据关键词生成标签
        if (text.contains("业绩")) {
            tags.add("业绩");
        }
        if (text.contains("重组")) {
            tags.add("重组");
        }
        if (text.contains("合作")) {
            tags.add("合作");
        }
        if (text.contains("创新")) {
            tags.add("创新");
        }
        if (text.contains("分红")) {
            tags.add("分红");
        }
        if (text.contains("产品")) {
            tags.add("产品发布");
        }
        if (text.contains("调研")) {
            tags.add("机构调研");
        }

        // 如果没有生成标签，添加默认标签
        if (tags.isEmpty()) {
            tags.add("其他");
        }

        return String.join(",", tags);
    }
}