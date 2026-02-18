package org.example.entity;

/**
 * 产品信息实体类 - 用于演示 BeanOutputConverter
 * 
 * 结构化输出转换器可以将 AI 的 JSON 响应自动映射到此 Java 类
 * 
 * @author Spring AI Course
 */
public class ProductInfo {

    /**
     * 产品名称
     */
    private String name;

    /**
     * 产品描述
     */
    private String description;

    /**
     * 产品价格
     */
    private double price;

    /**
     * 产品类别
     */
    private String category;

    /**
     * 产品库存数量
     */
    private int stock;

    public ProductInfo() {
    }

    public ProductInfo(String name, String description, double price, String category, int stock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.stock = stock;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    @Override
    public String toString() {
        return "ProductInfo{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", stock=" + stock +
                '}';
    }
}
