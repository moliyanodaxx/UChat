// WebSocket 连接
let ws = null;
let username = '';
let selectedAvatar = '';
let reconnectTimer = null;
let heartbeatTimer = null;
let isManualClose = false;
let currentRoomId = '0000000000';
let userRooms = [];
let isLoginMode = true;
let currentUserInfo = null;
let contextMenuTarget = null;

// 登录凭据（用于重连）
let savedAccount = '';
let savedPassword = '';

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

// DOM 元素
const authModal = document.getElementById('authModal');
const accountInput = document.getElementById('accountInput');
const passwordInput = document.getElementById('passwordInput');
const authBtn = document.getElementById('authBtn');
const switchAuthBtn = document.getElementById('switchAuthBtn');
const authTitle = document.getElementById('authTitle');
const authSubtitle = document.getElementById('authSubtitle');
const profileModal = document.getElementById('profileModal');
const nicknameInput = document.getElementById('nicknameInput');
const avatarSelector = document.getElementById('avatarSelector');
const saveProfileBtn = document.getElementById('saveProfileBtn');
const usernameModal = document.getElementById('usernameModal');
const messageInput = document.getElementById('messageInput');
const sendBtn = document.getElementById('sendBtn');
const messagesContainer = document.getElementById('messagesContainer');
const onlineCountEl = document.getElementById('onlineCount');
const currentRoomNameEl = document.getElementById('currentRoomName');
const emojiBtn = document.getElementById('emojiBtn');
const emojiPicker = document.getElementById('emojiPicker');
const emojiPickerClose = document.getElementById('emojiPickerClose');
const emojiPickerBody = document.getElementById('emojiPickerBody');
const roomList = document.getElementById('roomList');
const createRoomBtn = document.getElementById('createRoomBtn');
const joinRoomBtn = document.getElementById('joinRoomBtn');
const createRoomModal = document.getElementById('createRoomModal');
const joinRoomModal = document.getElementById('joinRoomModal');
const roomMenu = document.getElementById('roomMenu');
const roomMenuBtn = document.getElementById('roomMenuBtn');
const roomMenuClose = document.getElementById('roomMenuClose');
const roomMenuBody = document.getElementById('roomMenuBody');
const roomMenuFooter = document.getElementById('roomMenuFooter');
const leaveRoomBtn = document.getElementById('leaveRoomBtn');
const friendsBtn = document.getElementById('friendsBtn');
const profileBtn = document.getElementById('profileBtn');
const friendsModal = document.getElementById('friendsModal');
const profileViewModal = document.getElementById('profileViewModal');
const editProfileModal = document.getElementById('editProfileModal');
const contextMenu = document.getElementById('contextMenu');
const inviteFriendBtn = document.getElementById('inviteFriendBtn');
const inviteFriendModal = document.getElementById('inviteFriendModal');

// ===== 表情相关 =====
const emojiData = {
    '笑脸': ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩'],
    '手势': ['👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '👇', '☝️', '✋', '🤚', '🖐️'],
    '爱心': ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖'],
    '动物': ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐸', '🐵', '🐔'],
    '食物': ['🍎', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🍈', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🥑'],
    '活动': ['⚽', '🏀', '🏈', '⚾', '🥎', '🎾', '🏐', '🏉', '🥏', '🎱', '🏓', '🏸', '🏒', '🏑', '🥍', '🏏'],
    '符号': ['⭐', '✨', '💫', '💥', '💢', '💦', '💨', '🔥', '⚡', '☀️', '🌙', '⭐', '🌟', '💯', '✅', '❌']
};

function initEmojiPicker() {
    emojiPickerBody.innerHTML = '';
    Object.keys(emojiData).forEach(category => {
        const categoryDiv = document.createElement('div');
        categoryDiv.className = 'emoji-category';
        const title = document.createElement('div');
        title.className = 'emoji-category-title';
        title.textContent = category;
        const grid = document.createElement('div');
        grid.className = 'emoji-grid';
        emojiData[category].forEach(emoji => {
            const btn = document.createElement('button');
            btn.className = 'emoji-item';
            btn.textContent = emoji;
            btn.onclick = () => insertEmoji(emoji);
            grid.appendChild(btn);
        });
        categoryDiv.appendChild(title);
        categoryDiv.appendChild(grid);
        emojiPickerBody.appendChild(categoryDiv);
    });
}

function insertEmoji(emoji) {
    const cursorPos = messageInput.selectionStart;
    const textBefore = messageInput.value.substring(0, cursorPos);
    const textAfter = messageInput.value.substring(cursorPos);
    messageInput.value = textBefore + emoji + textAfter;
    messageInput.focus();
    messageInput.setSelectionRange(cursorPos + emoji.length, cursorPos + emoji.length);
}

emojiBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    emojiPicker.classList.toggle('show');
});

emojiPickerClose.addEventListener('click', () => {
    emojiPicker.classList.remove('show');
});

// ===== 头像 =====
const AVATAR_LIST = ['😀', '😎', '🤓', '😊', '🥳', '🤩', '😇', '🙂', '😃', '😄', '🐶', '🐱', '🐭', '🐹', '🐰', '🦊'];

