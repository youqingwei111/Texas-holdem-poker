package com.poker.game.logic;

import com.poker.game.engine.GameEngine;
import com.poker.game.engine.SidePotManager;
import com.poker.game.model.Card;
import com.poker.game.model.Player;
import com.poker.game.model.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokerLogicTest {

    private PokerLogic pokerLogic;
    private HandEvaluator handEvaluator;
    private Room room;

    @BeforeEach
    void setUp() {
        handEvaluator = new HandEvaluator();
        SidePotManager sidePotManager = new SidePotManager();
        pokerLogic = new PokerLogic(sidePotManager);
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
            p.setIsFold(false);
            p.setIsAllIn(false);
            players.add(p);
        }
        room.setPlayers(players);
        return room;
    }

    @Test
    void testDetermineWinners() {
        Player p1 = room.getPlayers().get(0);
        Player p2 = room.getPlayers().get(1);
        Player p3 = room.getPlayers().get(2);

        p1.setHandCards(createCards("Ah", "Ad"));
        p2.setHandCards(createCards("Kh", "Kd"));
        p3.setHandCards(createCards("Qh", "Qd"));

        List<Card> community = createCommunityCards("2h", "3c", "4s", "5d", "6h");

        List<Player> winners = pokerLogic.determineWinners(room, community);

        assertEquals(1, winners.size());
        assertEquals(p1.getUserId(), winners.get(0).getUserId());
    }

    @Test
    void testDetermineWinnersWithTie() {
        Player p1 = room.getPlayers().get(0);
        Player p2 = room.getPlayers().get(1);

        p1.setHandCards(createCards("Ah", "Ad"));
        p2.setHandCards(createCards("Ah", "Ad"));

        List<Card> community = createCommunityCards("2h", "3c", "4s", "5d", "6h");

        List<Player> winners = pokerLogic.determineWinners(room, community);

        assertEquals(2, winners.size());
    }

    @Test
    void testSettlePotNoAllIn() {
        Player p1 = room.getPlayers().get(0);
        Player p2 = room.getPlayers().get(1);

        p1.setHandCards(createCards("Ah", "Ad"));
        p2.setHandCards(createCards("Kh", "Kd"));
        p1.setChips(1000L);
        p2.setChips(1000L);

        room.getGameState().setPot(200L);
        room.getGameState().setCurrentBet(0L);

        List<Card> community = createCommunityCards("2h", "3c", "4s", "5d", "6h");

        var winnings = pokerLogic.settlePot(room, community);

        assertTrue(winnings.containsKey(p1));
        assertEquals(200L, winnings.get(p1));
    }

    @Test
    void testEvaluatePlayer() {
        Player player = room.getPlayers().get(0);
        player.setHandCards(createCards("Ah", "Kh", "Qh", "Jh", "10h"));

        var result = pokerLogic.evaluatePlayer(player, new ArrayList<>());

        assertEquals(HandEvaluator.HandRank.ROYAL_FLUSH, result.getRank());
    }

    @Test
    void testComparePlayers() {
        Player p1 = room.getPlayers().get(0);
        Player p2 = room.getPlayers().get(1);

        p1.setHandCards(createCards("Ah", "Ad"));
        p2.setHandCards(createCards("Kh", "Kd"));

        List<Card> community = createCommunityCards("2h", "3c", "4s", "5d", "6h");

        int result = pokerLogic.comparePlayers(p1, p2, community);

        assertTrue(result > 0);
    }

    private List<Card> createCards(String... cardStrs) {
        List<Card> cards = new ArrayList<>();
        for (String cardStr : cardStrs) {
            cards.add(parseCard(cardStr));
        }
        return cards;
    }

    private List<Card> createCommunityCards(String... cardStrs) {
        return createCards(cardStrs);
    }

    private Card parseCard(String cardStr) {
        String suitStr = cardStr.substring(cardStr.length() - 1);
        Card.Suit suit = switch (suitStr) {
            case "h" -> Card.Suit.HEARTS;
            case "d" -> Card.Suit.DIAMONDS;
            case "c" -> Card.Suit.CLUBS;
            case "s" -> Card.Suit.SPADES;
            default -> Card.Suit.SPADES;
        };
        String rankStr = cardStr.substring(0, cardStr.length() - 1);
        Card.Rank rank = Card.Rank.valueOf(rankStr.toUpperCase());
        return new Card(suit, rank);
    }
}