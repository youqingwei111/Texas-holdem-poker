package com.poker.game.logic;

import com.poker.game.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void testShuffleCreates52Cards() {
        Deck deck = new Deck();
        deck.shuffle();

        assertEquals(52, deck.remaining());
    }

    @Test
    void testDealReducesRemaining() {
        Deck deck = new Deck();
        deck.shuffle();

        Card card1 = deck.deal();
        assertNotNull(card1);
        assertEquals(51, deck.remaining());

        Card card2 = deck.deal();
        assertNotNull(card2);
        assertEquals(50, deck.remaining());
    }

    @Test
    void testDealMultiple() {
        Deck deck = new Deck();
        deck.shuffle();

        List<Card> cards = deck.deal(5);
        assertEquals(5, cards.size());
        assertEquals(47, deck.remaining());
    }

    @Test
    void testAllCardsUnique() {
        Deck deck = new Deck();
        deck.shuffle();

        assertEquals(52, deck.remaining());

        for (int i = 0; i < 52; i++) {
            Card card = deck.deal();
            assertNotNull(card);
        }

        assertEquals(0, deck.remaining());
        assertNull(deck.deal());
    }

    @Test
    void testDeckContainsAllSuitsAndRanks() {
        Deck deck = new Deck();
        deck.shuffle();

        int[] suitCounts = new int[4];
        int[] rankCounts = new int[13];

        for (int i = 0; i < 52; i++) {
            Card card = deck.deal();
            suitCounts[card.getSuit().ordinal()]++;
            rankCounts[card.getRank().ordinal()]++;
        }

        for (int count : suitCounts) {
            assertEquals(13, count);
        }
        for (int count : rankCounts) {
            assertEquals(4, count);
        }
    }

    @Test
    void testReshuffleResetsDeck() {
        Deck deck = new Deck();
        deck.shuffle();
        deck.deal(10);

        deck.shuffle();
        assertEquals(52, deck.remaining());
    }
}