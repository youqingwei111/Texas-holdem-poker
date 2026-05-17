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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

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

        Player ownerPlayer = new Player(ownerId, owner.getUsername(), owner.getNickname(), room.getMinBuyIn().longValue());
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

        if (room.hasPlayer(userId)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "已在房间中");
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