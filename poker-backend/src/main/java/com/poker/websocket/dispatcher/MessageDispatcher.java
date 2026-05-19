package com.poker.websocket.dispatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poker.websocket.manager.WebSocketSessionManager;
import com.poker.websocket.message.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

/**
 * 消息分发器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageDispatcher {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public void sendToUser(Long userId, WsMessage<?> message) {
        try {
            WebSocketSession session = sessionManager.getSession(userId);
            if (session != null && session.isOpen()) {
                String payload = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(payload));
            }
        } catch (Exception e) {
            log.error("发送消息给用户失败: {}", userId, e);
        }
    }

    public void broadcastToRoom(String roomCode, WsMessage<?> message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            Map<Long, WebSocketSession> sessions = sessionManager.getRoomSessions(roomCode);

            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(payload));
                    } catch (Exception e) {
                        log.error("广播消息失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("广播消息序列化失败", e);
        }
    }

    public void broadcastToRoomExcept(String roomCode, Long excludeUserId, WsMessage<?> message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            Map<Long, WebSocketSession> sessions = sessionManager.getRoomSessions(roomCode);

            for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
                if (!entry.getKey().equals(excludeUserId) && entry.getValue().isOpen()) {
                    try {
                        entry.getValue().sendMessage(new TextMessage(payload));
                    } catch (Exception e) {
                        log.error("广播消息失败", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("广播消息序列化失败", e);
        }
    }
}