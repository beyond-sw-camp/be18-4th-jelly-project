package com.domino.smerp.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.domino.smerp.client.Client;
import com.domino.smerp.client.ClientRepository;
import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.common.util.DocumentNoGenerator;
import com.domino.smerp.item.Item;
import com.domino.smerp.item.ItemServiceImpl;
import com.domino.smerp.itemorder.ItemOrderRepository;
import com.domino.smerp.itemorder.dto.request.ItemOrderRequest;
import com.domino.smerp.order.constants.OrderStatus;
import com.domino.smerp.order.dto.request.CreateOrderRequest;
import com.domino.smerp.order.dto.response.CreateOrderResponse;
import com.domino.smerp.order.repository.OrderRepository;
import com.domino.smerp.user.User;
import com.domino.smerp.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemServiceImpl itemServiceImpl;

    @Mock
    private ItemOrderRepository itemOrderRepository;

    @Mock
    private DocumentNoGenerator documentNoGenerator;

    @InjectMocks
    private OrderServiceImpl orderService;

    // 주문 등록 성공 테스트
    @Test
    @DisplayName("주문 등록 성공 - 정상 입력")
    void createOrder_success() {
        // given
        LocalDate documentDate = LocalDate.of(2025, 10, 13);
        LocalDate deliveryDate = LocalDate.of(2025, 10, 20);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .documentDate(documentDate)
                .companyName("도미노상사")
                .empNo("EMP001")
                .deliveryDate(deliveryDate)
                .remark("테스트 주문")
                .items(List.of(ItemOrderRequest.builder()
                        .itemId(1L)
                        .qty(BigDecimal.valueOf(10))
                        .specialPrice(BigDecimal.valueOf(10000))
                        .build()))
                .build();

        Client client = Client.builder().clientId(1L).companyName("도미노상사").build();
        User user = User.builder().userId(1L).empNo("EMP001").build();
        Item item = Item.builder()
                .itemId(1L)
                .outboundUnitPrice(BigDecimal.valueOf(9000))
                .build();

        when(clientRepository.findByCompanyName(anyString())).thenReturn(Optional.of(client));
        when(userRepository.findByEmpNo(anyString())).thenReturn(Optional.of(user));
        when(itemServiceImpl.findItemById(anyLong())).thenReturn(item);
        when(documentNoGenerator.generate(any(LocalDate.class), any())).thenReturn("2025/10/13-1");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        CreateOrderResponse response = orderService.createOrder(request);

        // then
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(itemOrderRepository, times(1)).saveAll(anyList());
        assertThat(response).isNotNull();
    }

    // 주문 등록 실패 - 거래처 없음
    @Test
    @DisplayName("주문 등록 실패 - 존재하지 않는 거래처")
    void createOrder_fail_clientNotFound() {
        // given
        CreateOrderRequest request = CreateOrderRequest.builder()
                .documentDate(LocalDate.now())
                .companyName("없는회사")
                .empNo("EMP001")
                .deliveryDate(LocalDate.now())
                .remark("테스트")
                .items(List.of(ItemOrderRequest.builder()
                        .itemId(1L)
                        .qty(BigDecimal.valueOf(1))
                        .specialPrice(BigDecimal.valueOf(1000))
                        .build()))
                .build();

        when(clientRepository.findByCompanyName(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.CLIENT_NOT_FOUND.getMessage());
    }

    // 주문 조회 실패 - 존재하지 않는 주문
    @Test
    @DisplayName("주문 조회 실패 - 존재하지 않는 ID")
    void findOrderById_fail_notFound() {
        // given
        when(orderRepository.findByIdWithDetails(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.findOrderById(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    // 주문 삭제 실패 - 존재하지 않는 주문
    @Test
    @DisplayName("주문 삭제 실패 - 존재하지 않는 주문 ID")
    void deleteOrder_fail_notFound() {
        // given
        when(orderRepository.findByIdForDelete(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.deleteOrder(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ORDER_NOT_FOUND.getMessage());
    }

    // 주문 삭제 실패 - 이미 승인된 주문
    @Test
    @DisplayName("주문 삭제 실패 - 이미 승인된 주문")
    void deleteOrder_fail_alreadyApproved() {
        // given
        Order order = Order.builder().orderId(1L).status(OrderStatus.APPROVED).build();

        when(orderRepository.findByIdForDelete(anyLong())).thenReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.deleteOrder(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.ORDER_ALREADY_APPROVED.getMessage());
    }
}
