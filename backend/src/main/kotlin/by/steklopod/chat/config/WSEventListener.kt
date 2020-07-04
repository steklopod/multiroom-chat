package by.steklopod.chat.config

import by.steklopod.chat.controller.ChatController
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionConnectedEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent


@Component
class WSEventListener(
    private val chatController: ChatController,
    private val messagingTemplate: SimpMessageSendingOperations
) {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    @EventListener
    fun handleWebSocketConnectListener(event: SessionConnectedEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = headerAccessor.sessionId
        logger.info("Установлено новое подключение к веб-сокету $sessionId")
    }

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val headerAccessor = StompHeaderAccessor.wrap(event.message)
        logger.info("Разъеденился ${headerAccessor.sessionId}")
        val username = headerAccessor.sessionAttributes["username"] as String?
        username?.let {
            chatController.handleUserDisconnection(it)
        }
    }

}
