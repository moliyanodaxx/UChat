# UChat 技术文档

本文档详细介绍 UChat 的技术架构、API规范和实现细节。

## 目录

- [系统架构](#系统架构)
- [消息协议](#消息协议)
- [API规范](#api规范)
- [数据结构](#数据结构)
- [AI集成](#ai集成)

---

## 系统架构

### 整体架构

```
┌─────────────┐      WebSocket      ┌──────────────┐
│  Web Client │ ◄──────────────────► │              │
└─────────────┘                      │              │
                                     │    Node.js   │      HTTP      ┌─────────────┐
┌─────────────┐      WebSocket      │    Server    │ ◄────────────► │  Claude API │
│   Android   │ ◄──────────────────► │              │                └─────────────┘
│   Client    │                      │              │
└─────────────┘                      └──────────────┘
                                            │
                                            ▼
                                     ┌──────────────┐
                                     │  JSON Files  │
                                     │ users.json   │
                                     │ rooms.json   │
                                     └──────────────┘
```

### 技术选型

**服务端**
- **运行时**: Node.js
- **WebSocket库**: ws (8.20.0)
- **HTTP客户端**: axios (用于AI API调用)
- **数据持久化**: JSON文件存储

**Web客户端**
- **纯原生技术栈**: HTML5 + CSS3 + JavaScript (ES6+)
- **无框架依赖**: 直接操作DOM，轻量高效
- **WebSocket API**: 原生浏览器支持

**Android客户端**
- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material Design 3
- **架构模式**: MVVM
- **WebSocket**: OkHttp WebSocket

---

## 消息协议

### 协议概述

UChat 使用自定义的 WebSocket 消息协议，所有消息均为 JSON 格式。

### 消息类型列表

| 类型 | 名称 | 方向 | 说明 |
|------|------|------|------|
| 0 | ENTER | S→C | 用户进入房间 |
| 1 | LEAVE | S→C | 用户离开房间 |
| 2 | MSG | C↔S | 聊天消息 |
| 3 | HEARTBEAT | C↔S | 心跳保持连接 |
| 4 | SET_USERNAME | C→S | 设置用户名（已废弃） |
| 5 | CREATE_ROOM | C↔S | 创建房间 |
| 6 | JOIN_ROOM | C↔S | 加入房间 |
| 7 | LEAVE_ROOM | C↔S | 离开房间 |
| 8 | ROOM_LIST | S→C | 房间列表 |
| 9 | ROOM_MEMBERS | C↔S | 房间成员列表 |
| 10 | KICK_MEMBER | C↔S | 踢出成员 |
| 11 | TRANSFER_OWNER | C↔S | 转让房主 |
| 12 | SWITCH_ROOM | C↔S | 切换房间 |
| 13 | HISTORY | S→C | 历史消息 |
| 14 | REGISTER | C↔S | 用户注册 |
| 15 | LOGIN | C↔S | 用户登录 |
| 16 | SET_PROFILE | C↔S | 设置个人资料 |
| 17 | SEARCH_USER | C↔S | 搜索用户 |
| 18 | GET_USER_INFO | C↔S | 获取用户信息 |
| 19 | SEND_FRIEND_REQUEST | C↔S | 发送好友请求 |
| 20 | GET_FRIEND_REQUESTS | C↔S | 获取好友请求列表 |
| 21 | HANDLE_FRIEND_REQUEST | C↔S | 处理好友请求 |
| 22 | GET_FRIENDS | C↔S | 获取好友列表 |
| 23 | UPDATE_PROFILE | C↔S | 更新个人资料 |
| 24 | GET_ROOM_INFO | C↔S | 获取房间信息 |
| 25 | UPDATE_ROOM_PASSWORD | C↔S | 修改房间密码 |
| 26 | INVITE_TO_ROOM | C↔S | 邀请好友进房间 |
| 29 | ADD_BANNED_WORD | C↔S | 添加违禁词 |
| 30 | REMOVE_BANNED_WORD | C↔S | 删除违禁词 |
| 31 | GET_BANNED_WORDS | C↔S | 获取违禁词列表 |
| 32 | BANNED_WORDS_LIST | S→C | 违禁词列表响应 |
| 33 | MSG_BLOCKED | S→C | 消息被拦截 |
| 34 | AI_REQUEST | C→S | AI助手请求 |
| 35 | AI_RESPONSE | S→C | AI助手响应 |

C→S: 客户端到服务端
S→C: 服务端到客户端
C↔S: 双向通信

---

## API规范

### 用户相关

#### 注册 (TYPE=14)

**请求**
```json
{
  "type": 14,
  "accountId": "user123456",
  "password": "password123"
}
```

**响应**
```json
{
  "type": 14,
  "success": true
}
```

#### 登录 (TYPE=15)

**请求**
```json
{
  "type": 15,
  "accountId": "user123456",
  "password": "password123"
}
```

**响应**
```json
{
  "type": 15,
  "success": true,
  "needProfile": false,
  "userData": {
    "accountId": "user123456",
    "nickname": "用户昵称",
    "avatar": "😀",
    "signature": "个性签名"
  }
}
```

### 房间相关

#### 创建房间 (TYPE=5)

**请求**
```json
{
  "type": 5,
  "roomName": "技术交流",
  "roomId": "1234567890",
  "password": "123456"
}
```

**响应**
```json
{
  "type": 5,
  "success": true,
  "room": {
    "id": "1234567890",
    "name": "技术交流",
    "memberCount": 1
  }
}
```

#### 发送消息 (TYPE=2)

**请求**
```json
{
  "type": 2,
  "roomId": "1234567890",
  "msg": "大家好"
}
```

**广播**
```json
{
  "type": 2,
  "username": "用户昵称",
  "accountId": "user123456",
  "msg": "大家好",
  "time": "12:34:56"
}
```

### AI助手

#### AI请求 (TYPE=34)

**请求**
```json
{
  "type": 34,
  "message": "帮我创建一个技术交流房间"
}
```

**响应**
```json
{
  "type": 35,
  "success": true,
  "response": "已为您创建房间'技术交流'，房间ID为1234567890"
}
```

---

## 数据结构

### 用户数据

```javascript
{
  "accountId": "user123456",      // 账号ID（唯一）
  "password": "hashedPassword",   // 密码（明文存储）
  "nickname": "用户昵称",          // 昵称
  "avatar": "😀",                 // 头像emoji
  "signature": "个性签名",        // 个性签名
  "rooms": ["0000000000"],        // 已加入的房间ID列表
  "friends": ["friend001"],       // 好友账号列表
  "friendRequests": []            // 待处理的好友请求
}
```

### 房间数据

```javascript
{
  "id": "1234567890",             // 房间ID（唯一）
  "name": "技术交流",              // 房间名称
  "password": "123456",           // 房间密码（可选）
  "owner": "user123456",          // 房主账号ID
  "isSystem": false,              // 是否系统房间
  "createdAt": 1234567890000      // 创建时间戳
}
```

### 运行时数据

**连接对象**
```javascript
{
  uid: 1,                         // 连接唯一ID
  accountId: "user123456",        // 账号ID
  username: "用户昵称",            // 昵称
  avatar: "😀",                   // 头像
  signature: "个性签名",          // 签名
  rooms: Set(['0000000000']),    // 当前在的房间集合
  isAlive: true,                 // 心跳状态
  readyState: WebSocket.OPEN     // WebSocket状态
}
```

**房间对象（内存）**
```javascript
{
  id: "1234567890",               // 房间ID
  name: "技术交流",                // 房间名称
  password: "123456",             // 密码
  owner: "user123456",            // 房主
  members: Set([1, 2, 3]),       // 成员UID集合
  history: [],                    // 消息历史（最近100条）
  bannedWords: Set(['违禁词'])   // 违禁词集合
}
```

---

## AI集成

### Claude API配置

```javascript
const AI_API_URL = 'https://cn.luckyapi.chat/v1/messages';
const AI_API_KEY = 'sk-xxx';  // 需要配置有效密钥
const AI_MODEL = 'claude-sonnet-4-6';
```

### 工具定义

UU支持4个工具，通过 Claude Tool Use 实现：

**1. create_room**
```json
{
  "name": "create_room",
  "description": "创建聊天室",
  "input_schema": {
    "type": "object",
    "properties": {
      "name": { "type": "string", "description": "房间名称" },
      "password": { "type": "string", "description": "房间密码(可选)" }
    },
    "required": ["name"]
  }
}
```

**2. join_room**
```json
{
  "name": "join_room",
  "description": "加入聊天室",
  "input_schema": {
    "type": "object",
    "properties": {
      "roomId": { "type": "string", "description": "房间ID" },
      "password": { "type": "string", "description": "房间密码(可选)" }
    },
    "required": ["roomId"]
  }
}
```

**3. send_message**
```json
{
  "name": "send_message",
  "description": "发送消息到指定房间",
  "input_schema": {
    "type": "object",
    "properties": {
      "roomId": { "type": "string", "description": "房间ID" },
      "message": { "type": "string", "description": "消息内容" }
    },
    "required": ["roomId", "message"]
  }
}
```

**4. update_profile**
```json
{
  "name": "update_profile",
  "description": "更新个人资料",
  "input_schema": {
    "type": "object",
    "properties": {
      "nickname": { "type": "string", "description": "昵称" },
      "avatar": { "type": "string", "description": "头像" },
      "signature": { "type": "string", "description": "个性签名" }
    }
  }
}
```

### AI处理流程

```
用户输入 → AI_REQUEST(type=34)
    ↓
服务器调用Claude API（附带工具定义）
    ↓
Claude返回tool_use或text响应
    ↓
如果是tool_use → 执行对应工具
    ↓
AI_RESPONSE(type=35) → 返回执行结果
```

### System Prompt

```
你叫UU，是UChat在线聊天室的专属AI助手。
你不是Kiro，也不是其他助手。
你的唯一职责是帮助用户使用UChat的功能：
创建房间、加入房间、发送消息、更新资料等。
请用友好、简洁的方式回应，并积极使用提供的工具来完成用户的请求。
```

---

## 性能优化

### 连接管理

- 心跳机制：每25秒客户端发送心跳
- 心跳检测：服务器每30秒检测连接活性
- 自动重连：客户端断线后3秒自动重连

### 消息优化

- 历史消息限制：每个房间最多保存100条
- ENTER/LEAVE消息不保存到历史记录
- 广播消息时跳过已关闭的连接

### 内存管理

- 使用Set存储房间成员（O(1)查找）
- 使用Map存储房间对象（O(1)查找）
- 定期清理断开的连接

---

## 安全考虑

### 认证安全

- 密码明文存储（⚠️ 生产环境应使用bcrypt加密）
- 账号格式验证（10位字母或数字）
- 密码格式验证（8-10位字母或数字）

### 消息安全

- 违禁词过滤
- 消息长度限制（前端控制）
- XSS防护（innerHTML改用textContent）

### 连接安全

- 多设备登录检测
- 房间权限验证
- 房主权限控制

### API安全

- AI API Key不应硬编码
- 建议使用环境变量或配置文件
- 限制API调用频率（待实现）

---

## 扩展性

### 水平扩展

当前架构为单机版，扩展建议：
1. 使用Redis存储会话和房间数据
2. 引入消息队列（RabbitMQ/Kafka）
3. 多实例部署 + 负载均衡
4. WebSocket粘性会话

### 功能扩展

易于扩展的功能：
- 添加新的消息类型
- 添加新的AI工具
- 引入数据库（MongoDB/PostgreSQL）
- 添加文件上传功能
- 实现一对一私聊
- 添加消息撤回功能

---

## 依赖版本

**服务端**
```json
{
  "ws": "^8.20.0",
  "axios": "^1.x"
}
```

**Web端**
- 无外部依赖
- 要求浏览器支持ES6+和WebSocket

**Android端**
```kotlin
- Kotlin 1.9+
- Compose BOM 2024.01.00
- OkHttp 4.12.0
```
