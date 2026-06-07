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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uchat.data.model.Friend
import com.example.uchat.data.model.FriendRequest
import com.example.uchat.data.model.SearchResult
import com.example.uchat.data.model.UserData
import com.example.uchat.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    vm: ChatViewModel = viewModel(),
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val friends by vm.friends.collectAsState()
    val friendRequests by vm.friendRequests.collectAsState()
    val searchResults by vm.searchResults.collectAsState()
    val viewedUser by vm.viewedUser.collectAsState()
    val viewedUserIsSelf by vm.viewedUserIsSelf.collectAsState()

    var showUserProfileDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.getFriends()
        vm.getFriendRequests()
        vm.toastMessage.collectLatest { msg ->
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
                title = { Text("好友") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; vm.getFriends() },
                    text = { Text("好友列表") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; vm.getFriendRequests() },
                    text = {
                        BadgedBox(
                            badge = {
                                if (friendRequests.isNotEmpty()) {
                                    Badge { Text("${friendRequests.size}") }
                                }
                            }
                        ) { Text("好友请求") }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; vm.clearSearchResults() },
                    text = { Text("搜索用户") }
                )
            }

            when (selectedTab) {
                0 -> FriendsListTab(
                    friends = friends,
                    onViewProfile = { vm.getUserInfo(it) }
                )
                1 -> RequestsTab(
                    requests = friendRequests,
                    onAccept = { vm.handleFriendRequest(it, true) },
                    onReject = { vm.handleFriendRequest(it, false) }
                )
                2 -> SearchTab(
                    results = searchResults,
                    onSearch = { vm.searchUser(it) },
                    onViewProfile = { vm.getUserInfo(it) },
                    onSendRequest = { vm.sendFriendRequest(it) }
                )
            }
        }
    }

    if (showUserProfileDialog && viewedUser != null) {
        FriendProfileDialog(
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
private fun FriendsListTab(
    friends: List<Friend>,
    onViewProfile: (String) -> Unit
) {
    if (friends.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.People,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("还没有好友", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("去搜索用户添加好友吧", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(friends, key = { it.accountId }) { friend ->
                FriendItem(friend = friend, onClick = { onViewProfile(friend.accountId) })
            }
        }
    }
}

@Composable
private fun FriendItem(friend: Friend, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(friend.avatar.ifEmpty { "😀" }, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(friend.nickname, style = MaterialTheme.typography.bodyLarge)
            if (friend.signature.isNotEmpty()) {
                Text(
                    friend.signature,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (friend.online) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                if (friend.online) "在线" else "离线",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (friend.online) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
}

@Composable
private fun RequestsTab(
    requests: List<FriendRequest>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (requests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无好友请求", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
            items(requests, key = { it.from }) { request ->
                RequestItem(
                    request = request,
                    onAccept = { onAccept(request.from) },
                    onReject = { onReject(request.from) }
                )
            }
        }
    }
}

@Composable
private fun RequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(request.fromAvatar.ifEmpty { "😀" }, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(request.fromNickname, style = MaterialTheme.typography.bodyLarge)
            Text(request.from, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalButton(onClick = onAccept) { Text("接受") }
            OutlinedButton(onClick = onReject) { Text("拒绝") }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
}

@Composable
private fun SearchTab(
    results: List<SearchResult>,
    onSearch: (String) -> Unit,
    onViewProfile: (String) -> Unit,
    onSendRequest: (String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入账号或昵称") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }
            )
            Button(
                onClick = { if (searchText.isNotBlank()) onSearch(searchText) },
                enabled = searchText.isNotBlank()
            ) { Text("搜索") }
        }

        if (results.isEmpty() && searchText.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到用户", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(results, key = { it.accountId }) { result ->
                    SearchResultItem(
                        result = result,
                        onViewProfile = { onViewProfile(result.accountId) },
                        onSendRequest = { onSendRequest(result.accountId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    result: SearchResult,
    onViewProfile: () -> Unit,
    onSendRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewProfile)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(result.avatar.ifEmpty { "😀" }, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(result.nickname, style = MaterialTheme.typography.bodyLarge)
            Text(result.accountId, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FilledTonalButton(onClick = onSendRequest) {
            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("加好友")
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 64.dp))
}

@Composable
private fun FriendProfileDialog(
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
            if (user.signature.isNotEmpty()) {
                Text(
                    "\"${user.signature}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
