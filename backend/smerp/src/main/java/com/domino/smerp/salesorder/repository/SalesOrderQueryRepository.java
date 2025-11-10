package com.domino.smerp.salesorder.repository;

import com.domino.smerp.salesorder.SalesOrder;
import com.domino.smerp.salesorder.dto.request.SearchSalesOrderRequest;
import com.domino.smerp.salesorder.dto.request.SearchSummarySalesOrderRequest;
import com.domino.smerp.salesorder.dto.response.SummarySalesOrderResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SalesOrderQueryRepository {
    Page<SalesOrder> searchSalesOrders(SearchSalesOrderRequest condition, Pageable pageable);

    List<SummarySalesOrderResponse> searchSummarySalesOrder(
            SearchSummarySalesOrderRequest condition, Pageable pageable);
}
