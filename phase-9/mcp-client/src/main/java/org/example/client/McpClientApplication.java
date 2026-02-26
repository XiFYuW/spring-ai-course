package org.example.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MCP 客户端启动类
 * 连接到 MCP 服务器
 */
@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpClientApplication.class, args);
        System.out.println("========================================");
        System.out.println("MCP 客户端已启动！");
        System.out.println("API 地址: http://localhost:8081");
    }
}
