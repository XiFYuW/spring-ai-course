package org.example.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 计算器工具类
 * 演示数学计算类工具
 */
@Component
public class CalculatorTools {

    private static final int DEFAULT_SCALE = 10;

    /**
     * 基础加法运算
     */
    @Tool(description = "计算两个数字的和")
    public double add(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b) {
        double result = a + b;
        System.out.printf("[工具调用] add(%.2f, %.2f) = %.2f%n", a, b, result);
        return result;
    }

    /**
     * 基础减法运算
     */
    @Tool(description = "计算两个数字的差（第一个数减去第二个数）")
    public double subtract(
            @ToolParam(description = "被减数") double a,
            @ToolParam(description = "减数") double b) {
        double result = a - b;
        System.out.printf("[工具调用] subtract(%.2f, %.2f) = %.2f%n", a, b, result);
        return result;
    }

    /**
     * 基础乘法运算
     */
    @Tool(description = "计算两个数字的乘积")
    public double multiply(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b) {
        double result = a * b;
        System.out.printf("[工具调用] multiply(%.2f, %.2f) = %.2f%n", a, b, result);
        return result;
    }

    /**
     * 基础除法运算
     */
    @Tool(description = "计算两个数字的商（第一个数除以第二个数）")
    public double divide(
            @ToolParam(description = "被除数") double a,
            @ToolParam(description = "除数") double b) {
        if (b == 0) {
            System.err.println("[工具调用错误] 除数不能为 0");
            throw new ArithmeticException("除数不能为 0");
        }
        double result = a / b;
        System.out.printf("[工具调用] divide(%.2f, %.2f) = %.4f%n", a, b, result);
        return result;
    }

    /**
     * 高精度加法（使用 BigDecimal）
     */
    @Tool(description = "高精度计算两个数字的和，适用于金融计算")
    public String addPrecise(
            @ToolParam(description = "第一个数字（字符串格式）") String a,
            @ToolParam(description = "第二个数字（字符串格式）") String b,
            @ToolParam(description = "小数位数", required = false) Integer scale) {
        
        try {
            BigDecimal num1 = new BigDecimal(a);
            BigDecimal num2 = new BigDecimal(b);
            int precision = scale != null ? scale : 2;
            
            BigDecimal result = num1.add(num2).setScale(precision, RoundingMode.HALF_UP);
            String resultStr = result.toPlainString();
            
            System.out.printf("[工具调用] addPrecise(%s, %s) = %s%n", a, b, resultStr);
            return resultStr;
        } catch (NumberFormatException e) {
            String error = "错误：数字格式不正确 - " + e.getMessage();
            System.err.println("[工具调用错误] " + error);
            return error;
        }
    }

    /**
     * 计算幂运算
     */
    @Tool(description = "计算一个数的幂次方")
    public double power(
            @ToolParam(description = "底数") double base,
            @ToolParam(description = "指数") double exponent) {
        double result = Math.pow(base, exponent);
        System.out.printf("[工具调用] power(%.2f, %.2f) = %.4f%n", base, exponent, result);
        return result;
    }

    /**
     * 计算平方根
     */
    @Tool(description = "计算一个数的平方根")
    public double sqrt(
            @ToolParam(description = "要计算平方根的数字（必须大于等于0）") double number) {
        if (number < 0) {
            System.err.println("[工具调用错误] 不能计算负数的平方根");
            throw new IllegalArgumentException("不能计算负数的平方根");
        }
        double result = Math.sqrt(number);
        System.out.printf("[工具调用] sqrt(%.2f) = %.4f%n", number, result);
        return result;
    }

