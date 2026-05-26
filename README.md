# Texas Hold'em Poker 德州扑克

全栈开发的德州扑克在线游戏平台，支持房间创建、实时对战、筹码管理、WebSocket 双向通信。

## 技术栈

### 后端
- **Spring Boot 3.2** + Java 17
- **Spring Data JPA** + MySQL 8.0
- **Spring Data Redis** + Redis
- **Spring Security** + JWT
- **Spring WebSocket** (STOMP)

### 前端
- **Vue 3.4** + Vite 5.0
- **Pinia 2.1** (状态管理)
- **Vue Router 4.2**
- **Element Plus 2.4**
- **Axios**

## 项目结构

```
Texas holdem poker/
├── poker-backend/                    # Spring Boot 后端
│   └── src/main/java/com/poker/
│       ├── config/                   # Redis/JWT/Security/WebSocket配置
│       ├── controller/              # REST API控制器
│       ├── service/                 # 业务逻辑层
│       ├── game/                    # 核心游戏模块
│       │   ├── engine/              # GameEngine/BettingManager
│       │   ├── logic/               # Deck/HandEvaluator
│       │   └── model/               # Card/Player/Room/GameState
│       ├── websocket/                # WebSocket通信
│       └── entity/                  # JPA实体
│
└── poker-vue/                        # Vue 3 前端
    └── src/
        ├── api/                     # HTTP API封装
        ├── store/                   # Pinia状态管理
        ├── views/                   # 页面组件
        ├── components/              # 公共组件
        └── websocket/               # WebSocket客户端
```

## 核心功能

- [x] 用户注册 / 登录（JWT 认证）
- [x] 创建房间 / 加入房间 / 离开房间
- [x] 游戏状态机（PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN）
- [x] 下注动作（FOLD / CHECK / CALL / RAISE / ALL_IN）
- [x] 边池结算 / 退还未跟注筹码（Uncalled Bet）
- [x] 弃牌提前获胜（Early Win on Fold）
- [x] All-In 极速发牌（Fast-forward on All-In）
- [x] 补充筹码（Rebuy，仅 WAITING 阶段可用）
- [x] WebSocket 实时同步（Backend is the Source of Truth）
- [x] 游戏记录持久化

## 运行要求

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Redis | 6.0+ |

## 本地运行

### 后端

```bash
cd poker-backend
# 配置 application.yml 中的数据库和 Redis 连接
mvn spring-boot:run
```

### 前端

```bash
cd poker-vue
npm install
npm run dev
```

访问 `http://localhost:5173`

## API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/auth/register | 注册 |
| POST | /api/auth/login | 登录 |
| GET | /api/user/me | 获取当前用户信息 |
| GET | /api/room/all | 获取房间列表 |
| POST | /api/room/create | 创建房间 |
| POST | /api/room/join/{roomCode} | 加入房间 |
| POST | /api/room/leave/{roomCode} | 离开房间 |
| POST | /api/room/rebuy | 补充筹码 |

## WebSocket 消息类型

| 类型 | 方向 | 说明 |
|------|------|------|
| CONNECT | 服务端 | 连接成功 |
| JOIN_ROOM | 客户端→服务端 | 加入房间 |
| PLAYER_ACTION | 双向 | 玩家动作（FOLD/CALL/RAISE/ALL_IN） |
| YOUR_TURN | 服务端 | 轮到玩家行动 |
| GAME_STATE | 服务端 | 游戏状态同步 |
| SHOWDOWN_RESULT | 服务端 | 结算结果 |
| ROUND_RESULT | 服务端 | 提前获胜结果 |
| ROOM_UPDATE | 服务端 | 房间状态更新 |