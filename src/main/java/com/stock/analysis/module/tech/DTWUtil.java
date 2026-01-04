package com.stock.analysis.module.tech;

import java.util.List;

/**
 * 动态时间规整 (DTW) 算法工具类
 */
public class DTWUtil {

    /**
     * 计算两个序列的 DTW 距离
     * @param s1 序列1 (如当前K线收盘价序列)
     * @param s2 序列2 (如标准暴涨模型序列)
     * @return 距离 (越小越相似)
     */
    public static double calculateDTW(List<Double> s1, List<Double> s2) {
        int n = s1.size();
        int m = s2.size();
        
        if (n == 0 || m == 0) return Double.MAX_VALUE;

        double[][] dtw = new double[n + 1][m + 1];

        // 初始化
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                dtw[i][j] = Double.MAX_VALUE;
            }
        }
        dtw[0][0] = 0;

        // 动态规划计算
        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                double cost = Math.abs(s1.get(i - 1) - s2.get(j - 1));
                dtw[i][j] = cost + Math.min(dtw[i - 1][j],      // 插入
                                   Math.min(dtw[i][j - 1],      // 删除
                                            dtw[i - 1][j - 1])); // 匹配
            }
        }

        return dtw[n][m];
    }
}
