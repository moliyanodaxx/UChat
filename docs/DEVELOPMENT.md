# UChat 开发指南

本文档介绍如何在本地开发环境中运行和开发 UChat。

## 目录

- [环境准备](#环境准备)
- [快速开始](#快速开始)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [测试指南](#测试指南)
- [贡献指南](#贡献指南)

---

## 环境准备

### 必需工具

**服务端开发**
- Node.js 14.x 或更高版本
- npm 6.x 或更高版本
- 文本编辑器（推荐 VS Code）

**Web端开发**
- 现代浏览器（Chrome/Firefox/Safari）
- 浏览器开发者工具

**Android端开发**
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11 或更高版本
- Android SDK (Min SDK 30, Target SDK 36)
- Kotlin 1.9+

### 可选工具

- Git（版本控制）
- Postman（API测试）
- WebSocket调试工具

---

## 快速开始

### 克隆项目

```bash
git clone https://github.com/moliyanodaxx/UChat.git
cd UChat
```

### 启动服务器

```bash
cd server
npm install
node chatroom-server.js
```

服务器启动在 `http://localhost:10086`

### 访问Web端

浏览器打开 `http://localhost:10086`

### 运行Android端

1. 用Android Studio打开 `android` 目录
2. 同步Gradle依赖
3. 修改服务器地址（如需要）
4. 运行到模拟器或真机

---

## 开发流程

### 项目结构

```
UChat/
├── server/                    # 服务端
│   ├── chatroom-server.js    # 主服务器文件
│   ├── users.json            # 用户数据（运行时生成）
│   ├── rooms.json            # 房间数据（运行时生成）
│   ├── package.json          # 依赖配置
│   └── node_modules/         # 依赖包
│
├── web/                       # Web客户端
│   ├── chatroom.html         # 主HTML
│   ├── chatroom.css          # 样式表
│   └── chatroom.js           # 主逻辑
│
├── android/                   # Android客户端
│   └── app/
│       └── src/
│           └── main/
│               ├── java/      # Kotlin源码
│               └── res/       # 资源文件
│
└── docs/                      # 文档
    ├── FEATURES.md
    ├── TECHNICAL.md
    ├── DEVELOPMENT.md
    └── DEPLOYMENT.md
```

### 服务器开发

**修改端口**

编辑 `server/chatroom-server.js`:
```javascript
const PORT = 10086; // 修改为其他端口
```

**添加新消息类型**

1. 定义常量:
```javascript
const TYPE_NEW_FEATURE = 36;
```

2. 添加处理逻辑:
```javascript
if (msg.type === TYPE_NEW_FEATURE) {
    // 处理逻辑
}
```

3. 更新Web/Android客户端常量

**修改AI配置**

```javascript
const AI_API_KEY = 'your-api-key';
const AI_MODEL = 'claude-sonnet-4-6';
```

### Web端开发

**文件说明**
- `chatroom.html`: 页面结构
- `chatroom.css`: 样式定义
- `chatroom.js`: 业务逻辑

**开发建议**
- 使用浏览器开发者工具调试
- WebSocket消息可在Network标签查看
- Console查看日志输出

**添加新UI组件**

1. HTML添加结构
2. CSS添加样式
3. JS添加事件监听和逻辑

**WebSocket调试**

```javascript
// 在chatroom.js中添加日志
console.log('收到消息:', data);
console.log('发送消息:', JSON.stringify(message));
```

### Android端开发

**项目结构**
```
app/src/main/java/com/example/uchat/
├── data/
│   ├── local/          # DataStore
│   ├── model/          # 数据模型
│   └── remote/         # WebSocket客户端
├── ui/
│   ├── screens/        # Compose界面
│   └── theme/          # 主题
├── util/               # 工具类
├── viewmodel/          # ViewModel
└── MainActivity.kt
```

**添加新界面**

1. 创建Screen文件:
```kotlin
@Composable
fun NewScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    // UI代码
}
```

2. 添加路由:
```kotlin
composable("new_screen") {
    NewScreen(viewModel = vm, onBack = { navController.popBackStack() })
}
```

**WebSocket通信**

```kotlin
// 发送消息
viewModel.sendMessage(type, data)

// 处理响应
when (msg.type) {
    MsgType.NEW_TYPE -> {
        // 处理逻辑
    }
}
```

---

## 代码规范

### JavaScript规范

**命名**
- 变量: camelCase (`userName`, `roomId`)
- 常量: UPPER_SNAKE_CASE (`TYPE_MSG`, `AI_API_URL`)
- 函数: camelCase (`sendMessage`, `handleAIRequest`)

**代码风格**
```javascript
// 使用const/let，不使用var
const userId = '123';
let count = 0;

// 使用箭头函数
const add = (a, b) => a + b;

// 使用模板字符串
const msg = `Hello, ${userName}`;
```

### Kotlin规范

**命名**
- 类: PascalCase (`ChatViewModel`, `WebSocketClient`)
- 函数: camelCase (`sendMessage`, `getUserInfo`)
- 常量: UPPER_SNAKE_CASE (`MAX_LENGTH`, `DEFAULT_PORT`)

**代码风格**
```kotlin
// 使用数据类
data class User(
    val id: String,
    val nickname: String
)

// 使用协程
viewModelScope.launch {
    // 异步操作
}
```

### 注释规范

```javascript
// 单行注释：解释为什么，不是做什么

/**
 * 多行注释：复杂函数的说明
 * @param ws WebSocket连接
 * @param message 用户消息
 * @returns AI响应结果
 */
async function handleAIRequest(ws, message) {
    // ...
}
```

---

## 测试指南

### 手动测试

**功能测试清单**

基础功能:
- [ ] 注册新账号
- [ ] 登录已有账号
- [ ] 设置个人资料
- [ ] 创建房间
- [ ] 加入房间
- [ ] 发送消息
- [ ] 接收消息
- [ ] 切换房间

高级功能:
- [ ] 添加好友
- [ ] 接受好友请求
- [ ] 邀请好友进房间
- [ ] 设置违禁词（房主）
- [ ] AI助手创建房间
- [ ] AI助手发送消息

### WebSocket测试

使用浏览器控制台测试:

```javascript
// 连接WebSocket
const ws = new WebSocket('ws://localhost:10086');

// 监听消息
ws.onmessage = (e) => console.log('收到:', JSON.parse(e.data));

// 发送测试消息
ws.send(JSON.stringify({
    type: 2,
    roomId: '0000000000',
    msg: 'test'
}));
```

### AI功能测试

```bash
# 测试AI API连接
curl -X POST https://cn.luckyapi.chat/v1/messages \
  -H "x-api-key: your-key" \
  -H "anthropic-version: 2023-06-01" \
  -H "content-type: application/json" \
  -d '{
    "model": "claude-sonnet-4-6",
    "max_tokens": 1024,
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

### 性能测试

**连接压力测试**
- 模拟多个用户同时连接
- 监控内存和CPU使用
- 检查消息延迟

**消息压力测试**
- 高频发送消息
- 检查消息丢失率
- 测试断线重连

---

## 贡献指南

### 提交Issue

**Bug报告**
- 描述问题
- 复现步骤
- 预期行为
- 实际行为
- 环境信息（浏览器/Android版本）

**功能建议**
- 功能描述
- 使用场景
- 预期效果

### 提交PR

**流程**

1. Fork项目
2. 创建功能分支
```bash
git checkout -b feature/new-feature
```

3. 提交代码
```bash
git add .
git commit -m "feat: 添加新功能"
```

4. 推送分支
```bash
git push origin feature/new-feature
```

5. 创建Pull Request

**Commit规范**

使用约定式提交:
- `feat:` 新功能
- `fix:` Bug修复
- `docs:` 文档更新
- `style:` 代码格式
- `refactor:` 重构
- `test:` 测试相关
- `chore:` 构建/工具

示例:
```
feat: 添加AI助手UU人格设定
fix: 修复房间邀请状态未持久化问题
docs: 更新API文档
```

### 代码审查

**审查要点**
- 功能是否完整
- 代码是否符合规范
- 是否有潜在bug
- 是否有测试
- 文档是否更新

---

## 常见问题

**Q: 服务器启动失败**
A: 检查端口是否被占用，尝试修改PORT常量

**Q: WebSocket连接失败**
A: 检查服务器是否启动，检查URL是否正确

**Q: AI功能不工作**
A: 检查API Key是否配置，检查网络连接

**Q: Android编译失败**
A: 清理项目后重新构建：Build > Clean Project > Rebuild Project

**Q: 数据丢失**
A: 检查users.json和rooms.json文件权限

---

## 调试技巧

### 服务器调试

```javascript
// 添加详细日志
console.log('[DEBUG] 收到消息:', msg.type, msg);
console.log('[DEBUG] 当前连接数:', connections.length);
console.log('[DEBUG] 房间列表:', Array.from(rooms.keys()));
```

### 浏览器调试

- 使用Chrome DevTools
- Network标签查看WebSocket帧
- Console查看日志
- Application查看LocalStorage

### Android调试

```kotlin
// 使用Logcat
Log.d("UChat", "发送消息: $message")
Log.e("UChat", "错误: ${e.message}")
```

---

## 资源链接

- [WebSocket API文档](https://developer.mozilla.org/zh-CN/docs/Web/API/WebSocket)
- [Jetpack Compose文档](https://developer.android.com/jetpack/compose)
- [Claude API文档](https://docs.anthropic.com/)
- [Kotlin协程指南](https://kotlinlang.org/docs/coroutines-guide.html)
