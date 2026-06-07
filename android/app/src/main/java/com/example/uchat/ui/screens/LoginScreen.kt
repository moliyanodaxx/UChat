package com.example.uchat.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.uchat.viewmodel.ChatViewModel

private const val SERVER_URL = "ws://43.143.220.74:10086"

@Composable
fun LoginScreen(
    vm: ChatViewModel = viewModel(),
    onLoginSuccess: () -> Unit,
    onNeedProfile: () -> Unit
) {
    val isRegistering by vm.isRegistering.collectAsState()
    val authError by vm.authError.collectAsState()
    val connecting by vm.connecting.collectAsState()
    val isLoggedIn by vm.isLoggedIn.collectAsState()
    val needProfile by vm.needProfile.collectAsState()

    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(isLoggedIn, needProfile) {
        if (isLoggedIn) {
            if (needProfile) onNeedProfile() else onLoginSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "UChat",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "网络聊天室",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = account,
            onValueChange = { if (it.length <= 10) account = it },
            label = { Text("账号（10位字母数字）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            isError = authError != null
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { if (it.length <= 10) password = it },
            label = { Text("密码（8-10位字母数字）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            isError = authError != null
        )

        if (authError != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = authError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                vm.clearAuthError()
                vm.connect(account, password, SERVER_URL, register = isRegistering)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !connecting && account.isNotEmpty() && password.isNotEmpty()
        ) {
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isRegistering) "注册" else "登录")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            vm.setRegistering(!isRegistering)
            vm.clearAuthError()
        }) {
            Text(if (isRegistering) "已有账号？去登录" else "没有账号？去注册")
        }
    }
}
