package com.stock.analysis.module.heatflow;

import com.stock.analysis.common.model.BaseEntity;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 席位信息模型
 */
@Data
@Accessors(chain = true)
public class SeatInfo extends BaseEntity {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 席位代码
     */
    private String seatCode;
    
    /**
     * 席位名称
     */
    private String seatName;
    
    /**
     * 席位类型：1-顶级游资，2-机构专用，3-普通席位
     */
    private Integer seatType;
    
    /**
     * 席位类型名称
     */
    private String seatTypeName;
    
    /**
     * 游资等级（仅顶级游资有值）：1-顶级，2-一线，3-二线
     */
    private Integer hotMoneyLevel;
    
    /**
     * 近30天上榜次数
     */
    private Integer recentAppearCount;
    
    /**
     * 活跃状态：0-不活跃，1-活跃
     */
    private Integer activeStatus;
}