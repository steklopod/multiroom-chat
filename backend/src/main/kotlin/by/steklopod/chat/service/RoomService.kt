package by.steklopod.chat.service

import by.steklopod.chat.domain.Room
import by.steklopod.chat.domain.User
import by.steklopod.chat.domain.dto.ChatRoomUserListDto
import by.steklopod.chat.domain.dto.SimpleRoomDto
import by.steklopod.chat.domain.dto.UserRoomKeyDto
import org.springframework.stereotype.Service


@Service
class RoomService {
    private val roomList: MutableList<Room> = mutableListOf(defaultRoom())

    fun roomList(): List<SimpleRoomDto> = roomList.map { it.asSimpleRoomDto() }


    fun addRoom(roomName: String): SimpleRoomDto {
        val room = Room(roomName)
        addRoom(room)
        return room.asSimpleRoomDto()
    }

    fun usersInChatRoom(roomKey: String): ChatRoomUserListDto? = roomList
        .find { it.key == roomKey }
        ?.let { ChatRoomUserListDto(it.key, it.users) }


    fun addUserToRoom(userRoomKey: UserRoomKeyDto): ChatRoomUserListDto? {
        val user = User(userRoomKey.username)
        roomList
            .find { it.key == userRoomKey.roomKey }
            ?.let { oldRoom ->
                val newRoom = oldRoom.subscribe(user)
                updateRoom(oldRoom, newRoom)
                newRoom
            }
        return usersInChatRoom(userRoomKey.roomKey)
    }

    fun removeUserFromRoom(userRoomKey: UserRoomKeyDto): ChatRoomUserListDto? {
        val user = User(userRoomKey.username)
        roomList
            .find { room: Room -> room.key == userRoomKey.roomKey }
            ?.let { oldRoom ->
                val newRoom = oldRoom.unsubscribe(user)
                updateRoom(oldRoom, newRoom)
                newRoom
            }
        return usersInChatRoom(userRoomKey.roomKey)
    }

    fun disconnectUser(user: User): List<Room> = roomList
        .filter { it.users.contains(user) }
        .map { oldRoom: Room ->
            val newRoom = oldRoom.unsubscribe(user)
            updateRoom(oldRoom, newRoom)
            newRoom
        }


    private fun defaultRoom() = Room("Main room")
    private fun addRoom(room: Room): List<Room> = roomList.apply { add(room) }
    private fun updateRoom(oldRoom: Room, newRoom: Room): List<Room> = roomList.apply { remove(oldRoom); add(newRoom) }


}
