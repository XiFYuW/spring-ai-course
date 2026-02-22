package org.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP 服务器启动类
 * 提供天气相关的工具服务
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
        System.out.println("========================================");
        System.out.println("MCP 服务器已启动！");
        System.out.println("SSE 端点: http://localhost:8080/sse");
        System.out.println("========================================");
    }
}
