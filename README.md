# UChat - 在线聊天室

UChat 是一个功能完整的实时聊天应用，支持多房间、好友系统、AI助手等功能。提供 Web 端和 Android 客户端。

## ✨ 核心特性

### 基础功能
- 🔐 用户注册/登录系统
- 👤 个人资料管理（昵称、头像、签名）
- 💬 实时消息通信（WebSocket）
- 🏠 多房间管理（创建/加入/切换/退出）
- 👥 好友系统（搜索/添加/管理）
- 🔒 房间密码保护
- 📝 违禁词过滤

### 高级功能
- 🤖 **AI助手 UU**（基于Claude API）
  - 自然语言创建/加入房间
  - 智能发送消息
  - 快速更新资料
- 🎯 房间成员管理
- 🔄 断线自动重连
- 📱 多设备登录检测
- 💾 消息历史记录

## 🚀 快速开始

### 环境要求
- Node.js 14+
- 现代浏览器（支持WebSocket）
- Android 11+ (移动端)

### 服务器部署

```bash
# 克隆项目
git clone https://github.com/moliyanodaxx/UChat.git
cd UChat/server

# 安装依赖
npm install

# 配置AI助手（可选）
# 编辑 chatroom-server.js 中的 AI_API_KEY

# 启动服务器
npm start
```

服务器将在 `http://localhost:10086` 启动。

### Web端访问

直接访问 `http://localhost:10086` 即可使用Web聊天室。

### Android端

1. 在 Android Studio 中打开 `android` 目录
2. 修改服务器地址配置
3. 运行到设备或模拟器

## 📚 文档

- [功能详细说明](./docs/FEATURES.md) - 完整功能介绍和使用指南
- [技术文档](./docs/TECHNICAL.md) - 架构设计、API规范、协议说明
- [开发指南](./docs/DEVELOPMENT.md) - 本地开发、贡献指南
- [部署指南](./docs/DEPLOYMENT.md) - 生产环境部署说明

## 🛠 技术栈

### 前端
- **Web**: 原生HTML/CSS/JavaScript
- **Android**: Kotlin + Jetpack Compose + Material Design 3

### 后端
- **服务器**: Node.js + WebSocket (ws)
- **AI服务**: Claude API (Sonnet 4.6)
- **数据存储**: JSON文件持久化

### 协议
- WebSocket 实时通信
- 自定义消息协议（35种消息类型）

## 🤖 AI助手 UU

UU 是 UChat 的智能助手，通过自然语言帮助用户操作：

```
"帮我创建一个技术交流房间"
"加入房间ID为1234567890"
"在当前房间发送：大家好"
"把我的昵称改成小明"
```

UU 支持的操作：
- 创建/加入房间
- 发送消息
- 更新个人资料

## 📁 项目结构

```
UChat/
├── server/              # Node.js 服务器
│   ├── chatroom-server.js
│   ├── users.json       # 用户数据
│   └── rooms.json       # 房间数据
├── web/                 # Web 客户端
│   ├── chatroom.html
│   ├── chatroom.css
│   └── chatroom.js
├── android/             # Android 客户端
│   └── app/
└── docs/               # 文档目录
    ├── FEATURES.md
    ├── TECHNICAL.md
    ├── DEVELOPMENT.md
    └── DEPLOYMENT.md
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目仅用于学习交流。

## 🔗 相关链接

- [GitHub仓库](https://github.com/moliyanodaxx/UChat)
- [问题反馈](https://github.com/moliyanodaxx/UChat/issues)
