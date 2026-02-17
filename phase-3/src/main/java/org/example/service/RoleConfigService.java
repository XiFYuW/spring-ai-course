package org.example.service;

import org.example.entity.RoleConfig;
import org.example.repository.RoleConfigRepository;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色配置服务类
 * 
 * 提供角色配置的CRUD操作、模板渲染和动态角色管理功能
 */
@Service
public class RoleConfigService {

    private final RoleConfigRepository roleConfigRepository;

    public RoleConfigService(RoleConfigRepository roleConfigRepository) {
        this.roleConfigRepository = roleConfigRepository;
    }

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
     * @return 创建的角色配置
     */
    public Mono<RoleConfig> createRoleConfig(
            String roleType,
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {
        // 验证角色类型是否有效
        if (!isValidRoleType(roleType)) {
            StringBuilder validTypes = new StringBuilder();
            for (MessageType messageType : MessageType.values()) {
                if (validTypes.length() > 0) {
                    validTypes.append(", ");
                }
                validTypes.append(messageType.getValue());
            }
            return Mono.error(new IllegalArgumentException("无效的角色类型: " + roleType + 
                    ", 有效类型为: " + validTypes.toString()));
        }

        // 检查角色名称是否已存在
        return roleConfigRepository.findByRoleName(roleName)
                .flatMap(existing -> Mono.error(new IllegalArgumentException("角色名称已存在: " + roleName)))
                .switchIfEmpty(Mono.defer(() -> {
                    RoleConfig config = RoleConfig.create(
                            roleType, roleName, description, templateContent, enabled, priority, tags
                    );
                    return roleConfigRepository.save(config);
                }))
                .cast(RoleConfig.class);
    }

    /**
     * 更新角色配置
     * 
     * @param id 配置ID
     * @param roleName 角色名称
     * @param description 角色描述
     * @param templateContent 模板内容
     * @param enabled 是否启用
     * @param priority 优先级
     * @param tags 适用场景标签
     * @return 更新后的角色配置
     */
    public Mono<RoleConfig> updateRoleConfig(
            Long id,
            String roleName,
            String description,
            String templateContent,
            Boolean enabled,
            Integer priority,
            String tags
    ) {
        return roleConfigRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("角色配置不存在: " + id)))
                .flatMap(config -> {
                    // 如果修改了角色名称，检查是否与其他配置冲突
                    if (roleName != null && !roleName.equals(config.roleName())) {
                        return roleConfigRepository.findByRoleName(roleName)
                                .flatMap(existing -> 
                                        Mono.error(new IllegalArgumentException("角色名称已存在: " + roleName))
                                )
                                .then(Mono.just(config));
                    }
                    return Mono.just(config);
                })
                .flatMap(config -> {
                    RoleConfig updated = config.update(roleName, description, templateContent, enabled, priority, tags);
                    return roleConfigRepository.save(updated);
                });
    }

    /**
     * 删除角色配置
     * 
     * @param id 配置ID
     * @return 删除操作完成信号
     */
    public Mono<Void> deleteRoleConfig(Long id) {
        return roleConfigRepository.existsById(id)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.error(new IllegalArgumentException("角色配置不存在: " + id));
                    }
                    return roleConfigRepository.deleteById(id);
                });
    }

    /**
     * 根据ID获取角色配置
     * 
     * @param id 配置ID
     * @return 角色配置
     */
    public Mono<RoleConfig> getRoleConfigById(Long id) {
        return roleConfigRepository.findById(id)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("角色配置不存在: " + id)));
    }

    /**
     * 获取所有角色配置
     * 
     * @return 所有角色配置列表
     */
    public Flux<RoleConfig> getAllRoleConfigs() {
        return roleConfigRepository.findAll();
    }

    /**
     * 获取所有启用的角色配置
     * 
     * @return 启用的角色配置列表
     */
    public Flux<RoleConfig> getAllEnabledRoleConfigs() {
        return roleConfigRepository.findAllEnabledOrderByRoleTypeAndPriority();
    }

    /**
     * 根据角色类型获取启用的配置
     * 
     * @param roleType 角色类型
     * @return 启用的角色配置列表
     */
    public Flux<RoleConfig> getEnabledRoleConfigsByType(String roleType) {
        if (!isValidRoleType(roleType)) {
            return Flux.error(new IllegalArgumentException("无效的角色类型: " + roleType));
        }
        return roleConfigRepository.findByRoleTypeAndEnabledTrueOrderByPriorityAsc(roleType);
    }

    /**
     * 根据角色名称获取配置
     * 
     * @param roleName 角色名称
     * @return 角色配置
     */
    public Mono<RoleConfig> getRoleConfigByRoleName(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("角色名称不能为空"));
        }
        return roleConfigRepository.findByRoleName(roleName)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("角色配置不存在: " + roleName)));
    }

    /**
     * 渲染角色模板
     * 
     * @param roleConfig 角色配置
     * @param variables 模板变量
     * @return 渲染后的内容
     */
    public Mono<String> renderRoleTemplate(RoleConfig roleConfig, Map<String, Object> variables) {
        return Mono.fromCallable(() -> {
            if (roleConfig.templateContent() == null || roleConfig.templateContent().trim().isEmpty()) {
                return "";
            }

            Map<String, Object> templateVariables = variables != null ? new HashMap<>(variables) : new HashMap<>();
            
            // 添加默认变量（使用putIfAbsent避免覆盖调用方传递的值）
            templateVariables.putIfAbsent("roleName", roleConfig.roleName());
            templateVariables.putIfAbsent("roleDescription", roleConfig.description());
            templateVariables.putIfAbsent("roleType", roleConfig.roleType());
            
            PromptTemplate promptTemplate = PromptTemplate.builder()
                    .template(roleConfig.templateContent())
                    .build();
            
            return promptTemplate.render(templateVariables);
        });
    }

    /**
     * 批量渲染角色模板
     * 
     * @param roleConfigs 角色配置列表
     * @param variables 模板变量
     * @return 渲染后的内容列表
     */
    public Flux<String> renderRoleTemplates(List<RoleConfig> roleConfigs, Map<String, Object> variables) {
        return Flux.fromIterable(roleConfigs)
                .flatMap(config -> renderRoleTemplate(config, variables));
    }
    
    /**
     * 验证角色类型是否有效
     * 
     * @param roleType 角色类型
     * @return 是否有效
     */
    private boolean isValidRoleType(String roleType) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.getValue().equals(roleType)) {
                return true;
            }
        }
        return false;
    }
}