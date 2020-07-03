package by.steklopod.chat.controller

import by.steklopod.chat.domain.Message
import by.steklopod.chat.domain.MessageTypes
import by.steklopod.chat.domain.Room
import by.steklopod.chat.domain.User
import by.steklopod.chat.domain.dto.ChatRoomUserListDto
import by.steklopod.chat.domain.dto.NewRoomDto
import by.steklopod.chat.domain.dto.SimpleRoomDto
import by.steklopod.chat.domain.dto.UserRoomKeyDto
import by.steklopod.chat.service.RoomService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.annotation.SubscribeMapping
import org.springframework.stereotype.Controller
import java.util.function.Consumer


@Controller
class ChatController(
    private val roomService: RoomService,
    private val messagingTemplate: SimpMessageSendingOperations
) {
    companion object {
        private val log = LoggerFactory.getLogger(ChatController::class.java)
    }

    @SubscribeMapping("/chat/roomList")
    fun roomList(): List<SimpleRoomDto> {
        return roomService.roomList()
    }

    @MessageMapping("/chat/addRoom")
    @SendTo("/chat/newRoom")
    fun addRoom(newRoom: NewRoomDto): SimpleRoomDto {
        return roomService.addRoom(newRoom.roomName)
    }

    @MessageMapping("/chat/{roomId}/join")
    fun userJoinRoom(userRoomKey: UserRoomKeyDto, headerAccessor: SimpMessageHeaderAccessor): ChatRoomUserListDto? {
//        with enabled spring security
//        final String securityUser = headerAccessor.getUser().getName();
        val username = headerAccessor.sessionAttributes.put("username", userRoomKey.userName) as String?
        val joinMessage = Message(MessageTypes.JOIN, userRoomKey.userName, "")
        return roomService.addUserToRoom(userRoomKey)
            .let {
                messagingTemplate.convertAndSend(String.format("/chat/%s/userList", it?.roomKey), it)
                sendMessage(userRoomKey.roomKey, joinMessage)
                it
            }
    }

    @MessageMapping("/chat/{roomId}/leave")
    fun userLeaveRoom(userRoomKey: UserRoomKeyDto, headerAccessor: SimpMessageHeaderAccessor?): ChatRoomUserListDto? {
        val leaveMessage = Message(MessageTypes.LEAVE, userRoomKey.userName, "")
        return roomService.removeUserFromRoom(userRoomKey)
            .let {
                messagingTemplate.convertAndSend(String.format("/chat/%s/userList", it?.roomKey), it)
                sendMessage(userRoomKey.roomKey, leaveMessage)
                it
            }
    }

    @MessageMapping("chat/{roomId}/sendMessage")
    fun sendMessage(@DestinationVariable roomId: String?, message: Message): Message {
        messagingTemplate.convertAndSend(String.format("/chat/%s/messages", roomId), message)
        return message
    }

    fun handleUserDisconnection(userName: String) {
        val user = User(userName)
        val leaveMessage = Message(MessageTypes.LEAVE, userName, "")
        val userRooms = roomService.disconnectUser(user)
        userRooms
            .map { room: Room -> ChatRoomUserListDto(room.key, room.users) }
            .forEach(Consumer { roomUserList: ChatRoomUserListDto ->
                messagingTemplate.convertAndSend(
                    String.format("/chat/%s/userList", roomUserList.roomKey), roomUserList
                )
                sendMessage(roomUserList.roomKey, leaveMessage)
            })
    }


}
