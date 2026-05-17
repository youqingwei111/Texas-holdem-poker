package com.poker.game.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;

/**
 * 扑克牌
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card implements Serializable {

    public enum Suit {
        SPADES("♠"), HEARTS("♥"), CLUBS("♣"), DIAMONDS("♦");

        private final String symbol;

        Suit(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public enum Rank {
        TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8),
        NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14);

        private final int value;

        Rank(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private Suit suit;
    private Rank rank;

    public Card() {}

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    @JsonIgnore
    public String getDisplayName() {
        String rankStr;
        switch (rank) {
            case JACK: rankStr = "J"; break;
            case QUEEN: rankStr = "Q"; break;
            case KING: rankStr = "K"; break;
            case ACE: rankStr = "A"; break;
            default: rankStr = String.valueOf(rank.getValue());
        }
        return suit.getSymbol() + rankStr;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}