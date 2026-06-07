package com.example.uchat.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.uchat.data.model.*
import com.example.uchat.data.remote.WebSocketClient
import com.example.uchat.util.PrefsManager
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    val prefs = PrefsManager(application)
    val wsClient = WebSocketClient()

    // Auth state
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _needProfile = MutableStateFlow(false)
    val needProfile: StateFlow<Boolean> = _needProfile

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError

    private val _isRegistering = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering

    // Connection
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting

    // User profile
    private val _myProfile = MutableStateFlow(UserData())
    val myProfile: StateFlow<UserData> = _myProfile

    // Rooms
    private val _rooms = MutableStateFlow<List<RoomInfo>>(emptyList())
    val rooms: StateFlow<List<RoomInfo>> = _rooms

    private val _currentRoomId = MutableStateFlow("")
    val currentRoomId: StateFlow<String> = _currentRoomId

    private val _currentRoomName = MutableStateFlow("")
    val currentRoomName: StateFlow<String> = _currentRoomName

    private val _onlineCount = MutableStateFlow(0)
    val onlineCount: StateFlow<Int> = _onlineCount

    // Messages per room
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages

    // Room members
    private val _roomMembers = MutableStateFlow<List<RoomMember>>(emptyList())
    val roomMembers: StateFlow<List<RoomMember>> = _roomMembers

    private val _isRoomOwner = MutableStateFlow(false)
    val isRoomOwner: StateFlow<Boolean> = _isRoomOwner

    private val _currentRoomInfo = MutableStateFlow<RoomInfo?>(null)
    val currentRoomInfo: StateFlow<RoomInfo?> = _currentRoomInfo

    // Friends
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _friendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val friendRequests: StateFlow<List<FriendRequest>> = _friendRequests

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    // Viewed user profile
    private val _viewedUser = MutableStateFlow<UserData?>(null)
    val viewedUser: StateFlow<UserData?> = _viewedUser

    private val _viewedUserIsSelf = MutableStateFlow(false)
    val viewedUserIsSelf: StateFlow<Boolean> = _viewedUserIsSelf

    // User avatar cache
    private val _userAvatarCache = MutableStateFlow<Map<String, String>>(emptyMap())
    val userAvatarCache: StateFlow<Map<String, String>> = _userAvatarCache

    // Toast/snackbar messages
    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessage: SharedFlow<String> = _toastMessage

    // Navigate to chat room (one-shot, consumed after use)
    private val _navigateToRoom = MutableStateFlow("")
    val navigateToRoom: StateFlow<String> = _navigateToRoom
    private var pendingNavigate = false

    fun consumeNavigateToRoom() { _navigateToRoom.value = "" }

    // Server URL
    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl

    init {
        viewModelScope.launch {
            prefs.serverUrl.collect { _serverUrl.value = it }
        }
        viewModelScope.launch {
            wsClient.connectionState.collect { connected ->
                _connected.value = connected
                if (!connected) _connecting.value = false
            }
        }
        viewModelScope.launch {
            wsClient.messages.collect { handleMessage(it) }
        }
    }

    fun setRegistering(value: Boolean) { _isRegistering.value = value }
    fun clearAuthError() { _authError.value = null }

    fun connect(accountId: String, password: String, serverUrl: String, register: Boolean = false) {
        if (_connecting.value) return
        if (!serverUrl.startsWith("ws://") && !serverUrl.startsWith("wss://")) {
            _authError.value = "服务器地址格式错误，请以 ws:// 或 wss:// 开头"
            return
        }
        if (serverUrl == "ws://" || serverUrl == "wss://") {
            _authError.value = "请填写完整的服务器地址"
            return
        }
        _connecting.value = true
        _authError.value = null
        _myProfile.value = _myProfile.value.copy(accountId = accountId)
        viewModelScope.launch {
            try {
                prefs.saveServerUrl(serverUrl)
                wsClient.serverUrl = serverUrl
                wsClient.disconnect()
                wsClient.connect(serverUrl)
                // Wait for connection (timeout after 10s)
                val connected = withTimeoutOrNull(10_000) {
                    wsClient.connectionState.filter { it }.first()
                }
                if (connected == null) {
                    _authError.value = "连接服务器超时，请检查地址"
                    _connecting.value = false
                    return@launch
                }
                val obj = JsonObject().apply {
                    addProperty("type", if (register) 14 else 15)
                    addProperty("accountId", accountId)
                    addProperty("password", password)
                }
                wsClient.send(obj)
            } catch (e: Exception) {
                _authError.value = "连接失败：${e.message}"
                _connecting.value = false
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || _currentRoomId.value.isEmpty()) return
        val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        val obj = JsonObject().apply {
            addProperty("type", MsgType.MSG)
            addProperty("msg", text)
            addProperty("time", now)
        }
        wsClient.send(obj)
    }

    fun createRoom(name: String, roomId: String?, password: String) {
        val obj = JsonObject().apply {
            addProperty("type", MsgType.CREATE_ROOM)
            addProperty("roomName", name)
            if (!roomId.isNullOrBlank()) addProperty("roomId", roomId)
            addProperty("password", password)
        }
        wsClient.send(obj)
    }

    fun joinRoom(roomId: String, password: String) {
        val obj = JsonObject().apply {
            addProperty("type", MsgType.JOIN_ROOM)
            addProperty("roomId", roomId)
            addProperty("password", password)
        }
        wsClient.send(obj)
    }

    fun switchRoom(roomId: String) {
        if (roomId == _currentRoomId.value) return
        val obj = JsonObject().apply {
            addProperty("type", MsgType.SWITCH_ROOM)
            addProperty("roomId", roomId)
        }
        wsClient.send(obj)
    }

    fun leaveRoom(roomId: String) {
        val obj = JsonObject().apply {
            addProperty("type", MsgType.LEAVE_ROOM)
            addProperty("roomId", roomId)
        }
        wsClient.send(obj)
    }

    fun requestRoomMembers() {
        val roomId = _currentRoomId.value
        if (roomId.isEmpty()) return
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.ROOM_MEMBERS)
            addProperty("roomId", roomId)
        })
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.GET_ROOM_INFO)
            addProperty("roomId", roomId)
        })
    }

    fun kickMember(targetUid: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.KICK_MEMBER)
            addProperty("roomId", _currentRoomId.value)
            addProperty("targetUid", targetUid)
        })
    }

    fun transferOwner(targetUid: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.TRANSFER_OWNER)
            addProperty("roomId", _currentRoomId.value)
            addProperty("targetUid", targetUid)
        })
    }

    fun updateRoomPassword(newPassword: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.UPDATE_ROOM_PASSWORD)
            addProperty("roomId", _currentRoomId.value)
            addProperty("newPassword", newPassword)
        })
    }

    fun setProfile(nickname: String, avatar: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.SET_PROFILE)
            addProperty("nickname", nickname)
            addProperty("avatar", avatar)
        })
    }

    fun updateProfile(nickname: String, avatar: String, signature: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.UPDATE_PROFILE)
            addProperty("nickname", nickname)
            addProperty("avatar", avatar)
            addProperty("signature", signature)
        })
    }

    fun searchUser(keyword: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.SEARCH_USER)
            addProperty("keyword", keyword)
        })
    }

    fun getUserInfo(accountId: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.GET_USER_INFO)
            addProperty("accountId", accountId)
        })
    }

    fun sendFriendRequest(targetAccountId: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.SEND_FRIEND_REQUEST)
            addProperty("targetAccountId", targetAccountId)
        })
    }

    fun getFriendRequests() {
        wsClient.send(JsonObject().apply { addProperty("type", MsgType.GET_FRIEND_REQUESTS) })
    }

    fun handleFriendRequest(fromAccountId: String, accept: Boolean) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.HANDLE_FRIEND_REQUEST)
            addProperty("fromAccountId", fromAccountId)
            addProperty("accept", accept)
        })
    }

    fun getFriends() {
        wsClient.send(JsonObject().apply { addProperty("type", MsgType.GET_FRIENDS) })
    }

    fun inviteToRoom(targetAccountId: String) {
        wsClient.send(JsonObject().apply {
            addProperty("type", MsgType.INVITE_TO_ROOM)
            addProperty("roomId", _currentRoomId.value)
            addProperty("targetAccountId", targetAccountId)
        })
    }

    fun clearSearchResults() { _searchResults.value = emptyList() }
    fun clearViewedUser() { _viewedUser.value = null }

    private fun addMessageToRoom(roomId: String, msg: ChatMessage) {
        val current = _messages.value.toMutableMap()
        val list = current[roomId]?.toMutableList() ?: mutableListOf()
        list.add(msg)
        current[roomId] = list
        _messages.value = current
    }

    private fun handleMessage(data: JsonObject) {
        val typeEl = data.get("type") ?: return
        // Handle string type (e.g. NEW_SYSTEM_MSG)
        if (typeEl.isJsonPrimitive && typeEl.asJsonPrimitive.isString) {
            when (typeEl.asString) {
                "NEW_SYSTEM_MSG" -> viewModelScope.launch { _toastMessage.emit("新系统消息") }
            }
            return
        }
        val type = typeEl.asInt
        when (type) {
            MsgType.REGISTER -> {
                val success = data.get("success")?.asBoolean ?: false
                _connecting.value = false
                if (success) {
                    viewModelScope.launch { _toastMessage.emit("注册成功，请登录") }
                    _isRegistering.value = false
                } else {
                    _authError.value = data.get("error")?.asString ?: "注册失败"
                }
            }
            MsgType.LOGIN -> {
                val success = data.get("success")?.asBoolean ?: false
                _connecting.value = false
                if (success) {
                    val needProfile = data.get("needProfile")?.asBoolean ?: false
                    val userDataEl = data.get("userData")
                    val userData = if (userDataEl?.isJsonObject == true) userDataEl.asJsonObject else null
                    val nickname = userData?.get("nickname")?.asString ?: ""
                    val avatar = userData?.get("avatar")?.asString ?: ""
                    val signature = userData?.get("signature")?.asString ?: ""
                    _myProfile.value = _myProfile.value.copy(nickname = nickname, avatar = avatar, signature = signature)
                    viewModelScope.launch { prefs.saveProfile(nickname, avatar, signature) }
                    _needProfile.value = needProfile
                    _isLoggedIn.value = true
                    // 如果不需要设置个人资料（老用户），立即触发服务器推送房间列表
                    if (!needProfile && nickname.isNotBlank()) {
                        wsClient.send(JsonObject().apply {
                            addProperty("type", MsgType.SET_USERNAME)
                            addProperty("username", nickname)
                        })
                    }
                } else {
                    _authError.value = data.get("error")?.asString ?: "登录失败"
                }
            }
            MsgType.SET_PROFILE -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val userData = data.getAsJsonObject("userData")
                    val nickname = userData?.get("nickname")?.asString ?: ""
                    val avatar = userData?.get("avatar")?.asString ?: ""
                    _myProfile.value = _myProfile.value.copy(nickname = nickname, avatar = avatar)
                    viewModelScope.launch { prefs.saveProfile(nickname, avatar) }
                    _needProfile.value = false
                    // 新用户设置完个人资料后，通知服务端触发房间列表推送
                    wsClient.send(JsonObject().apply {
                        addProperty("type", MsgType.SET_USERNAME)
                        addProperty("username", nickname)
                    })
                }
            }
            MsgType.UPDATE_PROFILE -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val user = data.getAsJsonObject("user")
                    val nickname = user?.get("nickname")?.asString ?: ""
                    val avatar = user?.get("avatar")?.asString ?: ""
                    val sig = user?.get("signature")?.asString ?: ""
                    _myProfile.value = _myProfile.value.copy(nickname = nickname, avatar = avatar, signature = sig)
                    viewModelScope.launch { prefs.saveProfile(nickname, avatar, sig) }
                    viewModelScope.launch { _toastMessage.emit("资料已更新") }
                }
            }
            MsgType.ROOM_LIST -> {
                val roomsArr = data.getAsJsonArray("rooms") ?: return
                val list = roomsArr.map { el ->
                    val r = el.asJsonObject
                    RoomInfo(
                        id = r.get("id")?.asString ?: "",
                        name = r.get("name")?.asString ?: "",
                        isSystem = r.get("isSystem")?.asBoolean ?: false,
                        memberCount = r.get("memberCount")?.asInt ?: 0
                    )
                }
                val oldIds = _rooms.value.map { it.id }.toSet()
                val isFirstLoad = oldIds.isEmpty()
                _rooms.value = list
                // 只在没有当前房间时设置默认房间，不自动导航
                val currentRoom = data.get("currentRoom")?.asString ?: ""
                if (currentRoom.isNotEmpty() && _currentRoomId.value.isEmpty()) {
                    _currentRoomId.value = currentRoom
                    _currentRoomName.value = list.find { it.id == currentRoom }?.name ?: currentRoom
                }
                // 新加入了一个房间（非首次加载），自动 switch 并导航
                if (!isFirstLoad) {
                    val newRoom = list.firstOrNull { it.id !in oldIds && !it.isSystem }
                    if (newRoom != null) {
                        pendingNavigate = true
                        switchRoom(newRoom.id)
                    }
                }
            }
            MsgType.CREATE_ROOM -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val room = data.getAsJsonObject("room")
                    val id = room?.get("id")?.asString ?: ""
                    val name = room?.get("name")?.asString ?: ""
                    val memberCount = room?.get("memberCount")?.asInt ?: 1
                    // 手动把新房间加入列表
                    val newRoom = RoomInfo(id = id, name = name, memberCount = memberCount)
                    _rooms.value = _rooms.value + newRoom
                    viewModelScope.launch { _toastMessage.emit("房间 $name 创建成功") }
                    if (id.isNotEmpty()) { pendingNavigate = true; switchRoom(id) }
                } else {
                    val err = data.get("error")?.asString ?: "创建失败"
                    viewModelScope.launch { _toastMessage.emit(err) }
                }
            }
            MsgType.JOIN_ROOM -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    // 重新请求房间列表让服务器推送最新数据
                    wsClient.send(JsonObject().apply {
                        addProperty("type", MsgType.SET_USERNAME)
                        addProperty("username", _myProfile.value.nickname)
                    })
                } else {
                    val err = data.get("error")?.asString ?: "加入失败"
                    viewModelScope.launch { _toastMessage.emit(err) }
                }
            }
            MsgType.SWITCH_ROOM -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val roomId = data.get("roomId")?.asString ?: ""
                    val count = data.get("onlineCount")?.asInt ?: 0
                    _currentRoomId.value = roomId
                    _currentRoomName.value = _rooms.value.find { it.id == roomId }?.name ?: roomId
                    _onlineCount.value = count
                    if (pendingNavigate) {
                        pendingNavigate = false
                        _navigateToRoom.value = roomId
                    }
                }
            }
            MsgType.LEAVE_ROOM -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val roomId = data.get("roomId")?.asString ?: ""
                    val kicked = data.get("kicked")?.asBoolean ?: false
                    if (kicked) viewModelScope.launch { _toastMessage.emit("你已被踢出房间") }
                    if (_currentRoomId.value == roomId) {
                        _currentRoomId.value = ""
                        _currentRoomName.value = ""
                        _onlineCount.value = 0
                    }
                }
            }
            MsgType.HISTORY -> {
                val roomId = data.get("roomId")?.asString ?: return
                val msgsArr = data.getAsJsonArray("messages") ?: return
                val list = msgsArr.mapNotNull { el ->
                    try {
                        val m = el.asJsonObject
                        val t = m.get("type")
                        if (t == null || t.isJsonNull) return@mapNotNull null
                        if (t.isJsonPrimitive && t.asJsonPrimitive.isString) {
                            if (t.asString == "ROOM_INVITE") {
                                ChatMessage.InviteMessage(
                                    from = m.get("from")?.asString ?: "",
                                    roomName = m.get("roomName")?.asString ?: "",
                                    roomId = m.get("roomId")?.asString ?: "",
                                    time = m.get("time")?.asString ?: "",
                                    needPassword = m.get("needPassword")?.asBoolean ?: false
                                )
                            } else null
                        } else {
                            when (t.asInt) {
                                MsgType.MSG -> ChatMessage.UserMessage(
                                    username = m.get("username")?.asString ?: "",
                                    msg = m.get("msg")?.asString ?: "",
                                    time = m.get("time")?.asString ?: "",
                                    accountId = m.get("accountId")?.asString ?: "",
                                    avatar = m.get("avatar")?.asString ?: "",
                                    isOwn = m.get("accountId")?.asString == _myProfile.value.accountId
                                )
                                MsgType.ENTER, MsgType.LEAVE -> ChatMessage.SystemMessage(
                                    msg = m.get("msg")?.asString ?: ""
                                )
                                else -> null
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ChatViewModel", "HISTORY parse error", e)
                        null
                    }
                }
                val current = _messages.value.toMutableMap()
                current[roomId] = list
                _messages.value = current
            }
            MsgType.MSG -> {
                val roomId = _currentRoomId.value
                val username = data.get("username")?.asString ?: ""
                val msg = data.get("msg")?.asString ?: ""
                val time = data.get("time")?.asString ?: ""
                val accountId = data.get("accountId")?.asString ?: ""
                val avatar = data.get("avatar")?.asString ?: ""
                addMessageToRoom(roomId, ChatMessage.UserMessage(
                    username = username, msg = msg, time = time,
                    accountId = accountId, avatar = avatar,
                    isOwn = accountId == _myProfile.value.accountId
                ))
            }
            MsgType.ENTER -> {
                val msg = data.get("msg")?.asString ?: ""
                val count = data.get("onlineCount")?.asInt ?: _onlineCount.value
                _onlineCount.value = count
                addMessageToRoom(_currentRoomId.value, ChatMessage.SystemMessage(msg = msg))
            }
            MsgType.LEAVE -> {
                val msg = data.get("msg")?.asString ?: ""
                val count = data.get("onlineCount")?.asInt ?: _onlineCount.value
                _onlineCount.value = count
                addMessageToRoom(_currentRoomId.value, ChatMessage.SystemMessage(msg = msg))
            }
            MsgType.ROOM_MEMBERS -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val membersArr = data.getAsJsonArray("members") ?: return
                    _roomMembers.value = membersArr.map { el ->
                        val m = el.asJsonObject
                        RoomMember(
                            uid = m.get("uid")?.asString ?: "",
                            username = m.get("username")?.asString ?: "",
                            avatar = m.get("avatar")?.asString ?: "",
                            signature = m.get("signature")?.asString ?: "",
                            isOwner = m.get("isOwner")?.asBoolean ?: false,
                            isOnline = m.get("isOnline")?.asBoolean ?: false
                        )
                    }
                    _isRoomOwner.value = data.get("isOwner")?.asBoolean ?: false
                }
            }
            MsgType.GET_ROOM_INFO -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val ri = data.getAsJsonObject("roomInfo")
                    _currentRoomInfo.value = RoomInfo(
                        id = ri?.get("id")?.asString ?: "",
                        name = ri?.get("name")?.asString ?: "",
                        password = ri?.get("password")?.asString ?: "",
                        isOwner = ri?.get("isOwner")?.asBoolean ?: false
                    )
                }
            }
            MsgType.KICK_MEMBER -> {
                viewModelScope.launch { _toastMessage.emit("操作成功") }
                requestRoomMembers()
            }
            MsgType.TRANSFER_OWNER -> {
                viewModelScope.launch { _toastMessage.emit("已转让房主") }
                requestRoomMembers()
            }
            MsgType.SEARCH_USER -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val arr = data.getAsJsonArray("results") ?: return
                    _searchResults.value = arr.map { el ->
                        val u = el.asJsonObject
                        SearchResult(
                            accountId = u.get("accountId")?.asString ?: "",
                            nickname = u.get("nickname")?.asString ?: "",
                            avatar = u.get("avatar")?.asString ?: ""
                        )
                    }
                }
            }
            MsgType.GET_USER_INFO -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val u = data.getAsJsonObject("user")
                    val accountId = u?.get("accountId")?.asString ?: ""
                    _viewedUser.value = UserData(
                        accountId = accountId,
                        nickname = u?.get("nickname")?.asString ?: "",
                        avatar = u?.get("avatar")?.asString ?: "",
                        signature = u?.get("signature")?.asString ?: ""
                    )
                    // Check if viewing self by comparing accountId
                    _viewedUserIsSelf.value = accountId == _myProfile.value.accountId
                    // Update own profile if it's self
                    if (_viewedUserIsSelf.value) {
                        _myProfile.value = _viewedUser.value!!
                    }
                }
            }
            MsgType.SEND_FRIEND_REQUEST -> {
                viewModelScope.launch { _toastMessage.emit("好友请求已发送") }
            }
            MsgType.GET_FRIEND_REQUESTS -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val arr = data.getAsJsonArray("requests") ?: return
                    _friendRequests.value = arr.map { el ->
                        val r = el.asJsonObject
                        FriendRequest(
                            from = r.get("from")?.asString ?: "",
                            fromNickname = r.get("fromNickname")?.asString ?: "",
                            fromAvatar = r.get("fromAvatar")?.asString ?: ""
                        )
                    }
                }
            }
            MsgType.HANDLE_FRIEND_REQUEST -> {
                viewModelScope.launch { _toastMessage.emit("操作成功") }
                getFriendRequests()
                getFriends()
            }
            MsgType.GET_FRIENDS -> {
                val success = data.get("success")?.asBoolean ?: false
                if (success) {
                    val arr = data.getAsJsonArray("friends") ?: return
                    _friends.value = arr.map { el ->
                        val f = el.asJsonObject
                        val accountId = f.get("accountId")?.asString ?: ""
                        val avatar = f.get("avatar")?.asString ?: ""
                        // 更新缓存
                        if (accountId.isNotEmpty() && avatar.isNotEmpty()) {
                            _userAvatarCache.value = _userAvatarCache.value + (accountId to avatar)
                        }
                        Friend(
                            accountId = accountId,
                            nickname = f.get("nickname")?.asString ?: "",
                            avatar = avatar,
                            signature = f.get("signature")?.asString ?: "",
                            online = f.get("online")?.asBoolean ?: false
                        )
                    }
                }
            }
            MsgType.INVITE_TO_ROOM -> {
                // Real-time invite push from server
                val from = data.get("from")?.asString ?: ""
                val roomName = data.get("roomName")?.asString ?: ""
                val roomId = data.get("roomId")?.asString ?: ""
                val needPassword = data.get("needPassword")?.asBoolean ?: false
                val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
                addMessageToRoom(_currentRoomId.value, ChatMessage.InviteMessage(
                    from = from, roomName = roomName, roomId = roomId,
                    time = now, needPassword = needPassword
                ))
            }
            MsgType.FORCE_LOGOUT -> {
                viewModelScope.launch { 
                    _toastMessage.emit("账号在其他设备登录，已被踢下线")
                    logout()
                }
            }
        }
    }

    fun logout() {
        wsClient.disconnect()
        _isLoggedIn.value = false
        _needProfile.value = false
        _rooms.value = emptyList()
        _currentRoomId.value = ""
        _currentRoomName.value = ""
        _messages.value = emptyMap()
        _myProfile.value = UserData()
        viewModelScope.launch { prefs.clearCredentials() }
    }

    override fun onCleared() {
        super.onCleared()
        wsClient.disconnect()
    }
}
