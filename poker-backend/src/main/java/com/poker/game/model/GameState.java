package com.poker.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.poker.game.enums.GamePhase;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 游戏状态
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameState implements Serializable {

    private GamePhase phase = GamePhase.WAITING;

    private List<Card> communityCards = new ArrayList<>();
    private List<Card> deck = new ArrayList<>();

    private Long pot = 0L;
    private Long currentBet = 0L;
    private Integer currentTurnIndex = 0;
    private Integer dealerIndex = 0;
    private Integer smallBlindIndex = 1;
    private Integer bigBlindIndex = 2;

    private Long roundStartTime;
    private Set<Long> actedPlayers = new HashSet<>();
    private Integer lastRaiserIndex = null;
    private Long lastRaiseAmount = null;

    @JsonIgnore
    public int getCurrentTurnPosition() {
        return currentTurnIndex;
    }

    public void nextTurn(int playerCount) {
        currentTurnIndex = (currentTurnIndex + 1) % playerCount;
    }

    public void setDealerPosition(int index, int playerCount) {
        this.dealerIndex = index;
        if (playerCount == 2) {
            this.smallBlindIndex = index;
            this.bigBlindIndex = (index + 1) % playerCount;
        } else {
            this.smallBlindIndex = (index + 1) % playerCount;
            this.bigBlindIndex = (index + 2) % playerCount;
        }
    }

    public void markPlayerActed(Long userId) {
        actedPlayers.add(userId);
    }

    public boolean hasPlayerActed(Long userId) {
        return actedPlayers.contains(userId);
    }

    public void resetActedPlayers() {
        actedPlayers.clear();
        lastRaiserIndex = null;
    }

    public void setLastRaiser(int index) {
        this.lastRaiserIndex = index;
    }

    public Integer getLastRaiserIndex() {
        return lastRaiserIndex;
    }
}