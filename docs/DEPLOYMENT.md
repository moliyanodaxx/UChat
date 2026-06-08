# UChat 部署指南

本文档介绍如何将 UChat 部署到生产环境。

## 目录

- [部署准备](#部署准备)
- [服务器部署](#服务器部署)
- [Web端部署](#web端部署)
- [Android端发布](#android端发布)
- [安全加固](#安全加固)
- [运维监控](#运维监控)

---

## 部署准备

### 硬件要求

**最低配置**
- CPU: 1核
- 内存: 512MB
- 存储: 10GB
- 带宽: 1Mbps

**推荐配置**
- CPU: 2核+
- 内存: 2GB+
- 存储: 20GB+
- 带宽: 5Mbps+

### 软件要求

- 操作系统: Linux (Ubuntu 20.04+ / CentOS 7+)
- Node.js: 14.x 或更高
- PM2: 进程管理工具（推荐）
- Nginx: 反向代理（可选）
- SSL证书（HTTPS部署）

---

## 服务器部署

### 方式一：直接部署

**1. 安装Node.js**

```bash
# Ubuntu
curl -fsSL https://deb.nodesource.com/setup_16.x | sudo -E bash -
sudo apt-get install -y nodejs

# CentOS
curl -fsSL https://rpm.nodesource.com/setup_16.x | sudo bash -
sudo yum install -y nodejs
```

**2. 克隆项目**

```bash
cd /var/www
git clone https://github.com/moliyanodaxx/UChat.git
cd UChat/server
```

**3. 安装依赖**

```bash
npm install --production
```

**4. 配置AI Key**

编辑 `chatroom-server.js`:
```javascript
const AI_API_KEY = 'your-production-api-key';
```

**5. 启动服务**

```bash
node chatroom-server.js
```

### 方式二：使用PM2（推荐）

**1. 安装PM2**

```bash
npm install -g pm2
```

**2. 创建PM2配置**

创建 `ecosystem.config.js`:
```javascript
module.exports = {
  apps: [{
    name: 'uchat',
    script: 'chatroom-server.js',
    cwd: '/var/www/UChat/server',
    instances: 1,
    autorestart: true,
    watch: false,
    max_memory_restart: '500M',
    env: {
      NODE_ENV: 'production'
    },
    error_file: './logs/err.log',
    out_file: './logs/out.log',
    log_date_format: 'YYYY-MM-DD HH:mm:ss'
  }]
};
```

**3. 启动服务**

```bash
cd /var/www/UChat/server
pm2 start ecosystem.config.js
```

**4. 常用命令**

```bash
pm2 status          # 查看状态
pm2 logs uchat      # 查看日志
pm2 restart uchat   # 重启
pm2 stop uchat      # 停止
pm2 delete uchat    # 删除
```

**5. 开机自启**

```bash
pm2 startup
pm2 save
```

### 方式三：Docker部署

**1. 创建Dockerfile**

```dockerfile
FROM node:16-alpine
WORKDIR /app
COPY server/package*.json ./
RUN npm install --production
COPY server/ ./
EXPOSE 10086
CMD ["node", "chatroom-server.js"]
```

**2. 构建镜像**

```bash
docker build -t uchat:latest .
```

**3. 运行容器**

```bash
docker run -d \
  --name uchat \
  -p 10086:10086 \
  -v /data/uchat:/app/data \
  --restart always \
  uchat:latest
```

**4. Docker Compose**

创建 `docker-compose.yml`:
```yaml
version: '3.8'
services:
  uchat:
    image: uchat:latest
    container_name: uchat
    ports:
      - "10086:10086"
    volumes:
      - ./data:/app/data
    restart: always
```

启动:
```bash
docker-compose up -d
```

---

## Web端部署

### 方式一：与服务器一起部署

服务器已内置静态文件服务，直接访问即可。

### 方式二：独立部署

**使用Nginx托管**

1. 复制web文件到Nginx目录:
```bash
cp -r /var/www/UChat/web/* /var/www/html/uchat/
```

2. 配置Nginx:
```nginx
server {
    listen 80;
    server_name chat.example.com;

    root /var/www/html/uchat;
    index chatroom.html;

    location / {
        try_files $uri $uri/ /chatroom.html;
    }
}
```

3. 修改WebSocket连接地址:

编辑 `chatroom.js`:
```javascript
const protocol = 'wss'; // 或 'ws'
ws = new WebSocket(`${protocol}://your-server.com:10086`);
```

### HTTPS配置（推荐）

**1. 获取SSL证书**

使用Let's Encrypt:
```bash
sudo apt-get install certbot python3-certbot-nginx
sudo certbot --nginx -d chat.example.com
```

**2. Nginx配置**

```nginx
server {
    listen 443 ssl http2;
    server_name chat.example.com;

    ssl_certificate /etc/letsencrypt/live/chat.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/chat.example.com/privkey.pem;

    root /var/www/html/uchat;
    index chatroom.html;

    # WebSocket代理
    location /ws {
        proxy_pass http://localhost:10086;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}

# HTTP重定向
server {
    listen 80;
    server_name chat.example.com;
    return 301 https://$server_name$request_uri;
}
```

**3. 自动续期**

```bash
sudo certbot renew --dry-run
```

---

## Android端发布

### 构建发布版本

**1. 配置签名**

创建 `keystore`:
```bash
keytool -genkey -v -keystore uchat-release.keystore \
  -alias uchat -keyalg RSA -keysize 2048 -validity 10000
```

**2. 配置build.gradle**

```gradle
android {
    signingConfigs {
        release {
            storeFile file("uchat-release.keystore")
            storePassword "your-password"
            keyAlias "uchat"
            keyPassword "your-password"
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

**3. 修改服务器地址**

将开发环境地址改为生产地址。

**4. 构建APK**

```bash
./gradlew assembleRelease
```

生成的APK位于: `app/build/outputs/apk/release/`

### 发布渠道

- Google Play Store
- 应用宝
- 小米应用商店
- 华为应用市场
- 其他第三方应用商店

---

## 安全加固

### 服务器安全

**1. 密码加密**

⚠️ 当前版本密码明文存储，生产环境应使用bcrypt:

```javascript
const bcrypt = require('bcrypt');

// 注册时加密
const hashedPassword = await bcrypt.hash(password, 10);

// 登录时验证
const isValid = await bcrypt.compare(password, user.password);
```

**2. API Key保护**

不要将API Key硬编码，使用环境变量:

```bash
export AI_API_KEY='your-api-key'
```

```javascript
const AI_API_KEY = process.env.AI_API_KEY;
```

**3. 速率限制**

防止API滥用:
```javascript
const rateLimit = require('express-rate-limit');

const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15分钟
    max: 100 // 限制100次请求
});
```

**4. 防火墙配置**

```bash
# UFW示例
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw allow 10086/tcp   # UChat
sudo ufw enable
```

### 数据安全

**1. 备份策略**

定时备份用户数据:
```bash
#!/bin/bash
DATE=$(date +%Y%m%d)
tar -czf /backup/uchat-$DATE.tar.gz /var/www/UChat/server/*.json
find /backup -name "uchat-*.tar.gz" -mtime +7 -delete
```

添加到crontab:
```bash
0 2 * * * /path/to/backup.sh
```

**2. 日志管理**

配置日志轮转:
```bash
# /etc/logrotate.d/uchat
/var/www/UChat/server/logs/*.log {
    daily
    rotate 7
    compress
    delaycompress
    notifempty
    create 0640 www-data www-data
}
```

### 网络安全

**1. 使用HTTPS/WSS**

所有通信使用加密连接。

**2. CORS配置**

限制跨域访问:
```javascript
res.setHeader('Access-Control-Allow-Origin', 'https://chat.example.com');
```

**3. DDoS防护**

使用Cloudflare或阿里云DDoS高防。

---

## 运维监控

### 系统监控

**1. PM2监控**

```bash
pm2 monit
```

**2. 系统资源**

```bash
# 内存使用
free -h

# CPU使用
top

# 磁盘使用
df -h
```

### 日志分析

**查看PM2日志**
```bash
pm2 logs uchat --lines 100
```

**实时监控**
```bash
tail -f /var/www/UChat/server/logs/out.log
```

### 性能优化

**1. 启用Gzip压缩**

Nginx配置:
```nginx
gzip on;
gzip_types text/plain text/css application/json application/javascript;
gzip_min_length 1000;
```

**2. 开启缓存**

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

**3. 数据库优化**

考虑使用Redis存储会话:
```javascript
const redis = require('redis');
const client = redis.createClient();
```

### 故障恢复

**1. 自动重启**

PM2自带自动重启功能。

**2. 健康检查**

创建健康检查端点:
```javascript
app.get('/health', (req, res) => {
    res.json({ status: 'ok', uptime: process.uptime() });
});
```

**3. 告警通知**

配置PM2-logrotate和邮件告警。

---

## 维护任务

### 日常维护

- [ ] 检查服务器状态
- [ ] 查看错误日志
- [ ] 监控资源使用
- [ ] 备份数据

### 定期维护

**每周**
- 更新系统安全补丁
- 检查磁盘空间
- 分析用户增长

**每月**
- 数据库清理
- 日志归档
- 性能评估

### 版本更新

```bash
cd /var/www/UChat
git pull origin main
cd server
npm install
pm2 restart uchat
```

---

## 故障排查

### 常见问题

**服务器无法启动**
- 检查端口是否被占用: `lsof -i :10086`
- 检查Node.js版本: `node -v`
- 查看错误日志: `pm2 logs`

**WebSocket连接失败**
- 检查防火墙规则
- 检查Nginx配置
- 验证SSL证书

**AI功能不工作**
- 验证API Key有效性
- 检查网络连接
- 查看API调用日志

**数据丢失**
- 恢复最近的备份
- 检查文件权限
- 验证磁盘空间

---

## 扩展部署

### 负载均衡

使用Nginx实现负载均衡:

```nginx
upstream uchat_backend {
    server 127.0.0.1:10086;
    server 127.0.0.1:10087;
    server 127.0.0.1:10088;
}

server {
    location / {
        proxy_pass http://uchat_backend;
    }
}
```

### 多实例部署

```bash
pm2 start ecosystem.config.js -i 4  # 4个实例
```

### 分布式部署

1. 使用Redis存储共享数据
2. 使用消息队列同步消息
3. 配置负载均衡
4. 实现会话粘性

---

## 成本估算

### 云服务器（按月）

**基础版**
- 1核2G: ¥50-100/月
- 带宽1M: 包含
- 存储20G: 包含
- 总计: ¥50-100/月

**标准版**
- 2核4G: ¥100-200/月
- 带宽5M: ¥30-50/月
- 存储50G: ¥10/月
- 总计: ¥140-260/月

**AI API费用**
- Claude API: 按使用量计费
- 预估: ¥50-200/月（取决于使用量）

---

## 检查清单

### 部署前

- [ ] 代码已测试
- [ ] API Key已配置
- [ ] 服务器已准备
- [ ] 域名已解析
- [ ] SSL证书已配置

### 部署后

- [ ] 服务正常运行
- [ ] WebSocket连接正常
- [ ] AI功能测试通过
- [ ] 备份任务已设置
- [ ] 监控已配置

---

## 参考资源

- [PM2文档](https://pm2.keymetrics.io/)
- [Nginx文档](https://nginx.org/en/docs/)
- [Let's Encrypt](https://letsencrypt.org/)
- [Docker文档](https://docs.docker.com/)
