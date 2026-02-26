package org.example.server.config;

import org.example.server.entity.User;
import org.example.server.repository.UserRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;

/**
 * 用户工具方法类
 * 用于 Spring AI ToolCallback 方式注册工具
 * 通过 @Tool 注解标记方法
 */
public class UserToolMethods {

    private final UserRepository userRepository;

    public UserToolMethods(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 创建新用户
     */
    @Tool(name = "createUser", description = "创建新用户，需要用户名、邮箱、手机号和年龄")
    public Mono<String> createUser(
            @ToolParam(description = "用户名，必填", required = true) String username,
            @ToolParam(description = "邮箱地址，必填", required = true) String email,
            @ToolParam(description = "手机号", required = false) String phone,
            @ToolParam(description = "年龄", required = false) Integer age) {

        System.out.println("[UserToolMethods] 开始创建用户: " + username);

        return userRepository.findByUsername(username)
                .flatMap(existingUser -> {
                    String errorMsg = "❌ 创建失败：用户名 '" + username + "' 已存在";
                    System.out.println("[UserToolMethods] " + errorMsg);
                    return Mono.just(errorMsg);
                })
                .switchIfEmpty(
                        userRepository.findByEmail(email)
                                .flatMap(existingUser -> {
                                    String errorMsg = "❌ 创建失败：邮箱 '" + email + "' 已被使用";
                                    System.out.println("[UserToolMethods] " + errorMsg);
                                    return Mono.just(errorMsg);
                                })
                                .switchIfEmpty(
                                        Mono.defer(() -> {
                                            User newUser = new User(username, email, phone, age);
                                            return userRepository.save(newUser)
                                                    .map(savedUser -> {
                                                        String successMsg = "✅ 用户创建成功！\n" + formatUser(savedUser);
                                                        System.out.println("[UserToolMethods] 用户创建成功: " + username);
                                                        return successMsg;
                                                    });
                                        })
                                )
                );
    }

    /**
     * 根据ID查询用户
     */
    @Tool(name = "getUserById", description = "根据用户ID查询用户信息")
    public Mono<String> getUserById(
            @ToolParam(description = "用户ID，必填", required = true) Long id) {

        System.out.println("[UserToolMethods] 查询用户ID: " + id);

        return userRepository.findById(id)
                .map(user -> {
                    String msg = "✅ 查询成功！\n" + formatUser(user);
                    System.out.println("[UserToolMethods] 查询成功: " + user.getUsername());
                    return msg;
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户");
    }

    /**
     * 根据用户名查询用户
     */
    @Tool(name = "getUserByUsername", description = "根据用户名查询用户信息")
    public Mono<String> getUserByUsername(
            @ToolParam(description = "用户名，必填", required = true) String username) {

        System.out.println("[UserToolMethods] 查询用户名: " + username);

        return userRepository.findByUsername(username)
                .map(user -> {
                    String msg = "✅ 查询成功！\n" + formatUser(user);
                    System.out.println("[UserToolMethods] 查询成功: " + user.getUsername());
                    return msg;
                })
                .defaultIfEmpty("❌ 未找到用户名为 '" + username + "' 的用户");
    }

    /**
     * 查询所有用户
     */
    @Tool(name = "getAllUsers", description = "查询所有用户列表")
    public Mono<String> getAllUsers() {

        System.out.println("[UserToolMethods] 查询所有用户");

        return userRepository.findAll()
                .collectList()
                .flatMap(users -> {
                    System.out.println("[UserToolMethods] 查询完成，共 " + users.size() + " 条记录");

                    if (users.isEmpty()) {
                        return Mono.just("📭 暂无用户数据");
                    }

                    StringBuilder result = new StringBuilder();
                    result.append("📋 用户列表（共 ").append(users.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (User user : users) {
                        result.append(formatUser(user)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 根据状态查询用户
     */
    @Tool(name = "getUsersByStatus", description = "根据状态查询用户列表，如 ACTIVE、INACTIVE、DISABLED")
    public Mono<String> getUsersByStatus(
            @ToolParam(description = "用户状态：ACTIVE(活跃)、INACTIVE(非活跃)、DISABLED(禁用)", required = true) String status) {

        System.out.println("[UserToolMethods] 查询状态为 '" + status + "' 的用户");

        return userRepository.findByStatus(status.toUpperCase())
                .collectList()
                .flatMap(users -> {
                    System.out.println("[UserToolMethods] 状态 '" + status + "' 查询完成，共 " + users.size() + " 条记录");

                    if (users.isEmpty()) {
                        return Mono.just("📭 暂无状态为 '" + status + "' 的用户");
                    }

                    StringBuilder result = new StringBuilder();
                    result.append("📋 状态为 '").append(status).append("' 的用户列表（共 ")
                            .append(users.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (User user : users) {
                        result.append(formatUser(user)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 更新用户信息
     */
    @Tool(name = "updateUser", description = "根据用户ID更新用户信息")
    public Mono<String> updateUser(
            @ToolParam(description = "用户ID，必填", required = true) Long id,
            @ToolParam(description = "新用户名（不修改传null）", required = false) String username,
            @ToolParam(description = "新邮箱（不修改传null）", required = false) String email,
            @ToolParam(description = "新手机号（不修改传null）", required = false) String phone,
            @ToolParam(description = "新年龄（不修改传null）", required = false) Integer age,
            @ToolParam(description = "新状态：ACTIVE、INACTIVE、DISABLED（不修改传null）", required = false) String status) {

        System.out.println("[UserToolMethods] 开始更新用户ID: " + id);

        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    if (username != null && !username.isEmpty()) {
                        existingUser.setUsername(username);
                    }
                    if (email != null && !email.isEmpty()) {
                        existingUser.setEmail(email);
                    }
                    if (phone != null) {
                        existingUser.setPhone(phone);
                    }
                    if (age != null) {
                        existingUser.setAge(age);
                    }
                    if (status != null && !status.isEmpty()) {
                        existingUser.setStatus(status.toUpperCase());
                    }
                    existingUser.setUpdatedAt(java.time.LocalDateTime.now());

                    return userRepository.save(existingUser)
                            .map(updatedUser -> {
                                String msg = "✅ 用户更新成功！\n" + formatUser(updatedUser);
                                System.out.println("[UserToolMethods] 更新成功: " + updatedUser.getUsername());
                                return msg;
                            });
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户，无法更新");
    }

    /**
     * 删除用户
     */
    @Tool(name = "deleteUser", description = "根据用户ID删除用户")
    public Mono<String> deleteUser(
            @ToolParam(description = "用户ID，必填", required = true) Long id) {

        System.out.println("[UserToolMethods] 开始删除用户ID: " + id);

        return userRepository.findById(id)
                .flatMap(existingUser -> {
                    String username = existingUser.getUsername();
                    return userRepository.deleteById(id)
                            .then(Mono.fromCallable(() -> {
                                String msg = "✅ 用户删除成功！\n已删除用户：" + username;
                                System.out.println("[UserToolMethods] 删除成功: " + username);
                                return msg;
                            }));
                })
                .defaultIfEmpty("❌ 未找到ID为 " + id + " 的用户，无法删除");
    }

    /**
     * 根据年龄范围查询用户
     */
    @Tool(name = "getUsersByAgeRange", description = "根据年龄范围查询用户")
    public Mono<String> getUsersByAgeRange(
            @ToolParam(description = "最小年龄", required = true) Integer minAge,
            @ToolParam(description = "最大年龄", required = true) Integer maxAge) {

        System.out.println("[UserToolMethods] 查询年龄范围: " + minAge + "-" + maxAge);

        return userRepository.findByAgeRange(minAge, maxAge)
                .collectList()
                .flatMap(users -> {
                    System.out.println("[UserToolMethods] 年龄范围查询完成，共 " + users.size() + " 条记录");

                    if (users.isEmpty()) {
                        return Mono.just("📭 年龄在 " + minAge + " 到 " + maxAge + " 之间的用户不存在");
                    }

                    StringBuilder result = new StringBuilder();
                    result.append("📋 年龄在 ").append(minAge).append("-").append(maxAge)
                            .append(" 岁的用户列表（共 ").append(users.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (User user : users) {
                        result.append(formatUser(user)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 模糊搜索用户
     */
    @Tool(name = "searchUsers", description = "根据关键词模糊搜索用户名")
    public Mono<String> searchUsers(
            @ToolParam(description = "搜索关键词", required = true) String keyword) {

        System.out.println("[UserToolMethods] 搜索关键词: " + keyword);

        return userRepository.findByUsernameContaining(keyword)
                .collectList()
                .flatMap(users -> {
                    System.out.println("[UserToolMethods] 搜索完成，共 " + users.size() + " 条记录");

                    if (users.isEmpty()) {
                        return Mono.just("📭 未找到包含 '" + keyword + "' 的用户");
                    }

                    StringBuilder result = new StringBuilder();
                    result.append("📋 搜索 '").append(keyword).append("' 的结果（共 ")
                            .append(users.size()).append(" 条）：\n");
                    result.append("=".repeat(80)).append("\n");
                    for (User user : users) {
                        result.append(formatUser(user)).append("\n");
                        result.append("-".repeat(80)).append("\n");
                    }
                    return Mono.just(result.toString());
                });
    }

    /**
     * 统计用户总数
     */
    @Tool(name = "countUsers", description = "统计系统中的用户总数")
    public Mono<String> countUsers() {

        System.out.println("[UserToolMethods] 统计用户总数");

        return userRepository.countAll()
                .map(count -> {
                    String msg = "📊 系统用户总数：" + count + " 人";
                    System.out.println("[UserToolMethods] " + msg);
                    return msg;
                });
    }

    /**
     * 格式化用户对象为字符串
     */
    private String formatUser(User user) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return String.format(
                "👤 用户ID: %d\n" +
                        "   用户名: %s\n" +
                        "   邮箱: %s\n" +
                        "   手机号: %s\n" +
                        "   年龄: %d\n" +
                        "   状态: %s\n" +
                        "   创建时间: %s\n" +
                        "   更新时间: %s",
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone() != null ? user.getPhone() : "未设置",
                user.getAge() != null ? user.getAge() : 0,
                user.getStatus(),
                user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : "未知",
                user.getUpdatedAt() != null ? user.getUpdatedAt().format(formatter) : "未知"
        );
    }
}
