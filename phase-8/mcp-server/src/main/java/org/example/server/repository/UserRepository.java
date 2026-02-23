package org.example.server.repository;

import org.example.server.entity.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 用户数据访问层接口
 * 使用R2DBC响应式编程操作数据库
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    /**
     * 根据用户名查询用户
     */
    Mono<User> findByUsername(String username);

    /**
     * 根据邮箱查询用户
     */
    Mono<User> findByEmail(String email);

    /**
     * 根据状态查询用户列表
     */
    Flux<User> findByStatus(String status);

    /**
     * 根据年龄范围查询用户
     */
    @Query("SELECT * FROM users WHERE age >= :minAge AND age <= :maxAge")
    Flux<User> findByAgeRange(Integer minAge, Integer maxAge);

    /**
     * 模糊查询用户名
     */
    @Query("SELECT * FROM users WHERE username LIKE '%' || :keyword || '%'")
    Flux<User> findByUsernameContaining(String keyword);

    /**
     * 统计用户总数
     */
    @Query("SELECT COUNT(*) FROM users")
    Mono<Long> countAll();

    /**
     * 根据ID更新用户信息
     */
    @Query("UPDATE users SET username = :username, email = :email, phone = :phone, " +
            "age = :age, status = :status, updated_at = NOW() WHERE id = :id")
    Mono<Integer> updateUser(Long id, String username, String email, String phone, Integer age, String status);
}
