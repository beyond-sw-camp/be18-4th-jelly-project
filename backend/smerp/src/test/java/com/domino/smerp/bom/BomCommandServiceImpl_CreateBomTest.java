package com.domino.smerp.bom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domino.smerp.bom.dto.request.CreateBomRequest;
import com.domino.smerp.bom.dto.response.BomDetailResponse;
import com.domino.smerp.bom.entity.Bom;
import com.domino.smerp.bom.event.BomChangedEvent;
import com.domino.smerp.bom.repository.BomClosureRepository;
import com.domino.smerp.bom.repository.BomRepository;
import com.domino.smerp.bom.service.command.BomCommandServiceImpl;
import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.item.Item;
import com.domino.smerp.item.ItemService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
public class BomCommandServiceImpl_CreateBomTest {
    @Mock
    private BomRepository bomRepository;

    @Mock
    private BomClosureRepository bomClosureRepository;

    @Mock
    private ItemService itemService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BomCommandServiceImpl bomCommandServiceImpl;

    // 정상 생성 케이스
    @Test
    @DisplayName("정상적인 BOM 생성 시 BOM이 저장되고 이벤트가 발행된다")
    void createBom_success() {
        // Arrange
        CreateBomRequest request = CreateBomRequest.builder()
                .parentItemId(1L)
                .childItemId(2L)
                .qty(BigDecimal.valueOf(10))
                .remark("테스트용 BOM")
                .build();

        // 부모, 자식 아이템 (DB대신 직접 생성)
        Item parent = Item.builder().itemId(1L).name("Parent Item").build();

        Item child = Item.builder().itemId(2L).name("Child Item").build();

        // Mock 동작 설정 — 중복 관계 및 순환 관계 없다고 가정
        when(bomRepository.existsByParentItem_ItemIdAndChildItem_ItemId(1L, 2L)).thenReturn(false);
        when(bomClosureRepository.existsById_AncestorItemIdAndId_DescendantItemId(2L, 1L))
                .thenReturn(false);

        // 아이템 조회 시 parent, child 리턴
        when(itemService.findItemByIdWithLock(1L)).thenReturn(parent);
        when(itemService.findItemByIdWithLock(2L)).thenReturn(child);

        // BOM 저장 Mock
        Bom savedBom = Bom.builder()
                .bomId(100L)
                .parentItem(parent)
                .childItem(child)
                .qty(BigDecimal.valueOf(10))
                .remark("테스트용 BOM")
                .build();

        when(bomRepository.save(any(Bom.class))).thenReturn(savedBom);

        // Act
        BomDetailResponse response = bomCommandServiceImpl.createBom(request);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.getBomId());
        assertEquals(1L, response.getParentItemId());
        assertEquals("Parent Item", response.getParentItemName());
        assertEquals(2L, response.getChildItemId());
        assertEquals("Child Item", response.getChildItemName());
        assertEquals("테스트용 BOM", response.getRemark());

        // 호출 검증 (정상 로직 흐름)
        verify(bomRepository, times(1)).save(any(Bom.class));
        verify(eventPublisher, times(1)).publishEvent(any(BomChangedEvent.class));
    }

    // 예외 케이스 1: 중복 BOM 관계
    @Test
    @DisplayName("이미 동일한 부모-자식 관계가 존재하면 CustomException이 발생한다")
    void createBom_duplicateRelationship() {
        // Arrange
        CreateBomRequest request = CreateBomRequest.builder()
                .parentItemId(1L)
                .childItemId(2L)
                .qty(BigDecimal.valueOf(5))
                .build();

        when(bomRepository.existsByParentItem_ItemIdAndChildItem_ItemId(1L, 2L)).thenReturn(true); // 중복 관계 존재

        // Act
        CustomException exception = assertThrows(CustomException.class, () -> bomCommandServiceImpl.createBom(request));
        // Assert
        assertEquals(ErrorCode.BOM_DUPLICATE_RELATIONSHIP, exception.getErrorCode());
        verify(bomRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // 예외 케이스 2: 순환 참조
    @Test
    @DisplayName("순환 참조 관계일 경우 CustomException이 발생한다")
    void createBom_circularReference() {
        // Arrange
        CreateBomRequest request = CreateBomRequest.builder()
                .parentItemId(1L)
                .childItemId(2L)
                .qty(BigDecimal.valueOf(3))
                .build();

        when(bomRepository.existsByParentItem_ItemIdAndChildItem_ItemId(1L, 2L)).thenReturn(false);
        when(bomClosureRepository.existsById_AncestorItemIdAndId_DescendantItemId(2L, 1L))
                .thenReturn(true); // 순환 참조 존재

        // Act
        CustomException exception = assertThrows(CustomException.class, () -> bomCommandServiceImpl.createBom(request));
        // Assert
        assertEquals(ErrorCode.BOM_CIRCULAR_REFERENCE, exception.getErrorCode());
        verify(bomRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
