const http = require('http');
const fs = require('fs');
const path = require('path');
const WebSocket = require('ws');
const crypto = require('crypto');
const axios = require('axios');

const PORT = 10086;
const USERS_FILE = path.join(__dirname, 'users.json');
const ROOMS_FILE = path.join(__dirname, 'rooms.json');

// 消息类型
const TYPE_ENTER = 0;
const TYPE_LEAVE = 1;
const TYPE_MSG = 2;
const TYPE_HEARTBEAT = 3;
const TYPE_SET_USERNAME = 4;
const TYPE_CREATE_ROOM = 5;
const TYPE_JOIN_ROOM = 6;
const TYPE_LEAVE_ROOM = 7;
const TYPE_ROOM_LIST = 8;
const TYPE_ROOM_MEMBERS = 9;
const TYPE_KICK_MEMBER = 10;
const TYPE_TRANSFER_OWNER = 11;
const TYPE_SWITCH_ROOM = 12;
const TYPE_HISTORY = 13;
const TYPE_REGISTER = 14;
const TYPE_LOGIN = 15;
const TYPE_SET_PROFILE = 16;
const TYPE_SEARCH_USER = 17;
const TYPE_GET_USER_INFO = 18;
const TYPE_SEND_FRIEND_REQUEST = 19;
const TYPE_GET_FRIEND_REQUESTS = 20;
const TYPE_HANDLE_FRIEND_REQUEST = 21;
const TYPE_GET_FRIENDS = 22;
const TYPE_UPDATE_PROFILE = 23;
const TYPE_GET_ROOM_INFO = 24;
const TYPE_UPDATE_ROOM_PASSWORD = 25;
const TYPE_INVITE_TO_ROOM = 26;
const TYPE_PRIVATE_CHAT = 27;
const TYPE_UPDATE_INVITE_STATUS = 28;
const TYPE_ADD_BANNED_WORD = 29;
const TYPE_REMOVE_BANNED_WORD = 30;
const TYPE_GET_BANNED_WORDS = 31;
const TYPE_BANNED_WORDS_LIST = 32;
const TYPE_MSG_BLOCKED = 33;
const TYPE_AI_REQUEST = 34;
const TYPE_AI_RESPONSE = 35;

const MAX_HISTORY = 100;
const SYSTEM_ROOM_ID = '0000000000';
const SYSTEM_MSG_ROOM_ID = '0000000001';

const AI_API_URL = 'https://cn.luckyapi.chat/v1/messages';
const AI_API_KEY = 'YOUR AI_API_KEY';
const AI_MODEL = 'claude-sonnet-4-6'; // 系统消息房间

// ===== 持久化 =====
let usersData = {};   // { accountId: { accountId, password, nickname, avatar, rooms[] } }
let roomsData = {};   // { roomId: { id, name, password, owner, isSystem, createdAt } }

function loadData() {
    try {
        if (fs.existsSync(USERS_FILE)) usersData = JSON.parse(fs.readFileSync(USERS_FILE, 'utf8'));
    } catch (e) { console.error('加载用户数据失败:', e); }

    try {
        if (fs.existsSync(ROOMS_FILE)) roomsData = JSON.parse(fs.readFileSync(ROOMS_FILE, 'utf8'));
    } catch (e) { console.error('加载房间数据失败:', e); }
}

function saveUsers() {
    try { fs.writeFileSync(USERS_FILE, JSON.stringify(usersData, null, 2)); }
    catch (e) { console.error('保存用户数据失败:', e); }
}

function saveRoomsData() {
    try {
        const data = {};
        rooms.forEach((room, id) => {
            data[id] = { id: room.id, name: room.name, password: room.password, owner: room.owner, isSystem: room.isSystem, createdAt: room.createdAt };
        });
        console.log('准备保存房间数据，房间数量:', Object.keys(data).length, '房间列表:', Object.keys(data));
        fs.writeFileSync(ROOMS_FILE, JSON.stringify(data, null, 2));
        console.log('房间数据已写入文件');
    } catch (e) { console.error('保存房间数据失败:', e); }
}

function hashPassword(password) {
    return crypto.createHash('sha256').update(password).digest('hex');
}

// ===== 初始化 =====
loadData();

// 内存中的房间（含成员集合和历史消息）
const rooms = new Map();

// 从持久化数据恢复所有房间
Object.values(roomsData).forEach(r => {
    rooms.set(r.id, { ...r, members: new Set(), history: [], bannedWords: new Set() });
    console.log('恢复房间:', r.id, r.name);
});

// 如果没有系统房间，创建它
if (!rooms.has(SYSTEM_ROOM_ID)) {
    const sysRoom = {
        id: SYSTEM_ROOM_ID, name: '在线聊天室', password: '',
        owner: 'system', members: new Set(), isSystem: true,
        createdAt: Date.now(), history: [], bannedWords: new Set()
    };
    rooms.set(SYSTEM_ROOM_ID, sysRoom);
    saveRoomsData();
    console.log('创建系统房间');
}

