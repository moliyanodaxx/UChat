# 服务端待修复问题清单

## 🔴 高优先级

### 问题1：聊天消息缺少发送者头像字段

**现象**：客户端聊天界面显示的用户头像与个人资料设置的不一致

**原因**：服务端发送的聊天消息中缺少 `avatar` 字段

**当前响应**：
```json
{
  "type": 2,
  "username": "用户昵称",
  "msg": "消息内容",
  "time": "12:34",
  "accountId": "1234567890"
}
```

**需要修改为**：
```json
{
  "type": 2,
  "username": "用户昵称",
  "msg": "消息内容",
  "time": "12:34",
  "accountId": "1234567890",
  "avatar": "😀"
}
```

**修改位置**：
1. 实时消息广播
2. 历史消息（HISTORY）返回

---

### 问题2：登录响应userData为null

**现象**：新注册用户首次登录时崩溃

**原因**：LOGIN响应中 `userData` 为 null

**需要修改**：
即使是新用户，也应返回空对象而非null：
```json
{
  "type": 15,
  "success": true,
  "needProfile": true,
  "userData": {
    "accountId": "1234567890",
    "nickname": "",
    "avatar": "",
    "signature": ""
  }
}
```

---

### 问题5：登录响应缺少完整signature字段

**现象**：用户登录后，个人资料界面的个性签名显示为空，但其他人查看该用户时能看到签名

**原因**：LOGIN响应中userData.signature字段未正确返回或为空字符串

**需要修改**：
确保LOGIN响应返回完整用户信息，包括signature：
```json
{
  "type": 15,
  "success": true,
  "needProfile": false,
  "userData": {
    "accountId": "1234567890",
    "nickname": "张三",
    "avatar": "😀",
    "signature": "这是我的个性签名"  // 必须包含且非空
  }
}
```

**影响范围**：
- LOGIN消息响应
- 确保从数据库读取用户信息时包含signature字段

---

## 🟡 中优先级

### 问题3：新用户未自动加入默认房间

**解决方案**：用户注册成功后自动加入：
- `0000000000` - 在线聊天室
- `0000000001` - 系统消息

```javascript
// 伪代码
function onUserRegister(user) {
  joinRoom(user, "0000000000");
  joinRoom(user, "0000000001");
}
```

---

## 🟢 低优先级

### 问题4：多设备登录检测

**方案**：新登录时向旧连接发送：
```json
{
  "type": 27,
  "reason": "账号在其他设备登录"
}
```

客户端已实现接收该消息后自动登出。

---

**测试清单**：
- [ ] 新用户注册登录检查默认房间
- [ ] 消息包含avatar字段
- [ ] 同账号两设备登录测试
- [ ] 新用户登录无崩溃
