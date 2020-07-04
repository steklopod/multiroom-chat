package by.steklopod.chat.domain

data class Message(
    val type: MessageTypes,
    val username: String,
    val message: String
)