function initAvatarSelector() {
    avatarSelector.innerHTML = '';
    AVATAR_LIST.forEach(emoji => {
        const btn = document.createElement('button');
        btn.className = 'avatar-option';
        btn.textContent = emoji;
        btn.onclick = () => {
            document.querySelectorAll('.avatar-option').forEach(el => el.classList.remove('selected'));
            btn.classList.add('selected');
            selectedAvatar = emoji;
        };
        avatarSelector.appendChild(btn);
    });
}

function getAvatarEmoji(name) {
    const hash = name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    return AVATAR_LIST[hash % AVATAR_LIST.length];
}

// ===== 初始化 =====
initEmojiPicker();
initAvatarSelector();

// ===== 登录/注册 =====
switchAuthBtn.addEventListener('click', () => {
    isLoginMode = !isLoginMode;
    if (isLoginMode) {
        authTitle.textContent = '欢迎来到UChat';
        authSubtitle.textContent = '请登录';
        authBtn.textContent = '登录';
        switchAuthBtn.textContent = '注册';
    } else {
        authTitle.textContent = '注册新账号';
        authSubtitle.textContent = '创建你的UChat账号';
        authBtn.textContent = '注册';
        switchAuthBtn.textContent = '登录';
    }
});

authBtn.addEventListener('click', () => {
    const account = accountInput.value.trim();
    const password = passwordInput.value;

    if (!account || account.length !== 10 || !/^[a-zA-Z0-9]+$/.test(account)) {
        alert('账号必须是10位字母或数字');
        return;
    }
    if (!password || password.length < 8 || password.length > 10 || !/^[a-zA-Z0-9]+$/.test(password)) {
        alert('密码必须是8-10位字母或数字');
        return;
    }

    savedAccount = account;
    savedPassword = password;
    connectWebSocket();
});

passwordInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') authBtn.click();
});

saveProfileBtn.addEventListener('click', () => {
    const nickname = nicknameInput.value.trim();
    if (!nickname) { alert('请输入昵称'); return; }
    if (!selectedAvatar) { alert('请选择头像'); return; }
    ws.send(JSON.stringify({ type: TYPE_SET_PROFILE, nickname, avatar: selectedAvatar }));
});

// ===== WebSocket =====
function connectWebSocket() {
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    ws = new WebSocket(`${protocol}://${location.host}`);

    ws.onopen = () => {
        isManualClose = false;
        ws.send(JSON.stringify(
            isLoginMode
                ? { type: TYPE_LOGIN, accountId: savedAccount, password: savedPassword }
                : { type: TYPE_REGISTER, accountId: savedAccount, password: savedPassword }
        ));
        startHeartbeat();
    };

    ws.onmessage = (event) => {
        handleMessage(JSON.parse(event.data));
    };

    ws.onclose = () => {
        stopHeartbeat();
        if (!isManualClose && savedAccount) {
            addSystemMessage('已断开连接，3秒后重连...');
            reconnectTimer = setTimeout(connectWebSocket, 3000);
        }
    };

    ws.onerror = (error) => {
        console.error('WebSocket 错误:', error);
    };
}

function startHeartbeat() {
    stopHeartbeat();
    heartbeatTimer = setInterval(() => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ type: TYPE_HEARTBEAT }));
        }
    }, 25000);
}

function stopHeartbeat() {
    if (heartbeatTimer) { clearInterval(heartbeatTimer); heartbeatTimer = null; }
}

