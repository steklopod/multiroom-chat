package by.steklopod.chat.domain

data class Message(
    val type: MessageTypes,
    val userName: String,
    val message: String
)
