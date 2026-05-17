package com.poker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomDTO {

    @NotBlank(message = "房间名称不能为空")
    private String name;

    @NotNull(message = "小盲注不能为空")
    private Integer smallBlind;

    @NotNull(message = "大盲注不能为空")
    private Integer bigBlind;

    @NotNull(message = "最小带入不能为空")
    private Integer minBuyIn;

    @NotNull(message = "最大带入不能为空")
    private Integer maxBuyIn;

    @NotNull(message = "最大人数不能为空")
    private Integer maxPlayers;
}