# 服务端待修复问题

## 🔴 高优先级问题

### 问题1：房间邀请消息状态未持久化

**问题描述：**
用户接受或拒绝房间邀请后，退出系统消息再进入，邀请消息的状态（已接受/已拒绝）会丢失，恢复为未操作状态，导致同一个邀请可以被重复操作。

**现象：**
1. 用户在系统消息中接受了某个房间邀请
2. 退出系统消息房间，再重新进入
3. 之前接受的邀请消息又变成未操作状态，可以再次接受或拒绝

**影响：**
- 网页版和Android客户端都存在此问题
- 用户体验差，邀请可以被重复操作

**原因分析：**
邀请消息在服务端的历史记录中没有保存用户的操作状态（accepted/rejected）。每次返回历史消息时，邀请消息都是初始状态。

**建议解决方案：**
1. 在服务端为每个邀请消息维护状态（待处理/已接受/已拒绝）
2. 用户接受或拒绝邀请时，更新对应邀请消息的状态
3. 返回历史消息时，包含邀请消息的最新状态
4. 客户端根据状态显示对应的UI（可操作/已接受/已拒绝）

**数据结构建议：**
```javascript
// 邀请消息应该包含状态字段
{
  type: 'ROOM_INVITE',
  from: 'user123',
  roomId: 'room456',
  roomName: '测试房间',
  time: '12:34',
  needPassword: false,
  status: 'pending' // 'pending' | 'accepted' | 'rejected'
}
```

**状态：**
- [x] 已修复
- [ ] 已测试

---

### 问题2：房间成员列表缺少头像和签名字段

### 问题描述
Android 客户端在显示房间成员列表时，无法获取成员的头像（avatar）和个性签名（signature），导致只能显示基于 UID 生成的默认头像，与用户实际设置的头像不一致。

### 原因分析
服务端的 `ROOM_MEMBERS`（type=9）响应中，每个成员对象只包含：
- `uid`
- `username`
- `isOnline`
- `isOwner`

**缺少**：
- `avatar` - 用户头像
- `signature` - 用户个性签名

### 修改方案

**文件：** `chatroom-server.js`

**找到 `getRoomMembers` 函数**，当前代码：

```javascript
const getRoomMembers = (roomId) => {
    const room = rooms.get(roomId);
    if (!room) return [];
    return connections
        .filter(c => c.rooms.has(roomId))
        .map(c => ({ 
            uid: c.uid, 
            username: c.username, 
            isOnline: c.readyState === WebSocket.OPEN, 
            isOwner: room.owner === c.uid 
        }));
};
```

**修改为**：

```javascript
const getRoomMembers = (roomId) => {
    const room = rooms.get(roomId);
    if (!room) return [];
    return connections
        .filter(c => c.rooms.has(roomId))
        .map(c => ({ 
            uid: c.uid, 
            username: c.username, 
            avatar: c.avatar || '',
            signature: c.signature || '',
            isOnline: c.readyState === WebSocket.OPEN, 
            isOwner: room.owner === c.uid 
        }));
};
```

### 影响范围
- 房间成员列表显示
- 用户头像显示的一致性

### 测试步骤
1. 修改服务端代码后重启服务器
2. Android 客户端登录并设置个人头像
3. 进入任意聊天室
4. 点击右上角查看房间成员
5. 验证成员列表中显示的是用户设置的头像，而不是默认头像

### 状态
- [x] 已修复
- [ ] 已测试
- [ ] 已上线

---

**Android 客户端**：已完成适配，等待服务端修改后即可正常显示用户头像。

---

## 🔴 高优先级问题

### 问题3：服务端不应保存ENTER/LEAVE消息到历史记录

**问题描述：**
服务端将用户进入/离开聊天室的消息保存到了历史记录中，导致这些消息与普通聊天消息混在一起，影响聊天体验。

**根本原因：**
这是**服务端的问题**。服务端在处理ENTER/LEAVE消息时，将其添加到了房间的历史记录数组中。

**期望行为：**
ENTER/LEAVE消息应该：
1. 实时广播给房间内的所有在线用户
2. **不保存到房间历史记录中**
3. 客户端收到后显示为临时播报（3秒后消失）

**服务端修改方案：**

在 `chatroom-server.js` 中，找到处理 ENTER/LEAVE 消息的代码：

当前代码可能类似：
```javascript
case MsgType.ENTER:
case MsgType.LEAVE:
    // 广播消息
    broadcastToRoom(roomId, message);
    // 保存到历史记录 ❌ 这一步不应该做
    room.history.push(message);
    break;
```

**修改为**：
```javascript
case MsgType.ENTER:
case MsgType.LEAVE:
    // 只广播，不保存到历史记录
    broadcastToRoom(roomId, message);
    // 不要 push 到 room.history
    break;
```

