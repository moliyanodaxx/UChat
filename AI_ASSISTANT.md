# AI助手功能说明

## 功能概述

AI助手通过Claude API为用户提供自然语言操作界面，用户可以通过对话方式执行应用内的各种操作，无需手动点击按钮和填写表单。

**核心特性：**
- 所有用户均可使用（无需特殊权限）
- 支持除注册/登录外的所有用户操作
- 基于Claude Tool Use实现智能操作解析和执行

## API规范

### 消息类型

```javascript
const TYPE_AI_REQUEST = 34;   // 客户端 → 服务端：AI助手请求
const TYPE_AI_RESPONSE = 35;  // 服务端 → 客户端：AI助手响应
```

### 数据结构

**客户端发送（AI_REQUEST）：**
```json
{
  "type": 34,
  "message": "帮我创建一个名为'技术交流'的房间"
}
```

**服务端响应（AI_RESPONSE）：**
```json
{
  "type": 35,
  "success": true,
  "response": "已为您创建房间'技术交流'，房间ID为xxx"
}
```

**失败响应：**
```json
{
  "type": 35,
  "success": false,
  "message": "AI服务异常"
}
```

## 服务端实现

### 配置

文件：`server/chatroom-server.js`

```javascript
const AI_API_URL = 'https://cn.luckyapi.chat/v1/messages';
const AI_API_KEY = 'sk-xxx';  // luckyapi中转站密钥
const AI_MODEL = 'claude-sonnet-4-6';
```

### 支持的工具（Tools）

AI可以调用以下4个工具执行操作：

1. **create_room** - 创建聊天室
   - 参数：`name` (必需), `password` (可选)
   
2. **join_room** - 加入聊天室
   - 参数：`roomId` (必需), `password` (可选)
   
3. **send_message** - 发送消息
   - 参数：`roomId` (必需), `message` (必需)
   
4. **update_profile** - 更新个人资料
   - 参数：`nickname` (可选), `avatar` (可选), `signature` (可选)

### 核心函数

```javascript
// 处理AI请求
async function handleAIRequest(ws, userMessage)

// 执行工具调用
function executeTool(ws, toolName, input)
```

### 消息处理

```javascript
if (msg.type === TYPE_AI_REQUEST) {
    const userMessage = msg.message;
    handleAIRequest(ws, userMessage).then(result => {
        send({ type: TYPE_AI_RESPONSE, ...result });
    }).catch(err => {
        send({ type: TYPE_AI_RESPONSE, success: false, message: 'AI服务异常' });
    });
}
```

## Android端实现

### 文件修改

1. **Models.kt** - 添加消息类型常量
```kotlin
const val AI_REQUEST = 34
const val AI_RESPONSE = 35
```

2. **ChatViewModel.kt** - 添加AI状态和方法
```kotlin
// 状态
private val _aiMessages = MutableStateFlow<List<ChatMessage.UserMessage>>(emptyList())
val aiMessages: StateFlow<List<ChatMessage.UserMessage>> = _aiMessages
private val _aiLoading = MutableStateFlow(false)
val aiLoading: StateFlow<Boolean> = _aiLoading

// 方法
fun sendAIRequest(message: String)

// 消息处理
MsgType.AI_RESPONSE -> { /* 添加AI响应到消息列表 */ }
```

3. **AIAssistantScreen.kt** - AI聊天界面（新建）
   - 消息列表（LazyColumn）
   - 输入框 + 发送按钮
   - Loading状态显示

4. **MainActivity.kt** - 添加路由
```kotlin
composable("ai_assistant") {
    AIAssistantScreen(viewModel = vm, onBack = { navController.popBackStack() })
}
```

5. **RoomListScreen.kt** - 添加入口按钮
   - TopAppBar的actions中添加机器人图标按钮

### 使用流程

1. 用户在房间列表点击顶部机器人图标
2. 进入AI助手聊天界面
3. 输入自然语言指令（如"帮我创建一个房间"）
4. AI解析意图并执行相应操作
5. 返回执行结果

## 网页端实现（待完成）

### 需要添加的文件/代码

1. **消息类型定义**
```javascript
const TYPE_AI_REQUEST = 34;
const TYPE_AI_RESPONSE = 35;
```

2. **AI助手界面组件**
   - 创建新页面或弹窗组件
   - 消息列表显示
   - 输入框和发送按钮
   - Loading状态

3. **WebSocket消息处理**
```javascript
case 35: // AI_RESPONSE
    if (data.success) {
        // 添加AI响应到消息列表
        addAIMessage({ from: 'ai', text: data.response });
    } else {
        showError(data.message);
    }
    break;
```

4. **发送AI请求**
```javascript
function sendAIRequest(message) {
    ws.send(JSON.stringify({
        type: 34,
        message: message
    }));
}
```

5. **UI入口**
   - 在主界面添加AI助手按钮
   - 建议使用机器人图标，保持与Android端一致

### 参考Android实现

网页端可参考Android端的实现逻辑：
- 状态管理：消息列表、loading状态
- 消息显示：用户消息（右对齐）、AI消息（左对齐）
- 交互流程：发送请求 → 显示loading → 接收响应 → 更新列表

## 测试建议

### 功能测试

1. **创建房间**
   - "帮我创建一个名为'测试房间'的房间"
   - "创建一个有密码的房间，名字叫技术交流，密码是123456"

2. **加入房间**
   - "加入房间ID为xxx的房间"
   - "加入xxx房间，密码是123456"

3. **发送消息**
   - "在xxx房间发送'大家好'"
   - "帮我在技术交流房间里说hello"

4. **更新资料**
   - "把我的昵称改成'小明'"
   - "更新个性签名为'热爱编程'"

### 边界测试

- 发送空消息
- 连续快速发送多条消息
- AI响应超时处理
- 网络断开重连
- 工具执行失败（如加入不存在的房间）

## 依赖

### 服务端
- axios (^1.x) - HTTP客户端，用于调用Claude API

### Claude API
- 提供商：luckyapi中转站
- 端点：https://cn.luckyapi.chat/v1/messages
- 模型：claude-sonnet-4-6
- 功能：消息生成 + Tool Use

## 状态

- [x] 服务端实现完成
- [x] Android端实现完成
- [ ] 网页端待实现
- [ ] 功能测试