// 创建系统消息房间
if (!rooms.has(SYSTEM_MSG_ROOM_ID)) {
    const sysMsgRoom = {
        id: SYSTEM_MSG_ROOM_ID, name: '系统消息', password: '',
        owner: 'system', members: new Set(), isSystem: true,
        createdAt: Date.now(), history: [], bannedWords: new Set()
    };
    rooms.set(SYSTEM_MSG_ROOM_ID, sysMsgRoom);
    saveRoomsData();
    console.log('创建系统消息房间');
}

let connections = [];
let uid = 0;

function generateRoomId() {
    return Math.floor(1000000000 + Math.random() * 9000000000).toString();
}

// ===== AI助手 =====
function executeTool(ws, toolName, input) {
    try {
        if (toolName === 'create_room') {
            const roomId = generateRoomId();
            const room = { id: roomId, name: input.name, password: input.password || '', owner: ws.uid, members: new Set([ws.uid]), createdAt: Date.now(), history: [], bannedWords: new Set() };
            rooms.set(roomId, room);
            ws.rooms.add(roomId);
            saveRoomsData();
            return { success: true, message: `已创建房间"${input.name}"，房间ID为${roomId}` };
        }
        if (toolName === 'join_room') {
            const room = rooms.get(input.roomId);
            if (!room) return { success: false, message: '房间不存在' };
            if (room.password && room.password !== input.password) return { success: false, message: '密码错误' };
            ws.rooms.add(input.roomId);
            room.members.add(ws.uid);
            return { success: true, message: `已加入房间"${room.name}"` };
        }
        if (toolName === 'send_message') {
            const room = rooms.get(input.roomId);
            if (!room) return { success: false, message: '房间不存在' };
            if (!ws.rooms.has(input.roomId)) return { success: false, message: '你不在这个房间' };
            const msg = { type: TYPE_MSG, username: ws.username, msg: input.message, time: new Date().toLocaleTimeString(), accountId: ws.accountId };
            room.history.push(msg);
            connections.forEach(c => { if (c.rooms.has(input.roomId) && c.readyState === WebSocket.OPEN) try { c.send(JSON.stringify(msg)); } catch (e) {} });
            return { success: true, message: '消息已发送' };
        }
        if (toolName === 'update_profile') {
            const user = usersData[ws.accountId];
            if (!user) return { success: false, message: '用户不存在' };
            if (input.nickname) { user.nickname = input.nickname; ws.username = input.nickname; }
            if (input.avatar) { user.avatar = input.avatar; ws.avatar = input.avatar; }
            if (input.signature !== undefined) user.signature = input.signature;
            saveUsersData();
            return { success: true, message: '资料已更新' };
        }
        return { success: false, message: '未知工具' };
    } catch (e) {
        return { success: false, message: '执行失败: ' + e.message };
    }
}

async function handleAIRequest(ws, userMessage) {
    try {
        const response = await axios.post(AI_API_URL, {
            model: AI_MODEL,
            max_tokens: 1024,
            system: '你叫UU，是UChat在线聊天室的专属AI助手。你不是Kiro，也不是其他助手。你的唯一职责是帮助用户使用UChat的功能：创建房间、加入房间、发送消息、更新资料等。请用友好、简洁的方式回应，并积极使用提供的工具来完成用户的请求。',
            messages: [{ role: 'user', content: userMessage }],
            tools: [
                { name: 'create_room', description: '创建聊天室', input_schema: { type: 'object', properties: { name: { type: 'string', description: '房间名称' }, password: { type: 'string', description: '房间密码(可选)' } }, required: ['name'] } },
                { name: 'join_room', description: '加入聊天室', input_schema: { type: 'object', properties: { roomId: { type: 'string', description: '房间ID' }, password: { type: 'string', description: '房间密码(可选)' } }, required: ['roomId'] } },
                { name: 'send_message', description: '发送消息到指定房间', input_schema: { type: 'object', properties: { roomId: { type: 'string', description: '房间ID' }, message: { type: 'string', description: '消息内容' } }, required: ['roomId', 'message'] } },
                { name: 'update_profile', description: '更新个人资料', input_schema: { type: 'object', properties: { nickname: { type: 'string', description: '昵称' }, avatar: { type: 'string', description: '头像' }, signature: { type: 'string', description: '个性签名' } } } }
            ]
        }, { headers: { 'x-api-key': AI_API_KEY, 'anthropic-version': '2023-06-01', 'content-type': 'application/json' } });

        const content = response.data.content;
        const toolUse = content.find(c => c.type === 'tool_use');
        if (toolUse) {
            const result = executeTool(ws, toolUse.name, toolUse.input);
            return { success: true, response: result.message };
        }
        const textContent = content.find(c => c.type === 'text');
        return { success: true, response: textContent?.text || '我不确定如何帮助你' };
    } catch (e) {
        console.error('AI请求失败:', e.message);
        return { success: false, message: 'AI服务异常' };
    }
}

