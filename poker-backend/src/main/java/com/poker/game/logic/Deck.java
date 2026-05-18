package com.poker.game.logic;

import com.poker.game.model.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 扑克牌组 - 非Spring Bean，每局游戏独立实例
 */
public class Deck {

    private List<Card> cards = new ArrayList<>();

    /**
     * 创建一副新牌并洗牌
     */
    public void shuffle() {
        cards.clear();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
        Collections.shuffle(cards);
    }

    /**
     * 发一张牌
     */
    public Card deal() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.remove(cards.size() - 1);
    }

    /**
     * 发多张牌
     */
    public List<Card> deal(int count) {
        List<Card> dealt = new ArrayList<>();
        for (int i = 0; i < count && !cards.isEmpty(); i++) {
            dealt.add(deal());
        }
        return dealt;
    }

    /**
     * 获取剩余牌数
     */
    public int remaining() {
        return cards.size();
    }
}