package com.poker.game.logic;

import com.poker.game.model.Card;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 手牌评估器 - 德州扑克比牌算法
 */
public class HandEvaluator {

    public enum HandRank {
        ROYAL_FLUSH(10, "皇家同花顺"),
        STRAIGHT_FLUSH(9, "同花顺"),
        FOUR_OF_A_KIND(8, "四条"),
        FULL_HOUSE(7, "葫芦"),
        FLUSH(6, "同花"),
        STRAIGHT(5, "顺子"),
        THREE_OF_A_KIND(4, "三条"),
        TWO_PAIR(3, "两对"),
        ONE_PAIR(2, "一对"),
        HIGH_CARD(1, "高牌");

        private final int value;
        private final String name;

        HandRank(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

    public static class HandResult {
        private final HandRank rank;
        private final List<Integer> kickers;

        public HandResult(HandRank rank, List<Integer> kickers) {
            this.rank = rank;
            this.kickers = kickers;
        }

        public HandRank getRank() {
            return rank;
        }

        public List<Integer> getKickers() {
            return kickers;
        }

        @Override
        public String toString() {
            return rank.getName() + " " + kickers;
        }
    }

    public HandResult evaluate(List<Card> holeCards, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(holeCards);
        allCards.addAll(communityCards);

        List<HandResult> results = new ArrayList<>();
        List<List<Card>> combinations = getCombinations(allCards, 5);
        for (List<Card> combo : combinations) {
            results.add(evaluateFiveCards(combo));
        }

        return Collections.max(results, this::compare);
    }

    public int compare(HandResult h1, HandResult h2) {
        int rankCompare = Integer.compare(h1.getRank().getValue(), h2.getRank().getValue());
        if (rankCompare != 0) {
            return rankCompare;
        }

        List<Integer> k1 = h1.getKickers();
        List<Integer> k2 = h2.getKickers();

        for (int i = 0; i < Math.min(k1.size(), k2.size()); i++) {
            int kickerCompare = Integer.compare(k1.get(i), k2.get(i));
            if (kickerCompare != 0) {
                return kickerCompare;
            }
        }

        return 0;
    }

    private HandResult evaluateFiveCards(List<Card> cards) {
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> Integer.compare(b.getRank().getValue(), a.getRank().getValue()));

        boolean isFlush = isFlush(sorted);
        boolean isStraight = isStraight(sorted);

        List<Integer> values = getValues(sorted);

        Map<Integer, Integer> valueCounts = new HashMap<>();
        for (Card card : sorted) {
            valueCounts.merge(card.getRank().getValue(), 1, Integer::sum);
        }

        List<Integer> counts = new ArrayList<>(valueCounts.values());
        counts.sort(Integer::compareTo);
        Collections.reverse(counts);

        if (isFlush && isStraight && values.get(0) == 14 && !isWheelStraight(sorted)) {
            return new HandResult(HandRank.ROYAL_FLUSH, values.subList(0, 5));
        }

        if (isFlush && isStraight) {
            return new HandResult(HandRank.STRAIGHT_FLUSH, getStraightKickers(sorted));
        }

        if (counts.get(0) == 4) {
            List<Integer> kickers = getKickers(valueCounts, 1);
            return new HandResult(HandRank.FOUR_OF_A_KIND, kickers);
        }

        if (counts.get(0) == 3 && counts.get(1) == 2) {
            return new HandResult(HandRank.FULL_HOUSE, getFullHouseValues(valueCounts));
        }

        if (isFlush) {
            return new HandResult(HandRank.FLUSH, values.subList(0, 5));
        }

        if (isStraight) {
            return new HandResult(HandRank.STRAIGHT, getStraightKickers(sorted));
        }

        if (counts.get(0) == 3) {
            List<Integer> kickers = getKickers(valueCounts, 2);
            return new HandResult(HandRank.THREE_OF_A_KIND, kickers);
        }

        if (counts.get(0) == 2 && counts.get(1) == 2) {
            List<Integer> kickers = getTwoPairKickers(valueCounts);
            return new HandResult(HandRank.TWO_PAIR, kickers);
        }

        if (counts.get(0) == 2) {
            List<Integer> kickers = getKickers(valueCounts, 3);
            return new HandResult(HandRank.ONE_PAIR, kickers);
        }

        return new HandResult(HandRank.HIGH_CARD, values.subList(0, 5));
    }

