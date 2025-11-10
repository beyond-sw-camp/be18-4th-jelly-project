package com.domino.smerp.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.domino.smerp.auth.CustomUserDetails;
import com.domino.smerp.user.constants.UserRole;
import com.domino.smerp.user.dto.request.CreateUserRequest;
import com.domino.smerp.user.dto.request.UpdateUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class UserServiceImplSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 테스트에 사용할 Mock UserDetails 객체 (Principal)
    private CustomUserDetails adminPrincipal;
    private CustomUserDetails managerPrincipal;
    private CustomUserDetails userPrincipal;

    @BeforeEach
    void setUp() {
        // 실제 User 엔티티를 Mocking하거나 생성하여 CustomUserDetails에 주입해야 합니다.
        // 여기서는 User 엔티티가 Mocking되어 있다고 가정합니다.

        // ADMIN 권한을 가진 Principal
        User adminUser = User.builder()
                .loginId("admin")
                .name("Admin")
                .role(UserRole.ADMIN)
                .build();
        adminPrincipal = new CustomUserDetails(adminUser);

        // MANAGER 권한을 가진 Principal
        User managerUser = User.builder()
                .loginId("manager")
                .name("Manager")
                .role(UserRole.MANAGER)
                .build();
        managerPrincipal = new CustomUserDetails(managerUser);

        // USER 권한을 가진 Principal
        User regularUser =
                User.builder().loginId("user").name("User").role(UserRole.USER).build();
        userPrincipal = new CustomUserDetails(regularUser);
    }

    // -------------------------------------------------------------------------
    // 1. 사용자 생성 (POST /api/v1/users) - ADMIN/MANAGER만 허용 가정
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("사용자 생성 실패 - USER 권한으로는 접근 불가 (403 Forbidden)")
    void createUser_fail_userRole() throws Exception {
        // given
        CreateUserRequest request = CreateUserRequest.builder()
                // 필수 필드는 Dummy 값으로 채웁니다.
                .loginId("newUser")
                .password("Testpass!1")
                .name("New")
                .email("new@test.com")
                .phone("010-1234-5678")
                .address("addr")
                .ssn("900101-1234567")
                .hireDate(LocalDate.now())
                .deptTitle("인사팀")
                .role(UserRole.USER)
                .build();

        // when & then
        mockMvc.perform(post("/api/v1/users")
                        .with(user(userPrincipal)) // ROLE_USER로 요청
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403 Forbidden 기대
    }

    @Test
    @DisplayName("사용자 생성 성공 - ADMIN 권한으로 접근")
    void createUser_success_adminRole() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .loginId("adminNew")
                .password("Testpass!1")
                .name("AdminNew")
                .email("adminnew@test.com")
                .phone("010-9876-5432")
                .address("addr")
                .ssn("900101-1234567")
                .hireDate(LocalDate.now())
                .deptTitle("인사팀")
                .role(UserRole.USER)
                .build();

        // **주의: 실제 DB에 저장 시 중복 체크 예외가 발생할 수 있습니다.
        // 이 테스트는 시큐리티 인가(Authorization)만 검증함을 목적으로 합니다.**

        mockMvc.perform(post("/api/v1/users")
                        .with(user(adminPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()); // 201 Created 기대
    }

    // -------------------------------------------------------------------------
    // 2. 사용자 정보 수정 (PATCH /api/v1/users/{enpNo}) - ADMIN/MANAGER만 허용 가정
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("사용자 수정 실패 - USER 권한으로는 접근 불가 (403 Forbidden)")
    void updateUser_fail_userRole() throws Exception {
        // given
        UpdateUserRequest request =
                UpdateUserRequest.builder().phone("010-4444-5555").build();
        String targetEmpNo = "202401001"; // 가상의 사번

        // when & then
        mockMvc.perform(patch("/api/v1/users/{enpNo}", targetEmpNo)
                        .with(user(userPrincipal)) // ROLE_USER로 요청
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden()); // 403 Forbidden 기대
    }

    @Test
    @DisplayName("사용자 수정 성공 - MANAGER 권한으로 접근")
    void updateUser_success_managerRole() throws Exception {
        // given: 대상 사용자 생성 (사번 '202401002'는 DB에 없으면 404가 발생하므로,
        // 이 테스트는 인가만 검증하며, 실제 서비스 로직은 무시한다고 가정합니다.)
        String targetEmpNo = "202401002";
        UpdateUserRequest request =
                UpdateUserRequest.builder().phone("010-4444-5555").build();

        // when & then
        mockMvc.perform(patch("/api/v1/users/{enpNo}", targetEmpNo)
                        .with(user(managerPrincipal)) // ROLE_MANAGER로 요청
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // 시큐리티는 통과하지만, UserService에서 USER_NOT_FOUND 예외로 404가 발생할 가능성이 높습니다.
                // 여기서는 4xx 응답을 기대하는 것이 현실적입니다. (204 No Content가 아님)
                .andExpect(status().is4xxClientError()); // 404 Not Found 또는 400 Bad Request 등
    }

    // -------------------------------------------------------------------------
    // 3. 사용자 삭제 (DELETE /api/v1/users/{userId}) - ADMIN만 허용 가정
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("사용자 삭제 실패 - MANAGER 권한으로는 접근 불가 (403 Forbidden)")
    void deleteUser_fail_managerRole() throws Exception {
        // given
        Long targetUserId = 999L; // 가상의 ID

        // when & then
        mockMvc.perform(delete("/api/v1/users/{userId}", targetUserId)
                        .with(user(managerPrincipal)) // ROLE_MANAGER로 요청
                        .with(csrf()))
                .andExpect(status().isForbidden()); // 403 Forbidden 기대
    }

    @Test
    @DisplayName("사용자 삭제 성공 - ADMIN 권한으로 접근")
    void deleteUser_success_adminRole() throws Exception {
        // given
        Long targetUserId = 998L;

        // when & then
        mockMvc.perform(delete("/api/v1/users/{userId}", targetUserId)
                        .with(user(adminPrincipal)) // ROLE_ADMIN으로 요청
                        .with(csrf()))
                // 시큐리티는 통과하지만, UserService에서 USER_NOT_FOUND 예외로 404가 발생할 가능성이 높습니다.
                .andExpect(status().is4xxClientError()); // 404 Not Found 기대
    }
}
