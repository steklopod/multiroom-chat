package by.steklopod.multiroomchat.chat.controller;

import by.steklopod.multiroomchat.chat.dto.ChatRoomUserListDto;
import by.steklopod.multiroomchat.chat.dto.NewRoomDto;
import by.steklopod.multiroomchat.chat.dto.SimpleRoomDto;
import by.steklopod.multiroomchat.chat.dto.UserRoomKeyDto;
import by.steklopod.multiroomchat.chat.service.RoomService;
import by.steklopod.multiroomchat.message.Message;
import by.steklopod.multiroomchat.message.MessageTypes;
import by.steklopod.multiroomchat.user.User;
import by.steklopod.multiroomchat.chat.domain.Room;
import io.vavr.collection.HashSet;
import io.vavr.collection.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import static java.lang.String.format;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final RoomService roomService;
    private final SimpMessageSendingOperations messagingTemplate;

    public ChatController(RoomService roomService, SimpMessageSendingOperations messagingTemplate) {
        this.roomService = roomService;
        this.messagingTemplate = messagingTemplate;
    }

    @SubscribeMapping("/chat/roomList")
    public List<SimpleRoomDto> roomList() {
        return roomService.roomList();
    }

    @MessageMapping("/chat/addRoom")
    @SendTo("/chat/newRoom")
    public SimpleRoomDto addRoom(NewRoomDto newRoom) {
        return roomService.addRoom(newRoom.roomName);
    }

    @MessageMapping("/chat/{roomId}/join")
    public ChatRoomUserListDto userJoinRoom(UserRoomKeyDto userRoomKey, SimpMessageHeaderAccessor headerAccessor) {
//        with enabled spring security
//        final String securityUser = headerAccessor.getUser().getName();
        final String username = (String) headerAccessor.getSessionAttributes().put("username", userRoomKey.userName);
        final Message joinMessage = new Message(MessageTypes.JOIN, userRoomKey.userName, "");
        return roomService.addUserToRoom(userRoomKey)
                .map(userList -> {
                    messagingTemplate.convertAndSend(String.format("/chat/%s/userList", userList.roomKey), userList);
                    sendMessage(userRoomKey.roomKey, joinMessage);
                    return userList;
                })
                .getOrElseGet(appError -> {
                    log.error("invalid room id...");
                    return new ChatRoomUserListDto(userRoomKey.roomKey, HashSet.empty());
                });
    }

    @MessageMapping("/chat/{roomId}/leave")
    public ChatRoomUserListDto userLeaveRoom(UserRoomKeyDto userRoomKey, SimpMessageHeaderAccessor headerAccessor) {
        final Message leaveMessage = new Message(MessageTypes.LEAVE, userRoomKey.userName, "");
        return roomService.removeUserFromRoom(userRoomKey)
                .map(userList -> {
                    messagingTemplate.convertAndSend(String.format("/chat/%s/userList", userList.roomKey), userList);
                    sendMessage(userRoomKey.roomKey, leaveMessage);
                    return userList;
                })
                .getOrElseGet(appError -> {
                    log.error("invalid room id...");
                    return new ChatRoomUserListDto(userRoomKey.roomKey, HashSet.empty());
                });
    }

    @MessageMapping("chat/{roomId}/sendMessage")
    public Message sendMessage(@DestinationVariable String roomId, Message message) {
        messagingTemplate.convertAndSend(format("/chat/%s/messages", roomId), message);
        return message;
    }

    public void handleUserDisconnection(String userName) {
        final User user = new User(userName);
        final Message leaveMessage = new Message(MessageTypes.LEAVE, userName, "");
        List<Room> userRooms = roomService.disconnectUser(user);
        userRooms
                .map(room -> new ChatRoomUserListDto(room.key, room.users))
                .forEach(roomUserList -> {
                    messagingTemplate.convertAndSend(format("/chat/%s/userList", roomUserList.roomKey), roomUserList);
                    sendMessage(roomUserList.roomKey, leaveMessage);
                });
    }

}