    private boolean isFlush(List<Card> cards) {
        Card.Suit suit = cards.get(0).getSuit();
        return cards.stream().allMatch(c -> c.getSuit() == suit);
    }

    private boolean isWheelStraight(List<Card> cards) {
        Set<Integer> valueSet = new HashSet<>();
        for (Card card : cards) {
            valueSet.add(card.getRank().getValue());
        }
        return valueSet.contains(14) && valueSet.contains(2) &&
               valueSet.contains(3) && valueSet.contains(4) && valueSet.contains(5);
    }

    private boolean isStraight(List<Card> cards) {
        List<Integer> values = getValues(cards);
        Set<Integer> valueSet = new HashSet<>(values);

        if (valueSet.size() != 5) {
            return false;
        }

        int max = Collections.max(values);
        int min = Collections.min(values);

        if (max - min == 4) {
            return true;
        }

        if (valueSet.contains(14) && valueSet.contains(2) &&
                valueSet.contains(3) && valueSet.contains(4) && valueSet.contains(5)) {
            return true;
        }

        return false;
    }

    private List<Integer> getStraightKickers(List<Card> cards) {
        List<Integer> values = getValues(cards);
        Set<Integer> valueSet = new HashSet<>(values);

        if (valueSet.contains(14) && valueSet.contains(2) &&
                valueSet.contains(3) && valueSet.contains(4) && valueSet.contains(5)) {
            return Arrays.asList(5, 4, 3, 2, 1);
        }

        return values.subList(0, 5);
    }

    private List<Integer> getValues(List<Card> cards) {
        List<Integer> values = new ArrayList<>();
        for (Card card : cards) {
            values.add(card.getRank().getValue());
        }
        values.sort(Integer::compareTo);
        Collections.reverse(values);
        return values;
    }

    private List<Integer> getKickers(Map<Integer, Integer> valueCounts, int singleCount) {
        int maxCount = valueCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        List<Integer> mainCards = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : valueCounts.entrySet()) {
            if (entry.getValue() == maxCount) {
                mainCards.add(entry.getKey());
            }
        }
        Collections.sort(mainCards);
        Collections.reverse(mainCards);

        List<Integer> singles = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : valueCounts.entrySet()) {
            if (entry.getValue() < maxCount) {
                singles.add(entry.getKey());
            }
        }
        Collections.sort(singles);
        Collections.reverse(singles);

        List<Integer> result = new ArrayList<>(mainCards);
        result.addAll(singles.subList(0, Math.min(singleCount, singles.size())));
        return result;
    }

    private List<Integer> getTwoPairKickers(Map<Integer, Integer> valueCounts) {
        List<Integer> pairs = new ArrayList<>();
        List<Integer> singles = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : valueCounts.entrySet()) {
            if (entry.getValue() == 2) {
                pairs.add(entry.getKey());
            } else {
                singles.add(entry.getKey());
            }
        }

        Collections.sort(pairs);
        Collections.reverse(pairs);
        Collections.sort(singles);
        Collections.reverse(singles);

        List<Integer> result = new ArrayList<>();
        result.addAll(pairs);
        result.addAll(singles);
        return result;
    }

    private List<Integer> getFullHouseValues(Map<Integer, Integer> valueCounts) {
        List<Integer> threes = new ArrayList<>();
        List<Integer> twos = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : valueCounts.entrySet()) {
            if (entry.getValue() == 3) {
                threes.add(entry.getKey());
            } else {
                twos.add(entry.getKey());
            }
        }

        Collections.sort(threes);
        Collections.reverse(threes);
        Collections.sort(twos);
        Collections.reverse(twos);

        List<Integer> result = new ArrayList<>();
        result.addAll(threes);
        result.addAll(twos);
        return result;
    }

    private <T> List<List<T>> getCombinations(List<T> list, int k) {
        List<List<T>> result = new ArrayList<>();
        combine(list, k, 0, new ArrayList<>(), result);
        return result;
    }

    private <T> void combine(List<T> list, int k, int start, List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            combine(list, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}