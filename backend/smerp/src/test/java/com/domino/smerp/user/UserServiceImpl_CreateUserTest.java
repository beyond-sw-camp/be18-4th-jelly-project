package com.domino.smerp.user;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domino.smerp.client.Client;
import com.domino.smerp.client.ClientRepository;
import com.domino.smerp.common.encrypt.SsnEncryptor;
import com.domino.smerp.common.exception.CustomException;
import com.domino.smerp.common.exception.ErrorCode;
import com.domino.smerp.user.constants.UserRole;
import com.domino.smerp.user.dto.request.CreateUserRequest;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImpl_CreateUserTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SsnEncryptor ssnEncryptor;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("회원 생성 성공 - 중복 없음 + 거래처 존재")
    void createUser_success() {
        // given
        CreateUserRequest request = CreateUserRequest.builder()
                .name("홍길동")
                .email("hong@test.com")
                .phone("010-1234-5678")
                .address("서울시 강남구")
                .ssn("123456-1234567")
                .loginId("hong")
                .password("pass123")
                .hireDate(LocalDate.of(2024, 1, 1))
                .deptTitle("인사팀")
                .role(UserRole.USER)
                .companyName("한화캠프")
                .build();

        Client mockClient = mock(Client.class);

        when(ssnEncryptor.encryptSsn(anyString())).thenReturn("encryptedSsn");
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(clientRepository.findByCompanyName(anyString())).thenReturn(Optional.of(mockClient));
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.existsByLoginId(anyString())).thenReturn(false);
        when(userRepository.existsBySsn(anyString())).thenReturn(false);
        when(userRepository.findLastEmpNoByYearMonth(anyString())).thenReturn(null);

        // when
        userService.createUser(request);

        // then
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("회원 생성 실패 - 이메일 중복")
    void createUser_fail_duplicateEmail() {
        // given
        CreateUserRequest request =
                CreateUserRequest.builder().email("dup@test.com").build();

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("회원 생성 실패 - 거래처 없음")
    void createUser_fail_clientNotFound() {
        // given
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@test.com")
                .loginId("user1")
                .phone("010-0000-0000")
                .ssn("123456-1234567")
                .companyName("없는회사")
                .hireDate(LocalDate.of(2025, 1, 1))
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.existsByLoginId(anyString())).thenReturn(false);
        when(ssnEncryptor.encryptSsn(anyString())).thenReturn("encrypted");
        when(userRepository.existsBySsn(anyString())).thenReturn(false);
        when(clientRepository.findByCompanyName(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.CLIENT_NOT_FOUND.getMessage());
    }
}
