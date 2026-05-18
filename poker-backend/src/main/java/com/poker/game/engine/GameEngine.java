package com.poker.game.engine;

import com.poker.game.enums.GamePhase;
import com.poker.game.logic.Deck;
import com.poker.game.model.Card;
import com.poker.game.model.GameState;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 游戏引擎 - 状态机核心
 */
@Slf4j
@Component
public class GameEngine {

    public void startGame(Room room) {
        Deck deck = new Deck();
        deck.shuffle();

        GameState state = new GameState();
        state.setPhase(GamePhase.PRE_FLOP);
        state.setPot(0L);
        state.setCurrentBet(0L);
        state.setRoundStartTime(System.currentTimeMillis());

        for (Player player : room.getPlayers()) {
            player.setHandCards(new java.util.ArrayList<>());
            player.setCurrentBet(0L);
            player.setTotalBetInRound(0L);
            player.setIsFold(false);
            player.setIsAllIn(false);
            player.setIsActive(true);
        }

        int dealerPos = (int) (System.currentTimeMillis() % room.getPlayerCount());
        state.setDealerPosition(dealerPos, room.getPlayerCount());

        for (Player player : room.getPlayers()) {
            player.setHandCards(deck.deal(2));
        }

        state.setDeck(deck.deal(deck.remaining()));

        Player smallBlindPlayer = room.getPlayers().get(state.getSmallBlindIndex());
        Player bigBlindPlayer = room.getPlayers().get(state.getBigBlindIndex());

        long sb = Math.min(room.getSmallBlind(), smallBlindPlayer.getChips());
        long bb = Math.min(room.getBigBlind(), bigBlindPlayer.getChips());

        smallBlindPlayer.setChips(smallBlindPlayer.getChips() - sb);
        smallBlindPlayer.setCurrentBet(sb);
        smallBlindPlayer.setTotalBetInRound(sb);

        bigBlindPlayer.setChips(bigBlindPlayer.getChips() - bb);
        bigBlindPlayer.setCurrentBet(bb);
        bigBlindPlayer.setTotalBetInRound(bb);

        state.setPot(sb + bb);
        state.setCurrentBet(bb);

        if (room.getPlayerCount() == 2) {
            state.setCurrentTurnIndex(state.getSmallBlindIndex());
            state.markPlayerActed(bigBlindPlayer.getUserId());
            state.setLastRaiseAmount(bb - sb);
        } else {
            state.setCurrentTurnIndex((state.getBigBlindIndex() + 1) % room.getPlayerCount());
            state.markPlayerActed(bigBlindPlayer.getUserId());
            state.markPlayerActed(smallBlindPlayer.getUserId());
            state.setLastRaiseAmount((long) room.getBigBlind());
        }

        room.setGameState(state);
        room.setIsPlaying(true);
    }

    public GamePhase nextPhase(Room room) {
        return advancePhase(room);
    }

    public GamePhase advancePhase(Room room) {
        GameState state = room.getGameState();
        GamePhase currentPhase = state.getPhase();
        List<Card> deck = state.getDeck();

        switch (currentPhase) {
            case PRE_FLOP:
                dealFromDeck(deck, state.getCommunityCards(), 3);
                state.setPhase(GamePhase.FLOP);
                break;

            case FLOP:
                dealFromDeck(deck, state.getCommunityCards(), 1);
                state.setPhase(GamePhase.TURN);
                break;

            case TURN:
                dealFromDeck(deck, state.getCommunityCards(), 1);
                state.setPhase(GamePhase.RIVER);
                break;

            case RIVER:
                state.setPhase(GamePhase.SHOWDOWN);
                break;

            default:
                break;
        }

        resetBettingRound(room);
        advanceDealerToFirstPlayer(room);

        log.info("[Engine] 阶段推进: {} → {}, 庄家={}, 当前行动={}",
                currentPhase, state.getPhase(),
                state.getDealerIndex(), state.getCurrentTurnIndex());

        return state.getPhase();
    }

