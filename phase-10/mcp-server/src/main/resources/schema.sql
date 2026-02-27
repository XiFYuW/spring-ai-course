-- 用户表初始化脚本
-- 用于 MCP Server 数据库操作演示

-- 删除已存在的表（如果需要重新创建）
DROP TABLE IF EXISTS users;

-- 创建用户表
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    age INTEGER,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以提高查询性能
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_age ON users(age);

-- 插入示例数据
INSERT INTO users (username, email, phone, age, status, created_at, updated_at) VALUES
('zhangsan', 'zhangsan@example.com', '13800138001', 25, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('lisi', 'lisi@example.com', '13800138002', 30, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('wangwu', 'wangwu@example.com', '13800138003', 28, 'INACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('zhaoliu', 'zhaoliu@example.com', '13800138004', 35, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('qianqi', 'qianqi@example.com', '13800138005', 22, 'DISABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 验证数据插入
SELECT * FROM users;
