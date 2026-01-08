package com.stock.analysis.module.heatflow;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * AkShare API响应封装
 */
@Data
public class AkShareResponse {

    /**
     * 响应码
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String msg;
    
    /**
     * 响应数据
     */
    private List<JsonNode> data;
}