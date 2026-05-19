package com.poker.websocket.message;

import lombok.Data;

/**
 * WebSocket消息基类
 */
@Data
public class WsMessage<T> {

    private MessageType type;
    private T data;
    private Long timestamp;

    public WsMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public WsMessage(MessageType type, T data) {
        this();
        this.type = type;
        this.data = data;
    }

    public static <T> WsMessage<T> of(MessageType type, T data) {
        return new WsMessage<>(type, data);
    }
}