package com.poker.controller;

import com.poker.common.Result;
import com.poker.dto.RoomDTO;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import com.poker.service.RoomService;
import com.poker.util.JwtUtil;
import com.poker.websocket.dispatcher.MessageDispatcher;
import com.poker.websocket.message.MessageType;
import com.poker.websocket.message.WsMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
@Tag(name = "房间接口", description = "房间列表、创建、加入、离开")
public class RoomController {

    private final RoomService roomService;
    private final JwtUtil jwtUtil;
    private final MessageDispatcher messageDispatcher;

    @GetMapping("/all")
    @Operation(summary = "获取房间列表", description = "返回所有可加入的房间")
    public Result<List<Room>> getRoomList() {
        return Result.success(roomService.getRoomList());
    }

    @PostMapping("/create")
    @Operation(summary = "创建房间", description = "创建一个新的游戏房间")
    public Result<Room> createRoom(@Valid @RequestBody RoomDTO dto,
                                   @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        Room room = roomService.createRoom(dto, userId);
        return Result.success(room);
    }

    @PostMapping("/join/{roomCode}")
    @Operation(summary = "加入房间", description = "玩家加入指定房间")
    public Result<Room> joinRoom(@PathVariable String roomCode,
                                 @RequestParam Long buyInChips,
                                 @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        Room room = roomService.joinRoom(roomCode, userId, buyInChips);
        return Result.success(room);
    }

    @PostMapping("/leave/{roomCode}")
    @Operation(summary = "离开房间", description = "玩家离开指定房间")
    public Result<Void> leaveRoom(@PathVariable String roomCode,
                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        roomService.leaveRoom(roomCode, userId);
        return Result.success();
    }

    @PostMapping("/rebuy")
    @Operation(summary = "补充筹码", description = "玩家补充游戏筹码")
    public Result<Room> rebuy(@RequestParam String roomCode,
                              @RequestParam Long amount,
                              @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);

        Room room = roomService.rebuy(roomCode, userId, amount);

        // 广播玩家筹码更新（后端即真理：让所有客户端同步最新筹码）
        messageDispatcher.broadcastToRoom(roomCode, WsMessage.of(MessageType.ROOM_UPDATE, buildRoomPlayersPayload(room)));

        return Result.success(room);
    }

    private Map<String, Object> buildRoomPlayersPayload(Room room) {
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
                        "chips", p.getChips() != null ? p.getChips() : 0,
                        "isReady", p.getIsReady() != null ? p.getIsReady() : false,
                        "isOnline", p.getIsOnline() != null ? p.getIsOnline() : false
                )).toList()
        );
    }

    @GetMapping("/{roomCode}")
    @Operation(summary = "获取房间详情", description = "根据房间编码获取房间详细信息")
    public Result<Room> getRoom(@PathVariable String roomCode) {
        Room room = roomService.getRoom(roomCode);
        if (room == null) {
            return Result.error(404, "房间不存在");
        }
        return Result.success(room);
    }
}