# UChat - Android 客户端

UChat 网络聊天室的 Android 原生客户端，使用 Kotlin + Jetpack Compose 开发。

## 功能特性

- ✅ 用户注册/登录
- ✅ 个人资料设置（昵称、头像、签名）
- ✅ WebSocket 实时通信
- ✅ 房间管理（创建/加入/切换/退出）
- ✅ 实时聊天（发送/接收消息、表情）
- ✅ 好友系统（搜索/添加/好友列表）
- ✅ 房间成员管理
- ✅ 断线自动重连
- ✅ 多设备登录检测

## 技术栈

- **语言**: Kotlin
- **UI框架**: Jetpack Compose + Material Design 3
- **架构**: MVVM
- **网络**: OkHttp WebSocket
- **数据存储**: DataStore Preferences
- **依赖注入**: ViewModel

## 开发环境

- Android Studio Hedgehog | 2023.1.1+
- Kotlin 1.9+
- Gradle 8.2+
- Min SDK: 30 (Android 11)
- Target SDK: 36

## 快速开始

1. **克隆项目**
```bash
git clone https://github.com/moliyanodaxx/UChat.git
cd UChat
```

2. **打开 Android 客户端**
   - 在 Android Studio 中打开 `android-client` 目录

3. **配置服务器地址**
   - 编辑 `app/src/main/java/com/example/uchat/ui/screens/LoginScreen.kt`
   - 修改默认服务器地址：
   ```kotlin
   private const val SERVER_URL = "ws://your-server:port"
   ```

4. **运行**
   - 连接 Android 设备或启动模拟器
   - 点击 Run 按钮

## 项目结构

```
app/src/main/java/com/example/uchat/
├── data/
│   ├── local/          # 本地数据存储
│   ├── model/          # 数据模型
│   └── remote/         # WebSocket 客户端
├── ui/
│   ├── screens/        # UI 界面
│   └── theme/          # 主题配置
├── util/               # 工具类
├── viewmodel/          # ViewModel
└── MainActivity.kt     # 主入口
```

## 注意事项

⚠️ **服务端兼容性问题**：当前版本存在一些需要服务端配合修复的问题，详见 [SERVER_ISSUES.md](./SERVER_ISSUES.md)

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

本项目仅用于学习交流。
