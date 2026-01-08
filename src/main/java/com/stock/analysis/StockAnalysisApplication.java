package com.stock.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class StockAnalysisApplication {

    public static void main(String[] args) {
        // 环境自检
        performEnvironmentCheck();
        
        // 启动Spring Boot应用
        SpringApplication.run(StockAnalysisApplication.class, args);
    }

    /**
     * 执行环境自检
     */
    private static void performEnvironmentCheck() {
        log.info("===== 环境自检开始 =====");
        
        // 检测Python环境
        checkPythonEnvironment();
        
        // 检测Redis端口
        checkRedisConnection();
        
        log.info("===== 环境自检完成 =====");
    }

    /**
     * 检测Python环境
     */
    private static void checkPythonEnvironment() {
        log.info("检测Python环境...");
        
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "--version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // 读取Python版本信息
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String version = reader.readLine();
                    log.info("✅ Python环境正常: {}", version);
                }
            } else {
                // 尝试使用python命令
                pb = new ProcessBuilder("python", "--version");
                process = pb.start();
                exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    // 读取Python版本信息
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String version = reader.readLine();
                        log.info("✅ Python环境正常: {}", version);
                    }
                } else {
                    log.warn("⚠️ 未检测到Python环境，请确保已安装Python 3.x");
                    log.warn("   部分依赖Python的功能（如机器学习模型）可能无法正常工作");
                }
            }
        } catch (IOException e) {
            log.warn("⚠️ 未检测到Python环境，请确保已安装Python 3.x");
            log.warn("   部分依赖Python的功能（如机器学习模型）可能无法正常工作");
        } catch (InterruptedException e) {
            log.error("Python环境检测中断", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 检测Redis连接
     */
    private static void checkRedisConnection() {
        log.info("检测Redis连接...");
        
        String host = "localhost";
        int port = 6379;
        int timeout = 2000;
        
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            log.info("✅ Redis连接正常: {}:{}", host, port);
        } catch (IOException e) {
            log.warn("⚠️ Redis连接失败: {}:{}", host, port);
            log.warn("   Redis缓存功能将不可用，系统将使用内存缓存替代");
            log.warn("   建议启动Redis服务或检查Redis配置");
        }
    }

}
