package by.steklopod.chat.domain.dto

import by.steklopod.chat.domain.User

data class ChatRoomUserListDto(
    val roomKey: String? = null,
    val users: MutableSet<User> = mutableSetOf()
)