// ===== 消息处理 =====
function handleMessage(data) {
    console.log('收到消息:', data.type, data);
    if (data.type === TYPE_HEARTBEAT) return;

    // 处理新系统消息通知
    if (data.type === 'NEW_SYSTEM_MSG') {
        // 如果当前在系统消息房间，刷新历史
        if (currentRoomId === '0000000001') {
            ws.send(JSON.stringify({ type: TYPE_SWITCH_ROOM, roomId: '0000000001' }));
        }
        // 更新房间列表，显示未读标记
        updateRoomList();
        return;
    }

    switch (data.type) {
        case TYPE_REGISTER:
            if (data.success) {
                // 注册成功，切换到登录模式，弹出资料设置
                isLoginMode = true;
                authModal.classList.add('hidden');
                profileModal.classList.remove('hidden');
                setTimeout(() => nicknameInput.focus(), 100);
            } else {
                alert('注册失败：' + data.error);
                ws.close();
            }
            break;

        case TYPE_LOGIN:
            if (data.success) {
                authModal.classList.add('hidden');
                if (data.needProfile) {
                    profileModal.classList.remove('hidden');
                    setTimeout(() => nicknameInput.focus(), 100);
                } else {
                    username = data.userData.nickname;
                    selectedAvatar = data.userData.avatar;
                    // 发送用户名，服务器会回复 TYPE_ROOM_LIST
                    ws.send(JSON.stringify({ type: TYPE_SET_USERNAME, username }));
                }
            } else {
                alert('登录失败：' + data.error);
                ws.close();
            }
            break;

        case TYPE_SET_PROFILE:
            if (data.success) {
                profileModal.classList.add('hidden');
                username = data.userData.nickname;
                selectedAvatar = data.userData.avatar;
                // 发送用户名，服务器会回复 TYPE_ROOM_LIST
                ws.send(JSON.stringify({ type: TYPE_SET_USERNAME, username }));
            } else {
                alert('设置失败：' + data.error);
            }
            break;

        case TYPE_ENTER:
            addSystemMessage(data.msg);
            if (data.onlineCount !== undefined) updateOnlineCount(data.onlineCount);
            break;

        case TYPE_LEAVE:
            addSystemMessage(data.msg);
            if (data.onlineCount !== undefined) updateOnlineCount(data.onlineCount);
            break;

        case TYPE_MSG:
            addMessage(data.username, data.msg, data.time, data.username === username, true, data.accountId);
            break;

        case TYPE_ROOM_LIST:
            // 服务器的权威房间列表，直接渲染
            userRooms = data.rooms;
            currentRoomId = data.currentRoom;
            updateRoomList();
            addSystemMessage('已连接到服务器');
            break;

        case TYPE_HISTORY:
            if (data.roomId === currentRoomId) {
                messagesContainer.innerHTML = '<div class="date-divider"><span>今天</span></div>';
                data.messages.forEach(msg => {
                    if (msg.type === TYPE_MSG) {
                        addMessage(msg.username, msg.msg, msg.time, msg.username === username, false, msg.accountId);
                    } else if (msg.type === TYPE_ENTER || msg.type === TYPE_LEAVE) {
                        addSystemMessage(msg.msg, false);
                    } else if (msg.type === 'ROOM_INVITE') {
                        addInviteMessage(msg, false);
                    }
                });
                smoothScrollToBottom();
            }
            break;

        case TYPE_CREATE_ROOM:
            if (data.success) {
                userRooms.push(data.room);
                updateRoomList();
                createRoomModal.classList.add('hidden');
                document.getElementById('roomNameInput').value = '';
                document.getElementById('roomIdInput').value = '';
                document.getElementById('roomPasswordInput').value = '';
                alert('房间创建成功！');
            } else {
                alert('创建失败：' + data.error);
            }
            break;

        case TYPE_JOIN_ROOM:
            if (data.success) {
                userRooms.push(data.room);
                updateRoomList();
                joinRoomModal.classList.add('hidden');
                document.getElementById('joinRoomIdInput').value = '';
                document.getElementById('joinRoomPasswordInput').value = '';

                // 如果当前在系统消息房间，刷新显示以更新邀请状态
                if (currentRoomId === '0000000001') {
                    ws.send(JSON.stringify({ type: TYPE_SWITCH_ROOM, roomId: '0000000001' }));
                }

                alert('加入成功！');
            } else {
                alert('加入失败：' + data.error);
            }
            break;

        case TYPE_SWITCH_ROOM:
            if (data.success) {
                currentRoomId = data.roomId;
                updateRoomList();
                updateOnlineCount(data.onlineCount);
                messagesContainer.innerHTML = '<div class="date-divider"><span>今天</span></div>';
            }
            break;

        case TYPE_LEAVE_ROOM:
            if (data.success) {
                userRooms = userRooms.filter(r => r.id !== data.roomId);
                if (data.kicked) alert('你已被移出房间');
                if (currentRoomId === data.roomId) switchRoom('0000000000');
                updateRoomList();
                roomMenu.classList.add('hidden');
            }
            break;

        case TYPE_ROOM_MEMBERS:
            if (data.success) showRoomMembers(data.members, data.isOwner, data.roomId);
            break;

        case TYPE_KICK_MEMBER:
        case TYPE_TRANSFER_OWNER:
            if (data.success) requestRoomMembers();
            break;

        case TYPE_SEARCH_USER:
            if (data.success) {
                displaySearchResults(data.results);
            } else {
                document.getElementById('searchResults').innerHTML = '<div style="text-align:center;padding:20px;color:#999;">未找到用户</div>';
            }
            break;

        case TYPE_GET_USER_INFO:
            if (data.success) {
                showUserProfile(data.user, data.isSelf);
            }
            break;

        case TYPE_SEND_FRIEND_REQUEST:
            if (data.success) {
                alert('好友请求已发送');
            } else {
                alert('发送失败：' + data.error);
            }
            break;

        case TYPE_GET_FRIEND_REQUESTS:
            if (data.success) {
                displayFriendRequests(data.requests);
            }
            break;

        case TYPE_HANDLE_FRIEND_REQUEST:
            if (data.success) {
                alert(data.accepted ? '已接受好友请求' : '已拒绝好友请求');
                ws.send(JSON.stringify({ type: TYPE_GET_FRIEND_REQUESTS }));
            }
            break;

        case TYPE_GET_FRIENDS:
            if (data.success) {
                if (inviteFriendModal.classList.contains('hidden')) {
                    displayFriendsList(data.friends);
                } else {
                    displayInviteFriendsList(data.friends);
                }
            }
            break;

        case TYPE_UPDATE_PROFILE:
            if (data.success) {
                alert('资料更新成功');
                username = data.user.nickname;
                selectedAvatar = data.user.avatar;
                currentUserInfo = data.user;
                editProfileModal.classList.add('hidden');
            } else {
                alert('更新失败：' + data.error);
            }
            break;

        case TYPE_GET_ROOM_INFO:
            if (data.success) {
                displayRoomInfo(data.roomInfo);
            }
            break;

        case TYPE_UPDATE_ROOM_PASSWORD:
            if (data.success) {
                alert('密码修改成功');
                requestRoomMembers();
            } else {
                alert('修改失败：' + data.error);
            }
            break;

        case TYPE_INVITE_TO_ROOM:
            if (data.success) {
                alert('邀请已发送');
            } else {
                alert('邀请失败：' + data.error);
            }
            break;
    }
}