    private void advanceDealerToFirstPlayer(Room room) {
        GameState state = room.getGameState();

        int newDealerIndex = (state.getDealerIndex() + 1) % room.getPlayerCount();
        state.setDealerIndex(newDealerIndex);

        int startIdx = (newDealerIndex + 1) % room.getPlayerCount();
        for (int i = 0; i < room.getPlayerCount(); i++) {
            int idx = (startIdx + i) % room.getPlayerCount();
            Player p = room.getPlayers().get(idx);
            if (!Boolean.TRUE.equals(p.getIsFold()) && !Boolean.TRUE.equals(p.getIsAllIn()) && p.getChips() > 0) {
                state.setCurrentTurnIndex(idx);
                log.info("[Engine] 首个行动玩家: {} (index={})", p.getUsername(), idx);
                return;
            }
        }
        state.setCurrentTurnIndex(startIdx);
    }

    public boolean canAdvancePhase(Room room) {
        return isBettingRoundOver(room);
    }

    public boolean isBettingRoundOver(Room room) {
        GameState state = room.getGameState();
        long currentBet = state.getCurrentBet();

        for (Player player : room.getPlayers()) {
            if (Boolean.TRUE.equals(player.getIsFold())) {
                continue;
            }

            if (Boolean.TRUE.equals(player.getIsAllIn())) {
                if (!state.hasPlayerActed(player.getUserId())) {
                    return false;
                }
                continue;
            }

            if (!player.getCurrentBet().equals(currentBet)) {
                log.debug("[Engine] 轮未结束：玩家 {} 的 bet={} != 当前下注 {}",
                        player.getUsername(), player.getCurrentBet(), currentBet);
                return false;
            }

            if (!state.hasPlayerActed(player.getUserId())) {
                log.debug("[Engine] 轮未结束：玩家 {} 尚未行动", player.getUsername());
                return false;
            }
        }

        log.info("[Engine] ✅ 下注轮结束，满足条件：currentBet={}", currentBet);
        return true;
    }

    private void resetBettingRound(Room room) {
        GameState state = room.getGameState();

        state.setCurrentBet(0L);
        state.resetActedPlayers();
        state.setLastRaiseAmount(null);

        for (Player player : room.getPlayers()) {
            player.setCurrentBet(0L);
        }

        log.info("[Engine] 重置下注轮，庄家位置={}", state.getDealerIndex());
    }

    public Player getCurrentPlayer(Room room) {
        GameState state = room.getGameState();
        return room.getPlayers().get(state.getCurrentTurnIndex());
    }

    public void nextPlayer(Room room) {
        GameState state = room.getGameState();
        int playerCount = room.getPlayerCount();
        int startIndex = state.getCurrentTurnIndex();
        int attempts = 0;

        do {
            state.nextTurn(playerCount);
            int idx = state.getCurrentTurnIndex();
            Player next = room.getPlayers().get(idx);

            if (Boolean.TRUE.equals(next.getIsFold())) {
                log.debug("[Engine] 跳过弃牌玩家: {}", next.getUsername());
            } else if (Boolean.TRUE.equals(next.getIsAllIn()) || next.getChips() <= 0) {
                log.debug("[Engine] 跳过全下玩家: {} (chips={})", next.getUsername(), next.getChips());
            } else if (state.hasPlayerActed(next.getUserId())) {
                log.debug("[Engine] 跳过已行动玩家: {}", next.getUsername());
            } else {
                log.info("[Engine] 下一个行动玩家: {} (index={})", next.getUsername(), idx);
                return;
            }

            attempts++;
        } while (state.getCurrentTurnIndex() != startIndex && attempts < playerCount);

        log.warn("[Engine] nextPlayer 未找到有效玩家，startIndex={}, currentIndex={}",
                startIndex, state.getCurrentTurnIndex());
    }

    public boolean isOnlyOnePlayerLeft(Room room) {
        long actableCount = room.getPlayers().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                .filter(p -> p.getChips() != null && p.getChips() > 0)
                .count();
        return actableCount == 1;
    }

    public boolean hasActablePlayers(Room room) {
        return room.getPlayers().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                .anyMatch(p -> p.getChips() != null && p.getChips() > 0);
    }

    public Player getLastActivePlayer(Room room) {
        return room.getPlayers().stream()
                .filter(p -> !p.getIsFold())
                .findFirst()
                .orElse(null);
    }

    public Player getWinner(Room room) {
        return getLastActivePlayer(room);
    }

    private void dealFromDeck(List<Card> deck, List<Card> communityCards, int count) {
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            communityCards.add(deck.remove(deck.size() - 1));
        }
    }
}