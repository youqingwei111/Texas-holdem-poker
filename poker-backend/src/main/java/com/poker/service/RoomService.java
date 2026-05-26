package com.poker.service;

import com.poker.common.ErrorCode;
import com.poker.common.Utils;
import com.poker.dto.RoomDTO;
import com.poker.entity.User;
import com.poker.exception.BusinessException;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import com.poker.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RedisUtil redisUtil;
    private final UserService userService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ROOM_PREFIX = "poker:room:";
    private static final String ROOM_LIST_KEY = "poker:rooms";
    private static final long ROOM_EXPIRE_HOURS = 24;

    public Room createRoom(RoomDTO dto, Long ownerId) {
        User owner = userService.getById(ownerId);
        Long buyInAmount = dto.getMinBuyIn().longValue();

        // 房主买入时也必须扣款
        if (owner.getChips() < buyInAmount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CHIPS, "账户筹码不足，无法创建房间");
        }
        userService.deductChips(ownerId, buyInAmount);

        String roomCode = generateUniqueRoomCode();

        Room room = new Room();
        room.setCode(roomCode);
        room.setName(dto.getName());
        room.setOwnerId(ownerId);
        room.setOwnerName(owner.getNickname());
        room.setSmallBlind(dto.getSmallBlind());
        room.setBigBlind(dto.getBigBlind());
        room.setMinBuyIn(dto.getMinBuyIn());
        room.setMaxBuyIn(dto.getMaxBuyIn());
        room.setMaxPlayers(dto.getMaxPlayers());
        room.setCreatedAt(System.currentTimeMillis());

        Player ownerPlayer = new Player(ownerId, owner.getUsername(), owner.getNickname(), buyInAmount);
        ownerPlayer.setPosition(0);
        room.addPlayer(ownerPlayer);

        room.setFull(room.isFull());

        saveRoom(room);

        return room;
    }

    public Room joinRoom(String roomCode, Long userId, Long buyInChips) {
        Room room = getRoom(roomCode);
        if (room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        if (room.isFull()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }

        // 幂等检查：如果是断线重连（玩家已在房间中），不再扣款
        if (room.hasPlayer(userId)) {
            // 玩家已在房间里，不重复扣款，直接返回
            log.info("[RoomService] 玩家 {} 已在房间 {} 中，视为重连，不扣款", userId, roomCode);
            return room;
        }

        if (room.getIsPlaying()) {
            throw new BusinessException(ErrorCode.GAME_ALREADY_STARTED);
        }

        User user = userService.getById(userId);
        if (user.getChips() < buyInChips) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_CHIPS);
        }

        if (buyInChips < room.getMinBuyIn() || buyInChips > room.getMaxBuyIn()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "带入筹码超出范围");
        }

        // 扣款买入（原子操作，防止并发）
        userService.deductChips(userId, buyInChips);

        Player player = new Player(userId, user.getUsername(), user.getNickname(), buyInChips);
        player.setPosition(room.getPlayerCount());
        room.addPlayer(player);

        room.setFull(room.isFull());
        saveRoom(room);
        return room;
    }

    public void leaveRoom(String roomCode, Long userId) {
        Room room = getRoom(roomCode);
        if (room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        if (!room.hasPlayer(userId)) {
            throw new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM);
        }

        // 离桌时退还剩余筹码
        Player player = room.getPlayer(userId);
        if (player != null && player.getChips() > 0) {
            userService.addChips(userId, player.getChips());
            log.info("[RoomService] 玩家 {} 离桌，退还 {} 筹码", userId, player.getChips());
            // 标记已退款，防止 afterConnectionClosed 重复退款
            player.setHasRefunded(true);
        }

        room.removePlayer(userId);

        room.setFull(room.isFull());

        if (room.getPlayerCount() == 0) {
            deleteRoom(roomCode);
        } else {
            saveRoom(room);
        }
    }

    public Room getRoom(String roomCode) {
        return (Room) redisUtil.get(ROOM_PREFIX + roomCode);
    }

    public void saveRoom(Room room) {
        room.setFull(room.isFull());
        redisUtil.set(ROOM_PREFIX + room.getCode(), room, ROOM_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    public void deleteRoom(String roomCode) {
        redisUtil.delete(ROOM_PREFIX + roomCode);
    }

    /**
     * 补充筹码（Rebuy）
     * 只能在游戏未开始（WAITING）时使用
     */
    public Room rebuy(String roomCode, Long userId, Long amount) {
        Room room = getRoom(roomCode);
        if (room == null) {
            throw new BusinessException(ErrorCode.ROOM_NOT_FOUND);
        }

        if (!room.hasPlayer(userId)) {
            throw new BusinessException(ErrorCode.PLAYER_NOT_IN_ROOM);
        }

        // 仅允许在游戏未开始时补充筹码
        if (room.getIsPlaying()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "游戏进行中无法补充筹码");
        }

        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "补充筹码金额必须大于0");
        }

        // 扣除用户真实余额
        userService.deductChips(userId, amount);

        // 给桌上的玩家增加筹码
        Player player = room.getPlayer(userId);
        if (player != null) {
            player.setChips(player.getChips() + amount);
            log.info("[RoomService] 玩家 {} 补充 {} 筹码，桌上剩余 {}",
                    userId, amount, player.getChips());
        }

        saveRoom(room);
        return room;
    }

    public List<Room> getRoomList() {
        List<Room> rooms = new ArrayList<>();
        Set<String> keys = redisTemplate.keys(ROOM_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return rooms;
        }
        for (String key : keys) {
            Object val = redisUtil.get(key);
            if (val instanceof Room) {
                Room room = (Room) val;
                room.setFull(room.isFull());
                rooms.add(room);
            }
        }
        rooms.sort((a, b) -> Long.compare(
                b.getCreatedAt() != null ? b.getCreatedAt() : 0L,
                a.getCreatedAt() != null ? a.getCreatedAt() : 0L));
        return rooms;
    }

    private String generateUniqueRoomCode() {
        String code;
        do {
            code = Utils.generateRoomCode();
        } while (getRoom(code) != null);
        return code;
    }
}