package com.domino.smerp.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domino.smerp.client.Client;
import com.domino.smerp.client.ClientRepository;
import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.user.dto.request.UpdateUserRequest;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
;

@ExtendWith(MockitoExtension.class)
class UserServiceImpl_UpdateUserTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("회원 수정 실패 - 전화번호 중복")
    void updateUser_fail_duplicatePhone() {
        // given
        UpdateUserRequest request =
                UpdateUserRequest.builder().phone("010-1111-2222").build();

        when(userRepository.existsByPhone(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.updateUser("202401001", request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_PHONE.getMessage());
    }

    @Test
    @DisplayName("회원 수정 실패 - 사용자 없음")
    void updateUser_fail_userNotFound() {
        // given
        UpdateUserRequest request =
                UpdateUserRequest.builder().phone("010-9999-9999").build();

        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.findByEmpNo(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser("202401001", request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원 수정 성공 - 거래처 변경 없음")
    void updateUser_success_noClientChange() {
        // given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .phone("010-3333-4444")
                .deptTitle("총무팀")
                .build();

        User mockUser = mock(User.class);

        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.findByEmpNo(anyString())).thenReturn(Optional.of(mockUser));

        // when
        userService.updateUser("202401001", request);

        // then
        verify(mockUser, times(1)).updateUser(request);
        verify(clientRepository, never()).findByCompanyName(anyString());
    }

    @Test
    @DisplayName("회원 수정 실패 - 거래처 없음")
    void updateUser_fail_clientNotFound() {
        // given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .phone("010-5555-6666")
                .companyName("없는회사")
                .build();

        User mockUser = mock(User.class);

        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.findByEmpNo(anyString())).thenReturn(Optional.of(mockUser));
        when(clientRepository.findByCompanyName(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser("202401001", request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.CLIENT_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("회원 수정 성공 - 거래처 변경 있음")
    void updateUser_success_withClientChange() {
        // given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .phone("010-7777-8888")
                .companyName("한화시스템")
                .build();

        User mockUser = mock(User.class);
        Client mockClient = mock(Client.class);

        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.findByEmpNo(anyString())).thenReturn(Optional.of(mockUser));
        when(clientRepository.findByCompanyName(anyString())).thenReturn(Optional.of(mockClient));

        // when
        userService.updateUser("202401001", request);

        // then
        verify(mockUser, times(1)).updateUser(request);
        verify(mockUser, times(1)).updateClient(mockClient);
    }
}
