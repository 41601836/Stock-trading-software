package com.stock.analysis.module.opinion;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 舆情数据实体类
 * 对应表: t_public_opinion
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicOpinion implements Serializable {

    private Long id;

    /**
     * 关联股票代码 (可为空，若为大盘舆情)
     */
    private String stockCode;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容摘要
     */
    private String content;

    /**
     * 来源: 财联社/雪球/东方财富等
     */
    private String source;

    /**
     * 情感评分 (0-100, 50中性)
     */
    private Integer sentimentScore;

    /**
     * 事件标签 (逗号分隔)
     */
    private String tags;

    /**
     * 原文链接
     */
    private String url;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
