package com.example.uchat.util

object AvatarUtil {
    val AVATARS = listOf(
        "😀","😎","🤩","😍","🥳","😇","🤓","😏","🥸","🤠",
        "👻","🐱","🐶","🦊","🐼","🐨","🦁","🐯","🐸","🐧",
        "🌟","🔥","💎","🎮","🎵","🌈","⚡","🍀","🎯","🚀"
    )

    fun getAvatarForAccount(accountId: String): String {
        if (accountId.isEmpty()) return "😀"
        val code = accountId.fold(0) { acc, c -> acc + c.code }
        return AVATARS[code % AVATARS.size]
    }

    fun getAvatarEmoji(name: String): String {
        if (name.isEmpty()) return "😀"
        val code = name.fold(0) { acc, c -> acc + c.code }
        return AVATARS[code % AVATARS.size]
    }
}
