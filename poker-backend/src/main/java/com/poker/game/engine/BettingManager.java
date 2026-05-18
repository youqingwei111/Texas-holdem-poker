package com.poker.game.engine;

import com.poker.game.enums.PlayerAction;
import com.poker.game.model.GameState;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * 下注管理器
 */
@Component
public class BettingManager {

    @Getter
    public static class BetResult {
        private final boolean success;
        private final String message;
        private final Long amount;
        private final Long pot;
        private final Long callAmount;
        private final Boolean roundOver;

        public BetResult(boolean success, String message) {
            this(success, message, 0L, 0L, 0L, false);
        }

        public BetResult(boolean success, String message, Long amount, Long pot, Long callAmount, Boolean roundOver) {
            this.success = success;
            this.message = message;
            this.amount = amount;
            this.pot = pot;
            this.callAmount = callAmount;
            this.roundOver = roundOver;
        }
    }

    public BetResult handleAction(Room room, Player player, PlayerAction action, Long raiseAmount) {
        GameState state = room.getGameState();

        if (Boolean.TRUE.equals(player.getIsFold())) {
            return new BetResult(false, "你已弃牌，无法行动");
        }
        if (Boolean.TRUE.equals(player.getIsAllIn())) {
            return new BetResult(false, "你已全下，无法继续下注");
        }
        if (!Boolean.TRUE.equals(player.getIsActive())) {
            return new BetResult(false, "玩家未在游戏中");
        }

        long callAmount = getCallAmount(player, state);
        long pot = state.getPot();

        switch (action) {
            case FOLD:
                return doFold(player, state);

            case CHECK:
                return doCheck(player, state, callAmount);

            case CALL:
                return doCall(room, player, state, callAmount);

            case RAISE:
                return doRaise(room, player, state, raiseAmount);

            case ALL_IN:
                return doAllIn(room, player, state, callAmount);

            default:
                return new BetResult(false, "未知动作");
        }
    }

    private BetResult doFold(Player player, GameState state) {
        player.setIsFold(true);
        state.markPlayerActed(player.getUserId());
        return new BetResult(true, "你已弃牌");
    }

    private BetResult doCheck(Player player, GameState state, long callAmount) {
        if (callAmount > 0) {
            return new BetResult(false, "无法过牌，需要跟注 " + callAmount);
        }
        state.markPlayerActed(player.getUserId());
        return new BetResult(true, "你已过牌", 0L, state.getPot(), 0L, false);
    }

    private BetResult doCall(Room room, Player player, GameState state, long callAmount) {
        if (callAmount <= 0) {
            return doCheck(player, state, 0);
        }

        long chipsToCall = Math.min(callAmount, player.getChips());
        player.setChips(player.getChips() - chipsToCall);
        player.setCurrentBet(player.getCurrentBet() + chipsToCall);
        player.setTotalBetInRound(player.getTotalBetInRound() + chipsToCall);
        state.setPot(state.getPot() + chipsToCall);
        state.markPlayerActed(player.getUserId());

        if (room.getPlayerCount() == 2) {
            resetOtherPlayersActed(state, room, player.getUserId());
        }

        boolean roundOver = isRoundComplete(room);
        return new BetResult(true, "跟注 " + chipsToCall, chipsToCall, state.getPot(), callAmount - chipsToCall, roundOver);
    }

    private void resetOtherPlayersActed(GameState state, Room room, Long raiserId) {
        state.resetActedPlayers();
        state.markPlayerActed(raiserId);
    }

    private BetResult doRaise(Room room, Player player, GameState state, Long raiseAmount) {
        long callAmount = getCallAmount(player, state);

        long minRaise = Math.max(callAmount + room.getBigBlind(), callAmount * 2);
        if (raiseAmount == null || raiseAmount < minRaise) {
            return new BetResult(false, "加注额不能少于 " + minRaise);
        }

        long totalBet = callAmount + raiseAmount;
        if (totalBet > player.getChips()) {
            return new BetResult(false, "筹码不足，当前剩余: " + player.getChips());
        }

        player.setChips(player.getChips() - totalBet);
        player.setCurrentBet(player.getCurrentBet() + totalBet);
        player.setTotalBetInRound(player.getTotalBetInRound() + totalBet);
        state.setPot(state.getPot() + totalBet);
        state.setCurrentBet(player.getCurrentBet());
        state.setLastRaiser(room.getPlayers().indexOf(player));
        state.markPlayerActed(player.getUserId());

        boolean roundOver = isRoundComplete(room);
        return new BetResult(true, "加注 " + totalBet + "（含跟注）", totalBet, state.getPot(), 0L, roundOver);
    }

    private BetResult doAllIn(Room room, Player player, GameState state, long callAmount) {
        long allInAmount = player.getChips();
        if (allInAmount <= 0) {
            return new BetResult(false, "没有可用的筹码");
        }

        if (allInAmount >= callAmount) {
            player.setCurrentBet(player.getCurrentBet() + allInAmount);
            player.setTotalBetInRound(player.getTotalBetInRound() + allInAmount);
            state.setPot(state.getPot() + allInAmount);
            if (player.getCurrentBet() > state.getCurrentBet()) {
                state.setCurrentBet(player.getCurrentBet());
                state.setLastRaiser(room.getPlayers().indexOf(player));
            }
        } else {
            player.setCurrentBet(player.getCurrentBet() + allInAmount);
            player.setTotalBetInRound(player.getTotalBetInRound() + allInAmount);
            state.setPot(state.getPot() + allInAmount);
        }

        player.setChips(0L);
        player.setIsAllIn(true);
        state.markPlayerActed(player.getUserId());

        boolean roundOver = isRoundComplete(room);
        return new BetResult(true, "全下 " + allInAmount, allInAmount, state.getPot(), callAmount, roundOver);
    }

    public long getCallAmount(Player player, GameState state) {
        return Math.max(0, state.getCurrentBet() - player.getCurrentBet());
    }

    public long getMinRaise(Room room, GameState state) {
        long callAmount = state.getCurrentBet();
        if (callAmount == 0) {
            return room.getBigBlind().longValue();
        }
        return callAmount + room.getBigBlind();
    }

    public boolean isRoundComplete(Room room) {
        GameState state = room.getGameState();
        long currentBet = state.getCurrentBet();

        for (Player player : room.getPlayers()) {
            if (Boolean.TRUE.equals(player.getIsFold()) || Boolean.TRUE.equals(player.getIsAllIn())) {
                continue;
            }
            if (player.getCurrentBet() < currentBet) {
                return false;
            }
            if (!state.hasPlayerActed(player.getUserId())) {
                return false;
            }
        }
        return true;
    }

    public int getPendingPlayerCount(Room room) {
        GameState state = room.getGameState();
        int count = 0;
        for (Player player : room.getPlayers()) {
            if (!Boolean.TRUE.equals(player.getIsFold()) &&
                    !Boolean.TRUE.equals(player.getIsAllIn()) &&
                    !state.hasPlayerActed(player.getUserId())) {
                count++;
            }
        }
        return count;
    }

    public java.util.List<Player> getActivePlayers(Room room) {
        return room.getPlayers().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                .toList();
    }

    public boolean isOnlyOnePlayerLeft(Room room) {
        return getActivePlayers(room).size() == 1;
    }

    public Player getWinnerWhenOneLeft(Room room) {
        if (!isOnlyOnePlayerLeft(room)) return null;
        return getActivePlayers(room).get(0);
    }
}