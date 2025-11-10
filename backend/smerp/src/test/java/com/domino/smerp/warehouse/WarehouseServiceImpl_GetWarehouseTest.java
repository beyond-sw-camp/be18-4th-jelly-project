package com.domino.smerp.warehouse;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.warehouse.constants.DivisionType;
import com.domino.smerp.warehouse.dto.response.WarehouseResponse;
import com.domino.smerp.warehouse.repository.WarehouseRepository;
import com.domino.smerp.warehouse.service.WarehouseServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImpl_GetWarehouseTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    @Test
    void getWarehouseById_success() {
        // given
        Warehouse mockWarehouse = Warehouse.builder()
                .id(1L)
                .name("Warehouse")
                .divisionType(DivisionType.WAREHOUSE)
                .active(true)
                .address("address")
                .zipcode("zipcode")
                .build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(mockWarehouse));

        // when
        WarehouseResponse response = warehouseService.getWarehouseById(1L);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Warehouse", response.getName());
        assertEquals(DivisionType.WAREHOUSE, response.getDivisionType());
        assertEquals("address", response.getAddress());
        assertEquals("zipcode", response.getZipcode());
        assertEquals(true, response.isActive());
    }

    @Test
    void getWarehouseById_notFound() {
        // given
        when(warehouseRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> warehouseService.getWarehouseById(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.WAREHOUSE_NOT_FOUND.getMessage());
    }
}
