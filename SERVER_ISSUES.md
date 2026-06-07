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
- [ ] 服务端已修改
- [ ] 已测试
- [ ] 网页端UI已改进
