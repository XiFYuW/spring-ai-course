package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 闹钟和提醒工具类
 * 演示执行操作类工具 - 在系统中执行具体操作
 */
@Component
public class AlarmTools {

    // 使用线程池来管理闹钟任务
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    
    // 存储已设置的闹钟
    private final Map<String, AlarmInfo> alarms = new ConcurrentHashMap<>();
    
    // 闹钟ID计数器
    private int alarmCounter = 0;

    /**
     * 闹钟信息内部类
     */
    private static class AlarmInfo {
        final String id;
        final String time;
        final String message;
        final boolean isRecurring;
        final long createdAt;

        AlarmInfo(String id, String time, String message, boolean isRecurring) {
            this.id = id;
            this.time = time;
            this.message = message;
            this.isRecurring = isRecurring;
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * 在指定时间设置闹钟
     * 这是一个执行操作工具，会在系统中实际设置一个提醒
     */
    @Tool(description = "在指定时间设置闹钟提醒，时间格式为 ISO-8601 (yyyy-MM-ddTHH:mm:ss)")
    public String setAlarm(
            @ToolParam(description = "闹钟时间，ISO-8601 格式，例如 2024-12-25T08:00:00") String time,
            @ToolParam(description = "提醒消息内容") String message,
            @ToolParam(description = "是否重复提醒", required = false) Boolean isRecurring) {
        
        try {
            LocalDateTime alarmTime = LocalDateTime.parse(time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime now = LocalDateTime.now();
            
            // 检查时间是否已经过去
            if (alarmTime.isBefore(now)) {
                return "错误：设置的时间 " + time + " 已经过去，请设置未来的时间";
            }
            
            // 生成闹钟ID
            String alarmId = "ALARM_" + (++alarmCounter);
            boolean recurring = isRecurring != null && isRecurring;
            
            // 计算延迟时间（秒）
            long delaySeconds = java.time.Duration.between(now, alarmTime).getSeconds();
            
            // 创建闹钟信息
            AlarmInfo alarmInfo = new AlarmInfo(alarmId, time, message, recurring);
            alarms.put(alarmId, alarmInfo);
            
            // 调度闹钟任务
            scheduler.schedule(() -> {
                triggerAlarm(alarmId, message);
            }, delaySeconds, TimeUnit.SECONDS);
            
            String result = String.format(
                "✅ 闹钟设置成功！\n" +
                "ID: %s\n" +
                "时间: %s\n" +
                "消息: %s\n" +
                "重复: %s\n" +
                "将在 %d 秒后触发",
                alarmId, time, message, recurring ? "是" : "否", delaySeconds
            );
            
            System.out.println("[工具调用] setAlarm() -> " + result);
            return result;
            
        } catch (DateTimeParseException e) {
            String error = "错误：时间格式不正确。请使用 ISO-8601 格式，例如 2024-12-25T08:00:00";
            System.err.println("[工具调用错误] " + error);
            return error;
        }
    }

    /**
     * 设置相对时间闹钟（从现在起多少分钟后）
     */
    @Tool(description = "设置一个相对时间的闹钟，从现在开始计算多少分钟后提醒")
    public String setAlarmInMinutes(
            @ToolParam(description = "从现在开始多少分钟后提醒") int minutes,
            @ToolParam(description = "提醒消息内容") String message) {
        
        if (minutes <= 0) {
            return "错误：分钟数必须大于 0";
        }
        
        LocalDateTime alarmTime = LocalDateTime.now().plusMinutes(minutes);
        String timeStr = alarmTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        return setAlarm(timeStr, message, false);
    }

    /**
     * 取消指定闹钟
     */
    @Tool(description = "根据闹钟ID取消已设置的闹钟")
    public String cancelAlarm(
            @ToolParam(description = "要取消的闹钟ID") String alarmId) {
        
        AlarmInfo alarm = alarms.remove(alarmId);
        if (alarm != null) {
            String result = String.format("✅ 闹钟 %s 已取消（原定于 %s，消息：%s）", 
                alarmId, alarm.time, alarm.message);
            System.out.println("[工具调用] cancelAlarm() -> " + result);
            return result;
        } else {
            String result = "❌ 未找到ID为 " + alarmId + " 的闹钟";
            System.out.println("[工具调用] cancelAlarm() -> " + result);
            return result;
        }
    }

    /**
     * 列出所有已设置的闹钟
     */
    @Tool(description = "获取所有已设置的闹钟列表")
    public String listAlarms() {
        if (alarms.isEmpty()) {
            return "当前没有设置任何闹钟";
        }
        
        StringBuilder sb = new StringBuilder("📋 已设置的闹钟列表：\n\n");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm");
        
        for (AlarmInfo alarm : alarms.values()) {
            LocalDateTime alarmTime = LocalDateTime.parse(alarm.time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            sb.append(String.format(
                "🔔 ID: %s\n" +
                "   时间: %s\n" +
                "   消息: %s\n" +
                "   重复: %s\n\n",
                alarm.id,
                alarmTime.format(displayFormatter),
                alarm.message,
                alarm.isRecurring ? "是" : "否"
            ));
        }
        
        String result = sb.toString();
        System.out.println("[工具调用] listAlarms() -> 找到 " + alarms.size() + " 个闹钟");
        return result;
    }

    /**
     * 清除所有闹钟
     */
    @Tool(description = "清除所有已设置的闹钟")
    public String clearAllAlarms() {
        int count = alarms.size();
        alarms.clear();
        String result = String.format("✅ 已清除所有闹钟（共 %d 个）", count);
        System.out.println("[工具调用] clearAllAlarms() -> " + result);
        return result;
    }

    /**
     * 触发闹钟（内部方法）
     */
    private void triggerAlarm(String alarmId, String message) {
        AlarmInfo alarm = alarms.get(alarmId);
        if (alarm != null && !alarm.isRecurring) {
            alarms.remove(alarmId);
        }
        
        // 在实际应用中，这里可以发送通知、邮件、推送等
        String notification = String.format(
            "\n" +
            "╔══════════════════════════════════════╗\n" +
            "║           ⏰ 闹钟提醒 ⏰              ║\n" +
            "╠══════════════════════════════════════╣\n" +
            "║  ID: %-30s  ║\n" +
            "║  消息: %-28s  ║\n" +
            "║  时间: %-28s  ║\n" +
            "╚══════════════════════════════════════╝\n",
            alarmId, message, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
        
        System.out.println(notification);
    }

    /**
     * 关闭调度器（应用关闭时调用）
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
