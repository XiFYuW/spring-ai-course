package org.example.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * MCP 服务器启动类
 */
@SpringBootApplication
public class McpServerApplication {

    static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
        
        System.out.println("========================================");
        System.out.println("MCP 服务器已启动！");
    }
}