// ===== 房间列表 =====
function updateRoomList() {
    roomList.innerHTML = '';
    userRooms.forEach(room => {
        const item = document.createElement('div');
        item.className = 'chat-item' + (room.id === currentRoomId ? ' active' : '');
        item.onclick = () => switchRoom(room.id);

        const avatar = document.createElement('div');
        avatar.className = 'avatar';
        avatar.textContent = room.isSystem ? '💬' : '🏠';

        const info = document.createElement('div');
        info.className = 'chat-info';

        const name = document.createElement('div');
        name.className = 'chat-name';
        name.textContent = room.name;

        const sub = document.createElement('div');
        sub.className = 'last-message';
        sub.textContent = room.isSystem ? '系统房间' : `房间号: ${room.id}`;

        info.appendChild(name);
        info.appendChild(sub);

        const meta = document.createElement('div');
        meta.className = 'chat-meta';
        const badge = document.createElement('div');
        badge.className = 'badge';
        badge.textContent = room.memberCount || 0;
        meta.appendChild(badge);

        item.appendChild(avatar);
        item.appendChild(info);
        item.appendChild(meta);
        roomList.appendChild(item);
    });

    const currentRoom = userRooms.find(r => r.id === currentRoomId);
    if (currentRoom) currentRoomNameEl.textContent = currentRoom.name;
}

function switchRoom(roomId) {
    if (roomId === currentRoomId) return;
    ws.send(JSON.stringify({ type: TYPE_SWITCH_ROOM, roomId }));
}

// ===== 创建/加入房间 =====
createRoomBtn.addEventListener('click', () => {
    createRoomModal.classList.remove('hidden');
    setTimeout(() => document.getElementById('roomNameInput').focus(), 100);
});

document.getElementById('cancelCreateBtn').addEventListener('click', () => {
    createRoomModal.classList.add('hidden');
    document.getElementById('roomNameInput').value = '';
    document.getElementById('roomIdInput').value = '';
    document.getElementById('roomPasswordInput').value = '';
});

document.getElementById('confirmCreateBtn').addEventListener('click', () => {
    const roomName = document.getElementById('roomNameInput').value.trim();
    const roomId = document.getElementById('roomIdInput').value.trim();
    const password = document.getElementById('roomPasswordInput').value.trim();

    if (!roomName) { alert('请输入房间名称'); return; }
    if (password.length !== 6) { alert('密码必须是6位'); return; }
    if (roomId && (roomId.length !== 10 || !/^\d+$/.test(roomId))) {
        alert('房间号必须是10位数字'); return;
    }

    ws.send(JSON.stringify({ type: TYPE_CREATE_ROOM, roomName, roomId: roomId || null, password }));
    document.getElementById('roomNameInput').value = '';
    document.getElementById('roomIdInput').value = '';
    document.getElementById('roomPasswordInput').value = '';
});

joinRoomBtn.addEventListener('click', () => {
    joinRoomModal.classList.remove('hidden');
    setTimeout(() => document.getElementById('joinRoomIdInput').focus(), 100);
});

document.getElementById('cancelJoinBtn').addEventListener('click', () => {
    joinRoomModal.classList.add('hidden');
    document.getElementById('joinRoomIdInput').value = '';
    document.getElementById('joinRoomPasswordInput').value = '';
});

document.getElementById('confirmJoinBtn').addEventListener('click', () => {
    const roomId = document.getElementById('joinRoomIdInput').value.trim();
    const password = document.getElementById('joinRoomPasswordInput').value.trim();

    if (roomId.length !== 10 || !/^\d+$/.test(roomId)) {
        alert('房间号必须是10位数字'); return;
    }

    ws.send(JSON.stringify({ type: TYPE_JOIN_ROOM, roomId, password }));
    document.getElementById('joinRoomIdInput').value = '';
    document.getElementById('joinRoomPasswordInput').value = '';
});

createRoomModal.addEventListener('click', (e) => {
    if (e.target === createRoomModal) {
        createRoomModal.classList.add('hidden');
        document.getElementById('roomNameInput').value = '';
        document.getElementById('roomIdInput').value = '';
        document.getElementById('roomPasswordInput').value = '';
    }
});

joinRoomModal.addEventListener('click', (e) => {
    if (e.target === joinRoomModal) {
        joinRoomModal.classList.add('hidden');
        document.getElementById('joinRoomIdInput').value = '';
        document.getElementById('joinRoomPasswordInput').value = '';
    }
});

