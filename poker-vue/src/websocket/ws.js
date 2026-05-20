import { ElMessage } from "element-plus";

const WS_URL = "ws://localhost:8080/ws/game";

class WebSocketClient {
  constructor() {
    this.ws = null;
    this.token = "";
    this.roomCode = "";
    this.connected = false;
    this.authenticated = false;
    this.reconnecting = false;
    this.reconnectTimer = null;
    this.reconnectDelay = 3000;
    this.maxReconnectDelay = 30000;
    this.messageCallbacks = new Set();
    this.sendCallbacks = new Set();
    this.heartbeatTimer = null;
    this.heartbeatInterval = 30000;
    this.stopPollFn = () => {};
    this.pollTimer = null;
    this.pollInterval = 2000;
  }

  connect(token, roomCode = "", userId = null) {
    if (this.ws && this.connected) {
      console.log("[WS] 已连接，如需切换房间请先断开");
      return;
    }

    this.token = token;
    this.roomCode = roomCode;
    this.userId = userId;
    const url = `${WS_URL}?token=${encodeURIComponent(token)}`;

    console.log("[WS] 正在连接...", url.replace(token, "***"));
    try {
      this.ws = new WebSocket(url);
      this.setupEventHandlers();
    } catch (error) {
      console.error("[WS] 连接异常:", error);
      this.scheduleReconnect();
    }
  }

  setupEventHandlers() {
    this.ws.onopen = () => {
      console.log("[WS] TCP 连接已建立，等待后端鉴权...");
      this.connected = true;
      if (this.roomCode) {
        this.joinRoom(this.roomCode);
      }
    };

    this.ws.onmessage = (event) => {
      try {
        const msg = JSON.parse(event.data);
        console.log("[WS] 收到:", msg);
        this.handleMessage(msg);
      } catch (error) {
        console.error("[WS] 消息解析失败:", error);
      }
    };

    this.ws.onerror = (error) => {
      console.error("[WS] 连接错误:", error);
    };

    this.ws.onclose = (event) => {
      const wasAuth = this.authenticated;
      this.connected = false;
      this.authenticated = false;
      this.stopHeartbeat();

      console.log(`[WS] 连接关闭 code=${event.code} reason=${event.reason}`);

      if (wasAuth && event.code !== 1000) {
        this.scheduleReconnect();
      }
    };
  }

  handleMessage(msg) {
    const { type, data } = msg;

    switch (type) {
      case "CONNECT": {
        if (data?.userId == null) {
          console.warn("[WS] CONNECT 缺少 userId，忽略:", data);
          return;
        }
        this.authenticated = true;
        this.userId = data.userId;
        console.log(`[WS] 用户身份识别成功 | userId=${data.userId} | username=${data.username}`);
        this.startHeartbeat();
        break;
      }

      case "ERROR": {
        console.error("[WS] 服务端错误:", data?.message);
        ElMessage.error(data?.message || "WebSocket 错误");
        break;
      }

      case "pong":
      case "PONG": {
        console.log("[WS] 心跳响应");
        return;
      }

      case "ROOM_UPDATE": {
        console.log("[WS] 房间更新", data);
        break;
      }

      case "PLAYER_JOINED": {
        console.log("[WS] 玩家加入", data);
        break;
      }

      case "PLAYER_LEFT": {
        console.log("[WS] 玩家离开", data);
        break;
      }

      case "PLAYER_READY": {
        console.log("[WS] 玩家准备", data);
        break;
      }

      case "GAME_START": {
        console.log("[WS] 游戏开始", data);
        this.stopPoll();
        break;
      }

      case "GAME_STATE": {
        console.log("[WS] 游戏状态", data);
        break;
      }

      case "DEAL_CARDS": {
        console.log("[WS] 发牌", data);
        this.stopPoll();
        break;
      }

      case "COMMUNITY_CARDS": {
        console.log("[WS] 公共牌", data);
        break;
      }

      case "YOUR_TURN": {
        console.log("[WS] 轮到你了", data);
        this.stopPoll();
        break;
      }

      case "PLAYER_ACTION": {
        console.log("[WS] 玩家动作", data);
        break;
      }

      case "ACTION_RESULT": {
        console.log("[WS] 动作结果", data);
        break;
      }

      case "PHASE_CHANGE": {
        console.log("[WS] 阶段变更", data);
        break;
      }

      case "FLOP": {
        console.log("[WS] 翻牌圈", data);
        break;
      }

      case "TURN": {
        console.log("[WS] 转牌圈", data);
        break;
      }

      case "RIVER": {
        console.log("[WS] 河牌圈", data);
        break;
      }

      case "SHOWDOWN": {
        console.log("[WS] 比牌", data);
        break;
      }

      case "SHOWDOWN_RESULT": {
        console.log("[WS] 比牌结果", data);
        break;
      }

      case "ROUND_RESULT": {
        console.log("[WS] 回合结果", data);
        break;
      }

      case "POT_UPDATE": {
        console.log("[WS] 底池更新", data);
        break;
      }

      case "GAME_END": {
        console.log("[WS] 游戏结束", data);
        break;
      }

      case "CHAT": {
        console.log("[WS] 聊天", data);
        break;
      }

      default: {
        console.log("[WS] 未知消息类型:", type, data);
        break;
      }
    }

    this.messageCallbacks.forEach((cb) => {
      try {
        cb(type, data);
      } catch (e) {
        console.error("[WS] 回调异常:", e);
      }
    });
  }

