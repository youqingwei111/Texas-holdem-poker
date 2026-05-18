package com.poker.game.engine;

import com.poker.game.model.Player;
import com.poker.game.model.Room;
import com.poker.game.model.SidePot;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 边池管理器
 */
@Component
public class SidePotManager {

    public List<SidePot> calculateSidePots(Room room) {
        List<Player> allPlayers = room.getPlayers();

        List<Player> contributors = allPlayers.stream()
                .filter(p -> p.getTotalBetInRound() != null && p.getTotalBetInRound() > 0)
                .sorted(Comparator.comparingLong(Player::getTotalBetInRound))
                .collect(Collectors.toList());

        if (contributors.isEmpty()) {
            return Collections.emptyList();
        }

        List<SidePot> sidePots = new ArrayList<>();
        long previousLevel = 0L;

        for (int i = 0; i < contributors.size(); i++) {
            long currentLevel = contributors.get(i).getTotalBetInRound();

            if (currentLevel <= previousLevel) {
                continue;
            }

            long levelAmount = currentLevel - previousLevel;

            List<Player> participantsAtLevel = contributors.subList(i, contributors.size());
            long potAtLevel = levelAmount * participantsAtLevel.size();

            List<Long> eligibleIds = participantsAtLevel.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                    .map(Player::getUserId)
                    .collect(Collectors.toList());

            if (!eligibleIds.isEmpty() && potAtLevel > 0) {
                sidePots.add(new SidePot(potAtLevel, eligibleIds));
            }

            previousLevel = currentLevel;
        }

        return sidePots;
    }

    public boolean needsSidePots(Room room) {
        return room.getPlayers().stream().anyMatch(p -> Boolean.TRUE.equals(p.getIsAllIn()));
    }

    public String describeSidePots(List<SidePot> sidePots) {
        if (sidePots.isEmpty()) return "无边池";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sidePots.size(); i++) {
            SidePot sp = sidePots.get(i);
            sb.append(i == 0 ? "主池" : "边池" + i)
              .append(": ").append(sp.getAmount())
              .append(" (参与者: ").append(sp.getEligiblePlayerIds()).append(")");
            if (i < sidePots.size() - 1) sb.append(" | ");
        }
        return sb.toString();
    }
}