package com.stock.analysis.module.tech;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
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
}
