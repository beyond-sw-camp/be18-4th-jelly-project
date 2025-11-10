package com.domino.smerp.purchase.itemrequestpurchaseorder.dto.request;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class ItemRequestPurchaseOrderCreateRequest {
    private final Long itemId;
    private final BigDecimal qty;
    private final BigDecimal inboundUnitPrice; // 입고 단가
    private final BigDecimal specialPrice; // 요청 시 선택 입력
}
