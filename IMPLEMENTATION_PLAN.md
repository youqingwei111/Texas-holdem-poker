# 德州扑克项目重新实现规划

## 一、技术选型

### 后端

- Spring Boot 3.2.0 + Java 21
- Spring Data JPA + MySQL 8.0
- Spring Data Redis + Redis
- Spring Security + JWT (jjwt 0.12.3)
- Spring WebSocket
- Lombok
- Gradle 构建

### 前端

- Vue 3.4 + Vite 5.0
- Pinia 2.1（状态管理）
- Vue Router 4.2
- Element Plus 2.4
- Axios 1.14

### 本地通信方案

- WebSocket + 轮询双轨并行（降级方案）

---

## 二、项目结构

```
Texas holdem poker/
├── poker-backend/                    # Spring Boot 后端
│   ├── src/main/java/com/poker/
│   │   ├── PokerApplication.java
│   │   ├── config/                   # Redis/JWT/Security/WebSocket配置
│   │   ├── common/                   # Result/Utils/ErrorCode
│   │   ├── entity/                   # User/GameRecord
│   │   ├── dto/                     # LoginDTO/RoomDTO等
│   │   ├── repository/
│   │   ├── service/
│   │   ├── controller/
│   │   ├── game/                    # 核心游戏模块
│   │   │   ├── enums/               # GamePhase/PlayerAction
│   │   │   ├── model/               # Card/Player/Room/GameState/SidePot
│   │   │   ├── logic/               # Deck/HandEvaluator/PokerLogic
│   │   │   └── engine/              # GameEngine/BettingManager/SidePotManager
│   │   ├── websocket/
│   │   ├── exception/
│   │   └── util/
│   └── build.gradle
└── poker-vue/                        # Vue 3 前端
    ├── src/
    │   ├── api/
    │   ├── components/
    │   ├── views/
    │   ├── store/
    │   ├── router/
    │   ├── websocket/
    │   └── utils/
    └── package.json
```

---

## 三、分阶段开发计划

### 阶段一：项目基础设施（第1-2天）

**后端：**

- 1创建 Gradle 项目结构，配置 build.gradle
- 1配置 application.yml
- 1实现 SecurityConfig、JwtConfig、RedisConfig
- 1数据库实体：User、GameRecord

**前端：**

- 1Vite 创建 Vue 3 项目
- 1配置 package.json 依赖
- 基础应用结构（main.js, App.vue, router, store）
- Axios 封装

**里程碑：** 后端启动成功，前端登录页可访问

---

### 阶段二：用户认证（第3-4天）

**后端：**

- UserRepository、AuthController、UserService
- JWT 生成与验证

**前端：**

- Login.vue、auth.js、userStore.js

**里程碑：** 注册/登录功能正常

---

### 阶段三：房间系统（第5-7天）

**后端：**

- Room、Player 模型
- RoomService、RoomController

**前端：**

- Lobby.vue、roomStore、room.js

**里程碑：** 房间创建、列表、加入功能正常

---

### 阶段四：核心游戏引擎（第8-12天）

**游戏数据模型：**

```
Card: suit, rank (2-14)
Player: userId, chips, handCards[], currentBet, isFold, isAllIn, isActive
GameState: phase, pot, communityCards[], dealerIndex, currentTurnIndex
Room: roomCode, players[], gameState, smallBlind, bigBlind
```

**核心类：**

| 类             | 职责                            |
| -------------- | ------------------------------- |
| Deck           | shuffle(), deal()               |
| HandEvaluator  | evaluate() - 10级手牌评级       |
| PokerLogic     | determineWinners(), settlePot() |
| GameEngine     | 状态机核心                      |
| BettingManager | FOLD/CHECK/CALL/RAISE/ALL_IN    |
| SidePotManager | calculateSidePots()             |

**手牌评级：** 皇家同花顺 > 同花顺 > 四条 > 葫芦 > 同花 > 顺子 > 三条 > 两对 > 一对 > 高牌

**阶段流转：** WAITING → PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN → FINISHED

**里程碑：** 游戏流程完整运行，比牌结算正确

---

### 阶段五：WebSocket 通信（第13-15天）

**后端：**

- GameWebSocketHandler
- MessageDispatcher
- WebSocketSessionManager

**前端：**

- ws.js（连接管理 + 重连 + 轮询降级）
- gameStore.js

**里程碑：** 实时同步正常，断线重连正常

---

### 阶段六：前端 UI/UX（第16-18天）

**组件：**

- PokerCard.vue、PlayerSeat.vue、CommunityCards.vue、ActionPanel.vue

**布局：** 环形座位、毛毡牌桌、庄家按钮、底池显示

**里程碑：** 游戏界面完整可用

---

### 阶段七：测试与调试（第19-21天）

- 单元测试（HandEvaluator、Deck、BettingManager、SidePotManager）
- 集成测试（完整游戏流程）
- 性能与稳定性测试

**里程碑：** 无明显Bug，功能完整

---

## 四、关键文件（从原项目参考）

1. `c:\Users\尤青伟\Desktop\德州\poker-backend\src\main\java\com\poker\game\logic\HandEvaluator.java`
2. `c:\Users\尤青伟\Desktop\德州\poker-backend\src\main\java\com\poker\game\engine\GameEngine.java`
3. `c:\Users\尤青伟\Desktop\德州\poker-backend\src\main\java\com\poker\game\engine\BettingManager.java`
4. `c:\Users\尤青伟\Desktop\德州\poker-backend\src\main\java\com\poker\game\engine\SidePotManager.java`
5. `c:\Users\尤青伟\Desktop\德州\poker-vue\src\views\Game.vue`

---

## 五、开发依赖关系

```
阶段一（基础搭建）
    ↓
阶段二（用户认证）依赖阶段一
    ↓
阶段三（房间系统）依赖阶段二
    ↓
阶段四（游戏引擎）独立开发
    ↓
阶段五（WebSocket）依赖阶段三+四
    ↓
阶段六（前端UI）依赖阶段五
    ↓
阶段七（测试调试）
```

---

## 六、本地开发环境要求

| 组件     | 要求                            |
| -------- | ------------------------------- |
| 后端端口 | 8080                            |
| 前端端口 | 5173                            |
| Java     | 21                              |
| Gradle   | 8.x                             |
| MySQL    | 本地安装，创建 `poker` 数据库   |
| Redis    | 本地安装并启动                  |
| 跨域     | 后端配置允许前端 localhost:5173 |
