package com.poker.game.logic;

import com.poker.game.engine.SidePotManager;
import com.poker.game.model.Card;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import com.poker.game.model.SidePot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 德州扑克逻辑 - 比牌与结算
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PokerLogic {

    private final HandEvaluator handEvaluator = new HandEvaluator();
    private final SidePotManager sidePotManager;

    public List<Player> determineWinners(Room room, List<Card> communityCards) {
        List<Player> activePlayers = room.getPlayers().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                .collect(Collectors.toList());

        if (activePlayers.isEmpty()) return Collections.emptyList();
        if (activePlayers.size() == 1) return activePlayers;

        Map<Player, HandEvaluator.HandResult> results = new LinkedHashMap<>();
        for (Player player : activePlayers) {
            HandEvaluator.HandResult result = handEvaluator.evaluate(
                    player.getHandCards(), communityCards);
            results.put(player, result);
        }

        HandEvaluator.HandResult best = null;
        for (HandEvaluator.HandResult r : results.values()) {
            if (best == null || handEvaluator.compare(r, best) > 0) {
                best = r;
            }
        }

        final HandEvaluator.HandResult bestFinal = best;
        return results.entrySet().stream()
                .filter(e -> handEvaluator.compare(e.getValue(), bestFinal) == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public Map<Player, Long> settlePot(Room room, List<Card> communityCards) {
        Map<Player, Long> winnings = new LinkedHashMap<>();

        if (sidePotManager.needsSidePots(room)) {
            List<SidePot> sidePots = sidePotManager.calculateSidePots(room);
            log.info("边池计算结果: {}", sidePotManager.describeSidePots(sidePots));

            for (SidePot sidePot : sidePots) {
                List<Player> eligible = room.getPlayers().stream()
                        .filter(p -> sidePot.getEligiblePlayerIds().contains(p.getUserId()))
                        .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                        .collect(Collectors.toList());

                if (eligible.isEmpty()) continue;

                List<Player> potWinners = findBestPlayers(eligible, communityCards);
                distributePot(sidePot.getAmount(), potWinners, winnings);
            }
        } else {
            long pot = room.getGameState().getPot();
            List<Player> winners = determineWinners(room, communityCards);
            distributePot(pot, winners, winnings);
        }

        return winnings;
    }

    private List<Player> findBestPlayers(List<Player> candidates, List<Card> communityCards) {
        if (candidates.size() == 1) return candidates;

        Map<Player, HandEvaluator.HandResult> results = new LinkedHashMap<>();
        for (Player p : candidates) {
            results.put(p, handEvaluator.evaluate(p.getHandCards(), communityCards));
        }

        HandEvaluator.HandResult best = results.values().stream()
                .max((a, b) -> handEvaluator.compare(a, b))
                .orElse(null);

        if (best == null) return candidates;

        final HandEvaluator.HandResult bestFinal = best;
        return results.entrySet().stream()
                .filter(e -> handEvaluator.compare(e.getValue(), bestFinal) == 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void distributePot(long amount, List<Player> winners, Map<Player, Long> winnings) {
        if (winners.isEmpty() || amount <= 0) return;

        long share = amount / winners.size();
        long remainder = amount % winners.size();

        for (int i = 0; i < winners.size(); i++) {
            Player winner = winners.get(i);
            long gain = (i == 0) ? share + remainder : share;
            winner.setChips(winner.getChips() + gain);
            winnings.merge(winner, gain, Long::sum);
        }
    }

    public HandEvaluator.HandResult evaluatePlayer(Player player, List<Card> communityCards) {
        return handEvaluator.evaluate(player.getHandCards(), communityCards);
    }

    public int comparePlayers(Player p1, Player p2, List<Card> communityCards) {
        HandEvaluator.HandResult r1 = handEvaluator.evaluate(p1.getHandCards(), communityCards);
        HandEvaluator.HandResult r2 = handEvaluator.evaluate(p2.getHandCards(), communityCards);
        return handEvaluator.compare(r1, r2);
    }

    public HandEvaluator getHandEvaluator() {
        return handEvaluator;
    }
}