package org.example.controller;

import org.example.entity.RoleConfig;
import org.example.service.RoleConfigService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 角色配置控制器 - RESTful API
 * 
 * 提供角色配置的CRUD操作和管理功能，支持动态配置Spring AI的四种角色类型：
 * - SYSTEM: 系统角色，定义AI的行为准则
 * - USER: 用户角色，代表用户输入
 * - ASSISTANT: 助手角色，AI的响应
 * - TOOL: 工具角色，函数调用响应
 */
@RestController
@RequestMapping("/api/role-configs")
public class RoleConfigController {

    private final RoleConfigService roleConfigService;

    public RoleConfigController(RoleConfigService roleConfigService) {
        this.roleConfigService = roleConfigService;
    }

    /**
     * 创建新的角色配置
     * 
     * @param request 创建角色配置请求
     * @return 创建的角色配置
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RoleConfig> createRoleConfig(@RequestBody CreateRoleConfigRequest request) {
        return roleConfigService.createRoleConfig(
                request.roleType(),
                request.roleName(),
                request.description(),
                request.templateContent(),
                request.enabled(),
                request.priority(),
                request.tags()
        );
    }

    /**
     * 更新角色配置
     * 
     * @param id 配置ID
     * @param request 更新角色配置请求
     * @return 更新后的角色配置
     */
    @PutMapping("/{id}")
    public Mono<RoleConfig> updateRoleConfig(
            @PathVariable Long id,
            @RequestBody UpdateRoleConfigRequest request
    ) {
        return roleConfigService.updateRoleConfig(
                id,
                request.roleName(),
                request.description(),
                request.templateContent(),
                request.enabled(),
                request.priority(),
                request.tags()
        );
    }

    /**
     * 删除角色配置
     * 
     * @param id 配置ID
     * @return 删除操作完成信号
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteRoleConfig(@PathVariable Long id) {
        return roleConfigService.deleteRoleConfig(id);
    }

    /**
     * 根据ID获取角色配置
     * 
     * @param id 配置ID
     * @return 角色配置
     */
    @GetMapping("/{id}")
    public Mono<RoleConfig> getRoleConfigById(@PathVariable Long id) {
        return roleConfigService.getRoleConfigById(id);
    }

    /**
     * 获取所有角色配置
     * 
     * @return 所有角色配置列表
     */
    @GetMapping
    public Flux<RoleConfig> getAllRoleConfigs() {
        return roleConfigService.getAllRoleConfigs();
    }

    /**
     * 获取所有启用的角色配置
     * 
     * @return 启用的角色配置列表
     */
    @GetMapping("/enabled")
    public Flux<RoleConfig> getAllEnabledRoleConfigs() {
        return roleConfigService.getAllEnabledRoleConfigs();
    }

    /**
     * 根据角色类型获取启用的配置
     * 
     * @param roleType 角色类型
     * @return 启用的角色配置列表
     */
    @GetMapping("/type/{roleType}")
    public Flux<RoleConfig> getEnabledRoleConfigsByType(@PathVariable String roleType) {
        return roleConfigService.getEnabledRoleConfigsByType(roleType);
    }

    /**
     * 渲染角色模板
     * 
     * @param id 配置ID
     * @param variables 模板变量
     * @return 渲染后的内容
     */
    @PostMapping("/{id}/render")
    public Mono<String> renderRoleTemplate(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> variables
    ) {
        return roleConfigService.getRoleConfigById(id)
                .flatMap(roleConfig -> roleConfigService.renderRoleTemplate(roleConfig, variables));
    }

    /**
     * 创建角色配置请求
     * 
     * @param roleType 角色类型（system/user/assistant/tool）
     * @param roleName 角色名称（唯一）
     * @param description 角色描述
     * @param templateContent 模板内容
     * @param enabled 是否启用
     * @param priority 优先级
     * @param tags 适用场景标签
     */
    public record CreateRoleConfigRequest(
            String roleType,
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {}

    /**
     * 更新角色配置请求
     * 
     * @param roleName 角色名称
     * @param description 角色描述
     * @param templateContent 模板内容
     * @param enabled 是否启用
     * @param priority 优先级
     * @param tags 适用场景标签
     */
    public record UpdateRoleConfigRequest(
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {}
}