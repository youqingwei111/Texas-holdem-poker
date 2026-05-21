package com.poker.game.logic;

import com.poker.game.model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandEvaluatorTest {

    private HandEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new HandEvaluator();
    }

    private List<Card> communityCards(String... cards) {
        return Arrays.asList(cards).stream()
                .map(this::parseCard)
                .toList();
    }

    private Card parseCard(String cardStr) {
        String suit = switch (cardStr.charAt(cardStr.length() - 1)) {
            case 'h' -> "HEARTS";
            case 'd' -> "DIAMONDS";
            case 'c' -> "CLUBS";
            case 's' -> "SPADES";
            default -> "SPADES";
        };
        String rank = cardStr.substring(0, cardStr.length() - 1).toUpperCase();
        Card.Rank rankEnum = Card.Rank.valueOf(rank);
        Card.Suit suitEnum = Card.Suit.valueOf(suit);
        return new Card(suitEnum, rankEnum);
    }

    private HandEvaluator.HandResult evaluate(String hole1, String hole2, String... community) {
        return evaluator.evaluate(
                Arrays.asList(parseCard(hole1), parseCard(hole2)),
                communityCards(community)
        );
    }

    @Test
    void testHighCard() {
        HandEvaluator.HandResult result = evaluate("2h", "7d", "3c", "9s", "Jh", "Qc", "Kd");
        assertEquals(HandEvaluator.HandRank.HIGH_CARD, result.getRank());
    }

    @Test
    void testOnePair() {
        HandEvaluator.HandResult result = evaluate("Ah", "Ad", "2c", "3s", "9h", "Jd", "Kd");
        assertEquals(HandEvaluator.HandRank.ONE_PAIR, result.getRank());
    }

    @Test
    void testTwoPair() {
        HandEvaluator.HandResult result = evaluate("Ah", "Ad", "2c", "2s", "9h", "Jd", "Kd");
        assertEquals(HandEvaluator.HandRank.TWO_PAIR, result.getRank());
    }

    @Test
    void testThreeOfAKind() {
        HandEvaluator.HandResult result = evaluate("Ah", "Ad", "Ac", "2c", "3s", "9h", "Jd");
        assertEquals(HandEvaluator.HandRank.THREE_OF_A_KIND, result.getRank());
    }

    @Test
    void testStraight() {
        HandEvaluator.HandResult result = evaluate("Ah", "2d", "3c", "4s", "5h", "6d", "7c");
        assertEquals(HandEvaluator.HandRank.STRAIGHT, result.getRank());
    }

    @Test
    void testWheelStraight() {
        HandEvaluator.HandResult result = evaluate("Ah", "2d", "3c", "4s", "5h", "6d", "7c");
        assertEquals(HandEvaluator.HandRank.STRAIGHT, result.getRank());
        assertTrue(result.getKickers().contains(5));
    }

    @Test
    void testFlush() {
        HandEvaluator.HandResult result = evaluate("Ah", "2h", "3h", "4h", "6h", "7d", "8c");
        assertEquals(HandEvaluator.HandRank.FLUSH, result.getRank());
    }

    @Test
    void testFullHouse() {
        HandEvaluator.HandResult result = evaluate("Ah", "Ad", "Ac", "2s", "2h", "Jd", "Kd");
        assertEquals(HandEvaluator.HandRank.FULL_HOUSE, result.getRank());
    }

    @Test
    void testFourOfAKind() {
        HandEvaluator.HandResult result = evaluate("Ah", "Ad", "Ac", "As", "2h", "3d", "4c");
        assertEquals(HandEvaluator.HandRank.FOUR_OF_A_KIND, result.getRank());
    }

    @Test
    void testStraightFlush() {
        HandEvaluator.HandResult result = evaluate("5h", "6h", "7h", "8h", "9h", "2d", "3c");
        assertEquals(HandEvaluator.HandRank.STRAIGHT_FLUSH, result.getRank());
    }

    @Test
    void testRoyalFlush() {
        HandEvaluator.HandResult result = evaluate("Ah", "Kh", "Qh", "Jh", "10h", "2d", "3c");
        assertEquals(HandEvaluator.HandRank.ROYAL_FLUSH, result.getRank());
    }

    @Test
    void testCompare() {
        HandEvaluator.HandResult pair = evaluate("Ah", "Ad", "2c", "3s", "9h", "Jd", "Kd");
        HandEvaluator.HandResult twoPair = evaluate("Kh", "Kd", "2c", "2s", "9h", "Jd", "Qd");

        assertTrue(evaluator.compare(twoPair, pair) > 0);
        assertTrue(evaluator.compare(pair, twoPair) < 0);
    }

    @Test
    void testAceLowStraight() {
        HandEvaluator.HandResult result = evaluate("Ah", "2d", "3c", "4s", "5h", "6d", "Jc");
        assertEquals(HandEvaluator.HandRank.STRAIGHT, result.getRank());
        assertTrue(result.getKickers().contains(5));
    }
}