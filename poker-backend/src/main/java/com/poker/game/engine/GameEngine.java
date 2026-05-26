package com.poker.game.engine;

import com.poker.game.enums.GamePhase;
import com.poker.game.enums.PlayerAction;
import com.poker.game.logic.Deck;
import com.poker.game.logic.HandEvaluator;
import com.poker.game.model.Card;
import com.poker.game.model.GameState;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import com.poker.service.GameRecordService;
import com.poker.service.RoomService;
import com.poker.websocket.dispatcher.MessageDispatcher;
import com.poker.websocket.message.MessageType;
import com.poker.websocket.message.WsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 游戏引擎 - 状态机核心
 */
@Slf4j
@Component
public class GameEngine {

    private final RoomService roomService;
    private final MessageDispatcher messageDispatcher;
    private final BettingManager bettingManager;
    private final GameRecordService gameRecordService;

    public GameEngine(RoomService roomService, MessageDispatcher messageDispatcher, BettingManager bettingManager, GameRecordService gameRecordService) {
        this.roomService = roomService;
        this.messageDispatcher = messageDispatcher;
        this.bettingManager = bettingManager;
        this.gameRecordService = gameRecordService;
    }

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
            player.setTotalInvestedInHand(0L);
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
        smallBlindPlayer.setTotalInvestedInHand(sb);

        bigBlindPlayer.setChips(bigBlindPlayer.getChips() - bb);
        bigBlindPlayer.setCurrentBet(bb);
        bigBlindPlayer.setTotalBetInRound(bb);
        bigBlindPlayer.setTotalInvestedInHand(bb);

        state.setPot(sb + bb);
        state.setCurrentBet(bb);

        if (room.getPlayerCount() == 2) {
            // 两人局：庄家是小盲注，第一个行动（PRE_FLOP）
            state.setCurrentTurnIndex(state.getSmallBlindIndex());
            state.setLastRaiseAmount(bb - sb);
            state.markPlayerActed(bigBlindPlayer.getUserId());
        } else {
            state.setCurrentTurnIndex((state.getBigBlindIndex() + 1) % room.getPlayerCount());
            state.setLastRaiseAmount((long) room.getBigBlind());
            state.markPlayerActed(bigBlindPlayer.getUserId());
            state.markPlayerActed(smallBlindPlayer.getUserId());
        }

        room.setGameState(state);
        room.setIsPlaying(true);

        // 给每个玩家单独发送私牌（仅自己可见）
        for (Player p : room.getPlayers()) {
            List<String> cardStrings = p.getHandCards().stream()
                    .map(Card::getDisplayName)
                    .collect(java.util.stream.Collectors.toList());
            messageDispatcher.sendToUser(p.getUserId(), WsMessage.of(MessageType.DEAL_CARDS, Map.of(
                    "cards", cardStrings
            )));
            log.info("[Engine] 向玩家 {} 发送私牌: {}", p.getUserId(), cardStrings);
        }

