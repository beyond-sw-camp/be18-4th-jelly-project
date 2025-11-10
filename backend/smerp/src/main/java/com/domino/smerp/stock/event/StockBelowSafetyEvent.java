package com.domino.smerp.stock.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
// 생산계획 생성용

@AllArgsConstructor
@Getter
public class StockBelowSafetyEvent {
    private final Long itemId;
}
