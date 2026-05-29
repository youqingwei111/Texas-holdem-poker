# 更新日志

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [1.0.0] - 2024-12-19

### 新特性

- ✨ 用户注册 / 登录（JWT 无状态认证）
- ✨ 创建房间 / 加入房间 / 离开房间
- ✨ 游戏状态机完整实现（PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN）
- ✨ 下注动作：FOLD / CHECK / CALL / RAISE / ALL_IN
- ✨ 边池结算（Side Pot）与退还未跟注筹码（Uncalled Bet）
- ✨ 弃牌提前获胜（Early Win on Fold）
- ✨ All-In 极速发牌（Fast-forward）
- ✨ 补充筹码（Rebuy，仅等待阶段可用）
- ✨ WebSocket 实时同步

### 技术升级

- 🔧 Spring Boot 3.2 + Java 17
- 🔧 Vue 3.4 + Vite 5.0
- 🔧 Spring Data Redis 房间状态缓存
- 🔧 STOMP 协议 WebSocket 通信