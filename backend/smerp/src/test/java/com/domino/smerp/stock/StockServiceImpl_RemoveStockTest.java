package com.domino.smerp.stock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.item.Item;
import com.domino.smerp.item.repository.ItemRepository;
import com.domino.smerp.location.Location;
import com.domino.smerp.location.LocationRepository;
import com.domino.smerp.lotno.LotNumber;
import com.domino.smerp.lotno.LotNumberService;
import com.domino.smerp.lotno.constants.LotNumberStatus;
import com.domino.smerp.stock.service.StockServiceImpl;
import com.domino.smerp.warehouse.Warehouse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StockServiceImpl_RemoveStockTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private LotNumberService lotNumberService;

    @Spy
    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    @DisplayName("재고 제거 성공 - 재고 수량 충분")
    void removeStock_success() {
        // given
        BigDecimal removeQty = new BigDecimal("600.000"); // 삭제할 수량

        Item item = Item.builder().itemId(1L).name("ItemA").build();

        LotNumber lotNumber = LotNumber.builder()
                .item(item)
                .name("name")
                .qty(new BigDecimal("600.000"))
                .status(LotNumberStatus.ACTIVE)
                .build();

        // builder에서 id 값 명시 x시 null -> db insert 시 반영
        Warehouse wh1 = Warehouse.builder().id(1L).name("WAREHOUSE1").build();
        Warehouse wh2 = Warehouse.builder().id(2L).name("WAREHOUSE2").build();

        Stock stock1 = Stock.builder()
                .id(1L)
                .qty(new BigDecimal("100.000"))
                .currentQty(new BigDecimal("600.000"))
                .location(Location.builder()
                        .id(1L)
                        .warehouse(wh1)
                        .curQty(new BigDecimal("100.000"))
                        .build())
                .build();
        wh1.addLocation(stock1.getLocation());

        Stock stock2 = Stock.builder()
                .id(2L)
                .qty(new BigDecimal("500.000"))
                .currentQty(new BigDecimal("600.000"))
                .location(Location.builder()
                        .id(2L)
                        .warehouse(wh2)
                        .curQty(new BigDecimal("500.000"))
                        .build())
                .build();
        wh2.addLocation(stock2.getLocation());

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(stockRepository.findAllByItemId(1L)).thenReturn(List.of(stock1, stock2));
        when(lotNumberService.createLotNumberForStock(item, removeQty)).thenReturn(lotNumber);
        when(locationRepository.save(any(Location.class))).thenAnswer(i -> i.getArgument(0));
        doReturn(new BigDecimal("600.000")).when(stockService).getTotalStock(1L);

        List<Stock> result = stockService.removeStock(1L, removeQty);

        System.out.println("Result0 location curQty="
                + result.get(0).getLocation().getCurQty().toPlainString());
        System.out.println("Result1 location curQty="
                + result.get(1).getLocation().getCurQty().toPlainString());
        System.out.println("Result0 qty=" + result.get(0).getQty().toPlainString());
        System.out.println("Result1 qty=" + result.get(1).getQty().toPlainString());

        System.out.println(
                "Result0 current qty=" + result.get(0).getCurrentQty().toPlainString());
        System.out.println(
                "Result1 current qty=" + result.get(1).getCurrentQty().toPlainString());
        System.out.println("Result0 stock id=" + result.get(0).getId());
        System.out.println("Result1 stock id=" + result.get(1).getId());

        // then : 예상 stock의 개수
        assertEquals(2, result.size());

        // 창고1의 재고
        assertEquals(0, result.get(0).getLocation().getCurQty().compareTo(BigDecimal.ZERO));
        assertEquals(wh1, result.get(0).getLocation().getWarehouse());
        assertEquals(0, result.get(0).getQty().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.get(0).getCurrentQty().compareTo(new BigDecimal("500.000")));
        assertEquals(lotNumber, result.get(0).getLotNumber());

        // 창고2의 재고
        assertEquals(0, result.get(1).getQty().compareTo(BigDecimal.ZERO));
        assertEquals(0, result.get(1).getLocation().getCurQty().compareTo(BigDecimal.ZERO));
        assertEquals(wh2, result.get(1).getLocation().getWarehouse());
        assertEquals(0, result.get(1).getQty().compareTo(BigDecimal.ZERO));
        assertEquals(lotNumber, result.get(1).getLotNumber());
    }

    @Test
    @DisplayName("재고 제거 실패 - 재고 수량 부족")
    void removeStock_fail() {
        // given
        BigDecimal removeQty = new BigDecimal("600.000"); // 삭제할 수량
        doReturn(new BigDecimal("500.000")).when(stockService).getTotalStock(1L);

        Item item = Item.builder().itemId(1L).name("ItemA").build();

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        // 예외
        assertThatThrownBy(() -> stockService.removeStock(1L, removeQty))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.STOCK_NOT_ENOUGH.getMessage());
    }
}
