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
    fun roomList(): List<SimpleRoomDto> = roomService.roomList()


    @MessageMapping("/chat/addRoom")
    @SendTo("/chat/newRoom")
    fun addRoom(newRoom: NewRoomDto): SimpleRoomDto = roomService.addRoom(newRoom.roomName)


    @MessageMapping("/chat/{roomId}/join")
    fun userJoinRoom(userRoomKey: UserRoomKeyDto, headerAccessor: SimpMessageHeaderAccessor): ChatRoomUserListDto? {
//        with enabled spring security
//        final String securityUser = headerAccessor.getUser().getName();
        val username = headerAccessor.sessionAttributes.put("username", userRoomKey.username) as String?
        log.info("User {$username} joined to chat {$userRoomKey}")

        val joinMessage = Message(MessageTypes.JOIN, userRoomKey.username, "")
        return roomService.addUserToRoom(userRoomKey)
            .apply {
                messagingTemplate.convertAndSend("/chat/${this!!.roomKey}/userList", this)
                sendMessage(userRoomKey.roomKey, joinMessage)
            }
    }

    @MessageMapping("/chat/{roomId}/leave")
    fun userLeaveRoom(userRoomKey: UserRoomKeyDto, headerAccessor: SimpMessageHeaderAccessor?): ChatRoomUserListDto? {
        val leaveMessage = Message(MessageTypes.LEAVE, userRoomKey.username, "")
        log.info("User left chat {$userRoomKey}")
        return roomService.removeUserFromRoom(userRoomKey)
            .apply {
                messagingTemplate.convertAndSend("/chat/${this!!.roomKey}/userList", this)
                sendMessage(userRoomKey.roomKey, leaveMessage)
            }
    }

    @MessageMapping("chat/{roomId}/sendMessage")
    fun sendMessage(@DestinationVariable roomId: String?, message: Message): Message {
        messagingTemplate.convertAndSend("/chat/$roomId/messages", message)
        return message
    }

    fun handleUserDisconnection(username: String) {
        val user = User(username)
        val leaveMessage = Message(MessageTypes.LEAVE, username, "")
        val userRooms = roomService.disconnectUser(user)
        userRooms
            .map { ChatRoomUserListDto(it.key, it.users) }
            .forEach {
                messagingTemplate.convertAndSend("/chat/${it.roomKey}/userList", it)
                sendMessage(it.roomKey, leaveMessage)
            }
    }


}