// ===== 房间成员 =====
roomMenuBtn.addEventListener('click', () => {
    roomMenu.classList.toggle('hidden');
    if (!roomMenu.classList.contains('hidden')) requestRoomMembers();
});

roomMenuClose.addEventListener('click', () => roomMenu.classList.add('hidden'));

function requestRoomMembers() {
    ws.send(JSON.stringify({ type: TYPE_ROOM_MEMBERS, roomId: currentRoomId }));
    ws.send(JSON.stringify({ type: TYPE_GET_ROOM_INFO, roomId: currentRoomId }));
}

function showRoomMembers(members, isOwner, roomId) {
    roomMenuBody.innerHTML = '';
    members.forEach(member => {
        const item = document.createElement('div');
        item.className = 'member-item';

        const avatar = document.createElement('div');
        avatar.className = 'member-avatar';
        avatar.textContent = member.avatar || getAvatarEmoji(member.username);

        const info = document.createElement('div');
        info.className = 'member-info';

        const nameEl = document.createElement('div');
        nameEl.className = 'member-name';
        nameEl.textContent = member.username + (member.isOwner ? ' (房主)' : '');

        if (member.signature) {
            const sigEl = document.createElement('div');
            sigEl.className = 'member-signature';
            sigEl.textContent = member.signature;
            sigEl.style.fontSize = '12px';
            sigEl.style.color = '#999';
            info.appendChild(sigEl);
        }

        const status = document.createElement('div');
        status.className = 'member-status' + (member.isOnline ? ' online' : '');
        status.textContent = member.isOnline ? '在线' : '离线';

        info.appendChild(nameEl);
        info.appendChild(status);
        item.appendChild(avatar);
        item.appendChild(info);

        if (isOwner && !member.isOwner) {
            const actions = document.createElement('div');
            actions.className = 'member-actions';

            const kickBtn = document.createElement('button');
            kickBtn.className = 'member-action-btn danger';
            kickBtn.textContent = '踢出';
            kickBtn.onclick = () => kickMember(member.uid);

            const transferBtn = document.createElement('button');
            transferBtn.className = 'member-action-btn';
            transferBtn.textContent = '转让房主';
            transferBtn.onclick = () => transferOwner(member.uid);

            actions.appendChild(kickBtn);
            actions.appendChild(transferBtn);
            item.appendChild(actions);
        }
        roomMenuBody.appendChild(item);
    });

    const currentRoom = userRooms.find(r => r.id === currentRoomId);
    roomMenuFooter.style.display = (currentRoom && currentRoom.isSystem) ? 'none' : 'block';
}

function displayRoomInfo(roomInfo) {
    const container = document.getElementById('roomInfo');
    container.innerHTML = `
        <div class="room-info-item">
            <span class="room-info-label">房间名称</span>
            <span class="room-info-value">${roomInfo.name}</span>
        </div>
        <div class="room-info-item">
            <span class="room-info-label">房间号</span>
            <span class="room-info-value">${roomInfo.id}</span>
        </div>
        <div class="room-info-item">
            <span class="room-info-label">房间密码</span>
            <span class="room-info-value">
                ${roomInfo.password}
                ${roomInfo.isOwner ? '<button onclick="changeRoomPassword()">修改</button>' : ''}
            </span>
        </div>
    `;
}

window.changeRoomPassword = function() {
    const newPassword = prompt('请输入新密码（6位）：');
    if (!newPassword) return;
    if (newPassword.length !== 6) { alert('密码必须是6位'); return; }
    ws.send(JSON.stringify({ type: TYPE_UPDATE_ROOM_PASSWORD, roomId: currentRoomId, newPassword }));
};

function handleRoomInvite(data) {
    const msg = `${data.from} 邀请你加入房间 "${data.roomName}"`;
    if (confirm(msg)) {
        if (data.needPassword) {
            const password = prompt('请输入房间密码（6位）：');
            if (!password) return;
            ws.send(JSON.stringify({ type: TYPE_JOIN_ROOM, roomId: data.roomId, password }));
        } else {
            // 房主邀请，传递空字符串作为密码
            ws.send(JSON.stringify({ type: TYPE_JOIN_ROOM, roomId: data.roomId, password: '' }));
        }
    }
}

function kickMember(targetUid) {
    if (confirm('确定要踢出该成员吗？')) {
        ws.send(JSON.stringify({ type: TYPE_KICK_MEMBER, roomId: currentRoomId, targetUid }));
    }
}

function transferOwner(targetUid) {
    if (confirm('确定要转让房主吗？')) {
        ws.send(JSON.stringify({ type: TYPE_TRANSFER_OWNER, roomId: currentRoomId, targetUid }));
    }
}

leaveRoomBtn.addEventListener('click', () => {
    if (confirm('确定要退出该房间吗？')) {
        ws.send(JSON.stringify({ type: TYPE_LEAVE_ROOM, roomId: currentRoomId }));
    }
});

inviteFriendBtn.addEventListener('click', () => {
    if (currentRoomId === '0000000000') {
        alert('无法邀请好友到系统房间');
        return;
    }
    inviteFriendModal.classList.remove('hidden');
    ws.send(JSON.stringify({ type: TYPE_GET_FRIENDS }));
});

