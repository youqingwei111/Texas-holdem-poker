package com.poker.game.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 房间模型
 */
@Data
@Entity
@Table(name = "rooms")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Room implements Serializable {

    @Id
    private String code;
    private String name;
    private Long ownerId;
    private String ownerName;

    private Integer smallBlind;
    private Integer bigBlind;
    private Integer minBuyIn;
    private Integer maxBuyIn;
    private Integer maxPlayers;

    private List<Player> players = new ArrayList<>();
    private GameState gameState;

    private Long createdAt;
    private Boolean isPlaying = false;
    private Boolean full = false;

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }

    public Boolean getFull() {
        return isFull();
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Long userId) {
        players.removeIf(p -> p.getUserId().equals(userId));
    }

    public Player getPlayer(Long userId) {
        return players.stream()
                .filter(p -> p.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
    }

    public boolean hasPlayer(Long userId) {
        return players.stream().anyMatch(p -> p.getUserId().equals(userId));
    }
}