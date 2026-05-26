package com.poker.service;

import com.poker.entity.GameRecord;
import com.poker.game.model.Player;
import com.poker.repository.GameRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 游戏记录服务
 * 负责保存每一局的结算记录到数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRecordService {

    private final GameRecordRepository gameRecordRepository;

    /**
     * 保存一局游戏记录（完整字段版）
     */
    @Transactional
    public void saveRecord(String roomCode, long potAmount, List<Player> allPlayers,
                           List<Player> winners, String winningHandType) {
        log.info("[GameRecord] 保存游戏记录 | roomCode={} | pot={} | winners={} | handType={}",
                roomCode, potAmount, winners.size(), winningHandType);

        for (Player player : allPlayers) {
            GameRecord record = new GameRecord();

            record.setRoomCode(roomCode);
            record.setUserId(player.getUserId());

            // 获取结算后的筹码
            long chipsAfter = player.getChips() != null ? player.getChips() : 0L;

            // 判断是否赢家
            boolean isWinner = winners.stream()
                    .anyMatch(w -> w.getUserId().equals(player.getUserId()));

            // 本回合玩家总共投入了多少筹码（从 totalBetInRound 可以看出）
            long totalBetInRound = player.getTotalBetInRound() != null ? player.getTotalBetInRound() : 0L;

            if (isWinner) {
                // 赢家：赢得 pot / winners.size()
                long winAmount = potAmount / winners.size();
                record.setChipsChange(winAmount - totalBetInRound);  // 净赢 = 赢得 - 自己投入
                record.setChipsAfter(chipsAfter);
                record.setChipsBefore(chipsAfter - (winAmount - totalBetInRound));
                record.setResult("WIN");
                record.setHandRank(winningHandType);
            } else {
                // 输家或弃牌
                record.setChipsChange(-totalBetInRound);
                record.setChipsAfter(chipsAfter);
                record.setChipsBefore(chipsAfter + totalBetInRound);
                record.setResult(player.getIsFold() ? "FOLDED" : "LOSS");
                record.setHandRank(player.getIsFold() ? "FOLDED" : winningHandType);
            }

            // 底牌字符串
            if (player.getHandCards() != null && !player.getHandCards().isEmpty()) {
                String cardsStr = player.getHandCards().stream()
                        .map(c -> c.getDisplayName())
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                record.setHandCards(cardsStr);
            }

            gameRecordRepository.save(record);
            log.info("[GameRecord] 记录已保存 | userId={} | result={} | handRank={}",
                    player.getUserId(), record.getResult(), record.getHandRank());
        }

        log.info("[GameRecord] 本局共 {} 条记录已保存", allPlayers.size());
    }
}