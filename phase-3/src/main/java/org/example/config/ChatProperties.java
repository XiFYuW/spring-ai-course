package org.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 聊天服务配置属性
 * 
 * 配置项前缀: app.chat
 * 可在 application.yml 中配置：
 * app:
 *   chat:
 *     system-prompt: "自定义系统提示词"
 *     max-context-pairs: 20
 */
@Configuration
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    /**
     * 系统提示词 - 定义AI助手的行为准则
     */
    private String systemPrompt = "你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。";

    /**
     * 最大保留的对话轮数（一对 = user + assistant）
     * 默认20对 = 40条消息，加上系统消息共41条
     */
    private int maxContextPairs = 20;

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public int getMaxContextPairs() {
        return maxContextPairs;
    }

    public void setMaxContextPairs(int maxContextPairs) {
        this.maxContextPairs = maxContextPairs;
    }

    /**
     * 获取最大保留的消息条数（不含系统消息）
     * 
     * @return 最大消息条数
     */
    public int getMaxContextMessages() {
        return maxContextPairs * 2;
    }
}
