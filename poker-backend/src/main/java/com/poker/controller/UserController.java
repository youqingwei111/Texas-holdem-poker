package com.poker.controller;

import com.poker.common.Result;
import com.poker.entity.User;
import com.poker.service.UserService;
import com.poker.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    public Result<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(token);
        User user = userService.getById(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("email", user.getEmail());
        data.put("chips", user.getChips());
        data.put("totalGames", user.getTotalGames());
        data.put("winGames", user.getWinGames());
        return Result.success(data);
    }
}