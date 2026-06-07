# 服务端待修复问题

## 🔴 需要立即修复：房间成员列表缺少头像和签名字段

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
- [ ] 待修复
- [ ] 已测试
- [ ] 已上线

---

**Android 客户端**：已完成适配，等待服务端修改后即可正常显示用户头像。