document.getElementById('closeInviteModal').addEventListener('click', () => {
    inviteFriendModal.classList.add('hidden');
});

// ===== 消息显示 =====
function sendMessage() {
    const message = messageInput.value.trim();
    if (message && ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({
            type: TYPE_MSG,
            msg: message,
            time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        }));
        messageInput.value = '';
        sendBtn.style.transform = 'scale(0.9)';
        setTimeout(() => { sendBtn.style.transform = 'scale(1)'; }, 100);
    }
}

sendBtn.addEventListener('click', sendMessage);
messageInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
});

function addMessage(senderName, content, time, isOwn = false, autoScroll = true, accountId = null) {
    const group = document.createElement('div');
    group.className = `message-group ${isOwn ? 'own' : ''}`;

    const avatar = document.createElement('div');
    avatar.className = 'message-avatar';
    avatar.textContent = isOwn ? selectedAvatar : getAvatarEmoji(senderName);

    // 添加右键菜单功能（仅对他人消息）
    if (!isOwn && accountId) {
        avatar.style.cursor = 'pointer';
        avatar.addEventListener('contextmenu', (e) => showContextMenu(e, senderName, accountId));
    }

    const wrapper = document.createElement('div');
    wrapper.className = 'message-content-wrapper';

    if (!isOwn) {
        const sender = document.createElement('div');
        sender.className = 'message-sender';
        sender.textContent = senderName;
        wrapper.appendChild(sender);
    }

    const bubble = document.createElement('div');
    bubble.className = 'message-bubble';
    const text = document.createElement('div');
    text.className = 'message-text';
    text.textContent = content;
    const timeEl = document.createElement('div');
    timeEl.className = 'message-time';
    timeEl.textContent = time;

    bubble.appendChild(text);
    bubble.appendChild(timeEl);
    wrapper.appendChild(bubble);
    group.appendChild(avatar);
    group.appendChild(wrapper);
    messagesContainer.appendChild(group);
    if (autoScroll) smoothScrollToBottom();
}

function addSystemMessage(content, autoScroll = true) {
    const msg = document.createElement('div');
    msg.className = 'system-message';
    const text = document.createElement('div');
    text.className = 'system-message-text';
    text.textContent = content;
    msg.appendChild(text);
    messagesContainer.appendChild(msg);
    if (autoScroll) smoothScrollToBottom();
}

function addInviteMessage(inviteData, autoScroll = true) {
    const msg = document.createElement('div');
    msg.className = 'invite-message';

    const isJoined = userRooms.some(r => r.id === inviteData.roomId);
    const status = inviteData.status || 'pending';

    let statusHtml = '';
    if (isJoined || status === 'accepted') {
        statusHtml = '<div class="invite-status">已接受</div>';
    } else if (status === 'rejected') {
        statusHtml = '<div class="invite-status">已拒绝</div>';
    } else {
        statusHtml = `
            <div class="invite-actions">
                <button class="btn-primary" onclick="acceptInvite('${inviteData.inviteId}', '${inviteData.roomId}', ${inviteData.needPassword})">接受</button>
                <button class="btn-secondary" onclick="rejectInvite('${inviteData.inviteId}')">拒绝</button>
            </div>
        `;
    }

    msg.innerHTML = `
        <div class="invite-content">
            <div class="invite-text">
                <strong>${inviteData.from}</strong> 邀请你加入房间 <strong>${inviteData.roomName}</strong>
            </div>
            <div class="invite-time">${inviteData.time}</div>
            ${statusHtml}
        </div>
    `;

    messagesContainer.appendChild(msg);
    if (autoScroll) smoothScrollToBottom();
}

window.acceptInvite = function(inviteId, roomId, needPassword) {
    if (needPassword) {
        const password = prompt('请输入房间密码（6位）：');
        if (!password) return;
        ws.send(JSON.stringify({ type: TYPE_JOIN_ROOM, roomId, password }));
    } else {
        ws.send(JSON.stringify({ type: TYPE_JOIN_ROOM, roomId, password: '' }));
    }
    ws.send(JSON.stringify({ type: 28, inviteId, status: 'accepted' }));
};

window.rejectInvite = function(inviteId) {
    ws.send(JSON.stringify({ type: 28, inviteId, status: 'rejected' }));
    location.reload();
};

function updateOnlineCount(count) {
    onlineCountEl.textContent = count;
}

function smoothScrollToBottom() {
    messagesContainer.scrollTo({ top: messagesContainer.scrollHeight, behavior: 'smooth' });
}

// ===== 好友系统 =====
friendsBtn.addEventListener('click', () => {
    friendsModal.classList.remove('hidden');
    switchFriendTab('friends');
});

profileBtn.addEventListener('click', () => {
    ws.send(JSON.stringify({ type: TYPE_GET_USER_INFO, accountId: savedAccount }));
});

document.getElementById('closeFriendsModal').addEventListener('click', () => {
    friendsModal.classList.add('hidden');
});

