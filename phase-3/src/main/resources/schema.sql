-- 创建会话表
CREATE TABLE IF NOT EXISTS conversation_sessions (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建消息表
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES conversation_sessions(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以加速查询
CREATE INDEX IF NOT EXISTS idx_messages_session_id ON conversation_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_sessions_updated_at ON conversation_sessions(updated_at DESC);


-- 创建角色配置表
CREATE TABLE IF NOT EXISTS role_config (
    id BIGSERIAL PRIMARY KEY,
    role_type VARCHAR(20) NOT NULL,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    template_content TEXT,
    enabled BOOLEAN DEFAULT true,
    priority INTEGER DEFAULT 10,
    tags VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以优化查询性能
CREATE INDEX IF NOT EXISTS idx_role_config_role_type ON role_config(role_type);
CREATE INDEX IF NOT EXISTS idx_role_config_enabled ON role_config(enabled);
CREATE INDEX IF NOT EXISTS idx_role_config_priority ON role_config(priority);
CREATE INDEX IF NOT EXISTS idx_role_config_tags ON role_config USING gin(to_tsvector('simple', tags));

-- 插入默认角色配置（使用改进后的模板系统）
INSERT INTO role_config (role_type, role_name, description, template_content, enabled, priority, tags) VALUES
('system', '默认助手', '友好的AI助手，提供专业、简洁的回答', '你是一个友好、专业的AI助手，请用简洁清晰的语言回答用户的问题。', true, 1, 'general,default'),
('system', '技术专家', '专注于技术问题的AI助手', '# {roleName}
{roleDescription}

## 核心能力
- 深入分析技术问题
- 提供详细的代码示例
- 解释技术原理和最佳实践

## 回答风格
请以专业、严谨的态度回答技术问题，确保信息的准确性和实用性。

系统提示：{systemPrompt}', true, 2, 'technical,programming'),
('system', '创意写作', '专注于创意写作的AI助手', '# {roleName}
{roleDescription}

## 创作风格
- 富有想象力和创造力
- 语言生动、形象
- 注重情感表达和故事性

## 适用场景
- 故事创作
- 诗歌写作
- 文案创作

', true, 3, 'creative,writing'),
('system', '语言翻译', '多语言翻译助手', '# {roleName}
{roleDescription}

## 翻译原则
- 准确传达原文意思
- 保持语言流畅自然
- 考虑文化差异和语境

## 支持语言
- 中文 ↔ 英文
- 其他语言翻译
', true, 4, 'translation,language'),
('user', '普通用户', '普通用户的提问模板', '{userQuestion}', true, 1, 'general,default'),
('assistant', '标准回答', 'AI助手的标准回答模板', '{assistantResponse}', true, 1, 'general,default')
ON CONFLICT (role_name) DO NOTHING;