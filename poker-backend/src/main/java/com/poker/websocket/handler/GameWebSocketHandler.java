package com.poker.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poker.game.engine.GameEngine;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import com.poker.game.enums.PlayerAction;
import com.poker.service.RoomService;
import com.poker.util.JwtUtil;
import com.poker.websocket.dispatcher.MessageDispatcher;
import com.poker.websocket.manager.WebSocketSessionManager;
import com.poker.websocket.message.MessageType;
import com.poker.websocket.message.PlayerActionMessage;
import com.poker.websocket.message.WsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;

/**
 * 游戏WebSocket处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;
    private final MessageDispatcher messageDispatcher;
    private final RoomService roomService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final GameEngine gameEngine;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        log.info("[WS] 连接建立: {}", sessionId);

        URI uri = session.getUri();
        if (uri == null) {
            log.warn("[WS] URI为空，拒绝连接");
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String query = uri.getQuery();
        String token = extractParam(query, "token");

        if (token == null || token.isBlank()) {
            log.warn("[WS] 缺少token参数，拒绝连接");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("missing token"));
            return;
        }

        if (!jwtUtil.validateToken(token)) {
            log.warn("[WS] Token无效: {}", token);
            session.close(CloseStatus.POLICY_VIOLATION.withReason("invalid token"));
            return;
        }

        Long userId = jwtUtil.getUserId(token);
        String username = jwtUtil.getUsername(token);

        if (userId == null) {
            log.warn("[WS] Token解析userId失败");
            session.close(CloseStatus.POLICY_VIOLATION.withReason("invalid token payload"));
            return;
        }

        session.getAttributes().put(WebSocketSessionManager.ATTR_USER_ID, userId);
        session.getAttributes().put(WebSocketSessionManager.ATTR_USERNAME, username);
        sessionManager.addSession(userId, session);

        log.info("[WS] 鉴权成功 | userId={} | username={} | session={}", userId, username, sessionId);

        WsMessage<?> msg = WsMessage.of(MessageType.CONNECT, Map.of("userId", userId, "username", username));
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("[WS] 收到消息 [{}]: {}", session.getId(), payload);

        Long userId = getUserId(session);
        if (userId == null) {
            log.warn("[WS] 消息来源未鉴权: {}", session.getId());
            sendError(session, "请先建立连接");
            return;
        }

        if ("ping".equalsIgnoreCase(payload.trim())) {
            handlePing(session);
            return;
        }

        try {
            WsMessage<?> wsMessage = objectMapper.readValue(payload, WsMessage.class);
            handleMessage(session, wsMessage, userId);
        } catch (Exception e) {
            log.error("[WS] 消息处理失败", e);
            sendError(session, "消息格式错误");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("[WS] 连接关闭 [{}] status={}", session.getId(), status);

        Long userId = getUserId(session);
        if (userId == null) return;

        String roomCode = sessionManager.getUserRoom(userId);
        if (roomCode != null) {
            Room room = roomService.getRoom(roomCode);
            if (room != null) {
                Player player = room.getPlayer(userId);
                if (player != null) {
                    player.setIsOnline(false);
                    roomService.saveRoom(room);
                }
                messageDispatcher.broadcastToRoomExcept(roomCode, userId,
                        WsMessage.of(MessageType.PLAYER_LEFT, Map.of("userId", userId, "reason", "disconnect")));
            }
        }

        sessionManager.removeSession(session.getId());
    }

    private void handleMessage(WebSocketSession session, WsMessage<?> wsMessage, Long userId) {
        MessageType type = wsMessage.getType();
        String username = getUsername(session);

        log.debug("[WS] 路由消息 type={} from userId={}", type, userId);

        switch (type) {
            case JOIN_ROOM -> handleJoinRoom(session, wsMessage, userId, username);
            case LEAVE_ROOM -> handleLeaveRoom(wsMessage, userId);
            case PLAYER_READY -> handlePlayerReady(wsMessage, userId);
            case PLAYER_ACTION -> handlePlayerAction(session, wsMessage, userId);
            case CHAT -> handleChat(wsMessage, userId);
            case START_GAME -> handleStartGame(session, wsMessage, userId);
            default -> sendError(session, "未知消息类型: " + type);
        }
    }

    private void handlePing(WebSocketSession session) {
        try {
            WsMessage<?> pong = WsMessage.of(MessageType.PONG, Map.of());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
        } catch (Exception e) {
            log.error("[WS] 发送pong失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleJoinRoom(WebSocketSession session, WsMessage<?> wsMessage, Long userId, String username) {
        try {
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String roomCode = (String) data.get("roomCode");

            if (roomCode == null || roomCode.isBlank()) {
                sendError(session, "roomCode不能为空");
                return;
            }

            Room room = roomService.getRoom(roomCode);
            if (room == null) {
                sendError(session, "房间不存在");
                return;
            }

            if (!room.hasPlayer(userId)) {
                sendError(session, "请先通过HTTP接口加入房间");
                return;
            }

            boolean isReconnect = !sessionManager.isOnline(userId) && roomCode.equals(sessionManager.getUserRoom(userId));

            sessionManager.joinRoom(roomCode, userId, session);

            Player player = room.getPlayer(userId);
            if (player != null) {
                player.setIsOnline(true);
                roomService.saveRoom(room);
            }

            if (isReconnect) {
                log.info("[WS] 断线重连 | userId={} | room={}", userId, roomCode);
                messageDispatcher.sendToUser(userId, WsMessage.of(MessageType.CONNECT, Map.of("message", "重连成功", "userId", userId, "reconnect", true)));
                messageDispatcher.sendToUser(userId, WsMessage.of(MessageType.ROOM_UPDATE, toRoomDTO(room)));
                messageDispatcher.broadcastToRoomExcept(roomCode, userId, WsMessage.of(MessageType.PLAYER_JOINED, Map.of("userId", userId, "username", username, "reconnect", true)));
            } else {
                log.info("[WS] 玩家加入房间 | userId={} | room={}", userId, roomCode);
                messageDispatcher.sendToUser(userId, WsMessage.of(MessageType.CONNECT, Map.of("message", "连接成功", "userId", userId)));
                messageDispatcher.sendToUser(userId, WsMessage.of(MessageType.ROOM_UPDATE, toRoomDTO(room)));
                messageDispatcher.broadcastToRoomExcept(roomCode, userId, WsMessage.of(MessageType.PLAYER_JOINED, Map.of("userId", userId, "username", username)));
            }
        } catch (Exception e) {
            log.error("[WS] 加入房间失败", e);
            sendError(session, "加入房间失败: " + e.getMessage());
        }
    }

    private void handleLeaveRoom(WsMessage<?> wsMessage, Long userId) {
        String roomCode = sessionManager.getUserRoom(userId);
        if (roomCode != null) {
            sessionManager.leaveRoom(roomCode, userId);
            messageDispatcher.broadcastToRoom(roomCode, WsMessage.of(MessageType.PLAYER_LEFT, Map.of("userId", userId, "reason", "leave")));
        }
    }

    private void handlePlayerReady(WsMessage<?> wsMessage, Long userId) {
        String roomCode = sessionManager.getUserRoom(userId);
        if (roomCode == null) {
            log.warn("[WS] 玩家{}不在房间中，无法准备", userId);
            return;
        }
        try {
            Room room = roomService.getRoom(roomCode);
            if (room != null) {
                Player player = room.getPlayer(userId);
                if (player != null) {
                    player.setIsReady(true);
                    roomService.saveRoom(room);
                    messageDispatcher.broadcastToRoom(roomCode, WsMessage.of(MessageType.PLAYER_READY, Map.of("userId", userId)));
                }
            }
        } catch (Exception e) {
            log.warn("[WS] 准备失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private void handlePlayerAction(WebSocketSession session, WsMessage<?> wsMessage, Long userId) {
        String roomCode = sessionManager.getUserRoom(userId);
        if (roomCode == null) {
            log.warn("[WS] 玩家{}不在房间中，无法操作", userId);
            sendError(session, "不在房间中");
            return;
        }

        try {
            PlayerActionMessage actionMsg = objectMapper.convertValue(wsMessage.getData(), PlayerActionMessage.class);

            if (actionMsg.getAction() == null || actionMsg.getAction().isBlank()) {
                sendError(session, "动作类型不能为空");
                return;
            }

            PlayerAction action = PlayerAction.valueOf(actionMsg.getAction().toUpperCase());
            Long raiseAmount = actionMsg.getAmount();
            log.info("[WS] 处理玩家动作 userId={} action={} amount={}", userId, action, raiseAmount);

            Room room = roomService.getRoom(roomCode);
            if (room == null) {
                sendError(session, "房间不存在");
                return;
            }

            // 调用 GameEngine 处理动作并推进状态机
            GameEngine.ActionResult result = gameEngine.processAction(room, userId, action, raiseAmount);

            if (!result.success()) {
                log.warn("[WS] 动作处理失败 userId={}: {}", userId, result.message());
                sendError(session, result.message());
                return;
            }

            log.info("[WS] 动作处理完成 userId={} msg={} roundOver={} gameOver={} nextPlayer={}",
                    userId, result.message(), result.roundOver(), result.gameOver(), result.nextPlayerIndex());

        } catch (IllegalArgumentException e) {
            log.warn("[WS] 未知动作类型: {}", wsMessage.getData());
            sendError(session, "未知动作类型: " + e.getMessage());
        } catch (Exception e) {
            log.error("[WS] 处理玩家动作失败 userId={}: {}", userId, e.getMessage());
            sendError(session, "动作处理失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleChat(WsMessage<?> wsMessage, Long userId) {
        String roomCode = sessionManager.getUserRoom(userId);
        if (roomCode == null) return;

        try {
            Map<String, Object> data = (Map<String, Object>) wsMessage.getData();
            String content = (String) data.get("content");

            Room room = roomService.getRoom(roomCode);
            Player player = room != null ? room.getPlayer(userId) : null;

            messageDispatcher.broadcastToRoom(roomCode, WsMessage.of(MessageType.CHAT, Map.of(
                    "userId", userId,
                    "username", player != null ? player.getUsername() : "unknown",
                    "content", content
            )));
        } catch (Exception e) {
            log.warn("[WS] 聊天消息处理失败: {}", e.getMessage());
        }
    }

    private void handleStartGame(WebSocketSession session, WsMessage<?> wsMessage, Long userId) {
        String roomCode = sessionManager.getUserRoom(userId);

        if (roomCode == null || !sessionManager.isUserInRoom(session, roomCode)) {
            sendError(session, "未在房间中，请先加入房间");
            return;
        }

        try {
            Room room = roomService.getRoom(roomCode);
            if (room == null) {
                sendError(session, "房间不存在");
                return;
            }

            if (!room.getOwnerId().equals(userId)) {
                sendError(session, "只有房主可以开始游戏");
                return;
            }

            if (room.getPlayerCount() < 2) {
                sendError(session, "至少需要2名玩家才能开始游戏");
                return;
            }

            if (room.getIsPlaying()) {
                sendError(session, "游戏已经开始");
                return;
            }

            log.info("========== 游戏开始 ==========");
            log.info("userId={}, room={}, 玩家数={}", userId, roomCode, room.getPlayerCount());
            for (Player p : room.getPlayers()) {
                log.info("  玩家: userId={}, position={}, username={}", p.getUserId(), p.getPosition(), p.getUsername());
            }

            // 调用 GameEngine 初始化游戏
            gameEngine.startGame(room);

            // 持久化房间状态（包含 GameState）
            roomService.saveRoom(room);

            // 获取当前行动玩家
            Integer currentTurnIdx = room.getGameState().getCurrentTurnIndex();
            log.info("游戏状态初始化完成: dealerIndex={}, smallBlindIdx={}, bigBlindIdx={}, currentTurnIndex={}",
                    room.getGameState().getDealerIndex(),
                    room.getGameState().getSmallBlindIndex(),
                    room.getGameState().getBigBlindIndex(),
                    currentTurnIdx);

            // 广播 GAME_START 包含完整游戏状态
            Player dealerPlayer = room.getPlayers().get(room.getGameState().getDealerIndex());
            messageDispatcher.broadcastToRoom(roomCode, WsMessage.of(MessageType.GAME_START, Map.of(
                    "roomCode", roomCode,
                    "dealerIndex", room.getGameState().getDealerIndex(),
                    "smallBlind", room.getSmallBlind(),
                    "bigBlind", room.getBigBlind(),
                    "pot", room.getGameState().getPot(),
                    "currentBet", room.getGameState().getCurrentBet(),
                    "phase", room.getGameState().getPhase().name(),
                    "currentTurnIndex", currentTurnIdx,
                    "dealerName", dealerPlayer != null ? dealerPlayer.getUsername() : "",
                    "players", room.getPlayers().stream().map(p -> Map.of(
                            "userId", p.getUserId(),
                            "username", p.getUsername() != null ? p.getUsername() : "",
                            "nickname", p.getNickname() != null ? p.getNickname() : "",
                            "chips", p.getChips() != null ? p.getChips() : 0,
                            "position", p.getPosition() != null ? p.getPosition() : 0,
                            "currentBet", p.getCurrentBet() != null ? p.getCurrentBet() : 0,
                            "isFold", p.getIsFold() != null ? p.getIsFold() : false,
                            "isAllIn", p.getIsAllIn() != null ? p.getIsAllIn() : false,
                            "isActive", p.getIsActive() != null ? p.getIsActive() : true,
                            "isOnline", p.getIsOnline() != null ? p.getIsOnline() : true
                    )).toList()
            )));

            // 单独向当前行动玩家发送 YOUR_TURN
            if (currentTurnIdx != null && currentTurnIdx < room.getPlayers().size()) {
                Player currentPlayer = room.getPlayers().get(currentTurnIdx);
                log.info("向玩家 {} (userId={}) 发送 YOUR_TURN", currentPlayer.getUsername(), currentPlayer.getUserId());
                messageDispatcher.sendToUser(currentPlayer.getUserId(), WsMessage.of(MessageType.YOUR_TURN, Map.of(
                        "userId", currentPlayer.getUserId(),
                        "currentTurnIndex", currentTurnIdx,
                        "availableActions", java.util.List.of("FOLD", "CHECK", "CALL", "RAISE", "ALL_IN"),
                        "callAmount", room.getGameState().getCurrentBet() - currentPlayer.getCurrentBet(),
                        "minRaise", room.getBigBlind(),
                        "phase", room.getGameState().getPhase().name()
                )));
            }

            log.info("========== 游戏开始流程完成 ==========");

        } catch (Exception e) {
            log.error("[WS] 开始游戏失败", e);
            sendError(session, "开始游戏失败: " + e.getMessage());
        }
    }

    private Long getUserId(WebSocketSession session) {
        Object val = session.getAttributes().get(WebSocketSessionManager.ATTR_USER_ID);
        return val instanceof Long ? (Long) val : null;
    }

    private String getUsername(WebSocketSession session) {
        Object val = session.getAttributes().get(WebSocketSessionManager.ATTR_USERNAME);
        return val instanceof String ? (String) val : null;
    }

    private String extractParam(String query, String key) {
        if (query == null || query.isBlank()) return null;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1];
            }
        }
        return null;
    }

    private void sendError(WebSocketSession session, String message) {
        try {
            WsMessage<?> error = WsMessage.of(MessageType.ERROR, Map.of("message", message));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            log.error("[WS] 发送错误消息失败", e);
        }
    }

    private Map<String, Object> toRoomDTO(Room room) {
        return Map.of(
                "code", room.getCode() != null ? room.getCode() : "",
                "name", room.getName() != null ? room.getName() : "",
                "playerCount", room.getPlayerCount(),
                "maxPlayers", room.getMaxPlayers() != null ? room.getMaxPlayers() : 0,
                "isPlaying", room.getIsPlaying() != null ? room.getIsPlaying() : false,
                "players", room.getPlayers().stream().map(p -> Map.of(
                        "userId", p.getUserId() != null ? p.getUserId() : 0,
                        "username", p.getUsername() != null ? p.getUsername() : "",
                        "nickname", p.getNickname() != null ? p.getNickname() : "",
                        "isReady", p.getIsReady() != null ? p.getIsReady() : false,
                        "isOnline", p.getIsOnline() != null ? p.getIsOnline() : false
                )).toList()
        );
    }
}