**客户端适配状态：**
- **Android端**：已完成，实时消息显示为临时播报，历史记录会过滤ENTER/LEAVE
- **网页端**：需要做同样的临时播报UI改进

**状态：**
- [x] 服务端已修改
- [ ] 已测试
- [x] 网页端UI已改进

---

## 🆕 新功能需求

### 功能4：房间违禁词管理

**功能描述：**
房主可以为房间设置违禁词列表。当任何成员发送包含违禁词的消息时，消息将被拦截，无法发送，并提示发送者。

**用户界面（客户端）：**
1. 房间详情页添加"设置违禁词"按钮（仅房主可见）
2. 违禁词设置页面包含：
   - 输入框 + "添加"按钮
   - 已有违禁词列表（气泡样式显示）
   - 每个违禁词右上角有X删除按钮
   - 点击X弹出确认对话框
3. 发送消息时如包含违禁词，拦截并弹出提示

**服务端实现方案：**

**1. 数据结构修改**

在 Room 对象中添加 `bannedWords` 字段：
```javascript
const room = {
    id: roomId,
    name: roomName,
    owner: ownerUid,
    password: password || null,
    members: new Set(),
    history: [],
    bannedWords: new Set() // 新增：违禁词集合
};
```

**2. 新增消息类型**

在 MsgType 枚举中添加：
```javascript
const MsgType = {
    // ... 现有类型
    ADD_BANNED_WORD: 29,      // 添加违禁词
    REMOVE_BANNED_WORD: 30,   // 删除违禁词
    GET_BANNED_WORDS: 31,     // 获取违禁词列表
    BANNED_WORDS_LIST: 32,    // 返回违禁词列表
    MSG_BLOCKED: 33           // 消息被拦截（包含违禁词）
};
```

**3. 消息处理逻辑**

```javascript
case MsgType.ADD_BANNED_WORD: {
    const { roomId, word } = data;
    const room = rooms.get(roomId);
    if (!room) break;
    
    // 仅房主可以添加违禁词
    if (room.owner !== ws.uid) {
        ws.send(JSON.stringify({
            type: MsgType.ERROR,
            message: '只有房主可以设置违禁词'
        }));
        break;
    }
    
    room.bannedWords.add(word);
    
    // 通知添加成功，返回完整列表
    ws.send(JSON.stringify({
        type: MsgType.BANNED_WORDS_LIST,
        roomId: roomId,
        words: Array.from(room.bannedWords)
    }));
    break;
}

case MsgType.REMOVE_BANNED_WORD: {
    const { roomId, word } = data;
    const room = rooms.get(roomId);
    if (!room) break;
    
    // 仅房主可以删除违禁词
    if (room.owner !== ws.uid) {
        ws.send(JSON.stringify({
            type: MsgType.ERROR,
            message: '只有房主可以删除违禁词'
        }));
        break;
    }
    
    room.bannedWords.delete(word);
    
    // 通知删除成功，返回完整列表
    ws.send(JSON.stringify({
        type: MsgType.BANNED_WORDS_LIST,
        roomId: roomId,
        words: Array.from(room.bannedWords)
    }));
    break;
}

case MsgType.GET_BANNED_WORDS: {
    const { roomId } = data;
    const room = rooms.get(roomId);
    if (!room) break;
    
    // 返回违禁词列表
    ws.send(JSON.stringify({
        type: MsgType.BANNED_WORDS_LIST,
        roomId: roomId,
        words: Array.from(room.bannedWords)
    }));
    break;
}
```

**4. 违禁词检查逻辑**

在 CHAT_MSG 处理中添加检查：
```javascript
case MsgType.CHAT_MSG: {
    const { roomId, content } = data;
    const room = rooms.get(roomId);
    if (!room) break;
    
    // 检查是否包含违禁词
    for (const word of room.bannedWords) {
        if (content.includes(word)) {
            // 消息包含违禁词，拦截并通知发送者
            ws.send(JSON.stringify({
                type: MsgType.MSG_BLOCKED,
                message: `消息包含违禁词：${word}`
            }));
            return; // 不继续处理
        }
    }
    
    // 未包含违禁词，正常处理消息
    // ... 原有的消息处理逻辑
    break;
}
```

**消息数据结构：**

客户端发送：
```javascript
// 添加违禁词
{ type: 29, roomId: "room123", word: "违禁词" }

// 删除违禁词
{ type: 30, roomId: "room123", word: "违禁词" }

// 获取违禁词列表
{ type: 31, roomId: "room123" }
```

服务端响应：
```javascript
// 返回违禁词列表
{ type: 32, roomId: "room123", words: ["词1", "词2"] }

// 消息被拦截
{ type: 33, message: "消息包含违禁词：xxx" }
```

**状态：**
- [x] 服务端已实现
- [ ] Android端已实现
- [x] 网页端已实现
- [ ] 已测试