    /**
     * 计算一组数字的统计信息
     */
    @Tool(description = "计算一组数字的统计信息（总和、平均值、最大值、最小值）")
    public String calculateStatistics(
            @ToolParam(description = "数字列表，用逗号分隔") String numbers) {
        
        try {
            String[] parts = numbers.split(",");
            List<Double> values = new ArrayList<>();
            
            for (String part : parts) {
                values.add(Double.parseDouble(part.trim()));
            }
            
            if (values.isEmpty()) {
                return "错误：没有提供有效的数字";
            }
            
            double sum = 0;
            double max = values.get(0);
            double min = values.get(0);
            
            for (double value : values) {
                sum += value;
                if (value > max) max = value;
                if (value < min) min = value;
            }
            
            double average = sum / values.size();
            
            // 计算标准差
            double varianceSum = 0;
            for (double value : values) {
                varianceSum += Math.pow(value - average, 2);
            }
            double stdDev = Math.sqrt(varianceSum / values.size());
            
            String result = String.format(
                "📊 统计结果（共 %d 个数字）：\n" +
                "   总和: %.4f\n" +
                "   平均值: %.4f\n" +
                "   最大值: %.4f\n" +
                "   最小值: %.4f\n" +
                "   标准差: %.4f",
                values.size(), sum, average, max, min, stdDev
            );
            
            System.out.println("[工具调用] calculateStatistics() -> 统计了 " + values.size() + " 个数字");
            return result;
            
        } catch (NumberFormatException e) {
            String error = "错误：请提供有效的数字列表，用逗号分隔";
            System.err.println("[工具调用错误] " + error);
            return error;
        }
    }

    /**
     * 计算复利
     */
    @Tool(description = "计算复利投资的最终金额")
    public String calculateCompoundInterest(
            @ToolParam(description = "本金") double principal,
            @ToolParam(description = "年利率（百分比，如 5 表示 5%）") double annualRate,
            @ToolParam(description = "投资年数") int years,
            @ToolParam(description = "每年复利次数（如 12 表示每月复利）", required = false) Integer timesPerYear) {
        
        int n = timesPerYear != null ? timesPerYear : 12;
        double r = annualRate / 100.0;
        
        // 复利公式: A = P * (1 + r/n)^(n*t)
        double amount = principal * Math.pow(1 + r / n, n * years);
        double interest = amount - principal;
        
        String result = String.format(
            "💰 复利计算结果：\n" +
            "   本金: %.2f\n" +
            "   年利率: %.2f%%\n" +
            "   投资期限: %d 年\n" +
            "   复利频率: 每年 %d 次\n" +
            "   最终金额: %.2f\n" +
            "   利息收入: %.2f",
            principal, annualRate, years, n, amount, interest
        );
        
        System.out.println("[工具调用] calculateCompoundInterest() -> 最终金额: " + String.format("%.2f", amount));
        return result;
    }

    /**
     * 单位转换 - 温度
     */
    @Tool(description = "温度单位转换（摄氏度、华氏度、开尔文）")
    public String convertTemperature(
            @ToolParam(description = "温度值") double value,
            @ToolParam(description = "原始单位（C/F/K）") String fromUnit,
            @ToolParam(description = "目标单位（C/F/K）") String toUnit) {
        
        double celsius;
        
        // 先转换为摄氏度
        switch (fromUnit.toUpperCase()) {
            case "C":
                celsius = value;
                break;
            case "F":
                celsius = (value - 32) * 5 / 9;
                break;
            case "K":
                celsius = value - 273.15;
                break;
            default:
                return "错误：原始单位必须是 C（摄氏度）、F（华氏度）或 K（开尔文）";
        }
        
        // 再转换为目标单位
        double result;
        String unitName;
        switch (toUnit.toUpperCase()) {
            case "C":
                result = celsius;
                unitName = "摄氏度";
                break;
            case "F":
                result = celsius * 9 / 5 + 32;
                unitName = "华氏度";
                break;
            case "K":
                result = celsius + 273.15;
                unitName = "开尔文";
                break;
            default:
                return "错误：目标单位必须是 C（摄氏度）、F（华氏度）或 K（开尔文）";
        }
        
        String resultStr = String.format("%.2f %s = %.2f %s", 
            value, fromUnit.toUpperCase(), result, unitName);
        System.out.println("[工具调用] convertTemperature() -> " + resultStr);
        return resultStr;
    }
}
