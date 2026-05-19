package com.poker.websocket.manager;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器（支持断线重连）
 */
@Component
public class WebSocketSessionManager {

    // userId -> session
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    // sessionId -> userId
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();

    // roomCode -> { userId -> session }
    private final Map<String, Map<Long, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // userId -> roomCode（用于断线重连时快速定位）
    private final Map<Long, String> userRoomIndex = new ConcurrentHashMap<>();

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USERNAME = "username";

    public void addSession(Long userId, WebSocketSession session) {
        userSessions.put(userId, session);
        sessionUserMap.put(session.getId(), userId);
    }

    public void removeSession(String sessionId) {
        Long userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            userSessions.remove(userId);
            roomSessions.values().forEach(room -> room.remove(userId));
        }
    }

    public WebSocketSession getSession(Long userId) {
        return userSessions.get(userId);
    }

    public Long getUserId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    public void joinRoom(String roomCode, Long userId, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomCode, k -> new ConcurrentHashMap<>())
                .put(userId, session);
        userRoomIndex.put(userId, roomCode);
    }

    public void leaveRoom(String roomCode, Long userId) {
        Map<Long, WebSocketSession> room = roomSessions.get(roomCode);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) {
                roomSessions.remove(roomCode);
            }
        }
        userRoomIndex.remove(userId);
    }

    public Map<Long, WebSocketSession> getRoomSessions(String roomCode) {
        return roomSessions.getOrDefault(roomCode, new ConcurrentHashMap<>());
    }

    public boolean isInRoom(String roomCode, Long userId) {
        Map<Long, WebSocketSession> room = roomSessions.get(roomCode);
        return room != null && room.containsKey(userId);
    }

    public String getUserRoom(Long userId) {
        return userRoomIndex.get(userId);
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }

    public boolean isUserInRoom(WebSocketSession session, String roomCode) {
        if (session == null || roomCode == null) return false;
        Long userId = sessionUserMap.get(session.getId());
        if (userId == null) return false;
        Map<Long, WebSocketSession> room = roomSessions.get(roomCode);
        return room != null && room.containsKey(userId);
    }
}