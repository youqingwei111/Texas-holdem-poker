package com.poker.common;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    SYSTEM_ERROR(500, "系统错误"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_INVALID(1004, "Token无效"),
    TOKEN_EXPIRED(1005, "Token已过期"),
    ROOM_NOT_FOUND(2001, "房间不存在"),
    ROOM_FULL(2002, "房间已满"),
    ROOM_ALREADY_EXISTS(2003, "房间已存在"),
    PLAYER_NOT_IN_ROOM(2004, "玩家不在房间中"),
    GAME_ALREADY_STARTED(2005, "游戏已开始"),
    NOT_YOUR_TURN(2006, "不是你的回合"),
    INSUFFICIENT_CHIPS(2007, "筹码不足");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}