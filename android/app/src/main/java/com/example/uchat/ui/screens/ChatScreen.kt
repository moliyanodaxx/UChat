package com.example.uchat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uchat.data.model.ChatMessage
import com.example.uchat.data.model.Friend
import com.example.uchat.data.model.RoomInfo
import com.example.uchat.data.model.RoomMember
import com.example.uchat.data.model.UserData
import com.example.uchat.util.AvatarUtil
import com.example.uchat.viewmodel.ChatViewModel

private val EMOJI_DATA = listOf(
    "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","😉","😊","😇","🥰","😍","🤩","😘",
    "😗","😚","😙","🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨",
    "😐","😑","😶","😏","😒","🙄","😬","🤥","😌","😔","😪","🤤","😴","😷","🤒","🤕",
    "🤢","🤮","🤧","🥵","🥶","🥴","😵","🤯","🤠","🥳","🥸","😎","🤓","🧐","😕","😟",
    "😮","😯","😲","😳","🥺","😦","😧","😨","😰","😥","😢","😭","😱","😖",
    "😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","💩","🤡",
    "👹","👺","👻","👽","👾","🤖","😺","😸","😹","😻","😼","😽","🙀","😿","😾","🙈"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    roomId: String,
    vm: ChatViewModel = viewModel(),
    onBack: () -> Unit
) {
    val allMessages by vm.messages.collectAsState()
    val messages = allMessages[roomId] ?: emptyList()
    val currentRoomName by vm.currentRoomName.collectAsState()
    val onlineCount by vm.onlineCount.collectAsState()
    val roomMembers by vm.roomMembers.collectAsState()
    val isRoomOwner by vm.isRoomOwner.collectAsState()
    val currentRoomInfo by vm.currentRoomInfo.collectAsState()
    val myProfile by vm.myProfile.collectAsState()
    val friends by vm.friends.collectAsState()
    val viewedUser by vm.viewedUser.collectAsState()
    val viewedUserIsSelf by vm.viewedUserIsSelf.collectAsState()

    var messageText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showMembersSheet by remember { mutableStateOf(false) }
    var showRoomInfoDialog by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showUserProfileDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            try {
                listState.animateScrollToItem(messages.size - 1)
            } catch (_: Exception) {
                listState.scrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(viewedUser) {
        if (viewedUser != null) showUserProfileDialog = true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                title = {
                    Column {
                        Text(currentRoomName.ifEmpty { roomId })
                        if (onlineCount > 0) {
                            Text(
                                "${onlineCount}人在线",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vm.requestRoomMembers()
                        showMembersSheet = true
                    }) {
                        Icon(Icons.Default.Group, "成员")
                    }
                    IconButton(onClick = {
                        vm.requestRoomMembers()
                        showRoomInfoDialog = true
                    }) {
                        Icon(Icons.Default.Info, "房间信息")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                lazyItems(messages, key = { msg ->
                    when (msg) {
                        is ChatMessage.UserMessage -> msg.id
                        is ChatMessage.SystemMessage -> msg.id
                        is ChatMessage.InviteMessage -> msg.id
                    }
                }) { msg ->
                    when (msg) {
                        is ChatMessage.UserMessage -> UserMessageBubble(
                            msg = msg,
                            onAvatarClick = { vm.getUserInfo(msg.accountId) }
                        )
                        is ChatMessage.SystemMessage -> SystemMessageItem(msg)
                        is ChatMessage.InviteMessage -> InviteMessageItem(
                            msg = msg,
                            currentRoomId = roomId,
                            onAccept = { vm.joinRoom(msg.roomId, "") },
                            onAcceptWithPassword = { pwd -> vm.joinRoom(msg.roomId, pwd) },
                            onUpdateStatus = { accepted -> vm.updateInviteStatus(roomId, msg.id, accepted) }
                        )
                    }
                }
            }

            if (showEmojiPicker) {
                EmojiPickerPanel(onEmojiSelected = { emoji -> messageText += emoji })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                    Icon(
                        Icons.Default.EmojiEmotions,
                        "表情",
                        tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息...") },
                    maxLines = 4,
                    shape = RoundedCornerShape(24.dp)
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            vm.sendMessage(messageText)
                            messageText = ""
                            showEmojiPicker = false
                        }
                    },
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Send,
                        "发送",
                        tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showMembersSheet) {
        MembersBottomSheet(
            members = roomMembers,
            isOwner = isRoomOwner,
            myAccountId = myProfile.accountId,
            onDismiss = { showMembersSheet = false },
            onKick = { uid -> vm.kickMember(uid) },
            onTransfer = { uid -> vm.transferOwner(uid) },
            onViewProfile = { uid -> vm.getUserInfo(uid) },
            onInviteFriend = {
                vm.getFriends()
                showInviteDialog = true
                showMembersSheet = false
            }
        )
    }

    if (showRoomInfoDialog) {
        RoomInfoDialog(
            roomInfo = currentRoomInfo,
            isOwner = isRoomOwner,
            onDismiss = { showRoomInfoDialog = false },
            onChangePassword = {
                showRoomInfoDialog = false
                showChangePasswordDialog = true
            },
            onLeaveRoom = {
                vm.leaveRoom(roomId)
                showRoomInfoDialog = false
                onBack()
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { newPwd ->
                vm.updateRoomPassword(newPwd)
                showChangePasswordDialog = false
            }
        )
    }

    if (showInviteDialog) {
        InviteFriendDialog(
            friends = friends,
            onDismiss = { showInviteDialog = false },
            onInvite = { accountId ->
                vm.inviteToRoom(accountId)
                showInviteDialog = false
            }
        )
    }

    if (showUserProfileDialog && viewedUser != null) {
        UserProfileDialog(
            user = viewedUser!!,
            isSelf = viewedUserIsSelf,
            onDismiss = {
                showUserProfileDialog = false
                vm.clearViewedUser()
            },
            onSendFriendRequest = { vm.sendFriendRequest(viewedUser!!.accountId) }
        )
    }
}

@Composable
private fun UserMessageBubble(
    msg: ChatMessage.UserMessage,
    onAvatarClick: () -> Unit
) {
    val isOwn = msg.isOwn
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isOwn) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onAvatarClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = msg.avatar.ifEmpty { AvatarUtil.getAvatarForAccount(msg.accountId) },
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            if (!isOwn) {
                Text(
                    text = msg.username,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isOwn) 16.dp else 4.dp,
                    topEnd = if (isOwn) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isOwn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = msg.msg,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = msg.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (isOwn) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = msg.avatar.ifEmpty { AvatarUtil.getAvatarForAccount(msg.accountId) },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun SystemMessageItem(msg: ChatMessage.SystemMessage) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Text(
                text = msg.msg,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InviteMessageItem(
    msg: ChatMessage.InviteMessage,
    currentRoomId: String,
    onAccept: () -> Unit,
    onAcceptWithPassword: (String) -> Unit,
    onUpdateStatus: (Boolean) -> Unit
) {
    var showPwdDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${msg.from} 邀请你加入",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    msg.roomName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    msg.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))
                when (msg.accepted) {
                    true -> Text("已接受", color = MaterialTheme.colorScheme.primary)
                    false -> Text("已拒绝", color = MaterialTheme.colorScheme.error)
                    null -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            if (msg.needPassword) showPwdDialog = true
                            else {
                                onUpdateStatus(true)
                                onAccept()
                            }
                        }) { Text("接受") }
                        OutlinedButton(onClick = { onUpdateStatus(false) }) { Text("拒绝") }
                    }
                }
            }
        }
    }

    if (showPwdDialog) {
        var pwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPwdDialog = false },
            title = { Text("输入房间密码") },
            text = {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { if (it.length <= 6) pwd = it },
                    label = { Text("密码（6位）") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateStatus(true)
                    onAcceptWithPassword(pwd)
                    showPwdDialog = false
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showPwdDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun EmojiPickerPanel(onEmojiSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        tonalElevation = 4.dp
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            gridItems(EMOJI_DATA) { emoji ->
                TextButton(
                    onClick = { onEmojiSelected(emoji) },
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.aspectRatio(1f)
                ) {
                    Text(emoji, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MembersBottomSheet(
    members: List<RoomMember>,
    isOwner: Boolean,
    myAccountId: String,
    onDismiss: () -> Unit,
    onKick: (String) -> Unit,
    onTransfer: (String) -> Unit,
    onViewProfile: (String) -> Unit,
    onInviteFriend: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("房间成员 (${members.size})", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onInviteFriend) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("邀请好友")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            members.forEach { member ->
                MemberItem(
                    member = member,
                    isOwner = isOwner,
                    isSelf = member.uid == myAccountId,
                    onViewProfile = { onViewProfile(member.uid) },
                    onKick = { onKick(member.uid) },
                    onTransfer = { onTransfer(member.uid) }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MemberItem(
    member: RoomMember,
    isOwner: Boolean,
    isSelf: Boolean,
    onViewProfile: () -> Unit,
    onKick: () -> Unit,
    onTransfer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProfile)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                member.avatar.ifBlank { AvatarUtil.getAvatarForAccount(member.uid) },
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member.username, style = MaterialTheme.typography.bodyMedium)
                if (member.isOwner) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            "房主",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            Text(
                if (member.isOnline) "在线" else "离线",
                style = MaterialTheme.typography.labelSmall,
                color = if (member.isOnline) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
            )
        }
        if (isOwner && !isSelf && !member.isOwner) {
            IconButton(onClick = onKick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.PersonRemove, "踢出", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onTransfer, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.SwapHoriz, "转让", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RoomInfoDialog(
    roomInfo: RoomInfo?,
    isOwner: Boolean,
    onDismiss: () -> Unit,
    onChangePassword: () -> Unit,
    onLeaveRoom: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("房间信息") },
        text = {
            if (roomInfo == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoRow("房间名", roomInfo.name)
                    InfoRow("房间号", roomInfo.id)
                    if (isOwner) InfoRow("密码", roomInfo.password.ifEmpty { "无" })
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isOwner) {
                    Button(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
                        Text("修改密码")
                    }
                }
                OutlinedButton(
                    onClick = onLeaveRoom,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("退出房间")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("关闭")
                }
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改房间密码") },
        text = {
            OutlinedTextField(
                value = pwd,
                onValueChange = { if (it.length <= 6) pwd = it },
                label = { Text("新密码（6位）") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pwd) },
                enabled = pwd.length == 6
            ) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun InviteFriendDialog(
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("邀请好友") },
        text = {
            if (friends.isEmpty()) {
                Text("暂无好友", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    lazyItems(friends) { friend ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInvite(friend.accountId) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                friend.avatar.ifEmpty { "😀" },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(friend.nickname, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (friend.online) "在线" else "离线",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (friend.online) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline
                                )
                            }
                            TextButton(onClick = { onInvite(friend.accountId) }) { Text("邀请") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun UserProfileDialog(
    user: UserData,
    isSelf: Boolean,
    onDismiss: () -> Unit,
    onSendFriendRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.avatar.ifEmpty { "😀" }, style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(user.nickname, style = MaterialTheme.typography.titleMedium)
                    Text(
                        user.accountId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Text(
                text = if (user.signature.isNotEmpty()) "\"${user.signature}\"" else " ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isSelf) {
                    Button(onClick = {
                        onSendFriendRequest()
                        onDismiss()
                    }) { Text("加好友") }
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}
