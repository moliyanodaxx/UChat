package com.example.uchat.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uchat.util.AvatarUtil
import com.example.uchat.viewmodel.ChatViewModel

@Composable
fun ProfileScreen(
    vm: ChatViewModel = viewModel(),
    isEditMode: Boolean = false,
    onSave: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val myProfile by vm.myProfile.collectAsState()
    val needProfile by vm.needProfile.collectAsState()

    var nickname by remember(myProfile.nickname) { mutableStateOf(myProfile.nickname) }
    var selectedAvatar by remember(myProfile.avatar) {
        mutableStateOf(myProfile.avatar.ifEmpty { AvatarUtil.AVATARS[0] })
    }
    var signature by remember(myProfile.signature) { mutableStateOf(myProfile.signature) }

    // 新用户设置完个人资料后，needProfile会变为false，此时触发导航
    LaunchedEffect(needProfile) {
        if (!needProfile && !isEditMode) {
            onSave()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isEditMode) "编辑资料" else "设置个人资料",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = selectedAvatar,
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { if (it.length <= 20) nickname = it },
            label = { Text("昵称") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            supportingText = { Text("${nickname.length}/20") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "选择头像",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(AvatarUtil.AVATARS) { avatar ->
                val isSelected = avatar == selectedAvatar
                Surface(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable { selectedAvatar = avatar },
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = avatar,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = signature,
            onValueChange = { if (it.length <= 100) signature = it },
            label = { Text("个性签名") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            supportingText = { Text("${signature.length}/100") }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (isEditMode) {
                    vm.updateProfile(nickname, selectedAvatar, signature)
                    onSave()
                } else {
                    vm.setProfile(nickname, selectedAvatar)
                    // 不立即调用onSave()，等待needProfile变为false时自动触发
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = nickname.isNotBlank()
        ) {
            Text("保存")
        }

        if (onDismiss != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("取消")
            }
        }
    }
}
