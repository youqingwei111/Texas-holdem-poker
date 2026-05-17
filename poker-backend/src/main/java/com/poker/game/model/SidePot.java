package com.poker.game.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 边池模型
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SidePot {

    private Long amount;
    private java.util.List<Long> eligiblePlayerIds;

    public SidePot() {}

    public SidePot(Long amount, java.util.List<Long> eligiblePlayerIds) {
        this.amount = amount;
        this.eligiblePlayerIds = eligiblePlayerIds;
    }
}