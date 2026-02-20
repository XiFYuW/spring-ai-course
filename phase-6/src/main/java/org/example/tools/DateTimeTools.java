package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 日期时间工具类
 * 演示信息检索类工具 - 获取当前日期时间相关信息
 */
@Component
public class DateTimeTools {

    /**
     * 获取当前日期和时间（用户时区）
     * 这是一个信息检索工具，用于获取实时时间信息
     */
    @Tool(description = "获取用户所在时区的当前日期和时间，格式为 ISO-8601")
    public String getCurrentDateTime() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        String result = now.format(DateTimeFormatter.ISO_DATE_TIME);
        System.out.println("[工具调用] getCurrentDateTime() = " + result);
        return result;
    }

    /**
     * 获取当前日期
     */
    @Tool(description = "获取用户所在时区的当前日期，格式为 yyyy-MM-dd")
    public String getCurrentDate() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        String result = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        System.out.println("[工具调用] getCurrentDate() = " + result);
        return result;
    }

    /**
     * 获取当前时间
     */
    @Tool(description = "获取用户所在时区的当前时间，格式为 HH:mm:ss")
    public String getCurrentTime() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        String result = now.format(DateTimeFormatter.ISO_LOCAL_TIME);
        System.out.println("[工具调用] getCurrentTime() = " + result);
        return result;
    }

    /**
     * 获取当前星期几
     */
    @Tool(description = "获取今天是星期几（中文）")
    public String getDayOfWeek() {
        ZoneId zoneId = LocaleContextHolder.getTimeZone().toZoneId();
        LocalDateTime now = LocalDateTime.now(zoneId);
        String[] days = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        // DayOfWeek 返回 1-7 (周一到周日)
        int dayIndex = now.getDayOfWeek().getValue() - 1;
        String result = days[dayIndex];
        System.out.println("[工具调用] getDayOfWeek() = " + result);
        return result;
    }

    /**
     * 计算两个日期之间的天数差
     */
    @Tool(description = "计算两个日期之间的天数差，日期格式为 yyyy-MM-dd")
    public long calculateDaysBetween(
            @ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endDate + "T00:00:00");
            long days = java.time.Duration.between(start, end).toDays();
            System.out.println("[工具调用] calculateDaysBetween(" + startDate + ", " + endDate + ") = " + days);
            return days;
        } catch (DateTimeParseException e) {
            System.err.println("[工具调用错误] 日期解析失败: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 格式化日期时间
     */
    @Tool(description = "将 ISO-8601 格式的时间转换为更友好的中文格式")
    public String formatDateTime(
            @ToolParam(description = "ISO-8601 格式的时间字符串") String isoDateTime) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时mm分ss秒");
            String result = dateTime.format(formatter);
            System.out.println("[工具调用] formatDateTime(" + isoDateTime + ") = " + result);
            return result;
        } catch (DateTimeParseException e) {
            System.err.println("[工具调用错误] 时间解析失败: " + e.getMessage());
            return "时间格式错误，请使用 ISO-8601 格式";
        }
    }
}
