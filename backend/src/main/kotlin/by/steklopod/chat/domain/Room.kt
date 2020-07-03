package by.steklopod.chat.domain

import by.steklopod.chat.domain.dto.SimpleRoomDto
import java.util.*


data class Room(
    val name: String,
    val key: String = UUID.randomUUID().toString(),
    val users: MutableSet<User> = mutableSetOf()
) {
    fun subscribe(user: User): Room {
        users.add(user)
        return Room(name, key, users)
    }

    fun unsubscribe(user: User): Room {
        users.remove(user)
        return Room(name, key, users)
    }

    fun asSimpleRoomDto(): SimpleRoomDto {
        return SimpleRoomDto(name, key)
    }

}