document.getElementById('friendsTab').addEventListener('click', () => switchFriendTab('friends'));
document.getElementById('requestsTab').addEventListener('click', () => switchFriendTab('requests'));
document.getElementById('searchTab').addEventListener('click', () => switchFriendTab('search'));

function switchFriendTab(tab) {
    document.querySelectorAll('.friend-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.friend-content').forEach(c => c.classList.remove('active'));

    if (tab === 'friends') {
        document.getElementById('friendsTab').classList.add('active');
        document.getElementById('friendsContent').classList.add('active');
        ws.send(JSON.stringify({ type: TYPE_GET_FRIENDS }));
    } else if (tab === 'requests') {
        document.getElementById('requestsTab').classList.add('active');
        document.getElementById('requestsContent').classList.add('active');
        ws.send(JSON.stringify({ type: TYPE_GET_FRIEND_REQUESTS }));
    } else if (tab === 'search') {
        document.getElementById('searchTab').classList.add('active');
        document.getElementById('searchContent').classList.add('active');
    }
}

document.getElementById('searchUserBtn').addEventListener('click', () => {
    const query = document.getElementById('searchUserInput').value.trim();
    if (!query) { alert('请输入账号或昵称'); return; }
    ws.send(JSON.stringify({ type: TYPE_SEARCH_USER, keyword: query }));
});

document.getElementById('searchUserInput').addEventListener('keypress', (e) => {
    if (e.key === 'Enter') document.getElementById('searchUserBtn').click();
});

// ===== 个人资料查看 =====
document.getElementById('closeProfileViewModal').addEventListener('click', () => {
    profileViewModal.classList.add('hidden');
});

document.getElementById('editProfileBtn').addEventListener('click', () => {
    profileViewModal.classList.add('hidden');
    editProfileModal.classList.remove('hidden');
    if (currentUserInfo) {
        document.getElementById('editNicknameInput').value = currentUserInfo.nickname;
        document.getElementById('editSignatureInput').value = currentUserInfo.signature || '';
        selectedAvatar = currentUserInfo.avatar;
        document.querySelectorAll('#editAvatarSelector .avatar-option').forEach(btn => {
            btn.classList.toggle('selected', btn.textContent === selectedAvatar);
        });
    }
});

document.getElementById('closeEditProfileModal').addEventListener('click', () => {
    editProfileModal.classList.add('hidden');
});

document.getElementById('saveEditProfileBtn').addEventListener('click', () => {
    const nickname = document.getElementById('editNicknameInput').value.trim();
    const signature = document.getElementById('editSignatureInput').value.trim();
    if (!nickname) { alert('请输入昵称'); return; }
    if (!selectedAvatar) { alert('请选择头像'); return; }
    ws.send(JSON.stringify({
        type: TYPE_UPDATE_PROFILE,
        nickname,
        avatar: selectedAvatar,
        signature
    }));
});

// 初始化编辑资料的头像选择器
function initEditAvatarSelector() {
    const selector = document.getElementById('editAvatarSelector');
    selector.innerHTML = '';
    AVATAR_LIST.forEach(emoji => {
        const btn = document.createElement('button');
        btn.className = 'avatar-option';
        btn.textContent = emoji;
        btn.onclick = () => {
            document.querySelectorAll('#editAvatarSelector .avatar-option').forEach(el => el.classList.remove('selected'));
            btn.classList.add('selected');
            selectedAvatar = emoji;
        };
        selector.appendChild(btn);
    });
}

initEditAvatarSelector();

function displaySearchResults(users) {
    const container = document.getElementById('searchResults');
    container.innerHTML = '';
    if (users.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:20px;color:#999;">未找到用户</div>';
        return;
    }
    users.forEach(user => {
        const item = document.createElement('div');
        item.className = 'user-list-item';
        item.innerHTML = `
            <div class="user-avatar">${user.avatar}</div>
            <div class="user-info">
                <div class="user-name">${user.nickname}</div>
                <div class="user-account">账号: ${user.accountId}</div>
            </div>
            <button class="user-action-btn" onclick="viewUserProfile('${user.accountId}')">查看</button>
        `;
        container.appendChild(item);
    });
}

function displayFriendRequests(requests) {
    const container = document.getElementById('requestsList');
    container.innerHTML = '';
    if (requests.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:20px;color:#999;">暂无好友请求</div>';
        return;
    }
    requests.forEach(req => {
        const item = document.createElement('div');
        item.className = 'user-list-item';
        item.innerHTML = `
            <div class="user-avatar">${req.fromAvatar}</div>
            <div class="user-info">
                <div class="user-name">${req.fromNickname}</div>
                <div class="user-account">账号: ${req.from}</div>
            </div>
            <div style="display:flex;gap:8px;">
                <button class="user-action-btn" onclick="handleFriendRequest('${req.from}', true)">接受</button>
                <button class="user-action-btn" style="background:#666;" onclick="handleFriendRequest('${req.from}', false)">拒绝</button>
            </div>
        `;
        container.appendChild(item);
    });
}

