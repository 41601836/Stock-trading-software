package com.stock.analysis.module.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position implements Serializable {
    private String stockCode;
    private String stockName;
    private int costPrice;      // 成本价(分)
    private int currentPrice;   // 现价(分)
    private int highestPrice;   // 持仓期间最高价(分) - 用于移动止损
    private int quantity;       // 持仓数量(股)
    
    public String getStockCode() {
        return stockCode;
    }
    
    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }
    
    public String getStockName() {
        return stockName;
    }
    
    public void setStockName(String stockName) {
        this.stockName = stockName;
    }
    
    public int getCostPrice() {
        return costPrice;
    }
    
    public void setCostPrice(int costPrice) {
        this.costPrice = costPrice;
    }
    
    public int getCurrentPrice() {
        return currentPrice;
    }
    
    public void setCurrentPrice(int currentPrice) {
        this.currentPrice = currentPrice;
    }
    
    public int getHighestPrice() {
        return highestPrice;
    }
    
    public void setHighestPrice(int highestPrice) {
        this.highestPrice = highestPrice;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
