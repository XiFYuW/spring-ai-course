package org.example.repository;

import org.example.entity.RoleConfig;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 角色配置Repository接口
 * 
 * 提供角色配置的CRUD操作和查询功能
 */
public interface RoleConfigRepository extends ReactiveCrudRepository<RoleConfig, Long> {

    /**
     * 根据角色类型查找启用的配置（按优先级排序）
     * 
     * @param roleType 角色类型
     * @return 启用的角色配置列表
     */
    @Query("SELECT * FROM role_config WHERE role_type = :roleType AND enabled = true ORDER BY priority ASC")
    Flux<RoleConfig> findByRoleTypeAndEnabledTrueOrderByPriorityAsc(String roleType);

    /**
     * 根据角色类型查找所有配置（按优先级排序）
     * 
     * @param roleType 角色类型
     * @return 角色配置列表
     */
    @Query("SELECT * FROM role_config WHERE role_type = :roleType ORDER BY priority ASC")
    Flux<RoleConfig> findByRoleTypeOrderByPriorityAsc(String roleType);

    /**
     * 查找所有启用的角色配置（按角色类型和优先级排序）
     * 
     * @return 启用的角色配置列表
     */
    @Query("SELECT * FROM role_config WHERE enabled = true ORDER BY role_type ASC, priority ASC")
    Flux<RoleConfig> findAllEnabledOrderByRoleTypeAndPriority();

    /**
     * 根据标签查找启用的角色配置
     * 
     * @param tag 标签
     * @return 匹配标签的角色配置列表
     */
    @Query("SELECT * FROM role_config WHERE enabled = true AND tags LIKE CONCAT('%', :tag, '%') ORDER BY role_type ASC, priority ASC")
    Flux<RoleConfig> findByTagAndEnabledTrue(String tag);

    /**
     * 检查指定角色类型是否存在启用的配置
     * 
     * @param roleType 角色类型
     * @return 是否存在启用的配置
     */
    @Query("SELECT COUNT(*) > 0 FROM role_config WHERE role_type = :roleType AND enabled = true")
    Mono<Boolean> existsByRoleTypeAndEnabledTrue(String roleType);

    /**
     * 根据角色名称查找配置
     * 
     * @param roleName 角色名称
     * @return 匹配的角色配置
     */
    Mono<RoleConfig> findByRoleName(String roleName);

    /**
     * 根据角色类型和名称查找配置
     * 
     * @param roleType 角色类型
     * @param roleName 角色名称
     * @return 匹配的角色配置
     */
    Mono<RoleConfig> findByRoleTypeAndRoleName(String roleType, String roleName);
}