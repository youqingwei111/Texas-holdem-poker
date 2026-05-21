package com.poker.game.engine;

import com.poker.game.enums.PlayerAction;
import com.poker.game.model.Card;
import com.poker.game.model.GameState;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BettingManagerTest {

    private BettingManager bettingManager;
    private Room room;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        bettingManager = new BettingManager();
        room = createRoomWithPlayers(4);
        gameState = room.getGameState();
    }

    private Room createRoomWithPlayers(int count) {
        Room room = new Room();
        room.setCode("TEST");
        room.setSmallBlind(5);
        room.setBigBlind(10);
        room.setMaxPlayers(count);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Player p = new Player((long) i, "user" + i, "Player" + i, 1000L);
            p.setPosition(i);
            p.setCurrentBet(0L);
            p.setTotalBetInRound(0L);
            players.add(p);
        }
        room.setPlayers(players);

        GameState state = new GameState();
        state.setCurrentBet(0L);
        state.setPot(0L);
        state.setCurrentTurnIndex(0);
        state.setDealerIndex(0);
        state.setSmallBlindIndex(1);
        state.setBigBlindIndex(2);
        room.setGameState(state);

        return room;
    }

    @Test
    void testCheckWhenNoBet() {
        Player player = room.getPlayers().get(0);
        player.setCurrentBet(0L);
        gameState.setCurrentBet(0L);

        BettingManager.BetResult result = bettingManager.handleAction(room, player, PlayerAction.CHECK, null);

        assertTrue(result.isSuccess());
        assertEquals("你已过牌", result.getMessage());
    }

    @Test
    void testCheckWhenBetRequired() {
        Player player = room.getPlayers().get(0);
        player.setCurrentBet(0L);
        gameState.setCurrentBet(100L);

        BettingManager.BetResult result = bettingManager.handleAction(room, player, PlayerAction.CHECK, null);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("需要跟注"));
    }

    @Test
    void testCall() {
        Player player = room.getPlayers().get(0);
        player.setChips(500L);
        player.setCurrentBet(0L);
        gameState.setCurrentBet(100L);

        BettingManager.BetResult result = bettingManager.handleAction(room, player, PlayerAction.CALL, null);

        assertTrue(result.isSuccess());
        assertEquals(100L, player.getCurrentBet());
        assertEquals(400L, player.getChips());
        assertEquals(100L, gameState.getPot());
    }

    @Test
    void testFold() {
        Player player = room.getPlayers().get(0);
        player.setIsFold(false);

        BettingManager.BetResult result = bettingManager.handleAction(room, player, PlayerAction.FOLD, null);

        assertTrue(result.isSuccess());
        assertTrue(player.getIsFold());
    }

    @Test
    void testAllIn() {
        Player player = room.getPlayers().get(0);
        player.setChips(100L);
        player.setCurrentBet(0L);
        gameState.setCurrentBet(50L);

        BettingManager.BetResult result = bettingManager.handleAction(room, player, PlayerAction.ALL_IN, null);

        assertTrue(result.isSuccess());
        assertEquals(0L, player.getChips());
        assertTrue(player.getIsAllIn());
    }

    @Test
    void testGetCallAmount() {
        Player player = room.getPlayers().get(0);
        player.setCurrentBet(50L);
        gameState.setCurrentBet(100L);

        assertEquals(50L, bettingManager.getCallAmount(player, gameState));
    }

    @Test
    void testGetMinRaise() {
        gameState.setCurrentBet(100L);
        assertEquals(110L, bettingManager.getMinRaise(room, gameState));
    }

    @Test
    void testIsRoundComplete() {
        for (Player p : room.getPlayers()) {
            p.setCurrentBet(100L);
            gameState.markPlayerActed(p.getUserId());
        }
        gameState.setCurrentBet(100L);

        assertTrue(bettingManager.isRoundComplete(room));
    }

    @Test
    void testIsRoundNotComplete() {
        room.getPlayers().get(0).setCurrentBet(50L);
        room.getPlayers().get(1).setCurrentBet(100L);
        room.getPlayers().get(2).setCurrentBet(100L);
        room.getPlayers().get(3).setCurrentBet(100L);
        gameState.setCurrentBet(100L);

        for (Player p : room.getPlayers()) {
            gameState.markPlayerActed(p.getUserId());
        }

        assertFalse(bettingManager.isRoundComplete(room));
    }

    @Test
    void testActivePlayers() {
        room.getPlayers().get(0).setIsFold(true);
        room.getPlayers().get(1).setIsFold(false);

        List<Player> active = bettingManager.getActivePlayers(room);
        assertEquals(3, active.size());
    }
}