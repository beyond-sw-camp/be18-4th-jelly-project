package com.domino.smerp.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.item.constants.ItemStatusStatus;
import com.domino.smerp.item.dto.request.CreateItemRequest;
import com.domino.smerp.item.dto.response.ItemDetailResponse;
import com.domino.smerp.item.repository.ItemRepository;
import com.domino.smerp.item.repository.ItemStatusRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ItemServiceImpl_CreateItemTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemStatusRepository itemStatusRepository;

    @InjectMocks
    private ItemServiceImpl itemServiceImpl;

    @Test
    @DisplayName("정상적인 Item 생성 시 Item이 저장되고, 리턴한다.")
    void createItem_success() {
        CreateItemRequest request = CreateItemRequest.builder()
                .itemStatusId(1L)
                .name("볼트")
                .specification("M8x20")
                .unit("EA")
                .inboundUnitPrice(BigDecimal.valueOf(100))
                .outboundUnitPrice(BigDecimal.valueOf(150))
                .itemAct("사용중")
                .safetyStock(BigDecimal.valueOf(50))
                .safetyStockAct("사용중")
                .rfid("RFID-001")
                .groupName1("가전")
                .groupName2("소켓")
                .groupName3("기타")
                .build();

        ItemStatus itemStatus = ItemStatus.builder()
                .itemStatusId(1L)
                .status(ItemStatusStatus.RAW_MATERIAL)
                .build();

        Item savedItem = Item.create(request, itemStatus);

        // itemStatus 선언 통과
        when(itemStatusRepository.findById(1L)).thenReturn(Optional.of(itemStatus));

        // 중복 검사 통과
        when(itemRepository.existsByName("볼트")).thenReturn(false);
        when(itemRepository.existsByRfid("RFID-001")).thenReturn(false);

        // 생성한 item 저장 통과
        when(itemRepository.save(any(Item.class))).thenReturn(savedItem);

        // Act — 메서드 실행
        ItemDetailResponse response = itemServiceImpl.createItem(request);

        // Assert — 결과 검증
        assertNotNull(response);
        assertEquals(savedItem.getItemId(), response.getItemId());
        assertEquals(itemStatus.getItemStatusId(), response.getItemStatusId());
        assertEquals(itemStatus.getStatus().getDescription(), response.getItemStatusName());
        assertEquals("볼트", response.getName());
        assertEquals("M8x20", response.getSpecification());
        assertEquals("EA", response.getUnit());
        assertEquals(BigDecimal.valueOf(100), response.getInboundUnitPrice());
        assertEquals(BigDecimal.valueOf(150), response.getOutboundUnitPrice());
        assertEquals(savedItem.getCreatedAt(), response.getCreatedAt());
        assertEquals(savedItem.getUpdatedAt(), response.getUpdatedAt());
        assertEquals(savedItem.getItemAct().getDescription(), response.getItemAct());
        assertEquals(BigDecimal.valueOf(50), response.getSafetyStock());
        assertEquals(savedItem.getSafetyStockAct().getDescription(), response.getSafetyStockAct());
        assertEquals("RFID-001", response.getRfid());
        assertEquals("가전", response.getGroupName1());
        assertEquals("소켓", response.getGroupName2());
        assertEquals("기타", response.getGroupName3());

        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    @DisplayName("같은 이름의 품목이 이미 존재하면 CustomException(DUPLICATE_ITEM)이 발생한다")
    void createItem_duplicateName() {
        // Arrange
        CreateItemRequest request = CreateItemRequest.builder()
                .itemStatusId(1L)
                .name("볼트")
                .specification("M8x20")
                .unit("EA")
                .inboundUnitPrice(BigDecimal.valueOf(100))
                .outboundUnitPrice(BigDecimal.valueOf(150))
                .itemAct("사용중")
                .safetyStock(BigDecimal.valueOf(50))
                .safetyStockAct("사용중")
                .rfid("RFID-001")
                .groupName1("가전")
                .groupName2("소켓")
                .groupName3("기타")
                .build();

        ItemStatus itemStatus = ItemStatus.builder()
                .itemStatusId(1L)
                .status(ItemStatusStatus.RAW_MATERIAL)
                .build();

        when(itemStatusRepository.findById(1L)).thenReturn(Optional.of(itemStatus));
        when(itemRepository.existsByName("볼트")).thenReturn(true); // 이미 존재

        // Act
        CustomException exception = assertThrows(CustomException.class, () -> itemServiceImpl.createItem(request));

        // Assert
        assertEquals(ErrorCode.DUPLICATE_ITEM, exception.getErrorCode());
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    @DisplayName("같은 RFID가 이미 존재하면 CustomException(DUPLICATE_RFID)이 발생한다")
    void createItem_duplicateRfid() {

        // Arrange
        CreateItemRequest request = CreateItemRequest.builder()
                .itemStatusId(1L)
                .name("볼트")
                .specification("M8x20")
                .unit("EA")
                .inboundUnitPrice(BigDecimal.valueOf(100))
                .outboundUnitPrice(BigDecimal.valueOf(150))
                .itemAct("사용중")
                .safetyStock(BigDecimal.valueOf(50))
                .safetyStockAct("사용중")
                .rfid("RFID-001")
                .groupName1("가전")
                .groupName2("소켓")
                .groupName3("기타")
                .build();

        ItemStatus itemStatus = ItemStatus.builder()
                .itemStatusId(1L)
                .status(ItemStatusStatus.RAW_MATERIAL)
                .build();

        when(itemStatusRepository.findById(1L)).thenReturn(Optional.of(itemStatus));
        when(itemRepository.existsByName("볼트")).thenReturn(false);
        when(itemRepository.existsByRfid("RFID-001")).thenReturn(true); // 중복 RFID

        // Act
        CustomException exception = assertThrows(CustomException.class, () -> itemServiceImpl.createItem(request));

        // Assert
        assertEquals(ErrorCode.DUPLICATE_RFID, exception.getErrorCode());
        verify(itemRepository, never()).save(any(Item.class));
    }
}