  send(type, data = {}) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn("[WS] 未连接，无法发送:", type);
      return false;
    }
    const payload = JSON.stringify({ type, data });
    try {
      this.ws.send(payload);
      console.log("[WS] 发送:", { type, data });
      this.sendCallbacks.forEach((cb) => {
        try {
          cb(type, data);
        } catch {}
      });
      return true;
    } catch (error) {
      console.error("[WS] 发送失败:", error);
      return false;
    }
  }

  joinRoom(roomCode) {
    this.roomCode = roomCode;
    console.log(`[WS] 申请加入房间: ${roomCode}`);
    return this.send("JOIN_ROOM", { roomCode });
  }

  leaveRoom() {
    const ok = this.send("LEAVE_ROOM");
    this.roomCode = "";
    return ok;
  }

  playerReady() {
    return this.send("PLAYER_READY");
  }

  playerAction(action, amount) {
    return this.send("PLAYER_ACTION", { action, amount });
  }

  chat(content) {
    return this.send("CHAT", { content });
  }

  startGame() {
    return this.send("START_GAME");
  }

  onMessage(callback) {
    this.messageCallbacks.add(callback);
    return () => this.messageCallbacks.delete(callback);
  }

  startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      if (this.connected && this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.ws.send("ping");
        console.log("[WS] ping");
      }
    }, this.heartbeatInterval);
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  startPoll(pollFn) {
    this.stopPoll();
    this.pollFn = pollFn;
    this.pollTimer = setInterval(() => {
      if (this.pollFn) {
        this.pollFn();
      }
    }, this.pollInterval);
  }

  stopPoll() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  scheduleReconnect() {
    if (this.reconnecting || !this.token) return;
    this.reconnecting = true;
    console.log(`[WS] ${this.reconnectDelay / 1000}s 后尝试重连...`);
    this.reconnectTimer = setTimeout(() => {
      console.log("[WS] 正在重连...");
      this.reconnecting = false;
      this.connect(this.token, this.roomCode, this.userId);
    }, this.reconnectDelay);
    this.reconnectDelay = Math.min(this.reconnectDelay * 2, this.maxReconnectDelay);
  }

  disconnect() {
    this.stopHeartbeat();
    this.stopPoll();
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.reconnecting = false;
    this.authenticated = false;
    this.roomCode = "";
    if (this.ws) {
      this.ws.close(1000, "主动断开");
      this.ws = null;
    }
    this.connected = false;
    this.messageCallbacks.clear();
  }

  isConnected() {
    return this.connected;
  }

  isAuthenticated() {
    return this.authenticated;
  }
}

export const wsClient = new WebSocketClient();

export const connectWs = (token, roomCode, userId) => wsClient.connect(token, roomCode, userId);
export const sendWs = (type, data) => wsClient.send(type, data);
export const joinRoomWs = (roomCode) => wsClient.joinRoom(roomCode);
export const leaveRoomWs = () => wsClient.leaveRoom();
export const playerReadyWs = () => wsClient.playerReady();
export const playerActionWs = (action, amount) => wsClient.playerAction(action, amount);
export const chatWs = (content) => wsClient.chat(content);
export const startGameWs = () => wsClient.startGame();
export const onWsMessage = (callback) => wsClient.onMessage(callback);
export const startPollWs = (fn) => wsClient.startPoll(fn);
export const stopPollWs = () => wsClient.stopPoll();
export const disconnectWs = () => wsClient.disconnect();
export const isWsConnected = () => wsClient.isConnected();
export const isWsAuthenticated = () => wsClient.isAuthenticated();