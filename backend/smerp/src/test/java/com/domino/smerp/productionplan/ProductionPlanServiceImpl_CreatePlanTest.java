package com.domino.smerp.productionplan;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.productionplan.dto.request.CreateProductionPlanRequest;
import com.domino.smerp.productionplan.dto.response.ProductionPlanResponse;
import com.domino.smerp.productionplan.repository.ProductionPlanRepository;
import com.domino.smerp.productionplan.service.ProductionPlanServiceImpl;
import com.domino.smerp.user.User;
import com.domino.smerp.user.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductionPlanServiceImpl_CreatePlanTest {

    @Mock
    private ProductionPlanRepository productionPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    @InjectMocks
    private ProductionPlanServiceImpl productionPlanService;

    @Test
    @DisplayName("생산계획 생성 성공")
    void createProductionPlan_success() {
        // given
        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .title("Test Plan")
                .name("John")
                .remark("Test Remark")
                .qty(new BigDecimal("100.000"))
                .build();

        User mockUser = User.builder().userId(1L).name("John").build();

        when(userRepository.findByName("John")).thenReturn(Optional.of(mockUser));
        when(productionPlanRepository.existsByTitle("Test Plan")).thenReturn(false);
        when(productionPlanRepository.save(any(ProductionPlan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doReturn("DOC12345").when(productionPlanService).generateDocumentNoWithRetry(any());

        // when
        ProductionPlanResponse response = productionPlanService.createProductionPlan(request);

        // then
        assertNotNull(response);
        assertEquals("Test Plan", response.getTitle());
        assertEquals(0, response.getQty().compareTo(new BigDecimal("100.000")));
        assertEquals("Test Remark", response.getRemark());
        assertEquals(mockUser.getName(), response.getName());

        // production plan repository에 저장되는 production plan 객체 캡쳐
        ArgumentCaptor<ProductionPlan> captor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(productionPlanRepository, times(1)).save(captor.capture());

        ProductionPlan savedPlan = captor.getValue();

        // 해당 객체 필드 확인
        assertEquals("Test Plan", savedPlan.getTitle());
        assertEquals(0, savedPlan.getQty().compareTo(new BigDecimal("100.000")));
        assertEquals(mockUser, savedPlan.getUser());
        assertFalse(savedPlan.isDeleted());
        assertEquals("PENDING", savedPlan.getStatus().name());
        assertNotNull(savedPlan.getDocumentNo());
    }

    @Test
    @DisplayName("생산계획 생성 실패 - 제목 중복")
    void createProductionPlan_fail_duplicateTitle() {
        // given
        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .title("Test Plan")
                .name("John")
                .remark("Test Remark")
                .qty(new BigDecimal("100.000"))
                .build();

        when(productionPlanRepository.existsByTitle("Test Plan")).thenReturn(true);

        // then
        assertThatThrownBy(() -> productionPlanService.createProductionPlan(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.PRODUCTION_PLAN_DUPLICATE_TITLE.getMessage());
    }

    @Test
    @DisplayName("생산계획 생성 실패 - 수량 음수")
    void createProductionPlan_fail_qtyNegative() {
        // given
        CreateProductionPlanRequest request = CreateProductionPlanRequest.builder()
                .title("Test Plan")
                .name("John")
                .remark("Test Remark")
                .qty(new BigDecimal("-100.000"))
                .build();

        // then
        assertThatThrownBy(() -> productionPlanService.createProductionPlan(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.QTY_UNDER_ZERO.getMessage());
    }
}
