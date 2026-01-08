package com.stock.analysis.module.heatflow;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * AkShare数据服务，用于调用Python接口获取股票数据
 */
@Service
@RequiredArgsConstructor
public class AkShareService {

    private final RestTemplate restTemplate;
    
    @Value("${akshare.api.url:http://localhost:5000}")
    private String akshareApiUrl;
    
    /**
     * 获取东方财富龙虎榜详情
     * @param stockCode 股票代码
     * @param date 日期
     * @return 龙虎榜详情数据
     */
    public List<JsonNode> getLhbDetail(String stockCode, String date) {
        String url = UriComponentsBuilder.fromHttpUrl(akshareApiUrl)
                .path("/api/v1/akshare/lhb/detail")
                .queryParam("stock_code", stockCode)
                .queryParam("date", date)
                .toUriString();
        
        ResponseEntity<AkShareResponse> response = restTemplate.getForEntity(url, AkShareResponse.class);
        return response.getBody().getData();
    }
    
    /**
     * 获取两融余量异动
     * @param stockCode 股票代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 两融余量异动数据
     */
    public List<JsonNode> getMarginDetail(String stockCode, String startDate, String endDate) {
        String url = UriComponentsBuilder.fromHttpUrl(akshareApiUrl)
                .path("/api/v1/akshare/margin/detail")
                .queryParam("stock_code", stockCode)
                .queryParam("start_date", startDate)
                .queryParam("end_date", endDate)
                .toUriString();
        
        ResponseEntity<AkShareResponse> response = restTemplate.getForEntity(url, AkShareResponse.class);
        return response.getBody().getData();
    }
}