function displayFriendsList(friends) {
    const container = document.getElementById('friendsList');
    container.innerHTML = '';
    if (friends.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:20px;color:#999;">暂无好友</div>';
        return;
    }
    friends.forEach(friend => {
        const item = document.createElement('div');
        item.className = 'user-list-item';
        item.innerHTML = `
            <div class="user-avatar">${friend.avatar}</div>
            <div class="user-info">
                <div class="user-name">${friend.nickname} ${friend.online ? '<span style="color:#4CAF50;">●</span>' : '<span style="color:#999;">●</span>'}</div>
                <div class="user-account">${friend.signature || '这个人很懒，什么都没写'}</div>
            </div>
            <button class="user-action-btn" onclick="viewUserProfile('${friend.accountId}')">查看</button>
        `;
        container.appendChild(item);
    });
}

function displayInviteFriendsList(friends) {
    const container = document.getElementById('inviteFriendList');
    container.innerHTML = '';
    if (friends.length === 0) {
        container.innerHTML = '<div style="text-align:center;padding:20px;color:#999;">暂无好友</div>';
        return;
    }
    friends.forEach(friend => {
        const item = document.createElement('div');
        item.className = 'user-list-item';
        item.innerHTML = `
            <div class="user-avatar">${friend.avatar}</div>
            <div class="user-info">
                <div class="user-name">${friend.nickname}</div>
                <div class="user-account">${friend.online ? '在线' : '离线'}</div>
            </div>
            <button class="user-action-btn" onclick="inviteFriend('${friend.accountId}')" ${!friend.online ? 'disabled' : ''}>邀请</button>
        `;
        container.appendChild(item);
    });
}

window.inviteFriend = function(accountId) {
    console.log('邀请好友:', accountId, '到房间:', currentRoomId);
    ws.send(JSON.stringify({ type: TYPE_INVITE_TO_ROOM, roomId: currentRoomId, targetAccountId: accountId }));
    inviteFriendModal.classList.add('hidden');
};

window.viewUserProfile = function(accountId) {
    ws.send(JSON.stringify({ type: TYPE_GET_USER_INFO, accountId }));
};

window.handleFriendRequest = function(accountId, accept) {
    ws.send(JSON.stringify({ type: TYPE_HANDLE_FRIEND_REQUEST, fromAccountId: accountId, accept }));
};

function showUserProfile(user, isSelf) {
    currentUserInfo = user;
    document.getElementById('profileAvatar').textContent = user.avatar;
    document.getElementById('profileNickname').textContent = user.nickname;
    document.getElementById('profileAccount').textContent = '账号: ' + user.accountId;
    document.getElementById('profileSignature').textContent = user.signature || '这个人很懒，什么都没写';

    const editBtn = document.getElementById('editProfileBtn');
    const addFriendBtn = document.getElementById('addFriendBtn');

    if (isSelf) {
        editBtn.style.display = 'block';
        addFriendBtn.style.display = 'none';
    } else {
        editBtn.style.display = 'none';
        addFriendBtn.style.display = 'block';
        addFriendBtn.onclick = () => {
            ws.send(JSON.stringify({ type: TYPE_SEND_FRIEND_REQUEST, targetAccountId: user.accountId }));
        };
    }

    profileViewModal.classList.remove('hidden');
}

// ===== 右键菜单 =====
function showContextMenu(e, targetUsername, targetAccountId) {
    e.preventDefault();
    contextMenuTarget = { username: targetUsername, accountId: targetAccountId };

    const menuItems = [
        { text: '@提及', action: () => mentionUser(targetUsername) },
        { text: '查看主页', action: () => viewUserProfile(targetAccountId) },
        { text: '添加好友', action: () => sendFriendRequest(targetAccountId) }
    ];

    contextMenu.innerHTML = '';
    menuItems.forEach(item => {
        const menuItem = document.createElement('div');
        menuItem.className = 'context-menu-item';
        menuItem.textContent = item.text;
        menuItem.onclick = () => {
            item.action();
            contextMenu.classList.remove('show');
        };
        contextMenu.appendChild(menuItem);
    });

    contextMenu.style.left = e.pageX + 'px';
    contextMenu.style.top = e.pageY + 'px';
    contextMenu.classList.add('show');
}

function mentionUser(username) {
    messageInput.value += `@${username} `;
    messageInput.focus();
}

function sendFriendRequest(accountId) {
    ws.send(JSON.stringify({ type: TYPE_SEND_FRIEND_REQUEST, targetAccountId: accountId }));
}

document.addEventListener('click', () => {
    contextMenu.classList.remove('show');
});

// 点击模态框背景关闭
friendsModal.addEventListener('click', (e) => {
    if (e.target === friendsModal) friendsModal.classList.add('hidden');
});

profileViewModal.addEventListener('click', (e) => {
    if (e.target === profileViewModal) profileViewModal.classList.add('hidden');
});

editProfileModal.addEventListener('click', (e) => {
    if (e.target === editProfileModal) editProfileModal.classList.add('hidden');
});

inviteFriendModal.addEventListener('click', (e) => {
    if (e.target === inviteFriendModal) inviteFriendModal.classList.add('hidden');
});

window.addEventListener('beforeunload', () => {
    if (ws) {
        isManualClose = true;
        stopHeartbeat();
        if (reconnectTimer) clearTimeout(reconnectTimer);
        ws.close();
    }
});
