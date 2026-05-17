package com.poker.controller;

import com.poker.common.Result;
import com.poker.dto.RoomDTO;
import com.poker.game.model.Room;
import com.poker.service.RoomService;
import com.poker.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final JwtUtil jwtUtil;

    @GetMapping("/all")
    public Result<List<Room>> getRoomList() {
        return Result.success(roomService.getRoomList());
    }

    @PostMapping("/create")
    public Result<Room> createRoom(@Valid @RequestBody RoomDTO dto,
                                   @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        Room room = roomService.createRoom(dto, userId);
        return Result.success(room);
    }

    @PostMapping("/join/{roomCode}")
    public Result<Room> joinRoom(@PathVariable String roomCode,
                                 @RequestParam Long buyInChips,
                                 @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        Room room = roomService.joinRoom(roomCode, userId, buyInChips);
        return Result.success(room);
    }

    @PostMapping("/leave/{roomCode}")
    public Result<Void> leaveRoom(@PathVariable String roomCode,
                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        roomService.leaveRoom(roomCode, userId);
        return Result.success();
    }

    @GetMapping("/{roomCode}")
    public Result<Room> getRoom(@PathVariable String roomCode) {
        Room room = roomService.getRoom(roomCode);
        if (room == null) {
            return Result.error(404, "房间不存在");
        }
        return Result.success(room);
    }
}