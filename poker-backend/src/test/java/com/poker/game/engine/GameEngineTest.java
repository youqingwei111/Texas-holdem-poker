package com.poker.game.engine;

import com.poker.game.enums.GamePhase;
import com.poker.game.model.Card;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private GameEngine gameEngine;
    private Room room;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine();
        room = createRoomWithPlayers(4);
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
            p.setIsFold(false);
            p.setIsAllIn(false);
            p.setIsActive(true);
            players.add(p);
        }
        room.setPlayers(players);

        return room;
    }

    @Test
    void testStartGame() {
        gameEngine.startGame(room);

        assertTrue(room.getIsPlaying());
        assertNotNull(room.getGameState());
        assertEquals(GamePhase.PRE_FLOP, room.getGameState().getPhase());
        assertEquals(4, room.getPlayers().size());

        for (Player p : room.getPlayers()) {
            assertNotNull(p.getHandCards());
            assertEquals(2, p.getHandCards().size());
        }
    }

    @Test
    void testAdvancePhasePreFlopToFlop() {
        gameEngine.startGame(room);
        room.getGameState().setPhase(GamePhase.PRE_FLOP);

        GamePhase next = gameEngine.advancePhase(room);

        assertEquals(GamePhase.FLOP, next);
        assertEquals(3, room.getGameState().getCommunityCards().size());
    }

    @Test
    void testAdvancePhaseFlopToTurn() {
        gameEngine.startGame(room);
        room.getGameState().setPhase(GamePhase.FLOP);
        room.getGameState().setCommunityCards(new ArrayList<>(List.of(
                new Card(Card.Suit.HEARTS, Card.Rank.TWO),
                new Card(Card.Suit.CLUBS, Card.Rank.THREE),
                new Card(Card.Suit.DIAMONDS, Card.Rank.FOUR)
        )));

        GamePhase next = gameEngine.advancePhase(room);

        assertEquals(GamePhase.TURN, next);
        assertEquals(4, room.getGameState().getCommunityCards().size());
    }

    @Test
    void testAdvancePhaseTurnToRiver() {
        gameEngine.startGame(room);
        room.getGameState().setPhase(GamePhase.TURN);
        room.getGameState().setCommunityCards(new ArrayList<>(List.of(
                new Card(Card.Suit.HEARTS, Card.Rank.TWO),
                new Card(Card.Suit.CLUBS, Card.Rank.THREE),
                new Card(Card.Suit.DIAMONDS, Card.Rank.FOUR),
                new Card(Card.Suit.SPADES, Card.Rank.FIVE)
        )));

        GamePhase next = gameEngine.advancePhase(room);

        assertEquals(GamePhase.RIVER, next);
        assertEquals(5, room.getGameState().getCommunityCards().size());
    }

    @Test
    void testAdvancePhaseRiverToShowdown() {
        gameEngine.startGame(room);
        room.getGameState().setPhase(GamePhase.RIVER);

        GamePhase next = gameEngine.advancePhase(room);

        assertEquals(GamePhase.SHOWDOWN, next);
    }

    @Test
    void testGetCurrentPlayer() {
        gameEngine.startGame(room);

        Player current = gameEngine.getCurrentPlayer(room);
        assertNotNull(current);
    }

    @Test
    void testIsOnlyOnePlayerLeft() {
        gameEngine.startGame(room);
        assertFalse(gameEngine.isOnlyOnePlayerLeft(room));
    }

    @Test
    void testIsOnlyOnePlayerLeftWhenOneLeft() {
        gameEngine.startGame(room);
        for (int i = 1; i < room.getPlayers().size(); i++) {
            room.getPlayers().get(i).setIsFold(true);
        }

        assertTrue(gameEngine.isOnlyOnePlayerLeft(room));
    }

    @Test
    void testResetBettingRound() {
        gameEngine.startGame(room);
        room.getPlayers().get(0).setCurrentBet(100L);
        room.getGameState().setCurrentBet(100L);

        gameEngine.nextPlayer(room);
        gameEngine.isBettingRoundOver(room);

        assertEquals(0L, room.getGameState().getCurrentBet());
    }

    @Test
    void testHasActablePlayers() {
        gameEngine.startGame(room);
        assertTrue(gameEngine.hasActablePlayers(room));
    }

    @Test
    void testHasActablePlayersFalseWhenAllFold() {
        gameEngine.startGame(room);
        for (Player p : room.getPlayers()) {
            p.setIsFold(true);
        }
        assertFalse(gameEngine.hasActablePlayers(room));
    }
}