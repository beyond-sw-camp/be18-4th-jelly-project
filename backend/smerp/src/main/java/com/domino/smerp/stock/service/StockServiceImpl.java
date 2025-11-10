package com.domino.smerp.stock.service;

import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.item.Item;
import com.domino.smerp.item.repository.ItemRepository;
import com.domino.smerp.itemorder.ItemOrder;
import com.domino.smerp.itemorder.ItemOrderRepository;
import com.domino.smerp.location.Location;
import com.domino.smerp.location.LocationRepository;
import com.domino.smerp.lotno.LotNumber;
import com.domino.smerp.lotno.LotNumberService;
import com.domino.smerp.stock.Stock;
import com.domino.smerp.stock.StockRepository;
import com.domino.smerp.stock.dto.response.StockListResponse;
import com.domino.smerp.stock.dto.response.StockResponse;
import com.domino.smerp.stock.event.StockAboveSafetyEvent;
import com.domino.smerp.stock.event.StockBelowSafetyEvent;
import com.domino.smerp.warehouse.Warehouse;
import com.domino.smerp.warehouse.repository.WarehouseRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class StockServiceImpl implements StockService {
    private final StockRepository stockRepository;
    private final ItemRepository itemRepository;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final LotNumberService lotNumberService;
    private final ItemOrderRepository itemOrderRepository;

    // 요청상 조회, 수정

    /*
    @Override
    @Transactional
    public void initializeStocksWithItems(){
      List<Item> items = itemRepository.findAll();

      for(Item item : items){
        if(!stockRepository.existsByItemId(item.getItemId())){
          Stock stock = Stock.create(item);
          stockRepository.save(stock);
        }
      }
    }
    */

    // 목록 조회
    @Transactional(readOnly = true)
    @Override
    public StockListResponse getAllStocks() {
        // soft delete 아니므로
        List<Stock> stocks = stockRepository.findAll();

        List<StockResponse> stockResponses = new ArrayList<>();

        for (Stock stock : stocks) {
            stockResponses.add(toStockResponse(stock));
        }

        return StockListResponse.builder().stockResponses(stockResponses).build();
    }

    // 상세 조회
    @Override
    @Transactional(readOnly = true)
    public StockResponse getStockById(Long stockId) {
        Stock stock =
                stockRepository.findById(stockId).orElseThrow(() -> new CustomException(ErrorCode.STOCK_NOT_FOUND));
        return toStockResponse(stock);
    }

    // 수정 - qty에 대해 직접 x => 재고수불로만 반영

    @Override
    @Transactional
    public List<Stock> allocateStock(Long itemId, BigDecimal qty) {

        List<Warehouse> availableWarehouses = warehouseRepository.findAvailableWarehousesWithCurQty();
        if (availableWarehouses.isEmpty()) {
            throw new CustomException(ErrorCode.NO_WAREHOUSE_EMPTY);
        }

        Item item = itemRepository.findById(itemId).orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        BigDecimal remainingQty = qty;

        BigDecimal runningTotal = getTotalStock(itemId);

        List<Stock> createdStocks = new ArrayList<>();

        LotNumber lotNumber = lotNumberService.createLotNumberForStock(item, qty);

        for (Warehouse warehouse : availableWarehouses) {

            List<Location> locations =
                    locationRepository.findAvailableLocationsWithCurQty(warehouse.getId(), remainingQty);

            for (Location loc : locations) {

                // 각 위치의 남은 공간
                // max, cur 기본값 있으므로 null check x
                BigDecimal available = loc.getMaxQty().subtract(loc.getCurQty());

                // 남은 공간보다 가용 공간 크면 남은 공간만큼
                // 남은 공간보다 가용 공간이 작으면 남은 공간만큼
                BigDecimal allocateQty = remainingQty.min(available);

                runningTotal = runningTotal.add(allocateQty);

                if (available.compareTo(BigDecimal.ZERO) <= 0) continue;

                Stock stock = Stock.builder()
                        .location(loc)
                        .lotNumber(lotNumber)
                        .item(item)
                        .currentQty(runningTotal) // 매 회차마다 추가되는 총 재고수량 500 1000 1500
                        .qty(allocateQty) // 현 회차에 추가하는 수량 (위치 400 비었으면 500 중 400 가능) == 실제 재고 하나의 total
                        .rfid(null)
                        .build();

                stockRepository.save(stock);
                createdStocks.add(stock);

                // 차지한 공간 -> 마찬가지로 null x
                loc.setCurQty(loc.getCurQty().add(allocateQty));
                locationRepository.save(loc);

                remainingQty = remainingQty.subtract(allocateQty);
                if (remainingQty.compareTo(BigDecimal.ZERO) == 0) break;
            }

            if (remainingQty.compareTo(BigDecimal.ZERO) == 0) break;
        }

        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            throw new CustomException(ErrorCode.LOCATION_NOT_ENOUGH);
        }

        return createdStocks;
    }

    @Transactional
    @Override
    public List<Stock> removeStock(Long itemId, BigDecimal qty) {

        BigDecimal remainQty = qty; // 빼야할 수량
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));
        BigDecimal runningTotalQty = getTotalStock(itemId);

        if (runningTotalQty.compareTo(remainQty) < 0) {
            throw new CustomException(ErrorCode.STOCK_NOT_ENOUGH);
        }
        List<Stock> stocks = stockRepository.findAllByItemId(itemId);
        List<Stock> stocksRemoved = new ArrayList<>();
        LotNumber lotNumber = lotNumberService.createLotNumberForStock(item, qty);

        for (Stock stock : stocks) {
            if (remainQty.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal removeQty = stock.getQty().min(remainQty);

            // 현 위치 재고 감소
            stock.setQty(stock.getQty().subtract(removeQty));
            stock.setLotNumber(lotNumber);
            // 총 재고 수량 - 각 변화되는 값 누적
            runningTotalQty = runningTotalQty.subtract(removeQty);
            stock.setCurrentQty(runningTotalQty);
            stockRepository.save(stock);

            Location location = stock.getLocation();
            location.setCurQty(location.getCurQty().subtract(removeQty));
            locationRepository.save(location);

            // 출고해야할 수량 감소
            remainQty = remainQty.subtract(removeQty);
            stocksRemoved.add(stock); // 실제 삭제 x : 수량 0이더라도 재고 유지됨
        }
        return stocksRemoved;
    }

    // 재고 삭제는 qty 0이어도 x
    // item과 매핑된다면 cascade

    // 창고의 현 재고 > 안전재고 수량
    // 품목 하나에 여러 재고 가능
    // 총 재고양 >= 안전재고 수량이어야 함
    @Override
    @Transactional(readOnly = true)
    public void alertBelowSafetyStock(Long itemId) {
        if (!isAboveSafetyStock(itemId)) {
            applicationEventPublisher.publishEvent(

                    // 부족한 수량에 대해서는 itemId로 파악
                    new StockBelowSafetyEvent(itemId));
        }
    }

    @Override
    @Transactional
    public void alertAboveSafetyStock(Long itemId) {
        if (isAboveSafetyStock(itemId)) {
            applicationEventPublisher.publishEvent(new StockAboveSafetyEvent(itemId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    // 총 재고 > 안전재고
    public boolean isAboveSafetyStock(Long itemId) {

        Item item = itemRepository.findById(itemId).orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        // 품목에 대한 총 재고
        BigDecimal totalQty = getTotalStock(itemId);

        // 총 재고 > 안전재고
        return totalQty.compareTo(item.getSafetyStock()) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAboveItemOrderQty(Long itemOrderId) {
        ItemOrder itemOrder = itemOrderRepository
                .findById(itemOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_ORDER_NOT_FOUND));

        // 해당 item의 총 재고
        BigDecimal totalQty =
                stockRepository.sumQuantityByItemId(itemOrder.getItem().getItemId());

        // 총 재고 > 주문량
        return totalQty.compareTo(itemOrder.getQty()) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalStock(Long itemId) {
        BigDecimal totalQty = stockRepository.sumQuantityByItemId(itemId);
        return totalQty != null ? totalQty : BigDecimal.ZERO;
    }

    public StockResponse toStockResponse(Stock stock) {

        Item item = stock.getLotNumber().getItem();

        return StockResponse.builder()
                .itemId(item.getItemId())
                .itemName(item.getName())
                .specification(item.getSpecification())
                .currentQty(stock.getQty())
                .build();
    }
}