        log.info("[Engine] 游戏开始 | dealer={} | smallBlindIdx={} | bigBlindIdx={} | currentTurn={} | pot={}",
                dealerPos, state.getSmallBlindIndex(), state.getBigBlindIndex(), state.getCurrentTurnIndex(), state.getPot());
    }

    public GamePhase nextPhase(Room room) {
        return advancePhase(room);
    }

    /**
     * 推进游戏阶段（PRE_FLOP → FLOP → TURN → RIVER → SHOWDOWN）
     * 包含：发公共牌、重置下注、设置新一轮起始玩家
     */
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

        // 重置下注状态（currentBet=0，currentBet清零，actedPlayers清空）
        // 但注意：翻牌后 lastRaiseAmount 不重置，因为它影响 minRaise 计算
        resetBettingRoundNoLastRaise(room);
        // 设置新一轮首个行动玩家（庄家下家，跳过弃牌/全下）
        advanceDealerAndSetFirstPlayer(room);

        log.info("[Engine] 阶段推进: {} → {}, 庄家={}, 当前行动={}",
                currentPhase, state.getPhase(),
                state.getDealerIndex(), state.getCurrentTurnIndex());

        return state.getPhase();
    }

    /**
     * 将庄家推进一位，并设置首个可行动玩家
     * 德州扑克规则：每轮结束后庄家按钮推进一位，新一轮从庄家下家开始
     */
    private void advanceDealerAndSetFirstPlayer(Room room) {
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
        // 所有玩家都无法行动
        state.setCurrentTurnIndex(startIdx);
        log.warn("[Engine] 无可行动玩家，设置 currentTurnIndex={}", startIdx);
    }

    public boolean canAdvancePhase(Room room) {
        return isBettingRoundOver(room);
    }

    /**
     * 判断下注轮是否结束
     * 条件：所有存活玩家（未弃牌、未全下、有筹码）的下注额都等于 currentBet
     */
    public boolean isBettingRoundOver(Room room) {
        GameState state = room.getGameState();
        long currentBet = state.getCurrentBet();

        for (Player player : room.getPlayers()) {
            if (Boolean.TRUE.equals(player.getIsFold())) {
                continue;
            }
            if (Boolean.TRUE.equals(player.getIsAllIn())) {
                continue;
            }
            if (player.getChips() != null && player.getChips() <= 0) {
                continue;
            }
            if (!player.getCurrentBet().equals(currentBet)) {
                log.debug("[Engine] 轮未结束：玩家 {} 的 bet={} != 当前下注 {}",
                        player.getUsername(), player.getCurrentBet(), currentBet);
                return false;
            }
        }

        log.info("[Engine] ✅ 下注轮结束，currentBet={}", currentBet);
        return true;
    }

    /**
     * 重置下注轮（保留 lastRaiseAmount，用于翻牌后的 minRaise 计算）
     */
    private void resetBettingRoundNoLastRaise(Room room) {
        GameState state = room.getGameState();

        state.setCurrentBet(0L);
        state.resetActedPlayers();
        // 注意：不重置 lastRaiseAmount！翻牌后需要用它计算 minRaise

        for (Player player : room.getPlayers()) {
            player.setCurrentBet(0L);
        }

        log.info("[Engine] 重置下注轮（保留 lastRaiseAmount={}）", state.getLastRaiseAmount());
    }

    private void resetBettingRound(Room room) {
        GameState state = room.getGameState();

        state.setCurrentBet(0L);
        state.resetActedPlayers();
        state.setLastRaiseAmount(null);

        for (Player player : room.getPlayers()) {
            player.setCurrentBet(0L);
        }

        log.info("[Engine] 重置下注轮");
    }

    public Player getCurrentPlayer(Room room) {
        GameState state = room.getGameState();
        int idx = state.getCurrentTurnIndex();
        if (idx < 0 || idx >= room.getPlayers().size()) {
            return null;
        }
        return room.getPlayers().get(idx);
    }

    /**
     * 环形查找下一个需要行动的玩家
     * 核心逻辑：pBet < currentBet → 必须行动
     */
    public int findNextPlayerIndex(Room room) {
        GameState state = room.getGameState();
        List<Player> players = room.getPlayers();
        int size = players.size();
        int current = state.getCurrentTurnIndex() != null ? state.getCurrentTurnIndex() : 0;
        long currentBet = state.getCurrentBet() != null ? state.getCurrentBet() : 0L;

        log.info("[Engine] findNextPlayerIndex | size={} | current={} | currentBet={}", size, current, currentBet);

        for (int i = 1; i < size; i++) {
            int checkIndex = (current + i) % size;
            Player p = players.get(checkIndex);

            boolean isFold = Boolean.TRUE.equals(p.getIsFold());
            boolean isAllIn = Boolean.TRUE.equals(p.getIsAllIn());
            long chips = p.getChips() != null ? p.getChips() : 0L;
            long pBet = p.getCurrentBet() != null ? p.getCurrentBet() : 0L;
            boolean hasActed = state.hasPlayerActed(p.getUserId());

            log.info("[Engine] 检查 i={} → idx={} | {} | fold={} | allIn={} | chips={} | pBet={} | currentBet={} | hasActed={}",
                    i, checkIndex, p.getUsername(), isFold, isAllIn, chips, pBet, currentBet, hasActed);

            if (isFold) {
                log.debug("[Engine] 跳过弃牌: {}", p.getUsername());
                continue;
            }
            if (isAllIn || chips <= 0) {
                log.debug("[Engine] 跳过全下/无筹码: {} (chips={})", p.getUsername(), chips);
                continue;
            }

            // 核心判断：pBet < currentBet → 还没跟注，必须行动
            // 或：hasPlayerActed == false → 本轮尚未行动，也需要继续
            if (pBet < currentBet || !hasActed) {
                log.info("[Engine] ✅ 找到需要行动的下一个玩家: idx={} username={} pBet={} currentBet={} hasActed={}",
                        checkIndex, p.getUsername(), pBet, currentBet, hasActed);
                return checkIndex;
            } else {
                log.debug("[Engine] 跳过已跟注且已行动玩家: {} (pBet={} >= currentBet={}, hasActed={})",
                        p.getUsername(), pBet, currentBet, hasActed);
            }
        }

        log.warn("[Engine] ❌ 未找到需要行动的玩家，所有人已跟注或弃牌");
        return -1;
    }

    public boolean isOnlyOnePlayerLeft(Room room) {
        long count = room.getPlayers().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                .filter(p -> p.getChips() != null && p.getChips() > 0)
                .count();
        return count == 1;
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

    /**
     * 处理玩家动作 - 状态机核心方法
     * 参考 GameService.doHandlePlayerAction 的决策树
     */
    public ActionResult processAction(Room room, Long userId, PlayerAction action, Long raiseAmount) {
        GameState state = room.getGameState();
        Player player = room.getPlayer(userId);

        log.info("[Engine] 处理动作 | userId={} action={} amount={} | phase={} currentTurn={} currentBet={}",
                userId, action, raiseAmount, state.getPhase(), state.getCurrentTurnIndex(), state.getCurrentBet());

        if (player == null) {
            return new ActionResult(false, "玩家不在房间中", null, false, false, null);
        }

        // 验证是否是轮到该玩家
        Player currentPlayer = getCurrentPlayer(room);
        Long currentPlayerUserId = currentPlayer != null ? currentPlayer.getUserId() : null;
        if (currentPlayer == null || !currentPlayerUserId.equals(userId)) {
            log.warn("[Engine] 玩家 {} 无法行动，当前行动玩家是 {}", userId, currentPlayerUserId);
            // 非当前玩家尝试操作时，重发自己的YOUR_TURN给真正该行动的玩家
            if (currentPlayer != null) {
                sendYourTurn(currentPlayer, state, room, state.getPhase().name());
            }
            return new ActionResult(false, "当前不是你的回合", null, false, false, null);
        }

        // 执行动作
        BettingManager.BetResult betResult = bettingManager.handleAction(room, player, action, raiseAmount);

        if (!betResult.isSuccess()) {
            log.warn("[Engine] 动作执行失败: {}", betResult.getMessage());
            // 动作失败时，重新发送 YOUR_TURN 给该玩家，刷新前端面板
            sendYourTurn(player, state, room, state.getPhase().name());
            return new ActionResult(false, betResult.getMessage(), null, false, false, null);
        }

        log.info("[Engine] 动作成功 | userId={} msg={} | pot={} | roundOver={}",
                userId, betResult.getMessage(), betResult.getPot(), betResult.getRoundOver());

        // 持久化 + 广播动作
        roomService.saveRoom(room);
        messageDispatcher.broadcastToRoom(room.getCode(), WsMessage.of(MessageType.PLAYER_ACTION, Map.of(
                "userId", userId,
                "action", action.name(),
                "amount", betResult.getAmount(),
                "pot", betResult.getPot(),
                "chips", player.getChips() != null ? player.getChips() : 0
        )));

        // ══════════════════════════════════════════════════════
        // 参考项目的决策树：
        // ① 只有1个可行动玩家 → 提前结算
        // ② 下注轮结束 → advancePhase
        // ③ 还有可行动玩家 → nextPlayer + notifyPlayerTurn
        // ④ 无人能行动但未平衡 → 强制 advancePhase
        // ══════════════════════════════════════════════════════

        // ① 只有1个可行动玩家 → 提前结算（不进入showdown，直接获胜）
        if (bettingManager.isOnlyOnePlayerLeft(room)) {
            Player winner = bettingManager.getWinnerWhenOneLeft(room);
            String winnerName = winner != null ? winner.getUsername() : "unknown";
            Long winnerId = winner != null ? winner.getUserId() : null;
            log.info("[Engine] 只有1个可行动玩家 {}，直接获胜", winnerName);

            // 将 pot 中的筹码全部分给该 winner
            if (winner != null) {
                winner.setChips(winner.getChips() + state.getPot());
                log.info("[Engine] 玩家 {} 获得底池 {} 筹码（弃牌获胜）", winnerName, state.getPot());
            }

            // 构建全量玩家筹码（弃牌获胜后）
        List<Map<String, Object>> allPlayersChips = new java.util.ArrayList<>();
        for (Player p : room.getPlayers()) {
            allPlayersChips.add(new java.util.LinkedHashMap<>(Map.of(
                    "userId", p.getUserId() != null ? p.getUserId() : 0,
                    "username", p.getUsername() != null ? p.getUsername() : "",
                    "nickname", p.getNickname() != null ? p.getNickname() : "",
                    "chips", p.getChips() != null ? p.getChips() : 0,
                    "isFold", Boolean.TRUE.equals(p.getIsFold()),
                    "isWinner", winner != null && winner.equals(p)
            )));
        }

        // 广播提前获胜消息
        messageDispatcher.broadcastToRoom(room.getCode(), WsMessage.of(MessageType.ROUND_RESULT, Map.of(
                "winnerId", winnerId != null ? winnerId : 0,
                "winnerName", winnerName,
                "winAmount", state.getPot(),
                "reason", "all_other_players_folded",
                "allPlayersChips", allPlayersChips
        )));

            // 保存游戏记录（对手全弃牌，不战而胜）
            if (winner != null) {
                try {
                    gameRecordService.saveRecord(room.getCode(), state.getPot(), room.getPlayers(),
                            java.util.List.of(winner), "OPPONENTS_FOLDED");
                } catch (Exception e) {
                    log.error("[Engine] 保存游戏记录失败（不影响广播）: {}", e.getMessage(), e);
                }
            }

            // 重置游戏状态为 WAITING，以便房间内玩家可以再次开局
            resetGameToWaiting(room);
            return new ActionResult(true, "获胜（对手弃牌）", winnerId, true, true, null);
        }

        // ② 本轮下注结束 → 进入下一阶段
        if (betResult.getRoundOver() || bettingManager.isRoundComplete(room)) {
            log.info("[Engine] 下注轮结束，进入下一阶段");
            GamePhase newPhase = advancePhase(room);
            log.info("[Engine] 阶段推进完成: {}", newPhase);

            broadcastGameState(room);
            roomService.saveRoom(room);

            if (newPhase == GamePhase.SHOWDOWN) {
                log.info("[Engine] 进入 SHOWDOWN，执行比牌结算");
                doShowdown(room);
                return new ActionResult(true, "比牌结算完成", null, true, true, null);
            }

            // Bug 2 Fix: 检查是否需要"极速发牌"（All-In后的连续发牌）
            // 如果还能继续下注的人 < 2，且还没到 SHOWDOWN，则继续推进阶段（跑马）
            long ableToBetCount = room.getPlayers().stream()
                    .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                    .filter(p -> !Boolean.TRUE.equals(p.getIsAllIn()))
                    .filter(p -> p.getChips() != null && p.getChips() > 0)
                    .count();

            if (ableToBetCount < 2 && newPhase != GamePhase.SHOWDOWN) {
                log.info("[Engine] 只有 {} 人可以继续下注（< 2），极速发牌到结算", ableToBetCount);
                fastForwardToShowdown(room);
                return new ActionResult(true, "极速发牌完成", null, true, true, null);
            }

            // 向新阶段首个行动玩家发 YOUR_TURN
            int nextIdx = state.getCurrentTurnIndex();
            if (nextIdx >= 0) {
                Player next = room.getPlayers().get(nextIdx);
                log.info("[Engine] 新阶段首个行动玩家: {} (index={})", next.getUsername(), nextIdx);
                sendYourTurn(next, state, room, newPhase.name());
            }

            return new ActionResult(true, "阶段推进到" + newPhase.name(), null, true, false, state.getCurrentTurnIndex());
        }

        // ③ 还有可行动玩家 → 轮到下一个人
        int nextIdx = findNextPlayerIndex(room);

        if (nextIdx < 0) {
            // ④ 没人能行动但下注未平衡 → 强制 advancePhase
            log.warn("[Engine] 无可行动玩家但下注未平衡，强制进入下一阶段");
            GamePhase newPhase = advancePhase(room);
            broadcastGameState(room);
            roomService.saveRoom(room);

            if (newPhase != GamePhase.SHOWDOWN) {
                int firstIdx = state.getCurrentTurnIndex();
                if (firstIdx >= 0) {
                    Player first = room.getPlayers().get(firstIdx);
                    sendYourTurn(first, state, room, newPhase.name());
                }
            }
            return new ActionResult(true, "强制推进阶段", null, true, false, null);
        }

        // 设置新的 currentTurnIndex，向玩家发 YOUR_TURN
        state.setCurrentTurnIndex(nextIdx);
        Player next = room.getPlayers().get(nextIdx);
        log.info("[Engine] 下一个行动玩家: {} (index={})", next.getUsername(), nextIdx);

        broadcastGameState(room);
        roomService.saveRoom(room);

        sendYourTurn(next, state, room, state.getPhase().name());

        return new ActionResult(true, betResult.getMessage(), null, false, false, nextIdx);
    }

    /**
     * 比牌结算（Showdown）
     * 流程：
     * 1. 收集所有未弃牌玩家
     * 2. 评估每个玩家的最佳手牌（2张底牌 + 5张公共牌）
     * 3. 找出最大牌型（处理平局）
     * 4. 分配底池筹码给赢家
     * 5. 广播结算结果（SHOWDOWN_RESULT）
     * 6. 重置游戏状态
     */
    private void doShowdown(Room room) {
        GameState state = room.getGameState();
        List<Card> communityCards = state.getCommunityCards();
        long totalPot = state.getPot();

        log.info("[Engine] ========== SHOWDOWN 结算开始 ==========");
        log.info("[Engine] 底池: {} | 公共牌: {}", totalPot, communityCards);

        // 1. 收集未弃牌玩家
        List<Player> activePlayers = room.getPlayers().stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFold()))
                .toList();

        if (activePlayers.isEmpty()) {
            log.warn("[Engine] 没有存活玩家，底池 {} 无人认领", totalPot);
            return;
        }

        log.info("[Engine] 存活玩家数: {}", activePlayers.size());

        // 2. 评估每个玩家手牌
        HandEvaluator evaluator = new HandEvaluator();
        Map<Player, HandEvaluator.HandResult> playerHands = new java.util.LinkedHashMap<>();

        for (Player p : activePlayers) {
            List<Card> holeCards = p.getHandCards();
            HandEvaluator.HandResult result = evaluator.evaluate(holeCards, communityCards);
            playerHands.put(p, result);
            log.info("[Engine] 玩家 {} 的手牌: {} ({})", p.getUsername(), result.getRank().getName(), result);
        }

        // 3. 找出最大牌型
        HandEvaluator.HandResult bestHand = playerHands.values().stream()
                .max(evaluator::compare)
                .orElse(null);

        if (bestHand == null) {
            log.error("[Engine] 无法确定最佳手牌");
            return;
        }

        // 4. 找出所有达到最佳牌型的赢家（处理平局）
        List<Player> winners = new java.util.ArrayList<>();
        for (Map.Entry<Player, HandEvaluator.HandResult> entry : playerHands.entrySet()) {
            if (evaluator.compare(entry.getValue(), bestHand) == 0) {
                winners.add(entry.getKey());
            }
        }

        log.info("[Engine] 赢家数量: {} | 牌型: {}", winners.size(), bestHand.getRank().getName());

        // ══════════════════════════════════════════════════════════════════════
        // 边池结算 / 退还未跟注筹码（Uncalled Bet）
        //
        // 数学规则：
        // - 每人最多只能赢自己投入的那份（totalInvestedInHand）
        // - 主池 = minInvested × 存活人数（所有玩家可公平竞争的部分）
        // - 未跟注筹码 = 总底池 - 主池（立即退还给出资最多的玩家）
        //
        // 示例：A(100) vs B(200)，pot=300
        //   minInvested=100，mainPotSize=200，uncalledBetTotal=100
        //   B 先拿回未跟注的 100
        //   主池 200 再按胜负分配
        //   A赢：A拿200，B拿100（退还）
        //   B赢：B拿200+100=300，A拿0
        //   平局：各拿100+50=150，B拿100+50=150
        // ══════════════════════════════════════════════════════════════════════

        // 计算主池（所有玩家投入的最小值 × 存活人数）
        long minInvested = activePlayers.stream()
                .mapToLong(p -> p.getTotalInvestedInHand() != null ? p.getTotalInvestedInHand() : 0L)
                .min()
                .orElse(0L);

        long mainPotSize = minInvested * activePlayers.size();
        long uncalledBetTotal = totalPot - mainPotSize; // 未跟注的筹码

        // 第一步：立即将未跟注筹码退还给出资最多的玩家（大筹码方）
        // 这一步在比牌之前完成，确保筹码不凭空消失
        if (uncalledBetTotal > 0) {
            Player bigSpender = activePlayers.stream()
                    .max((a, b) -> Long.compare(
                            a.getTotalInvestedInHand() != null ? a.getTotalInvestedInHand() : 0L,
                            b.getTotalInvestedInHand() != null ? b.getTotalInvestedInHand() : 0L))
                    .orElse(null);
            if (bigSpender != null) {
                bigSpender.setChips(bigSpender.getChips() + uncalledBetTotal);
                log.info("[Engine] 退还 {} 筹码给 {}（未跟注部分，Big Spender）",
                        uncalledBetTotal, bigSpender.getUsername());
            }
        }

        // 第二步：主池按胜负分配（主池 = 最小投入 × 人数）
        // 赢家拿走全部主池（或平分）
        long winAmount = mainPotSize / winners.size();
        for (Player winner : winners) {
            winner.setChips(winner.getChips() + winAmount);
            log.info("[Engine] 玩家 {} 获得主池 {} 筹码（牌型: {}）",
                    winner.getUsername(), winAmount, bestHand.getRank().getName());
        }

        // 验证：winner总所得 + bigSpender退还 = totalPot（数学上必须成立）
        long totalDistributed = winners.stream()
                .mapToLong(w -> winAmount)
                .sum() + (uncalledBetTotal > 0 ? uncalledBetTotal : 0);
        log.info("[Engine] 分配验证: 主池={} + 退还={} = {}（应等于 pot={}）",
                mainPotSize, uncalledBetTotal, totalDistributed, totalPot);

        // 6. 广播结算结果
        List<Map<String, Object>> showdownPlayers = new java.util.ArrayList<>();
        for (Player p : room.getPlayers()) {
            Map<String, Object> playerInfo = new java.util.LinkedHashMap<>();
            playerInfo.put("userId", p.getUserId());
            playerInfo.put("username", p.getUsername());
            playerInfo.put("nickname", p.getNickname() != null ? p.getNickname() : p.getUsername());

            HandEvaluator.HandResult hand = playerHands.get(p);
            if (hand != null) {
                playerInfo.put("handRank", hand.getRank().getName());
            } else {
                playerInfo.put("handRank", p.getIsFold() ? "已弃牌" : "未知");
            }

            playerInfo.put("handCards", p.getHandCards().stream()
                    .map(Card::getDisplayName)
                    .toList());

            playerInfo.put("chips", p.getChips() != null ? p.getChips() : 0);

            if (winners.contains(p)) {
                playerInfo.put("winAmount", winAmount);
            } else {
                playerInfo.put("winAmount", 0);
            }

            showdownPlayers.add(playerInfo);
        }

        // 构建全量玩家筹码（结算后）
        List<Map<String, Object>> allPlayersChips = new java.util.ArrayList<>();
        for (Player p : room.getPlayers()) {
            allPlayersChips.add(new java.util.LinkedHashMap<>(Map.of(
                    "userId", p.getUserId() != null ? p.getUserId() : 0,
                    "username", p.getUsername() != null ? p.getUsername() : "",
                    "nickname", p.getNickname() != null ? p.getNickname() : "",
                    "chips", p.getChips() != null ? p.getChips() : 0,
                    "isFold", Boolean.TRUE.equals(p.getIsFold()),
                    "isWinner", winners.contains(p)
            )));
        }

        boolean isSplit = winners.size() > 1;
        messageDispatcher.broadcastToRoom(room.getCode(), WsMessage.of(MessageType.SHOWDOWN_RESULT, Map.of(
                "players", showdownPlayers,
                "allPlayersChips", allPlayersChips,
                "communityCards", communityCards.stream().map(Card::getDisplayName).toList(),
                "pot", totalPot,
                "winAmount", winAmount,
                "isSplit", isSplit,
                "winners", winners.stream().map(p -> Map.of(
                        "userId", p.getUserId(),
                        "nickname", p.getNickname() != null ? p.getNickname() : p.getUsername(),
                        "handRank", bestHand.getRank().getName(),
                        "winAmount", winAmount
                )).toList()
        )));

        // 7. 重置游戏状态
        state.setPot(0L);
        state.setPhase(GamePhase.WAITING);
        state.getCommunityCards().clear();
        state.resetActedPlayers();
        state.setCurrentBet(0L);
        state.setLastRaiseAmount(null);

        for (Player p : room.getPlayers()) {
            p.setCurrentBet(0L);
            p.setTotalBetInRound(0L);
            p.setIsFold(false);
            p.setIsAllIn(false);
            p.setIsActive(true);
            p.getHandCards().clear();
        }

        room.setIsPlaying(false);
        roomService.saveRoom(room);

        // 保存游戏记录到数据库（所有玩家，赢家列表，牌型）
        try {
            gameRecordService.saveRecord(room.getCode(), totalPot, room.getPlayers(), winners, bestHand.getRank().getName());
        } catch (Exception e) {
            log.error("[Engine] 保存游戏记录失败（不影响广播）: {}", e.getMessage(), e);
        }

        log.info("[Engine] ========== SHOWDOWN 结算完成 ==========");
    }

    /**
     * 极速发牌：当少于2人可以继续下注时，自动连续推进所有阶段直到结算
     * 用于 All-In 后的自动跑马
     */
    private void fastForwardToShowdown(Room room) {
        GameState state = room.getGameState();
        log.info("[Engine] ========== 极速发牌开始 ==========");

        while (state.getPhase() != GamePhase.SHOWDOWN && state.getPhase() != GamePhase.WAITING) {
            GamePhase before = state.getPhase();
            GamePhase newPhase = advancePhase(room);
            log.info("[Engine] 极速: {} → {}", before, newPhase);

            broadcastGameState(room);
            roomService.saveRoom(room);

            if (newPhase == GamePhase.SHOWDOWN) {
                log.info("[Engine] 极速发牌到达 SHOWDOWN，执行结算");
                doShowdown(room);
                return;
            }
        }

        log.info("[Engine] ========== 极速发牌完成 ==========");
    }

    private void resetGameToWaiting(Room room) {
        GameState state = room.getGameState();

        state.setPot(0L);
        state.setPhase(GamePhase.WAITING);
        state.getCommunityCards().clear();
        state.resetActedPlayers();
        state.setCurrentBet(0L);
        state.setLastRaiseAmount(null);

        for (Player p : room.getPlayers()) {
            p.setCurrentBet(0L);
            p.setTotalBetInRound(0L);
            p.setIsFold(false);
            p.setIsAllIn(false);
            p.setIsActive(true);
            p.getHandCards().clear();
        }

        room.setIsPlaying(false);
        roomService.saveRoom(room);

        log.info("[Engine] 游戏状态重置为 WAITING");
    }

    /**
     * 向玩家发送 YOUR_TURN 消息
     * 严格按照德州扑克规则计算可用动作：
     * - FOLD：始终可用
     * - ALL_IN：只要有筹码就可用
     * - CHECK：仅当 callAmount == 0（无人加注，或已平齐）时可用
     * - CALL：仅当 callAmount > 0 且筹码足够跟注时可用
     * - RAISE：仅当（无人加注 OR 跟注后仍有筹码可加注）且筹码足够最小加注时可用
     */
    private void sendYourTurn(Player player, GameState state, Room room, String phase) {
        long callAmt = bettingManager.getCallAmount(player, state);
        long minRaise = bettingManager.getMinRaise(room, state);
        long playerChips = player.getChips() != null ? player.getChips() : 0L;

        java.util.List<String> availableActions = new java.util.ArrayList<>();

        // FOLD：始终可用
        availableActions.add("FOLD");

        // CHECK：仅当 callAmount == 0（无人加注，或已平齐）时可用
        if (callAmt == 0) {
            availableActions.add("CHECK");
        }

        // CALL：仅当 callAmount > 0 且筹码足够时可用
        if (callAmt > 0 && playerChips >= callAmt) {
            availableActions.add("CALL");
        }

        // RAISE：仅当满足以下条件时可用
        // 条件1：无人加注（callAmt == 0）→ 可以主动加注
        // 条件2：跟注后仍有筹码（playerChips > callAmt）→ 可以再加注
        // 条件3：筹码足够支付最小加注额
        boolean canRaise = false;
        if (callAmt == 0) {
            // 无人加注时：只要筹码够最小加注就可以raise
            canRaise = (playerChips >= minRaise);
        } else {
            // 有人加注时：跟注后还有剩余筹码才能raise
            canRaise = (playerChips > callAmt) && ((playerChips - callAmt) >= minRaise);
        }
        if (canRaise) {
            availableActions.add("RAISE");
        }

        // ALL_IN：只要有筹码就可用
        if (playerChips > 0) {
            availableActions.add("ALL_IN");
        }

        messageDispatcher.sendToUser(player.getUserId(), WsMessage.of(MessageType.YOUR_TURN, Map.of(
                "userId", player.getUserId(),
                "currentTurnIndex", state.getCurrentTurnIndex(),
                "availableActions", availableActions,
                "callAmount", callAmt,
                "minRaise", minRaise,
                "phase", phase
        )));
        log.info("[Engine] 向玩家 {} 发送 YOUR_TURN (callAmount={}, minRaise={}, chips={}, actions={})",
                player.getUserId(), callAmt, minRaise, playerChips, availableActions);
    }

    public void broadcastGameState(Room room) {
        GameState state = room.getGameState();
        messageDispatcher.broadcastToRoom(room.getCode(), WsMessage.of(MessageType.GAME_STATE, Map.of(
                "roomCode", room.getCode(),
                "phase", state.getPhase().name(),
                "pot", state.getPot(),
                "currentBet", state.getCurrentBet(),
                "dealerIndex", state.getDealerIndex(),
                "currentTurnIndex", state.getCurrentTurnIndex(),
                "communityCards", state.getCommunityCards().stream()
                        .map(Card::toString)
                        .toList(),
                "players", room.getPlayers().stream().map(p -> Map.of(
                        "userId", p.getUserId(),
                        "username", p.getUsername(),
                        "nickname", p.getNickname() != null ? p.getNickname() : p.getUsername(),
                        "chips", p.getChips(),
                        "currentBet", p.getCurrentBet(),
                        "isFold", Boolean.TRUE.equals(p.getIsFold()),
                        "isAllIn", Boolean.TRUE.equals(p.getIsAllIn()),
                        "position", p.getPosition()
                )).toList()
        )));
    }

    private void dealFromDeck(List<Card> deck, List<Card> communityCards, int count) {
        for (int i = 0; i < count && !deck.isEmpty(); i++) {
            communityCards.add(deck.remove(deck.size() - 1));
        }
    }

    public record ActionResult(
            boolean success,
            String message,
            Long winnerId,
            boolean roundOver,
            boolean gameOver,
            Integer nextPlayerIndex
    ) {}
}