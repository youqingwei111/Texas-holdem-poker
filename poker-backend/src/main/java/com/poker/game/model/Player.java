package com.poker.game.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家模型
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player implements Serializable {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;

    private Long chips;
    private Long currentBet;
    private Long totalBetInRound = 0L;
    private Long buyInChips = 0L;
    private Long totalInvestedInHand = 0L;
    private List<Card> handCards = new ArrayList<>();

    private Boolean isReady = false;
    private Boolean isFold = false;
    private Boolean isAllIn = false;
    private Boolean isActive = true;
    private Boolean isOnline = true;
    private Boolean hasRefunded = false;

    private Integer position;

    public Player() {}

    public Player(Long userId, String username, String nickname, Long chips) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.chips = chips;
        this.buyInChips = chips;
    }
}