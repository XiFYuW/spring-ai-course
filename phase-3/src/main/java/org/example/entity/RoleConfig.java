package org.example.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * 角色配置实体类
 * 
 * 支持动态配置Spring AI的四种角色类型：
 * - SYSTEM: 系统角色，定义AI的行为准则
 * - USER: 用户角色，代表用户输入
 * - ASSISTANT: 助手角色，AI的响应
 * - TOOL: 工具角色，函数调用响应
 * 
 * 每个角色可以配置多个模板，支持不同的场景和用途
 */
@Table("role_config")
public class RoleConfig {

    @Id
    private Long id;

    /**
     * 角色类型
     * 对应Spring AI的MessageType枚举值
     */
    @Column("role_type")
    private String roleType;

    /**
     * 角色名称（用于显示）
     */
    @Column("role_name")
    private String roleName;

    /**
     * 角色描述
     */
    @Column("description")
    private String description;

    /**
     * 角色模板内容
     * 支持变量替换，使用 {variable} 语法
     */
    @Column("template_content")
    private String templateContent;

    /**
     * 是否启用该角色配置
     */
    @Column("enabled")
    private Boolean enabled;

    /**
     * 优先级（数值越小优先级越高）
     * 用于同一角色类型多个配置时的排序
     */
    @Column("priority")
    private Integer priority;

    /**
     * 适用场景标签
     * 用于分类和筛选，多个标签用逗号分隔
     */
    @Column("tags")
    private String tags;

    /**
     * 创建时间
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 创建新的角色配置
     * 
     * @param roleType 角色类型
     * @param roleName 角色名称
     * @param description 角色描述
     * @param templateContent 模板内容
     * @param enabled 是否启用
     * @param priority 优先级
     * @param tags 适用场景标签
     * @return 角色配置实例
     */
    public static RoleConfig create(
            String roleType,
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {
        RoleConfig config = new RoleConfig();
        config.roleType = roleType;
        config.roleName = roleName;
        config.description = description;
        config.templateContent = templateContent;
        config.enabled = enabled != null ? enabled : true;
        config.priority = priority != null ? priority : 10;
        config.tags = tags;
        config.createdAt = LocalDateTime.now();
        config.updatedAt = LocalDateTime.now();
        return config;
    }

    /**
     * 更新角色配置
     * 
     * @param roleName 角色名称
     * @param description 角色描述
     * @param templateContent 模板内容
     * @param enabled 是否启用
     * @param priority 优先级
     * @param tags 适用场景标签
     * @return 更新后的角色配置
     */
    public RoleConfig update(
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {
        if (roleName != null) this.roleName = roleName;
        if (description != null) this.description = description;
        if (templateContent != null) this.templateContent = templateContent;
        if (enabled != null) this.enabled = enabled;
        if (priority != null) this.priority = priority;
        if (tags != null) this.tags = tags;
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    // Getter和Setter方法 - 标准JavaBean格式，用于JSON序列化
    public Long getId() {
        return id;
    }

    public String getRoleType() {
        return roleType;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getDescription() {
        return description;
    }

    public String getTemplateContent() {
        return templateContent;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Integer getPriority() {
        return priority;
    }

    public String getTags() {
        return tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 保持原有的记录风格getter方法，供内部使用
    public Long id() {
        return id;
    }

    public String roleType() {
        return roleType;
    }

    public String roleName() {
        return roleName;
    }

    public String description() {
        return description;
    }

    public String templateContent() {
        return templateContent;
    }

    public Boolean enabled() {
        return enabled;
    }

    public Integer priority() {
        return priority;
    }

    public String tags() {
        return tags;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public LocalDateTime updatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return "RoleConfig{" +
                "id=" + id +
                ", roleType='" + roleType + '\'' +
                ", roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                ", templateContent='" + templateContent + '\'' +
                ", enabled=" + enabled +
                ", priority=" + priority +
                ", tags='" + tags + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}