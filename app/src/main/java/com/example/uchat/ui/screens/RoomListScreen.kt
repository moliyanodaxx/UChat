package com.example.uchat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uchat.data.model.RoomInfo
import com.example.uchat.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomListScreen(
    vm: ChatViewModel = viewModel(),
    onRoomClick: (String) -> Unit,
    onFriendsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val rooms by vm.rooms.collectAsState()
    val currentRoomId by vm.currentRoomId.collectAsState()
    val myProfile by vm.myProfile.collectAsState()
    val connected by vm.connected.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val navigateToRoom by vm.navigateToRoom.collectAsState()

    LaunchedEffect(Unit) {
        vm.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // 创建/加入房间后自动导航，消费后清空防止重复触发
    LaunchedEffect(navigateToRoom) {
        if (navigateToRoom.isNotEmpty()) {
            vm.consumeNavigateToRoom()
            onRoomClick(navigateToRoom)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("UChat")
                        Text(
                            text = if (connected) "已连接" else "未连接",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (connected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onFriendsClick) {
                        Icon(Icons.Default.People, "好友")
                    }
                    IconButton(onClick = onProfileClick) {
                        Text(
                            text = myProfile.avatar.ifEmpty { "😀" },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallFloatingActionButton(onClick = { showJoinDialog = true }) {
                    Icon(Icons.Default.Login, "加入房间")
                }
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, "创建房间")
                }
            }
        }
    ) { padding ->
        if (rooms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "还没有加入任何房间",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "点击右下角创建或加入房间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomListItem(
                        room = room,
                        isActive = room.id == currentRoomId,
                        onClick = {
                            vm.switchRoom(room.id)
                            onRoomClick(room.id)
                        }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, roomId, password ->
                vm.createRoom(name, roomId.ifBlank { null }, password)
                showCreateDialog = false
            }
        )
    }

    if (showJoinDialog) {
        JoinRoomDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { roomId, password ->
                vm.joinRoom(roomId, password)
                showJoinDialog = false
            }
        )
    }
}

@Composable
private fun RoomListItem(
    room: RoomInfo,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (room.isSystem) Icons.Default.Notifications else Icons.Default.Forum,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        text = "房间号: ${room.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (room.memberCount > 0) {
                    Text(
                        text = "${room.memberCount}人",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isActive) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
            }
        }
    }
}
