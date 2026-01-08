package com.stock.analysis.module.tech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Kline implements Serializable {
    private LocalDate date;
    private int open;   // 分
    private int close;  // 分
    private int high;   // 分
    private int low;    // 分
    private long volume; // 股
    private long amount; // 分
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public int getOpen() {
        return open;
    }
    
    public void setOpen(int open) {
        this.open = open;
    }
    
    public int getClose() {
        return close;
    }
    
    public void setClose(int close) {
        this.close = close;
    }
    
    public int getHigh() {
        return high;
    }
    
    public void setHigh(int high) {
        this.high = high;
    }
    
    public int getLow() {
        return low;
    }
    
    public void setLow(int low) {
        this.low = low;
    }
    
    public long getVolume() {
        return volume;
    }
    
    public void setVolume(long volume) {
        this.volume = volume;
    }
    
    public long getAmount() {
        return amount;
    }
    
    public void setAmount(long amount) {
        this.amount = amount;
    }
}
