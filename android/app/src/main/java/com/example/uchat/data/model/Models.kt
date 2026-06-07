package com.example.uchat.data.model

// WebSocket message type constants
object MsgType {
    const val ENTER = 0
    const val LEAVE = 1
    const val MSG = 2
    const val HEARTBEAT = 3
    const val SET_USERNAME = 4
    const val CREATE_ROOM = 5
    const val JOIN_ROOM = 6
    const val LEAVE_ROOM = 7
    const val ROOM_LIST = 8
    const val ROOM_MEMBERS = 9
    const val KICK_MEMBER = 10
    const val TRANSFER_OWNER = 11
    const val SWITCH_ROOM = 12
    const val HISTORY = 13
    const val REGISTER = 14
    const val LOGIN = 15
    const val SET_PROFILE = 16
    const val SEARCH_USER = 17
    const val GET_USER_INFO = 18
    const val SEND_FRIEND_REQUEST = 19
    const val GET_FRIEND_REQUESTS = 20
    const val HANDLE_FRIEND_REQUEST = 21
    const val GET_FRIENDS = 22
    const val UPDATE_PROFILE = 23
    const val GET_ROOM_INFO = 24
    const val UPDATE_ROOM_PASSWORD = 25
    const val INVITE_TO_ROOM = 26
    const val FORCE_LOGOUT = 27  // 被踢下线
    const val UPDATE_INVITE_STATUS = 28  // 更新邀请状态
}

data class UserData(
    val nickname: String = "",
    val avatar: String = "",
    val signature: String = "",
    val accountId: String = ""
)

data class RoomInfo(
    val id: String = "",
    val name: String = "",
    val isSystem: Boolean = false,
    val memberCount: Int = 0,
    val password: String = "",
    val isOwner: Boolean = false
)

data class RoomMember(
    val uid: String = "",
    val username: String = "",
    val avatar: String = "",
    val signature: String = "",
    val isOwner: Boolean = false,
    val isOnline: Boolean = false
)

data class Friend(
    val accountId: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val signature: String = "",
    val online: Boolean = false
)

data class FriendRequest(
    val from: String = "",
    val fromNickname: String = "",
    val fromAvatar: String = ""
)

data class SearchResult(
    val accountId: String = "",
    val nickname: String = "",
    val avatar: String = ""
)

private val msgIdCounter = java.util.concurrent.atomic.AtomicLong(0)
private fun nextMsgId() = msgIdCounter.incrementAndGet()

sealed class ChatMessage {
    data class UserMessage(
        val id: Long = nextMsgId(),
        val username: String,
        val msg: String,
        val time: String,
        val accountId: String,
        val avatar: String = "",
        val isOwn: Boolean = false
    ) : ChatMessage()

    data class SystemMessage(
        val id: Long = nextMsgId(),
        val msg: String
    ) : ChatMessage()

    data class InviteMessage(
        val id: Long = nextMsgId(),
        val inviteId: String = "",
        val from: String,
        val roomName: String,
        val roomId: String,
        val time: String,
        val needPassword: Boolean,
        val status: String? = null  // "pending" | "accepted" | "rejected"
    ) : ChatMessage()
}

data class ConnectionState(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val error: String? = null
)

data class SystemNotification(
    val id: Long,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
