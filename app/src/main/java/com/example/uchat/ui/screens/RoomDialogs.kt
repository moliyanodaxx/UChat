package com.example.uchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var roomName by remember { mutableStateOf("") }
    var roomId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建房间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("房间名称") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roomId,
                    onValueChange = { if (it.length <= 10) roomId = it },
                    label = { Text("房间号（10位，留空自动生成）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { if (it.length <= 6) password = it },
                    label = { Text("密码（6位）") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(roomName, roomId, password) }) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRoomDialog(
    onDismiss: () -> Unit,
    onJoin: (String, String) -> Unit
) {
    var roomId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("加入房间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = roomId,
                    onValueChange = { if (it.length <= 10) roomId = it },
                    label = { Text("房间号") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { if (it.length <= 6) password = it },
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onJoin(roomId, password) }) {
                Text("加入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
