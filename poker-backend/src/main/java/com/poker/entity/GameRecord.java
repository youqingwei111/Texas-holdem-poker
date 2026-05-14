package com.poker.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 游戏记录实体
 */
@Data
@Entity
@Table(name = "game_records")
public class GameRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String roomCode;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long chipsBefore;

    @Column(nullable = false)
    private Long chipsChange;

    @Column(nullable = false)
    private Long chipsAfter;

    @Column(length = 50)
    private String handCards;

    @Column(length = 30)
    private String handRank;

    @Column(length = 10)
    private String result;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}