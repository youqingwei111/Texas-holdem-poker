package com.poker.websocket.message;

/**
 * 消息类型枚举
 */
public enum MessageType {

    // 连接相关
    CONNECT,           // 连接成功
    DISCONNECT,        // 断开连接
    ERROR,             // 错误
    PONG,              // 心跳响应

    // 房间相关
    JOIN_ROOM,         // 加入房间
    LEAVE_ROOM,        // 离开房间
    ROOM_UPDATE,       // 房间信息更新
    PLAYER_JOINED,     // 玩家加入
    PLAYER_LEFT,       // 玩家离开
    PLAYER_READY,      // 玩家准备

    // 游戏控制
    START_GAME,        // 开始游戏请求
    GAME_START,        // 游戏开始
    GAME_END,          // 游戏结束
    GAME_STATE,        // 游戏状态

    // 发牌相关
    DEAL_CARDS,        // 发牌（私牌）
    COMMUNITY_CARDS,   // 公共牌

    // 游戏流程
    NEW_ROUND,         // 新一轮开始
    YOUR_TURN,         // 轮到你了
    PLAYER_ACTION,     // 玩家动作
    ACTION_RESULT,     // 动作结果

    // 阶段切换
    PHASE_CHANGE,      // 阶段变更
    FLOP,              // 翻牌圈
    TURN,              // 转牌圈
    RIVER,             // 河牌圈
    SHOWDOWN,          // 比牌
    SHOWDOWN_RESULT,   // 比牌结果汇总

    // 结算
    ROUND_RESULT,      // 回合结果
    POT_UPDATE,        // 底池更新

    // 聊天
    CHAT               // 聊天消息
}