// ===== HTTP 服务 =====
const server = http.createServer((req, res) => {
    const urlPath = req.url.split('?')[0];
    let filePath, contentType;

    if (urlPath === '/') {
        filePath = path.join(__dirname, '../web/chatroom.html');
        contentType = 'text/html';
    } else if (urlPath.endsWith('.css')) {
        filePath = path.join(__dirname, '../web', urlPath);
        contentType = 'text/css';
    } else if (urlPath.endsWith('.js')) {
        filePath = path.join(__dirname, '../web', urlPath);
        contentType = 'application/javascript';
    } else {
        res.writeHead(404);
        return res.end('文件未找到');
    }

    fs.readFile(filePath, (err, data) => {
        if (err) { res.writeHead(500); return res.end('服务器错误'); }
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(data);
    });
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`服务器运行在端口 ${PORT}`);
});

// ===== WebSocket =====
const wss = new WebSocket.Server({ server });

wss.on('connection', ws => {
    ws.username = '';
    ws.isAlive = true;
    ws.currentRoom = null;
    ws.rooms = new Set();
    ws.isAuthenticated = false;
    ws.accountId = null;
    ws.uid = null; // uid 将在登录时设置为 accountId
    connections.push(ws);

    // ---- 工具函数 ----
    const send = (msg) => {
        if (ws.readyState === WebSocket.OPEN) {
            try { ws.send(JSON.stringify(msg)); } catch (e) {}
        }
    };

    const broadcast = (roomId, msg, excludeUid = null) => {
        const data = JSON.stringify(msg);
        const room = rooms.get(roomId);
        if (!room) return;
        connections.forEach(c => {
            if (c.currentRoom === roomId && c.readyState === WebSocket.OPEN && c.uid !== excludeUid) {
                try { c.send(data); } catch (e) {}
            }
        });
    };

    const getRoomMembers = (roomId) => {
        const room = rooms.get(roomId);
        if (!room) return [];
        return connections
            .filter(c => c.rooms.has(roomId))
            .map(c => ({ uid: c.uid, username: c.username, avatar: c.avatar || '', signature: c.signature || '', isOnline: c.readyState === WebSocket.OPEN, isOwner: room.owner === c.uid }));
    };

    const saveHistory = (roomId, msg) => {
        const room = rooms.get(roomId);
        if (!room) return;
        room.history.push(msg);
        if (room.history.length > MAX_HISTORY) room.history.shift();
    };

    const sendHistory = (roomId) => {
        const room = rooms.get(roomId);
        if (!room) return;

        // 如果是系统消息房间，发送用户专属的消息
        if (roomId === SYSTEM_MSG_ROOM_ID) {
            const userMessages = room.userMessages?.[ws.accountId] || [];
            send({ type: TYPE_HISTORY, roomId, messages: userMessages });
        } else {
            if (!room.history.length) return;
            send({ type: TYPE_HISTORY, roomId, messages: room.history });
        }
    };

    // 发送用户的房间列表
    const sendRoomList = () => {
        const roomsList = Array.from(ws.rooms)
            .map(roomId => {
                const room = rooms.get(roomId);
                if (!room) return null;
                return { id: room.id, name: room.name, isSystem: room.isSystem, memberCount: room.members.size };
            })
            .filter(r => r !== null);

        send({ type: TYPE_ROOM_LIST, rooms: roomsList, currentRoom: ws.currentRoom });
    };

    ws.on('pong', () => { ws.isAlive = true; });

    ws.on('message', raw => {
        try {
            const msg = JSON.parse(raw);

            // ---- 注册 ----
            if (msg.type === TYPE_REGISTER) {
                const { accountId, password } = msg;
                if (!accountId || accountId.length !== 10 || !/^[a-zA-Z0-9]+$/.test(accountId)) {
                    send({ type: TYPE_REGISTER, success: false, error: '账号必须是10位字母或数字' }); return;
                }
                if (!password || password.length < 8 || password.length > 10 || !/^[a-zA-Z0-9]+$/.test(password)) {
                    send({ type: TYPE_REGISTER, success: false, error: '密码必须是8-10位字母或数字' }); return;
                }
                if (usersData[accountId]) {
                    send({ type: TYPE_REGISTER, success: false, error: '账号已存在' }); return;
                }
                usersData[accountId] = {
                    accountId, password: hashPassword(password),
                    nickname: '', avatar: '', signature: '',
                    friends: [], friendRequests: [],
                    rooms: [SYSTEM_ROOM_ID, SYSTEM_MSG_ROOM_ID], createdAt: Date.now()
                };
                saveUsers();
                ws.isAuthenticated = true;
                ws.accountId = accountId;
                ws.uid = accountId; // 注册时也设置 uid
                send({ type: TYPE_REGISTER, success: true });
                return;
            }

            // ---- 登录 ----
            if (msg.type === TYPE_LOGIN) {
                const { accountId, password } = msg;
                if (!usersData[accountId]) {
                    send({ type: TYPE_LOGIN, success: false, error: '账号不存在' }); return;
                }
                if (usersData[accountId].password !== hashPassword(password)) {
                    send({ type: TYPE_LOGIN, success: false, error: '密码错误' }); return;
                }

                // 多设备登录检测：踢掉旧连接
                const oldConn = connections.find(c => c.accountId === accountId && c !== ws);
                if (oldConn && oldConn.readyState === WebSocket.OPEN) {
                    try {
                        oldConn.send(JSON.stringify({ type: 27, reason: '账号在其他设备登录' }));
                        oldConn.close();
                    } catch (e) {}
                }

                ws.isAuthenticated = true;
                ws.accountId = accountId;
                ws.uid = accountId; // 立即设置 uid 为 accountId
                const userData = usersData[accountId];
                const needProfile = !userData.nickname || !userData.avatar;
                send({ type: TYPE_LOGIN, success: true, needProfile, userData: {
                    accountId: userData.accountId,
                    nickname: userData.nickname || '',
                    avatar: userData.avatar || '',
                    signature: userData.signature || ''
                }});
                return;
            }

            // 以下需要认证
            if (!ws.isAuthenticated) { send({ error: '请先登录' }); return; }

            // ---- 设置资料（首次）----
            if (msg.type === TYPE_SET_PROFILE) {
                const { nickname, avatar } = msg;
                if (!nickname || !nickname.trim()) { send({ type: TYPE_SET_PROFILE, success: false, error: '昵称不能为空' }); return; }
                if (!avatar || !avatar.trim()) { send({ type: TYPE_SET_PROFILE, success: false, error: '请选择头像' }); return; }

                usersData[ws.accountId].nickname = nickname.trim();
                usersData[ws.accountId].avatar = avatar.trim();
                saveUsers();

                send({ type: TYPE_SET_PROFILE, success: true, userData: {
                    nickname: nickname.trim(), avatar: avatar.trim()
                }});
                return;
            }

            // ---- 设置用户名（登录/设置资料后调用，触发发送房间列表）----
            if (msg.type === TYPE_SET_USERNAME && msg.username) {
                ws.username = msg.username;
                ws.uid = ws.accountId; // 使用 accountId 作为 uid，保持房主身份

                // 恢复用户的房间
                const userRooms = usersData[ws.accountId]?.rooms || [SYSTEM_ROOM_ID];

                // 确保用户加入了系统消息房间
                if (!userRooms.includes(SYSTEM_MSG_ROOM_ID)) {
                    userRooms.push(SYSTEM_MSG_ROOM_ID);
                    if (usersData[ws.accountId]) {
                        usersData[ws.accountId].rooms = userRooms;
                        saveUsers();
                    }
                }

                ws.rooms = new Set();
                ws.currentRoom = SYSTEM_ROOM_ID;

                userRooms.forEach(roomId => {
                    const room = rooms.get(roomId);
                    if (room) {
                        ws.rooms.add(roomId);
                        room.members.add(ws.uid);
                    } else {
                        console.log('房间不存在，从用户数据中移除:', roomId);
                        // 房间不存在，从用户数据中移除
                        if (usersData[ws.accountId]) {
                            usersData[ws.accountId].rooms = usersData[ws.accountId].rooms.filter(r => r !== roomId);
                        }
                    }
                });

                // 确保至少在系统房间
                if (!ws.rooms.has(SYSTEM_ROOM_ID)) {
                    ws.rooms.add(SYSTEM_ROOM_ID);
                    rooms.get(SYSTEM_ROOM_ID).members.add(ws.uid);
                }

                // 保存清理后的用户数据
                saveUsers();

                // 广播进入消息到当前房间
                const enterMsg = {
                    type: TYPE_ENTER,
                    msg: `${ws.username} 进入聊天室`,
                    time: new Date().toLocaleTimeString(),
                    onlineCount: getRoomMembers(ws.currentRoom).length
                };
                broadcast(ws.currentRoom, enterMsg);
                // ENTER消息不保存到历史记录

                // 发送房间列表
                sendRoomList();

                // 发送当前房间历史
                sendHistory(ws.currentRoom);
                return;
            }

            // ---- 创建房间 ----
            if (msg.type === TYPE_CREATE_ROOM) {
                const roomId = msg.roomId || generateRoomId();
                console.log('创建房间请求:', roomId, msg.roomName);

                if (rooms.has(roomId)) {
                    console.log('房间号已存在:', roomId);
                    send({ type: TYPE_CREATE_ROOM, success: false, error: '房间号已存在' });
                    return;
                }

                const newRoom = {
                    id: roomId, name: msg.roomName || `房间${roomId}`,
                    password: msg.password, owner: ws.uid,
                    members: new Set([ws.uid]), isSystem: false,
                    createdAt: Date.now(), history: [], bannedWords: new Set()
                };
                rooms.set(roomId, newRoom);
                ws.rooms.add(roomId);

                console.log('房间已创建:', roomId, '当前房间总数:', rooms.size);

                // 保存
                if (usersData[ws.accountId] && !usersData[ws.accountId].rooms.includes(roomId)) {
                    usersData[ws.accountId].rooms.push(roomId);
                    saveUsers();
                    console.log('用户房间列表已更新');
                }
                saveRoomsData();
                console.log('房间数据已保存到文件');

                send({ type: TYPE_CREATE_ROOM, success: true, room: {
                    id: newRoom.id, name: newRoom.name, isSystem: false, memberCount: 1
                }});
                return;
            }

            // ---- 加入房间 ----
            if (msg.type === TYPE_JOIN_ROOM) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_JOIN_ROOM, success: false, error: '房间不存在' }); return; }
                // 如果有密码且不为空字符串，则验证密码
                if (room.password && msg.password !== '' && room.password !== msg.password) {
                    send({ type: TYPE_JOIN_ROOM, success: false, error: '密码错误' });
                    return;
                }
                if (ws.rooms.has(msg.roomId)) { send({ type: TYPE_JOIN_ROOM, success: false, error: '已在该房间中' }); return; }

                room.members.add(ws.uid);
                ws.rooms.add(msg.roomId);

                if (usersData[ws.accountId] && !usersData[ws.accountId].rooms.includes(msg.roomId)) {
                    usersData[ws.accountId].rooms.push(msg.roomId);
                    saveUsers();
                }

                send({ type: TYPE_JOIN_ROOM, success: true, room: {
                    id: room.id, name: room.name, isSystem: room.isSystem, memberCount: room.members.size
                }});

                const joinMsg = { type: TYPE_ENTER, msg: `${ws.username} 加入房间`, time: new Date().toLocaleTimeString(), onlineCount: getRoomMembers(msg.roomId).length };
                broadcast(msg.roomId, joinMsg, ws.uid);
                // ENTER消息不保存到历史记录
                sendHistory(msg.roomId);
                return;
            }

            // ---- 切换房间 ----
            if (msg.type === TYPE_SWITCH_ROOM) {
                if (!ws.rooms.has(msg.roomId)) { send({ type: TYPE_SWITCH_ROOM, success: false, error: '未加入该房间' }); return; }
                ws.currentRoom = msg.roomId;
                send({ type: TYPE_SWITCH_ROOM, success: true, roomId: msg.roomId, onlineCount: getRoomMembers(msg.roomId).length });
                sendHistory(msg.roomId);
                return;
            }

            // ---- 退出房间 ----
            if (msg.type === TYPE_LEAVE_ROOM) {
                const room = rooms.get(msg.roomId);
                if (!room || room.isSystem) { send({ type: TYPE_LEAVE_ROOM, success: false, error: '无法退出该房间' }); return; }

                room.members.delete(ws.uid);
                ws.rooms.delete(msg.roomId);

                if (usersData[ws.accountId]) {
                    usersData[ws.accountId].rooms = usersData[ws.accountId].rooms.filter(r => r !== msg.roomId);
                    saveUsers();
                }

                // 如果是房主退出，自动转让给最早加入的成员
                if (room.owner === ws.uid && room.members.size > 0) {
                    const newOwner = Array.from(room.members)[0];
                    room.owner = newOwner;
                    saveRoomsData();
                    const transferMsg = { type: TYPE_TRANSFER_OWNER, msg: `房主已自动转让`, time: new Date().toLocaleTimeString() };
                    broadcast(msg.roomId, transferMsg);
                    saveHistory(msg.roomId, transferMsg);
                }

                const leaveMsg = { type: TYPE_LEAVE, msg: `${ws.username} 退出房间`, time: new Date().toLocaleTimeString(), onlineCount: getRoomMembers(msg.roomId).length };
                broadcast(msg.roomId, leaveMsg);
                // LEAVE消息不保存到历史记录
                send({ type: TYPE_LEAVE_ROOM, success: true, roomId: msg.roomId });

                if (room.members.size === 0) { rooms.delete(msg.roomId); saveRoomsData(); }
                return;
            }

            // ---- 获取房间成员 ----
            if (msg.type === TYPE_ROOM_MEMBERS) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_ROOM_MEMBERS, success: false, error: '房间不存在' }); return; }
                send({ type: TYPE_ROOM_MEMBERS, success: true, roomId: msg.roomId, members: getRoomMembers(msg.roomId), isOwner: room.owner === ws.uid });
                return;
            }

            // ---- 踢出成员 ----
            if (msg.type === TYPE_KICK_MEMBER) {
                const room = rooms.get(msg.roomId);
                if (!room || room.owner !== ws.uid) { send({ type: TYPE_KICK_MEMBER, success: false, error: '无权限' }); return; }
                const target = connections.find(c => c.uid === msg.targetUid);
                if (!target) { send({ type: TYPE_KICK_MEMBER, success: false, error: '用户不存在' }); return; }

                room.members.delete(msg.targetUid);
                target.rooms.delete(msg.roomId);
                send({ type: TYPE_KICK_MEMBER, success: true });
                if (target.readyState === WebSocket.OPEN) {
                    try { target.send(JSON.stringify({ type: TYPE_LEAVE_ROOM, success: true, roomId: msg.roomId, kicked: true })); } catch (e) {}
                }
                const kickMsg = { type: TYPE_LEAVE, msg: `${target.username} 被移出房间`, time: new Date().toLocaleTimeString(), onlineCount: getRoomMembers(msg.roomId).length };
                broadcast(msg.roomId, kickMsg);
                // LEAVE消息不保存到历史记录
                return;
            }

            // ---- 转让房主 ----
            if (msg.type === TYPE_TRANSFER_OWNER) {
                const room = rooms.get(msg.roomId);
                if (!room || room.owner !== ws.uid) { send({ type: TYPE_TRANSFER_OWNER, success: false, error: '无权限' }); return; }
                if (!room.members.has(msg.targetUid)) { send({ type: TYPE_TRANSFER_OWNER, success: false, error: '目标用户不在房间中' }); return; }
                room.owner = msg.targetUid;
                send({ type: TYPE_TRANSFER_OWNER, success: true });
                const transferMsg = { type: TYPE_ENTER, msg: '房主已转让', time: new Date().toLocaleTimeString() };
                broadcast(msg.roomId, transferMsg);
                saveHistory(msg.roomId, transferMsg);
                return;
            }

            // ---- 心跳 ----
            if (msg.type === TYPE_HEARTBEAT) {
                send({ type: TYPE_HEARTBEAT }); return;
            }

            // ---- 搜索用户 ----
            if (msg.type === TYPE_SEARCH_USER) {
                const keyword = msg.keyword?.trim();
                if (!keyword) { send({ type: TYPE_SEARCH_USER, success: false, error: '请输入搜索关键词' }); return; }

                const results = Object.values(usersData)
                    .filter(u => u.nickname && (u.accountId.includes(keyword) || u.nickname.includes(keyword)))
                    .slice(0, 20)
                    .map(u => ({ accountId: u.accountId, nickname: u.nickname, avatar: u.avatar, signature: u.signature || '' }));

                send({ type: TYPE_SEARCH_USER, success: true, results });
                return;
            }

            // ---- 获取用户信息 ----
            if (msg.type === TYPE_GET_USER_INFO) {
                const targetUser = usersData[msg.accountId];
                if (!targetUser || !targetUser.nickname) {
                    send({ type: TYPE_GET_USER_INFO, success: false, error: '用户不存在' }); return;
                }

                const isFriend = usersData[ws.accountId]?.friends?.includes(msg.accountId) || false;
                send({
                    type: TYPE_GET_USER_INFO, success: true,
                    user: {
                        accountId: targetUser.accountId,
                        nickname: targetUser.nickname,
                        avatar: targetUser.avatar,
                        signature: targetUser.signature || '',
                        isFriend
                    }
                });
                return;
            }

            // ---- 发送好友请求 ----
            if (msg.type === TYPE_SEND_FRIEND_REQUEST) {
                const targetUser = usersData[msg.targetAccountId];
                if (!targetUser) { send({ type: TYPE_SEND_FRIEND_REQUEST, success: false, error: '用户不存在' }); return; }
                if (msg.targetAccountId === ws.accountId) {
                    send({ type: TYPE_SEND_FRIEND_REQUEST, success: false, error: '不能添加自己为好友' }); return;
                }

                const myData = usersData[ws.accountId];
                if (myData.friends?.includes(msg.targetAccountId)) {
                    send({ type: TYPE_SEND_FRIEND_REQUEST, success: false, error: '已经是好友了' }); return;
                }

                // 检查是否已发送过请求
                if (targetUser.friendRequests?.some(r => r.from === ws.accountId)) {
                    send({ type: TYPE_SEND_FRIEND_REQUEST, success: false, error: '已发送过好友请求' }); return;
                }

                // 添加好友请求
                if (!targetUser.friendRequests) targetUser.friendRequests = [];
                targetUser.friendRequests.push({
                    from: ws.accountId,
                    fromNickname: ws.username,
                    fromAvatar: myData.avatar,
                    time: Date.now()
                });
                saveUsers();

                send({ type: TYPE_SEND_FRIEND_REQUEST, success: true });

                // 通知对方（如果在线）
                const targetConn = connections.find(c => c.accountId === msg.targetAccountId);
                if (targetConn && targetConn.readyState === WebSocket.OPEN) {
                    try {
                        targetConn.send(JSON.stringify({ type: 'FRIEND_REQUEST_RECEIVED', from: ws.accountId, fromNickname: ws.username }));
                    } catch (e) {}
                }
                return;
            }

            // ---- 获取好友请求列表 ----
            if (msg.type === TYPE_GET_FRIEND_REQUESTS) {
                const requests = usersData[ws.accountId]?.friendRequests || [];
                send({ type: TYPE_GET_FRIEND_REQUESTS, success: true, requests });
                return;
            }

            // ---- 处理好友请求（接受/拒绝）----
            if (msg.type === TYPE_HANDLE_FRIEND_REQUEST) {
                const myData = usersData[ws.accountId];
                const request = myData.friendRequests?.find(r => r.from === msg.fromAccountId);

                if (!request) {
                    send({ type: TYPE_HANDLE_FRIEND_REQUEST, success: false, error: '请求不存在' }); return;
                }

                // 移除请求
                myData.friendRequests = myData.friendRequests.filter(r => r.from !== msg.fromAccountId);

                if (msg.accept) {
                    // 接受好友请求
                    if (!myData.friends) myData.friends = [];
                    if (!myData.friends.includes(msg.fromAccountId)) {
                        myData.friends.push(msg.fromAccountId);
                    }

                    const friendData = usersData[msg.fromAccountId];
                    if (friendData) {
                        if (!friendData.friends) friendData.friends = [];
                        if (!friendData.friends.includes(ws.accountId)) {
                            friendData.friends.push(ws.accountId);
                        }
                    }
                }

                saveUsers();
                send({ type: TYPE_HANDLE_FRIEND_REQUEST, success: true, accepted: msg.accept });
                return;
            }

            // ---- 获取好友列表 ----
            if (msg.type === TYPE_GET_FRIENDS) {
                const friendIds = usersData[ws.accountId]?.friends || [];
                const friends = friendIds
                    .map(id => usersData[id])
                    .filter(u => u && u.nickname)
                    .map(u => ({
                        accountId: u.accountId,
                        nickname: u.nickname,
                        avatar: u.avatar,
                        signature: u.signature || '',
                        online: connections.some(c => c.accountId === u.accountId && c.readyState === WebSocket.OPEN)
                    }));

                send({ type: TYPE_GET_FRIENDS, success: true, friends });
                return;
            }

            // ---- 更新个人资料 ----
            if (msg.type === TYPE_UPDATE_PROFILE) {
                const { nickname, avatar, signature } = msg;
                const myData = usersData[ws.accountId];

                if (nickname && nickname.trim()) {
                    myData.nickname = nickname.trim();
                    ws.username = nickname.trim();
                }
                if (avatar && avatar.trim()) {
                    myData.avatar = avatar.trim();
                }
                if (signature !== undefined) {
                    myData.signature = signature.trim();
                }

                saveUsers();
                send({ type: TYPE_UPDATE_PROFILE, success: true, user: {
                    nickname: myData.nickname,
                    avatar: myData.avatar,
                    signature: myData.signature
                }});
                return;
            }

            // ---- 普通消息 ----
            if (msg.type === TYPE_MSG) {
                const room = rooms.get(ws.currentRoom);
                if (room) {
                    // 检查违禁词
                    for (const word of room.bannedWords) {
                        if (msg.msg.includes(word)) {
                            send({ type: TYPE_MSG_BLOCKED, message: `消息包含违禁词：${word}` });
                            return;
                        }
                    }
                }
                const userData = usersData[ws.accountId];
                const msgData = {
                    type: TYPE_MSG,
                    username: ws.username,
                    accountId: ws.accountId,
                    avatar: userData?.avatar || '',
                    msg: msg.msg,
                    time: new Date().toLocaleTimeString()
                };
                broadcast(ws.currentRoom, msgData);
                saveHistory(ws.currentRoom, msgData);
            }

            // ---- 获取房间信息 ----
            if (msg.type === TYPE_GET_ROOM_INFO) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_GET_ROOM_INFO, success: false }); return; }
                const isOwner = room.owner === ws.uid;
                send({ type: TYPE_GET_ROOM_INFO, success: true, roomInfo: { id: room.id, name: room.name, password: room.password, isOwner } });
                return;
            }

            // ---- 修改房间密码 ----
            if (msg.type === TYPE_UPDATE_ROOM_PASSWORD) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_UPDATE_ROOM_PASSWORD, success: false, error: '房间不存在' }); return; }
                if (room.owner !== ws.uid) { send({ type: TYPE_UPDATE_ROOM_PASSWORD, success: false, error: '只有房主可以修改密码' }); return; }
                if (!msg.newPassword || msg.newPassword.length !== 6) { send({ type: TYPE_UPDATE_ROOM_PASSWORD, success: false, error: '密码必须是6位' }); return; }
                room.password = msg.newPassword;
                saveRoomsData();
                send({ type: TYPE_UPDATE_ROOM_PASSWORD, success: true });
                return;
            }

            // ---- 邀请好友进房间 ----
            if (msg.type === TYPE_INVITE_TO_ROOM) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_INVITE_TO_ROOM, success: false, error: '房间不存在' }); return; }
                const targetUser = usersData[msg.targetAccountId];
                if (!targetUser) { send({ type: TYPE_INVITE_TO_ROOM, success: false, error: '用户不存在' }); return; }

                const isOwner = room.owner === ws.accountId;

                // 将邀请作为系统消息发送到目标用户的系统消息房间
                const inviteMsg = {
                    type: 'ROOM_INVITE',
                    inviteId: Date.now().toString() + Math.random().toString(36).substr(2, 9),
                    from: ws.username,
                    fromAccountId: ws.accountId,
                    roomId: room.id,
                    roomName: room.name,
                    needPassword: !isOwner,
                    time: new Date().toLocaleTimeString(),
                    status: 'pending'
                };

                // 保存到系统消息房间的历史记录（只对目标用户可见）
                const sysMsgRoom = rooms.get(SYSTEM_MSG_ROOM_ID);
                if (sysMsgRoom) {
                    // 为每个用户维护独立的系统消息
                    if (!sysMsgRoom.userMessages) sysMsgRoom.userMessages = {};
                    if (!sysMsgRoom.userMessages[msg.targetAccountId]) sysMsgRoom.userMessages[msg.targetAccountId] = [];
                    sysMsgRoom.userMessages[msg.targetAccountId].push(inviteMsg);

                    // 限制历史消息数量
                    if (sysMsgRoom.userMessages[msg.targetAccountId].length > MAX_HISTORY) {
                        sysMsgRoom.userMessages[msg.targetAccountId].shift();
                    }
                }

                // 如果目标用户在线，通知刷新系统消息
                const targetConn = connections.find(c => c.accountId === msg.targetAccountId && c.isAuthenticated);
                if (targetConn && targetConn.readyState === WebSocket.OPEN) {
                    try {
                        targetConn.send(JSON.stringify({ type: 'NEW_SYSTEM_MSG' }));
                    } catch (e) {}
                }

                send({ type: TYPE_INVITE_TO_ROOM, success: true });
                return;
            }

            if (msg.type === TYPE_UPDATE_INVITE_STATUS) {
                const sysMsgRoom = rooms.get(SYSTEM_MSG_ROOM_ID);
                if (sysMsgRoom && sysMsgRoom.userMessages && sysMsgRoom.userMessages[ws.accountId]) {
                    const invite = sysMsgRoom.userMessages[ws.accountId].find(m => m.inviteId === msg.inviteId);
                    if (invite) {
                        invite.status = msg.status;
                        send({ type: TYPE_UPDATE_INVITE_STATUS, success: true });
                    } else {
                        send({ type: TYPE_UPDATE_INVITE_STATUS, success: false, error: '邀请不存在' });
                    }
                } else {
                    send({ type: TYPE_UPDATE_INVITE_STATUS, success: false, error: '系统消息房间不存在' });
                }
                return;
            }

            // ---- 添加违禁词 ----
            if (msg.type === TYPE_ADD_BANNED_WORD) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_BANNED_WORDS_LIST, success: false, error: '房间不存在' }); return; }
                if (room.owner !== ws.uid) { send({ type: TYPE_BANNED_WORDS_LIST, success: false, error: '只有房主可以设置违禁词' }); return; }
                room.bannedWords.add(msg.word);
                send({ type: TYPE_BANNED_WORDS_LIST, roomId: msg.roomId, words: Array.from(room.bannedWords) });
                return;
            }

            // ---- 删除违禁词 ----
            if (msg.type === TYPE_REMOVE_BANNED_WORD) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_BANNED_WORDS_LIST, success: false, error: '房间不存在' }); return; }
                if (room.owner !== ws.uid) { send({ type: TYPE_BANNED_WORDS_LIST, success: false, error: '只有房主可以删除违禁词' }); return; }
                room.bannedWords.delete(msg.word);
                send({ type: TYPE_BANNED_WORDS_LIST, roomId: msg.roomId, words: Array.from(room.bannedWords) });
                return;
            }

            // ---- 获取违禁词列表 ----
            if (msg.type === TYPE_GET_BANNED_WORDS) {
                const room = rooms.get(msg.roomId);
                if (!room) { send({ type: TYPE_BANNED_WORDS_LIST, success: false, error: '房间不存在' }); return; }
                send({ type: TYPE_BANNED_WORDS_LIST, roomId: msg.roomId, words: Array.from(room.bannedWords) });
                return;
            }

            // ---- AI助手请求 ----
            if (msg.type === TYPE_AI_REQUEST) {
                handleAIRequest(ws, msg.message).then(result => {
                    send({ type: TYPE_AI_RESPONSE, ...result });
                }).catch(err => {
                    send({ type: TYPE_AI_RESPONSE, success: false, message: 'AI服务异常' });
                });
                return;
            }

        } catch (e) {
            console.error('处理消息出错:', e);
        }
    });

    ws.on('close', () => {
        connections = connections.filter(c => c !== ws);
        ws.rooms.forEach(roomId => {
            const room = rooms.get(roomId);
            if (!room) return;
            room.members.delete(ws.uid);
            if (ws.username) {
                const leaveMsg = { type: TYPE_LEAVE, msg: `${ws.username} 离开聊天室`, time: new Date().toLocaleTimeString(), onlineCount: room.members.size };
                const data = JSON.stringify(leaveMsg);
                connections.forEach(c => {
                    if (c.rooms.has(roomId) && c.readyState === WebSocket.OPEN) {
                        try { c.send(data); } catch (e) {}
                    }
                });
                // LEAVE消息不保存到历史记录
            }
            // 不删除空房间，保留房间数据以便用户重新登录
        });
    });

    ws.on('error', err => console.error('连接错误:', err));
});

// 心跳检测
const heartbeatInterval = setInterval(() => {
    connections.forEach(ws => {
        if (!ws.isAlive) { ws.terminate(); return; }
        ws.isAlive = false;
        ws.ping();
    });
}, 30000);

wss.on('close', () => clearInterval(heartbeatInterval));
