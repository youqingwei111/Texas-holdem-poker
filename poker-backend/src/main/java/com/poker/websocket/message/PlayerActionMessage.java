package com.poker.websocket.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 玩家动作消息
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerActionMessage {

    private String action;
    private Long amount;
}