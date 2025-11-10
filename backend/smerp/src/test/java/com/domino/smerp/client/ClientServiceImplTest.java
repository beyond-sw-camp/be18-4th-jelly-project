package com.domino.smerp.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domino.smerp.client.dto.request.CreateClientRequest;
import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    @DisplayName("거래처 생성 성공 - 중복 없음")
    void createClient_success() {
        // given
        CreateClientRequest request = CreateClientRequest.builder()
                .companyName("테스트회사")
                .businessNumber("123-45-67890")
                .build();

        when(clientRepository.existsByCompanyName(anyString())).thenReturn(false);
        when(clientRepository.existsByBusinessNumber(anyString())).thenReturn(false);

        // when
        clientService.createClient(request);

        // then
        verify(clientRepository, times(1)).save(any(Client.class));
    }

    @Test
    @DisplayName("거래처 생성 실패 - 회사명 중복")
    void createClient_fail_duplicateCompanyName() {
        // given
        CreateClientRequest request =
                CreateClientRequest.builder().companyName("중복회사").build();

        when(clientRepository.existsByCompanyName(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> clientService.createClient(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_COMPANY_NAME.getMessage());
    }

    @Test
    @DisplayName("거래처 삭제 실패 - 존재하지 않는 ID")
    void deleteClient_fail_notFound() {
        // given
        when(clientRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> clientService.deleteClient(1L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.CLIENT_NOT_FOUND.getMessage());
    }
}
