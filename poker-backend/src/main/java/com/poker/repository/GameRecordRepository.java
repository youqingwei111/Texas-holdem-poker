package com.poker.repository;

import com.poker.entity.GameRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 游戏记录数据访问层
 */
@Repository
public interface GameRecordRepository extends JpaRepository<GameRecord, Long> {

    List<GameRecord> findByRoomCode(String roomCode);

    List<GameRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}