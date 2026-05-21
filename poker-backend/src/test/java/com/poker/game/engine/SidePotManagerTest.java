package com.poker.game.engine;

import com.poker.game.model.Player;
import com.poker.game.model.Room;
import com.poker.game.model.SidePot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SidePotManagerTest {

    private SidePotManager sidePotManager;

    @BeforeEach
    void setUp() {
        sidePotManager = new SidePotManager();
    }

    private Room createRoom(int playerCount) {
        Room room = new Room();
        room.setCode("TEST");
        room.setMaxPlayers(playerCount);

        List<Player> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            Player p = new Player((long) i, "user" + i, "Player" + i, 1000L);
            p.setPosition(i);
            p.setIsFold(false);
            p.setIsAllIn(false);
            p.setTotalBetInRound(0L);
            players.add(p);
        }
        room.setPlayers(players);
        return room;
    }

    @Test
    void testNoSidePotWhenNoAllIn() {
        Room room = createRoom(3);
        for (Player p : room.getPlayers()) {
            p.setTotalBetInRound(100L);
        }

        assertFalse(sidePotManager.needsSidePots(room));
        List<SidePot> pots = sidePotManager.calculateSidePots(room);
        assertEquals(1, pots.size());
    }

    @Test
    void testSidePotWithOneAllIn() {
        Room room = createRoom(3);
        room.getPlayers().get(0).setTotalBetInRound(100L);
        room.getPlayers().get(1).setTotalBetInRound(100L);
        room.getPlayers().get(2).setTotalBetInRound(200L);
        room.getPlayers().get(2).setIsAllIn(true);

        List<SidePot> pots = sidePotManager.calculateSidePots(room);
        assertEquals(2, pots.size());
        assertEquals(200L, pots.get(0).getAmount());
        assertEquals(2, pots.get(0).getEligiblePlayerIds().size());
        assertEquals(100L, pots.get(1).getAmount());
        assertEquals(1, pots.get(1).getEligiblePlayerIds().size());
    }

    @Test
    void testSidePotWithMultipleAllIn() {
        Room room = createRoom(4);
        room.getPlayers().get(0).setTotalBetInRound(50L);
        room.getPlayers().get(1).setTotalBetInRound(50L);
        room.getPlayers().get(1).setIsAllIn(true);
        room.getPlayers().get(2).setTotalBetInRound(150L);
        room.getPlayers().get(2).setIsAllIn(true);
        room.getPlayers().get(3).setTotalBetInRound(100L);

        List<SidePot> pots = sidePotManager.calculateSidePots(room);
        assertEquals(3, pots.size());
    }

    @Test
    void testEligiblePlayersExcludesFolded() {
        Room room = createRoom(3);
        room.getPlayers().get(0).setTotalBetInRound(100L);
        room.getPlayers().get(1).setTotalBetInRound(100L);
        room.getPlayers().get(2).setTotalBetInRound(100L);
        room.getPlayers().get(0).setIsFold(true);

        List<SidePot> pots = sidePotManager.calculateSidePots(room);
        assertEquals(1, pots.size());
        assertEquals(2, pots.get(0).getEligiblePlayerIds().size());
    }

    @Test
    void testEmptyPotWhenNoBets() {
        Room room = createRoom(2);
        List<SidePot> pots = sidePotManager.calculateSidePots(room);
        assertTrue(pots.isEmpty());
    }

    @Test
    void testDescribeSidePots() {
        Room room = createRoom(2);
        room.getPlayers().get(0).setTotalBetInRound(100L);
        room.getPlayers().get(1).setTotalBetInRound(200L);
        room.getPlayers().get(1).setIsAllIn(true);

        List<SidePot> pots = sidePotManager.calculateSidePots(room);
        String desc = sidePotManager.describeSidePots(pots);
        assertFalse(desc.isEmpty());
    }
}